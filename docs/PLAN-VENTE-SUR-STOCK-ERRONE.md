# Plan — Vendre sur un stock erroné, sans le confondre avec un avoir

## Le problème en une phrase

Le forçage de stock a été construit pour **un** besoin — la rupture qu'on solde en avoir — et il
sert aujourd'hui à **deux**. Le second, l'écart d'inventaire, passe par la porte du premier et en
sort déformé : un avoir dû à un client qui est pourtant reparti servi, un client exigé à
l'encaissement sans raison, et un écart entre stock machine et stock physique que rien ne signale.

---

## Règle de gestion : le stock peut être négatif

Ce n'est pas un défaut de conception. Une vente forcée sort du stock la quantité **demandée**,
qu'elle existe ou non ; le stock passe en négatif, et un ajustement ou une réception le ramène
ensuite à la réalité. Le négatif dit quelque chose de vrai : l'officine doit de la marchandise
(cas A), ou la machine ignore de la marchandise présente (cas B).

Les consommateurs qui ne supportent pas un négatif s'en protègent déjà (ex. `GREATEST(…, 0)` dans
`V1.3.5__fix_stock_rotation_negative_values.sql`). Ce plan ne change pas cette règle.

---

## Deux besoins, qu'il faut cesser de confondre

### A — Rupture réelle → avoir client

Le rayon est vide, la machine dit vrai. Le client paie aujourd'hui et repart les mains vides ;
l'officine lui doit la marchandise et la lui remettra après la prochaine entrée en stock.

Un client identifié est ici **légitimement obligatoire** : sans lui, on ne sait pas à qui livrer.
C'est le besoin que l'application couvre — aux bugs 2 et 3 près (voir plus bas).

### B — Écart d'inventaire → servir et corriger

Le rayon a des boîtes que la machine ignore : réception mal saisie, retour non enregistré, vol non
constaté. Le client repart **servi**, immédiatement, avec la marchandise en main.

Rien n'est dû à personne. Aucun avoir n'a de sens, aucun client n'a à être saisi — c'est une vente
comptant ordinaire. Ce qui doit changer, c'est le **stock informatique**, qui vient d'être démenti
par les faits.

Ce besoin n'est couvert par rien aujourd'hui.

### Seul le caissier peut les distinguer

Les deux cas sont identiques pour la machine : stock insuffisant, forçage demandé. La différence
est devant le rayon — la boîte est là, ou elle ne l'est pas. L'application doit donc **poser la
question** au moment du forçage, et non la déduire.

---

## État actuel — ce que le code fait réellement

La chaîne, du clic à la base :

