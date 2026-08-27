# Données de démonstration — Pharma-Smart

Scripts SQL autonomes générant un jeu de données de pharmacie cohérent.

**Ils ne sont pas des migrations Flyway** : ils ne s'exécutent jamais au démarrage de
l'application et se lancent à la demande, sur une base dédiée.

Conception détaillée : [`docs/PLAN-GENERATION-DONNEES-DEMO.md`](../../docs/PLAN-GENERATION-DONNEES-DEMO.md).

---

## Nomenclature

| | |
|---|---|
| Rôle PostgreSQL | `pharma_smart` |
| Base | `pharma_smart_demo` |
| Schéma | `pharma_smart` |

Le schéma est surchargeable (`-v schema=...`) pour les installations historiques qui en
utilisent un autre.

---

## Mise en route

### 1. Créer la base

Avec un compte superutilisateur, depuis ce répertoire :

```bash
psql -U postgres -d postgres -v ON_ERROR_STOP=1 -f create_database.sql
```

Crée le rôle `pharma_smart`, la base `pharma_smart_demo` et le schéma `pharma_smart`.
Idempotent : ne fait rien si l'objet existe déjà.

Surcharges : `-v db=...`, `-v owner=...`, `-v pwd=...`

### 2. Créer les tables (Flyway)

Les scripts de démo **ne créent aucune table** : ils s'adossent au schéma et aux
référentiels posés par Flyway.

```bash
mvnw.cmd flyway:migrate ^
  -Dflyway.url=jdbc:postgresql://localhost:5432/pharma_smart_demo ^
  -Dflyway.user=pharma_smart -Dflyway.password=2802_pharma_smart
```

Le mot de passe est celui de `application-dev.yml`. Si `create_database.sql` a créé le rôle
avec un autre (`-v pwd=...`), utiliser celui-là.

### 3. Charger les données

```bash
cd scripts/demo-data
psql -U pharma_smart -d pharma_smart_demo ^
     -v ON_ERROR_STOP=1 -v confirm_reset=1 -f run_all.sql
```

> `ON_ERROR_STOP=1` est **indispensable** : sans lui, psql poursuit après une erreur et
> laisse une base à moitié chargée.

### 4. Redémarrer l'application

`01_config.sql` modifie des clés **mises en cache** par l'application
(`APP_GESTION_LOT`, `APP_GESTION_LOT_INVENTAIRE`). Un redémarrage — ou une purge du cache —
est nécessaire si l'instance tourne déjà.

---

## Scripts

| Fichier | Rôle |
|---|---|
| `create_database.sql` | Rôle, base et schéma. **Hors `run_all`** : vise une autre base. |
| `_header.sql` | En-tête commun, pose le `search_path`. Inclus par chaque script. |
| `00_reset.sql` | Purge des données métier, préserve les référentiels Flyway. |
| `01_config.sql` | Active la gestion de lot. |
| `02_fournisseurs.sql` | 5 grossistes principaux + 12 agences. |
| `02b_dci.sql` | 59 substances actives (DCI), référentiel du catalogue. |
| `03_produits.sql` | 600 produits, codes fournisseurs, rayons, substances actives. |
| `03b_substituts.sql` | Catalogue de substitution générique et thérapeutique. |
| `04_clients.sql` | 320 clients, 12 organismes tiers-payants, 170 contrats. |
| `05_commandes.sql` | 120 commandes fournisseurs, ~2 000 lignes. |
| `06_lots.sql` | Lots, cohortes de péremption, sérials FMD. |
| `07_stock.sql` | Réceptions, emplacements par stockage, stock produit. |
| `08_caisses.sql` | Postes, vendeurs, ~154 caisses journalières. |
| `09_ventes.sql` | ~3 850 ventes, lignes, FEFO, règlements, ventilation TP. |
| `10_repartitions.sql` | Ventilation TVA de la part de chaque payeur. |
| `10b_ventes_depot.sql` | Magasin dépôt, transferts, retours. |
| `12_destruction.sql` | Produits périmés en attente ou détruits. |
| `13_consommations.sql` | Cumuls mensuels par contrat et par organisme. |
| `14_facturation.sql` | Factures tiers-payant mensuelles par organisme. |
| `15_reference.sql` | Compteurs de numérotation par jour et par type. |
| `16_mouvements.sql` | Historique des mouvements produit. |
| `99_verification.sql` | Contrôles d'intégrité. Sort en erreur si un invariant est violé. |
| `run_all.sql` | Enchaîne le tout. |

