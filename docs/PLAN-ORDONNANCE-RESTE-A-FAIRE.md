# Ordonnance — reste à faire

> Statut : **à jour au 6 octobre 2026.**
> Plan d'origine et historique de ce qui est livré : [PLAN-EXTRACTION-ORDONNANCE-OCR.md](PLAN-EXTRACTION-ORDONNANCE-OCR.md)
> (§15 à §33 bis). Ce document ne reprend que ce qui **n'est pas fait**.
> Plan parent : [PLAN-INTEGRATION-SPRING-AI.md](PLAN-INTEGRATION-SPRING-AI.md), étape 5 (UC-5).

---

## 0. Ce qui est livré (pour situer le reste)

| Lot | Livré |
|---|---|
| 1′ — interactions | Tables, entités, dépôts (`V2.1.28`) ; seules les versions **publiées** sont lues. |
| 2 — contrôles | Moteur (interactions, redondances, contre-indications de profil), modale d'alertes à la vente, blocage **paramétrable par niveau** (`APP_CONTROLE_ORDONNANCE_BLOQUANT_CI/AD/PE/APEC`, `V2.1.30`). |
| 3 — suivi | Prescripteurs (création à la volée, correction, fusion, désactivation), ordonnances, reste à délivrer, renouvellements, retours clients, appariement des génériques, pastille « en cours », type de prescription déduit du rattachement, renouvellement du traitement chronique. |
| Obligation | Produit sur ordonnance : **ordonnance ou prescripteur obligatoire à la clôture** (`APP_VENTE_ORDONNANCE_OBLIGATOIRE`, `V2.1.31`). |
| Démo | Statuts légaux du jeu de démo rendus réalistes (script `03c` et base en cours). |

---

## 1. À construire — sans dépendre d'une décision

