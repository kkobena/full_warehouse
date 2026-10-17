# DataMatrix & codes-barres — Adaptation au marché ivoirien (v2)

> **v2 du 2026-10-15, mise à jour le 2026-10-16 (lot 3 livré).** Reprend [PLAN-DATAMATRIX-MARCHE-CI.md](PLAN-DATAMATRIX-MARCHE-CI.md) (v1)
> après confrontation au code en place. Le diagnostic de la v1 surévaluait le problème et
> plusieurs extraits ne compilaient pas (champ `produit.codeEan` inexistant, mutation d'un
> `record`, hints ZXing inopérants). La v2 ne garde que ce qui est vérifié.
>
> **Problème** : en Côte d'Ivoire, les grossistes n'intègrent pas les codes issus du DataMatrix
> dans leurs systèmes. Quand le code produit encodé par le fabricant n'est pas celui du
> catalogue, le scan ne résout rien et les données riches du DataMatrix (lot, péremption) sont
> perdues.

---

## 0. État d'avancement (2026-10-16)

> Dans tout ce document : **[x]** livré et vérifié, **[ ]** reste à faire.

| Lot | État | Détail |
|-----|------|--------|
| **1 — Robustesse de la lecture** | **fait** | `normalizeCode` consolidé, `parse()` normalise une fois (bug du préfixe de symbologie corrigé), `BarcodeType.GTIN_14` + `parseGtin14`, détection 1D avant GS1, miroir front aligné et testé |
| **2 — Résolution multi-code et alias** | **fait** | `V2.1.45` + `V2.1.46`, entité et repository, `ProductCodeResolverService`, `findFirstByCommandeIdAndAnyCode`, 13ᵉ composant du DTO, 3 endpoints, modale de résolution (`app-modal`), onglet « Codes alternatifs » |
| **Écran de réception séquentiel** | **fait** | code inconnu résolu en une touche sur la ligne courante (voir ci-dessous) |
| **3 — Étiquettes internes** | **fait, en second type d'étiquette** | arbitré le 2026-10-16 : l'étiquette existante n'est pas touchée, un deuxième type s'ajoute et l'utilisateur choisit à la génération (voir ci-dessous) |
| **4.5 — Parser heuristique** | **fait, en mode proposition** | arbitré le 2026-10-15 : les valeurs déduites pré-remplissent l'écran, elles ne créent jamais de lot et ne ciblent jamais un lot à la vente |

**Mode proposition.** `DataMatrixInfo.proposition` distingue « lu dans un AI GS1 » de « déduit
par motif ». Deux garde-fous en découlent, portés par `hasBatchInfoCertaine()` :
`StockEntryServiceImpl` ne crée un lot automatique que sur une valeur certaine (sinon il la
renvoie en pré-remplissage), et `SalesLineServiceImpl` ne cible un lot que sur un numéro certain
— débiter un lot d'après un numéro deviné fausserait la traçabilité et le FEFO. L'écran marque
ces valeurs « Déduit de la boîte — à vérifier ». Sont reconnus : EAN-13 à clé juste, GTIN-14,
lot **explicitement étiqueté** (`LOT:`, `BATCH=`, `CHARGE`), dates AAMMJJ et AAAAMMJJ, la plus
proche date future retenue. Un lot non étiqueté n'est jamais deviné.

**Écart assumé par rapport au plan** : un code hors nomenclature (le code interne d'un
grossiste, `GROS/A-7788`) était rejeté par `parse()` avant toute recherche, ce qui rendait un
alias `CUSTOM` inatteignable. `DataMatrixInfo` porte donc un composant `rawCode`, remonté sans
être interprété (ni lot, ni péremption, ni type déduits). Conséquence visible : un code
illisible donne maintenant « Code X absent de la commande » au lieu de « Format de code non
reconnu », ce dernier message ne restant que pour un scan vide.

**Écran séquentiel.** En saisie séquentielle l'opérateur est déjà positionné sur une ligne : lui
présenter un sélecteur lui ferait refaire un choix déjà fait. Un code inconnu ouvre donc un
bandeau sur la ligne courante — `Entrée` associe, `A` ouvre le sélecteur de ligne (le cas où la
boîte n'est pas la ligne courante), `Échap` ignore. Deux écritures s'y cachent, et les confondre
serait grave :

| Cas | Écriture |
|-----|----------|
| La ligne porte un code **provisoire** | le code scanné **devient** le code catalogue → `updateCodeCip`, circuit AX-23 inchangé |
| La ligne porte un **vrai CIP** | le CIP n'est pas touché, le code devient un **alias** |

Après mémorisation, le scan est **rejoué** : quantité, lot et FMD repassent par le chemin normal,
rien n'est appliqué deux fois. Si la mémorisation échoue, le bandeau reste, pour ne pas perdre le
scan.

Un seul bandeau porte les deux cas (`rh-seq__code-banner`), un seul état (`scanInconnu`) et un
seul point d'écriture (`onAssocierCodeALaLigneCourante`), qui dispatche sur
`associationEcraseLeCip()`. L'opérateur voit la même coque et fait le même geste ; seul le
libellé du bouton distingue « Associer (devient le CIP) » de « Associer à cet article ». Les
styles reposent sur `--pharma-*` et `--icon-warning` : les couleurs en dur de l'ancien bandeau
provisoire (`#fffbeb`, `#fde68a`, `#92400e`) ont disparu. Le reste de cet écran en compte encore
~70, antérieures à ce chantier.

**Étiquettes internes : un second type, pas une modification.** L'étiquette standard (Code 39, code
article seul) garde exactement son code, son gabarit et son endpoint. À côté d'elle vient une
étiquette « lot », et le **choix du type se fait à la génération**, dans les deux modales
d'impression déjà en place :

| Écran | Standard | Lot |
|-------|----------|-----|
| Bon de livraison (`EtiquetteComponent`) | une étiquette par ligne reçue, inchangé | une étiquette **par lot reçu** |
| Fiche produit (`ProduitEtiquetteModalComponent`) | `qty` étiquettes, inchangé | `qty` étiquettes d'un **lot choisi dans la liste** |

Chemin entièrement séparé : `EtiquetteLotExportService` + `EtiquetteLotExportReportServiceImpl`,
gabarit `templates/etiquetes/lot.html`, endpoints `/commandes/data/entree-stock/etiquettes-lot/…` et
`/produits/{id}/lots/{lotId}/etiquettes`. `EtiquetteExportReportServiceImpl` et
`templates/etiquetes/main.html` ne sont pas modifiés. Seuls deux points sont communs, et par
addition : `EtiquetteDTO` gagne trois champs optionnels que l'ancien gabarit ignore, et
`EtiquetteBarcodeReplacedElement` gagne une branche que seul le préfixe `dm:` déclenche.

**Écart assumé : AI 240, et non AI 01.** La v2 prescrivait `01` + GTIN-14 (`"0" + ean13`).
Impossible ici : le code du catalogue est `fournisseur_produit.code_cip`, un `varchar(10)` interne,
et un GTIN-14 n'accepte que quatorze chiffres — le bourrer de zéros produirait un code que la
résolution ne retrouverait pas. La trame est donc
`FNC1 240<code> GS 10<lot> GS 17<yyMMdd>` : l'AI **240** (identification additionnelle du
fabricant) est le seul qui accepte un code alphanumérique. Le parser le lit et le remonte en
`rawCode`, d'où la résolution du lot 2 le confronte au catalogue puis aux alias. Lot et péremption
venant d'AI GS1, `proposition` reste faux : la réception peut créer le lot.

**Vérifications** : 4600 tests backend (dont 8 d'intégration dédiés sur PostgreSQL 18, migrations
rejouées), 69 tests front, build Angular propre. Le test
`Gs1DataMatrixEncoderTest.relectureParLApplication` boucle la chaîne : trame → symbole →
`DataMatrixReader` → `DataMatrixParserService`, et retrouve code, lot et péremption. Reste un
échec antérieur et étranger à ce chantier : `DashboardWidgetCatalogueIntegrationTest`
(quatre `nav_item` de widget sans fournisseur côté code).

---

## 0 bis. Ce qui change depuis la v1

