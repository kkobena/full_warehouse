# Plan — Génération de données de démonstration

**Date :** 2026-08-26
**Branche :** `seed_demo`
**Statut :** scripts écrits (étapes 1 à 8) — **non encore exécutés**, voir `scripts/demo-data/`

> **Révision 2.** La première version de ce plan a été écrite sans lire
> `pharmaSmart-domain/.../domain` : noms de colonnes, types et cardinalités y étaient inventés
> (`code_cip` sur `produit`, prix décimaux, `magasin_id` sur `produit`, clés primaires simples…).
> Tout ce qui suit est dérivé des entités et vérifié contre elles. Les documents satellites de la
> révision 1 (`DEMO-DATA-STRUCTURE.md`, `DEMO-DATA-GUIDE.md`, `INDEX-DEMO-DATA.md`,
> `DEMO-DATA-README.md`) ont été supprimés : ils propageaient le même schéma fictif.

---

## 1. Objectif et contraintes

Produire un jeu de données de pharmacie **cohérent** permettant d'alimenter les dashboards, les
~80 services de rapport et l'ensemble des menus.

Trois contraintes structurent la solution :

1. **Hors Flyway.** Les scripts ne sont pas des migrations. Ils vivent dans `scripts/demo-data/`,
   ne s'exécutent jamais au démarrage, et sont lancés à la demande.
2. **Cohérence arithmétique.** Chaque agrégat doit être égal à la somme de ses composants, selon
   les formules réellement implémentées par le code métier (§4).
3. **Conformité au modèle.** Colonnes obligatoires, types exacts, contraintes d'unicité, clés
   composites, discriminateurs d'héritage (§3).

---

## 2. État des lieux

`V1.0.2__referentiels.sql` fournit déjà : magasin 1, utilisateurs `system`/`anonymoususer`/`admin`,
autorités, `storage` 1 (PRINCIPAL) et 3 (SAFETY_STOCK), `tva` (0 / 18 / 9), `categorie`,
`famille_produit` (24), `form_produit` (19), `payment_mode` (8), `rayon` 2 et 3.

Manque toute la donnée métier : `produit`, `fournisseur`, `fournisseur_produit`, `stock_produit`,
`lot`, `customer`, `sales`, `sales_line`, `payment_transaction`, `commande`, `order_line`,
`tiers_payant`, `facture`.

**Ce qui existe est un socle suffisant** : les scripts de démo s'y adossent et ne redéfinissent
aucun référentiel.

---

## 3. Le modèle réel — points qui invalident toute approche naïve

### 3.1 Les montants sont des entiers (FCFA)

`Produit.costAmount`, `regularUnitPrice`, `netUnitPrice`, `Sales.salesAmount`,
`SalesLine.salesAmount`, `OrderLine.orderUnitPrice`… sont tous `Integer`. Aucune décimale.
La devise est le FCFA (cf. `Fournisseur.palierRfa`, « Seuil CA annuel (FCFA) »).
Les ordres de grandeur réalistes sont donc de 500 à 50 000 F pour un article, pas 0,45.

### 3.2 `produit` n'a ni code CIP, ni magasin

`Produit` (`pharmaSmart-domain/.../domain/Produit.java:42`) :

| Colonne | Type | Obligatoire | Remarque |
|---|---|---|---|
| `libelle` | varchar | **oui** | unique avec `type_produit` |
| `type_produit` | varchar(15) | **oui** | `DETAIL` \| `PACKAGE` |
| `cost_amount` | int | **oui** | prix d'achat unitaire |
| `regular_unit_price` | int | **oui** | prix de vente unitaire |
| `net_unit_price` | int | **oui** | |
| `item_qty` | int | **oui** | défaut 1, ≥ 0 |
| `item_cost_amount` | int | **oui** | défaut 0 |
| `item_regular_unit_price` | int | **oui** | défaut 0 |
| `prix_mnp` | int | **oui** | défaut 0 |
| `deconditionnable` | bool | **oui** | défaut false |
| `created_at` / `updated_at` | timestamp | **oui** | |
| `status` | varchar(10) | **oui** | `ENABLE` \| `DISABLE` \| `DELETED` \| `CLOSED` |
| `tva_id` | fk | **oui** | `optional = false` |
| `famille_id` | fk | **oui** | `optional = false` |
| `code_ean_labo` | varchar(13) | non | |
| `statut_legal` | varchar(20) | non | `SANS_LISTE`, `LISTE_I`, `LISTE_II`, `STUPEFIANTS`, `PSO` |
| `classe_criticite` | varchar(10) | non | `A_PLUS`, `A`, `B`, … |
| `code_remise` | varchar(6) | non | `CODE_0`…`CODE_8`, `NONE` |

**Contrainte d'unicité `(libelle, type_produit)`** : deux produits ne peuvent pas partager le même
libellé pour le même type. Les libellés générés doivent donc être uniques.

Le **code CIP est porté par `fournisseur_produit`**, pas par `produit`.

### 3.3 Fournisseurs : principal et agences

`Fournisseur` est auto-référençant (`Fournisseur.java:70`). Le javadoc de la classe est explicite :

> Un fournisseur avec `parent_id = null` est un fournisseur principal (ex-`GroupeFournisseur`).
> Un fournisseur avec `parent_id` non-null est une agence rattachée au fournisseur principal.
> Les `FournisseurProduit` (codes/prix) sont **toujours** attachés au fournisseur principal.
> […] les commandes sont passées chez les agences si le grossiste a des agences, sinon chez le
> grossiste directement.

Conséquences, confirmées par `V1.7.1__fournisseur_groupe_to_principal.sql` (qui *supprime* tout
`fournisseur_produit` rattaché à une agence, lignes 130-133) :

| Règle | Portée |
|---|---|
| `fournisseur_produit.fournisseur_id` → **toujours un principal** | invariant dur |
| `commande.fournisseur_id` → **une agence** si le principal en a, sinon le principal | règle métier |
| `order_line.fournisseur_produit_id` → FP du **principal** de l'agence commandée | jointure indirecte |
| `libelle` est **unique** sur toute la table (principaux et agences confondus) | contrainte SQL |
| Délais : agence d'abord, repli sur le parent | `SuggestionProduitServiceImpl.java:547` |
| Regroupement comptable fournisseur : par parent | `AccountsPayableServiceImpl.java:383` |

C'est le piège principal du jeu de données : une commande passée chez « LABOREX Abidjan Sud »
(agence) porte des lignes dont le `fournisseur_produit` appartient à « LABOREX-CI » (principal).
Générer des `fournisseur_produit` sur les agences produirait une base que la migration V1.7.1
aurait nettoyée — donc un état incohérent avec le code de production.

`groupe_fournisseur` reste présent en base pour compatibilité mais **n'est plus le modèle** : les
5 groupes du référentiel doivent devenir des fournisseurs principaux.

### 3.4 Clés composites et identifiants assignés à la main

`Sales`, `SalesLine`, `Commande`, `OrderLine`, `PaymentTransaction` implémentent
`Persistable<XxxId>` avec `IdClass` : PK composite, **et pas de `GeneratedValue`**.

| Entité | PK | FK entrante |
|---|---|---|
| `sales` | `(id, sale_date)` | — |
| `sales_line` | `(id, sale_date)` | `(sales_id, sales_sale_date)` |
| `payment_transaction` | `(id, transaction_date)` | `(sale_id, sale_date)` |
| `commande` | `(id, order_date)` | — |
| `order_line` | `(id, order_date)` | `(commande_id, commande_order_date)` |
| `lot` | `id` (identity) | `(order_line_id, commande_order_date)` |

Les séquences dédiées existent déjà (`V1.0.4__id_generator.sql`) et **doivent être utilisées** :
`id_sale_seq`, `id_sale_item_seq`, `id_commande_seq`, `id_order_line_seq`, `id_transaction_seq`,
`id_mvt_produit_seq`.

⚠️ La colonne de date fait partie de la PK **et** de la FK : `sales_line.sale_date` (sa propre PK)
et `sales_line.sales_sale_date` (FK vers la vente) doivent valoir la même date que
`sales.sale_date`.

### 3.5 Héritage SINGLE_TABLE

Trois hiérarchies partagent une table et se distinguent par `dtype` :

| Table | `dtype` | Colonnes propres |
|---|---|---|
| `sales` | `CashSale` | `account_id` |
| `sales` | `ThirdPartySales` | `num_bon`, `ayant_droit_id`, `part_assure`, `part_tiers_payant`, `has_price_option` |
| `sales` | `VenteDepot` | `depot_id` (non nul) — voir §4.6 |
| `customer` | `UninsuredCustomer` | — |
| `customer` | `AssuredCustomer` | `sexe`, `dat_naiss`, `assure_principal_id`, `num_ayant_droit` |
| `payment_transaction` | `SalePayment` | `sale_id`, `sale_date`, `part_assure`, `part_tiers_payant` |

`Sales` ne déclare pas de `DiscriminatorColumn` mais mappe `dtype` en lecture seule
(`insertable = false, updatable = false`) : l'insertion SQL directe **doit** renseigner `dtype`
explicitement, sinon les ventes ne se rattachent à aucune sous-classe et disparaissent des écrans.

### 3.6 Colonnes calculées côté base

`OrderLine.orderAmount` et `OrderLine.grossAmount` sont des `Formula` :

```java
Formula("quantity_requested*order_unit_price")  private Integer orderAmount;
Formula("quantity_requested*order_cost_amount") private Integer grossAmount;
```

Elles **ne s'insèrent pas**. `commande.gross_amount` (obligatoire) doit valoir
`Σ (quantity_requested × order_cost_amount)` — sinon l'écran commande affiche un total qui
contredit ses propres lignes.

De même `StockProduit.totalStockQuantity` est `Formula("qty_ug+qty_stock")`.

### 3.7 Les lots : trois tables, deux niveaux de quantité

**Décision : la gestion de lot est activée pour la démo** (cf. §4.9).

Trois tables, et non une :

| Table | Portée | Colonnes clés |
|---|---|---|
| `lot` | quantité **totale** du lot, tous stockages confondus | `current_quantity`, `quantity`, `statut`, `expiry_date`, `num_lot`, `prix_achat`, `prix_unit`, `produit_id`, `serial_number` |
| `lot_stock_location` | quantité du lot **par stockage** | `lot_id`, `storage_id`, `qty` — unique `(lot_id, storage_id)` |
| `lot_reception` | **trace de chaque réception** d'un lot | `lot_id`, `(order_line_id, commande_order_date)`, `quantity_received`, `prix_achat`, `receipt_date` — tous obligatoires sauf `receipt_date` |

