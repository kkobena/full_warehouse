# Plan — Passer la relation Produit ↔ DCI de 1-n à n-n

> Statut : **lots 1 et 2 livrés** (29/09/2026) — décomposition (lot 3) et contraction (lot 4) à faire.
> Date : septembre 2026.
> Prérequis de : [PLAN-EXTRACTION-ORDONNANCE-OCR.md](PLAN-EXTRACTION-ORDONNANCE-OCR.md) (§6.1, table
> `produit_dci`) — le contrôle des interactions ne vaut que ce que vaut le lien produit → DCI.

---

## 1. Pourquoi

Aujourd'hui un produit porte **au plus une DCI** (`produit.dci_id`). Une association
(amoxicilline + acide clavulanique, paracétamol + codéine) ne peut donc pas être décrite par ses
molécules. Le référentiel a contourné la limite en créant des **DCI composées** — une seule ligne
`AMOXICILLINE/ACIDE CLAVULANIQU` — qui ne sont reliées à aucune des deux molécules.

Conséquences : une interaction portée par la codéine est invisible sur un produit rattaché à
« PARACETAMOL/CODEINE » ; un générique ne peut pas être reconnu molécule par molécule ; le filtre
« DCI = codéine » du catalogue ignore les associations.

---

## 2. L'existant (relevé le 28/09/2026)

### 2.1 Base de données