| Sujet | v1 | v2 (vérifié dans le code) |
|-------|----|---------------------------|
| Diagnostic réception | « la recherche se fait par CIP seul » | `findFirstByCommandeIdAndCip` cherche **déjà** `codeCip` OR `codeEan` OR `produit.codeEanLaboratoire` (`OrderLineRepository.java:108`) |
| Champ produit | `produit.codeEan` | **n'existe pas** : `FournisseurProduit.codeEan` (`code_ean`, `varchar(20)` depuis `V1.2.1`) et `Produit.codeEanLaboratoire` (`code_ean_labo`) |
| Apprentissage | table d'alias + auto-apprentissage à inventer | un circuit existe déjà (AX-23) : lignes `provisionalCode` + `updateCodeCip` → `FournisseurProduitService.updateCip` ; on l'**étend** |
| Import des EAN vers les alias | oui (§3.6) | **non** : l'alias complète les codes existants, il ne les duplique pas |
| Contrainte | `UNIQUE (code)` | `UNIQUE (produit_id, code)` + index sur `code` ; un code peut viser 2 produits → désambiguïsation à l'écran |
| DTO | champ mutable + `withDataMatrixHint()` sur un `record` | 13ᵉ composant du record + DTO dans son propre fichier |
| Scan au comptoir | `SalesLineServiceImpl` | `ProduitServiceImpl:497` → `search_produits_json` (fonction PL/pgSQL, `V2.1.22`) ; `SalesLineServiceImpl` ne fait que le ciblage de lot |
| Étiquette GS1 | `GS1_FORMAT = true` suffit | **faux** : `DataMatrixWriter` ne lit `GS1_FORMAT` et `CHARACTER_SET` que si `DATA_MATRIX_COMPACT = true` |
| 14 chiffres | `case 14 -> DATAMATRIX` | nouveau `BarcodeType.GTIN_14` + branche de parsing (sinon `parseDataMatrix` échoue et renvoie `empty`) |
| Front | non mentionné | `ScanOrchestratorService.detectBarcodeType` (`scan-orchestrator.service.ts:652`) est un **miroir** du Java : toute règle changée l'est des deux côtés |
| Endpoints | nav/rôles non traités | `@RequiresNavAccess` au niveau classe (`ProduitResource` → `catalogue`) ; `NavAccessCompletenessTest` échoue si on oublie |
| Exemples de codes | clés de contrôle invalides, préfixe GS1 CI erroné | jeu d'essai recalculé en annexe C |

---

## 1. Diagnostic vérifié

### 1.1 Ce qui fonctionne déjà — à ne pas refaire

| Fonction | Où | État |
|----------|-----|------|
| Parsing GS1 (AI 01, 10, 11, 17, 21, 37, 710–714) | `DataMatrixParserServiceImpl` | OK |
| Normalisation `]d2`, `]C1`, `<GS>`, `{GS}` | `normalizeCode()` | OK (incomplet, cf. 1.2) |
| Réception : lookup **multi-code** (CIP fournisseur, EAN fournisseur, EAN labo) | `OrderLineRepository.java:108` | OK |
| Réception : création auto du lot si lot + péremption et `APP_GESTION_LOT` | `StockEntryServiceImpl:538-570` | OK |
| Réception : contrôle de doublon de n° de série | `lotRepository.existsBySerialNumberAndProduitId` | OK |
| Réception : quantité depuis l'AI **37** | `StockEntryServiceImpl:529` | OK |
| Réception : association d'un code inconnu à une ligne provisoire | front AX-23 + `OrderLineServiceImpl.updateCodeCip` → `FournisseurProduitService.updateCip` | OK, **persistant** |
| Comptoir : recherche par `code_cip` / `code_ean` / `code_ean_labo` (préfixe) | `search_produits_json` (`V2.1.22`) | OK |
| Vente : ciblage du lot scanné avant FEFO | `SalesLineServiceImpl:391-403` | OK |

Conséquence : **dès que l'EAN de la boîte est enregistré sur une fiche fournisseur, le scan
DataMatrix fonctionne déjà** — en réception comme au comptoir. Le chantier est donc bien plus
étroit que ce que décrivait la v1.

### 1.2 Ce qui manque réellement

1. **Le GTIN-14 brut n'est jamais exploitable.**
   `detectBarcodeType` ne connaît pas les codes de 14 chiffres → `UNKNOWN` → `parse()` renvoie
   `empty`. Et dans un DataMatrix, si le chiffre indicateur de l'AI 01 n'est pas `0`,
   `extractEan13FromGtin()` renvoie `null` : `getProductCode()` sert alors le GTIN-14, que
   personne n'a en base.

2. **Un seul code mémorisable par (produit, fournisseur).**
   `updateCip` écrase `code_cip`. Impossible de mémoriser à la fois le CIP du grossiste, l'EAN de
   la boîte et le GTIN du fabricant.

3. **En cas d'échec, les données riches sont jetées.**
   Les trois retours d'échec de `processScanReception` (`:489`, `:498`, `:506`) passent
   `lot = null`, `serialNumber = null` et `scannedQty = 1` — même quand le DataMatrix a été parsé
   correctement. Le lot et la péremption lus sur la boîte ne remontent pas à l'écran.

4. **Normalisation partielle, et un bug de normalisation.**
   `parse()` normalise pour *détecter* mais passe la chaîne **non normalisée** aux sous-parseurs :
   un scanner qui émet `]C13400933399931` fait stocker `cip13 = "]C13400933399931"`. Manquent
   aussi le FNC1 émis en `è` (Datalogic) et le `\u001D` seul en tête (Code 128 GS1).

5. **Pas de reprise au comptoir.** Un code inconnu scanné en vente ne propose rien : ni recherche
   pré-remplie, ni mémorisation.

### 1.3 Racine du problème

| Ce qu'encode le fabricant | Ce que connaît le catalogue |
|---------------------------|-----------------------------|
| GTIN-14 `07353163006031` (AI 01) | rien, ou l'EAN-13 `7353163006031` si quelqu'un l'a saisi |
| CIP-13 `3400933399931` (AI 711, France) | absent hors produits d'import français |
| Lot, péremption, n° de série (AI 10/17/21) | jamais fournis par le grossiste |
| — | CIP interne du grossiste, seul code sûr (`code_cip`) |

---

## 2. Approches possibles

Trois approches se rencontrent sur des marchés où la chaîne logistique n'est pas GS1-compliant.
Elles sont décrites ici comme des familles de solutions, pas comme l'implémentation de tel
éditeur.

**A. EAN-only + saisie manuelle du lot.** On ignore le DataMatrix et on ne lit que le code 1D.
Simple, mais on perd lot, péremption et série — donc toute la traçabilité.

**B. Table d'alias avec apprentissage.** Chaque produit accepte plusieurs codes ; au premier scan
inconnu, l'utilisateur tranche et la liaison est mémorisée. Les scans suivants passent seuls. Le
lot et la péremption restent capturés.

**C. Multi-lookup + enrichissement du catalogue.** Le scan interroge tous les codes disponibles ;
en cas d'échec, une recherche textuelle s'ouvre avec le lot et la péremption pré-remplis, et le
code est ajouté au produit retenu.

**Retenu : B + C**, qui ne font qu'un ici — c'est aussi la direction qu'avait prise AX-23 avec les
lignes provisoires. L'apport de ce plan est de rendre la mémorisation *multi-code* et de la faire
servir au comptoir.

---

## 3. Stratégie retenue

Trois principes :

1. **Le code scanné n'est jamais jeté.** En cas d'échec, le DTO remonte le code brut, le lot, la
   péremption et la quantité lus sur la boîte.
2. **La résolution dégrade proprement** : codes du catalogue → alias → échec documenté. Jamais de
   rapprochement approximatif sans validation humaine.
3. **On apprend là où l'utilisateur tranche déjà** : le point de décision existe (ligne provisoire
   en réception, recherche produit au comptoir). On y branche l'écriture de l'alias, on n'invente
   pas un second circuit.

Découpage en trois lots **indépendants**, livrables séparément :

| Lot | Contenu | Dépend de | Sans migration ? |
|-----|---------|-----------|------------------|
| **1** | Robustesse de la lecture (normalisation, GTIN-14, miroir front) | — | oui |
| **2** | Résolution multi-code + alias + apprentissage + comptoir | 1 | non |
| **3** | Étiquettes DataMatrix internes | 2 | non (DTO + template) |

---

## 4. Lot 1 — Robustesse de la lecture — [x]

Aucun changement de schéma, aucun changement d'API. Gain immédiat sur les scanners mal
configurés.

### 4.1 `normalizeCode()` — version unique et consolidée — [x]

La v1 proposait deux correctifs concurrents (§9.5 et §10.4), dont l'un plantait sur chaîne vide.
Version retenue, dans `DataMatrixParserServiceImpl` :

```java
// Préfixes de symbologie AIM : ]d2/]d1 DataMatrix, ]C1 Code 128 GS1, ]Q3 QR GS1, ]e0 DataBar
private static final Pattern GS1_PREFIX_PATTERN =
    Pattern.compile("^(\\]d2|\\]d1|\\]C1|\\]Q3|\\]e0)?(.*)$", Pattern.DOTALL);

private String normalizeCode(String code) {
    String s = code.trim();

    // 1. FNC1 émis comme caractère imprimable par certains scanners (è = Datalogic)
    while (!s.isEmpty() && s.charAt(0) == 'è') {
        s = s.substring(1);
    }

    // 2. Préfixe de symbologie AIM
    Matcher m = GS1_PREFIX_PATTERN.matcher(s);
    if (m.matches()) {
        s = m.group(2);
    }

    // 3. Toutes les représentations textuelles du séparateur → GS réel
    s = s.replace("<GS>", GS_STRING)
         .replace("{GS}", GS_STRING)
         .replace("^029", GS_STRING)   // échappement décimal Honeywell
         .replace("%1D", GS_STRING);   // URL-encodé (imports fichier)

    // 4. GS en tête : Code 128 GS1 sans préfixe AIM — le contenu est identique
    while (!s.isEmpty() && s.charAt(0) == GS_CHAR) {
        s = s.substring(1);
    }

    return s;
}
```

