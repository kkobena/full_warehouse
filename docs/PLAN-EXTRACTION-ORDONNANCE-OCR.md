# Plan — Lecture, suivi et contrôle des ordonnances (OCR, sans IA)

> Statut : **analyse révisée** — aucune ligne de code de l'ordonnance écrite. Les prérequis de données
> (produit ↔ DCI n-n, référentiel BDPM chargé par Flyway) sont, eux, livrés.
> Date : 4 octobre 2026 (première version : 28 septembre 2026).
> Plan parent : [PLAN-INTEGRATION-SPRING-AI.md](PLAN-INTEGRATION-SPRING-AI.md), étape 5 (UC-5).
> Plans liés : [PLAN-PRODUIT-DCI-N-N.md](PLAN-PRODUIT-DCI-N-N.md) ; référentiel :
> [scripts/referentiel-bdpm/README.md](../scripts/referentiel-bdpm/README.md).
> Portée : `pharmaSmart-domain`, `pharmaSmart-app`, écran de vente, installeur du poste serveur.

---

## 1. La question posée, et la réponse

> Peut-on répondre au besoin — lire une ordonnance, la suivre, détecter contre-indications et
> incompatibilités au regard des habitudes d'achat du client — avec **PostgreSQL, Apache Tika et
> Tess4J seulement** ?

**Oui.** Et pour la partie contrôle, **c'est même la seule réponse acceptable** : l'IA reste hors
de ce besoin.

| Besoin | Réponse sans IA | Pourquoi pas un LLM |
|---|---|---|
| Lire une ordonnance **imprimée** ou un PDF | Tika (PDF numérique) + Tess4J (image) + règles | L'OCR fait la lecture ; les lignes d'ordonnance sont assez régulières pour des règles. |
| Retrouver les produits du catalogue | `pg_trgm` + `unaccent` sur le catalogue **et sur le référentiel BDPM** (§4.3) | Un LLM peut inventer un produit ou un dosage absent du catalogue. |
| Suivre l'ordonnance (délivrances, renouvellements) | Tables relationnelles | C'est de la gestion, pas de la compréhension. |
| **Interactions, redondances, contre-indications** | **Référentiel sourcé + requêtes SQL** | Une alerte de sécurité doit être **déterministe, sourcée et reproductible**. Un LLM ne l'est pas, et sa réponse n'engagerait personne sauf le pharmacien. |

L'option IA (Spring AI, désactivée par défaut) n'est conservée que pour **un seul point
facultatif** : aider à structurer une ligne d'ordonnance que les règles n'ont pas comprise (§8).
Elle ne touche **jamais** au contrôle des interactions.

### Ce que l'OCR ne fera pas

Tesseract lit le **texte imprimé**, pas l'**écriture manuscrite**. Une ordonnance manuscrite est
détectée par sa confiance OCR basse et renvoyée à la saisie manuelle — le suivi et les contrôles,
eux, s'appliquent quelle que soit la façon dont l'ordonnance a été saisie. C'est le point clé :
**la valeur de sécurité ne dépend pas de l'OCR**.

---

## 2. Ce qui a changé depuis la première version

Le plan initial listait « Produit ↔ DCI : une seule DCI » et « référentiel : absent » comme les deux
grands manques. **Le premier est levé, le second l'est à moitié.**