| Élément | État |
|---|---|
| `dci` | `id`, `code` (unique, 20 car.), `libelle` (unique), index sur `libelle`. Alimentée par `V1.9.4__nav_dci.sql` (~1 100 lignes). |
| `produit.dci_id` | FK nullable vers `dci`, **sans index**. |
| DCI composées | **437 sur 1 095** ont un `/` ou un `+` dans le libellé. |
| Libellés tronqués | **236** font exactement 30 caractères (export d'origine tronqué : « …ACIDE CLAVULANIQU »). |
| Jeu de démo | 603 produits, 490 avec une DCI, 59 DCI distinctes, **aucun sur une DCI composée**. Une base client réelle peut différer : à mesurer au lot 0. |
| `substitut` | Table **indépendante** de la DCI (génériques / substituts thérapeutiques), alimentée par PharmaML et par la fusion de produits. |

### 2.2 Backend

| Fichier | Usage de la DCI |
|---|---|
| `domain/Produit` | `@ManyToOne Dci dci` (LAZY), entité en cache `READ_WRITE`. |
| `domain/Dci`, `DciRepository` | Référentiel ; recherche code/libellé, unicité. |
| `ProduitRepository` | `countByDciId`, `findAllByDciIdForDetail` (JPQL `p.dci.id`). |
| `CustomizedProductRepository` | Recherche plein texte : jointure `Produit_.dci` sur le libellé (l.331) ; filtre `dciId` (l.445). |
| `ProduitCriteria` | `dciId`. |
| `ProduitDTO` | `dciId`, `dciLibelle`, `dciCode` (ce dernier n'est jamais renseigné). |
| `ProduitBuilder` | création : `setDci(dciFromId(dto.dciId))` ; produit déconditionné : copie la DCI du parent ; lecture : `updateDci`. |
| `ProduitServiceImpl.update` | **n'applique pas la DCI** — voir §2.4. |
| `DciServiceImpl` | CRUD, import CSV, `findProduits`, `rattacherProduits` (remplace la DCI des produits sélectionnés), suppression refusée si `countByDciId > 0`. |
| `DciResource` (`/api/dci`) | liste, détail, `/{id}/produits` (GET, POST rattachement), CRUD, `/importcsv`. |
| `ProduitResource` | paramètre de recherche `dciId`. |
| `ProduitMergeServiceImpl` | **ignore la DCI** lors d'une fusion de doublons. |

### 2.3 Front et clients

| Fichier | Usage |
|---|---|
| `shared/model/produit.model.ts` | `dciId`, `dciLibelle`, classe `Dci`. |
| `features/products/ui/produit-form` | `app-select-search` **simple** sur `dciId` (via `entities/dci/dci.service.ts`). |
| `features/products/feature/produit-home` | filtre catalogue par DCI ; bouton d'affectation en masse. |
| `features/products/ui/dci-assignation-modal` | rattache une DCI à une sélection de produits. |
| `features/products/ui/produit-synthese-tab` | affiche `dciLibelle`. |
| `features/products/ui/produit-generiques-tab` | message « l'équivalence se fonde sur la molécule (DCI) » — **inexact** : l'onglet lit `substitut`. |
| `features/dci/**` | écran DCI : liste, formulaire, import, panneau des produits rattachés (`features/dci/data-access/dci-api.service.ts`). |
| `entities/dci/dci.service.ts` | ancien service, encore utilisé par la fiche produit, le catalogue et la modale. |
| `mobile/` (Flutter) | n'utilise pas la DCI (le modèle déclare `dciLibelle`, sans usage). |
| e2e | REF-63 (affectation en masse), REF-12 (génériques), RFD-01, REF-11/14/43 (fiche). |
| `scripts/demo-data` | `02b_dci.sql`, `03_produits.sql` (écrit `dci_id`), `03b_substituts.sql`, `99_verification.sql`. |

### 2.4 Défauts relevés au passage

1. **La DCI modifiée dans la fiche produit n'est pas enregistrée.** `ProduitBuilder.buildProduitFromProduitDTO`
   → `applyCommonFields` met à jour gamme et forme, pas la DCI ; seule la création la positionne.
   À corriger au lot 1 : c'est précisément le chemin que la saisie multi-DCI empruntera.
2. **`dciCode` n'est jamais renseigné** dans `ProduitDTO` (`updateDci` ne pose que l'id et le libellé).
3. **La fusion de produits perd la DCI du produit absorbé.**
4. **Le message de l'onglet Génériques** attribue à la DCI ce qui vient de la table `substitut`.

---

## 3. Cible

### 3.1 Modèle

```
dci (inchangée, + colonne statut)          produit_dci                         produit
─────────────────────────────              ─────────────────────               ─────────
id, code, libelle                  1 ── n  produit_id  (PK, FK cascade)  n ── 1  id
statut: ACTIVE | COMPOSEE | ARCHIVEE       dci_id      (PK, FK)                  dci_id (transitoire)
                                           rang        smallint (1 = principale)
                                           dosage_valeur numeric  (nullable)
                                           dosage_unite  varchar(10) (nullable)
```

- **`rang`** : ordre d'affichage et molécule **principale** (`rang = 1`), unique par produit.
- **`dosage_*`** : facultatif dès maintenant pour ne pas remigrer plus tard — il servira à
  l'équivalence générique et au contrôle de surdosage. Non saisi au premier jalon.
- **`dci.statut`** : `COMPOSEE` marque les anciennes DCI « A/B » ; elles restent en base pour
  l'historique, disparaissent des sélecteurs une fois décomposées (§5).

### 3.2 Entité JPA

Une **entité d'association** `ProduitDci` (clé composite `produit_id, dci_id` + attributs), et non
un `@ManyToMany` nu : `rang` et `dosage` sont des attributs du lien.

```java
// Produit
@OneToMany(mappedBy = "produit", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderBy("rang")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
private List<ProduitDci> produitDcis = new ArrayList<>();
```

`Produit.dci` reste mappé pendant la transition (§4), **en lecture seule** vis-à-vis du métier :
il n'est plus écrit que par la synchronisation.

### 3.3 API

`ProduitDTO` gagne `dcis: List<ProduitDciDTO>` (`dciId`, `code`, `libelle`, `rang`, dosage).
Pendant la transition, les anciens champs restent **calculés** :

| Champ historique | Valeur en n-n |
|---|---|
| `dciId` | DCI principale (`rang = 1`) |
| `dciLibelle` | libellés joints par « + », dans l'ordre des rangs |
| `dciCode` | code de la principale (corrige le défaut §2.4-2) |

En écriture : si `dcis` est fourni, il fait foi ; sinon `dciId` seul est accepté comme « une seule
molécule, principale ». Un client ancien (exe Tauri non mis à jour, application mobile) continue
donc de fonctionner.

---

## 4. Stratégie de migration : étendre, basculer, contracter

On ne supprime rien tant que tout ne lit pas la nouvelle table.

| Phase | Base | Code | Clients |
|---|---|---|---|
| **1. Étendre** | `produit_dci` créée et remplie depuis `dci_id` ; `dci_id` conservée. | Lecture **et** écriture sur `produit_dci` ; `dci_id` recopiée depuis la principale à chaque écriture. | inchangés |
| **2. Basculer** | — | Toutes les requêtes lisent `produit_dci`. | Front multi-DCI ; mobile inchangé (lit `dciLibelle`). |
| **3. Contracter** | `produit.dci_id` supprimée (migration dédiée, après une version complète sans lecture). | `Produit.dci` et champs DTO historiques retirés. | Mobile et exe à jour. |

### 4.1 Migration de la phase 1

```sql
-- V2.x.y__produit_dci.sql (prochaine version libre ; au 28/09 la dernière est V2.1.9)
CREATE TABLE produit_dci (
    produit_id     integer      NOT NULL REFERENCES produit (id) ON DELETE CASCADE,
    dci_id         integer      NOT NULL REFERENCES dci (id),
    rang           smallint     NOT NULL DEFAULT 1 CHECK (rang >= 1),
    dosage_valeur  numeric(12, 4),
    dosage_unite   varchar(10),
    PRIMARY KEY (produit_id, dci_id),
    CONSTRAINT produit_dci_rang_uk UNIQUE (produit_id, rang)
);
CREATE INDEX produit_dci_dci_idx ON produit_dci (dci_id);

INSERT INTO produit_dci (produit_id, dci_id, rang)
SELECT id, dci_id, 1 FROM produit WHERE dci_id IS NOT NULL;

ALTER TABLE dci ADD COLUMN statut varchar(10) NOT NULL DEFAULT 'ACTIVE'
    CONSTRAINT dci_statut_check CHECK (statut IN ('ACTIVE', 'COMPOSEE', 'ARCHIVEE'));
UPDATE dci SET statut = 'COMPOSEE' WHERE libelle ~ '[/+]';
```

Pas de nom de schéma en dur (`search_path` Flyway). La migration ne **décompose pas** les DCI
composées : c'est une opération relue par un humain (§5), pas une transformation aveugle au
démarrage d'une officine.

### 4.2 Synchronisation `dci_id` pendant la transition

Faite **en Java**, au même endroit que l'écriture de `produit_dci` (méthode unique
`Produit.remplacerDcis(List<ProduitDci>)` qui repositionne aussi `dci`). Pas de trigger : la règle
reste lisible dans le code et testable sans base.