> Le `\\x1D` que la v1 ajoutait au `Pattern` est retiré : le GS en tête est traité à l'étape 4,
> qui gère aussi le cas de plusieurs GS consécutifs.

### 4.2 `parse()` — normaliser une seule fois — [x]

Corrige le bug du § 1.2-4 : les sous-parseurs reçoivent aujourd'hui la chaîne brute.

```java
@Override
public Optional<DataMatrixInfo> parse(String barcodeData) {
    if (!StringUtils.hasText(barcodeData)) {
        return Optional.empty();
    }
    String code = normalizeCode(barcodeData);   // ← une fois, pour la détection ET le parsing
    return switch (detectBarcodeType(code)) {
        case EAN_8 -> parseEan8(code);
        case EAN_13 -> parseEan13(code);
        case CIP_7 -> parseCip7(code);
        case CIP_13 -> parseCip13(code);
        case GTIN_14 -> parseGtin14(code);
        case DATAMATRIX -> parseDataMatrix(code);
        case UNKNOWN -> Optional.empty();
    };
}
```

`parseDataMatrix` et `detectBarcodeType` renormalisent sans dommage : la fonction est idempotente.

### 4.3 `GTIN_14` — nouveau type et nouvelle branche — [x]

```java
enum BarcodeType { EAN_8, EAN_13, CIP_7, CIP_13, GTIN_14, DATAMATRIX, UNKNOWN }
```

```java
@Override
public BarcodeType detectBarcodeType(String code) {
    if (!StringUtils.hasText(code)) {
        return BarcodeType.UNKNOWN;
    }
    String n = normalizeCode(code);

    // Un code purement numérique est d'abord un code 1D : la longueur tranche.
    // Testé AVANT la détection GS1, sinon un EAN-13 américain (préfixes 01x, 10x, 11x)
    // passerait pour un DataMatrix. UNKNOWN laisse la main à la détection GS1 ci-dessous,
    // ce qui préserve le cas « AI 17 + date » (17250131 = 8 chiffres).
    if (n.matches("\\d+")) {
        BarcodeType oneD = switch (n.length()) {
            case 7 -> BarcodeType.CIP_7;
            case 8 -> isValidEan8(n) ? BarcodeType.EAN_8 : BarcodeType.UNKNOWN;
            case 13 -> n.startsWith(CIP_13_PREFIX) ? BarcodeType.CIP_13
                : isValidEan13(n) ? BarcodeType.EAN_13 : BarcodeType.UNKNOWN;
            // 14 chiffres : GTIN-14 issu d'un AI 01 sans en-tête, ou ITF-14 de carton.
            // Sans préfixe AIM la distinction est impossible ; on suppose GTIN-14,
            // l'ITF-14 étant rare au comptoir.
            case 14 -> BarcodeType.GTIN_14;
            default -> BarcodeType.UNKNOWN;
        };
        if (oneD != BarcodeType.UNKNOWN) {
            return oneD;
        }
    }

    if (n.indexOf(GS_CHAR) >= 0 || isValidDataMatrix(n)) {
        return BarcodeType.DATAMATRIX;
    }
    return BarcodeType.UNKNOWN;
}

private Optional<DataMatrixInfo> parseGtin14(String code) {
    DataMatrixInfo.Builder builder = DataMatrixInfo.builder().gtin(code);
    String ean13 = extractEan13FromGtin(code);   // null si l'indicateur n'est pas 0
    if (ean13 != null) {
        builder.ean13(ean13);
    }
    return Optional.of(builder.build());
}
```

> Ambiguïté résiduelle assumée : `10` + 11 chiffres est à la fois un EAN-13 plausible et un AI 10
> numérique. La clé de contrôle tranche dans la plupart des cas ; sinon le code part en EAN-13.
> Le lookup multi-code du lot 2 rattrape l'erreur.

### 4.4 Miroir côté front — obligatoire — [x]

`ScanOrchestratorService.detectBarcodeType` (`scan-orchestrator.service.ts:652`) duplique la
logique Java pour éviter un aller-retour réseau. Toute règle du 4.3 doit y être reportée, et
`BarcodeType` (`shared/model/reception-scan-result.model.ts:1`) doit gagner `'GTIN_14'` :

```typescript
export type BarcodeType = 'EAN_8' | 'EAN_13' | 'CIP_7' | 'CIP_13' | 'GTIN_14' | 'DATAMATRIX' | 'UNKNOWN';
```

Le front teste aujourd'hui `length === 8` sans valider la clé et ignore les 14 chiffres : aligner
les deux implémentations, test à l'appui.

### 4.5 Parser heuristique — [x] arbitré : mode proposition

La v1 proposait (§10.6) d'extraire lot, EAN et date par reconnaissance de motifs pour les
DataMatrix sans AI GS1 (`"LOT:ABC123/EXP:20250131/EAN:..."`).

C'est utile mais risqué : un lot ou une péremption déduits d'un motif se retrouvent dans le stock
sans que personne ne les ait validés, et une date mal lue fausse le FEFO. **Décision prise le
2026-10-15 : mode proposition.** Les valeurs alimentent le `dataMatrixHint` (5.5) pour
pré-remplir l'écran, **jamais** la création automatique de lot ni le ciblage d'un lot à la vente.
`DataMatrixInfo.proposition` porte la distinction, `hasBatchInfoCertaine()` la fait respecter.
Sont reconnus : EAN-13 à clé juste, GTIN-14, lot **explicitement étiqueté** (`LOT:`, `BATCH=`,
`CHARGE`), dates AAMMJJ et AAAAMMJJ dont la plus proche dans le futur est retenue. Un lot non
étiqueté n'est jamais deviné.

### 4.6 Tests du lot 1 — [x]

`DataMatrixParserServiceImplTest` existe : y ajouter

- les variantes de préfixe et de séparateur du tableau A.1 ;
- `]C1` + CIP-13 → `cip13` sans préfixe (non-régression du bug 1.2-4) ;
- GTIN-14 nu avec indicateur `0` → `gtin` + `ean13` ; avec indicateur `1` → `gtin` seul ;
- `17250131` seul → `DATAMATRIX`, pas `EAN_8` ;
- un EAN-13 commençant par `01`, `10`, `11` → `EAN_13`, pas `DATAMATRIX`.

---

## 5. Lot 2 — Résolution multi-code, alias et apprentissage — [x]

### 5.1 Migration `V2.1.45__produit_code_alias.sql` — [x]

```sql
CREATE TABLE produit_code_alias
(
    id         INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    produit_id INTEGER     NOT NULL REFERENCES produit (id) ON DELETE CASCADE,
    code       VARCHAR(20) NOT NULL,
    code_type  VARCHAR(20) NOT NULL,                  -- GTIN_14 | EAN_13 | EAN_8 | CIP_13 | CIP_7 | CUSTOM
    source     VARCHAR(20) NOT NULL DEFAULT 'MANUAL', -- MANUAL | AUTO_LEARNED
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50),
    CONSTRAINT uq_produit_code_alias UNIQUE (produit_id, code)
);

-- Lookup par code : volontairement NON unique.
CREATE INDEX idx_produit_code_alias_code ON produit_code_alias (code);
```

Trois choix explicites :

- **`VARCHAR(20)`** comme `fournisseur_produit.code_cip` / `code_ean` (élargis par `V1.2.1`) : un
  GTIN-14 entre.
- **`UNIQUE (produit_id, code)`, pas `UNIQUE (code)`.** Deux conditionnements d'un même
  laboratoire partagent parfois l'EAN chez un grossiste CI, et `fournisseur_produit.code_ean`
  comme `produit.code_ean_labo` n'ont qu'un index **non unique** : une unicité globale casserait à
  la première collision. Un code qui vise plusieurs produits est donc légal, et la désambiguïsation
  est faite à l'écran (5.4).
- **`TIMESTAMP`** (et non `timestamptz`) : convention du schéma, un seul script sur une centaine
  utilise `timestamptz`. Horodatage UTC, comme le reste.

**Pas d'import des codes existants.** La table *complète* `code_cip`, `code_ean` et
`code_ean_labo`, elle ne les recopie pas : pas de double source de vérité, pas de
resynchronisation à prévoir quand un EAN change.

### 5.2 Entité et repository — [x]