| Élément | Septembre (28/09) | État au 04/10/2026 |
|---|---|---|
| Produit ↔ DCI | 1 DCI par produit (`produit.dci_id`) | **`produit_dci` livrée** (V2.1.11) : n molécules, `rang` (1 = principale), dosage facultatif ; `produit.dci_id` alignée sur le rang 1. Lots 3–4 du plan DCI n-n (décomposition des DCI composées, contraction) restent à faire. |
| Référentiel médicament | Absent | **Tables `ref_*` créées** (V2.1.19) **et chargées par Flyway** (V2.1.28 à V2.1.31), voir §2.1. |
| Lien produit ↔ spécialité | Absent | **`produit_ref_specialite`** (V2.1.20) : proposition par produit (`SUR`, `A_VERIFIER`, `PAR_DCI`, `NON_TROUVE`), décision `AUTO` / `VALIDE` / `REJETE` ; les `SUR` / `PAR_DCI` sont acceptés automatiquement et **donnent leurs molécules** à un produit sans DCI. Étape de nuit + job à la demande + événement à l'ajout de DCI. |
| Substituts | Table `substitut` seule | + **groupes génériques BDPM** (`v_ref_substitut`). |
| Interactions, contre-indications structurées | Absentes | **Toujours absentes** — c'est le point bloquant (§6.1). |
| Ordonnance, prescripteur | Absents | Absents. |
| Profil patient | Âge et sexe sur les assurés seulement | Inchangé. |
| Tika, Tess4J | Absents | Absents. |

### 2.1 Ce que contient le référentiel BDPM chargé

Source : base de données publique des médicaments (ANSM), réutilisation libre sous réserve de citer
la source et de ne pas altérer les données. Migrations livrées :

| Migration | Table(s) | Lignes |
|---|---|---|
| `V2.1.28` | `ref_groupe_generique`, `ref_substance`, `ref_dci` (+ `ref_lier_dci()`) | 1 528 · 3 907 · 3 180 |
| `V2.1.29` | `ref_specialite` | 15 883 |
| `V2.1.30` | `ref_specialite_composition` | 27 660 |
| `V2.1.31` | `ref_specialite_rcp` | 1 373 |

Mesures faites en rejouant les migrations sur une base jetable (copie du schéma du jeu e2e) :

- **3 556 spécialités ont 2 substances actives ou plus** : les associations sont donc **décomposables
  molécule par molécule** à partir de `ref_specialite_composition`, sans passer par les DCI composées
  du catalogue (437 sur 1 095, dont 236 libellés tronqués à 30 caractères).
- Chaque composition porte un **dosage canonique** (`dosage_canon`, `unite_canon` : 0,5 g = 500 mg)
  — la base d'un futur contrôle de surdosage (§6.2).
- **Liaison au catalogue** : sur le jeu e2e, `ref_lier_dci()` relie **368 DCI du référentiel sur
  3 180**, soit environ **un tiers des 1 097 DCI du catalogue**. Le reste est soit absent de la base
  française (marques ivoiriennes, indiennes), soit nommé différemment, soit ambigu. **C'est le
  chiffre à mesurer sur une base client réelle** (lot 0).
- **Le RCP est du texte libre** : 1 373 spécialités ont indications, posologie et contre-indications
  (1 359 avec contre-indications). Utilisable pour l'**affichage**, **pas pour déclencher une
  alerte** — aucune structure, aucun niveau de gravité.
- **Aucune interaction** : la BDPM ne publie pas le thésaurus des interactions. Le référentiel
  d'interactions reste à trouver (§6.1).

### 2.2 Limites héritées, à afficher et non à cacher

1. **Données françaises.** Environ **un quart** des médicaments d'un catalogue ivoirien existent en
   France (constat du README du référentiel). Les autres relèvent d'une saisie manuelle de la DCI
   ou d'une source locale.
2. **L'historique d'achat ne voit que les ventes rattachées à un client.** Une vente comptant
   anonyme n'entre pas dans les « traitements en cours ».
3. **Le contrôle ne vaut que ce que vaut le lien produit → molécules.** Un produit sans molécule
   est invisible pour le moteur : c'est un critère de go / no-go (§11).

---

## 3. Vue d'ensemble

```
 Capture (poste client)          Poste serveur                                   Poste client
 ───────────────────────         ────────────────────────────────────────────    ─────────────────────
 téléversement / scanner  ──►  Tika : PDF numérique ? ──oui──► texte             Écran « Ordonnance »
 photo mobile                        │non                                          image en regard
                                     ▼                                             lignes + candidats
                               Tess4J : préparation + OCR + confiance      ──►    validation ligne à ligne
                                     ▼                                                    │
                               Règles : découpage, anonymisation, champs                  ▼
                                     ▼                                           Ordonnance enregistrée
                               pg_trgm/unaccent : candidats                               │
                               (catalogue + ref_specialite)                               ▼
                                     ▼                                          produit → molécules
                               Moteur de contrôle (SQL) ◄─────  produit_dci / spécialité retenue
                                     ▲                                                    │
                                     └─────── panier + traitements en cours du client     ▼
                               Alertes graduées ──► pharmacien : prise en compte tracée
```