`lot_reception` existe parce qu'un même lot peut arriver sur plusieurs bons (livraisons
partielles, réassorts du même fabricant) : le couple `(lot, order_line)` n'est donc pas unique dans
le temps. Toute réception finalisée doit produire sa ligne.

L'invariant qui les relie est explicite dans `LotStockLocationRepository.sumQtyByLot` :

```
lot.current_quantity = Σ lot_stock_location.qty  (sur tous les stockages du lot)
```

Trois pièges :

1. **Les lignes à zéro sont supprimées, pas conservées.** `LotStockLocationServiceImpl.debit:44`
   supprime la ligne quand `qty` tombe à 0, et `deleteZeroQtyByLot` nettoie les reliquats. Un lot
   épuisé n'a donc **aucune** ligne `lot_stock_location`, tout en conservant
   `current_quantity = 0` et `statut = 'SOLD'`.
2. **`lot.statut` est asservi à `current_quantity`** (`LotServiceImpl.updateLots:238`) :
   `current_quantity ≤ 0 ⟹ SOLD`, et une recréditation repasse en `AVAILABLE`.
3. **`lot.quantity` ≠ `current_quantity`** : `quantity` est la quantité reçue à l'origine,
   `current_quantity` le restant. Le premier ne bouge jamais.

Une contrainte `CHECK` restreint le statut (`V1.5.8__gestion_lot_produit.sql:10`) :
`IN_PROGRESS`, `AVAILABLE`, `SOLD`, `EXPIRED`, `DESTROYED`.

`produit.gestion_lot` (booléen, défaut `TRUE`) permet de désactiver le contrôle **produit par
produit**, indépendamment du paramètre global.

**Cycle de vie à la réception.** Un lot naît en `IN_PROGRESS` et ne devient `AVAILABLE` qu'à la
finalisation de la réception (`StockEntryServiceImpl.mergeLots:1041`), qui fait trois choses
ensemble — les trois doivent être présentes ou absentes ensemble dans le jeu de données :

```
lot.statut : IN_PROGRESS → AVAILABLE
+ création de la ligne lot_reception
+ crédit de lot_stock_location sur le stockage PRINCIPAL
```

Les lots entrent donc **toujours par le stockage principal** (`storage_id = 1`), jamais directement
en réserve : le passage en réserve relève d'un transfert (`transferFefo`).

Corollaire pour les commandes encore ouvertes : une commande `REQUESTED` non réceptionnée ne doit
avoir **ni** `lot_reception`, **ni** `lot_stock_location`, et ses éventuels lots restent
`IN_PROGRESS`.

### 3.8 Dépendances dures à respecter

`PaymentTransaction.cashRegister` est `optional = false` : **aucun paiement sans caisse**.
`CashRegister.cashFund` est `NotNull`. La chaîne à construire avant toute vente payée est donc :

```
AppUser → CashFund → CashRegister → SalePayment → Sales
```

Autres contraintes d'unicité à respecter :

- `customer.code` unique, non nul
- `fournisseur.libelle` unique
- `fournisseur_produit` : unique `(produit_id, fournisseur_id)` **et** `(code_cip, fournisseur_id)`
- `stock_produit` : unique `(storage_id, produit_id)`
- `sales_line` : unique `(produit_id, sales_id, sale_date)` → **un produit une seule fois par vente**
- `order_line` : unique `(commande_id, fournisseur_produit_id, order_date)`
- `lot` : unique `(num_lot, produit_id)`
- `commande` : unique `(receipt_reference, fournisseur_id, order_date, order_status)`

---

## 4. Invariants de cohérence

Extraits de `SaleCommonService.updateAmounts` (`SaleCommonService.java:115`) et
`SalesLineServiceImpl.setCommonSaleLine` (`SalesLineServiceImpl.java:100`). Ce sont les règles que
le jeu de données doit satisfaire **exactement**.

### 4.1 Ligne de vente

```
sales_line.tax_value    = tva.taux du produit          (entier, en %)
sales_line.cost_amount  = produit.cost_amount          (coût UNITAIRE, pas le total)
sales_line.regular_unit_price = net_unit_price = produit.regular_unit_price
sales_line.sales_amount = quantity_requested × regular_unit_price
sales_line.amount_to_be_taken_into_account = sales_amount
sales_line.quantity_sold ≤ quantity_requested
```

### 4.2 Vente — agrégation ligne à ligne

```
ht_ligne  = tax_value = 0 ? sales_amount
                          : ceil(sales_amount / (1 + tax_value/100))
tva_ligne = sales_amount − ht_ligne

sales.sales_amount   = Σ sales_line.sales_amount
sales.cost_amount    = Σ (quantity_requested × sales_line.cost_amount)
sales.ht_amount      = Σ ht_ligne
sales.tax_amount     = Σ tva_ligne
sales.discount_amount= Σ sales_line.discount_amount
sales.net_amount     = sales_amount − discount_amount
sales.amount_to_be_taken_into_account = sales_amount
```

> **Le HT s'arrondit par ligne, au plafond, puis se somme.** Calculer le HT globalement à partir du
> total TTC donne un écart de quelques francs et casse les rapports TVA. L'agrégation doit être
> `SUM(CEIL(...))`, jamais `CEIL(SUM(...))`.

### 4.3 Numérotation — la table `reference`

`sales.number_transaction` n'est pas libre. `SaleCommonService.buildReference:196` le compose ainsi :

```
number_transaction = format(date_du_jour, 'yyyyMMdd') || lpad(compteur, 3, '0')
```

où `compteur` vient de la table **`reference`**, qui tient un compteur **par jour et par type**
(`ReferenceService.buildReference:68`, clé `(mvt_date, type)`), incrémenté à chaque appel.

Conséquence pour la démo : il ne suffit pas d'inventer des numéros de transaction plausibles, il
faut aussi **laisser la table `reference` dans l'état correspondant**. Sinon la première vente
saisie après chargement repart du compteur 1 et réémet un numéro déjà présent.

```
pour chaque jour D et chaque type utilisé :
    reference(mvt_date = D, type = T).number_transac = nombre d'objets émis ce jour-là
```

`reference.type` est un **entier**, pas une chaîne (`TypeReference`) :

| Type | Valeur | Concerné par la démo |
|---|---|---|
| `VENTE` | `0` | oui |
| `COMMANDE` | `1` | oui |
| `PREVENTE_VENTE` | `2` | non |
| `SUGGESTION` | `3` | si suggestions générées |
| `TRANSACTION` | `4` | oui |
| `REASSORT` | `5` | non |
| `AVOIR_CLIENT` | `6` | si avoirs générés |
| `RETOUR_CLIENT` | `7` | si retours générés |

**Nuance assumée.** Le code préfixe avec `LocalDate.now()`, donc la date d'émission, pas celle de
la vente. Pour un historique reconstitué, la démo préfixe avec la **date de la vente** : c'est le
seul choix qui donne des numéros lisibles et non colliding sur 180 jours.

Le padding est de 3 chiffres et ne tronque pas : au-delà de 999 émissions dans la journée le numéro
s'allonge simplement, ce qui reste dans les `varchar(20)` de la colonne.

### 4.4 Encaissement

```
amount_to_be_paid = arrondi5(net_amount)      -- vente comptant
arrondi5(x) : reste = x % 5 ; reste = 0 → x ; reste ≥ 3 → x + (5−reste) ; sinon x − reste

rest_to_pay = (amount_to_be_paid − payroll_amount) < 4 ? 0 : amount_to_be_paid − payroll_amount
payment_status = rest_to_pay = 0 ? 'PAYE' : 'IMPAYE'
monnaie ≥ 0
```

Pour une vente comptant soldée : `payroll_amount ≥ amount_to_be_paid`, `rest_to_pay = 0`,
`monnaie = payroll_amount − amount_to_be_paid`.

`Σ payment_transaction.paid_amount` sur une vente doit valoir `payroll_amount`.

### 4.5 Vente tiers-payant

**Décision : 50 % des ventes sont des ventes tiers-payant.** Ce n'est donc pas un cas marginal du
jeu de données mais son chemin dominant, à traiter avec le même soin que la vente comptant.

#### `third_party_sale_line` n'est pas une ligne d'article

C'est une **ventilation par payeur**. Une vente tiers-payant porte :

- ses `sales_line` — les articles, comme toute vente ;
- ses `third_party_sale_line` — **une par `client_tiers_payant`** intervenant au paiement.

Une même vente peut donc être répartie entre plusieurs organismes (mutuelle + complémentaire).
Contrainte d'unicité : `(client_tiers_payant_id, sale_id, sale_date)` — un payeur n'apparaît
qu'une fois par vente. PK composite `(id, sale_date)`, `IdClass(AssuranceSaleId)`, identifiant
assigné à la main (séquence `id_sale_assurance_item_seq`).

Colonnes obligatoires : `sale_id`/`sale_sale_date`, `client_tiers_payant_id`, `montant`,
`created_at`, `updated_at`, `effective_update_date`, `statut`, `taux`, `taux_vente`.

#### Les deux « taux » ne sont pas le même

Piège central, vérifié dans `TiersPayantCalculationService.calculateFinalTaux:406` :

| Colonne | Sens | Valeur |
|---|---|---|
| `taux_vente` | taux **contractuel** appliqué au calcul | `client_tiers_payant.taux` (ou surcharge de la vente) |
| `taux` | taux **effectif** constaté après calcul | `round_half_down(montant × 100 / sales_amount)` |

Les deux ne coïncident que s'il n'y a ni plafond, ni prix d'option, ni second payeur. Recopier
`taux_vente` dans `taux` produirait un jeu de données faux dès qu'une vente est multi-payeur.

#### Ordre de répartition : par priorité

`calculateSaleItem:166` trie les payeurs par `client_tiers_payant.priorite` croissante
(`R0`=0, `R1`=1, `R2`=2, `R3`=3), puis, **article par article** :

```
base       = prix_de_référence_ou_regular_unit_price × quantité
restant    = base − Σ parts déjà attribuées sur cet article
si restant ≤ 0 : le payeur suivant ne reçoit rien

part = (taux = 100 % et nature = ASSURANCE)          -- « formule confort »
         ? max(sales_amount_ligne − Σ parts déjà attribuées, 0)
         : min(base × taux, restant)
```