```java
// pharmaSmart-domain/.../domain/ProduitCodeAlias.java
@Entity
@Table(name = "produit_code_alias")
public class ProduitCodeAlias implements Serializable { /* id, produit, code, codeType, source, createdAt, createdBy */ }
```

```java
// pharmaSmart-domain/.../repository/ProduitCodeAliasRepository.java
public interface ProduitCodeAliasRepository extends JpaRepository<ProduitCodeAlias, Integer> {
    List<ProduitCodeAlias> findAllByCode(String code);           // plusieurs produits possibles
    List<ProduitCodeAlias> findAllByProduitIdOrderByCreatedAtDesc(Integer produitId);
    boolean existsByProduitIdAndCode(Integer produitId, String code);
}
```

`code_type` et `source` sont des enums du domaine (`enumeration/`), pas des `String` libres.

### 5.3 `ProductCodeResolverService` — [x]

```java
// pharmaSmart-app/.../service/stock/ProductCodeResolverService.java
public interface ProductCodeResolverService {

    /** Codes candidats extraits d'un scan, du plus sûr au plus large : CIP-13, EAN-13, GTIN-14. */
    List<String> listerCodesCandidats(DataMatrixInfo parsed);

    /** Produits portant l'un des codes candidats, via les codes du catalogue puis les alias. */
    List<ProductResolveResult> resoudre(DataMatrixInfo parsed);

    /** Mémorise un code pour un produit. Idempotent. */
    void enregistrerAlias(String code, CodeAliasType type, Integer produitId, CodeAliasSource source);
}
```

```java
// pharmaSmart-domain/.../service/dto/ProductResolveResult.java   (DTO dans son propre fichier)
public record ProductResolveResult(Integer produitId, String libelle, String matchedCode, MatchedBy matchedBy) {
    public enum MatchedBy { CODE_CIP, CODE_EAN, CODE_EAN_LABO, ALIAS }
}
```

Trois écarts volontaires avec la v1, conformes aux conventions du projet :

- `resoudre` renvoie une **liste** : zéro résultat → résolution manuelle, un seul → association
  directe, plusieurs → désambiguïsation. La v1 renvoyait un `Optional` et ne pouvait pas exprimer
  le cas multiple qu'autorise sa propre contrainte.
- **pas de `isNewAlias`** : dans la v1 ce booléen n'était jamais positionné à `true` (les étapes
  1-3 trouvent un code déjà en base, l'étape 4 un alias déjà existant), le bloc d'apprentissage de
  son §3.3 était du code mort. Ici l'écriture de l'alias est déclenchée par l'utilisateur, en 5.6.
- l'impl ne porte **aucune requête** : tout passe par les repositories (`ProduitCodeAliasRepository`,
  `OrderLineRepository`), et les noms de méthodes sont des verbes.

### 5.4 Réception — `processScanReception` — [x]

Un seul lookup, étendu aux alias, essayé sur chaque code candidat :

```java
// OrderLineRepository
@Query("""
    SELECT o FROM OrderLine o
    WHERE o.commande.id = :commandeId
      AND (o.fournisseurProduit.codeCip = :code
           OR o.fournisseurProduit.codeEan = :code
           OR o.fournisseurProduit.produit.codeEanLaboratoire = :code
           OR EXISTS (SELECT 1 FROM ProduitCodeAlias a
                      WHERE a.produit.id = o.fournisseurProduit.produit.id
                        AND a.code = :code))
    """)
Optional<OrderLine> findFirstByCommandeIdAndAnyCode(@Param("commandeId") Integer commandeId,
                                                    @Param("code") String code);
```

`findFirstByCommandeIdAndCip` est conservée tant que ses tests existants ne sont pas migrés, puis
supprimée.

Dans `StockEntryServiceImpl` :

```java
DataMatrixInfo parsed = parsedOpt.get();
OrderLine line = null;
for (String code : productCodeResolverService.listerCodesCandidats(parsed)) {
    line = orderLineRepository.findFirstByCommandeIdAndAnyCode(commandeId, code).orElse(null);
    if (line != null) {
        break;
    }
}
if (line == null) {
    return scanNonResolu(parsed, barcodeType);   // ← lot + péremption + qté conservés (5.5)
}
```

> La recherche de la ligne reste pilotée par les codes, **pas** par un `produitId` : la v1
> proposait `findFirstByCommandeIdAndProduitId`, nom de requête dérivée invalide puisque
> `OrderLine` n'a pas de `produitId` (le chemin réel est `fournisseurProduit.produit.id`).

Le reste de la méthode (FMD, incrément AI 37, création du lot) est inchangé.

### 5.5 `DataMatrixHintDTO` et 13ᵉ composant du record — [x]

`ReceptionScanResultDTO` est un **`record`** : on ne peut pas y ajouter un champ mutable ni un
`withXxx()` qui retourne `this`, comme le faisait la v1. On ajoute un composant.

```java
// pharmaSmart-domain/.../service/dto/DataMatrixHintDTO.java   (fichier dédié)
public record DataMatrixHintDTO(
    String scannedCode,    // code normalisé
    String numLot,         // AI 10
    LocalDate expiryDate,  // AI 17
    String serialNumber,   // AI 21
    int qty                // AI 37, 1 par défaut
) {}
```

```java
public record ReceptionScanResultDTO(
    boolean found, Long orderLineId, String produitLibelle, String produitCip,
    boolean lotAutoCreated, String lotNumero, LotDTO lot, String warningMessage,
    BarcodeType barcodeType, String serialNumber, FmdStatus fmdStatus, int scannedQty,
    DataMatrixHintDTO dataMatrixHint          // ← nouveau, null quand found = true
) {}
```

À reprendre : les **4 constructions** de `StockEntryServiceImpl` (`:489`, `:498`, `:506`, `:575`)
et `StockEntryServiceImplTest`. Les retours d'échec cessent de jeter les données parsées :

```java
private ReceptionScanResultDTO scanNonResolu(DataMatrixInfo parsed, BarcodeType barcodeType) {
    return new ReceptionScanResultDTO(
        false, null, null, parsed.getProductCode(),
        false, parsed.batchNumber(), null,
        "Code non reconnu — sélectionnez le produit",
        barcodeType, parsed.serialNumber(), FmdStatus.ABSENT, parsed.scannedQty(),
        new DataMatrixHintDTO(parsed.getProductCode(), parsed.batchNumber(),
            parsed.expiryDate(), parsed.serialNumber(), parsed.scannedQty())
    );
}
```

Côté front, `IReceptionScanResult` gagne `dataMatrixHint?: IDataMatrixHint | null`. Les champs
`scannedCode` et `provisionalLines` du modèle TS restent **calculés côté client** comme
aujourd'hui (`commande-received.component.ts:672-675`) : le backend ne les renseigne pas.

### 5.6 Apprentissage — étendre AX-23, pas le remplacer — [x]

Le circuit existant écrit le code scanné dans `fournisseur_produit.code_cip` via `updateCodeCip`
→ `updateCip`, qui refuse les doublons par fournisseur et par produit puis efface
`provisionalCode`. On le garde pour le **code du grossiste** et on ajoute, à côté, la mémorisation
des codes du fabricant :

| Situation à l'écran | Action |
|---------------------|--------|
| Ligne provisoire, code du grossiste saisi ou scanné | inchangé : `updateCodeCip` (code CIP définitif) |
| Code non reconnu, produit choisi dans la modale | `POST /api/produits/{id}/codes-alias`, `source = MANUAL` |
| Code reconnu via un alias, scan suivant | rien à écrire — l'alias existe déjà |
| Code reconnu par `code_ean_labo` mais absent de la fiche fournisseur | proposer l'alias, case cochée par défaut |

`AUTO_LEARNED` est réservé à un futur import ; aucune écriture silencieuse d'alias en v2.

Écran de résolution manuelle (réception) :

```
┌──────────────────────────────────────────────────────────┐
│  ⚠ Code non reconnu — sélection manuelle requise         │
│                                                          │
│  Code scanné : 0107353163006031 10ABC101 ␝ 17250131 ...  │
│                                                          │
│  Données extraites :                                     │
│  • Lot        : ABC101                                   │
│  • Péremption : 31/01/2025      ← pré-remplis            │
│  • Qté        : 1                                        │
│                                                          │
│  Rechercher le produit : [ ______________________ ]      │
│                                                          │
│  ☑ Mémoriser ce code pour ce produit                     │
│                                                          │
│                      [ Annuler ]  [ Confirmer ]          │
└──────────────────────────────────────────────────────────┘
```

### 5.7 Comptoir — `V2.1.46__search_produits_json_alias.sql` — [x]

Le lookup du comptoir n'est **pas** dans `SalesLineServiceImpl` : `ProduitServiceImpl:497` parse
le scan puis délègue à `produitRepository.searchProduitsJson` → fonction PL/pgSQL
`search_produits_json`, dont la définition courante est dans `V2.1.22` (lignes 9-136).

