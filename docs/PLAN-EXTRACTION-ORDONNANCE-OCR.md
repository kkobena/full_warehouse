# Plan — Lecture, suivi et contrôle des ordonnances (OCR, sans IA)

> Statut : **analyse** — aucune ligne de code écrite.
> Date : septembre 2026.
> Plan parent : [PLAN-INTEGRATION-SPRING-AI.md](PLAN-INTEGRATION-SPRING-AI.md), étape 5 (UC-5).
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
| Retrouver les produits du catalogue | `pg_trgm` + `unaccent` (déjà installés) | Un LLM peut inventer un produit ou un dosage absent du catalogue. |
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

## 2. Ce qui existe, ce qui manque

Constats sur le code au 28/09/2026 :

| Élément | État |
|---|---|
| `pg_trgm`, `unaccent` | Installés (`V1.0.1__init.sql`) ; `search_produits_json` s'appuie sur `unaccent`. |
| Produit ↔ DCI | **Une seule DCI par produit** (`Produit.dci`, `Dci` = code + libellé). Les associations (ex. amoxicilline + acide clavulanique) ne sont pas modélisables. |
| Vente ↔ client | `Sales.customer`, `Sales.typePrescription` (`PRESCRIPTION`, `CONSEIL`, `DEPOT`). |
| Profil patient | Âge (`datNaiss`) et sexe **uniquement sur les assurés** (`AssuredCustomer`). Rien sur grossesse, allergies, insuffisance rénale. |
| Ordonnance, prescripteur | **Absents.** |
| Référentiel d'interactions / contre-indications | **Absent.** |
| PDFBox | Déjà dépendance de `pharmaSmart-app` — compatibilité de version à vérifier avec Tika. |
| Tika, Tess4J | Absents. |

Deux conséquences structurent le plan :

1. **Le contrôle ne vaut que ce que vaut le lien produit → DCI.** Un produit sans DCI est
   invisible pour le moteur d'interactions. Le lot 0 mesure cette couverture avant tout.
2. **L'historique d'achat ne voit que les ventes rattachées à un client.** Une vente comptant
   anonyme n'entre pas dans les « traitements en cours ». C'est une limite à afficher, pas à cacher.

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
                               pg_trgm/unaccent : candidats produits                      │
                                                                                          ▼
                               Moteur de contrôle (SQL) ◄───── panier + traitements en cours du client
                                     ▼
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
| Dosage | `\d+([.,]\d+)?\s?(mg|g|µg|ml|UI|%)` |
| Forme | cp, comprimé, gél., gélule, sirop, sachet, suppo, pommade, inj… (liste paramétrable) |
| Posologie | `\d+\s?(cp|gél|cuill|sachet).*?(x|fois)\s?\d+\s?/\s?j` |
| Durée | `\d+\s?(j|jours|sem|mois)` |
| Quantité | `QSP`, `\d+\s?(boîte|bte)` |
| Libellé lu | le reste, normalisé (`unaccent`, majuscules) |

### 4.3 Rapprochement avec le catalogue

Recherche `pg_trgm` sur le libellé lu, filtrée puis bonifiée par le **dosage** et la **forme**
quand ils sont reconnus, et élargie par la **DCI** (un générique répond pour un princeps). Résultat :
3 à 5 candidats **réels**, chacun avec un score. Aucun produit n'est inventé ; aucun n'est choisi
automatiquement.

### 4.4 Contraintes d'exploitation

- **Charge du poste serveur** : il sert aussi les caisses. OCR dans un exécuteur dédié, 1 à 2
  traitements simultanés, délai maximal, une instance Tesseract par traitement (non partageable
  entre threads).
- **Java 25** : JNA déclenche l'avertissement « restricted methods » ; ajouter
  `--enable-native-access=ALL-UNNAMED` au lancement du jar et au service Windows.
- **Installeur** : embarquer `fra.traineddata` (et `eng`) — de quelques Mo à ~20 Mo selon la
  qualité retenue. Chemin paramétrable `pharma.ordonnance.ocr.tessdata-path`.

---

## 5. Suivi d'ordonnance

### 5.1 Modèle

| Table | Contenu |
|---|---|
| `ordonnance` | client (obligatoire), prescripteur (texte libre au départ), date de prescription, source (`SCAN`, `PDF`, `MANUELLE`), statut (`EN_COURS`, `TERMINEE`, `EXPIREE`), renouvelable (nombre), date de fin de validité, `created_by`. |
| `ordonnance_ligne` | texte lu, confiance OCR, produit validé, DCI, posologie, durée (jours), quantité prescrite, quantité délivrée. |
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

