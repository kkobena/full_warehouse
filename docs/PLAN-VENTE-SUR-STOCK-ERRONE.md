# Plan — Vendre sur un stock erroné, sans le confondre avec un avoir

## Le problème en une phrase

Le forçage de stock a été construit pour **un** besoin — la rupture qu'on solde en avoir — et il
sert aujourd'hui à **deux**. Le second, l'écart d'inventaire, passe par la porte du premier et en
sort déformé : un avoir dû à un client qui est pourtant reparti servi, et un stock informatique
que la vente vient d'écarter un peu plus de la réalité.

---

## Deux besoins, qu'il faut cesser de confondre

### A — Rupture réelle → avoir client

Le rayon est vide, la machine dit vrai. Le client paie aujourd'hui et repart les mains vides ;
l'officine lui doit la marchandise et la lui remettra après la prochaine entrée en stock.

Un client identifié est ici **légitimement obligatoire** : sans lui, on ne sait pas à qui livrer.
C'est le besoin que l'application couvre, et elle le couvre correctement.

### B — Écart d'inventaire → servir et corriger

Le rayon a des boîtes que la machine ignore : réception mal saisie, retour non enregistré, vol non
constaté. Le client repart **servi**, immédiatement, avec la marchandise en main.

Rien n'est dû à personne. Aucun avoir n'a de sens, aucun client n'a à être saisi — c'est une vente
comptant ordinaire. Ce qui doit changer, c'est le **stock informatique**, qui vient d'être démenti
par les faits.

Ce besoin n'est couvert par rien aujourd'hui.

---

## État actuel — ce que le code fait réellement

La chaîne, du clic à la base :

| Étage | Fichier | Comportement |
|---|---|---|
| Résolution du stock | `SalesLineServiceImpl.getCurrentStockQuantity` | `forceStock=true` ne crée pas de stock : il évite seulement la levée de `StockException`, et déclenche le transfert réserve → rayon quand la réserve peut combler. |
| Quantité servie | `SalesLineServiceImpl.calculateQuantitySold` | `0` si le stock est nul, sinon `min(stock, demandé)`. **La quantité servie ne dépasse jamais le stock réel.** |
| Clôture | `SalesLineServiceImpl.save(Set<SalesLine>, …)` | `quantityAvoir = quantityRequested − quantitySold`, puis `avoirClientDocumentService.createAvoirsFromSale(ligne, sale.getCustomer())` dès que cet écart est positif. |
| Sortie de stock | `StockUpdateService.updateStock` | Suit la quantité **servie**. Un produit à zéro ne bouge pas. |
| Détection front | `SalesStore.isAvoir` | `total demandé ≠ total servi` — aucune autre condition. |
| Garde encaissement | `sale-payment.facade.ts:37-45` | `avoir && !customerId` → refus : « Un client est obligatoire pour une vente avec avoir (livraison partielle) ». |
| Dialogue | `force-stock.mixin.ts` | Une seule question, une seule intention : « La quantité saisie est supérieure à la quantité stock du produit. Voulez-vous continuer ? » |

### Ce que cela produit sur le cas B

1. Le caissier force, croyant dire « la machine se trompe, je sers ».
2. `quantitySold = 0` : **rien ne sort du stock informatique**, qui reste à zéro alors que le rayon
   vient de perdre les boîtes réellement remises. L'écart ne se corrige pas — il grandit.
3. `quantityAvoir = quantité demandée` : un avoir est créé pour une marchandise déjà livrée. La
   dette est fictive, et elle restera ouverte dans la liste des avoirs.
4. L'encaissement exige un client. Au comptoir, on saisit donc un client de complaisance, ou on
   renonce.

Trois conséquences, une seule cause : l'application n'a aucun moyen d'apprendre que le stock qu'elle
affiche est faux.

---

## Cible

Le forçage cesse d'être un booléen et devient un **motif**. Deux branches, deux traitements :

| | A — Rupture (existant) | B — Écart d'inventaire (nouveau) |
|---|---|---|
| Quantité servie | bornée au stock | **égale à la quantité demandée** |
| Quantité en avoir | l'écart | **zéro** |
| Avoir client | créé | aucun |
| Client obligatoire | **oui** | non |
| Stock informatique | inchangé | **corrigé** (voir arbitrage ci-dessous) |
| Privilège | forçage de stock | privilège distinct (il corrige du stock) |
| Trace | ligne en avoir | mouvement d'ajustement motivé |

---

## Arbitrage à trancher — comment corriger le stock

Deux façons de rendre compte de la sortie sur le cas B.

**Option 1 — laisser le stock passer en négatif.** La sortie suit la quantité demandée, sans
plancher. Simple, et l'écart devient visible à l'inventaire.
*Contre :* un stock négatif se propage à tout ce qui valorise l'existant — valorisation du stock,
marge, suggestions de réapprovisionnement, seuils. Il faudrait vérifier chaque consommateur, et
un négatif oublié quelque part produit des montants négatifs inexplicables dans un rapport.

**Option 2 — écrire un ajustement d'entrée, puis vendre normalement.** Avant la sortie, un
mouvement d'ajustement porte le stock à la quantité demandée, avec un motif dédié
(« Régularisation constatée à la vente »). La vente se déroule ensuite sans rien de particulier.
*Pour :* le stock reste positif partout, l'écart est daté, motivé, attribué à un utilisateur, et
il apparaît dans l'historique des mouvements comme n'importe quelle régularisation. Le référentiel
`motif_ajustement` existe déjà et n'attend qu'une entrée de plus.
*Contre :* un mouvement de plus par vente forcée, et une entrée d'ajustement qui n'a pas été
décidée depuis l'écran d'ajustement.