`create or replace function` réécrit le corps **entier** : repartir de `V2.1.22`, et ajouter dans
la clause `WHERE` et dans le calcul du score la branche alias :

```sql
-- dans le WHERE, à côté de pf.code_cip / pf.code_ean / p.code_ean_labo
OR (left(q.query, 1) ~ '[0-9]' AND EXISTS (SELECT 1
                                           FROM produit_code_alias a
                                           WHERE a.produit_id = p.id
                                             AND upper(a.code) LIKE upper(q.query) || '%'))
```

```sql
-- dans le score, même rang qu'une correspondance exacte de code
WHEN left(q.query, 1) ~ '[0-9]' AND EXISTS (SELECT 1
                                            FROM produit_code_alias a
                                            WHERE a.produit_id = p.id
                                              AND upper(a.code) = upper(q.query)) THEN 1000
```

Appliquer la même greffe à `search_produits_by_storage_json` (même fichier), utilisée par les
écrans de stock. `SalesLineServiceImpl:391-403` n'est **pas** modifié : il cible déjà le lot par
`numLot`, indépendamment du code produit.

### 5.8 Endpoints — [x]

```
POST   /api/produits/{id}/codes-alias        Mémorise un code pour un produit
GET    /api/produits/{id}/codes-alias        Codes alternatifs d'un produit
DELETE /api/produits/codes-alias/{aliasId}   Supprime un alias erroné
```

Les trois vivent dans `ProduitResource`, qui porte déjà `@RequiresNavAccess("catalogue")` au
niveau classe : **aucun `nav_item` à créer**, l'action est déduite du verbe HTTP (`POST` →
`CREATE`, `DELETE` → `DELETE`). À vérifier en revanche que les rôles censés recevoir la fonction
ont bien `can_create` / `can_delete` sur `catalogue` dans `nav_item_role` — sinon 403, le mode
`PHARMA_NAV_ACCESS_MODE` valant `ENFORCE` par défaut (`application.yml:178`).
`NavAccessCompletenessTest` échoue si un handler n'est pas classé : il sert de garde-fou.

Pas d'endpoint `/api/admin/**` : la gestion des alias est une opération de catalogue, pas
d'administration.

### 5.9 Tests du lot 2 — [x]

- `ProductCodeResolverServiceImplTest` : résolution par `code_cip`, `code_ean`, `code_ean_labo`,
  alias ; zéro, un et **plusieurs** produits pour un même code ; idempotence de
  `enregistrerAlias`.
- `StockEntryServiceImplTest` : code inconnu → `found = false` **et** `dataMatrixHint` renseigné
  (lot, péremption, qté AI 37, n° de série) ; code connu par alias seul → ligne trouvée.
- Test d'intégration sur `findFirstByCommandeIdAndAnyCode` (le `EXISTS` sur une entité jointe est
  typiquement ce qui casse en JPQL).
- Test SQL sur `search_produits_json` : produit trouvé par alias, score identique à une
  correspondance exacte de code.

---

## 6. Lot 3 — Étiquettes internes — [x]

### 6.1 Principe — [x]

Quand la boîte porte un DataMatrix étranger, on imprime une étiquette qui réencode le **code du
catalogue** + lot + péremption. Les scans suivants (vente, inventaire) lisent une étiquette que le
système maîtrise.

**Ce n'est pas une modification de l'étiquette existante mais un second type**, choisi à la
génération. Décision du 2026-10-16 : l'étiquette standard reste identique — même gabarit, même
endpoint, même service — et le type « lot » vit à côté, dans ses propres fichiers. Les deux modales
d'impression déjà en place portent le choix (`app-pill-selector`, deux pilules) ; le libellé sous
le sélecteur dit ce que chaque type encode.

### 6.2 Correction du point bloquant de la v1 — [x]

La v1 affirmait que `GS1_FORMAT = true` fait insérer le FNC1 par ZXing. C'est faux. Dans
`DataMatrixWriter.encode()` (zxing 3.5.4) :

```java
boolean hasCompactionHint = hints.containsKey(EncodeHintType.DATA_MATRIX_COMPACT) && ...;
if (hasCompactionHint) {
    boolean hasGS1FormatHint = hints.containsKey(EncodeHintType.GS1_FORMAT) && ...;
    encoded = MinimalEncoder.encodeHighLevel(contents, charset, hasGS1FormatHint ? 0x1D : -1, shape);
} else {
    encoded = HighLevelEncoder.encodeHighLevel(contents, shape, minSize, maxSize, hasForceC40Hint);
}
```

`GS1_FORMAT` **et** `CHARACTER_SET` ne sont lus que sous `DATA_MATRIX_COMPACT`. Le code de la v1
ne passait pas ce hint : les deux autres étaient ignorés en silence et aucun FNC1 n'était émis —
l'étiquette n'était pas un GS1 DataMatrix, ce qui annulait l'argument même de la section. De plus,
le paramètre `fnc1` de `MinimalEncoder` désigne un caractère **déjà présent** dans le contenu :
ZXing n'ajoute rien, il marque.

Livré dans `service/pdf/Gs1DataMatrixEncoder` :

```java
hints.put(EncodeHintType.DATA_MATRIX_SHAPE, SymbolShapeHint.FORCE_SQUARE);
hints.put(EncodeHintType.DATA_MATRIX_COMPACT, true);   // sans lui, les deux suivants sont ignorés
hints.put(EncodeHintType.GS1_FORMAT, true);
hints.put(EncodeHintType.CHARACTER_SET, "ISO-8859-1");
```

### 6.2 bis `construireEntree` — AI 240 et non AI 01 — [x]

La v2 prescrivait `01` + GTIN-14 (`"0" + ean13`). **Inapplicable** : le code imprimé est
`fournisseur_produit.code_cip`, un `varchar(10)` interne à l'officine, là où l'AI 01 exige
quatorze chiffres. Un bourrage de zéros donnerait un GTIN que la résolution ne retrouverait pas en
base, et l'étiquette ne serait lisible par personne, nous compris.

La trame retenue est donc :

```
240<code catalogue> + GS + 10<lot> + GS + 17<yyMMdd>
```

**L'en-tête FNC1 ne s'écrit pas dans le contenu.** La v2 prescrivait un `FNC1` explicite en tête
(`GS + buildGs1Content(...)`) en s'appuyant sur le fait que ZXing « n'ajoute rien, il marque ».
C'est vrai des caractères *du contenu*, mais l'encodeur pose **en plus** l'en-tête FNC1 du symbole
dès que `GS1_FORMAT` est passé : un 0x1D de tête en produisait donc **deux**, mesuré à la
relecture (`<GS><GS>240…`). Notre parser l'encaissait — `normalizeCode` retire les GS de tête en
boucle — mais un scanner tiers aurait rendu le second comme une donnée, et un validateur GS1
aurait refusé le symbole. Corrigé le 2026-10-16, avec un test de non-régression qui compte les
FNC1 de tête après relecture.

`240` est l'identification additionnelle du fabricant, le seul AI qui accepte un code
alphanumérique de longueur libre. Conséquences, toutes vérifiées par test :

- le parser gagne `AI_IDENTIFIANT_ADDITIONNEL` et remonte son contenu en **`rawCode`** — le
  composant du lot 1 fait précisément pour un code hors nomenclature. `ProductCodeResolverService`
  le confronte alors à `code_cip`, `code_ean`, `code_ean_labo` puis aux alias ;
- lot et péremption venant d'AI GS1, `proposition` reste **faux** : `hasBatchInfoCertaine()` est
  vraie et la réception peut créer le lot, à l'inverse d'une valeur devinée (4.5) ;
- `isValidDataMatrix` n'est **pas** touchée. Ajouter `240` à sa liste de préfixes ferait prendre un
  code interne de sept chiffres commençant par 240 pour un DataMatrix. Inutile de toute façon :
  la trame contient toujours au moins un GS, puisqu'un AI de longueur variable y est suivi d'un
  autre AI, et la détection reconnaît le GS.

**Séparateurs en notation textuelle.** La trame transite par l'attribut `id` d'un `<img>` du
gabarit : un 0x1D nu y serait un caractère illégal en XML. Les séparateurs y sont donc écrits
`{GS}`, que `Gs1DataMatrixEncoder.encoder` reconvertit — notation que `normalizeCode` traite déjà
depuis le lot 1, donc rien de nouveau à apprendre à la lecture. Une entrée d'étiquette se lit
ainsi : `dm:2403794609{GS}10L26050002{GS}17270530`.