| Étage | Fichier | Comportement |
|---|---|---|
| Résolution du stock | `SalesLineServiceImpl.getCurrentStockQuantity` | `forceStock=true` ne crée pas de stock : il évite seulement la levée de `StockException`, et déclenche le transfert réserve → rayon quand la réserve peut combler. |
| Quantité servie | `SalesLineServiceImpl.calculateQuantitySold` | `0` si le stock est nul ou négatif, sinon `min(stock, demandé)`. |
| Clôture | `SalesLineServiceImpl.save(Set<SalesLine>, …)` | `quantityAvoir = quantityRequested − quantitySold`, puis `avoirClientDocumentService.createAvoirsFromSale(ligne, sale.getCustomer())` dès que cet écart est positif. |
| Sortie de stock (`qty_stock`) | `StockUpdateService.updateStock` | Sort la quantité **demandée** (`qtyStock − (quantityRequested − quantityUg)`), sans plancher : le stock passe en négatif, conformément à la règle de gestion. |
| Sortie des lots | `SalesLineServiceImpl.updateSaleLineLotSold` | Suit la quantité **servie** (FEFO) : rien n'est débité des lots pour la part en avoir. |
| Clôture d'avoir | `AvoirClientDocumentServiceImpl.cloturerAvoir` | Exige `SUM(qtyStock + qtyUG) ≥ quantité de l'avoir`, puis clôt — sans aucun mouvement de stock ni de lot. |
| Détection front | `SalesStore.isAvoir` | `total demandé ≠ total servi` — aucune autre condition. |
| Garde encaissement | `sale-payment.facade.ts:37-45` | `avoir && !customerId` → refus : « Un client est obligatoire pour une vente avec avoir (livraison partielle) ». |
| Dialogue | `force-stock.mixin.ts` | Une seule question, une seule intention : « La quantité saisie est supérieure à la quantité stock du produit. Voulez-vous continuer ? » |

### Ce que cela produit sur le cas B

Exemple : rayon physique 5, machine 0, vente de 3.

1. Le caissier force, croyant dire « la machine se trompe, je sers ».
2. `quantitySold = 0`, `qty_stock` passe à −3. Le physique est à 2 : l'écart machine/physique
   reste de 5, mais **rien ne le signale** — un −3 ressemble exactement à une dette d'avoir.
3. `quantityAvoir = 3` : un avoir est créé pour une marchandise déjà livrée. La dette est
   fictive, et elle restera ouverte dans la liste des avoirs.
4. L'encaissement exige un client. Au comptoir, on saisit donc un client de complaisance, ou on
   renonce.

---

## Bugs relevés

Constatés à la lecture du code ; les bugs 2 et 3 touchent le cas A, c'est-à-dire l'existant, et
sont indépendants du reste du plan.

| # | Bug | Scénario | Où |
|---|---|---|---|
| 1 | **Écart d'inventaire traité comme une rupture** | Rayon 3 boîtes, machine 0. Le caissier force, le client repart servi — mais `quantitySold = 0`, un avoir de 3 est créé pour une marchandise remise, et l'encaissement exige un client. | `calculateQuantitySold`, `save(Set<SalesLine>, …)`, `sale-payment.facade.ts` |
| 2 | **La clôture d'avoir compte deux fois la quantité due** | Stock 0, vente forcée de 3 → stock −3, avoir de 3. On reçoit 3 boîtes → stock 0. La clôture est **refusée** (« stock disponible = 0 ») alors que les 3 boîtes sont en rayon : la dette est déjà déduite de `qtyStock` au moment de la vente. Le contrôle devrait être `stock ≥ 0`, pas `stock ≥ quantité`. | `AvoirClientDocumentServiceImpl.cloturerAvoir`, `StockProduitRepository.findTotalQuantityByMagasinIdIdAndProduitId` |
| 3 | **La remise d'un avoir ne débite pas les lots** | Après réception, les boîtes remises au client sortent physiquement, mais aucun lot n'est débité (la vente n'a débité que la part servie). Les lots surestiment le physique. | `cloturerAvoir` |
| 5 | **Un avoir remboursé laisse la dette dans le stock** | Stock −3 après une vente en avoir, le client est remboursé (espèces, CB, bon, compensation) : la marchandise ne sera jamais remise, mais les 3 unités restent déduites du stock. Pire, le contrôle de stock s'appliquait à tous les modes : on ne pouvait pas rembourser faute de stock — alors qu'on rembourse justement parce qu'il n'y en a pas. | `cloturerAvoir` |
| 6 | **Incrémenter une ligne contourne le contrôle de stock** | Ligne de 2, stock 4, on ajoute 3 sans forçage : le contrôle comparait l'incrément (3) au stock (4) au lieu du total (5). La ligne passait, servie à 4, avec 1 en avoir — un forçage sans privilège. | `SalesLineServiceImpl.incrementItemQuantityRequested` |
| 4 | **Documentation fausse** | La version précédente de ce plan affirmait « la sortie suit la quantité servie » et « l'écart grandit » ; le commentaire d'en-tête de `vte-41-forcer-le-stock.spec.ts` affirme « le stock ne passe pas en négatif ». Les trois sont démentis par `StockUpdateService.updateStock`. Le parcours décline le forçage, donc rien ne l'a jamais vérifié. | `e2e/parcours/ventes/vte-41-forcer-le-stock.spec.ts:12` |

---

## Cible

Le forçage cesse d'être un booléen et devient un **motif**. Deux branches, deux traitements :

| | A — Rupture (existant) | B — Écart d'inventaire (nouveau) |
|---|---|---|
| Question posée au caissier | « Le client sera livré plus tard » | « Le produit est en rayon, la machine se trompe » |
| Quantité servie | bornée au stock | **égale à la quantité demandée** |
| Quantité en avoir | l'écart | **zéro** |
| Avoir client | créé | aucun |
| Client obligatoire | **oui** | non |
| Stock informatique | négatif (règle de gestion) | **corrigé par ajustement automatique**, puis vente normale |
| Privilège | forçage de stock | privilège distinct (il corrige du stock) |
| Trace | ligne en avoir | ajustement d'entrée motivé, daté, attribué |

---

## Ajustement automatique sur le cas B

Au moment de la clôture, avant la sortie, un ajustement d'entrée porte le stock à ce que le rayon
vient de prouver, avec un motif dédié (« Régularisation constatée à la vente »). La vente se
déroule ensuite sans rien de particulier.

Pourquoi un ajustement plutôt qu'un simple négatif : sur le cas B, le négatif ne serait pas une
dette mais une erreur, et il se confondrait avec les dettes d'avoir. L'ajustement dit « la machine
s'est trompée » à l'endroit prévu pour cela, il est daté, motivé, attribué à un utilisateur, et il
se relit six mois plus tard dans l'historique des mouvements.

### Quantité à régulariser

```
qtyRegul = quantityRequested − max(stockRayonCourant, 0)
```

Le `max(…, 0)` est indispensable : un stock déjà négatif porte des **dettes d'avoir** (cas A) qu'on
ne doit pas effacer. Exemple : stock −3 (3 boîtes dues), rayon physique 2, vente B de 2 →
régularisation de +2 → −1 → vente → −3. La dette reste intacte.

### Trois contraintes d'exécution

1. **À la clôture, pas à l'ajout de la ligne.** `calculateQuantitySold` s'exécute à l'ajout
   (`setCommonSaleLine`), mais le stock ne bouge qu'à la clôture (`save(Set<SalesLine>, …)`).
   Régulariser à l'ajout laisserait un ajustement orphelin sur un panier abandonné. D'où le motif
   **persisté sur `sales_line`**, relu à la clôture.
2. **Sur le stock courant.** Une autre caisse a pu vendre entre l'ajout et la clôture : la quantité
   se recalcule à la clôture.
3. **Dans cet ordre**, dans `save(Set<SalesLine>, …)` pour une ligne B :
   1. ajustement d'entrée (crédit `qty_stock` + crédit du dernier lot reçu + mouvement + log) ;
   2. `updateSaleLineLotSold` — les lots ayant été crédités, le débit FEFO couvre toute la
      quantité ;
   3. `updateStock` — le stock ne passe pas sous zéro du fait de B, et le log `FORCE_STOCK`
      (« Vente en avoir ») ne se déclenche plus ;
   4. `quantityAvoir = 0` → `createAvoirsFromSale` n'est pas appelé.

Le transfert implicite réserve → rayon (`getCurrentStockQuantity`) reste **avant** : si la réserve
a du stock, on transfère d'abord et on ne régularise que le reliquat.

---

## Conception

### 1. Domaine — porter l'intention jusqu'au calcul

`SaleLineDTO.forceStock: boolean` ne dit pas *pourquoi* on force. On ajoute :

```java
public enum MotifForcageStock {
    RUPTURE_AVOIR,      // le client sera livré plus tard  → comportement actuel
    ECART_INVENTAIRE    // le client repart servi          → régularisation
}
```

porté par `SaleLineDTO.motifForcage` (nullable) et persisté sur `sales_line`
(`@Enumerated(EnumType.STRING)`). `forceStock` reste, mais devient dérivé : `motifForcage != null`.
Les deux cohabitent le temps d'une version, puis `forceStock` disparaît des DTO.

### 2. Backend

- `calculateQuantitySold(quantityRequested, currentStock, motif)` : renvoie `quantityRequested`
  quand `motif == ECART_INVENTAIRE`, le `min` actuel sinon. Le motif est propagé à **tous** les
  appels (`setCommonSaleLine`, `updateSalesLine`, `updateStock`, import de données — ce dernier
  passe `null`).
- `updateSalesLine` (ajout de quantité sur une ligne existante) : conserver le motif déjà porté
  par la ligne si le DTO n'en envoie pas.
- `AjustementService` : extraire de `saveItems` (privée) une méthode publique
  `regulariserALaVente(StockProduit, int qty)` qui crée un `Ajust` directement `CLOSED` et réutilise
  crédit de lot, mouvement journalisé et logs — rien à dupliquer.
- `SalesLineServiceImpl.save(Set<SalesLine>, …)` : appel de la régularisation selon l'ordre
  ci-dessus. `quantityAvoir` vaut zéro sur cette branche ; la condition `quantityAvoir > 0`
  suffit déjà à ne pas créer d'avoir.
- Contrôle du privilège dédié côté service (`setCommonSaleLine`), et pas seulement côté écran.

### 3. Front

- `force-stock.mixin.ts` : le dialogue à deux boutons devient un **choix à deux branches** —
  « Le client sera livré plus tard » / « Le produit est en rayon, la machine se trompe ». Le second
  n'apparaît qu'avec le privilège de régularisation.
- `SalesStore.isAvoir` : la branche B garde demandé = servi, donc le calcul actuel reste juste par
  construction. **Aucun changement** — c'est le signe que la modélisation est la bonne.
- `sale-payment.facade.ts` : la garde client reste inchangée, et ne mordra plus sur le cas B
  puisqu'il ne produit pas d'avoir.

### 4. Migration Flyway

- `ALTER TABLE sales_line ADD COLUMN motif_forcage varchar(20)` (nullable).
- Une ligne dans `motif_ajustement` : « Régularisation constatée à la vente ».
- Le privilège `pr-regulariser-stock-vente`.
- Rétro-compatibilité : les lignes existantes ayant produit un avoir (`quantity_avoir > 0` ou avoir
  rattaché) sont qualifiées `RUPTURE_AVOIR` par un `UPDATE`.

### 5. Privilèges

Le forçage actuel autorise à *vendre* ce qu'on n'a pas. La régularisation autorise à *corriger le
stock* et à ne pas créer de dette client — c'est un pouvoir d'inventaire, pas de caisse. Deux
privilèges distincts, quitte à ce que le même profil les porte tous les deux au départ.

### 6. Rendre l'écart visible

Pour un produit, le stock négatif légitime vaut la somme des avoirs ouverts. Tout négatif **au-delà**
est un écart machine/physique qui a échappé à la branche B (forçage A choisi à tort, sortie non
saisie…). Une liste « Écarts à régulariser » — produits dont `qty_stock < −Σ avoirs ouverts` — donne
au pharmacien sa liste de travail pour l'ajustement ou l'inventaire suivant. Lecture seule, rien
n'est écrit automatiquement.

---

## Effets de bord à vérifier

- **Inventaire** : un écart régularisé à la vente doit rester lisible dans l'écart d'inventaire
  suivant (motif dédié dans l'historique), pas s'y fondre silencieusement.
- **Déclaration de CA** : la vente est une vente comptant ordinaire, aucun impact attendu — à
  confirmer sur `declarationCaService.appliquerExclusions`.
- **Valorisation du stock et marge** : l'ajustement d'entrée se fait à quel prix ? Au coût courant
  du produit, faute de mieux, et c'est à assumer explicitement dans le mouvement.
- **Annulation de la vente** : le stock vendu est restitué, l'ajustement d'entrée reste. C'est
  juste — les boîtes existaient.
- **VTE-42 (transfert réserve → rayon)** et **VTE-43 (déconditionnement)** : inchangés. Ils servent
  déjà la totalité de la commande et ne produisent pas d'avoir.
- **Ventes dépôt** (`StockUpdateService.updateStockDepot`) : chemin distinct, hors périmètre.
- **Liste des avoirs clients** : les avoirs fictifs déjà créés par des forçages du cas B ne sont pas
  rattrapables automatiquement — ils sont indiscernables des vrais. À clôturer à la main.

---

## Lots de livraison

**Lot 0 — Bugs de l'existant. ✅ Livré.** Voir « Ce qui a déjà été fait ».

**Lot 1 — Le socle. ✅ Livré.** Voir « Ce qui a déjà été fait ».

**Lot 2 — La branche B côté serveur. ✅ Livré.** Voir « Ce qui a déjà été fait ».

**Lot 3 — Le choix à l'écran. ✅ Livré.** Voir « Ce qui a déjà été fait ».

**Lot 4 — Écarts à régulariser. ✅ Livré.** Voir « Ce qui a déjà été fait ».

**Lot 5 — Cahier de recette. ✅ Livré.** Voir « Ce qui a déjà été fait ».

---

## Ce qui a déjà été fait

La description de **VTE-41** a été corrigée : elle annonçait « quand le stock affiché est erroné »
— soit exactement le cas B, que l'application ne traite pas. Elle décrit désormais le forçage en
avoir : quantité servie bornée au stock, écart parti en avoir, client obligatoire pour encaisser.

**Lot 0**, dans `AvoirClientDocumentServiceImpl.cloturerAvoir` :

- **Bug 2** — le contrôle de stock ne s'applique plus qu'à la remise du produit
  (`RETOUR_PRODUIT`), et exige `stock ≥ 0` : la vente a déjà déduit la dette.
- **Bug 3** — à la remise du produit, les lots sont débités en FEFO sur le rayon
  (`lotService.adjustLots` + `lotStockLocationService.debitFefo`) ; `qty_stock` ne bouge pas.
- **Bug 5** — un avoir soldé sans remise du produit recrédite la quantité due sur le stock rayon,
  tracé dans les logs sous `AVOIR_SOLDE_SANS_PRODUIT` (migration
  `V2.1.3__logs_avoir_solde_sans_produit.sql`, qui étend `logs_transaction_type_check`).
- Les effets sur le stock n'interviennent qu'au solde de l'avoir, jamais sur une utilisation
  partielle ; c'est le mode de la clôture finale qui décide.
- **Bug 4** — commentaire d'en-tête de `vte-41-forcer-le-stock.spec.ts` corrigé : le stock passe
  en négatif, c'est la dette envers le client.

Tests : `AvoirClientDocumentServiceImplTest` et `AvoirClientDocumentServiceIntegrationTest`
(PostgreSQL) — chaque mode de clôture, soldes en plusieurs tranches, réserve couvrant la dette,
remise refusée sans effet, trace acceptée par la contrainte de `logs`.

**Lot 1** :

- `MotifForcageStock` (`RUPTURE_AVOIR`, `ECART_INVENTAIRE`) et `SalesLine.motifForcage`, colonne
  `sales_line.motif_forcage` contrainte aux deux valeurs.
- Le motif est **recalculé** à chaque création ou modification de quantité : `RUPTURE_AVOIR` quand la
  ligne est forcée et que la demande dépasse le stock après transfert réserve → rayon, `null` sinon.
  Un forçage comblé par la réserve, ou une quantité ramenée sous le stock, n'a donc pas de motif.
- `calculateQuantitySold(demandé, stock, motif)` sert toute la demande sur `ECART_INVENTAIRE` ; aucun
  appel ne le produit encore. `SaleLineDTO.motifForcage` n'est **pas** ajouté : il arrivera au lot 2
  avec le contrôle du privilège, pour qu'aucune API n'accepte un écart d'inventaire sans contrôle.
- Migration `V2.1.4__motif_forcage_stock.sql` : colonne, rattrapage des lignes existantes en
  `RUPTURE_AVOIR` (`quantity_avoir > 0` ou avoir rattaché), motif d'ajustement « Régularisation
  constatée à la vente », privilège `pr-regulariser-stock-vente` accordé à `ROLE_ADMIN` et `ROLE_CAISSIER`.
- **Bug 6** corrigé : l'incrément contrôle le total de la ligne.

Tests : `SalesLineServiceImplForcageTest` (motif et quantité servie, les deux motifs) et
`SalesLineServiceIntegrationTest` (motif écrit en base, absent sans manque ou quand la réserve comble,
effacé quand la quantité redescend, incrément forcé, bug 6, référentiels de la migration). Le
rattrapage de la migration n'est pas couvert : la base de test part vide.

**Lot 2** — corrige le bug 1 côté serveur :

- **Contrat d'API** : `SaleLineDTO.motifForcage` (`RUPTURE_AVOIR` | `ECART_INVENTAIRE`) est accepté
  en entrée et vaut forçage ; `forceStock: true` seul reste une rupture. Le motif n'est **jamais
  renvoyé** (`WRITE_ONLY`) et `forceStock` reste le drapeau brut : un DTO relu puis renvoyé pour un
  changement de quantité ne peut pas forcer sans confirmation.
- **Motif** : sans manque, aucun motif, quel que soit ce qui est demandé. Avec manque,
  `ECART_INVENTAIRE` s'il est demandé, `RUPTURE_AVOIR` sinon. Sur une modification de quantité, la
  ligne garde son motif si le DTO n'en envoie pas.
- **Privilège** : `ECART_INVENTAIRE` exige `pr-regulariser-stock-vente` ou `ROLE_ADMIN`, contrôlé
  dans le service à chaque création ou modification (`GenericError`, clé
  `regularisationStockNonAutorisee`).
- **Clôture** : `AjustementService.regulariserALaVente` écrit, avant la sortie, un `Ajust` clôturé
  d'office et une ligne `AJUSTEMENT_IN` de `demandé − max(stock, 0)` au motif « Régularisation
  constatée à la vente », attribué à l'utilisateur, commentaire « Vente <numéro> ». Rien n'est écrit
  si le stock a été reconstitué entre-temps.
- La régularisation **n'emprunte pas** `saveItems`, pour deux raisons découvertes en chemin : il
  remet `qty_ug` à zéro (la vente qui suit consomme des UG, qui passeraient en négatif), et
  `StockProduit.totalStockQuantity` est une `@Formula` lue au chargement, que la sortie de vente
  aurait lue périmée. La régularisation préserve les UG et rafraîchit le total.

Tests : `SalesLineServiceImplForcageTest`, `SaleLineDTOForcageJsonTest` (contrat JSON),
`AjustementServiceTest` (`regulariserALaVente` : entrée clôturée et attribuée, UG préservées, stock
négatif, lots, journal et trace, motif absent), `SalesLineServiceIntegrationTest` (PostgreSQL :
service entier sans avoir, refus sans privilège, administrateur, pas de motif sans manque,
ajustement écrit à la clôture avec stock avant/après, dettes d'avoir préservées, stock reconstitué
avant la clôture, rupture inchangée, incrément qui garde l'écart).

**Lot 3** — le choix à l'écran :

- `ForceStockChoiceModalComponent` (`features/sales/ui/force-stock-choice-modal/`) remplace la
  question « Voulez-vous continuer ? » sur l'erreur `stock` : « Le client sera livré plus tard »
  (offert avec `pr-force-stock`) / « Le produit est en rayon, la machine se trompe » (offert avec
  `pr-regulariser-stock-vente`) / « Annuler ». Le focus initial est sur « Annuler » : un Entrée
  résiduel ne force rien.
- `force-stock.mixin.ts` ouvre ce choix et pose `forceStock` **et** `motifForcage` sur la ligne ; il
  est commun aux cinq écrans (comptant, assurance, carnet, devis, vente dépôt). La vente dépôt est
  couverte : sa clôture passe par `SalesLineService.save`, donc la régularisation s'applique au rayon
  de l'officine.
- Le choix s'affiche pour qui détient l'un **ou** l'autre privilège ; la confirmation du transfert
  réserve → rayon reste inchangée et exige `pr-force-stock`.
- `AuthorizationService.canRegulariserStock()`, `Authority.PR_REGULARISER_STOCK_VENTE`, type
  `MotifForcageStock` sur `ISalesLine` ; la ligne créée depuis la recherche porte désormais
  `produitLibelle`, pour que la modale nomme le produit.

Tests (Jest) : `force-stock-choice-modal.component.spec.ts` (options selon les privilèges, motif
renvoyé, annulation, focus), `force-stock.mixin.spec.ts` (motif transmis à la création, à l'ajout et
à l'édition dans le tableau, annulation, transfert réserve sans motif, déclenchement selon les
privilèges), `authorization.service.spec.ts`. Typage vérifié par `tsc` ; ESLint n'a pas de
configuration pour `pharmaSmart-app/src/main/webapp`. Le parcours e2e `vte-41-forcer-le-stock.spec.ts`
attend désormais le nouveau choix (non rejoué : il exige une démo réinitialisée). L'application
Android (`sales-android`) envoie toujours `forceStock` seul, donc une rupture.

**Lot 4** — la liste de travail du pharmacien :

- `GET /api/ajustements/ecarts-a-regulariser` → `EcartStockDTO` (produit, CIP, stock machine, dû aux
  avoirs, écart). Requête native `StockProduitRepository.findEcartsARegulariser` : stock du magasin
  courant **tous emplacements et UG comprises** (même assiette que le contrôle de clôture d'avoir),
  moins la somme des avoirs `OUVERT` ; ne retient que `stock + dû < 0`, plus grand écart en tête.
  Lecture seule.
- Écran `/features-ajustement/ecarts` (`EcartsARegulariserComponent`), atteint par le bouton
  « Écarts à régulariser » de l'historique des ajustements — pas de nouvelle entrée de menu, donc
  les mêmes droits que les ajustements. Bandeau d'explication, total d'unités non expliquées,
  bouton « Nouvel ajustement ».

Tests : `EcartsARegulariserIntegrationTest` (PostgreSQL : négatif sans avoir, couvert, partiellement
couvert, avoirs clos ou annulés ignorés, réserve et UG comptées, positif absent, tri),
`AjustementServiceTest` (calcul de l'écart), `ecarts-a-regulariser.component.spec.ts`. Build Angular
de développement sans erreur.

**Lot 5** — cahier de recette (`cahier-recette.model.ts`, JSON régénéré) :

- **VTE-41** réécrit : la question posée, le choix « Le client sera livré plus tard », le stock qui
  descend de toute la quantité demandée.
- **VTE-63** (nouveau) : « Le produit est en rayon, la machine se trompe » — client servi sans avoir
  ni client obligatoire, ajustement « Régularisation constatée à la vente » dans l'historique.
- **VTE-28** : la clôture vérifie le stock seulement quand le produit est remis ; un avoir remboursé
  rend la quantité due au stock.
- **STK-46** (nouveau) : la liste « Écarts à régulariser ».
- Parcours e2e : `vte-41` adapté, `vte-63-regulariser-le-stock-a-la-vente.spec.ts` (produit jetable à
  stock nul : vente de 2, ajustement dans l'historique, stock final à 0) et
  `stk-46-ecarts-a-regulariser.spec.ts`. Reconnus par Playwright (`--list`), **non rejoués** :
  l'application n'était pas démarrée, et une campagne exige une démo réinitialisée.
- La clôture d'avoir après réception exacte (bug 2) reste couverte par les tests d'intégration,
  pas par un parcours : le jeu de démonstration n'offre pas d'avoir dont on maîtrise le stock.

**Limite connue** : après « transfert réserve → rayon », si la réserve ne couvre pas tout, le
reliquat part en rupture sans que le caissier choisisse. Comportement antérieur, conservé.

**Point ouvert, hors périmètre** : `AjustementService.saveItems` remet `qty_ug` à zéro à chaque
ajustement validé depuis l'écran, sans trace, et enregistre un `stock_before` UG comprises face à un
`stock_after` UG exclues. À confirmer comme règle de gestion ou à corriger.