**Recommandation : option 2.** Un stock négatif est une information vraie posée à un endroit où
personne ne l'attend ; l'ajustement dit la même chose à l'endroit prévu pour cela, et se relit six
mois plus tard. Le coût — un mouvement supplémentaire — est négligeable devant l'audit de tous les
consommateurs de `qty_stock` qu'imposerait l'option 1.

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

porté par `SaleLineDTO.motifForcage` (nullable) et persisté sur `sales_line`. `forceStock` reste,
mais devient dérivé : `motifForcage != null`. Les deux cohabitent le temps d'une version, puis
`forceStock` disparaît des DTO.

### 2. Backend

- `calculateQuantitySold(quantityRequested, currentStock, motif)` : renvoie `quantityRequested`
  quand `motif == ECART_INVENTAIRE`, le `min` actuel sinon.
- Sur `ECART_INVENTAIRE`, avant la sortie de stock : écrire l'ajustement d'entrée de
  `quantityRequested − stockDisponible` unités via le service d'ajustement existant, avec le motif
  dédié et l'utilisateur courant.
- `SalesLineServiceImpl.save(Set<SalesLine>, …)` : `quantityAvoir` vaut zéro sur cette branche,
  donc `createAvoirsFromSale` n'est pas appelé — aucune règle à ajouter, la condition
  `quantityAvoir > 0` suffit déjà.
- Contrôle de privilège dédié côté service, et pas seulement côté écran.

### 3. Front

- `force-stock.mixin.ts` : le dialogue à deux boutons devient un **choix à deux branches** —
  « Le client sera livré plus tard » / « Le rayon a le produit, la machine se trompe ». Le second
  n'apparaît qu'avec le privilège de régularisation.
- `SalesStore.isAvoir` ne peut plus se déduire de `demandé ≠ servi` : la branche B garde les deux
  quantités égales, donc le calcul actuel devient juste par construction. **Aucun changement** —
  c'est le signe que la modélisation est la bonne.
- `sale-payment.facade.ts` : la garde client reste inchangée, et ne mordra plus sur le cas B
  puisqu'il ne produit pas d'avoir.

### 4. Migration Flyway

- `ALTER TABLE sales_line ADD COLUMN motif_forcage varchar(20)` (nullable).
- Une ligne dans `motif_ajustement` : « Régularisation constatée à la vente ».
- Rétro-compatibilité : les lignes existantes portant `force_stock = true` sont, par construction,
  des `RUPTURE_AVOIR` — elles ont produit des avoirs. Un `UPDATE` les qualifie comme telles.

### 5. Privilèges

Le forçage actuel autorise à *vendre* ce qu'on n'a pas. La régularisation autorise à *corriger le
stock* — c'est un pouvoir d'inventaire, pas de caisse. Deux privilèges distincts, quitte à ce que
le même profil les porte tous les deux au départ.

---

## Effets de bord à vérifier

- **Inventaire** : un écart régularisé à la vente doit rester lisible dans l'écart d'inventaire
  suivant, pas s'y fondre silencieusement.
- **Déclaration de CA** : la vente est une vente comptant ordinaire, aucun impact attendu — à
  confirmer sur `declarationCaService.appliquerExclusions`.
- **Valorisation du stock et marge** : l'ajustement d'entrée se fait à quel prix ? Au PMP courant,
  faute de mieux, et c'est à assumer explicitement dans le mouvement.
- **VTE-42 (transfert réserve → rayon)** et **VTE-43 (déconditionnement)** : inchangés. Ils servent
  déjà la totalité de la commande et ne produisent pas d'avoir.
- **Liste des avoirs clients** : les avoirs fictifs déjà créés par des forçages du cas B ne sont pas
  rattrapables automatiquement — ils sont indiscernables des vrais. À clôturer à la main.

---

## Lots de livraison

**Lot 1 — Le socle.** L'enum, la colonne, la migration, le motif d'ajustement, et le paramètre
supplémentaire de `calculateQuantitySold`. Aucun changement de comportement : tous les appels
passent `RUPTURE_AVOIR`. Livrable vérifiable, invisible à l'utilisateur.

**Lot 2 — La branche B côté serveur.** Régularisation, sortie de stock complète, privilège dédié.
Testable par l'API avant tout écran.

**Lot 3 — Le choix à l'écran.** Le dialogue à deux branches, le privilège côté front, les libellés.

**Lot 4 — Cahier de recette.** VTE-41 reste le scénario de l'avoir, corrigé en ce sens. Un nouveau
scénario décrit la régularisation : le client repart servi, aucun avoir n'est créé, et le mouvement
d'ajustement porte le motif. Le parcours doit vérifier les trois.

---

## Ce qui a déjà été fait

La description de **VTE-41** a été corrigée : elle annonçait « quand le stock affiché est erroné »
— soit exactement le cas B, que l'application ne traite pas. Elle décrit désormais ce que le code
fait vraiment : quantité servie bornée au stock, écart parti en avoir, client obligatoire pour
encaisser. Le cas B n'y est plus évoqué du tout, puisqu'il n'existe pas encore — c'est ce document
qui en tient lieu, jusqu'à ce qu'un scénario propre lui soit consacré (lot 4).

Le commentaire d'en-tête du parcours `vte-41-forcer-le-stock.spec.ts` a été corrigé lui aussi : il
annonçait un passage du stock en négatif, ce qui n'arrive pas — rien ne sort de ce qui n'existe pas.