Tout le traitement tourne sur le **poste serveur** : les postes clients n'ont que l'exécutable
(cf. déploiement poste serveur / postes clients). L'image et le texte ne quittent jamais
l'officine.

---

## 4. Lecture de l'ordonnance (Tika + Tess4J + règles)

### 4.1 Rôles

- **Apache Tika** : détection du type réel du fichier et extraction du texte des documents
  **numériques** (PDF émis par une clinique). Modules minimaux : `tika-core` + module PDF + module
  image — **pas** `tika-parsers-standard-package`, qui tire des dizaines de dépendances. L'OCR
  interne de Tika (qui appelle `tesseract.exe`) n'est **pas** utilisé.
- **Tess4J** : OCR des images et des PDF scannés. Liaison JNA vers la bibliothèque native
  Tesseract, dont les DLL Windows sont fournies dans le jar — rien à installer sur le poste hors
  les fichiers de langue.

### 4.2 Étapes

1. **Préparation** (outils Leptonica fournis par Tess4J) : orientation EXIF, niveaux de gris,
   redressement, mise à l'échelle ~300 dpi.
2. **OCR** en `fra` (+ `eng` pour les DCI anglo-saxonnes), segmentation par blocs. On récupère le
   texte **et la confiance par ligne**.
3. **Tri** : confiance moyenne sous le seuil (paramétrable, ~60) → « manuscrit probable »,
   bascule vers la saisie manuelle avec l'image affichée.
4. **Découpage et anonymisation** : l'en-tête (patient, prescripteur, date, téléphone, adresse) est
   séparé des lignes de médicament. Les lignes de médicament seules alimentent la suite ; l'en-tête
   n'est **jamais** journalisé.
5. **Analyse des lignes par règles** :

| Champ | Motif indicatif |
|---|---|
| Dosage | `\d+([.,]\d+)?\s?(mg|g|µg|ml|UI|%)` — mêmes unités que `ref_specialite_composition.dosage_unite` |
| Forme | cp, comprimé, gél., gélule, sirop, sachet, suppo, pommade, inj… — liste alimentée par les `forme` distinctes de `ref_specialite`, plus des abréviations paramétrables |
| Posologie | `\d+\s?(cp|gél|cuill|sachet).*?(x|fois)\s?\d+\s?/\s?j` |
| Durée | `\d+\s?(j|jours|sem|mois)` |
| Quantité | `QSP`, `\d+\s?(boîte|bte)` |
| Libellé lu | le reste, normalisé par **`ref_normaliser`** (même fonction que le référentiel : sans accent, majuscules, ponctuation réduite) |

### 4.3 Rapprochement avec le catalogue — deux voies

Une ligne d'ordonnance nomme soit une **marque** (« DOLIPRANE 1000 mg »), soit une **molécule**
(« amoxicilline 500 mg gélule »). Le référentiel BDPM permet de traiter les deux :

1. **Voie catalogue (marque)** : `pg_trgm` sur le libellé lu, bonifié par le **dosage** et la
   **forme** reconnus.
2. **Voie molécule (DCI)** : la molécule lue est résolue dans `ref_dci` / `ref_substance`
   (`libelle_normalise`), le dosage comparé à `dosage_canon`, puis les **produits du catalogue**
   portant cette molécule sont proposés — par `produit_dci` si renseignée, sinon par leur
   spécialité retenue (`produit_ref_specialite` en décision `AUTO` ou `VALIDE` **seulement**) et
   la composition de celle-ci. Un générique répond ainsi pour un princeps (groupe générique).