| # | Chantier | Pourquoi | Effort indicatif |
|---|---|---|---|
| 1.1 | **FAIT (6 oct.)** — relu, rien à corriger ; `produit_ref_specialite` reste vide tant que le job de nuit n'a pas tourné. **Relecture du code de rapprochement** avec le référentiel BDPM (`ref_appliquer_rapprochements`, job de nuit). Le jeu de démo a été refait avec des produits du **BDPM** et des données fictives : plus de campagne de rapprochement ni de relecture des `A_VERIFIER` à prévoir, **la relecture du code suffit**. | Le chiffre de couverture (63 %, 380 / 600) portait sur l'ancien catalogue : il est caduc, à remesurer sur le nouveau jeu (3.1). | 0,25 j |
| 1.2 | **FAIT (6 oct.)** — `rappelerOrdonnanceRequise` dans `product-handling.mixin.ts` : avertissement sans blocage, un par vente, lu dans `ProduitSearch.statutLegal`. **Contrôle à l'ajout au panier** de la règle « produit sur ordonnance » (aujourd'hui seulement à la clôture). Prévenir le comptoir plus tôt, sans bloquer : le blocage reste à la clôture. **Sans appel base à chaque ajout** : lire l'indicateur dans le **produit déjà chargé** (statut légal / « sur ordonnance » porté par le produit renvoyé à la recherche) ; s'il n'y figure pas, l'ajouter au DTO de recherche plutôt que d'interroger la base. | Éviter de découvrir l'exigence au moment d'encaisser. | 0,5 j |
| 1.3 | **FAIT (6 oct.)** — `SaleDTO.prescripteur` renseigné par `SaleDataService.fetchPurchaseBy`, affiché dans la ligne dépliée du journal des ventes. **Prescripteur de la vente visible** sur la fiche de la vente. **Pas sur le ticket** (décidé). Aujourd'hui `vente_prescripteur` n'est lu que par le contrôle. | Traçabilité de la dispensation. | 0,25 j |
| 1.4 | **PARTIEL (6 oct.)** — démo rechargée (BDPM réel). Les parcours d'ordonnance (5) sont adaptés et passent : `AMOXICILLINE TEVA 1 g`, `ADVIL 400 mg`. **Reste : le reste des parcours cherche encore les anciens noms** (`DOLIPRANE 500MG`, `DOLIPRANE 1G`, `PARACETAMOL 1G`, `ARNICA MONTANA 9CH`, `ZYRTEC 250MG`, `ATORVASTATINE 100MG`…), absents du catalogue BDPM (ex. `DOLIPRANE 500 mg, gélule`) : à adapter, puis campagne complète (VTE-05 et parcours qui encaissent). | Le parcours de suivi a échoué **une fois**, non reproduit : surveillé. | 1 j |
| 1.5 | **FAIT** (anciennes copies supprimées par l'utilisateur). **Migrations BDPM : ne plus passer par Flyway** (décidé : trop lourd). Les données BDPM ne sont **pas** régénérées dans `src/` avec `--premiere-version` : les scripts (`generer_sql.py`) sont **exécutés une seule fois** (*one shot*) sur la base, hors historique Flyway. Reste à faire : **supprimer les anciennes copies** `V*` BDPM de `target/classes` (et de tout `src/`) pour que Flyway ne voie plus de doublon (« Found more than one migration with version »), et documenter la commande d'exécution unique. | Débloque les tests d'intégration. | 0,25 j |
| 1.6 | **FAIT (6 oct.)** — `e2e/a11y/themes-modales-ordonnance.spec.ts` : 75 éléments examinés, aucun défaut propre à un thème. | Garde-fou des thèmes. | — |

---

## 2. Bloqué par une décision ou une source

### 2.1 Source des interactions (question 1 du plan d'origine)

- **Constat** : aucune source en dehors des tables `ref_*` de la BDPM (seule base retenue pour l'instant), qui ne contiennent **aucune** interaction. Le contrôle fonctionne sans elle (redondances, équivalence générique, contre-indications de profil) mais ne lève **aucune interaction**.
- **À décider** : thésaurus ANSM (droits de réutilisation à vérifier), fournisseur commercial, source locale ivoirienne, ou liste tenue par un pharmacien.
- **Mécanisme prêt à construire dès qu'une source existe — import par fichier** (décrit au §32 du plan d'origine) :
  - un **CSV** crée une **version non publiée**, sourcée et datée ; rien n'est lu par le contrôle avant la **publication** (relecture humaine tracée) ;
  - colonnes : molécule A ou classe A ; molécule B ou classe B ; niveau (`CI`, `AD`, `PE`, `APEC`) ; mécanisme ; conduite à tenir ; fichiers facultatifs pour les classes (`classe_interaction`, `dci_classe`) et les contre-indications de profil (`contre_indication`) ;
  - rapprochement avec `ref_dci` par libellé normalisé (`ref_normaliser`) : **ligne sans correspondance ou ambiguë rejetée avec sa raison**, jamais devinée ;
  - écran de relecture (lignes, rejets, nombre par niveau), bouton « Publier » (droit dédié, relecteur et date tracés), retrait d'une version publiée ;
  - **aucun changement du moteur** : tables du lot 1′, `InteractionRepository.findEntre`, paramètres de blocage par niveau.
- **Si la source est le thésaurus ANSM (PDF)** : second usage de Tika, toujours suivi de la même relecture.
- **Effort** : 1 à 1,5 semaine une fois la source connue.

### 2.2 Ordonnancier (question 8, volet « registre »)

- **Décidé** : l'obligation d'ordonnance ou de prescripteur à la clôture (§33 du plan d'origine).
- **Reste à décider** : un **registre numéroté** des produits réglementés (stupéfiants, certaines substances : n° d'ordre, prescripteur, patient, date, produits), le prescripteur et la date devenant obligatoires pour eux. Dépend de la **loi locale** et de ce qu'exigent assureurs et inspections ; à ne construire que s'il est exigé.
- **Informations nécessaires** : catégories de produits concernées, obligation ou non d'un registre, forme attendue (état imprimable, consultable).

### 2.3 Autres règles de gestion (tranchées le 6 octobre 2026)

| Question | Décision |
|---|---|
| 10 | **Tranchée.** Pas d'obligation pour toute vente sur ordonnance : le prescripteur n'est exigé que pour les ordonnances **contenant des produits soumis à l'obligation** (c'est le comportement actuel, `APP_VENTE_ORDONNANCE_OBLIGATOIRE`). Rien à construire. |
| 11 | **Tranchée : non.** Pas de règle supplémentaire sur l'ordonnance sans client ; le prescripteur seul suffit à clôturer. |
| 9 | **Tranchée.** Identité du prescripteur = **nom et prénom** ; l'identifiant national (numéro d'ordre) est **facultatif**, jamais exigé. La déduplication repose donc sur nom, prénom et structure ; à vérifier dans le code de fusion. |
| 2 | **Sans réponse pour l'instant** : on s'appuie sur les tables `ref_*` seules ; pas de nomenclature locale ni de saisie des molécules hors BDPM. À rouvrir si la couverture mesurée (3.1) est insuffisante. |
| 4 | Conservation de l'image de l'ordonnance (désactivée par défaut) : sans objet tant que le lot 4 n'est pas lancé. |
| 5, 6 | **Écartées** (surdosage ; capture par photo mobile) : à ne pas traiter. |

---

## 3. Lot 0 — mesures et validations (hors code) : **décision go / no-go**

À faire avec l'officine ou le matériel ; **rien de cela n'est faisable par le seul développement**. Déjà acquis : POC Tika + Tess4J sous Java 25 (lecture d'une ordonnance imprimée synthétique, 461 ms à 300 dpi, 30,5 Mo de dépendances).

| # | Mesure | Critère |
|---|---|---|
| 3.1 | **Couverture en molécules** : d'abord sur le nouveau jeu de démo BDPM, puis sur une base client réelle | ≥ 90 % des produits vendus sur ordonnance |
| 3.2 | **Part d'ordonnances manuscrites** sur une semaine de comptoir (question 16) | conditionne tout le §8.1 |
| 3.3 | **Parc de scanners** : à plat ou à défilement, USB ou réseau, **pilote 64 bits** (questions 12, 14, 15) | WIA retenu ; eSCL et dossier surveillé en repli |
| 3.4 | **POC WIA en Rust** sur deux scanners réels (ADF, fin de pile, 200 et 300 dpi) | premier livrable avant toute écriture de l'écran de validation |
| 3.5 | **Démonstration de deux logiciels de gestion d'officine** (un français, un africain) pour confirmer le tableau §13.2 ; relever ce qu'exigent les assureurs sur le prescripteur | arguments à confirmer, non acquis |
| 3.6 | **Décision go / no-go** | référentiel d'interactions utilisable, couverture suffisante, OCR assez rapide pour le poste serveur sans dégrader les caisses |

---

## 4. Lot 4 — lecture OCR et numérisation (non commencé)

**Conditionné par le go / no-go du 3.6.** L'OCR n'est qu'un accélérateur de saisie : la valeur de sécurité (contrôles, suivi) ne dépend pas de lui.

- **Lecture** : `DocumentTextExtractor` (Tika : type du fichier, PDF numérique), `OrdonnanceOcrService` (Tess4J : préparation, OCR, confiance par ligne, seuil ~ 60 → « manuscrit probable »), `OrdonnanceLineParser` (règles : dosage, forme, posologie, durée, quantité), anonymisation de l'en-tête.
- **Candidats produit** : voie catalogue (marque, `pg_trgm`) et voie molécule (DCI → produits portant la molécule, générique pour princeps).
- **Écran de validation** : image en regard, lignes proposées distinguées des lignes saisies, prescripteur proposé depuis le cachet, **rien n'entre au panier ni ne déclenche de contrôle avant validation** ; traitement asynchrone page par page.
- **Numériseur** : `numeriseur.rs` (WIA natif en Rust, thread COM dédié, trois commandes `lister_numeriseurs`, `numeriser`, `annuler_numerisation`), `TauriNumeriseurService`, aperçu et recadrage, repli sans Tauri (dépôt de fichier, photo mobile).
- **Exploitation** : exécuteur dédié (1 à 2 traitements simultanés), `--enable-native-access=ALL-UNNAMED` au lancement du jar et du service Windows, `fra.traineddata` (et `eng`) dans l'installeur (`pharma.ordonnance.ocr.tessdata-path`), détection d'un même document numérisé deux fois (empreinte).
- **Licences** : entrées `Feature` `ORDONNANCE_LECTURE` et `ORDONNANCE_CONTROLE`, optionnelles.
- **Effort indicatif** : 2 semaines, + 0,5 à 1 semaine pour le numériseur.
- **Option IA** (structurer une ligne que les règles n'ont pas comprise) : modèle **local** uniquement, `pharma.ai.enabled=false` par défaut, jamais pour le contrôle des interactions ; à n'envisager qu'après le pilote.

---

## 5. Lot 5 — pilote et mesures (après le lot 4 et une source d'interactions)

Sur 50 à 100 ordonnances réelles anonymisées, moitié imprimées, moitié manuscrites (§11 du plan d'origine) :

| Mesure | Seuil à fixer avant le pilote |
|---|---|
| Lignes imprimées correctement lues | ≥ 85 % |
| Bon produit dans les 3 premiers candidats (imprimées) | ≥ 70 % |
| Gain de temps de saisie par ordonnance | ≥ 30 % |
| Produits vendus sur ordonnance dotés de molécules exploitables | ≥ 90 % |
| Alertes jugées non pertinentes par le pharmacien | ≤ 20 % |

**Arrêt ou gel** si : référentiel d'interactions inutilisable ; couverture non rattrapable ; OCR trop lent pour le poste serveur ; alertes massivement ignorées.

**Manuscrit** : saisie manuelle assistée au lancement ; essai d'un moteur ou d'un modèle local **seulement si** les critères du §8.1 sont réunis (substantiel, bon candidat dans les 3 premiers pour ≥ 60 % des lignes avec une confiance qui sépare juste et faux, matériel mesuré, gain supérieur à la saisie assistée).

---

## 6. Points d'attention techniques

- **Tests d'intégration** : Docker Desktop exige une connexion à l'organisation (sinon `docker ps` est refusé). Commande : `DOCKER_API_VERSION=1.44 TESTCONTAINERS_RYUK_DISABLED=true mvnw.cmd test -pl pharmaSmart-app -am -Dskip.npm=true -Dtest=OrdonnanceControleIntegrationTest` ; image `postgres:18-alpine` à taguer depuis `postgres:18` si elle ne peut pas être téléchargée. **Les migrations BDPM obsolètes de `target/classes` (1.5) font échouer Flyway : les supprimer, les données BDPM étant chargées en *one shot* hors Flyway.**
- **Parcours e2e d'ordonnance** : `npx playwright test -c e2e --project=regression ordonnance --reporter=list` (4 parcours) ; `e2e:a11y` pour les thèmes (`themes-panneau-ordonnance`). Ils **écrivent** dans la base (ventes réelles pour `ordonnance-obligatoire`) ; jeu d'essai de `scripts/essai-ordonnance/` (`preparer.sql`, `nettoyer.sql`).
- **Démo** : un dossier `/tmp/demo-data-statuts` reste dans le conteneur Postgres (copie des scripts, sans effet) ; à supprimer à la main.
- **Aucune commande git** n'est lancée par l'assistant sur ce projet (consigne de l'utilisateur).

---

## 7. Ordre proposé

1. **1.5** nettoyage des migrations BDPM → **1.1** relecture du rapprochement → **1.2**, **1.3**, **1.4**, **1.6**.
2. En parallèle côté métier : décisions **2.1** (source), **2.2** (ordonnancier) et le lot 0 (**3**).
3. **2.1** : import par fichier dès qu'une source existe.
4. **Lot 4** seulement après un **go** au 3.6 ; puis le **lot 5**.