---

## 5. Décomposer les DCI composées

C'est le **vrai travail de données** du chantier, et il ne peut pas être entièrement automatique :
236 libellés sont tronqués.

### 5.1 Algorithme de proposition

Pour chaque DCI `COMPOSEE` :

1. Découper le libellé sur `/` et `+`, nettoyer (espaces multiples, `unaccent`, majuscules).
2. Pour chaque fragment, chercher une DCI **simple** (`ACTIVE`, sans `/` ni `+`) :
   - égalité exacte normalisée → proposée avec confiance « exacte » ;
   - sinon similarité `pg_trgm` ≥ seuil (0,6 à régler) → proposée « probable » (cas du fragment
     tronqué : « ACIDE CLAVULANIQU » → « ACIDE CLAVULANIQUE ») ;
   - sinon → « à créer », avec le libellé du fragment pré-rempli.
3. Les abréviations fréquentes (« VIT » → « VITAMINE », « MAGNES » → « MAGNESIUM ») passent par une
   **table de correspondance** paramétrable, pas par des règles cachées dans le code.

### 5.2 Validation et application

- Écran « Associations à décomposer » dans `features/dci` : une ligne par DCI composée, ses
  fragments, la molécule proposée (modifiable), le nombre de produits concernés.
- **Appliquer** une ligne : crée les molécules manquantes, remplace, pour chaque produit rattaché à
  la DCI composée, la ligne `produit_dci` par une ligne par molécule (rangs dans l'ordre du libellé),
  puis passe la DCI composée en `ARCHIVEE`. Journalisé (`logs`), rejouable ligne par ligne.
- Une DCI composée **non utilisée** par un produit est simplement archivée.
- Tant qu'une DCI composée n'est pas décomposée, elle fonctionne comme aujourd'hui (une ligne
  `produit_dci`) : rien n'est cassé en attendant.

---

## 6. Backend — changements détaillés