Résultat : 3 à 5 candidats **réels**, chacun avec un score et la voie qui l'a produit. Aucun produit
n'est inventé ; aucun n'est choisi automatiquement. Une ligne sans candidat reste lisible et
saisissable à la main.

### 4.4 Contraintes d'exploitation

- **Charge du poste serveur** : il sert aussi les caisses. OCR dans un exécuteur dédié, 1 à 2
  traitements simultanés, délai maximal, une instance Tesseract par traitement (non partageable
  entre threads).
- **Java 25** : JNA déclenche l'avertissement « restricted methods » ; ajouter
  `--enable-native-access=ALL-UNNAMED` au lancement du jar et au service Windows.
- **Installeur** : embarquer `fra.traineddata` (et `eng`) — de quelques Mo à ~20 Mo selon la
  qualité retenue. Chemin paramétrable `pharma.ordonnance.ocr.tessdata-path`.
- **Taille déjà consommée par le référentiel** : environ 12 Mo de SQL dans le jar (V2.1.28–31) et
  ~21 Mo de tables `ref_*` à l'installation. À ajouter au bilan du lot 0.

---

## 5. Suivi d'ordonnance

### 5.1 Modèle

| Table | Contenu |
|---|---|
| `ordonnance` | client (obligatoire), prescripteur (texte libre au départ), date de prescription, source (`SCAN`, `PDF`, `MANUELLE`), statut (`EN_COURS`, `TERMINEE`, `EXPIREE`), renouvelable (nombre), date de fin de validité, `created_by`. |
| `ordonnance_ligne` | texte lu, confiance OCR, produit validé, posologie, durée (jours), quantité prescrite, quantité délivrée. **Pas de colonne DCI** : les molécules se déduisent du produit (`produit_dci`), elles ne sont pas dupliquées. |
| `ordonnance_delivrance` | lien ligne ↔ `sales_line` (clé composite `id, sale_date`), quantité, date. |
| `ordonnance_document` *(optionnel)* | image ou PDF, **uniquement si l'officine l'active**, avec durée de conservation. |

La vente garde sa logique actuelle : l'ordonnance **alimente** le panier, elle ne le remplace pas.
Une vente `PRESCRIPTION` peut être rattachée à une ordonnance.

### 5.2 Ce que le suivi apporte au comptoir

- **Délivrance partielle** : reste à délivrer par ligne, visible au retour du client.
- **Renouvellement** : ordonnances renouvelables arrivant à échéance, par client.
- **Fin de traitement** : date estimée = délivrance + (quantité ÷ posologie journalière), qui
  alimente aussi la notion de « traitement en cours » (§6.3).
- **Historique** : les ordonnances d'un client, consultables depuis sa fiche.
- **Substitution** : à la délivrance, les substituts du groupe générique (`v_ref_substitut`) sont
  déjà exposés au comptoir (`/api/referentiel-medicament/produits/{id}`) ; l'ordonnance les réutilise.

---

## 6. Contrôles de sécurité (sans IA)

### 6.1 Référentiel d'interactions — le seul vrai manque

`produit_dci` et `ref_*` fournissent la **couche « qui est quelle molécule »**. Il manque la couche
**« quelle molécule interagit avec quelle autre »**.

| Table | Contenu | Statut |
|---|---|---|
| `produit_dci` | N molécules par produit | **Livrée** (V2.1.11) |
| `ref_dci`, `ref_substance`, `ref_specialite_composition` | molécules et dosages des spécialités | **Livrées** (V2.1.28–31) |
| `classe_interaction` | classes du thésaurus (ex. « AINS », « anticoagulants oraux »). | À créer |
| `dci_classe` | appartenance d'une molécule à une ou plusieurs classes. | À créer |
| `interaction` | couple (molécule ou classe) × (molécule ou classe), **niveau** (`CI`, `AD`, `PE`, `APEC`), mécanisme, conduite à tenir, **source + version**. | À créer |
| `contre_indication` | molécule × critère patient (âge, sexe, grossesse, allaitement…), niveau, source. | À créer |

