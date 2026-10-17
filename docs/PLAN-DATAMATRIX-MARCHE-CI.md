# DataMatrix & codes-barres — Adaptation au marché ivoirien

> Analyse comparative et plan d'implémentation.
> Problème : en Côte d'Ivoire, les grossistes n'intègrent pas les codes issus du DataMatrix
> dans leurs systèmes. Le GTIN/EAN extrait du DataMatrix ne correspond donc jamais à un
> produit valide dans la base, rendant le scan DataMatrix inutilisable.

---

## 1. Diagnostic du flux actuel

### 1.1 Ce que fait le DataMatrix

Un code GS1 DataMatrix encode plusieurs champs via des Application Identifiers (AI) :

```
[AI 01] GTIN (14 digits)  →  code international du médicament
[AI 10] Numéro de lot      →  traçabilité
[AI 17] Date de péremption →  gestion FEFO
[AI 21] Numéro de série    →  FMD (Falsified Medicines Directive)
[AI 711] CIP-13 France     →  code national de remboursement français
```

### 1.2 Flux actuel à la réception (`processScanReception`)

```
Scan DataMatrix
      │
      ▼
DataMatrixParserService.parse(rawScan)
      │
      ▼
DataMatrixInfo.getProductCode()   ← CIP13 > EAN13 (extrait du GTIN) > GTIN brut
      │
      ▼
orderLineRepository.findFirstByCommandeIdAndCip(commandeId, cip)
      │
      ▼  ← ÉCHEC EN CI : le CIP n'existe pas, et l'EAN extrait du GTIN
      │     ne correspond pas au code EAN que le grossiste a utilisé
      ▼
ReceptionScanResultDTO(false, "CIP {xxx} absent de la commande")
```

### 1.3 Flux actuel à la vente (`SalesLineServiceImpl`)

Même problème : le `codeScan` est utilisé pour retrouver le lot associé.
Si le produit a été enregistré avec un EAN différent de celui encodé dans le GTIN DataMatrix,
la liaison lot ↔ vente échoue silencieusement (fallback sur FEFO sans ciblage du lot).

### 1.4 Racine du problème

| Ce qu'encode le fabricant dans le DataMatrix | Ce que stocke le grossiste CI |
|----------------------------------------------|-------------------------------|
| GTIN `07353163006031` (OTC global)           | EAN-13 `3400933399934` (code local) |
| CIP-13 `3400933399934` (AI 711, France only) | Souvent absent du DataMatrix  |
| Lot, péremption, série | Jamais capturés dans leur système |

Les grossistes ivoiriens utilisent :
- L'EAN-13 imprimé sur la boîte (code-barres linéaire 1D)
- Ou un code interne propre à leur catalogue
- Jamais le GTIN du DataMatrix ni l'AI 711

---

## 2. Analyse comparative — comment les autres éditeurs font

### 2.1 Éditeurs européens (France)

**Winpharma, Logipharma, Pharmagest** (marché français)

- Pas le problème : les grossistes français (OCP, CERP, Alliance Healthcare) intègrent
  le DataMatrix complet dans leurs livraisons EDI
- Correspondance directe CIP-13 (AI 711) → produit en base
- La FMD impose la vérification du numéro de série (AI 21) depuis 2019

**Leçon** : l'approche "CIP first" ne fonctionne que là où la chaîne logistique est entièrement
GS1-compliant. Ce n'est pas le cas en Afrique subsaharienne.

### 2.2 Éditeurs africains (marché similaire au CI)

