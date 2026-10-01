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