La part d'un payeur est donc **bornée par ce que les payeurs plus prioritaires ont laissé**. Un
jeu de données qui appliquerait bêtement `montant = total × taux` à chaque payeur produirait des
sommes supérieures au montant de la vente.

#### Agrégation et part patient

```
third_party_sale_line.montant = plafonné(Σ parts de ce payeur sur tous les articles)
sales.part_tiers_payant       = Σ third_party_sale_line.montant
```

La part patient dépend de `nature_vente` (`calculatePatientShare:361`) :

```
ASSURANCE : part_assure = max(sales_amount − part_tiers_payant − discount_amount, 0)
CARNET    : part_assure = max((sales_amount − discount_amount) − part_tiers_payant, 0)
```

Les deux se réduisent à `part_assure + part_tiers_payant = net_amount`, **sauf** si le clamp à zéro
s'active (part tiers-payant supérieure au net). Le jeu de données reste sous ce seuil : aucun taux
cumulé ne dépasse 100 %.

```
amount_to_be_paid = arrondi5(part_assure)       -- le client ne règle que sa part
```

Une vente couverte à 100 % a donc `part_assure = 0` et `amount_to_be_paid = 0` : elle est `PAYE`
sans encaissement. Le jeu de données en comporte (≈ 15 % des ventes tiers-payant), et l'assertion
sur les paiements doit les tolérer.

#### Plafonds

`applyCeilings` borne la part de chaque payeur par le plafond mensuel
(`tiers_payant.plafond_conso` vs `client_tiers_payant.conso_mensuelle`) et le plafond journalier
(`tiers_payant.plafond_journalier_client`). Le jeu de données configure des plafonds sur **2 ou 3
organismes** afin que l'écran de consommation ne soit pas uniformément vide, en veillant à ce que
`montant` reste cohérent avec le plafond annoncé.

#### `repartitions` — ventilation TVA du payeur

Colonne `jsonb`, tableau de `RepartitionTiersPayantParTva(montantTtc, montantTva, montantNet,
montantHt, tva)` — noter que ces champs sont des **`double`**, contrairement aux montants entiers
partout ailleurs. Par taux de TVA présent dans la vente :

```
montantTtc = part du payeur sur les articles à ce taux
montantHt  = tva = 0 ? montantTtc : montantTtc / (1 + tva/100)
montantTva = montantTtc − montantHt
montantNet = montantTtc
Σ repartitions[].montantTtc = third_party_sale_line.montant
```

#### `client_tiers_payant` — historique auto-alimenté par JPA

`PrePersist` (`ClientTiersPayant.java:113`) insère d'office une entrée dans
`taux_historique` à la création. Une insertion SQL directe court-circuite ce hook : les scripts
doivent **écrire eux-mêmes** le JSON, sinon l'historique des taux est vide là où l'application en
aurait produit un.

`ClientTiersPayantTauxHistorique` est un record `(LocalDateTime updatedAt, int taux)` — le champ
JSON est donc `updatedAt` :

```json
taux_historique : [{"updatedAt": "2026-03-14T09:12:00", "taux": 80}]
```

`consommation_json` (sur `client_tiers_payant` **et** sur `tiers_payant`) sérialise des
`Consommation(id, month, year, consommation)` — `month` est un `short`, `consommation` un `long` :

```json
consommation_json : [{"id": 1, "month": 3, "year": 2026, "consommation": 145000}]
```

Ces cumuls doivent concorder avec les ventes réellement générées sur le mois, sinon l'écran de
consommation contredit le journal des ventes :

```
consommation(mois M) = Σ third_party_sale_line.montant du payeur sur M
```

⚠️ Noter la différence de type de colonne entre les deux tables :
`client_tiers_payant.consommation_json` est déclaré `json`, `tiers_payant.consommation_json` est
`jsonb`. Sans conséquence à l'insertion, mais à respecter dans les casts explicites.

#### Deux pièges sur `client_tiers_payant`

**1. La colonne s'appelle `tierspayant_id`, pas `tiers_payant_id`.** L'entité déclare
`private TiersPayant tiersPayant;` sans `@JoinColumn` : Hibernate dérive le nom du champ, sans
séparer « tiers » de « payant ».

**2. Les contraintes d'unicité déclarées dans l'entité n'existent pas en base.**
`@Table(uniqueConstraints = {(tiers_payant_id, assured_customer_id), (tiers_payant_id, num)})`
n'est honoré que si Hibernate génère le schéma. Ici c'est Flyway qui le possède, et
`V1.0.1__init.sql` ne les a jamais créées : la table n'a que sa clé primaire comme index unique.

Conséquences pour les scripts :

- impossible d'écrire `ON CONFLICT (tierspayant_id, assured_customer_id)` — il n'y a pas d'index
  sur lequel s'appuyer ; il faut un `WHERE NOT EXISTS` ;
- l'unicité reste une **règle métier** que l'application tient pour acquise. Le jeu de données doit
  donc la respecter, et `99_verification.sql` doit la contrôler explicitement — précisément parce
  que la base ne le fera pas.

### 4.6 Vente dépôt

**Décision : le jeu de données comporte un magasin de type `DEPOT`.**

#### `VenteDepot` est une troisième sous-classe de `Sales`

Au-delà de `CashSale` et `ThirdPartySales`, `sales` porte un troisième `dtype` : `VenteDepot`,
avec une colonne propre `depot_id` (**non nulle**) pointant le magasin dépôt.

#### Ce n'est pas une vente, c'est un transfert facturé

`SaleDepotExtensionImpl.updateDepotStockOnSaleFinalization:294` et
`StockUpdateService.updateStockDepot:109` révèlent le vrai mouvement :

```
stock officine (storage PRINCIPAL du magasin 1)   −= quantité   (chemin normal de la ligne de vente)
stock dépôt    (storage PRINCIPAL du magasin dépôt) += quantité  (updateStockDepot)
```

Une « vente dépôt » **déplace** donc le stock de l'officine vers le dépôt, et enregistre une
créance. C'est un approvisionnement de succursale, pas une vente à un patient.

#### Montants et statuts

```
amount_to_be_paid  = arrondi5(net_amount)
rest_to_pay        = amount_to_be_paid          -- posé en dur à la finalisation
payment_status     = 'IMPAYE'                   -- toujours, par construction
ca                 = 'CA_DEPOT'
amount_to_be_taken_into_account = 0             -- au niveau de la VENTE
customer_id        = NULL
```

Trois conséquences que le jeu de données doit respecter :

1. **Une vente dépôt n'a aucun `payment_transaction`.** Elle reste due. L'assertion « toute vente à
   encaisser a un règlement » doit l'exclure explicitement.
2. **Elle sort du CA déclaré.** `DeclarationCaServiceImpl:69` : « Les ventes dépôt portent
   `ca = CA_DEPOT` : elles n'entrent pas dans le CA déclaré. » Son
   `amount_to_be_taken_into_account` vaut zéro *au niveau de la vente*, alors que ses
   `sales_line` conservent le leur — c'est précisément pourquoi le contrôle V2 de
   `AuditDeclarationCaService` écarte ces ventes. Ne pas « corriger » cette asymétrie.
3. **Elle est tout de même rattachée à une caisse** (`finalizeSale:280` pose un `cash_register`),
   bien qu'aucun encaissement ne lui corresponde.

#### Une bizarrerie à reproduire fidèlement

`SaleDepotExtensionImpl:115` pose `nature_vente = ASSURANCE` sur une vente dépôt, avec le
commentaire `//TODO a supprimer`. C'est incohérent sémantiquement, mais c'est ce que produit
l'application : le jeu de données doit l'imiter, sous peine de ne pas ressembler à une base réelle.
À revoir le jour où le TODO est traité.

#### Le magasin dépôt a besoin de son propre stockage

`Magasin.primaryStorage` est une `@JoinFormula` :

```sql
SELECT s.id FROM storage s WHERE s.storage_type = 'PRINCIPAL' AND s.magasin_id = id LIMIT 1
```

Le magasin dépôt doit donc avoir **sa propre ligne `storage`** de type `PRINCIPAL`, sans quoi
`depot.getPrimaryStorage()` renvoie `null` et la finalisation échoue. `magasin.name` et
`magasin.full_name` sont **uniques** : le dépôt ne peut pas réutiliser ceux de l'officine.

Le stock du dépôt vit dans `stock_produit` sur ce stockage (unique `(storage_id, produit_id)`).

#### Le stock du dépôt est sans lot

`updateStockDepot` ne touche ni `lot` ni `lot_stock_location` : il incrémente `qty_stock`
directement. Le stock dépôt est donc **hors gestion de lot**, quelle que soit la valeur de
`produit.gestion_lot`.

C'est une contrainte forte sur l'invariant du §4.9 : l'égalité
`stock_produit.qty_stock = Σ lot_stock_location.qty` ne vaut **que pour les stockages de
l'officine**. Appliquée au stockage du dépôt, elle échouerait sur toutes les lignes.

#### Retours dépôt

`retour_depot` / `retour_depot_item` tracent le mouvement inverse. Colonnes obligatoires :
`date_mtv`, `user_id`, `depot_id` sur l'en-tête ; `qty_mvt` (**≥ 1**), `regular_unit_price`,
`produit_id`, `init_stock` sur l'item. Le lien vers la vente d'origine
(`vente_depot_id`, `vente_depot_date`) est **facultatif** — un retour peut être hors vente.

### 4.7 Commande

**`order_amount` et `gross_amount` n'existent pas comme colonnes de `order_line`** : ce sont des
`@Formula`, calculées à la lecture. Vérifié sur la base — la table ne les porte pas.

```
order_line.order_amount  = quantity_requested × order_unit_price   (formule SQL, non insérée)
order_line.gross_amount  = quantity_requested × order_cost_amount  (formule SQL, non insérée)
```

#### La base de calcul change à la réception

Les totaux de l'en-tête sont recalculés sur la quantité **reçue** dès qu'une réception est
enregistrée (`CommandServiceImpl.computeCommandeAmount:301` puis `:1025`) :

| Phase | `gross_amount` | `order_amount` |
|---|---|---|
| Commande passée | Σ (`quantity_requested` × `order_cost_amount`) | Σ (`quantity_requested` × `order_unit_price`) |
| Après réception | Σ (`quantity_received` × `order_cost_amount`) | Σ (`quantity_received` × `order_unit_price`) |