**Clé des tables d'interactions : `ref_dci.id`**, non la DCI du catalogue. La DCI du catalogue n'est
reliée qu'à un tiers du référentiel ; une clé stable côté référentiel, atteinte par **deux chemins**,
couvre bien davantage :

```
produit ──produit_dci──► dci ──ref_dci.dci_id──► ref_dci ◄── interaction / dci_classe
   │                                                ▲
   └─produit_ref_specialite (AUTO | VALIDE)──► ref_specialite_composition.dci_id
```

Le second chemin (spécialité retenue → composition) rend un produit visible au moteur **même sans
DCI saisie au catalogue**, et décompose les associations. Il faut cependant que le rapprochement
soit de confiance : une proposition `A_VERIFIER` ou `EN_ATTENTE` n'entre pas dans le contrôle.

**Source** : le *Thésaurus des interactions médicamenteuses* de l'ANSM, public, publié en PDF. Son
import est un **second usage de Tika** : extraction du PDF, analyse de sa structure (entrées « DCI +
DCI : niveau, conduite »), **relecture humaine** avant publication d'une version. **À valider avant
tout développement** : conditions de réutilisation, et pertinence pour la Côte d'Ivoire (un
référentiel national ou un fournisseur commercial — Vidal, Thériaque — peut s'imposer). Chaque alerte
affiche sa source et la version du référentiel.

**Apport possible du RCP, en affichage seulement** : la rubrique « interactions » du RCP (4.5) n'est
pas extraite aujourd'hui (le référentiel ne reprend que indications, posologie, contre-indications).
L'ajouter à `referentiel_bdpm.py` permettrait d'**afficher** le texte du RCP à côté d'une alerte.
Il ne la déclenche pas.

### 6.2 Contrôles

| Contrôle | Portée | Donnée nécessaire |
|---|---|---|
| **Interaction** entre lignes de l'ordonnance | ordonnance | molécules des produits + `interaction` |
| **Interaction avec les traitements en cours** | ordonnance × historique | + ventes du client (§6.3) |
| **Redondance** : même molécule (ou même classe) deux fois | ordonnance × historique | molécules + `dci_classe` |
| **Contre-indication liée au profil** | ordonnance × client | âge, sexe (assurés) ; autres critères si saisis |
| **Surdosage apparent** *(plus tard)* | ligne | posologie × dose maximale : `dosage_canon` est prêt, **la dose maximale par molécule ne l'est pas** (le RCP n'est pas structuré) |

Tout est une **requête SQL** sur des tables versionnées : même entrée, même résultat, et l'on sait
toujours dire *pourquoi* une alerte est levée.

### 6.3 « Traitements en cours » — les habitudes d'achat

Un produit est considéré **en cours** pour un client si :

- il figure dans une ordonnance `EN_COURS` du client, **ou**
- il a été acheté par le client dans une fenêtre paramétrable (90 jours par défaut), ou dans la
  durée de traitement estimée (§5.2) si elle est connue.

Requête : `sales` (client, statut clôturé, non annulée) → `sales_line` → molécules (§6.1), sur la
fenêtre. La limite est affichée à l'écran : *« contrôle établi sur les achats identifiés de ce
client »* — une vente anonyme n'y figure pas.

### 6.4 Présentation et responsabilité

- Alertes **graduées** (CI > AD > PE > APEC), avec couple en cause, conduite à tenir, source.
- **Aucun blocage automatique.** Une `CI` exige une **prise en compte explicite** (motif saisi) ;
  les autres niveaux, un simple acquittement.
- **Traçabilité** : alerte, niveau, utilisateur, décision, horodatage — rattachés à l'ordonnance et
  à la vente.
- **Produit non couvert, dit comme tel** : une ligne dont le produit n'a aucune molécule exploitable
  affiche « non contrôlé », jamais une absence d'alerte. Silence ≠ sécurité.
- Les contrôles s'exécutent aussi sur un panier **sans ordonnance** (vente conseil) dès qu'un client
  est identifié : c'est le même moteur.