**Vérifié sur le PDF réel** (2026-10-16, backend démarré sur un port dédié, base de démo) :
l'étiquette d'un lot de METFORMINE VIATRIS 1000 mg sort en 7,1 ko, le PDF embarque cinq symboles
carrés de 120 × 120 px, et l'image extraite du PDF relue par `DataMatrixReader` donne
`<GS>2403794609<GS>10L26050002<GS>17270530`. Repassée dans `DataMatrixParserService`, cette trame
rend `code = 3794609`, `lot = L26050002`, `péremption = 2027-05-30`, `lot certain = true`, et
`GET /api/produits/resoudre-code?code=3794609` retrouve le produit par `CODE_CIP`. L'étiquette
standard du même produit, elle, sort cinq Code 39 rectangulaires de 116 × 24 px : elle est bien
restée inchangée.

**Reste à valider sur le matériel** : imprimer sur l'étiqueteuse et relire avec le scanner du
poste — la chaîne numérique est bouclée, l'encre et l'optique ne le sont pas. Si la lecture échoue,
l'étiquette standard reste la solution de repli, à un clic.

### 6.3 Intégration dans le pipeline d'étiquettes existant — [x]

Les étiquettes ne passent pas par des images base64 : un service rend un template Thymeleaf et
`EtiquetteBarcodeReplacedElement` (un `ReplacedElementFactory` Flying Saucer) remplace les éléments
dont l'`id` figure dans `barcodesData` par un **Barcode39** OpenPDF.

On étend ce mécanisme plutôt que d'en ouvrir un second : une entrée préfixée **`dm:`** déclenche
une branche DataMatrix (ZXing `BitMatrix` → `BufferedImage` → `ITextFSImage`), les entrées nues
gardent Barcode39. C'est le seul point de la chaîne existante touché, et il l'est par addition :
sans le préfixe, le comportement est identique au bit près.

Le reste est neuf et séparé :

| Élément | Standard (inchangé) | Lot |
|---------|---------------------|-----|
| Service | `EtiquetteExportReportServiceImpl` | `EtiquetteLotExportService` + son impl |
| Gabarit | `templates/etiquetes/main.html` | `templates/etiquetes/lot.html` |
| Endpoint bon | `…/entree-stock/etiquettes/{id}/{orderDate}` | `…/entree-stock/etiquettes-lot/{id}/{orderDate}` |
| Endpoint fiche | `/produits/{id}/etiquettes` | `/produits/{id}/lots/{lotId}/etiquettes` |
| Fichier PDF | `etiquettes_…` | `etiquettes_lot_…` |

`EtiquetteDTO` (POJO partagé) gagne `numLot`, `expiryDate` et `codeDataMatrix`, par setters et
sans toucher au constructeur ; l'ancien gabarit ne les lit pas. Pas de champ `dataMatrixBase64`,
pas de `hasLotInfo` (redondant avec `numLot != null`).

Mise en page de l'étiquette « lot » : le symbole est carré, il tient en 11 mm à gauche de la case
de 38 × 21,2 mm, et le texte (code, `Lot …`, `Exp jj/mm/aaaa`, prix) occupe la droite. Un lot sans
rien à encoder — ni numéro ni péremption — se replie sur le Code 39, pour ne jamais imprimer une
trame GS1 incomplète.

**Format de la péremption lisible : `dd/MM/yyyy`**, arbitré le 2026-10-16. Les boîtes, elles,
portent mois/année — la directive 2001/83/CE l'impose « en clair, mois et année », et GS1 encode
ce cas par `DD=00` dans l'AI 17, que `parseDate` traduit en dernier jour du mois. L'étiquette
s'en écarte volontairement : ce n'est pas une reproduction du marquage fabricant mais une
étiquette interne, relue à côté du tableau des lots de la fiche produit, où les six écrans et les
deux autres gabarits PDF affichent `dd/MM/yyyy`. Conséquence assumée : sur une boîte marquée
`05/2027`, l'étiquette affiche `31/05/2027` — un jour déduit, pas un jour imprimé. Le ticket de
caisse, seul autre support en `MM/yyyy`, a été aligné dans le même mouvement
(`AbstractSaleReceiptService`).

Largeurs mesurées en Arial gras 8 px à 96 DPI, pour 23,0 mm de colonne de texte : `3794609`
7,4 mm, `Lot L26050002` 13,8 mm, `Exp 30/05/2027` 13,8 mm, `1 500 CFA` 9,5 mm. **Limite connue** :
`lot.num_lot` est un `varchar(20)`, et `Lot ` + 20 caractères fait 23,3 mm — un numéro de lot de
longueur maximale débordera la colonne. Non corrigé : le numéro est de toute façon dans le
symbole, et les lots réels font 6 à 10 caractères.

### 6.4 QR Code — un seul format de date — [x] sans objet

Aucun QR « consultation smartphone » n'est produit : la règle est respectée par construction. Si
l'un est ajouté un jour, la péremption y sera encodée en **`yyMMdd`** comme partout ailleurs. La v1
utilisait `ddMMyyyy` pour le QR et `yyMMdd` pour le GS1 : l'application n'aurait pas su relire son
propre QR.

### 6.5 Quand proposer l'impression — [x]

Le choix du type remplace l'idée d'une proposition automatique : plutôt que de deviner quand
l'officine veut une étiquette « lot », on la lui laisse demander là où elle imprime déjà.

| Déclencheur | Comportement livré |
|-------------|--------------------|
| Réception, bon clôturé → « Impression des étiquettes » | le sélecteur offre les deux types ; « Lot » sort une étiquette par lot reçu |
| Fiche produit → « Imprimer étiquettes » | le sélecteur offre les deux types ; « Lot » demande le lot (liste des lots en stock : numéro, péremption, quantité) |
| Fiche produit d'un article sans lot | le type « Lot » n'est pas imprimable, un message le dit, le standard reste disponible |

Aucune invite n'est ajoutée après la modale de résolution d'un code inconnu : à cet instant le lot
n'existe pas encore en base — il naît à l'enregistrement de la réception. L'impression se demande
donc depuis le bon, une fois celui-ci clôturé.

---

## 7. Hors périmètre

| Sujet | Raison |
|-------|--------|
| **FMD** (vérification du n° de série en base européenne) | hors réglementation CI ; le contrôle de doublon local existe déjà et suffit |
| **Rapprochement BDPM** | référentiel français, non applicable |
| **Rapprochement approximatif sur le libellé** | ambigu sans validation humaine ; le lookup par code couvre le besoin |
| **Import des codes existants dans `produit_code_alias`** | double source de vérité ; la résolution lit les deux sources |
| **Alias par fournisseur** | l'alias décrit la boîte, pas la relation commerciale ; le code du grossiste reste dans `fournisseur_produit.code_cip` |
| **Parser heuristique non-GS1** | cf. 4.5, tranché le 2026-10-15 : livré en mode proposition |
| **`UNIQUE (code)` global** | incompatible avec les EAN partagés constatés chez les grossistes CI |

---

## 8. Fichiers à créer / modifier