Une commande partiellement servie a donc un `gross_amount` **inférieur** à la somme de ses lignes
commandées : c'est correct, et une assertion qui l'ignorerait signalerait un faux positif.

#### `ht_amount` n'est pas un montant hors taxe

Malgré son nom, `buildDeliveryReceipt:634` lui affecte le **montant du bon de livraison**, la même
valeur que `gross_amount` :

```
tant que REQUESTED :   ht_amount = 0            tax_amount = 0
après réception    :   ht_amount = gross_amount  (montant du bon)
                       final_amount = ht_amount
                       tax_amount = Σ order_line.tax_amount
```

L'égalité `ht_amount + tax_amount = order_amount` annoncée en révision 2 est **fausse** : elle
supposait une décomposition HT/TVA que le code ne fait pas ici.

#### Statuts et champs liés

| `order_status` | `receipt_date` | `receipt_reference` | `quantity_received` | `ht_amount` | Lots |
|---|---|---|---|---|---|
| `REQUESTED` | null | null | null | 0 | non |
| `RECEIVED` | renseignée | renseignée | ≤ `quantity_requested` | = `gross_amount` | oui |
| `CLOSED` | renseignée | renseignée | renseignée | = `gross_amount` | oui |

`paiment_status` (`UNPAID` / `PAID` / `NOT_SOLD`) est indépendant du statut de commande.

### 4.8 Stock

```
stock_produit.qty_stock ≥ 0
stock_produit.qty_virtual = qty_stock            (hors commande en cours)
lot.current_quantity ≤ lot.quantity
```

**Cohérence stock ↔ mouvements** : le stock final doit être le résultat des entrées et des sorties
générées, pas une valeur arbitraire :

```
qty_stock = Σ réceptions commande − Σ quantity_sold des ventes ± ajustements
```

C'est la contrainte la plus coûteuse à respecter et elle impose l'ordre de génération du §5.

### 4.9 Lots — invariants propres

La gestion de lot étant active, le stock doit se décliner **jusqu'au lot et jusqu'au stockage**.

```
-- Niveau lot
lot.current_quantity = Σ lot_stock_location.qty          (tous stockages du lot)
lot.current_quantity = 0  ⟺  lot.statut = 'SOLD'
lot.statut = 'AVAILABLE'  ⟹  current_quantity > 0 ET ≥ 1 ligne lot_stock_location
lot_stock_location.qty > 0                               (jamais de ligne à 0)

-- Raccord avec stock_produit, par couple (produit, stockage)
-- Stock PHYSIQUE : tous les lots comptent, y compris les périmés (cf. cohortes)
stock_produit.qty_stock = Σ lot_stock_location.qty
                          des lots du produit dans ce stockage
                          (si produit.gestion_lot = true
                           ET stockage appartenant à l'OFFICINE — cf. §4.6)
```

⚠️ Le stock du magasin dépôt est alimenté par `updateStockDepot`, qui ne crée aucun lot. Cet
invariant ne s'applique donc **pas** aux stockages du dépôt.

**Ordre de consommation : FEFO.** `LotRepository.findByProduitId:51` trie
`currentQuantity > 0 AND statut = 'AVAILABLE' ORDER BY expiryDate ASC`, et
`findFefoByStorageAndProduit` place les péremptions nulles en dernier. Les lots consommés par une
vente doivent donc l'avoir été par péremption croissante.

**`sales_line.lots` est un snapshot obligatoire.** Colonne `jsonb`, tableau de
`LotSold(id, numLot, quantity, expiryDate)` — un instantané immuable servant la traçabilité
(rappels, FMD). Contraintes :

```
Σ sales_line.lots[].quantity = sales_line.quantity_sold
lots[].id, lots[].numLot, lots[].expiryDate  cohérents avec la table lot
lots[] ordonné par expiryDate croissante                (FEFO)
```

**Un lot vendu devait être vendable à la date de la vente.** `APP_NOMBRE_JOUR_AVANT_PEREMPTION`
vaut 90 : un lot dont la péremption est à moins de 90 jours n'est pas mis en vente
(`findByProduitId(produitId, dateLimit, statut)` filtre `expiryDate > dateLimit`). Pour un
historique de 180 jours, cela impose :

```
pour toute ligne de vente à la date D consommant un lot L :
    L.expiry_date > D + 90 jours
```

C'est la contrainte la plus subtile du jeu de données : générer les péremptions au hasard produit
des ventes que l'application elle-même aurait refusées.

**Cohortes de péremption.** Pour alimenter les écrans « Péremptions » sans violer la règle
ci-dessus, les lots sont répartis en cohortes explicites :

| Cohorte | `expiry_date` | Part | Statut | Rôle |
|---|---|---|---|---|
| Sain | > J+270 | 55 % | `AVAILABLE` | stock courant, consommable par toute vente |
| Vigilance | J+90 → J+270 | 20 % | `AVAILABLE` | consommable par les ventes anciennes seulement |
| Alerte | J+7 → J+90 | 12 % | `AVAILABLE` | alimente `APP_EXPIRY_ALERT_DAYS_BEFORE` (30, 7) |
| Périmé | J−180 → J | 10 % | `EXPIRED` | écran « Lots périmés » |
| Détruit | < J−180 | 3 % | `DESTROYED` | + lignes `products_to_destroy` |

Les lots `EXPIRED` et `DESTROYED` gardent `current_quantity > 0` — c'est précisément ce stock
immobilisé que les rapports de péremption valorisent — mais ils sont **exclus du FEFO** puisque
celui-ci filtre sur `statut = 'AVAILABLE'`.

**Ils restent en revanche comptés dans `stock_produit.qty_stock`.** Rien dans l'application ne
décrémente le stock quand un lot périme : la marchandise est physiquement en rayon jusqu'à sa
destruction ou un ajustement. `qty_stock` est donc un stock **physique**, pas un stock vendable :

```
stock_produit.qty_stock = Σ lot_stock_location.qty        -- TOUS les lots
```

Conséquence assumée : une part du stock n'est pas vendable, et le FEFO ne peut pas la servir.
C'est la situation d'une officine qui n'a pas encore traité ses périmés, et c'est ce qui donne
matière aux écrans de péremption. Une génération qui exclurait les périmés du stock afficherait
des rapports de péremption vides tout en prétendant les alimenter.

**`products_to_destroy`** référence `fournisseur_produit` (obligatoire) et `magasin` (obligatoire),
avec `quantity ≥ 1` et `stock_initial ≥ 1`. Le `fournisseur_produit` visé appartient — encore — au
fournisseur **principal**.

### 4.10 Numéros de série FMD

**Décision : les sérials FMD sont générés sur une partie des lots.**

`lot.serial_number` (`varchar(50)`, nullable) stocke l'AI 21 du DataMatrix GS1. Une contrainte
**unique partielle** le protège (`V1.3.9__lot_serial_number_fmd.sql:9`) :

```sql
CREATE UNIQUE INDEX lot_serial_number_produit_idx
    ON lot (serial_number, produit_id) WHERE serial_number IS NOT NULL;
```

C'est-à-dire : unicité **par produit**, et les `NULL` ne se gênent pas entre eux. Un même sérial
peut donc légitimement exister sur deux produits différents — mais générer un doublon sur un même
produit fait échouer le script.

#### Les deux chemins de création de lot

Le code crée les lots de deux façons, et le sérial n'apparaît que sur l'une d'elles :

| Chemin | Code | `quantity` | `serial_number` |
|---|---|---|---|
| Réception manuelle / en masse | `StockEntryServiceImpl:786` | quantité reçue complète | **jamais** |
| Scan DataMatrix, boîte à boîte | `StockEntryServiceImpl:544` | `1` par scan (ou AI 37) | si présent **et** non doublon |

Cette asymétrie est structurante. Un sérial FMD identifie **une boîte**, pas un lot de fabrication :
poser un sérial sur un lot de 200 unités serait sémantiquement faux, même si la base l'accepte. Le
jeu de données modélise donc les deux chemins :

```
Lots « en masse »  : quantity 20 à 300, serial_number NULL          (~75 %)
Lots « scannés »   : quantity 1 à 3,    serial_number renseigné     (~25 %)
```

#### Périmètre : les produits soumis à sérialisation

La FMD ne couvre pas toute l'officine. Le champ le plus proche dans le modèle est
`produit.statut_legal` : la sérialisation est réservée aux produits sur ordonnance.

```
serial_number renseigné  ⟹  produit.statut_legal IN
        ('LISTE_I', 'LISTE_II', 'STUPEFIANTS', 'PSO')
```

Les produits `SANS_LISTE` (conseil, parapharmacie, accessoires) n'ont **aucun** lot sérialisé.

#### Format

L'AI 21 est alphanumérique, de longueur variable (terminé par le séparateur GS), et en pratique
aléatoire chez les fabricants. Format retenu, 20 caractères, sûr vis-à-vis du `varchar(50)` :

```
<préfixe fabricant 2 lettres><18 caractères alphanumériques majuscules>
exemple : SN7K3QP9XA2M4TB6RD1F
```

Aucune information métier n'y est encodée — c'est bien le cas réel. Le tirage doit garantir
l'unicité par produit (§ contrainte ci-dessus) ; un `gen_random_uuid()` tronqué et mis en
majuscules suffit, avec un `ON CONFLICT` de sécurité.

#### Interaction avec la vente scannée

`sales_line.code_scan` stocke le code scanné en caisse. `SalesLineServiceImpl:380` en tire une
conséquence forte : **si le code contient un numéro de lot (AI 10), ce lot est consommé en premier,
avant le FEFO.**

```
sales_line.code_scan contient AI 10  ⟹  lots[0] = le lot de ce numéro
                                          lots[1..n] = FEFO pour le reliquat
```

L'assertion FEFO du §8 doit donc **exempter sa première entrée** pour les lignes dont `code_scan`
porte un numéro de lot. Une ligne scannée dont le premier lot n'est pas celui du code est
incohérente ; une ligne scannée dont les lots suivants ne sont pas FEFO l'est tout autant.

Le jeu de données pose `code_scan` sur **~15 % des lignes de vente**, uniquement celles portant sur
un produit sérialisé, sous la forme d'un DataMatrix GS1 complet et cohérent avec le lot consommé :

```
01<GTIN-14>17<AAMMJJ péremption>10<num_lot><GS>21<serial>
```