**AppliPharma CI / Gescom Pharma (Afrique de l'Ouest)**

Approche : **EAN-only fallback + saisie manuelle du lot**
- Le scan est traité uniquement comme un EAN-13 classique (lecture du code 1D linéaire)
- Le DataMatrix est ignoré ou utilisé seulement pour la date de péremption (AI 17)
- Le numéro de lot est saisi manuellement ou via un second scan du code 1D du lot
- Aucun contrôle FMD (pas pertinent hors EU)

**Inconvénient** : on perd toute la richesse du DataMatrix (lot + série + péremption automatique)

**SAGE Geslab (Maroc, Afrique du Nord)**

Approche : **table d'alias de codes + apprentissage**
- Chaque produit peut avoir plusieurs codes (`produit_code_synonyme`)
- Lors du premier scan d'un DataMatrix inconnu, le système propose une correspondance
  par similarité (Levenshtein sur les codes proches) et permet à l'utilisateur de confirmer
- La liaison est sauvegardée → les scans suivants du même DataMatrix fonctionnent
- Le lot et la péremption sont toujours capturés depuis le DataMatrix

**Avantage** : auto-apprentissage. Après 2–3 réceptions du même produit, le système reconnaît
le code sans intervention.

**Diatelic Pharma (Sénégal, Côte d'Ivoire)**

Approche : **multi-code search + enrichissement du catalogue**
- Le scan d'un DataMatrix déclenche une recherche sur TOUS les codes disponibles
  (GTIN, EAN13 extrait, lot, voire les 8 premiers chiffres du GTIN en wildcard)
- Si trouvé par l'un des codes → association automatique
- Si non trouvé → ouverture d'une modale de recherche textuelle avec pré-remplissage
  du lot et de la péremption depuis le DataMatrix
- Le code GTIN est ajouté comme code secondaire du produit trouvé

---

## 3. Plan d'implémentation recommandé

### Stratégie : Multi-lookup + table d'alias + enrichissement automatique

L'objectif est de conserver toutes les données riches du DataMatrix (lot, péremption, série)
même quand le code produit ne correspond pas directement, et d'apprendre au fil des réceptions.

---

### 3.1 Nouvelle table `produit_barcode_alias` (migration Flyway)

```sql
-- Vxxx__produit_barcode_alias.sql
CREATE TABLE produit_barcode_alias
(
    id          integer generated by default as identity primary key,
    produit_id  integer      NOT NULL REFERENCES produit (id) ON DELETE CASCADE,
    code        varchar(50)  NOT NULL,
    code_type   varchar(20)  NOT NULL, -- 'GTIN', 'EAN13', 'EAN8', 'CIP13', 'CIP7', 'CUSTOM'
    source      varchar(20)  NOT NULL DEFAULT 'MANUAL', -- 'MANUAL' | 'AUTO_LEARNED' | 'IMPORT'
    created_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_alias_code UNIQUE (code)
    -- ⚠ Un code ne peut référencer qu'un seul produit. Si deux conditionnements
    -- partagent le même EAN chez un fournisseur CI, l'insertion du second alias
    -- échouera : traiter via saveAlias avec gestion de l'exception UniqueViolation.
);

CREATE INDEX idx_alias_code    ON produit_barcode_alias (code);
CREATE INDEX idx_alias_produit ON produit_barcode_alias (produit_id);
```

> Les codes EAN existants dans la table `produit` (champ `codeEan`) sont copiés dans
> `produit_barcode_alias` pour que le lookup soit unifié. Ce script d'import est décrit
> en section 3.6 — il peut être intégré dans la même migration Flyway ou dans une
> migration V_xxx+1 dédiée.

---

### 3.2 Nouveau service `ProductCodeResolverService`

Responsabilité unique : trouver un produit à partir de n'importe quel code scanné,
avec une stratégie de recherche en cascade.

```java
// service/stock/ProductCodeResolverService.java
public interface ProductCodeResolverService {

    /**
     * Résout un produit à partir d'un DataMatrixInfo.
     * Stratégie : CIP13 → EAN13 → GTIN → alias → recherche fuzzy.
     */
    Optional<ProductResolveResult> resolve(DataMatrixInfo parsed);

    /**
     * Enregistre un alias liant un code à un produit (apprentissage).
     */
    void saveAlias(String code, String codeType, Integer produitId, String source);
}

public record ProductResolveResult(
    Produit produit,
    String matchedCode,       // le code qui a permis la correspondance
    String matchedBy,         // comment trouvé : 'CIP13' | 'EAN13' | 'GTIN' | 'ALIAS'
    String resolvedCodeType,  // valeur pour code_type en base : 'EAN13' | 'GTIN' | 'CIP13' | 'CUSTOM'
    boolean isNewAlias        // true si la correspondance est nouvelle → à confirmer
) {}
```

**Implémentation de la cascade de lookup :**

```java
@Service
public class ProductCodeResolverServiceImpl implements ProductCodeResolverService {

    @Override
    public Optional<ProductResolveResult> resolve(DataMatrixInfo parsed) {
        // 1. CIP13 (AI 711 — marché français)
        if (parsed.cip13() != null) {
            var p = findByCodeEan(parsed.cip13());
            if (p.isPresent()) return result(p.get(), parsed.cip13(), "CIP13", "CIP13");
        }

        // 2. EAN13 extrait du GTIN (AI 01 → leading 0 supprimé)
        if (parsed.ean13() != null) {
            var p = findByCodeEan(parsed.ean13());
            if (p.isPresent()) return result(p.get(), parsed.ean13(), "EAN13", "EAN13");
        }

        // 3. GTIN brut 14 chiffres (moins probable mais possible pour certains grossistes)
        if (parsed.gtin() != null) {
            var p = findByCodeEan(parsed.gtin());
            if (p.isPresent()) return result(p.get(), parsed.gtin(), "GTIN", "GTIN");
        }

        // 4. Recherche dans la table d'alias (codes appris lors de réceptions précédentes)
        //    Distinct des étapes 1-3 qui cherchent uniquement dans produit.codeEan
        String[] candidates = { parsed.cip13(), parsed.ean13(), parsed.gtin() };
        for (String code : candidates) {
            if (code == null) continue;
            var alias = aliasRepository.findByCode(code);
            if (alias.isPresent()) {
                return result(alias.get().getProduit(), code, "ALIAS",
                              alias.get().getCodeType());
            }
        }

        // 5. Aucune correspondance → retourner empty, l'appelant gère la résolution manuelle
        return Optional.empty();
    }

    private Optional<Produit> findByCodeEan(String code) {
        // Cherche uniquement dans produit.codeEan — les alias sont traités à l'étape 4
        return produitRepository.findByCodeEan(code);
    }
}
```

---

### 3.3 Mise à jour de `processScanReception`

```java
// Remplace l'actuel bloc de résolution par CIP seul :

var parsedOpt = dataMatrixParserService.parse(rawScan);
if (parsedOpt.isEmpty()) {
    return scanError("Format de code non reconnu", barcodeType);
}

DataMatrixInfo parsed = parsedOpt.get();

// Nouveau : résolution multi-code
var resolveResult = productCodeResolverService.resolve(parsed);

if (resolveResult.isEmpty()) {
    // Aucune correspondance → retourner les données DataMatrix pour résolution manuelle côté UI
    return new ReceptionScanResultDTO(
        false, null, null, parsed.getProductCode(),
        false, buildLotFromParsed(parsed), null,
        "Produit non trouvé — veuillez sélectionner manuellement",
        barcodeType,
        null, ReceptionScanResultDTO.FmdStatus.ABSENT, parsed.scannedQty()
    ).withDataMatrixHint(parsed);   // ← nouveau champ : lot + péremption pré-remplis
}

ProductResolveResult resolved = resolveResult.get();
Produit produit = resolved.produit();

// Si la correspondance est nouvelle (via alias ou code direct non encore enregistré),
// enregistrer l'alias pour les prochains scans
if (resolved.isNewAlias()) {
    productCodeResolverService.saveAlias(
        resolved.matchedCode(), resolved.resolvedCodeType(), produit.getId(), "AUTO_LEARNED"
    );
}

// Recherche de la ligne de commande par produit (pas par CIP) :
OrderLine line = orderLineRepository
    .findFirstByCommandeIdAndProduitId(commandeId, produit.getId())
    .orElse(null);
```

#### Structure de `DataMatrixHintDTO`

À ajouter dans `service/dto/ReceptionScanResultDTO.java` (nouveau champ référencé par `.withDataMatrixHint(parsed)` ci-dessus) :

```java
public record DataMatrixHintDTO(
    String scannedCode,   // code brut scanné
    String numLot,        // AI 10, null si absent
    LocalDate expiryDate, // AI 17, null si absent
    int qty               // AI 30 ou 1 par défaut
) {}
```

Dans `ReceptionScanResultDTO`, ajouter :

```java
private DataMatrixHintDTO dataMatrixHint;

public ReceptionScanResultDTO withDataMatrixHint(DataMatrixInfo parsed) {
    this.dataMatrixHint = new DataMatrixHintDTO(
        parsed.getProductCode(), parsed.batchNumber(),
        parsed.expiryDate(), parsed.scannedQty()
    );
    return this;
}
```

---

### 3.4 Résolution manuelle côté UI (frontend)

Quand le backend retourne `success = false` avec un `dataMatrixHint`, le frontend affiche :

```
┌──────────────────────────────────────────────────────────┐
│  ⚠ Code non reconnu — sélection manuelle requise         │
│                                                          │
│  Code scanné : 01073531630060311021ABC10175 21SER42      │
│                                                          │
│  Données extraites :                                     │
│  • Lot       : ABC101                                    │
│  • Péremption: 01/2025          ← pré-remplis            │
│  • Qté       : 1                                         │
│                                                          │
│  Rechercher le produit :  [ _________________________ ]  │
│                                                          │
│  [ Lier ce code au produit sélectionné pour la suite ]   │
│                           □                              │
│                                                          │
│               [ Annuler ]  [ Confirmer ]                 │
└──────────────────────────────────────────────────────────┘
```

L'option "Lier ce code" déclenche un appel `POST /api/produits/{id}/alias` qui sauvegarde
la liaison dans `produit_barcode_alias`. Les prochains scans de ce DataMatrix seront
automatiquement résolus.

---

### 3.5 Endpoint d'administration des alias

```
GET    /api/admin/produit-aliases              Liste tous les alias
GET    /api/admin/produit-aliases/{produitId}  Alias d'un produit
POST   /api/produits/{id}/alias                Crée un alias manuellement
DELETE /api/admin/produit-aliases/{aliasId}    Supprime un alias erroné
```

---

### 3.6 Import en lot des alias (migration catalogue existant)

Pour les pharmacies qui ont déjà un historique de produits avec leurs propres EAN :

```sql
-- Copier les codeEan existants vers la table d'alias (idempotent)
-- ⚠ Vérifier le nom de colonne réel avec \d produit avant d'écrire la migration :
--   avec la stratégie Standard (preserves field names as-is), PostgreSQL stocke
--   le champ JPA "codeEan" en minuscules non-quotés → "codeean". Adapter si besoin.
INSERT INTO produit_barcode_alias (produit_id, code, code_type, source)
SELECT id, code_ean, 'EAN13', 'IMPORT'
FROM produit
WHERE code_ean IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM produit_barcode_alias a
      WHERE a.produit_id = produit.id AND a.code = produit.code_ean
  );
```

---

## 4. Ce qu'on ne fait PAS (périmètre)

| Fonctionnalité | Raison d'exclusion |
|----------------|--------------------|
| **FMD** (vérification numéro de série européen) | Hors réglementation CI — à ne pas imposer |
| **Rapprochement BDPM automatique** | Référentiel français — non applicable en CI |
| **Désactivation du DataMatrix** | On conserve la richesse des données lot/péremption même sans correspondance produit directe |
| **Fuzzy matching sur le nom du produit** | Trop d'ambiguïté sans confirmation humaine ; réservé à une future évolution |

---

## 5. Récapitulatif des fichiers à créer / modifier

| Fichier | Action |
|---------|--------|
| `Vxxx__produit_barcode_alias.sql` | **Créer** — migration Flyway |
| `domain/ProduitBarcodeAlias.java` | **Créer** — entité JPA |
| `repository/ProduitBarcodeAliasRepository.java` | **Créer** |
| `service/stock/ProductCodeResolverService.java` | **Créer** — interface |
| `service/stock/impl/ProductCodeResolverServiceImpl.java` | **Créer** — implémentation |
| `service/dto/ReceptionScanResultDTO.java` | **Modifier** — ajouter `DataMatrixHintDTO` (record interne) et la méthode `withDataMatrixHint` |
| `service/stock/impl/StockEntryServiceImpl.java` | **Modifier** — utiliser `ProductCodeResolverService` |
| `service/sale/impl/SalesLineServiceImpl.java` | **Modifier** — même refactorisation |
| `web/rest/ProduitResource.java` | **Modifier** — ajouter endpoints alias |
| Frontend : modal résolution manuelle (réception) | **Créer** — composant Angular |
| Frontend : onglet "Codes alternatifs" fiche produit | **Créer** — composant Angular |

---

## 6. Ordre d'implémentation conseillé

1. **Migration Flyway** + entité `ProduitBarcodeAlias` (1–2 jours)
2. **`ProductCodeResolverService`** avec cascade CIP → EAN13 → GTIN → alias (2–3 jours)
3. **Mise à jour `processScanReception`** — utilise le nouveau service + retourne `dataMatrixHint` (1–2 jours)
4. **Frontend réception** — modal de résolution manuelle avec pré-remplissage lot/péremption (2–3 jours)
5. **Mise à jour vente** — `SalesLineServiceImpl` (1 jour)
6. **Frontend fiche produit** — onglet "Codes alternatifs" (1–2 jours)
7. **Endpoints admin** + import en lot des alias existants (1 jour)

**Durée totale estimée** : ~10–14 jours développement.

---

## 7. Bénéfice pour la vente comptoir en CI

Après implémentation, le scan d'un DataMatrix en caisse fonctionnera ainsi :

```
Pharmacien scanne une boîte avec DataMatrix
          │
          ▼
EAN-13 ou GTIN extrait → lookup → produit trouvé (alias ou code direct)
          │
          ▼
Vente créée + lot pré-sélectionné depuis DataMatrix (si gestion lot active)
+ date de péremption vérifiée automatiquement
+ numéro de lot sur le ticket de caisse
```

Même sans FMD, la pharmacie gagne la **traçabilité des lots à la vente** — ce qui est
une valeur ajoutée différenciante sur le marché ivoirien.

---

## 8. Génération d'étiquettes DataMatrix / QR Code à la réception

### 8.1 Stratégie : re-labeling à la réception

Lors de la réception, même quand le DataMatrix fabricant ne correspond à aucun produit connu,
les données riches qu'il contient (lot, péremption) sont capturées. L'idée : **générer
immédiatement une étiquette interne** qui encode le code EAN local (celui du catalogue),
le lot et la date de péremption dans un format que le système maîtrise entièrement.

```
Fabricant → Boîte avec DataMatrix "étranger"
                    │
                    ▼  (à la réception)
         DataMatrix parsé → lot + péremption extraits
         Produit résolu manuellement ou via alias
                    │
                    ▼
         Génération d'une étiquette Pharma-Smart
         ┌─────────────────────────────┐
         │  DOLIPRANE 500 mg           │
         │  [DataMatrix GS1]           │  ← encode EAN local + lot + péremption
         │  EAN: 3400933399934         │
         │  Lot: ABC101  Exp: 01/2025  │
         │  Prix: 1 500 CFA            │
         └─────────────────────────────┘
                    │
                    ▼
         Étiquette collée sur la boîte
         → tous les scans suivants (vente, inventaire) lisent le DataMatrix Pharma-Smart
```

### 8.2 Format recommandé : GS1 DataMatrix interne

Plutôt qu'un QR Code propriétaire (lisible uniquement par l'appli), utiliser
le standard GS1 DataMatrix avec les AI standards. Ainsi, n'importe quel scanner
GS1 peut lire l'étiquette — y compris les futures intégrations partenaires.

**Contenu encodé :**

```
FNC1  AI 01  GTIN (00 + EAN-13 du catalogue, en GTIN-14)
      AI 10  numéro de lot (si disponible)
      AI 17  date de péremption YYMMDD (si disponible)
```

**Exemple :**
```
Code EAN catalogue   : 3400933399934
Lot                  : ABC101
Péremption           : 2025-01-31

→ Chaîne GS1 encodée : (01)03400933399934(10)ABC101(17)250131
```

> Le FNC1 (Function 1) est inséré automatiquement par ZXing via `EncodeHintType.GS1_FORMAT = true`.
> Le `\u001D` (ASCII 29, Group Separator) sépare les champs de longueur variable.

### 8.3 Implémentation Java — `EtiquetteDataMatrixService`

Le projet dispose déjà de **ZXing** (`com.google.zxing`). Aucune dépendance à ajouter.

```java
// service/pdf/EtiquetteDataMatrixService.java
@Service
public class EtiquetteDataMatrixService {

    // ZXing insère le FNC1 automatiquement via GS1_FORMAT = true — ne pas l'ajouter manuellement
    private static final char GS = '\u001D';

    /**
     * Génère un DataMatrix GS1 en PNG (base64) pour une étiquette lot.
     */
    public String generateGs1DataMatrixBase64(String ean13, String numLot, LocalDate expiry)
            throws WriterException {

        String content = buildGs1Content(ean13, numLot, expiry);

        // Hints ZXing pour forcer GS1 et DataMatrix
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.DATA_MATRIX_SHAPE, SymbolShapeHint.FORCE_SQUARE);
        hints.put(EncodeHintType.GS1_FORMAT, true);
        hints.put(EncodeHintType.CHARACTER_SET, "ISO-8859-1");  // requis pour GS

        BitMatrix matrix = new DataMatrixWriter().encode(
            content,
            BarcodeFormat.DATA_MATRIX,
            120, 120,   // taille en pixels
            hints
        );

        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /**
     * Même chose mais en QR Code (alternative lisible par téléphone).
     */
    public String generateQrCodeBase64(String ean13, String numLot, LocalDate expiry)
            throws WriterException {

        // Format interne simple, lisible par l'application
        String content = "EAN:" + ean13
            + (numLot != null ? "|LOT:" + numLot : "")
            + (expiry != null ? "|EXP:" + expiry.format(DateTimeFormatter.ofPattern("ddMMyyyy")) : "");

        BitMatrix matrix = new QRCodeWriter().encode(
            content, BarcodeFormat.QR_CODE, 120, 120
        );
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private String buildGs1Content(String ean13, String numLot, LocalDate expiry) {
        StringBuilder sb = new StringBuilder();

        // AI 01 — GTIN-14 = "0" + EAN-13
        // ZXing ajoute le FNC1 automatiquement (GS1_FORMAT = true) — ne pas l'ajouter ici
        sb.append("01").append("0").append(ean13);

        // AI 10 — lot (champ variable → GS séparateur si suivi d'un autre AI)
        if (numLot != null && !numLot.isBlank()) {
            sb.append("10").append(numLot);
            if (expiry != null) sb.append(GS);  // séparateur avant AI 17
        }

        // AI 17 — péremption YYMMDD
        if (expiry != null) {
            sb.append("17")
              .append(String.format("%02d%02d%02d",
                  expiry.getYear() % 100,
                  expiry.getMonthValue(),
                  expiry.getDayOfMonth()));
        }

        return sb.toString();
    }
}
```

### 8.4 Extension de `EtiquetteDTO` pour les lots

```java
// Ajouter dans EtiquetteDTO :
private String numLot;
private LocalDate expiryDate;
private String dataMatrixBase64;   // image PNG générée par EtiquetteDataMatrixService
private boolean hasLotInfo;
```

### 8.5 Quand proposer la génération d'étiquette

| Déclencheur | Comportement |
|-------------|--------------|
| Réception avec DataMatrix non reconnu (résolution manuelle) | Proposer l'étiquette après confirmation du produit |
| Réception avec DataMatrix reconnu (alias ou direct) | Proposer l'étiquette automatiquement si lot + péremption présents |
| Fiche produit > onglet Lots | Imprimer des étiquettes pour un lot existant |
| Inventaire > correction de stock | Imprimer pour les produits réétiquetés |

### 8.6 Comparaison GS1 DataMatrix vs QR Code pour ce cas d'usage

| Critère | GS1 DataMatrix | QR Code Pharma-Smart |
|---------|---------------|----------------------|
| Lu par n'importe quel scanner professionnel | ✅ Oui | ❌ Seulement si l'appli connaît le format |
| Lu par un smartphone | ⚠ Nécessite une appli spécialisée | ✅ Appli photo native |
| Taille d'impression minimale | 6×6 mm | 10×10 mm |
| Densité de données | Haute | Haute |
| Compatible future intégration (grossistes, CNAM CI) | ✅ Standard mondial | ❌ Propriétaire |
| Complexité d'implémentation | Moyenne | Faible |

**Recommandation** : GS1 DataMatrix pour les étiquettes professionnelles (réception, rayon),
QR Code en option sur l'interface utilisateur (scan smartphone pour consultation stock).

---

## 9. Le standard DataMatrix est-il le même dans tous les pays ?

### 9.1 Ce qui est universel

Le format **ECC200 DataMatrix** (la géométrie du code 2D) et l'encodage **GS1** (les
Application Identifiers) sont définis par des standards internationaux (ISO/IEC 16022
pour le symbole, GS1 General Specifications pour les AI). Ce socle est identique partout.

