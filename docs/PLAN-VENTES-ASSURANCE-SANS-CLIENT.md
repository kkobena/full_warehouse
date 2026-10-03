# Point à traiter — ventes assurance sans client, laissées par un changement de type de vente

> Deux symptômes, une même origine (la transformation comptant → assurance) : les ventes orphelines (§1 à §5) et
> l'identifiant périmé après transformation (§6).

> **Hors périmètre** du chantier des styles du comptoir ([PLAN-STYLES-COMPTOIR.md](PLAN-STYLES-COMPTOIR.md)) :
> noté ici pour être repris à part. Aucune correction de fond n'est faite à ce stade, seulement un
> garde-fou à la lecture (§3).

## 1. Constat

Des ventes `ThirdPartySales` (assurance, « VO ») existent en base **sans client rattaché**. Une
prévente de ce type remontait dans `SaleDataService.allPrevente` avec une valeur `null` à la place de son
`SaleDTO` : le front, qui lit les informations de l'assuré, plantait sur cet élément.

## 2. Origine (décrite par le métier, code lu en partie)

Elles naissent quand le caissier **change de type de vente en cours de route** — il passe de l'onglet
Comptant à l'onglet Assurance avec un panier non vide — **puis abandonne sans aller au bout**.

Chemin de code relevé (front, `sales-home.component.ts`) :
1. `onNavChange` demande confirmation (« Changement de type de vente »), puis appelle `dispatchTabTransition`.
2. `COMPTANT → ASSURANCE` : `switchComptantToAssurance()` vide le client (`setSelectedCustomer(null)`) puis
   `onChangeCashSaleToVo()` → `salesFacade.transformCashSaleToAssurance()`.
3. La vente comptant **est donc transformée côté serveur en vente assurance, sans client**, avant même qu'un
   assuré soit choisi. Si l'utilisateur quitte l'écran à ce moment, la vente reste en base, incomplète.

Le même schéma vaut pour `COMPTANT → CARNET` (`switchComptantToCarnet`), qui vide aussi le client puis
transforme la vente ; **à vérifier** : un carnet sans client se comporte-t-il pareil ?

Non relu à ce jour : le code serveur de `transformCashSaleToAssurance` (ce qu'il crée, ce qu'il conserve).

## 3. Déjà fait

`predicatesPrevente` (`SaleDataService`) exclut maintenant, **côté requête**, les `ThirdPartySales` sans client :
`type <> ThirdPartySales OU client non nul`. Les trois méthodes qui l'emploient — `allPreventes`,
`allPreventeVNO`, `allPreventeVO` — n'envoient plus de `null` au front. Test :
`SaleDataServiceIntegrationTest#preventeAssuranceSansClientNonRenvoyee`.

C'est un **masque**, pas un remède : les ventes orphelines restent en base.

## 4. À faire

### 4.1 Auditer les autres lecteurs
`SaleDataService.buildSaleDTO` renvoie `null` pour toute vente assurance sans client (`buildFromEntity`).
D'autres appels passent par lui et peuvent encore produire des éléments nuls :
- `SaleDataService.java:228` — `salesRepository.findAll(specification, page).map(this::buildSaleDTO)` ;
- le journal des ventes, les ventes en cours (`countPendingSales`) et la liste des ventes en attente :
  à relire pour savoir s'ils comptent ou affichent ces ventes.

Question de fond : un lecteur doit-il **filtrer** ces ventes, **les afficher** avec un client vide, ou le
renvoi de `null` doit-il disparaître au profit d'une exception explicite ?