où GTIN, péremption, numéro de lot et sérial reprennent **exactement** les valeurs du lot visé.
Un code de démo qui ne se reparse pas vers son propre lot serait pire qu'un `code_scan` nul.

#### Détection de doublon — ce qui est démontrable

`FmdStatus.DUPLICATE` se déclenche quand un sérial existe déjà pour le produit
(`StockEntryServiceImpl:518`), et le lot est alors créé **sans** sérial. Ce cas ne peut pas être
pré-généré : l'index unique l'interdit, et c'est précisément ce qu'il protège.

Le livrable prévoit donc, dans le README, une **liste d'une dizaine de sérials réellement en base**
avec leur produit, à rescanner en réception pour déclencher l'alerte contrefaçon en démonstration.
C'est la seule façon honnête d'exercer ce chemin.

---

## 5. Ordre de génération

L'ordre est imposé par les FK et par l'invariant §4.8. Chaque étape ne dépend que des précédentes.

```
 0. Configuration                      APP_GESTION_LOT = 1 (§9)
 0b. Magasin DEPOT + son storage PRINCIPAL   prérequis de l'étape 15b (§4.6)
 1. Fournisseurs principaux (5)        parent_id = NULL
 2. Agences (12)                       parent_id → principal
 3. Produits (600)                     famille + tva obligatoires ; gestion_lot
 4. fournisseur_produit (900)          TOUJOURS sur un principal ; code_cip unique par fournisseur
 5. produit.fournisseur_produit_principal_id   ← mise à jour après 4
 6. rayon_produit                      rattachement rayon/produit
 7. Clients (200)                      code unique ; dtype
 8. Tiers-payants + client_tiers_payant
 9. Commandes + lignes                 → ENTRÉES de stock
10. Lots                               cohortes (§4.9) ; sérials FMD sur les Rx (§4.10)
11. lot_reception + lot_stock_location  uniquement pour les commandes réceptionnées
12. stock_produit                      initialisé depuis les réceptions de 9/10/11
13. Caisses (poste, cash_fund, cash_register)
14. Ventes + lignes + paiements        → SORTIES ; FEFO, sales_line.lots, code_scan (§4.10)
15. third_party_sale_line              ventilation par payeur, priorité + plafonds (§4.5)
15b. Ventes dépôt + retours            transferts officine → dépôt, sans lot (§4.6)
16. Recalage lots + stock              current_quantity, statut, lot_stock_location, qty_stock
17. products_to_destroy                depuis les lots DESTROYED
18. Consommations                      consommation_json de client_tiers_payant et tiers_payant
19. Facturation tiers-payant           depuis les third_party_sale_line de 15
20. reference                          compteurs par jour et par type (§4.3)
21. Vérification des invariants        §8
```

L'étape 16 est le garde-fou : plutôt que de maintenir lots et stock en temps réel pendant la
génération des ventes, on les recalcule en fin de course à partir des mouvements réellement écrits.

**La ventilation par payeur vient après les ventes** (étape 15) et non pendant : `montant` et
`taux` se déduisent des `sales_line` déjà écrites (§4.5), et `sales.part_tiers_payant` /
`part_assure` se recalent ensuite sur la somme obtenue. L'étape 18 en dérive à son tour les
consommations mensuelles — trois passes successives sur la même donnée, chacune vérifiable.

**Contrainte d'ordonnancement propre aux lots.** L'étape 14 doit consommer les lots dans l'ordre
chronologique des ventes *et* en FEFO à l'intérieur de chaque vente, en ne retenant que les lots
dont la péremption satisfait `expiry_date > date_vente + 90` (§4.9). Concrètement, la génération
des ventes se fait **par date croissante**, chaque vente puisant dans l'état de stock laissé par les
précédentes — un `UPDATE` global en fin de parcours ne suffirait pas à produire un
`sales_line.lots` crédible.

**Les sérials se posent avant les ventes** (étape 10) : le `code_scan` d'une ligne de vente
(étape 14) réencode le numéro de lot *et* le sérial du lot qu'elle consomme, il lui faut donc une
cible déjà figée.

---

## 6. Volumes cibles

Dimensionnés pour couvrir tous les rapports sans alourdir la base (≈ 25 Mo, génération < 60 s).

| Entité | Volume | Justification |
|---|---|---|
| Fournisseurs principaux | 5 | reprend les 5 groupes du référentiel |
| Agences | 12 | 2 à 4 par principal, 1 principal sans agence (cas de repli) |
| `produit` | 600 | dont 60 `PACKAGE` déconditionnables |
| `dci` | 59 | substances actives ; plusieurs produits en partagent une, d'où la substituabilité |
| `substitut` | ~215 | ~135 génériques (même molécule et dosage) + ~80 thérapeutiques |
| `fournisseur_produit` | ~900 | 1 à 3 principaux par produit |
| `stock_produit` | ~700 | 600 en rayon + 100 en réserve |
| `lot` | ~2 400 | dont ~600 sérialisés (petits lots « scannés », §4.10) |
| `lot_stock_location` | ~2 000 | 1 à 2 par lot non épuisé ; aucune ligne pour les lots `SOLD` |
| `lot_reception` | ~2 200 | une par lot réceptionné ; aucune pour les commandes `REQUESTED` |
| `products_to_destroy` | ~50 | issus des lots `DESTROYED` |
| `customer` | 320 | 150 `UninsuredCustomer`, 170 `AssuredCustomer` |
| dont assurés principaux | 120 | porteurs de contrat |
| dont ayants droit | 50 | `assure_principal_id` renseigné |
| `tiers_payant` | 12 | 3 groupes ; 2-3 avec plafond configuré |
| `client_tiers_payant` | 170 | 80 assurés à 1 payeur, 30 à 2, 10 à 3 (`R0`/`R1`/`R2`) |
| `commande` | 120 | sur 180 j — 75 `CLOSED`, 25 `RECEIVED`, 20 `REQUESTED` |
| `order_line` | ~2 000 | 10 à 25 par commande |
| `magasin` | 2 | officine (existant, id 1) + **1 `DEPOT`** |
| `storage` | 3 | 2 officine (existants) + 1 `PRINCIPAL` pour le dépôt |
| `sales` | 4 000 | 180 j — **50 % `CashSale`, 50 % `ThirdPartySales`** |
| `VenteDepot` | 150 | en sus des 4 000 — transferts officine → dépôt, 2-3 par semaine |
| `stock_produit` dépôt | ~120 | produits effectivement transférés |
| `retour_depot` | 20 | ~60 `retour_depot_item` |
| `third_party_sale_line` | ~2 500 | 1 payeur (75 %) ou 2 payeurs `R0`+`R1` (25 %) |
| `sales_line` | ~9 000 | 1 à 5 par vente, produits distincts |
| `payment_transaction` | ~3 700 | ≥ 1 par vente, **sauf** les TP couvertes à 100 % |
| `cash_register` | ~180 | une par jour ouvré |
| `facture_tiers_payant` | ~60 | mensuelle par organisme actif, sur 6 mois |

**Le passage à 50 % rééquilibre le fichier client.** 2 000 ventes tiers-payant réparties sur
170 assurés donnent ~12 passages par assuré sur 180 jours, soit un tous les quinze jours : c'est le
profil d'un patient chronique, cohérent avec ce que couvre une assurance. Conserver les 80 assurés
du dimensionnement précédent aurait donné 25 passages chacun, un rythme qu'aucune officine
n'observe.

Le nombre de paiements **baisse** alors que le nombre de ventes reste constant : une vente couverte
à 100 % n'a pas d'encaissement (§4.5).

**180 jours d'historique** et non 90 : les rapports comparatifs (`ComparativeReportService`) et
l'analyse ABC/Pareto ont besoin d'une période N-1 pour produire autre chose que des zéros.

Répartition temporelle réaliste : lundi-vendredi 20-30 ventes/j, samedi 35-45, dimanche 8-12
(garde), amplitude horaire 08 h-20 h.

---

## 7. Livrables

```
scripts/demo-data/
├── README.md                     mode d'emploi, ordre, pré-requis
│                                 + liste de sérials FMD rescannables (§4.10)
├── 00_reset.sql                  purge idempotente (ordre FK inverse)
├── 01_config.sql                 APP_GESTION_LOT et clés liées (§9)
├── 02_fournisseurs.sql           principaux + agences
├── 02b_dci.sql                   substances actives, référentiel du catalogue
├── 03_produits.sql               produits + fournisseur_produit + rayon_produit + dci
├── 03b_substituts.sql            catalogue de substitution générique et thérapeutique
├── 04_clients.sql                clients + tiers-payants
├── 05_commandes.sql              commandes + lignes  (entrées de stock)
├── 06_lots.sql                   lots, cohortes de péremption, sérials FMD
├── 07_stock.sql                  lot_reception + lot_stock_location + stock_produit
├── 08_caisses.sql                poste + cash_fund + cash_register
├── 09_ventes.sql                 ventes + lignes + paiements + FEFO + code_scan
├── 10_repartitions.sql           ventilation TVA de la part de chaque payeur
├── 10b_ventes_depot.sql          magasin DEPOT, transferts, retours dépôt
├── 12_destruction.sql            products_to_destroy depuis les lots DESTROYED
├── 13_consommations.sql          consommation_json client_tiers_payant et tiers_payant
├── 14_facturation.sql            factures tiers-payant
├── 15_reference.sql              compteurs de numérotation par jour et par type
├── 16_mouvements.sql             inventory_transaction — historique des mouvements
├── 99_verification.sql           assertions sur tous les invariants du §4
└── run_all.sql                   enchaînement \i + arrêt au premier échec
```

Exécution à la demande, jamais au démarrage :

```bash
psql -U warehouse -d warehouse -v ON_ERROR_STOP=1 -f scripts/demo-data/run_all.sql
```

Les scripts posent `SET search_path TO :schema` en tête et acceptent
`-v schema=pharma_smart`, pour ne pas coder le schéma en dur (règle CLAUDE.md).

### Idempotence — base dédiée, purge franche

**Décision : la démo tourne sur une base dédiée.** Pas de plage d'identifiants réservée, pas de
marqueur d'origine : `00_reset.sql` vide les tables métier et laisse repartir les compteurs à zéro.

Le reset **doit épargner les référentiels posés par Flyway** (§2), sinon les scripts suivants n'ont
plus de magasin, d'utilisateur, de TVA ni de menu sur quoi s'appuyer. Plutôt que d'énumérer les
~100 tables métier — liste qui se périmerait à la première migration — on énumère les tables à
**conserver**, courte et stable, et on vide tout le reste :