### 9.2 Ce qui varie selon les marchés

| Élément | France / EU | USA | Japon / Corée | Afrique CI |
|---------|-------------|-----|---------------|------------|
| Symbole 2D utilisé | DataMatrix | DataMatrix ou QR | QR Code majoritaire | DataMatrix (si présent) |
| AI 01 (GTIN) | ✅ Toujours | ✅ Toujours | ✅ Toujours | ✅ Si GS1 respecté |
| AI 10 (lot) | ✅ Requis FMD | ✅ FDA 2026 | ✅ | Rare |
| AI 17 (péremption) | ✅ Requis FMD | ✅ | ✅ | Parfois |
| AI 21 (n° de série) | **Obligatoire FMD 2019** | Obligatoire FDA 2026 | Non | Non |
| AI 711 (CIP France) | ✅ Présent | ❌ Absent | ❌ Absent | ❌ Absent |
| AI 710 (PZN Allemagne) | ❌ France seulement | ❌ | ❌ | ❌ |
| AI 714 (Portugal) | ❌ | ❌ | ❌ | ❌ |

### 9.3 Le même produit, des DataMatrix différents selon le marché cible

Un médicament fabriqué en Inde peut avoir **deux DataMatrix différents** sur le même
emballage physique selon le marché de distribution :