**Les sept étapes sont écrites.** Les scripts n'ont pas encore été exécutés : voir
« Ce qui reste à faire » en fin de document.

---

## Le reset est destructeur

`00_reset.sql` vide **toutes** les tables métier du schéma. Il refuse de s'exécuter sans
confirmation explicite :

```bash
psql ... -v confirm_reset=1 -f 00_reset.sql
```

Il fonctionne par **liste blanche** : on énumère les tables à conserver — les référentiels
posés par Flyway — et tout le reste est tronqué. La liste des tables métier dépasse la
centaine et se périmerait à la première migration ; celle des référentiels est courte et
stable, et une nouvelle table métier est ainsi couverte automatiquement.

Il remet aussi à zéro les **séquences autonomes** (`id_sale_seq`, `id_commande_seq`…) que
`RESTART IDENTITY` ne touche pas : ce sont elles qui alimentent les identifiants assignés à
la main des entités à clé composite.

---

## Ce que produisent les scripts

### Fournisseurs

Le modèle est **auto-référençant** :

- `parent_id IS NULL` → fournisseur **principal**
- `parent_id IS NOT NULL` → **agence** rattachée à un principal

Deux règles à ne pas inverser :

1. Les `fournisseur_produit` (codes CIP, prix) sont **toujours** portés par le principal.
   `V1.7.1__fournisseur_groupe_to_principal.sql` *supprime* ceux rattachés à une agence :
   en créer produirait une base que la production nettoierait.
2. Les commandes se passent **chez l'agence** quand le principal en a.

| Principal | Agences |
|---|---|
| LABOREX-CI | Treichville, Cocody, Bouaké |
| DPCI | Marcory, Yopougon, Daloa |
| COPHARMED | Plateau, Adjamé, San-Pédro |
| UBIPHARM CI | Koumassi, Abobo, Yamoussoukro |
| DIVERS FOURNISSEURS | *aucune* — cas de repli |

Une partie des agences a un `delai_livraison_jours` nul : c'est le cas de repli sur le délai
du parent, et il doit être représenté.

### Produits

600 produits, montants **entiers en FCFA**.

| Famille | Nombre | TVA | Statut légal |
|---|---|---|---|
| Médicaments France (1050) | 250 | 0 % | Liste I |
| Spécialités publiques (1000) | 80 | 0 % | Liste I |
| Génériques (1030) | 60 | 0 % | Liste II |
| Homéopathie (1040) | 50 | 18 % | Sans liste |
| Diététique infantile (5000) | 40 | 18 % | Sans liste |
| Diététique adulte (6000) | 20 | 18 % | Sans liste |
| Parfumerie (3000) | 20 | 18 % | Sans liste |
| Accessoires (8000) | 30 | 18 % | Sans liste |
| Déconditionnables | 25 `PACKAGE` + 25 `DETAIL` | 0 % | Liste II |

Les libellés sont construits par combinaison (base, dosage, conditionnement). L'index est
décomposé en base numérique variable, ce qui **garantit l'unicité** exigée par la contrainte
`(libelle, type_produit)` — sans recourir au hasard, donc reproductible à l'identique.

Les prix suivent la même logique : étalement déterministe dans une fourchette par famille,
arrondi à 5 F.

**Chaque médicament porte sa substance active** (`dci_id`), et plusieurs produits partagent
la même : `DOLIPRANE`, `EFFERALGAN`, `PARACETAMOL`, `PARACETAMOL GE` et `PARACETAMOL SIROP`
pointent tous la molécule *PARACETAMOL*. C'est ce qui rend la **substitution générique**
démontrable — sans cela, l'écran de substitution ne propose jamais rien.