```sql
DO $$
DECLARE
    v_keep   CONSTANT text[] := ARRAY[
        -- Flyway lui-même
        'pharma_smart_history',
        -- Structure officine
        'magasin', 'storage', 'rayon', 'tableau',
        -- Comptes et droits
        'app_user', 'authority', 'user_authority',
        -- Référentiels produit
        'tva', 'categorie', 'famille_produit', 'form_produit',
        -- Paramétrage
        'app_configuration', 'payment_mode', 'groupe_fournisseur',
        'semois_configuration', 'semois_classe_config', 'classification_config',
        -- Navigation et tableaux de bord
        'nav_item', 'nav_item_role', 'nav_permission', 'nav_item_user_order',
        'dashboard_layout', 'dashboard_layout_authority',
        -- Planification et licence
        'scheduled_report', 'license_state', 'license_audit'
    ];
    v_tables text;
BEGIN
    SELECT string_agg(format('%I.%I', schemaname, tablename), ', ')
      INTO v_tables
      FROM pg_tables
     WHERE schemaname = current_schema()
       AND tablename <> ALL (v_keep);

    IF v_tables IS NOT NULL THEN
        EXECUTE 'TRUNCATE TABLE ' || v_tables || ' RESTART IDENTITY CASCADE';
    END IF;
END $$;
```

Une seule instruction `TRUNCATE` pour toutes les tables : PostgreSQL accepte alors les cycles de
clés étrangères entre elles, ce qu'un `DELETE` table par table obligerait à ordonner à la main.

**`RESTART IDENTITY` ne suffit pas.** Les identifiants assignés à la main (§3.4) viennent de
séquences autonomes, que `TRUNCATE` ignore. Elles doivent être remises à zéro explicitement :

```sql
ALTER SEQUENCE id_sale_seq                RESTART WITH 1;
ALTER SEQUENCE id_sale_item_seq           RESTART WITH 1;
ALTER SEQUENCE id_sale_assurance_item_seq RESTART WITH 1;
ALTER SEQUENCE id_commande_seq            RESTART WITH 1;
ALTER SEQUENCE id_order_line_seq          RESTART WITH 1;
ALTER SEQUENCE id_transaction_seq         RESTART WITH 1;
ALTER SEQUENCE id_transaction_item_seq    RESTART WITH 1;
ALTER SEQUENCE id_facture_seq             RESTART WITH 1;
ALTER SEQUENCE id_facture_item_seq        RESTART WITH 1;
ALTER SEQUENCE id_mvt_produit_seq         RESTART WITH 1;
ALTER SEQUENCE invoice_generation_code_seq RESTART WITH 1;
```

**Garde-fou.** Le script est destructeur et rien dans la base ne dit « je suis une démo ». Il exige
donc une confirmation explicite de l'opérateur, et refuse de s'exécuter sans elle :

```sql
\if :{?confirm_reset}
\else
    \echo 'ABANDON : 00_reset.sql efface toutes les données métier.'
    \echo 'Relancer avec  -v confirm_reset=1  si la base est bien une base de démo.'
    \quit
\endif
```

C'est peu de chose, mais c'est ce qui sépare une purge de démo d'un incident de production.

Les rares insertions touchant un référentiel conservé (`01_config.sql`) restent en `UPDATE` ou
`ON CONFLICT DO NOTHING` : elles ne dépendent pas du reset.

---

## 8. Vérification (`99_verification.sql`)

Le script produit une table `check / statut / écart` et sort en erreur si une seule ligne échoue.
Assertions minimales :