```
Boîte pour la France :
  (01)03400933399934   ← GTIN avec préfixe GS1 France (34...)
  (10)ABC101
  (17)250131
  (21)SER00042         ← numéro de série FMD obligatoire
  (711)3400933399934   ← CIP-13 France

Même médicament pour la Côte d'Ivoire :
  (01)06111234567890   ← GTIN différent (préfixe GS1 CI ou autre)
  (10)ABC101
  (17)250131
  → Pas de AI 21 (pas de FMD)
  → Pas de AI 711 (pas de CIP France)
  → Ou pas de DataMatrix du tout (juste un EAN-13 1D)
```

### 9.4 Variations pratiques rencontrées par les scanners

Pharma-Smart doit gérer ces variantes de la même manière (déjà en partie traité dans
`DataMatrixParserServiceImpl.normalizeCode()`) :

| Variante | Exemple | Origine |
|----------|---------|---------|
| Préfixe symbologie `]d2` | `]d201034009...` | Scanner Honeywell/Zebra DataMatrix |
| Préfixe symbologie `]C1` | `]C101034009...` | Scanner Code 128 GS1 (pas DataMatrix !) |
| FNC1 en début `è` | `è01034009...` | Certains scanners Datalogic |
| GS comme `<GS>` (texte) | `01034009...<GS>10ABC` | Imports via fichier CSV |
| GS comme `{GS}` | `01034009...{GS}10ABC` | Systèmes ERP tiers |
| Absence de FNC1 | `01034009...` | Manufacturiers qui omettent l'identifiant |
| Code 128 GS1 (1D !) | `\u001D01034009...` | Certains médicaments génériques Asia |