La résolution suit deux cas : le libellé de base *est* déjà la molécule (médicaments France,
génériques, souches homéopathiques), ou c'est un **nom de marque** qu'une correspondance
explicite ramène à sa molécule (spécialités publiques). Les suffixes `GE` et `SIROP` sont
retirés avant recherche : ce sont des présentations, pas des substances différentes.

Parapharmacie, diététique et accessoires n'ont **pas** de DCI — la colonne reste nulle.

Le **catalogue de substitution** (`substitut`) en découle. Deux types, contraints par un CHECK :

- `GENERIQUE` — même molécule **et même dosage**, sous une marque différente. Les deux
  conditions comptent : `PARACETAMOL 100MG` ne remplace pas `PARACETAMOL 1G`, et deux
  conditionnements du même produit (`B/10`, `B/20`) ne sont pas une substitution mais un choix
  de boîte.
- `THERAPEUTIQUE` — molécules différentes d'une même classe, le rayon commercial tenant lieu
  de classe thérapeutique.

La table est écrite dans un sens mais **lue dans les deux** (`findAllByProduitId` et
`findAllBySubstitutId`) : on ne pose donc qu'**une ligne par paire**, dans un sens canonique.
En poser deux ferait apparaître chaque partenaire en double à l'écran.

Le catalogue est volontairement **creux**. En production il s'alimente au fil de l'eau, quand
une substitution proposée par un grossiste est acceptée (`PharmaMlHttpClientService`) : un
catalogue exhaustif de toutes les équivalences théoriques ne ressemblerait pas à une base réelle.

Points de modèle exercés volontairement :

- **le CIP vit sur `fournisseur_produit`**, pas sur `produit` ;
- accessoires et parfumerie sont **hors gestion de lot** (`gestion_lot = false`), le chemin
  mixte étant un cas réel en officine ;
- les couples `PACKAGE` / `DETAIL` portent le **déconditionnement** : c'est la boîte qui est
  `deconditionnable`, et l'unité qui porte `parent_id`.

### Clients et tiers-payants

| | Nombre | |
|---|---|---|
| Clients comptant | 150 | `UninsuredCustomer` |
| Assurés principaux | 120 | `AssuredCustomer`, `type_assure = PRINCIPAL` |
| Ayants droit | 50 | `assure_principal_id` renseigné |
| Comptes carnet | ~37 | `customer_account`, pour les ventes différées |
| Organismes | 12 | 11 assurances + 1 carnet, répartis en 3 groupes |
| Contrats | 170 | 80 assurés à 1 payeur, 30 à 2, 10 à 3 |

Les contrats sont portés par les **assurés principaux** : un ayant droit est couvert par le
contrat de son principal, via `ThirdPartySales.ayant_droit_id`.

Les priorités `R0` / `R1` / `R2` ordonnent les payeurs (70 % / 20 % / 10 %). Cet ordre gouverne
la répartition — `TiersPayantCalculationService` trie par priorité croissante et borne chaque
part par ce que les précédents ont laissé. Le cumul ne dépasse jamais 100 %, sans quoi la part
patient deviendrait négative et le clamp à zéro masquerait l'incohérence.

Deux pièges du modèle, vérifiés sur une base réelle et documentés dans le script :

- **la colonne est `tierspayant_id`**, pas `tiers_payant_id` — l'entité déclare le champ sans
  `@JoinColumn`, Hibernate n'a pas séparé les deux mots ;
- **les `@UniqueConstraint` de `ClientTiersPayant` n'existent pas en base.** Elles ne sont
  honorées que si Hibernate génère le schéma ; ici Flyway le possède et ne les a jamais créées.
  La table n'a que sa clé primaire comme index unique. L'unicité reste donc une règle métier,
  garantie par construction et **contrôlée explicitement** par `99_verification.sql` — puisque
  la base ne la protège pas.

### Commandes fournisseurs

| | Nombre | |
|---|---|---|
| Commandes | 120 | sur 180 jours |
| dont soldées | 75 | `CLOSED` |
| dont réceptionnées | 25 | `RECEIVED` |
| dont en attente | 20 | `REQUESTED` |
| Lignes | ~2 000 | 10 à 25 par commande |