---

## 6. Contrôles de sécurité (sans IA)

### 6.1 Référentiel

| Table | Contenu |
|---|---|
| `classe_interaction` | classes du thésaurus (ex. « AINS », « anticoagulants oraux »). |
| `dci_classe` | appartenance d'une DCI à une ou plusieurs classes. |
| `interaction` | couple (DCI ou classe) × (DCI ou classe), **niveau** (`CI` contre-indication, `AD` association déconseillée, `PE` précaution d'emploi, `APEC` à prendre en compte), mécanisme, conduite à tenir, **source + version**. |
| `contre_indication` | DCI × critère patient (âge < / > seuil, sexe, grossesse, allaitement…), niveau, source. |
| `produit_dci` | **N DCI par produit** (remplace à terme `Produit.dci`, qui ne porte qu'une DCI) — plan dédié : [PLAN-PRODUIT-DCI-N-N.md](PLAN-PRODUIT-DCI-N-N.md). |

**Source** : le *Thésaurus des interactions médicamenteuses* de l'ANSM, public et mis à jour
régulièrement, publié en PDF. Son import est d'ailleurs un **second usage de Tika** : extraction
du PDF, puis analyse de sa structure (entrées « DCI + DCI : niveau, conduite »), avec relecture
humaine avant publication d'une version. **À valider avant tout développement** : conditions de
réutilisation du thésaurus, et pertinence pour la Côte d'Ivoire (un référentiel national ou un
fournisseur commercial — Vidal, Thériaque — peut s'imposer). Chaque alerte affiche sa source et
la version du référentiel.

### 6.2 Contrôles

| Contrôle | Portée | Donnée nécessaire |
|---|---|---|
| **Interaction** entre lignes de l'ordonnance | ordonnance | `produit_dci` + `interaction` |
| **Interaction avec les traitements en cours** | ordonnance × historique | + ventes du client (§6.3) |
| **Redondance** : même DCI (ou même classe) deux fois | ordonnance × historique | `produit_dci`, `dci_classe` |
| **Contre-indication liée au profil** | ordonnance × client | âge, sexe (assurés) ; autres critères si saisis |
| **Surdosage apparent** *(plus tard)* | ligne | posologie × dose maximale — nécessite un référentiel de doses |

Tout est une **requête SQL** sur des tables versionnées : même entrée, même résultat, et l'on sait
toujours dire *pourquoi* une alerte est levée.

### 6.3 « Traitements en cours » — les habitudes d'achat

Un produit est considéré **en cours** pour un client si :

- il figure dans une ordonnance `EN_COURS` du client, **ou**
- il a été acheté par le client dans une fenêtre paramétrable (90 jours par défaut), ou dans la
  durée de traitement estimée (§5.2) si elle est connue.

Requête : `sales` (client, statut clôturé, non annulée) → `sales_line` → `produit_dci`, sur la
fenêtre. La limite est affichée à l'écran : *« contrôle établi sur les achats identifiés de ce
client »* — une vente anonyme n'y figure pas.

### 6.4 Présentation et responsabilité

- Alertes **graduées** (CI > AD > PE > APEC), avec couple en cause, conduite à tenir, source.
- **Aucun blocage automatique.** Une `CI` exige une **prise en compte explicite** (motif saisi) ;
  les autres niveaux, un simple acquittement.
- **Traçabilité** : alerte, niveau, utilisateur, décision, horodatage — rattachés à l'ordonnance et
  à la vente.
- Les contrôles s'exécutent aussi sur un panier **sans ordonnance** (vente conseil) dès qu'un client
  est identifié : c'est le même moteur.

---

## 7. Placement dans le code

```
pharmaSmart-domain/
├── domain/ordonnance/          Ordonnance, OrdonnanceLigne, OrdonnanceDelivrance
├── domain/pharmacovigilance/   Interaction, ClasseInteraction, ContreIndication, ProduitDci
└── repository/…                dont les requêtes de contrôle (natives)

pharmaSmart-app/
├── service/ordonnance/
│   ├── lecture/                DocumentTextExtractor (Tika), OrdonnanceOcrService (Tess4J),
│   │                           OrdonnanceLineParser (règles), ProduitCandidatService (pg_trgm)
│   ├── OrdonnanceService       création, validation, délivrances, renouvellements
│   └── controle/               ControleOrdonnanceService, TraitementsEnCoursService
├── service/referentiel/        ImportThesaurusService (Tika) — import versionné, relu
└── web/rest/                   OrdonnanceResource, ControleOrdonnanceResource

webapp/app/features/ordonnance/   écran de lecture/validation, fiche ordonnance, panneau d'alertes
```

Aucun de ces services ne dépend de `service/ai/`. Le paquetage `service/ordonnance/` porte
**ordonnance** et non **ai** : il n'y a pas d'IA dedans.

**Licence** : deux entrées `Feature`, optionnelles — `ORDONNANCE_LECTURE` (OCR) et
`ORDONNANCE_CONTROLE` (suivi + contrôles). Le contrôle a de la valeur même sans OCR.

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
| Droits | Lecture/validation : privilège dédié ; saisie du motif sur une `CI` : pharmacien. |
| Responsabilité | L'outil **aide**, il ne **décide** pas. Mention à l'écran et clause contractuelle. |
| Référentiel | Versionné, sourcé, date d'import affichée ; une version non relue n'est pas publiée. |

---

## 10. Lots

| Lot | Contenu | Effort indicatif |
|---|---|---|
| **0 — Mesures et validations** | Couverture DCI du catalogue (produits sans DCI, associations) ; choix et droits du référentiel d'interactions ; POC Tika + Tess4J dans le jar sous Java 25 ; taille ajoutée à l'installeur. **Décision go / no-go.** | 3–4 j |
| **1 — Référentiel** | `produit_dci`, tables d'interactions et de contre-indications, import versionné (Tika) avec relecture. | 1,5–2 sem |
| **2 — Contrôles** | Traitements en cours, moteur de contrôle, panneau d'alertes à la vente, traçabilité. **Utile sans OCR.** | 1,5 sem |
| **3 — Suivi d'ordonnance** | Tables, saisie manuelle, délivrances partielles, renouvellements, fiche client. | 1,5 sem |
| **4 — Lecture OCR** | Extraction Tika/Tess4J, règles, candidats `pg_trgm`, écran de validation avec l'image. | 2 sem |
| **5 — Pilote** | Corpus réel, mesures (§11), ajustement des seuils. | 1 sem |

L'ordre est volontaire : **les contrôles (lots 1–2) apportent la valeur de sécurité avant l'OCR**,
et l'OCR (lot 4) n'est qu'un accélérateur de saisie.

---

## 11. Mesures et critères d'arrêt

**Pilote** sur 50 à 100 ordonnances réelles anonymisées (moitié imprimées, moitié manuscrites) :

| Mesure | Seuil à fixer avant le pilote (proposition) |
|---|---|
| Lignes imprimées correctement lues | ≥ 85 % |
| Bon produit dans les 3 premiers candidats (imprimées) | ≥ 70 % |
| Temps de saisie par ordonnance vs saisie manuelle | gain ≥ 30 % |
| Produits vendus sur ordonnance couverts par une DCI | ≥ 90 % (sinon les contrôles sont trompeurs) |
| Alertes jugées non pertinentes par le pharmacien | ≤ 20 % (au-delà, elles seront ignorées) |

**Arrêt ou gel** si : référentiel d'interactions inutilisable (droits ou pertinence locale) ;
couverture DCI insuffisante et non rattrapable ; OCR trop lent pour le poste serveur sans dégrader
les caisses ; alertes massivement ignorées.

---

## 12. Questions ouvertes

1. **Référentiel** : thésaurus ANSM (droits de réutilisation ?) ou fournisseur commercial ?
   Existe-t-il un référentiel ivoirien de référence ?
2. **Profil patient** : faut-il ajouter grossesse, allaitement, allergies à la fiche client ? Qui
   les saisit, et comment sont-ils protégés ?
3. **Conservation de l'image** : utile en cas de litige, sensible au regard des données de santé.
   Désactivée par défaut — à confirmer.
4. **Associations** : migration de `Produit.dci` vers `produit_dci` — qui renseigne les DCI des
   associations existantes ?
5. **Mobile** : la capture photo par l'application Flutter entre-t-elle dans le premier jalon ?

---

## Annexe — Documents liés

- [PLAN-INTEGRATION-SPRING-AI.md](PLAN-INTEGRATION-SPRING-AI.md) — UC-5, règles de dégradation, licence
- [PLAN-GESTION-LICENCE.md](PLAN-GESTION-LICENCE.md) — mécanisme `Feature`