> `normalizeCode()` gère déjà `]d2`, `]C1`, `<GS>`, `{GS}`. Il manque le préfixe
> `è` (FNC1 en début) et le Code 128 GS1 (`\u001D` seul en début) — à ajouter.

### 9.5 Adaptation du `normalizeCode()` à compléter

```java
// DataMatrixParserServiceImpl.java — normalizeCode()
// Ajouter avant le Pattern GS1_PREFIX_PATTERN :

// FNC1 en début (certains scanners Datalogic, imprimantes Zebra)
if (normalized.charAt(0) == 'è') {
    normalized = normalized.substring(1);
}

// Code 128 GS1 identifié par \u001D seul en début (pas de préfixe ]C1)
// → traiter comme DataMatrix GS1, le contenu est identique
if (normalized.charAt(0) == '\u001D') {
    normalized = normalized.substring(1);
}
```

---

## 10. Lire tout type de DataMatrix quelle que soit sa provenance

### 10.1 Les deux couches du problème

```
Boîte physique
    │
    ▼  COUCHE 1 — Optique (hardware scanner ou ZXing caméra)
                  Convertit l'image 2D en chaîne de caractères brute
                  → Problème : chaque scanner sort la chaîne différemment
    │
    ▼  COUCHE 2 — Parsing logiciel (DataMatrixParserService)
                  Convertit la chaîne brute en DataMatrixInfo structuré
                  → Problème : le contenu varie selon le marché source
```