| Élément | Changement |
|---|---|
| `ProduitDci` (+ id composite) | nouvelle entité, `ProduitDciRepository`. |
| `Produit` | collection `produitDcis` ; `remplacerDcis(...)` ; `dci` conservé en transition. |
| `ProduitBuilder` | création : `dcis` (ou `dciId` seul) ; déconditionné : copie **la liste** du parent ; lecture : remplit `dcis` + champs calculés. **Mise à jour : applique enfin les DCI** (défaut §2.4-1). |
| `CustomizedProductRepository` | recherche texte et filtre `dciId` via **sous-requête `EXISTS`** sur `produit_dci` — une jointure dupliquerait les lignes d'un produit à deux molécules et fausserait la pagination. |
| `ProduitRepository` | `countByDciId` et `findAllByDciIdForDetail` réécrits sur `produit_dci`. |
| `DciServiceImpl.rattacherProduits` | nouveau paramètre **mode** : `AJOUTER` (défaut : la molécule s'ajoute en fin de rang, sans doublon) ou `REMPLACER` (comportement actuel). |
| `DciServiceImpl.delete` | refus si la DCI est portée par un produit (`produit_dci`). |
| `DciServiceImpl.findAll` | exclut `ARCHIVEE` par défaut ; `COMPOSEE` affichée avec un marqueur. |
| `ProduitMergeServiceImpl` | union des DCI des produits fusionnés, rangs du produit conservé d'abord (défaut §2.4-3). |
| `DciDecompositionService` + endpoints `/api/dci/decomposition` | propositions (§5.1), application (§5.2). |
| Cache Hibernate | région de la nouvelle collection ; invalidation `Produit` inchangée. |

`search_produits_json` (recherche au comptoir) ne lit pas la DCI : non concernée.

---

## 7. Front — changements détaillés

| Élément | Changement |
|---|---|
| `produit.model.ts` | `dcis?: IProduitDci[]` ; `dciId` / `dciLibelle` conservés (calculés serveur). |
| Fiche produit (`produit-form`) | `app-select-search` simple → **`app-multi-select`** (Design System), ordre = rang, première = principale (badge). Dosage non saisi au premier jalon. |
| Synthèse (`produit-synthese-tab`) | libellés joints « + », principale en premier. |
| Catalogue (`produit-home`) | filtre par DCI inchangé côté écran (le serveur répond par `EXISTS`). |
| Affectation en masse (`dci-assignation-modal`) | choix **« Ajouter cette molécule »** (défaut) / **« Remplacer les molécules »**, avec le nombre de produits qui en avaient déjà. |
| Écran DCI (`features/dci`) | marqueur « association » sur les `COMPOSEE` ; nouvel onglet « Associations à décomposer » (§5.2) ; panneau produits inchangé. |
| Onglet Génériques | corriger le message (défaut §2.4-4). |
| Services | à l'occasion, fiche/catalogue/modale passent de `entities/dci/dci.service.ts` à `features/dci/data-access/dci-api.service.ts` ; l'ancien service est retiré une fois sans appelant. |
| Mobile Flutter | **aucun changement** : n'utilise pas la DCI. |

Conventions du projet : composants standalone, signaux, `@if`/`@for`, composants de
`app/shared/ui` (pas d'équivalent natif).

---

## 8. Jeu de démonstration et cahier de recette

- `03_produits.sql` : écrire aussi `produit_dci` (et au moins **deux associations** réelles, pour que
  les parcours aient de quoi montrer) ; `99_verification.sql` : cohérence `dci_id` ↔ rang 1 pendant
  la transition ; `00_reset.sql` : ajouter `produit_dci`.
- Cahier de recette : mettre à jour la fiche produit, l'affectation en masse (REF-63), l'écran DCI ;
  nouveau scénario « Décomposer une association ».

---

## 9. Tests

| Niveau | Contenu |
|---|---|
| Unitaires | `ProduitBuilder` (création, déconditionné, **mise à jour**, champs calculés) ; `remplacerDcis` (rangs, doublons, synchro `dci`) ; proposition de décomposition (exacte, trigram, à créer, abréviations) ; `rattacherProduits` en `AJOUTER` / `REMPLACER`. |
| Intégration PostgreSQL | migration de phase 1 (backfill, contraintes, statut `COMPOSEE`) ; recherche catalogue sans doublon de pagination ; `countByDciId` ; application d'une décomposition ; fusion de produits. |
| Jest | fiche produit multi-DCI ; modale d'affectation (modes) ; écran de décomposition. |
| e2e | REF-63, REF-12, RFD-01, fiche produit ; nouveau parcours de décomposition. |

---

## 10. Lots

| Lot | Contenu | Effort indicatif |
|---|---|---|
| **0 — Mesure** | Sur une base client réelle : produits sans DCI, produits sur DCI composée, DCI composées réellement utilisées. Décide de l'urgence du lot 3. | 0,5 j |
| **1 — Étendre** | Migration phase 1, entité `ProduitDci`, écriture double, lecture n-n, API `dcis` + champs calculés, correctifs §2.4-1/2/3, tests. **Aucun changement visible.** | 4–5 j |
| **2 — Écrans** | Fiche multi-DCI, synthèse, affectation en masse (modes), message Génériques, Jest, e2e, démo. | 3–4 j |
| **3 — Décomposition** | Service de proposition, écran de validation, application, table d'abréviations, tests. | 4–5 j |
| **4 — Contracter** | Après une version complète sans lecture de `dci_id` et mise à jour du mobile : suppression de la colonne et des champs historiques. | 1 j |

Le lot 1 est livrable seul et sans risque visible. Le lot 3 peut attendre si le lot 0 montre que les
associations sont peu utilisées sur le parc.

---

## 11. Risques

| Risque | Parade |
|---|---|
| Pagination faussée par la jointure n-n | `EXISTS` partout où l'on filtre ; test d'intégration dédié. |
| Client ancien qui envoie `dciId` seul et efface les autres molécules | Règle d'écriture §3.3 : `dciId` seul **ne remplace** la liste que si le produit n'a qu'une molécule ; sinon il ne touche qu'à la principale. À couvrir par un test. |
| Décomposition erronée (fragment tronqué mal rapproché) | Jamais automatique : proposée, relue, appliquée ligne par ligne, journalisée. |
| Unicité de `dci.libelle` bloquant la création d'une molécule déjà présente sous une autre graphie | La proposition cherche d'abord l'existant (exact puis trigram) ; la création passe par `DciServiceImpl.create` et ses contrôles. |
| Oubli de la synchronisation `dci_id` | Une seule méthode d'écriture (`remplacerDcis`) ; vérification dans `99_verification.sql`. |

---

## 12. Ce qui a été fait (lots 1 et 2)

**Base** — `V2.1.11__produit_dci.sql` : table `produit_dci` (clé technique, rang, dosage facultatif,
unicités `(produit, dci)` et `(produit, rang)` **différées**), reprise des `dci_id` existants au
rang 1, colonne `dci.statut` (437 DCI « A/B » ou « A+B » marquées `COMPOSEE` sur la base de démo).
`produit.dci_id` est conservée, alignée sur la principale.

**Backend**
- `ProduitDci` (entité), `Produit.produitDcis` (`@OrderBy("rang")`, `@BatchSize`), méthode d'écriture
  unique `Produit.remplacerDcis` — met à jour les liens conservés sur place (d'où les contraintes
  différées : Hibernate insère avant de supprimer).
- `ProduitDTO.dcis` ; `dciId`, `dciCode`, `dciLibelle` calculés (principale ; libellés joints « + »).
  Règle d'écriture §3.3 dans `ProduitBuilder.appliquerDcis`.
- Catalogue (`CustomizedProductRepository`) : recherche texte et filtre par DCI via `produitDcis`
  (le `distinct` / `countDistinct` existant évite les doublons) ; `countByDciId` et
  `findAllByDciIdForDetail` sur `ProduitDci`.
- Affectation en masse : paramètre `mode` (`AJOUTER` par défaut, `REMPLACER`).
- Défauts §2.4 corrigés : DCI enfin enregistrée à la **modification** de la fiche ; `dciCode`
  renseigné ; la **fusion** ajoute les molécules du produit absorbé ; message de l'onglet Génériques.
- `DciDTO.statut`.

**Front** — fiche produit en `app-multi-select` (ordre de choix = rang) ; modale d'affectation avec
« Ajouter cette molécule » / « Remplacer les molécules » ; badge « Association » sur les DCI
composées ; message de l'onglet Génériques corrigé.

**Jeu de démo** — `02b_dci.sql` ajoute les molécules manquantes ; `03_produits.sql` remplit
`produit_dci` et décompose les deux associations de démo (8 produits à deux molécules) ;
`99_verification.sql` contrôle l'alignement `dci_id` ↔ principale. Rejoué en entier sur une base
jetable : tous les contrôles au vert.

**Cahier de recette et e2e** — REF-63 (mode ajouter / remplacer, remise en état sur la liste
complète des molécules) ; REF-12 corrigé (les génériques viennent de `substitut`).

**Tests** — `ProduitBuilderDciTest` (11), `DciProduitIntegrationTest` (5, PostgreSQL), cas ajoutés à
`ProduitServiceIntegrationTest` (5) et `ProduitMergeServiceIntegrationTest` (1), Jest de la modale (4).
Paquets `service.stock` et `service.dci` : 1 026 tests au vert. REF-63 non rejoué (application arrêtée).