La commande est passée **chez l'agence** quand le grossiste en a, mais ses lignes pointent un
`fournisseur_produit` du **principal** : la jointure commande → ligne traverse le lien parent.
C'est le piège central du modèle fournisseur, et `99_verification.sql` le contrôle.

Une commande sur cinq est **partiellement servie** — les reliquats sont un cas réel du métier.

Trois particularités du modèle, vérifiées dans le code :

- **`order_line.order_amount` et `gross_amount` n'existent pas** comme colonnes : ce sont des
  `@Formula`, calculées à la lecture. Seul l'en-tête porte des totaux.
- **La base de calcul des totaux change à la réception** : quantité *commandée* tant que la
  commande est `REQUESTED`, quantité *reçue* ensuite
  (`CommandServiceImpl.computeCommandeAmount`). Une commande partiellement servie a donc un
  `gross_amount` inférieur à la somme de ses lignes commandées — c'est correct.
- **`ht_amount` n'est pas un montant hors taxe.** Malgré son nom, `buildDeliveryReceipt` lui
  affecte le montant du bon de livraison, égal à `gross_amount`. Il vaut zéro tant que la
  commande n'est pas réceptionnée.

### Lots, péremptions et stock

Les lots naissent des lignes réceptionnées, pour les seuls produits suivis par lot. La somme
des quantités d'une ligne égale sa quantité reçue : le stock ne sort pas de nulle part.

**Les péremptions sont tirées d'une cohorte, puis bornées à `receipt_date + 15 jours`** — on ne
réceptionne pas une marchandise déjà périmée. Le statut est ensuite **dérivé** de la date
obtenue, jamais posé indépendamment : ils ne peuvent donc pas se contredire.

| Statut | Part | Rôle |
|---|---|---|
| `AVAILABLE` | ~85 % | dont ~10 % à moins de 90 jours (alertes) |
| `EXPIRED` | ~14 % | écran « Lots périmés » |
| `DESTROYED` | ~4 % | écran « Lots à détruire » |

Des **lots historiques** (`order_line_id` nul) complètent le tableau : 180 jours de commandes ne
peuvent pas produire de marchandise périmée depuis longtemps. La colonne étant nullable, c'est un
état que l'application sait représenter — ces lots n'ont simplement pas de `lot_reception`.

**Sérials FMD** sur les lots « scannés » : petite quantité (1 à 3 unités), et uniquement sur des
produits sur ordonnance. Un sérial identifie **une boîte**, pas un lot de fabrication — en poser
un sur un lot de 200 unités passerait en base mais serait sémantiquement faux.

Trois particularités du modèle :

- **`lot.prixachat` et `lot.prixunit` s'écrivent sans underscore**, alors que
  `lot_reception.prix_achat` en a un. `Lot` déclare ses champs sans `@Column`, `LotReception`
  avec. Même piège que `tierspayant_id`.
- **La réception fait trois choses ensemble** (`StockEntryServiceImpl.mergeLots`) :
  `IN_PROGRESS → AVAILABLE`, création du `lot_reception`, crédit du `lot_stock_location` sur le
  stockage **principal**. Elles doivent être présentes ou absentes ensemble.
- **`stock_produit.qty_stock` est un stock PHYSIQUE.** Les lots périmés y sont comptés : rien
  dans l'application ne décrémente le stock quand un lot périme, la marchandise reste en rayon
  jusqu'à destruction ou ajustement. Une part du stock est donc invendable — c'est ce qui donne
  matière aux écrans de péremption.

La réserve se remplit par **transfert** depuis le rayon, jamais directement : les lots entrent
toujours par le stockage principal.

### Ventes, tiers-payant et dépôt

| | Volume |
|---|---|
| Ventes | ~3 850 sur 154 jours ouverts (fermeture le lundi) |
| Répartition | 50 % comptant, 50 % tiers-payant |
| Affluence | samedi 40, semaine 25, dimanche de garde 10 |
| Ventes dépôt | ~50 transferts, en sus |