| État | Lot | Fichier | Action |
|------|-----|---------|--------|
| [x] | 1 | `service/stock/impl/DataMatrixParserServiceImpl.java` | **Modifier** — `normalizeCode`, `parse`, `detectBarcodeType`, `parseGtin14`, `parseHeuristique`, `parseCodeBrut` |
| [x] | 1 | `service/stock/DataMatrixParserService.java` | **Modifier** — `BarcodeType.GTIN_14` |
| [x] | 1 | `shared/model/reception-scan-result.model.ts` | **Modifier** — `'GTIN_14'`, `IDataMatrixHint`, `IProduitCodeAlias`, `IProductResolveResult` |
| [x] | 1 | `shared/scanner/scan-orchestrator.service.ts` | **Modifier** — miroir de la détection |
| [x] | 1 | `DataMatrixParserServiceImplTest.java` | **Modifier** — cas du 4.6 (132 tests) |
| [x] | 1 | `service/dto/DataMatrixInfo.java` | **Modifier** — `rawCode`, `proposition`, `hasBatchInfoCertaine()` (hors liste v2) |
| [x] | 1 | `shared/scanner/scan-orchestrator.service.spec.ts` | **Créer** — 32 cas, miroir du test Java (hors liste v2) |
| [x] | 2 | `db/migration/V2.1.45__produit_code_alias.sql` | **Créer** |
| [x] | 2 | `domain/ProduitCodeAlias.java` + `domain/enumeration/CodeAliasType.java`, `CodeAliasSource.java` | **Créer** |
| [x] | 2 | `repository/ProduitCodeAliasRepository.java` | **Créer** |
| [x] | 2 | `service/dto/ProductResolveResult.java`, `service/dto/DataMatrixHintDTO.java`, `service/dto/ProduitCodeAliasDTO.java` | **Créer** |
| [x] | 2 | `service/stock/ProductCodeResolverService.java` + `service/stock/impl/ProductCodeResolverServiceImpl.java` | **Créer** |
| [x] | 2 | `repository/OrderLineRepository.java` | **Modifier** — `findFirstByCommandeIdAndAnyCode` |
| [x] | 2 | `service/dto/ReceptionScanResultDTO.java` | **Modifier** — 13ᵉ composant |
| [x] | 2 | `service/stock/impl/StockEntryServiceImpl.java` | **Modifier** — 4 constructions, boucle de résolution, garde-fou du lot proposé |
| [x] | 2 | `service/sale/impl/SalesLineServiceImpl.java` | **Modifier** — ciblage de lot sur valeur certaine seulement (hors liste v2, cf. 4.5) |
| [x] | 2 | `web/rest/stock/ProduitResource.java` | **Modifier** — 4 endpoints (3 alias + `resoudre-code`) |
| [x] | 2 | `db/migration/V2.1.46__search_produits_json_alias.sql` | **Créer** — `search_produits_json` + `search_produits_by_storage_json` |
| [x] | 2 | `features/commande/ui/code-resolution/` | **Créer** — contenu de la résolution, enveloppé par `app-modal` |
| [x] | 2 | `features/commande/.../commande-received` | **Modifier** — `app-modal`, rejeu du scan |
| [x] | 2 | `features/commande/.../sequential` | **Modifier** — bandeau unique, association en une touche |
| [x] | 2 | `features/products/data-access/services/products-api.service.ts` | **Modifier** — 4 appels alias (hors liste v2) |
| [x] | 2 | `features/products/ui/produit-codes-alias-tab/` + `produit-detail-panel` | **Créer** — onglet « Codes alternatifs » |
| [x] | 2 | `StockEntryServiceImplTest`, `ProductCodeResolverServiceImplTest`, `ProductCodeAliasIntegrationTest` | **Créer / modifier** |
| [x] | 2 | `code-resolution.component.spec.ts`, `reception-sequential.component.spec.ts` | **Créer** — 19 cas (hors liste v2) |
| [x] | 3 | `service/pdf/Gs1DataMatrixEncoder.java` | **Créer** — trame AI 240 et symbole ZXing |
| [x] | 3 | `service/pdf/EtiquetteBarcodeReplacedElement.java` | **Modifier** — branche sur le préfixe `dm:` |
| [x] | 3 | `service/dto/EtiquetteDTO.java` | **Modifier** — `numLot`, `expiryDate`, `codeDataMatrix` (par setters) |
| [x] | 3 | `service/stock/EtiquetteLotExportService.java` + `impl/EtiquetteLotExportReportServiceImpl.java` | **Créer** — second type, chemin séparé |
| [x] | 3 | `templates/etiquetes/lot.html` | **Créer** — gabarit du type « lot » |
| [x] | 3 | `service/report/Constant.java` | **Modifier** — `ETIQUETES_LOT_TEMPLATE_FILE` |
| [x] | 3 | `service/stock/StockEntryDataService.java` + impl | **Modifier** — `printEtiquetteLot` |
| [x] | 3 | `web/rest/commande/StockEntryDataResource.java` | **Modifier** — endpoint `etiquettes-lot` |
| [x] | 3 | `web/rest/stock/ProduitResource.java` | **Modifier** — endpoint `/produits/{id}/lots/{lotId}/etiquettes` |
| [x] | 3 | `service/stock/impl/DataMatrixParserServiceImpl.java` | **Modifier** — AI 240 lu en `rawCode` (hors liste v2) |
| [x] | 3 | `features/commande/ui/delivery/etiquette` | **Modifier** — sélecteur de type |
| [x] | 3 | `features/products/ui/etiquette-modal` | **Modifier** — sélecteur de type + choix du lot |
| [x] | 3 | `entities/commande/delevery/delivery.service.ts`, `products/data-access/services/products-api.service.ts` | **Modifier** — appels du second type |
| [x] | 3 | `Gs1DataMatrixEncoderTest`, `EtiquetteLotExportReportServiceImplTest`, `produit-etiquette-modal.component.spec.ts` | **Créer** — 41 cas |
| [x] | 3 | `service/stock/impl/EtiquetteExportReportServiceImpl.java`, `templates/etiquetes/main.html` | **Non modifiés** — l'étiquette standard est inchangée |
| [x] | 3 | `scripts/demo-data/27_codes_alias_datamatrix.sql` | **Créer** — codes alternatifs et cibles d'impression |
| [x] | 3 | `scripts/demo-data/99_verification.sql`, `run_all.sql`, `README.md` | **Modifier** — section « codes alternatifs » (10 contrôles) |
| [x] | — | `repository/OrderLineRepository.java` | **Nettoyé** — `findFirstByCommandeIdAndCip`, devenue morte, est supprimée |

> `ProduitServiceImpl` ne figure pas dans cette liste : il passe déjà `getProductCode()` à la
> recherche, c'est la fonction SQL qui change. `SalesLineServiceImpl` y est entré en cours de
> route, non pour résoudre un code — il ne le fait toujours pas — mais pour refuser de cibler un
> lot sur un numéro seulement proposé (4.5).

---

## 9. Ordre et charge

| État | # | Étape | Charge |
|------|---|-------|--------|
| [x] | 1 | Lot 1 — parser + miroir front + tests | 2 j |
| [x] | 2 | Migration `V2.1.45` + entité + repository | 1 j |
| [x] | 3 | `ProductCodeResolverService` + `findFirstByCommandeIdAndAnyCode` + tests | 2 j |
| [x] | 4 | `processScanReception` + `DataMatrixHintDTO` + reprise des 4 constructions et des tests | 1,5 j |
| [x] | 5 | Front réception — modale de résolution, pré-remplissage, mémorisation | 2,5 j |
| [x] | 6 | `V2.1.46` — comptoir et recherche par stock | 1 j |
| [x] | 7 | Endpoints + onglet « Codes alternatifs » | 1,5 j |
| [x] | 8 | Lot 3 — étiquettes, en second type au choix | 2,5 j |

**Lots 1, 2 et 3 : 14 j, livrés.** S'y sont ajoutés, hors estimation initiale : le mode
proposition (4.5), le bandeau de l'écran séquentiel, le passage des styles aux jetons du thème,
et — pour le lot 3 — le second chemin d'étiquette avec son sélecteur de type, là où le plan
prévoyait d'enrichir l'étiquette existante.

**Reste** : la validation sur imprimante et scanner du poste (6.2 bis), et la recette manuelle (10).

---

## 10. Recette

Aucun point n'a encore été passé **sur l'écran réel** : la colonne dit ce qu'un test automatisé
couvre déjà, ce qui ne remplace pas le parcours à la main ni une campagne e2e.

**Jeu de données.** `scripts/demo-data/27_codes_alias_datamatrix.sql` charge ~330 codes
alternatifs, dont sept cas nommés qui couvrent les points 6, 9 et 11 (code partagé, code interne
de grossiste, GTIN à indicateur non nul), et termine en nommant les lots et les bons de livraison
sur lesquels imprimer l'étiquette interne. Les clés de contrôle y sont calculées, pas écrites.
`99_verification.sql` ajoute dix contrôles sur cette section ; la chaîne complète a été rejouée
sur une base jetable, 383 contrôles au vert.

**Parcours e2e écrits (2026-10-17).** Les gestes de la recette sont désormais des parcours
Playwright rattachés au cahier de recette, donc rejouables et illustrés dans le guide. Ils ne
remplacent pas la colonne « passé à l'écran » : ils la rendent reproductible.

| Parcours | Points couverts |
|----------|-----------------|
| `ACH-85` — désigner l'article d'un code inconnu (vue tableau) | 2, 3, 4 (le scan est rejoué), 8 (trame à séparateur `{GS}`) |
| `ACH-86` — associer un code inconnu à la ligne courante (séquentiel) | 10 |
| `REF-69` — consulter et compléter les codes alternatifs | 7, 9, 16 |
| `VTE-74` — retrouver un article au comptoir par un code mémorisé | 5 |
| `REF-65` — imprimer l'étiquette d'un lot | 13, 14 |
| `ACH-76` — étiquettes d'un bon reçu, type et format | 12, 13 |

Restent sans parcours, et pour deux raisons distinctes : le point 6 (même code sur deux
produits) parce que la désambiguïsation se joue dans la recherche du comptoir et non dans la
modale de réception, qui ne propose que les lignes du bon ; le point 15 parce qu'il demande une
imprimante et un scanner, qu'un navigateur n'a pas.

Deux écarts ont été trouvés en les écrivant : le sélecteur de ligne s'ouvrait **par-dessus** le
bandeau du mode séquentiel (corrigé dans `commande-received`, cf. `e2e/parcours/README.md`), et
`99_verification.sql` ne contrôlait pas la présence de chaque cas nommé — un cas disparu aurait
fait échouer un parcours sans qu'on sache que la cause était le jeu de données.