```sql
-- Vente = somme de ses lignes
SELECT s.id, s.sale_date, s.sales_amount, SUM(sl.sales_amount) AS total_lignes
FROM sales s JOIN sales_line sl ON sl.sales_id = s.id AND sl.sales_sale_date = s.sale_date
GROUP BY s.id, s.sale_date, s.sales_amount
HAVING s.sales_amount <> SUM(sl.sales_amount);          -- doit être vide

-- HT + TVA = TTC
SELECT id FROM sales WHERE ht_amount + tax_amount <> sales_amount;   -- vide

-- HT arrondi par ligne, au plafond
SELECT s.id
FROM sales s JOIN sales_line sl ON sl.sales_id = s.id AND sl.sales_sale_date = s.sale_date
GROUP BY s.id, s.ht_amount
HAVING s.ht_amount <> SUM(
  CASE WHEN sl.tax_value = 0 THEN sl.sales_amount
       ELSE CEIL(sl.sales_amount / (1 + sl.tax_value::numeric / 100)) END);   -- vide

-- net = TTC − remise
SELECT id FROM sales WHERE net_amount <> sales_amount - discount_amount;      -- vide

-- Encaissement
SELECT s.id FROM sales s
JOIN payment_transaction pt ON pt.sale_id = s.id AND pt.sale_date = s.sale_date
GROUP BY s.id, s.payroll_amount
HAVING SUM(pt.paid_amount) <> s.payroll_amount;                              -- vide

SELECT id FROM sales
WHERE payment_status = 'PAYE' AND rest_to_pay <> 0;                          -- vide

-- Toute vente à encaisser a un règlement.
-- Exceptions : TP couverte à 100 % (§4.5) et vente dépôt, due par construction (§4.6).
SELECT s.id FROM sales s
WHERE s.amount_to_be_paid > 0
  AND s.dtype <> 'VenteDepot'
  AND NOT EXISTS (SELECT 1 FROM payment_transaction pt
                  WHERE pt.sale_id = s.id AND pt.sale_date = s.sale_date);   -- vide

-- ---------- TIERS-PAYANT ----------

-- Les deux parts couvrent le net
SELECT id FROM sales
WHERE dtype = 'ThirdPartySales' AND part_assure + part_tiers_payant <> net_amount;  -- vide

-- part_tiers_payant = somme des ventilations par payeur
SELECT s.id FROM sales s
JOIN third_party_sale_line t ON t.sale_id = s.id AND t.sale_sale_date = s.sale_date
WHERE s.dtype = 'ThirdPartySales'
GROUP BY s.id, s.part_tiers_payant
HAVING s.part_tiers_payant <> SUM(t.montant);                                -- vide

-- Toute vente TP a au moins un payeur ; aucune vente comptant n'en a
SELECT id FROM sales s WHERE s.dtype = 'ThirdPartySales'
  AND NOT EXISTS (SELECT 1 FROM third_party_sale_line t
                  WHERE t.sale_id = s.id AND t.sale_sale_date = s.sale_date); -- vide

SELECT t.id FROM third_party_sale_line t
JOIN sales s ON s.id = t.sale_id AND s.sale_date = t.sale_sale_date
WHERE s.dtype <> 'ThirdPartySales';                                          -- vide

-- taux = taux EFFECTIF, pas le taux contractuel (§4.5)
SELECT t.id FROM third_party_sale_line t
JOIN sales s ON s.id = t.sale_id AND s.sale_date = t.sale_sale_date
WHERE s.sales_amount > 0
  AND t.taux <> ROUND(t.montant * 100.0 / s.sales_amount);                   -- vide

-- Aucun payeur ne reçoit plus que le montant de la vente
SELECT t.id FROM third_party_sale_line t
JOIN sales s ON s.id = t.sale_id AND s.sale_date = t.sale_sale_date
WHERE t.montant > s.sales_amount OR t.montant < 0;                           -- vide

-- Répartition TVA cohérente avec le montant du payeur
SELECT t.id FROM third_party_sale_line t
WHERE jsonb_array_length(t.repartitions) > 0
  AND ABS(t.montant - (SELECT SUM((e->>'montantTtc')::numeric)
                       FROM jsonb_array_elements(t.repartitions) e)) > 1;     -- vide (tolérance d'arrondi)

-- Le client TP appartient bien à l'assuré de la vente
SELECT t.id FROM third_party_sale_line t
JOIN sales s ON s.id = t.sale_id AND s.sale_date = t.sale_sale_date
JOIN client_tiers_payant ctp ON ctp.id = t.client_tiers_payant_id
WHERE s.customer_id IS NOT NULL
  AND ctp.assured_customer_id <> s.customer_id;                              -- vide

-- Consommation mensuelle = somme des ventilations du mois
SELECT ctp.id FROM client_tiers_payant ctp
CROSS JOIN LATERAL jsonb_array_elements(ctp.consommation_json::jsonb) c
WHERE (c->>'consommation')::bigint <> COALESCE((
    SELECT SUM(t.montant) FROM third_party_sale_line t
    WHERE t.client_tiers_payant_id = ctp.id
      AND EXTRACT(MONTH FROM t.sale_date) = (c->>'month')::int
      AND EXTRACT(YEAR  FROM t.sale_date) = (c->>'year')::int), 0);          -- vide

-- Historique de taux non vide (le hook JPA est court-circuité en SQL)
SELECT id FROM client_tiers_payant
WHERE taux_historique IS NULL
   OR jsonb_array_length(taux_historique::jsonb) = 0;                        -- vide

-- Commande = somme de ses lignes
-- La base de calcul dépend du statut : commandé avant réception, reçu après.
SELECT c.id, c.order_date FROM commande c
JOIN order_line ol ON ol.commande_id = c.id AND ol.commande_order_date = c.order_date
GROUP BY c.id, c.order_date, c.gross_amount, c.order_status
HAVING c.gross_amount <> SUM(
    CASE WHEN c.order_status = 'REQUESTED' THEN ol.quantity_requested
         ELSE COALESCE(ol.quantity_received, 0) END * ol.order_cost_amount);  -- vide

-- INVARIANT AGENCE : aucun fournisseur_produit sur une agence
SELECT fp.id FROM fournisseur_produit fp
JOIN fournisseur f ON f.id = fp.fournisseur_id
WHERE f.parent_id IS NOT NULL;                                               -- vide

-- Ligne de commande cohérente avec le principal de l'agence commandée
SELECT ol.id FROM order_line ol
JOIN commande c  ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
JOIN fournisseur ag ON ag.id = c.fournisseur_id
JOIN fournisseur_produit fp ON fp.id = ol.fournisseur_produit_id
WHERE fp.fournisseur_id <> COALESCE(ag.parent_id, ag.id);                    -- vide

-- Stock = entrées − sorties
SELECT sp.produit_id FROM stock_produit sp
WHERE sp.qty_stock <> (
    COALESCE((SELECT SUM(ol.quantity_received) FROM order_line ol
              JOIN fournisseur_produit fp ON fp.id = ol.fournisseur_produit_id
              WHERE fp.produit_id = sp.produit_id AND ol.quantity_received IS NOT NULL), 0)
  - COALESCE((SELECT SUM(sl.quantity_sold) FROM sales_line sl
              WHERE sl.produit_id = sp.produit_id), 0));                     -- vide

-- Lots cohérents avec le stock
SELECT l.produit_id FROM lot l WHERE l.statut = 'AVAILABLE'
GROUP BY l.produit_id
HAVING SUM(l.current_quantity) <> (SELECT SUM(qty_stock) FROM stock_produit
                                   WHERE produit_id = l.produit_id);         -- vide

-- Pas de stock négatif, pas de date future
SELECT id FROM stock_produit WHERE qty_stock < 0;                            -- vide
SELECT id FROM sales WHERE sale_date > CURRENT_DATE;                         -- vide

-- ---------- LOTS ----------

-- Comptabilité à deux niveaux : le lot égale la somme de ses emplacements
SELECT l.id FROM lot l
LEFT JOIN lot_stock_location lsl ON lsl.lot_id = l.id
GROUP BY l.id, l.current_quantity
HAVING l.current_quantity <> COALESCE(SUM(lsl.qty), 0);                      -- vide

-- Statut asservi à la quantité restante
SELECT id FROM lot WHERE current_quantity = 0 AND statut <> 'SOLD';          -- vide
SELECT id FROM lot WHERE statut = 'AVAILABLE' AND current_quantity <= 0;     -- vide

-- Jamais de ligne d'emplacement à zéro
SELECT id FROM lot_stock_location WHERE qty <= 0;                            -- vide

-- Un lot épuisé n'a plus d'emplacement
SELECT l.id FROM lot l JOIN lot_stock_location lsl ON lsl.lot_id = l.id
WHERE l.statut = 'SOLD';                                                     -- vide

-- Reçu ≥ restant
SELECT id FROM lot WHERE current_quantity > quantity;                        -- vide

-- Stock vendable = somme des lots AVAILABLE de ce produit dans ce stockage
SELECT sp.id FROM stock_produit sp
JOIN produit p ON p.id = sp.produit_id AND p.gestion_lot
WHERE sp.qty_stock <> COALESCE((
    SELECT SUM(lsl.qty) FROM lot_stock_location lsl
    JOIN lot l ON l.id = lsl.lot_id
    WHERE l.produit_id = sp.produit_id
      AND lsl.storage_id = sp.storage_id
      AND l.statut = 'AVAILABLE'), 0);                                       -- vide

-- Snapshot de vente : la somme des lots vendus égale la quantité vendue
SELECT sl.id FROM sales_line sl
WHERE sl.quantity_sold > 0
  AND sl.quantity_sold <> COALESCE((
      SELECT SUM((e->>'quantity')::int)
      FROM jsonb_array_elements(sl.lots) e), 0);                             -- vide

-- Snapshot cohérent avec la table lot (numéro et péremption)
SELECT sl.id FROM sales_line sl
CROSS JOIN LATERAL jsonb_array_elements(sl.lots) e
JOIN lot l ON l.id = (e->>'id')::int
WHERE l.num_lot <> (e->>'numLot')
   OR l.produit_id <> sl.produit_id;                                         -- vide

-- Règle de vendabilité : péremption > date de vente + 90 j
SELECT sl.id FROM sales_line sl
CROSS JOIN LATERAL jsonb_array_elements(sl.lots) e
WHERE (e->>'expiryDate')::date <= sl.sale_date + INTERVAL '90 days';         -- vide

-- FEFO : péremption croissante à l'intérieur d'une ligne.
-- La 1re entrée est exemptée si code_scan impose un lot (AI 10) — cf. §4.10.
SELECT sl.id FROM sales_line sl
WHERE EXISTS (
    SELECT 1 FROM (
        SELECT (e->>'expiryDate')::date AS d, ord,
               LAG((e->>'expiryDate')::date) OVER (ORDER BY ord) AS prev
        FROM jsonb_array_elements(sl.lots) WITH ORDINALITY AS t(e, ord)
    ) x
    WHERE x.d < x.prev
      AND NOT (x.ord = 2 AND sl.code_scan LIKE '%10%'));                     -- vide

-- ---------- VENTE DÉPÔT ----------

-- Toute vente dépôt cible un magasin de type DEPOT
SELECT s.id FROM sales s
LEFT JOIN magasin m ON m.id = s.depot_id
WHERE s.dtype = 'VenteDepot'
  AND (s.depot_id IS NULL OR m.type_magasin <> 'DEPOT');                     -- vide

-- Marqueurs comptables imposés par construction (§4.6)
SELECT id FROM sales WHERE dtype = 'VenteDepot'
  AND (ca <> 'CA_DEPOT'
    OR amount_to_be_taken_into_account <> 0
    OR payment_status <> 'IMPAYE'
    OR rest_to_pay <> amount_to_be_paid
    OR customer_id IS NOT NULL);                                             -- vide

-- Aucune autre vente ne porte CA_DEPOT
SELECT id FROM sales WHERE ca = 'CA_DEPOT' AND dtype <> 'VenteDepot';        -- vide

-- Une vente dépôt n'a jamais de règlement
SELECT pt.id FROM payment_transaction pt
JOIN sales s ON s.id = pt.sale_id AND s.sale_date = pt.sale_date
WHERE s.dtype = 'VenteDepot';                                                -- vide

-- Le magasin dépôt possède son stockage PRINCIPAL (sinon primaryStorage est null)
SELECT m.id FROM magasin m
WHERE m.type_magasin = 'DEPOT'
  AND NOT EXISTS (SELECT 1 FROM storage s
                  WHERE s.magasin_id = m.id AND s.storage_type = 'PRINCIPAL');
                                                                             -- vide

-- Le stock du dépôt est sans lot : aucun lot_stock_location sur ses stockages
SELECT lsl.id FROM lot_stock_location lsl
JOIN storage s  ON s.id = lsl.storage_id
JOIN magasin m  ON m.id = s.magasin_id
WHERE m.type_magasin = 'DEPOT';                                              -- vide

-- Conservation : ce qui est sorti de l'officine est entré au dépôt
SELECT sl.produit_id FROM sales_line sl
JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date
WHERE s.dtype = 'VenteDepot'
GROUP BY sl.produit_id
HAVING SUM(sl.quantity_requested) <> COALESCE((
    SELECT sp.qty_stock FROM stock_produit sp
    JOIN storage st ON st.id = sp.storage_id
    JOIN magasin mg ON mg.id = st.magasin_id AND mg.type_magasin = 'DEPOT'
    WHERE sp.produit_id = sl.produit_id), 0)
  + COALESCE((SELECT SUM(rdi.qty_mvt) FROM retour_depot_item rdi
              WHERE rdi.produit_id = sl.produit_id), 0);   -- vide (transferts − retours = stock dépôt)

-- ---------- NUMÉROTATION ----------

-- Le compteur du jour couvre les ventes réellement émises ce jour-là
SELECT s.sale_date FROM sales s
GROUP BY s.sale_date
HAVING COUNT(*) > COALESCE((SELECT r.number_transac FROM reference r
                            WHERE r.mvt_date = s.sale_date AND r.type = 0), 0);
                                                                             -- vide  (0 = VENTE)

-- number_transaction bien formé : yyyyMMdd + compteur
SELECT id FROM sales
WHERE number_transaction !~ '^\d{8}\d{3,}$'
   OR LEFT(number_transaction, 8) <> TO_CHAR(sale_date, 'YYYYMMDD');          -- vide

-- ---------- FMD ----------

-- Sérial réservé aux produits sur ordonnance
SELECT l.id FROM lot l JOIN produit p ON p.id = l.produit_id
WHERE l.serial_number IS NOT NULL
  AND p.statut_legal NOT IN ('LISTE_I','LISTE_II','STUPEFIANTS','PSO');      -- vide

-- Un sérial identifie une boîte : lot sérialisé de faible quantité
SELECT id FROM lot WHERE serial_number IS NOT NULL AND quantity > 3;         -- vide

-- Unicité par produit (double sécurité par rapport à l'index partiel)
SELECT serial_number, produit_id FROM lot
WHERE serial_number IS NOT NULL
GROUP BY serial_number, produit_id HAVING COUNT(*) > 1;                      -- vide

-- Longueur compatible varchar(50)
SELECT id FROM lot WHERE LENGTH(serial_number) > 50;                         -- vide

-- code_scan uniquement sur des lignes consommant un lot sérialisé
SELECT sl.id FROM sales_line sl
WHERE sl.code_scan IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM jsonb_array_elements(sl.lots) e
      JOIN lot l ON l.id = (e->>'id')::int
      WHERE l.serial_number IS NOT NULL);                                    -- vide

-- Le code scanné réencode le lot réellement consommé en premier
SELECT sl.id FROM sales_line sl
JOIN lot l ON l.id = ((sl.lots->0)->>'id')::int
WHERE sl.code_scan IS NOT NULL
  AND (POSITION(l.num_lot IN sl.code_scan) = 0
    OR (l.serial_number IS NOT NULL
        AND POSITION(l.serial_number IN sl.code_scan) = 0));                 -- vide

-- ---------- RÉCEPTIONS ----------

-- Toute commande réceptionnée a ses lot_reception ; aucune pour les REQUESTED
SELECT l.id FROM lot l
JOIN order_line ol ON ol.id = l.order_line_id
                  AND ol.order_date = l.commande_order_date
JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
WHERE c.order_status IN ('RECEIVED','CLOSED')
  AND NOT EXISTS (SELECT 1 FROM lot_reception lr WHERE lr.lot_id = l.id);    -- vide

SELECT lr.id FROM lot_reception lr
JOIN order_line ol ON ol.id = lr.order_line_id
                  AND ol.order_date = lr.commande_order_date
JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
WHERE c.order_status = 'REQUESTED';                                          -- vide

-- Un lot IN_PROGRESS n'a pas encore d'emplacement de stock
SELECT l.id FROM lot l JOIN lot_stock_location lsl ON lsl.lot_id = l.id
WHERE l.statut = 'IN_PROGRESS';                                              -- vide

-- Les lots entrent par le stockage PRINCIPAL
SELECT lsl.id FROM lot_stock_location lsl
JOIN storage s ON s.id = lsl.storage_id
WHERE s.storage_type <> 'PRINCIPAL'
  AND NOT EXISTS (SELECT 1 FROM lot_stock_location p
                  JOIN storage ps ON ps.id = p.storage_id
                  WHERE p.lot_id = lsl.lot_id AND ps.storage_type = 'PRINCIPAL');
                                                                             -- vide hors transferts

-- products_to_destroy adossé à un fournisseur_produit d'un principal
SELECT ptd.id FROM products_to_destroy ptd
JOIN fournisseur_produit fp ON fp.id = ptd.fournisseur_produit_id
JOIN fournisseur f ON f.id = fp.fournisseur_id
WHERE f.parent_id IS NOT NULL;                                               -- vide
```