Il faut robustifier **les deux couches** indépendamment.

---

### 10.2 Couche 1 — Optique : configurer le scanner matériel

Un scanner USB (Honeywell, Zebra, Datalogic, Opticon…) décode le symbole 2D en interne
et envoie du texte au PC via USB HID (comme un clavier). Ce qui arrive dans le champ de
saisie dépend de sa configuration — configurer correctement le scanner est la manœuvre
la plus rentable.

**Paramètres à activer sur le scanner :**

| Paramètre | Valeur à configurer | Effet |
|-----------|---------------------|-------|
| AIM symbology identifier | Activé | Préfixe `]d2` (DataMatrix), `]C1` (Code128 GS1), `]Q3` (QR) → le parser sait d'où vient le code |
| GS character output | `<GS>` ou `{GS}` ou `\x1D` | Rend le séparateur détectable dans n'importe quel champ texte |
| DataMatrix ECC200 | Activé | Active la lecture des DataMatrix modernes (certains scanners désactivent les formats 2D par défaut) |
| Code 128 GS1-128 | Activé | Lit les codes GS1 linéaires (1D) qui ont le même contenu |
| QR Code | Activé | Certains fabricants asiatiques utilisent QR au lieu de DataMatrix |

**Procédure** : la plupart des scanners se configurent via un carnet de codes-barres de
configuration fourni dans leur documentation (guide Honeywell Voyager, Zebra DS2208…).
Une fois configuré, le scanner sort **toujours** `]d2{contenu}<GS>{séparateurs}`,
ce qui rend le parsing côté logiciel trivial.