| # | Point | Couvert par un test | Passé à l'écran |
|---|-------|---------------------|-----------------|
| 1 | EAN déjà en fiche fournisseur → ligne trouvée, lot créé si `APP_GESTION_LOT` (non-régression) | [x] `ProductCodeAliasIntegrationTest`, `StockEntryServiceIntegrationTest` | [ ] |
| 2 | DataMatrix inconnu → lot et péremption affichés, case « mémoriser » cochée | [x] `StockEntryServiceImplTest`, `code-resolution.component.spec` | [ ] |
| 3 | Confirmer le produit → alias écrit | [x] `ProductCodeAliasIntegrationTest` | [ ] |
| 4 | Rescanner la même boîte → trouvée via l'alias, sans intervention | [x] `ProductCodeAliasIntegrationTest` | [ ] |
| 5 | La même boîte **au comptoir** → produit trouvé | [x] `rechercheComptoirParAlias` (SQL réel) | [ ] |
| 6 | Même EAN sur deux produits → les deux proposés | [x] `codePartageParDeuxProduits` | [ ] |
| 7 | Supprimer l'alias → scan non reconnu, fiche intacte | [x] `aliasSupprime` | [ ] |
| 8 | Scanner mal configuré (pas d'AIM ID, GS en `<GS>`) → même résultat | [x] `DataMatrixParserServiceImplTest`, `scan-orchestrator.service.spec` | [ ] |
| 9 | Code interne alphanumérique d'un grossiste → résolu, mémorisable | [x] `aliasSurUnCodeInterne` | [ ] |
| 10 | Mode séquentiel : code inconnu → association en une touche, CIP ou alias selon la ligne | [x] `reception-sequential.component.spec` | [ ] |
| 11 | Code en texte libre → valeurs marquées « déduites », aucun lot créé | [x] `ParseHeuristique`, `code-resolution.component.spec` | [ ] |
| 12 | Bon clôturé, type « Standard » → étiquettes identiques à avant le lot 3 (non-régression) | [x] `EtiquetteExportReportServiceImplTest` (inchangé) | [ ] |
| 13 | Bon clôturé, type « Lot » → une étiquette par lot reçu, avec DataMatrix, lot et péremption | [x] `EtiquetteLotExportReportServiceImplTest` | [ ] |
| 14 | Fiche produit, type « Lot » → le lot se choisit dans la liste ; un article sans lot n'offre que le standard | [x] `produit-etiquette-modal.component.spec` | [ ] |
| 15 | **Imprimer l'étiquette « lot », la rescanner au comptoir** → produit trouvé, lot ciblé | [x] `Gs1DataMatrixEncoderTest` (chaîne encodage → relecture → parsing, sur les valeurs du jeu de démo) | [x] **sur le PDF réel** (6.2 bis) ; [ ] sur le matériel |
| 17 | L'étiquette standard du même produit reste identique (non-régression visuelle) | [x] `EtiquetteExportReportServiceImplTest` (inchangé) | [x] cinq Code 39 de 116 × 24 px dans le PDF |
| 16 | Onglet « Codes alternatifs » d'un produit → les codes mémorisés, leur nature, leur auteur et leur date | [x] `27_codes_alias_datamatrix.sql` + contrôles 99 | [ ] |

---

## Annexe A — Variations de sortie des scanners

### A.1 Ce qui arrive réellement dans le champ de saisie

| Variante | Exemple | Origine | Géré |
|----------|---------|---------|------|
| Préfixe `]d2` | `]d20107353163006031…` | Honeywell / Zebra, DataMatrix | avant v2 |
| Préfixe `]C1` | `]C10107353163006031…` | Code 128 GS1 (1D, pas DataMatrix) | avant v2, mais contenu non normalisé (1.2-4) |
| Préfixe `]Q3` | `]Q30107353163006031…` | QR GS1 | lot 1 |
| FNC1 imprimable | `è0107353163006031…` | certains Datalogic | lot 1 |
| GS seul en tête | `␝0107353163006031…` | Code 128 GS1 sans AIM ID | lot 1 |
| `<GS>` / `{GS}` | `…10ABC101<GS>17250131` | imports fichier, ERP tiers | avant v2 |
| `^029` / `%1D` | `…10ABC101^02917250131` | échappements Honeywell / URL | lot 1 |
| Aucun préfixe | `0107353163006031…` | fabricants qui omettent le FNC1 | avant v2 |

### A.2 Configurer le scanner reste la mesure la plus rentable

| Paramètre | Valeur | Effet |
|-----------|--------|-------|
| AIM symbology identifier | activé | `]d2` / `]C1` / `]Q3` → le parser sait d'où vient le code |
| GS character output | `<GS>` ou `\x1D` | séparateur détectable dans n'importe quel champ texte |
| DataMatrix ECC200 | activé | certains scanners désactivent les formats 2D par défaut |
| Code 128 / GS1-128 | activé | lit les codes GS1 linéaires, de contenu identique |
| QR Code | activé | utilisé par une partie des fabricants asiatiques |

Les scanners se configurent par carnet de codes-barres (documentation Honeywell Voyager,
Zebra DS2208…).

> **Tauri + scanner USB** : le texte arrive par l'événement clavier du WebView ; le poste
> distingue un scan d'une frappe par la durée inter-touches. `ScanOrchestratorService` gère déjà
> ce cas, port série CDC inclus, et bascule sur HID en secours.

---

## Annexe B — Ce qui varie selon les marchés

Le symbole **ECC200** (ISO/IEC 16022) et les **Application Identifiers** (GS1 General
Specifications) sont identiques partout. Ce qui varie, c'est le contenu.

| Élément | France / UE | USA | Japon / Corée | Côte d'Ivoire |
|---------|-------------|-----|---------------|---------------|
| Symbole 2D | DataMatrix | DataMatrix ou QR | QR majoritaire | DataMatrix quand il y en a un |
| AI 01 (GTIN) | toujours | toujours | toujours | si GS1 respecté |
| AI 10 (lot) | exigé (FMD) | exigé (DSCSA) | oui | rare |
| AI 17 (péremption) | exigé (FMD) | oui | oui | parfois |
| AI 21 (n° de série) | obligatoire depuis 2019 (FMD) | sérialisation DSCSA depuis 2017-2018 ; 2026 ne concerne que des exemptions résiduelles de petits dispensateurs | non | non |
| AI 711 (CIP, France) | présent | absent | absent | absent |
| AI 710 (PZN, Allemagne) | présent en Allemagne, absent en France | absent | absent | absent |
| AI 714 (Portugal) | présent au Portugal | absent | absent | absent |

> Le parser traite déjà les AI 710, 712, 713 et 714 (`DataMatrixParserServiceImpl:330`) en les
> rangeant dans `cip13` — y compris lorsqu'un AI 711 est déjà présent, qu'ils écrasent. À
> corriger si un produit à double NHRN se présente ; non prioritaire en CI, où aucun n'apparaît.

Même médicament, deux boîtes selon le marché de distribution :

```
Boîte France :                      Boîte Côte d'Ivoire :
  (01)03400933399931                  (01)06181234567898   ← préfixe GS1 CI = 618
  (10)ABC101                          (10)ABC101
  (17)250131                          (17)250131
  (21)SER42      ← FMD                → pas d'AI 21
  (711)3400933399931  ← CIP           → pas d'AI 711
                                      → ou pas de DataMatrix du tout, juste un EAN-13 1D
```

---

## Annexe C — Jeu d'essai (clés de contrôle vérifiées)

Les exemples de la v1 (`3400933399934`, `06111234567890`) portaient des clés de contrôle
**invalides** et auraient été rejetés par `isValidEan13`. `611` désignait par ailleurs le Maroc,
pas la Côte d'Ivoire (`618`). Valeurs recalculées :

| Code | Type | Clé |
|------|------|-----|
| `7353163006031` | EAN-13 | valide |
| `07353163006031` | GTIN-14, indicateur 0 | valide |
| `3400933399931` | CIP-13 / EAN-13, préfixe 340 | valide |
| `03400933399931` | GTIN-14 | valide |
| `6181234567898` | EAN-13, préfixe GS1 CI | valide |
| `06181234567898` | GTIN-14 | valide |

Chaîne DataMatrix de référence (`␝` = `\u001D`) :

```
]d20107353163006031 10ABC101 ␝ 17250131 21SER42
```

sans les espaces de lisibilité :

```
]d2010735316300603110ABC101␝1725013121SER42
```

Décodage attendu par `DataMatrixParserService` :

| Champ | Valeur |
|-------|--------|
| `gtin` | `07353163006031` |
| `ean13` | `7353163006031` |
| `cip13` | `null` |
| `batchNumber` | `ABC101` |
| `expiryDate` | `2025-01-31` |
| `serialNumber` | `SER42` |
| `scannedQty` | `1` |
| `getProductCode()` | `7353163006031` |

> La chaîne d'exemple de la v1 (`01073531630060311021ABC10175 21SER42`) ne décodait pas en
> « lot ABC101 / péremption 01/2025 » : sans séparateur, l'AI 10 étant de longueur variable, le
> lot aurait absorbé toute la fin de la chaîne.