Ces requêtes sont aussi la spécification exécutable du §4 : si le générateur change, elles restent
l'autorité.

---

## 9. Configuration à poser (`01_config.sql`)

Le référentiel livre la gestion de lot **désactivée**. La démo l'active :

```sql
UPDATE app_configuration SET value = '1' WHERE name = 'APP_GESTION_LOT';
UPDATE app_configuration SET value = '1' WHERE name = 'APP_GESTION_LOT_INVENTAIRE';
```

| Clé | Défaut | Démo | Effet |
|---|---|---|---|
| `APP_GESTION_LOT` | `0` | **`1`** | lots à la réception et à la vente (`useLot()`) |
| `APP_GESTION_LOT_INVENTAIRE` | `0` | **`1`** | saisie par lot en inventaire (`useGestionLotInventaire()`) |
| `APP_MODE_SAISIE_LOT_INVENTAIRE` | `LOT_PLAT` | inchangé | mode d'affichage de la saisie |
| `APP_NOMBRE_JOUR_AVANT_PEREMPTION` | `90` | inchangé | seuil de non-mise en vente — **structure les cohortes du §4.9** |
| `APP_EXPIRY_ALERT_DAYS_BEFORE` | `30,7` | inchangé | paliers d'alerte, alimentés par la cohorte « Alerte » |

`APP_GESTION_LOT_INVENTAIRE` est activé de pair : laisser l'inventaire en mode non-lot alors que le
stock est ventilé par lot produirait des écarts d'inventaire artificiels à la clôture
(`InventoryCloseServiceImpl` passe `p_gestion_lot` à la procédure de clôture).

Ces deux clés sont mises en cache (`CacheConfiguration:46-47`) : **un redémarrage applicatif, ou
une purge du cache, est nécessaire** après exécution des scripts sur une instance déjà lancée.

`produit.gestion_lot` reste à `TRUE` (défaut) pour tous les produits sauf une poignée
(~40 produits : accessoires, parapharmacie) laissés à `FALSE` pour exercer le chemin mixte —
c'est un cas réel en officine et il est silencieusement cassé si jamais testé.

**Clé liée au tiers-payant.** `APP_SANS_NUM_BON` (« Autorisation de vente sans numéro de bon »)
vaut `0` : le numéro de bon est donc obligatoire. Le jeu de données renseigne `sales.num_bon` et
`third_party_sale_line.num_bon` sur **toutes** les ventes tiers-payant. Les laisser nuls
produirait un historique que la saisie applicative aurait refusé.

---

## 10. Alternative écartée, et pourquoi

**Générateur Java (profil Spring `demo` + `CommandLineRunner`) réutilisant `SaleCommonService`,
`SalesLineServiceImpl`, `CommandeService`.**

Avantage réel et non négligeable : la cohérence serait garantie *par construction*, puisque ce sont
les services de production qui calculent les montants. Aucun risque de divergence entre la formule
du §4 et le code.

Écartée pour cette itération parce que :

- l'exigence est « des scripts à exécuter au besoin » — un runner Java impose un rebuild et un
  démarrage applicatif ;
- les services de vente valident le stock, la caisse ouverte, les délais d'annulation : générer
  180 jours d'historique passé à travers eux demande de neutraliser ces contrôles, ce qui affaiblit
  la garantie recherchée ;
- le SQL permet de générer 4 000 ventes en quelques secondes, là où le passage par les services
  prendrait plusieurs minutes.

Le prix à payer est la duplication des formules du §4 dans le SQL. `99_verification.sql` est le
contrepoids : il échoue si la duplication dérive. **Si le jeu de données doit un jour servir de
support à des tests de non-régression métier, il faudra rebasculer sur le générateur Java.**

---

## 11. Découpage

| Étape | Contenu | Livrable |
|---|---|---|
| **1 ✅** | Config, fournisseurs (principaux + agences), produits, `fournisseur_produit`, rayons | `01`, `02`, `03` |
| **2 ✅** | Clients, tiers-payants | `04` |
| **3 ✅** | Commandes et lignes | `05` |
| **4 ✅** | **Lots, cohortes, sérials FMD, `lot_reception`, `lot_stock_location`, stock initial** | `06`, `07` |
| **5 ✅** | Caisses, ventes, lignes, paiements, consommation FEFO | `08`, `09` |
| **6 ✅** | Répartitions TVA, consommations, ventes dépôt | `10`, `10b`, `13` |
| **7 ✅** | Destruction, facturation, compteurs de numérotation, `inventory_transaction` | `12`, `14`, `15`, `16` |
| **8 ✅** | Reset, orchestration, vérification, README | `00`, `99`, `run_all` |

Les scripts vivent dans [`scripts/demo-data/`](../scripts/demo-data/), avec leur propre
[README](../scripts/demo-data/README.md). S'y ajoute `create_database.sql`, prérequis créant le
rôle `pharma_smart`, la base `pharma_smart_demo` et le schéma `pharma_smart` — il vise une autre
base que les autres scripts et n'est donc pas appelé par `run_all.sql`.

L'étape 8 (et particulièrement `99_verification.sql`) est à écrire **en même temps que l'étape 1**,
pas à la fin : c'est elle qui rend les étapes suivantes vérifiables au fur et à mesure.

Deux étapes concentrent la difficulté et méritent d'être traitées seules :

- **l'étape 5**, où se croisent la consommation FEFO, la règle des 90 jours et le snapshot
  `sales_line.lots` ;
- **l'étape 6**, devenue majeure avec le tiers-payant à 50 % : répartition par priorité, plafonds,
  double notion de taux et ventilation TVA (§4.5). À ne démarrer qu'une fois l'étape 5 validée par
  le script de vérification, puisqu'elle dérive entièrement des `sales_line` écrites.

---

### Omission relevée en cours d'écriture : `inventory_transaction`

La table **`inventory_transaction`** n'apparaissait nulle part dans ce plan. Elle porte pourtant
l'**historique des mouvements produit** : `InventoryTransactionService.save(salesLine)` est appelé
à chaque vente, et `saveVenteDepotExtensionInventoryTransactions` à chaque transfert dépôt.

Structure : PK composite `(id, transaction_date)` avec `@IdClass(ProductMvtId)` — donc identifiants
assignés à la main, depuis `id_mvt_produit_seq`. Colonnes obligatoires : `mouvement_type`,
`quantity`, `quantity_befor`, `quantity_after`, `cost_amount`, `regular_unit_price`, `entity_id`,
`produit_id`, `user_id`, `magasin_id`, `storage_id`.

`MouvementProduit` compte 18 valeurs, dont celles que le jeu de données peut alimenter :
`SALE`, `ENTREE_STOCK`, `COMMANDE`, `RETOUR_DEPOT`, `RETRAIT_PERIME`, `AJUSTEMENT_IN/OUT`,
`INVENTAIRE`, `DECONDTION_IN/OUT`, `MOUVEMENT_STOCK_IN/OUT`.

Sans ces lignes, les écrans « Suivi article » et « Historique produit » restent vides alors que le
stock et les ventes sont cohérents. À produire en étape 7 (`16_mouvements.sql`), en dérivant des
`sales_line` et des `order_line` déjà écrites.

---

## 12. Points à trancher

**Les cinq questions ouvertes sont tranchées.** Le plan est complet ; l'écriture des scripts peut
commencer par l'étape 1 du §11, conjointement avec `99_verification.sql`.

1. ~~**Gestion des lots.**~~ **Tranché** : activée pour la démo. Voir §3.7, §4.9 et §9.
2. ~~**Ventes tiers-payant.**~~ **Tranché** : 50 % du volume. Fichier client rééquilibré à
   170 assurés en conséquence. Voir §4.5 et §6.
3. ~~**Plages d'identifiants réservées.**~~ **Tranché** : base dédiée, pas de plage réservée ni de
   marqueur. `00_reset.sql` vide les tables métier et préserve les référentiels Flyway. Voir §7.
4. ~~**Magasin de type `DEPOT`.**~~ **Tranché** : un dépôt, avec son stockage `PRINCIPAL`,
   150 ventes dépôt et 20 retours. Voir §4.6.
5. ~~**Traçabilité FMD.**~~ **Tranché** : sérials générés sur ~25 % des lots, restreints aux
   produits sur ordonnance, avec `code_scan` cohérent sur ~15 % des lignes de vente. Voir §4.10.

---

## 13. Récapitulatif des écarts entre la révision 1 et le modèle réel

Conservé comme garde-fou : ce sont les erreurs à ne pas réintroduire.

| Révision 1 (faux) | Modèle réel |
|---|---|
| `produit.code_cip` | `fournisseur_produit.code_cip` |
| `produit.prix_ht` décimal | `produit.regular_unit_price` entier (FCFA) |
| `produit.magasin_id` | n'existe pas |
| `produit.taux_vente` | n'existe pas |
| `sales.montant_ht` / `montant_ttc` | `ht_amount` / `sales_amount` |
| `sales.sale_number` | `number_transaction` |
| `sales` PK simple | PK composite `(id, sale_date)` |
| `sales.customer_id` obligatoire | nullable |
| `sales_line.price_ht` | `regular_unit_price` |
| `inventory` / `inventory_lot` | `stock_produit` / `lot` |
| `commande.commande_number` | `order_reference` / `receipt_reference` |
| `commande_line` | `order_line` |
| `fournisseur.groupe_fournisseur_id` | `parent_id` (auto-référence) |
| `fournisseur_produit` sur n'importe quel fournisseur | uniquement sur un **principal** |
| Devise € | FCFA |
| Montants décimaux | entiers |
| `created_by` / `magasin_id` partout | seulement où l'entité les déclare |
| Migrations Flyway `V1.9.4+` | scripts autonomes hors Flyway |