### 4.2 Traiter la cause : ne plus fabriquer de vente sans client
Options, à arbitrer (c'est une règle de gestion, pas un correctif technique) :

| Option | Principe | Réserve |
|---|---|---|
| A — transformation différée | le changement d'onglet reste côté écran ; la vente n'est transformée côté serveur qu'au choix de l'assuré | le panier doit survivre au changement d'onglet sans vente assurance en base |
| B — annulation au départ | si l'utilisateur quitte l'écran ou change d'onglet sans client, la vente transformée est annulée ou remise en comptant | à écrire aux deux endroits (navigation, fermeture) |
| C — nettoyage planifié | un traitement périodique supprime ou annule les ventes assurance sans client plus anciennes que N heures | ne supprime pas la cause ; à valider côté stock et caisse (une vente « en cours » a pu prélever du stock) |

### 4.3 Nettoyer l'existant
Compter, puis décider du sort des ventes déjà orphelines. Requête de dénombrement (schéma `pharma_smart`) :

```sql
SELECT id, statut, sales_amount, created_at
FROM sales
WHERE dtype = 'ThirdPartySales' AND customer_id IS NULL
ORDER BY created_at;
```

À examiner avant toute suppression : le stock prélevé, les lots débités et les éventuels règlements liés. Voir
aussi la règle sur le stock négatif (une vente forcée sort la quantité demandée).

## 5. Critères de sortie
- Plus aucune vente `ThirdPartySales` sans client créée par un changement d'onglet (§4.2).
- Plus aucun `null` renvoyé dans une liste de ventes (§4.1), avec un test par lecteur.
- Les ventes orphelines existantes ont été comptées et traitées (§4.3).
- Après une transformation comptant → assurance, choisir l'assuré calcule les montants et l'ajout de produit reste
  possible (§6).

## 6. Second symptôme — l'identifiant de la vente change à la transformation (relevé le 2026-10-03)

### 6.1 Ce que voit le caissier
Sur une vente comptant avec des lignes, il passe à l'onglet Assurance puis choisit l'assuré : **les montants ne se
calculent pas** et **on ne peut plus ajouter de produit**. Côté serveur :

```
jakarta.persistence.EntityNotFoundException: No row with the given identifier exists for entity
[com.kobe.warehouse.domain.ThirdPartySales with id '{id:4151, saleDate:2026-10-03}']
```

### 6.2 Établi (code et base lus)
- `ThirdPartySaleServiceImpl.changeCashSaleToThirdPartySale` ne convertit pas la vente : elle en **crée une nouvelle**
  (`copyFromCashSale` → `setId` → `idGeneratorService.nextId()`), y rattache les lignes, **supprime la vente comptant**,
  et renvoie le nouvel identifiant. L'identifiant de la vente **change donc** à chaque transformation.
- Base de démonstration, juste après l'incident : la vente **4151 n'existe pas** ; **4152** existe, `ThirdPartySales`,
  `ACTIVE`, **sans client**, 25 645 F, avec la ligne du produit. 4151 est l'ancienne vente comptant, supprimée par la
  transformation ; 4152 la vente assurance qui l'a remplacée.
- L'exception vient d'un `getReferenceById(SaleId)` : il rend un proxy sans interroger la base, et l'erreur n'éclate qu'au
  premier accès à ses champs. `updateTransformedSale` (`findById`, puis `setCustomer`) et les autres méthodes qui
  retrouvent la vente par son identifiant la lèvent donc dès qu'on leur donne **4151**.
- Côté écran (`features/sales`), `doTransformCashSale` relit bien la vente sous son nouvel identifiant
  (`transformSale` → `findSale` → `setCurrentSale`). En revanche `voFromCashSale`, posé à `true` à ce moment, **n'est lu
  nulle part** dans `features/sales` ; et l'appel `PUT /sales/assurance/transform/add-customer` n'est émis que par l'ancien
  `entities/sales/service/vo-sales.service.ts`.

### 6.3 Hypothèse à vérifier (non établie)
Un appel émis **après le choix de l'assuré** envoie encore l'ancien identifiant 4151 — repris d'un état de l'écran qui n'a
pas suivi la transformation (copie de la vente dans un composant, identifiant capturé avant la transformation, ou chemin
de l'ancien écran). Tout ce qui suit échoue alors avec la même exception : calcul des montants, ajout de produit.
**Vérification** : rejouer le scénario avec l'onglet réseau ouvert et lire l'identifiant envoyé par la première requête qui
échoue ; comparer à celui renvoyé par `GET /sales/assurance/transform`.

### 6.4 À faire
1. **Reproduire** (comptant avec une ligne → Assurance → choisir l'assuré) et identifier la requête qui porte 4151.
2. Corriger l'appelant pour qu'il utilise l'identifiant renvoyé par la transformation, puis ajouter un test e2e :
   la transformation suivie du choix de l'assuré doit afficher les montants et accepter un second produit.
3. Faire échouer proprement côté serveur : un identifiant inconnu doit rendre un 404 explicite, pas une
   `EntityNotFoundException` levée au premier accès d'un proxy (500). Voir aussi `findById` de `ThirdPartySaleServiceImpl`.
4. Question de fond, liée au §4.2 : faut-il garder une transformation qui **change l'identifiant** de la vente, ou la
   faire en place ? Une transformation en place supprimerait à la fois cette classe de bug et les ventes orphelines
   issues d'un abandon. C'est une règle de gestion : à arbitrer, rien n'est modifié.

**Reste en base de démonstration :** la vente 4152 (assurance sans client, 25 645 F) est un orphelin du §4.3, à purger par
l'utilisateur depuis « Ventes en cours ».
