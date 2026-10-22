# `scripts/` — reset-base.sql

Purge sélective d'une base Pharma-Smart : vider le catalogue produit, les tables de référence,
les données d'exploitation, ou n'importe quelle table à l'unité.

Le reste du répertoire (`build-app.js`, `sync-version.js`, `prepare-*.js`…) sert à la
construction et au packaging ; `demo-data/` et `sql/` ont chacun leur propre README.

---

## En deux commandes

```bash
cd scripts

# 1. Voir ce qui serait vidé — rien n'est touché
psql -U pharma_smart -d pharma_smart -v ON_ERROR_STOP=1 \
     -v cibles=catalogue -f reset-base.sql

# 2. Vider pour de bon
psql -U pharma_smart -d pharma_smart -v ON_ERROR_STOP=1 \
     -v cibles=catalogue -v confirm_reset=1 -f reset-base.sql
```

**Sans `-v confirm_reset=1`, le script ne modifie rien.** Il affiche le plan : tables ciblées,
tables que le `CASCADE` emportera en plus, et le nombre exact de lignes de chacune. C'est le
mode à utiliser d'abord, systématiquement — une table de référence en entraîne souvent beaucoup
d'autres (voir [Ce que CASCADE emporte](#ce-que-cascade-emporte)).

---

## Paramètres

| Paramètre | Défaut | Rôle |
|---|---|---|
| `-v cibles=<groupes>` | — | Groupes à purger, séparés par des virgules |
| `-v tables=<tables>`  | — | Tables à purger à l'unité, en plus ou à la place des groupes |
| `-v confirm_reset=1`  | absent | Sans lui : simulation. Avec lui : purge |
| `-v schema=<schéma>`  | `pharma_smart` | Installations historiques sous un autre schéma |
| `-v ON_ERROR_STOP=1`  | — | À passer toujours : sinon psql continue après une erreur |

Un groupe ou une table dont le nom est inconnu arrête le script avant toute modification.

---

## Groupes

| Groupe | Contenu |
|---|---|
| `catalogue` | `produit` et ses satellites : `produit_code_alias`, `produit_dci`, `produit_ref_specialite`, `produit_tiers_payant_prix`, `fournisseur_produit`, `fournisseur_produit_price_history`, `rayon_produit`, `substitut` |
| `referentiels` | `tva`, `categorie`, `famille_produit`, `form_produit`, `tableau`, `dci`, `magasin`, `storage`, `payment_mode`, `grille_remise`, `motif_ajustement`, `motif_retour_produit` |
| `bdpm` | Référentiel médicament importé : `ref_*`, plus `interaction`, `contre_indication`, `classe_interaction`, `dci_classe`, `referentiel_interaction_version` |
| `socle` | Comptes, droits, navigation, paramétrage, licence : `app_user`, `authority`, `nav_*`, `app_configuration`, `dashboard_layout*`, `license_*`, `semois_classe_config`, `poste*`… |
| `exploitation` | **Tout le reste**, par soustraction : ventes, caisses, commandes, stock, lots, inventaires, factures, clients — et ce que l'officine saisit elle-même : `fournisseur`, `groupe_fournisseur`, `rayon`, `laboratoire`, `gamme_produit`, `banque`, `tiers_payant`, `groupe_tiers_payant`, `remise`, `prescripteur`, `semois_configuration` |
| `tout` | Les cinq groupes |

`exploitation` est défini par soustraction et non par une liste : les tables de données se
comptent par centaines et s'enrichissent à chaque migration, alors que les quatre listes
ci-dessus sont courtes et stables. Une table ajoutée par une migration future tombe donc
d'elle-même dans `exploitation`.

Une table absente de la base est ignorée : le script reste lançable sur une base plus ancienne
que le code.

---

## Ce que CASCADE emporte

Le script vide ses cibles en un seul `TRUNCATE … RESTART IDENTITY CASCADE`, ce qui emporte tout
ce qui les référence, de proche en proche. Deux exemples que le plan rend visibles :

- `-v tables=tva` emporte `produit` (qui porte une TVA), donc `sales_line`, `lot`,
  `stock_produit`… — soit près de cinquante tables.
- `referentiels` emporte **`app_user`**, parce que `app_user.magasin_id` référence `magasin` :
  plus aucun compte, donc plus de connexion possible.
- `exploitation` emporte `grille_remise`, qui référence `remise`, alors qu'elle appartient aux
  `referentiels` : l'appartenance à un groupe dit ce qui est ciblé, pas ce qui survit.

C'est la raison d'être du mode simulation : lire le plan avant de confirmer.

---

## Après une purge

Le script enchaîne de lui-même :

- **Séquences autonomes** (`id_sale_seq`, `id_commande_seq`, `avoir_numero_seq`…) remises à 1,
  **uniquement** si toute l'exploitation est purgée. Les remettre à 1 alors qu'il reste des
  lignes provoquerait des collisions de clés ; dans ce cas le script les laisse en l'état et le
  dit.
- **Vues matérialisées** rafraîchies, sans quoi elles garderaient l'image d'avant la purge.
- L'**historique Flyway** n'est jamais touché. Il est reconnu à sa colonne `installed_rank`,
  pas à son nom, qui dépend de la configuration.

### Les lignes de Flyway ne reviennent pas toutes seules

`magasin`, `app_user`, `tva`, `categorie`, `famille_produit`, `form_produit`, `payment_mode`,
`storage`, `rayon`, les menus… sont posées par `V1.0.2__referentiels.sql`. Flyway ne rejoue pas
une migration déjà inscrite dans son historique : après un `referentiels` ou un `socle`, il faut
**recréer la base** et laisser les migrations se rejouer au démarrage du backend. Le script
affiche un avertissement en ce sens.

---

## Et `demo-data/00_reset.sql` ?

Les deux ne servent pas au même usage :

| | `demo-data/00_reset.sql` | `reset-base.sql` |
|---|---|---|
| Portée | Fixe : toutes les données métier | Au choix, par groupes ou par tables |
| Référentiels | Toujours conservés | Purgeables |
| Appelé par | `run_all.sql`, à chaque rechargement de la démo | À la main |

Pour **recharger le jeu de démonstration**, c'est `demo-data/run_all.sql` qu'il faut lancer : il
appelle `00_reset.sql` lui-même. `reset-base.sql` sert aux cas que la démo ne couvre pas —
repartir d'un catalogue vide avant un import client, retirer un référentiel importé par erreur,
vider une base avant de la céder.

---

## Exemples

```bash
# Catalogue produit seul, avant un import client
psql ... -v cibles=catalogue -v confirm_reset=1 -f reset-base.sql

# Catalogue + référentiels produit
psql ... -v cibles=catalogue,referentiels -v confirm_reset=1 -f reset-base.sql

# Deux tables de référence nommément
psql ... -v tables=tva,payment_mode -v confirm_reset=1 -f reset-base.sql

# Base entièrement vide, schéma et historique Flyway conservés
psql ... -v cibles=tout -v confirm_reset=1 -f reset-base.sql

# Installation historique sous un autre schéma
psql ... -v schema=warehouse -v cibles=catalogue -v confirm_reset=1 -f reset-base.sql
```