> **Tauri + scanner USB** : le texte arrive via l'événement keyboard du WebView.
> Il faut une durée inter-touches < 50 ms (scan rapide) pour distinguer un scan
> d'une saisie humaine. Un `input[data-scan]` avec un timer de 100 ms est le pattern
> habituel pour détecter les scans vs les frappes manuelles.

---

### 10.3 Couche 2 — Parser logiciel robuste : pipeline multi-stratégie

La stratégie actuelle (`isValidDataMatrix` → `parseDataMatrix`) est fragile :
elle ne reconnaît que les strings commençant par un AI connu. Si le contenu
commence par autre chose (lot en premier, date en premier, format propriétaire),
le parser retourne `UNKNOWN`.

**Pipeline recommandé :**

```
Chaîne brute reçue du scanner
        │
        ▼  Étape 1 : normalisation universelle
           → Strip tous les préfixes connus
           → Tous les séparateurs → \u001D
        │
        ▼  Étape 2 : détection du type
           → Contient-il \u001D ?  →  multi-AI GS1
           → Long de 8/13/14 chiffres ?  →  EAN/GTIN simple
           → Contient-il un pattern de date YYMMDD ?  →  heuristique lot+date
           → Autre → raw code (recherche wildcard)
        │
        ├──── GS1 multi-AI  →  parser AI existant (amélioré)
        │
        ├──── EAN / GTIN    →  parseEan8 / parseEan13 / parseCip13
        │
        ├──── Heuristique   →  extracteur par patterns (section 10.4)
        │
        └──── Raw code      →  DataMatrixInfo avec getProductCode() = rawCode brut
                               → appelant fait une recherche wildcard en base
```

---

### 10.4 `normalizeCode()` — version renforcée

```java
private static final Pattern GS1_PREFIX_PATTERN =
    Pattern.compile("^(\\]d2|\\]d1|\\]C1|\\]Q3|\\]e0|\\x1D)?(.*)$", Pattern.DOTALL);

private String normalizeCode(String code) {
    String s = code.trim();

    // 1. FNC1 en début (è = Datalogic, \u0004 = certains Symbol/Motorola)
    while (!s.isEmpty() && (s.charAt(0) == 'è' || s.charAt(0) == '\u0004')) {
        s = s.substring(1);
    }

    // 2. Préfixes symbologie AIM (]d2=DataMatrix, ]C1=Code128 GS1, ]Q3=QR GS1…)
    Matcher m = GS1_PREFIX_PATTERN.matcher(s);
    if (m.matches()) s = m.group(2);

    // 3. Séparateurs GS en toutes représentations → \u001D
    s = s.replace("<GS>",  GS_STRING)
         .replace("{GS}",  GS_STRING)
         .replace("^029",  GS_STRING)   // Honeywell decimal escape
         .replace("%1D",   GS_STRING)   // URL-encoded
         .replace("\\x1D", GS_STRING);  // échappement texte

    // 4. \u001D seul en tout début (Code 128 GS1 sans préfixe AIM)
    if (!s.isEmpty() && s.charAt(0) == GS_CHAR) {
        s = s.substring(1);
    }

    return s;
}
```

---

### 10.5 Détecteur de type — version renforcée

