# Référentiel médicaments (BDPM) et rapprochement du catalogue

Construit un référentiel CSV à partir de la **base de données publique des médicaments** (ANSM) et
rapproche un catalogue d'officine de ce référentiel. Python 3.10+, bibliothèque standard seule.

```bash
python referentiel_bdpm.py --catalogue catalogue.csv --sortie sortie            # sans les RCP
python referentiel_bdpm.py --catalogue catalogue.csv --sortie sortie --rcp      # + indications, posologie, contre-indications
```

Le catalogue est un CSV `;` avec au moins les colonnes `CIP` et `NOM` : celui de
`pharmaSmart-app/src/main/resources/model/modelle_NOUVELLE_INSTALLATION.csv` convient. Les fichiers de la BDPM
sont téléchargés dans `--cache` (par défaut `cache/`) et réutilisés une semaine ; `--rafraichir` force leur mise à jour.

## Fichiers produits

| Fichier | Contenu |
|---|---|
| `referentiel_bdpm.csv` | Une ligne par spécialité et par molécule : libellé commercial, forme, voies, DCI, composition, substance, code substance, dosage, SA/FT, groupe générique, princeps ou générique, princeps du groupe, nombre de substituables ; indications, posologie et contre-indications si les RCP ont été récupérés. |
| `groupes_generiques.csv` | Les membres de chaque groupe générique, princeps en tête : c'est la table de substitution. |
| `catalogue_rapproche.csv` | Chaque produit du catalogue : statut, score, motif, spécialité retenue, DCI, composition, groupe générique, **substituts présents dans le catalogue**, et les rubriques du RCP. |
| `dci_catalogue.csv` | `code;libelle` des molécules du catalogue : s'importe tel quel dans l'écran DCI (Import). |
| `rapport.txt` | Les chiffres du rapprochement. |

Tous les CSV sont en UTF-8 (avec BOM, pour Excel), séparateur `;`.

## Statuts du rapprochement

| Statut | Signification | Que faire |
|---|---|---|
| `SUR` | Même marque, même dosage, même forme. | Accepter. |
| `A_VERIFIER` | Marque approchante ; la colonne `motif` dit ce qui diffère (dosage, forme). | Relire. |
| `PAR_DCI` | Générique non français, rattaché par sa molécule et son dosage à la spécialité française de même composition (le princeps de préférence). | Accepter la composition ; **la monographie est celle du princeps français**, pas celle du produit. |
| `NON_TROUVE` | Parapharmacie, ou médicament absent de la base française. | Saisie manuelle de la DCI dans l'application. |

## Limites

- **Les codes CIP ne servent à rien ici** : ceux du catalogue sont des codes locaux, différents d'un grossiste à
  l'autre. Tout passe par le libellé.
- **Couverture** : sur le catalogue modèle, environ un quart des médicaments listés (tableau A/C) existent en France.
  Les autres sont des marques ivoiriennes, indiennes, etc., dont le nom ne contient pas la molécule. Seule une source
  locale (nomenclature des médicaments autorisés en Côte d'Ivoire) ou une saisie manuelle les couvrira.
- **Indications, posologie, contre-indications** : texte libre du RCP, repris tel quel (condition de réutilisation de
  la BDPM). Utilisable pour l'affichage, pas pour déclencher des alertes automatiques.
- **Données françaises** : les indications et posologies d'un produit autorisé en Côte d'Ivoire peuvent différer.

Source : Base de données publique des médicaments — https://base-donnees-publique.medicaments.gouv.fr (réutilisation
libre, sous réserve de citer la source et de ne pas altérer les données).

## Chargement en base (tables `ref_*`)

Le CSV `sortie-gpc/referentiel_bdpm.csv` alimente les tables créées par la migration Flyway
`V2.1.19__referentiel_medicament_bdpm.sql` : `ref_specialite`, `ref_specialite_composition`,
`ref_substance`, `ref_dci`, `ref_groupe_generique`, `ref_specialite_rcp` et la vue `v_ref_substitut`.

```bash
python generer_sql.py                 # régénère scripts/sql/01 … 06 depuis le CSV
cd ../sql
psql -U pharma_smart -d <base> -v ON_ERROR_STOP=1 -f run_all.sql
```

`07_lier_dci.sql` relie `ref_dci` aux DCI du catalogue par libellé normalisé (`ref_normaliser`) et
liste les cas ambigus ; `99_verification.sql` donne les comptes. Les fichiers `01` à `06` sont
générés : ne pas les modifier à la main.

## Rapprochement produit ↔ référentiel

La migration `V2.1.20__rapprochement_produit_referentiel.sql` ajoute la table `produit_ref_specialite`
(une proposition par produit : spécialité, statut `SUR` / `A_VERIFIER` / `PAR_DCI` / `NON_TROUVE`, score,
motif, décision) et trois fonctions PostgreSQL qui font tout le calcul, en une requête ensembliste :

| Fonction | Rôle |
|---|---|
| `ref_rapprocher_produits(ids, forcer)` | Propose une spécialité par produit : nom (exact, puis approché par trigramme), molécule (DCI du produit ou lue en tête de libellé), dosage et forme lus dans le libellé. `forcer = FALSE` : produits sans proposition ; `TRUE` : recalcule aussi les `EN_ATTENTE`. |
| `ref_lier_dci(ids)` | Relie `ref_dci` aux DCI du catalogue par libellé normalisé. |
| `ref_rapprocher_apres_nouvelles_dci(ids)` | Lie, puis recalcule les produits dont le libellé commence par la DCI. |

Déclenchement : étape `rapprocherReferentielStep` du pipeline de nuit (produits nouveaux), job
`recalculReferentielJob` (à la demande, après rechargement du référentiel), et événement `DcisAjouteesEvent`
publié par `DciServiceImpl` à la création ou à l'import de DCI (traité après le commit, hors requête).

Rien n'est appliqué au produit sans validation : `RapprochementProduitService.valider` fixe la décision et,
si le produit n'a aucune DCI, lui donne celles de la spécialité retenue.

### Acceptation automatique et dissociation

`ref_automatiser_rapprochement` enchaîne le rapprochement et `ref_appliquer_rapprochements` : les propositions
`SUR` et `PAR_DCI` passent en décision `AUTO` ; un produit **sans DCI** reçoit celles de la spécialité
(`produit_dci`, rang 1 = première molécule, `produit.dci_id` alignée), un produit déjà doté de DCI n'est pas
modifié. Si une molécule n'est pas encore reliée au catalogue, la proposition reste en attente et est reprise
dès l'import de la DCI manquante. Il reste à relire : `A_VERIFIER` et les `PAR_DCI` incomplets
(`GET /api/referentiel-medicament/rapprochements`, `POST .../valider|rejeter|reinitialiser`).

Retirer une DCI d'un produit (fiche produit, rattachement en lot, fusion) déclenche `produit_dci_dissociation` :
si cette DCI appartient à la spécialité retenue, le rapprochement passe à `REJETE` et n'est plus jamais
recalculé ni réappliqué. `reinitialiser` supprime la proposition pour que le produit redevienne candidat.

Au comptoir : `GET /api/referentiel-medicament/produits/{id}` donne spécialité, molécules et dosages, RCP
(indications, posologie, contre-indications) et substituts du catalogue (même groupe générique) — seulement
pour un rapprochement de confiance (`AUTO` ou `VALIDE`).