---

## 7. Placement dans le code

```
pharmaSmart-domain/
├── domain/ordonnance/          Ordonnance, OrdonnanceLigne, OrdonnanceDelivrance
├── domain/pharmacovigilance/   Interaction, ClasseInteraction, ContreIndication
│                               (ProduitDci, RefSpecialite*, ProduitRefSpecialite existent déjà)
└── repository/…                dont les requêtes de contrôle (natives)

pharmaSmart-app/
├── service/ordonnance/
│   ├── lecture/                DocumentTextExtractor (Tika), OrdonnanceOcrService (Tess4J),
│   │                           OrdonnanceLineParser (règles), ProduitCandidatService (pg_trgm,
│   │                           catalogue + référentiel)
│   ├── OrdonnanceService       création, validation, délivrances, renouvellements
│   └── controle/               ControleOrdonnanceService, TraitementsEnCoursService,
│                               MoleculesProduitService (les deux chemins du §6.1)
├── service/referentiel/        ImportThesaurusService (Tika) — import versionné, relu
└── web/rest/                   OrdonnanceResource, ControleOrdonnanceResource

webapp/app/features/ordonnance/   écran de lecture/validation, fiche ordonnance, panneau d'alertes
```

Aucun de ces services ne dépend de `service/ai/`. Le paquetage `service/ordonnance/` porte
**ordonnance** et non **ai** : il n'y a pas d'IA dedans.

**Licence** : deux entrées `Feature`, optionnelles — `ORDONNANCE_LECTURE` (OCR) et
`ORDONNANCE_CONTROLE` (suivi + contrôles). Le contrôle a de la valeur même sans OCR.

**Rechargement du référentiel** : une migration Flyway est immuable. Pour mettre le référentiel à
jour : `referentiel_bdpm.py --rcp` → `generer_sql.py` → `vers_flyway.py --premiere-version <n>` produit
quatre nouvelles migrations (upserts ; la composition est remplacée), puis le job
`recalculReferentielJob` recalcule les rapprochements. Les décisions `VALIDE` et `REJETE` ne sont
jamais touchées.

---

## 8. Place laissée à l'IA

| Usage | Statut |
|---|---|
| Contrôle des interactions / contre-indications | **Exclu, définitivement.** |
| Rapprochement produit | **Exclu** (SQL uniquement). |
| Structuration d'une ligne que les règles n'ont pas comprise | **Option**, `pharma.ai.enabled=false` par défaut, modèle **local** uniquement (données de santé), ligne anonymisée. Sortie traitée comme la sortie des règles : proposée, jamais validée. |
| Lecture de l'écriture manuscrite | Hors portée de Tesseract ; relèverait d'un modèle de vision local — à réévaluer plus tard, sans engagement. |

Si l'option n'est jamais activée, le plan fonctionne à l'identique.

---

## 9. Sécurité et conformité

| Sujet | Règle |
|---|---|
| Image et en-tête | Ne quittent pas le poste serveur ; jamais journalisés. Conservation de l'image désactivée par défaut. |
| Journalisation | Durées, confiances, nombre de lignes, alertes et décisions — pas le texte de l'ordonnance. |
| Droits | Lecture/validation : privilège dédié ; saisie du motif sur une `CI` : pharmacien. Chaque endpoint `/api/**` doit porter son droit dans `nav_item_role` (sécurisation en mode ENFORCE : un droit oublié donne un 403). |
| Responsabilité | L'outil **aide**, il ne **décide** pas. Mention à l'écran et clause contractuelle. |
| Référentiels | Versionnés, sourcés, date d'import affichée ; une version non relue n'est pas publiée. BDPM : citer la source, ne pas altérer les données. |

---

## 10. Lots