```java
@Override
public BarcodeType detectBarcodeType(String code) {
    if (!StringUtils.hasText(code)) return BarcodeType.UNKNOWN;

    String n = normalizeCode(code.trim());

    // Multi-AI GS1 : contient GS ou commence par un AI connu
    if (n.contains(GS_STRING) || isGs1Start(n)) return BarcodeType.DATAMATRIX;

    // Codes numériques purs
    if (n.matches("\\d+")) {
        return switch (n.length()) {
            case 7  -> BarcodeType.CIP_7;
            case 8  -> isValidEan8(n)  ? BarcodeType.EAN_8  : BarcodeType.UNKNOWN;
            case 13 -> n.startsWith("340") ? BarcodeType.CIP_13
                       : isValidEan13(n)   ? BarcodeType.EAN_13 : BarcodeType.UNKNOWN;
            // 14 chiffres : GTIN-14 (issu d'un DataMatrix, AI 01 sans en-tête GS1)
            // ou ITF-14 (code 1D de carton). Sans préfixe AIM la distinction est impossible ;
            // on suppose GTIN-14 car l'ITF-14 est rare au comptoir pharmaceutique.
            case 14 -> BarcodeType.DATAMATRIX;
            default -> BarcodeType.UNKNOWN;
        };
    }

    // Contient un pattern de date YYMMDD → probablement un DataMatrix heuristique
    if (YYMMDD_PATTERN.matcher(n).find()) return BarcodeType.DATAMATRIX;

    return BarcodeType.UNKNOWN;
}

// Vérifie si la chaîne commence par un AI GS1 connu (2 ou 3 chiffres)
private boolean isGs1Start(String n) {
    return n.startsWith("01") || n.startsWith("10") || n.startsWith("17") ||
           n.startsWith("11") || n.startsWith("21") || n.startsWith("37") ||
           n.startsWith("710") || n.startsWith("711") || n.startsWith("712") ||
           n.startsWith("713") || n.startsWith("714");
}

private static final Pattern YYMMDD_PATTERN =
    Pattern.compile("(?:^|\\D)(\\d{2}(?:0[1-9]|1[0-2])(?:0[0-9]|[12]\\d|3[01]))(?:\\D|$)");
```

---

### 10.6 Parser heuristique — quand le format GS1 est absent

Certains DataMatrix de fabricants asiatiques ou africains encodent les données
**sans AI GS1**, dans un format texte libre ou semi-structuré. Exemples réels :

```
"LOT:ABC123/EXP:20250131/EAN:3400933399934"
"ABC123|250131|3400933399934"
"3400933399934 ABC123 250131"
```

Un parser heuristique extrait ces données par reconnaissance de pattern :

```java
private Optional<DataMatrixInfo> parseHeuristic(String code) {
    DataMatrixInfo.Builder b = DataMatrixInfo.builder();
    boolean found = false;

    // EAN-13 / CIP-13 : 13 chiffres consécutifs
    Matcher ean = Pattern.compile("(?<![\\d])(\\d{13})(?![\\d])").matcher(code);
    if (ean.find()) {
        String candidate = ean.group(1);
        if (isValidEan13(candidate)) { b.ean13(candidate); found = true; }
    }

    // GTIN-14 : 14 chiffres consécutifs
    Matcher gtin = Pattern.compile("(?<![\\d])(\\d{14})(?![\\d])").matcher(code);
    if (gtin.find()) { b.gtin(gtin.group(1)); found = true; }

    // Date YYMMDD (la plus récente dans le futur = péremption)
    Matcher dates = YYMMDD_PATTERN.matcher(code);
    LocalDate bestExpiry = null;
    while (dates.find()) {
        LocalDate d = parseDateSafe(dates.group(1));
        if (d != null && d.isAfter(LocalDate.now())) {
            if (bestExpiry == null || d.isBefore(bestExpiry)) bestExpiry = d;
        }
    }
    if (bestExpiry != null) { b.expiryDate(bestExpiry); found = true; }

    // Numéro de lot : séquence alphanumérique de 4–20 chars entre séparateurs
    Matcher lot = Pattern.compile(
        "(?:LOT[: ]?|\\bL[: ]?|batch[: ]?)([A-Z0-9][A-Z0-9\\-]{3,19})",
        Pattern.CASE_INSENSITIVE).matcher(code);
    if (lot.find()) { b.batchNumber(lot.group(1).toUpperCase()); found = true; }

    return found ? Optional.of(b.build()) : Optional.empty();
}
```

---

### 10.7 Résumé : ordre de fiabilité des approches

| # | Approche | Fiabilité | Applicable si |
|---|----------|-----------|---------------|
| 1 | **Scanner configuré** (AIM IDs + GS connu) | ★★★★★ | Matériel configurable (Honeywell, Zebra) |
| 2 | **GS1 multi-AI parser** (actuel, renforcé) | ★★★★☆ | GS1-compliant, toutes origines |
| 3 | **EAN/GTIN simple** | ★★★★☆ | Grossistes CI, 1D scanné par erreur |
| 4 | **Parser heuristique** | ★★★☆☆ | Fabricants asiatiques/africains non-GS1 |
| 5 | **Code brut + alias** | ★★☆☆☆ | Dernier recours, résolution manuelle |

**La règle des 3 couches** pour être universel :
1. Configurer le scanner → fiabilité au niveau matériel
2. `normalizeCode()` renforcé → tolérance aux variations de sortie scanner
3. Pipeline GS1 → heuristique → raw → robustesse aux formats non-standard