**Consommation FEFO sans boucle.** Les lots sont ordonnés par péremption croissante et cumulés,
les lignes de vente cumulées chronologiquement ; une ligne consomme un lot quand leurs
intervalles se chevauchent. C'est l'équivalent ensembliste d'une boucle chronologique.

**La règle des 90 jours est satisfaite par construction** : seuls les lots dont la péremption
dépasse *aujourd'hui + 90* sont éligibles. Toute vente de l'historique étant antérieure à
aujourd'hui, `expiry > date_vente + 90` est vrai mécaniquement.

**Les prix étant des multiples de 5 F**, l'arrondi caisse est neutre sur les ventes comptant :
`amount_to_be_paid = net_amount` exactement, et le contrôle V2b de l'audit CA (« l'encaissement
déclaré ne dépasse pas le CA déclaré ») passe avec égalité.

Une vente tiers-payant couverte à 100 % a `part_assure = 0` : elle est réglée **sans
encaissement**. Ce n'est pas un oubli.

**La vente dépôt n'est pas une vente, c'est un transfert facturé** : le stock sort de l'officine
et entre au dépôt. Elle porte `ca = CA_DEPOT`, un montant déclarable nul *au niveau de la vente*
alors que ses lignes gardent le leur, reste `IMPAYE` par construction et n'a aucun règlement. Le
stock du dépôt est **sans lot**.

Une bizarrerie est reproduite fidèlement : `SaleDepotExtensionImpl` pose
`nature_vente = ASSURANCE` sur une vente dépôt, avec un `//TODO a supprimer`. Sémantiquement
faux, mais c'est ce que produit l'application.

---

## Vérification

`99_verification.sql` est la **spécification exécutable** des invariants du plan. Il affiche
un tableau `section / nom / statut / detail` et **sort en erreur** dès qu'un contrôle échoue,
ce qui interrompt `run_all.sql`.

Il se lance aussi seul, pour contrôler une base déjà chargée :

```bash
psql -U pharma_smart -d pharma_smart_demo -f 99_verification.sql
```

Contrôles couverts à ce stade : configuration, fournisseurs (hiérarchie à deux niveaux, cas
de repli), produits (colonnes obligatoires, marge positive, unicité, enums, déconditionnement),
`fournisseur_produit` (**jamais rattaché à une agence**, unicité du CIP, principal désigné),
rayons, et intégrité des référentiels après reset.

La liste des contrôles à activer aux étapes suivantes figure en fin de fichier.

---

## Réinitialiser

Rejouer `run_all.sql` : le reset est en tête d'enchaînement, les scripts sont donc
rejouables sans accumulation.

Pour repartir d'une base entièrement vierge, supprimer la base et reprendre à l'étape 1 :

```bash
psql -U postgres -d postgres -c "DROP DATABASE pharma_smart_demo"
```

---

## Ce qui reste à faire

**Les scripts n'ont jamais été exécutés.** Le rôle `pharma_smart` n'a pas le droit `CREATEDB`,
et la base de développement contient de vraies données que `00_reset.sql` effacerait.

Les logiques les plus risquées ont été validées à blanc, en lecture seule :

| Vérifié | Résultat |
|---|---|
| Unicité des 550 libellés produits générés | 0 doublon |
| Étalement des prix par famille | chaque famille couvre sa fourchette |
| Répartition des contrats tiers-payant | 170 contrats, cumul de taux ≤ 100 % |
| Allocation FEFO par recouvrement d'intervalles | chaque ligne couverte, ordre respecté |
| Cohortes de péremption | 0 lot périmé avant sa réception |
| Concordance des calendriers ventes / caisses | 0 jour de vente sans caisse |

Ce qui n'est **pas** vérifié : les `INSERT` eux-mêmes, les contraintes réelles, et les
218 contrôles de `99_verification.sql`, qui ne se déclenchent qu'à l'exécution.

Pour débloquer, avec un compte superutilisateur :

```bash
psql -U postgres -d postgres -v ON_ERROR_STOP=1 -f create_database.sql
```

puis Flyway, puis `run_all.sql`.