| Lot | Contenu | Effort indicatif |
|---|---|---|
| **0 — Mesures et validations** | Sur une **base client réelle** : part des produits vendus sur ordonnance dotés de molécules (par `produit_dci` ou spécialité retenue) ; choix et droits du référentiel d'interactions ; POC Tika + Tess4J dans le jar sous Java 25 ; taille ajoutée à l'installeur. **Décision go / no-go.** | 2–3 j *(était 3–4 : la mesure de couverture s'appuie maintenant sur `produit_dci` et `ref_*`)* |
| ~~1 — Référentiel~~ **1′ — Interactions** | `classe_interaction`, `dci_classe`, `interaction`, `contre_indication` clés sur `ref_dci` ; import versionné (Tika) avec relecture. `produit_dci` et `ref_*` sont **faits**. | 1–1,5 sem *(était 1,5–2)* |
| **2 — Contrôles** | `MoleculesProduitService` (deux chemins), traitements en cours, moteur de contrôle, panneau d'alertes à la vente, mention « non contrôlé », traçabilité. **Utile sans OCR.** | 1,5 sem |
| **3 — Suivi d'ordonnance** | Tables, saisie manuelle, délivrances partielles, renouvellements, fiche client. | 1,5 sem |
| **4 — Lecture OCR** | Extraction Tika/Tess4J, règles, candidats (voies catalogue et molécule), écran de validation avec l'image. | 2 sem |
| **5 — Pilote** | Corpus réel, mesures (§11), ajustement des seuils. | 1 sem |

L'ordre est volontaire : **les contrôles (lots 1′–2) apportent la valeur de sécurité avant l'OCR**,
et l'OCR (lot 4) n'est qu'un accélérateur de saisie.

---

## 11. Mesures et critères d'arrêt

**Pilote** sur 50 à 100 ordonnances réelles anonymisées (moitié imprimées, moitié manuscrites) :

| Mesure | Seuil à fixer avant le pilote (proposition) |
|---|---|
| Lignes imprimées correctement lues | ≥ 85 % |
| Bon produit dans les 3 premiers candidats (imprimées) | ≥ 70 % |
| Temps de saisie par ordonnance vs saisie manuelle | gain ≥ 30 % |
| Produits vendus sur ordonnance dotés de molécules exploitables | ≥ 90 % (sinon les contrôles sont trompeurs) |
| Dont : par rapprochement `AUTO` / `VALIDE` plutôt que par saisie | à relever (mesure de l'effort de relecture) |
| Alertes jugées non pertinentes par le pharmacien | ≤ 20 % (au-delà, elles seront ignorées) |

**Arrêt ou gel** si : référentiel d'interactions inutilisable (droits ou pertinence locale) ;
couverture en molécules insuffisante et non rattrapable (rappel : un quart seulement du catalogue
ivoirien existe dans la BDPM) ; OCR trop lent pour le poste serveur sans dégrader les caisses ;
alertes massivement ignorées.

---

## 12. Questions ouvertes

1. **Référentiel d'interactions** : thésaurus ANSM (droits de réutilisation ?) ou fournisseur
   commercial ? Existe-t-il un référentiel ivoirien de référence ? *(inchangée — c'est la question
   qui conditionne le plan.)*
2. **Couverture locale** : les produits absents de la BDPM (marques ivoiriennes) auront leurs
   molécules saisies à la main. Qui s'en charge, et existe-t-il une nomenclature locale importable ?
3. **Profil patient** : faut-il ajouter grossesse, allaitement, allergies à la fiche client ? Qui
   les saisit, et comment sont-ils protégés ?
4. **Conservation de l'image** : utile en cas de litige, sensible au regard des données de santé.
   Désactivée par défaut — à confirmer.
5. **Dose maximale** : renoncer au contrôle de surdosage, ou financer un référentiel de doses
   structuré (le RCP en texte libre ne suffit pas) ?
6. **Mobile** : la capture photo par l'application Flutter entre-t-elle dans le premier jalon ?
7. ~~Associations : qui renseigne les DCI ?~~ **Largement réglée** : le rapprochement donne leurs
   molécules aux produits sans DCI ; reste la décomposition des DCI composées existantes (lot 3 du
   plan DCI n-n).
