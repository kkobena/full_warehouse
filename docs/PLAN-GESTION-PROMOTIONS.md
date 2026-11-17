# Plan — Gestion des promotions

> État : **proposition**. Rien n'est implémenté, rien n'est tranché. Les chiffres de ce document
> ont été mesurés le 2026-11-17 sur la base de recette `pharma_smart_e2e` (1 127 produits,
> 21 219 ventes) et sur le code à la même date.

## Décisions à prendre

Elles sont rassemblées au §9. Six questions, dont une réglementaire qui commande tout le reste :
**un médicament à prix administré peut-il être promu ?** Tant qu'elle n'est pas tranchée, la
conception ci-dessous suppose que **non**, et borne les promotions aux articles à prix libre.

---

## 1. Ce que « promotion » veut dire ici

Une promotion est une **décision commerciale datée, portant sur un périmètre d'articles, qui fait
payer au client moins que le prix affiché, sans que personne ait à le saisir au comptoir.**

Quatre mots portent tout le travail :

- **datée** — elle commence et elle finit, et le logiciel la met en route et l'arrête tout seul.
  C'est ce qui la distingue d'une remise : personne ne doit se souvenir de la retirer.
- **périmètre** — elle désigne des articles, pas un client et pas une vente.
- **sans saisie** — c'est le moteur qui l'applique. Une remise que la caissière doit choisir n'est
  pas une promotion : elle est oubliée les jours de monde, et appliquée deux fois les autres.
- **moins que le prix affiché** — le prix de référence reste le prix, et la différence est une
  remise. Le §5.1 explique pourquoi ce choix n'est pas cosmétique.

Ce n'est **pas** : une remise de courtoisie (elle existe, §2.1), un tarif tiers payant
(`OptionPrixProduit`, déjà là), une remise d'achat fournisseur (inexistante dans l'espace UEMOA —
voir la note sur le prix administré), ni une carte de fidélité (sujet distinct, voir
`docs/PLAN-FIDELITE-CLIENT.md`).

---

## 2. État des lieux mesuré

### 2.1 Ce qui existe : la remise manuelle, choisie vente par vente

| Élément | Ce qu'il fait |
|---|---|
| `remise` (table) + héritage | Socle : `libelle`, `enable` |
| `RemiseProduit` + `GrilleRemise` | Une **grille** : pour chaque code (`CODE_12`…`CODE_21`, types VO/VNO), un taux |
| `Produit.codeRemise` | Le code de l'article, qui choisit la ligne de la grille |
| `RemiseClient` | Un taux unique sur un montant remisable — **à décommissionner**, décidé |
| `Sales.remise` | **Une** remise par vente, posée par la caissière |
| `SalesLine` | `taux_remise`, `discount_unit_price`, `discount_amount` |
| `AuthorizationService` | `canApplyDiscount()`, `requestDiscountAuthorization()` — l'autorisation existe déjà |

Le calcul tient en quatre lignes, dans `SalesLineServiceImpl.processProductDiscount` :

```java
getGrilleRemise(salesLine.getProduit().getCodeRemise(), remiseProduit, sales).ifPresent(grille -> {
    int discount = (int) Math.ceil(salesLine.getSalesAmount() * grille.getTauxRemise());
    salesLine.setDiscountAmount(discount);
    salesLine.setTauxRemise(grille.getTauxRemise());
});
```

**Mesuré en base** : 1 remise définie, 10 grilles, **0 vente portant un `remise_id`**, et pourtant
**3 864 lignes remisées pour 2 620 474 F**. Les remises réellement pratiquées sont donc posées
**ligne à ligne**, pas par le mécanisme de grille. Celui-ci est en place et inemployé.

### 2.2 Ce qui n'existe pas

Aucune trace de promotion dans le code : ni entité, ni table, ni migration, ni écran. Et le
mécanisme de remise n'a **aucun** des attributs d'une promotion :

- pas de **dates** — rien ne commence ni ne finit ;
- pas de **périmètre produit** — la grille s'applique à tout article portant le code ;
- pas de **magasin** — alors que l'officine et l'unité de gestion n'ont ni le même prix libre ni le
  même assortiment (98 références propres à l'unité, mesuré) ;
- pas de **seuil de quantité**, pas de **lot** (« 2 + 1 ») ;
- pas de **priorité** ni de **règle de cumul** ;
- pas d'**application automatique** : il faut la choisir ;
- pas de **restitution** — aucun rapport ne dit ce qu'une opération a coûté ni fait vendre ;
- pas d'**historique du prix de vente** : seul `FournisseurProduitPriceHistory` existe, côté achat.
  Ce dernier point décide du §5.1.

### 2.3 Trois défauts mesurés du mécanisme actuel

**a. La remise n'est appliquée qu'une fois, au moment où on la choisit.** Dans
`SalesLineServiceImpl`, quatre appels à `processProductDiscount(salesLine)` sont **commentés** —
lignes 292, 318, 473 et 525, c'est-à-dire exactement aux quatre points de mutation du panier
(`updateSaleLine`, `updateItemRegularPrice`, `updateItemQuantitySold`, `updateStock`). Quelqu'un a
donc tenté la réapplication et y a renoncé. Conséquence : un article ajouté après le choix de la
remise n'est pas remisé. Un moteur de promotions **ne peut pas** reproduire ce schéma (§5.4).

**b. Le chemin d'import ne porte pas de taux.** `buildSaleLineFromDTO` pose `discount_amount`
depuis le DTO, et `SaleLineDTO` **n'a pas de champ `tauxRemise`**. Or la FNE construit sa ligne avec
le **taux** : `new FneInvoiceItem(…, detailProduitFacture.getTauxRemise())`. Une vente importée
portant une remise la déclarerait donc à zéro. Aucune ligne de la base n'est dans ce cas
aujourd'hui (3 864 sur 3 864 portent un taux), mais le chemin est celui de l'import des reventes du
dépôt — la phase 4 du plan hiérarchie.

**c. Deux arrondis se suivent et leur ordre compte.** La remise est arrondie **au supérieur**
(`Math.ceil`), puis l'encaissement espèces est arrondi au multiple de 5 (`arrondirMontantCaisse`,
appelé après `computeCashSaleAmountToPaid`). Les montants sont des entiers de francs. Toute
promotion exprimée en pourcentage traverse ces deux arrondis, dans cet ordre, ou le ticket ne
boucle plus.

---

## 3. Le cadre réglementaire borne le périmètre

En Côte d'Ivoire, le prix des médicaments est **administré** : le décret n°94-667 du 21 décembre
1994 fixe les conditions d'acquisition et le régime des prix, et la politique pharmaceutique
nationale renvoie à des taux de marge par spécialité. La publicité des médicaments relève du décret
2016-714 du 17 septembre 2016. Je **n'ai trouvé aucun texte ivoirien autorisant une remise sur le
prix d'un médicament** — ni l'interdisant explicitement d'ailleurs ; c'est précisément pourquoi
c'est une question à trancher avec votre conseil, pas dans un plan technique.

Le droit français, qui n'oblige en rien ici mais qui a été plaidé, va dans le même sens : le prix
des médicaments non remboursables est libre, mais l'Autorité de la concurrence évoquait en 2013 une
remise commerciale plafonnée à 17 %, et la CJUE a jugé qu'un État peut interdire aux pharmacies les
offres promotionnelles sur les **médicaments**, à condition d'épargner la parapharmacie.

**La ligne de partage est mesurable dans la base** :

| TVA | Statut légal | Articles | Lecture |
|---|---|---|---|
| 0 % | LISTE_I, LISTE_II, PSO, STUPEFIANTS | 934 | médicament, prix administré |
| 0 % | SANS_LISTE | 31 | médicament conseil, prix administré |
| **18 %** | SANS_LISTE | **162** | **article taxable — prix libre** |

Les 162 articles à 18 % sont le terrain naturel d'une promotion. C'est aussi, et ce n'est pas un
hasard, l'essentiel de l'assortiment d'une unité de gestion : les 98 références de la parapharmacie
de recette sont toutes `SANS_LISTE`.

**Conséquence de conception** : le périmètre d'une promotion est contrôlé **à la création**, pas au
comptoir. Un refus à la caisse serait découvert par la cliente ; un refus à la création est lu par
celui qui se trompe. Le critère par défaut est « TVA ≠ 0 », et il doit rester **un paramètre**, pas
une constante : c'est une règle de gestion, elle changera avant le code.

---

## 4. Comment les autres éditeurs traitent ce besoin

### 4.1 Winpharma — la promotion est une donnée reçue, désactivée par défaut

Winpharma publie l'interface « Push Promotions » entre son LGO et l'application de gestion de
promotions d'un groupement. Trois éléments méritent d'être repris tels quels :

- les types sont **peu nombreux et explicites** : remise en pourcentage, remise en montant, et
  remise en pourcentage **sur le moins cher de N produits** ;
- **toutes les promotions sont intégrées au statut « Désactivé »** ; c'est ensuite le LGO qui les
  active **selon leurs dates** ;
- une promotion **supprimée à la main n'est pas réactivable**.

Autrement dit : l'import ne décide pas, les dates décident, et une suppression est définitive. Les
deux premiers points sont de bonne ingénierie ; le troisième est une protection contre la
résurrection accidentelle d'une opération terminée.

### 4.2 Le marché français : deux éditeurs, et la promotion comme fonction d'encaissement

Smart Rx et Pharmagest (LGPI) tiennent plus de 70 % du parc officinal français, Winpharma faisant
figure de challenger. La documentation publique de LGPI et Smart Rx sur le paramétrage des remises
de parapharmacie n'est pas accessible — je le dis plutôt que de le deviner. Ce qui est public, c'est
que la promotion y est traitée comme une **fonction de l'encaissement et de la synchronisation web**
(stocks, prix et promotions synchronisés entre l'officine et son site), et non comme un module de
gestion séparé.

### 4.3 Odoo — programmes de promotion séparés des listes de prix

Odoo distingue deux objets : la **liste de prix** (un prix pour un segment, durable) et le
**programme de promotion / fidélité** (daté, conditionnel). Les types natifs sont « Promotion »,
« Code de réduction » et « Buy X Get Y », ce dernier implémenté comme un **crédit** : X articles
achetés créditent un droit, échangeable contre Y. La remise « sur le moins cher » n'est **pas**
native et passe par des modules tiers.

La leçon utile est la séparation : le prix du catalogue et la mécanique promotionnelle ne sont pas
le même objet et ne vivent pas dans la même table.

### 4.4 Les moteurs du commerce de détail — priorité, classes de cumul, plancher de marge

La littérature des moteurs de promotion converge sur un petit nombre de règles :

- chaque promotion déclare **explicitement** avec quoi elle se cumule ; l'ordre d'application vient
  d'une **priorité**, jamais de la date de création ;
- une promotion **exclusive** s'applique seule ;
- piège documenté : regrouper toutes les promotions sans classe déclarée dans un même seau les rend
  mutuellement exclusives **par accident** ;
- pour un lot, **l'unité offerte est la moins chère** du groupe, et un lot incomplet ne remise rien ;
- le vrai risque est le **cumul** : l'empilement d'un lot et d'un pourcentage dépasse vite 60 % de
  remise effective. D'où le **plancher de marge** comme garde-fou, et une **trace d'explication**
  par ligne.

### 4.5 Ce que j'en retiens pour Pharma-Smart

1. **Peu de types, bien définis.** Pourcentage, montant, prix fixe. Les lots viennent après.
2. **Les dates pilotent, un humain active.** Le statut est explicite, et il ne se déduit pas.
3. **Le cumul se déclare, il ne se devine pas.** Par défaut : pas de cumul.
4. **Un plancher de marge**, parce que `cost_amount` est déjà sur la ligne : le contrôle est gratuit.
5. **Une trace par ligne vendue**, sinon aucun rapport n'est possible et aucun ticket n'est
   justifiable six mois après.

---

## 5. Conception retenue

### 5.1 Une promotion ne change pas le prix : elle produit une remise

C'est la décision structurante, et elle est dictée par trois mesures.

- **Il n'existe aucun historique du prix de vente.** `FournisseurProduitPriceHistory` ne couvre que
  l'achat. Une promotion qui écrirait dans `produit.regular_unit_price` n'aurait aucun moyen
  **prouvable** de restaurer le prix d'avant ; et une promotion oubliée deviendrait un prix.
- **Le prix de référence doit rester lisible.** `SaleAmountCalculator` calcule la TVA en détaxant le
  TTC ligne par ligne, et la FNE reçoit un prix unitaire HT **plus un taux de remise**. Le prix
  barré du ticket et la base taxable sortent du même couple (prix de référence, remise).
- **Tout l'aval sait déjà traiter une remise** : ticket, FNE, déclaration de CA (`exclureUg`),
  ventilation tiers payant, comptabilité. Une promotion qui produit `taux_remise` +
  `discount_amount` ne demande **aucune** modification de cet aval.

Donc : la promotion remplit les mêmes champs qu'une remise manuelle. La nouveauté est **qui décide,
et quand**.

### 5.2 Le modèle

```
promotion
  id, code (unique), libelle
  type            PROMO_POURCENTAGE | PROMO_MONTANT | PROMO_PRIX_FIXE
  valeur          int (taux en %, ou montant en F, ou prix fixe en F)
  date_debut, date_fin     date, bornes incluses
  statut          BROUILLON | ACTIVE | SUSPENDUE | TERMINEE
  priorite        int, défaut 0
  cumulable       bool, défaut false
  quantite_min    int, défaut 1
  created_at, created_by, updated_at

promotion_magasin   (promotion_id, magasin_id)   -- PK composite
promotion_produit   (promotion_id, produit_id)   -- PK composite

sales_line.promotion_id   FK nullable
```

Pourquoi cette forme, et pas une autre :

- **`promotion_magasin` en table de liaison, pas une colonne.** Une opération de groupement porte
  sur plusieurs magasins ; une opération de la parapharmacie sur un seul. Et le modèle suit celui
  des favoris du comptoir, qui a déjà fait ses preuves : `produit_favori` est unique sur
  `(magasin_id, produit_id)` et son service résout le magasin connecté à chaque lecture **et** à
  chaque écriture. On ne réinvente pas, on imite ce qui marche.
- **Pas de magasin implicite.** Une promotion sans ligne de magasin ne s'applique **nulle part**.
  L'alternative — « aucune ligne = tous les magasins » — fait qu'un oubli devient une promotion
  nationale. Le même raisonnement a déjà été fixé par essai pour la portée de supervision, dans
  l'autre sens, et la leçon vaut ici en négatif.
- **`sales_line.promotion_id` plutôt qu'une table d'application.** La ligne porte déjà le montant
  et le taux ; il ne manque que le lien. Une colonne, aucune jointure nouvelle, et le rapport est
  un `GROUP BY`.
- **`valeur` en entier.** Les montants du logiciel sont des entiers de francs (`cost_amount`,
  `regular_unit_price`, `discount_amount`). Un taux en pourcentage entier suffit : une promotion à
  12,5 % n'existe pas dans une officine.

### 5.3 Les invariants

1. Une promotion a **au moins un magasin** et **au moins un produit**.
2. `date_debut <= date_fin`.
3. Tout produit du périmètre est à **prix libre** (critère paramétrable, défaut : TVA ≠ 0).
4. Pour chaque produit du périmètre et chaque magasin visé, le prix après promotion est
   **strictement supérieur au prix de revient** de la ligne (`cost_amount`). Contrôlé à la création
   et à chaque modification.
5. Une ligne de vente porte **au plus une** promotion.
6. Une promotion `TERMINEE` ne redevient jamais `ACTIVE` — on en crée une nouvelle. C'est la règle
   de Winpharma, et elle évite la résurrection d'une opération dont plus personne ne se souvient.
7. Une promotion ne s'applique **jamais** à une ligne remisée à la main, sauf `cumulable`.

### 5.4 Le moteur

`MoteurPromotionService.appliquerLesPromotions(Sales)` — **une fonction du panier**, pas un geste.

```
appliquerLesPromotions(vente) :
    1. effacer ce que le moteur a posé   (lignes dont promotion_id est renseigné)
    2. lire les promotions candidates    (magasin figé de la vente, date de la vente, statut ACTIVE)
    3. pour chaque ligne :
         retenir la promotion la plus avantageuse pour le client
         parmi celles dont le périmètre contient le produit et dont quantite_min est atteinte,
         en départageant par priorite DESC, puis id ASC
    4. poser taux_remise, discount_amount, discount_unit_price, promotion_id
    5. recalculer les montants de la vente (processDiscountCommonAmounts), puis l'arrondi caisse
```

Cinq points non négociables :

- **Appelé à chaque mutation du panier** : ajout, changement de quantité, changement de prix,
  retrait. Ce sont exactement les quatre points où les appels sont aujourd'hui commentés (§2.3a).
  Les décommenter ne suffira pas : le moteur doit être **idempotent**, c'est-à-dire effacer avant de
  recalculer, ce que `processProductDiscount` ne fait pas.
- **Idempotent et borné à ce qu'il a posé.** Une remise humaine (`promotion_id` nul,
  `discount_amount > 0`) n'est jamais écrasée.
- **Le magasin lu est celui de la vente**, pas celui de l'utilisateur connecté. `Sales.magasin` est
  estampillé depuis le caissier (`SaleCommonService:248`), et la délégation
  `openCashRegister(admin, caissier)` a déjà prouvé que les deux diffèrent.
- **La date lue est celle de la vente**, pas `LocalDate.now()` : une vente en attente reprise le
  lendemain, ou une vente différée, ne doit pas changer de prix entre-temps.
- **Déterministe** : priorité puis identifiant. Jamais la date de création, jamais l'ordre d'un
  `Set`. Ce dernier point n'est pas une précaution de style : c'est exactement le défaut qui vient
  d'être corrigé dans `ProduitBuilder`, où un `findFirst()` sur un ensemble rendait un rangement au
  hasard.

### 5.5 La portée magasin

Trois affirmations, chacune vérifiable :

- **L'écran de création préselectionne le magasin de la session** et ne propose que les magasins de
  la portée de l'utilisateur (`PorteeMagasinService.resoudreLaPorteeDeSupervision`). Le sélecteur
  est **masqué s'il n'y en a qu'un** — la convention déjà posée pour les favoris, la gestion de
  caisse et le réassort.
- **Le responsable de l'unité de gestion crée les promotions de son unité**, s'il détient le droit.
  Il en existe un (`ykouame`, `ROLE_RESPONSABLE_COMMANDE` dans le jeu de démo).
- **Un dépôt n'a pas de promotion.** Il ne vend pas au comptoir : ses reventes entrent par import.
  Le filtre est le même que celui du réassort : `commandeEnPropre` d'un côté, « vend au comptoir »
  de l'autre — à nommer explicitement plutôt qu'à déduire du type de magasin.

### 5.6 L'affichage et le ticket

- Au comptoir : prix de référence **barré**, prix remisé, et le **libellé** de la promotion sur la
  ligne. Pas de notification de succès — l'écran montre l'état (règle en vigueur au comptoir).
- Sur le ticket : la ligne porte déjà la remise ; il faut y ajouter le libellé de la promotion,
  faute de quoi la cliente ne sait pas pourquoi elle paie moins.
- Sur la FNE : rien de nouveau, le taux est déjà transmis — à condition que le moteur renseigne
  `taux_remise` **et** `discount_amount`, y compris pour une promotion exprimée en montant ou en
  prix fixe. C'est le §2.3b en positif.
- Dans le catalogue : un badge « en promotion » sur la fiche et dans la grille des favoris du
  comptoir. Les deux écrans sont déjà propres au magasin.

---

## 6. Le plan par phases

### Phase 0 — Les défauts qui gêneraient le moteur

1. Rendre la remise **réapplicable** : remplacer les quatre appels commentés par un appel au moteur
   idempotent. Sans cela, promotion et remise manuelle se contrediront au premier ajout d'article.
2. Ajouter `tauxRemise` à `SaleLineDTO` et le renseigner dans `buildSaleLineFromDTO`, pour que la
   FNE d'une vente importée ne déclare pas une remise nulle (§2.3b).

Livrable court, indépendant, et utile même si la suite est ajournée.

### Phase 1 — Le socle : une promotion simple, datée, par magasin

- Migration : `promotion`, `promotion_magasin`, `promotion_produit`, `sales_line.promotion_id`.
- Domaine : `Promotion`, `PromotionMagasin`, `PromotionProduit` ; DTO **dans leurs propres
  fichiers** ; `PromotionRepository` en JPQL ; `PromotionService` + `PromotionServiceImpl`.
- Moteur : `MoteurPromotionService` + impl, branché aux quatre points de mutation.
- Garde réglementaire : contrôle du prix libre et du plancher de marge **à la création**.
- `nav_item` « promotions » + droit, non accordé par la migration — c'est à l'officine de décider
  qui en dispose, comme pour `supervision.magasins`.
- Écran : liste, création, périmètre produit (recherche au catalogue), magasins, dates, suspension.
  Design System (`app-button`, `app-data-table`, `app-select`), devise via `APP_DEVISE`.
- Comptoir : prix barré, libellé, et recalcul à chaque changement du panier.
- Un travail planifié quotidien qui fait passer `ACTIVE` → `TERMINEE` à l'échéance (et
  `BROUILLON` → `ACTIVE` à la date de début pour une promotion validée).
- **Portée** : ventes au comptant uniquement. Assurance et carnet : §9.
- Essais : les invariants du §5.3, un essai par règle, et deux parcours de recette — « une
  promotion s'applique sans saisie » et « elle s'arrête d'elle-même à l'échéance ».

### Phase 2 — Les mécaniques que le commerce réclame vraiment

- **Lot (N achetés, M offerts)**, l'unité offerte étant la moins chère du groupe. Attention : **ne
  pas** réutiliser `SalesLine.quantity_ug`, qui consomme les unités gratuites du fournisseur
  (`stock_produit.qty_ug`) — deux notions, un seul mot (§7.4).
- **Périmètre par rayon ou par famille**, en plus de la liste explicite.
- **Seuil en montant** (« −10 % au-delà de 20 000 F »), qui est une promotion de panier et non de
  ligne : elle a besoin d'un objet de remise au niveau vente, pas ligne.
- **Cumul** : `cumulable` + classes d'exclusion, et un plancher de marge sur la remise **cumulée**.

### Phase 3 — La restitution

- Ce que l'opération a coûté (somme des `discount_amount` par `promotion_id`), ce qu'elle a fait
  vendre (quantités, tickets, clients distincts), et l'effet marge par rapport à la période
  précédente.
- Export serveur via `CsvExportService` et le droit `EXPORT` existants.

### Phase 4 — L'import d'un groupement

Le fichier de promotions d'un groupement, sur le modèle Winpharma : intégration au statut
**SUSPENDUE**, activation par les dates, et une promotion supprimée non réactivable. À ne faire que
si l'officine adhère à un groupement qui en fournit.

---

## 7. Pièges identifiés

1. **La remise appliquée une seule fois** (§2.3a) : quatre appels commentés dans le code en sont la
   preuve. C'est le piège principal, et il est déjà dans le dépôt.
2. **Deux arrondis successifs** (§2.3c) : `ceil` sur la remise, puis multiple de 5 sur l'espèce. Ne
   pas inverser, ne pas en ajouter un troisième.
3. **La FNE veut un taux** : une promotion en montant ou en prix fixe doit **calculer** le taux
   équivalent, sinon la facture normalisée déclare une remise nulle.
4. **`quantity_ug` est déjà pris.** Il porte les unités gratuites **du fournisseur**, décrémentées
   de `stock_produit.qty_ug` par `StockUpdateService`. Un « 2 + 1 » maison qui l'emploierait
   consommerait le stock gratuit de l'officine pour financer sa propre promotion — invisible au
   bilan, et faux.
5. **`Produit.codeRemise` et `GrilleRemise` ne sont pas un début de promotion.** Les codes 12 à 21,
   typés VO/VNO, sont une convention de grille ; les étendre mélangerait deux sujets.
6. **Le magasin de la vente n'est pas celui de l'utilisateur** (§5.4). La délégation d'ouverture de
   caisse l'a déjà démontré.
7. **Le tiers payant calcule sa part sur le prix.** Remiser une ligne couverte par une assurance
   déplace la charge entre l'assureur et le patient. C'est une question de gestion (§9), pas un
   détail d'implémentation — et c'est pourquoi la phase 1 s'en tient au comptant.
8. **Le stock négatif est une règle de gestion.** Une vente forcée sort la quantité demandée ; une
   promotion ne doit ni la bloquer ni la recalculer.
9. **Une réponse en retard ne doit pas écraser un panier plus récent.** Le catalogue vient d'être
   corrigé de ce défaut exact ; le comptoir recalcule à chaque frappe et y est plus exposé encore.
10. **Les parcours de recette partagent le compte de l'utilisateur.** Une promotion créée par un
    parcours et laissée active changerait les prix d'une démonstration — tout parcours de promotion
    doit nettoyer derrière lui, et le vérifier.

---

## 8. Ce que je ne ferais pas

- **Écrire le prix promotionnel dans `produit.regular_unit_price`.** Sans historique du prix de
  vente, le retour en arrière n'est pas prouvable (§5.1).
- **Un moteur de règles générique.** 162 articles à prix libre, de 0 à 10 magasins : une liste
  d'articles et un sélecteur suffisent. Un langage de règles coûterait dix fois son usage.
- **Étendre `RemiseClient`**, dont le retrait est déjà décidé.
- **Promouvoir un médicament à prix administré** avant d'avoir la réponse du §9.1.
- **Cumuler par défaut.** C'est la première cause documentée de dérive de marge.
- **Faire dépendre une promotion du client.** C'est de la fidélité, et elle a son propre plan.

---

## 9. Questions ouvertes

1. **Un médicament à prix administré peut-il être promu ?** Ma recommandation : non, et le logiciel
   le refuse à la création. Le critère par défaut serait « TVA ≠ 0 » (162 articles). À confirmer
   avec votre conseil, le décret 94-667 fixant prix et marges.
2. **Les promotions s'appliquent-elles aux ventes assurance et carnet ?** Recommandation : non en
   phase 1. Remiser une ligne couverte déplace la charge vers l'assureur ou le patient selon le
   mode de calcul, et ce choix vous appartient.
3. **Le plancher de marge : refus ou avertissement ?** Recommandation : refus à la création, car une
   promotion à perte est presque toujours une erreur de saisie — mais la vente à perte peut être
   voulue (fin de série, péremption proche).
4. **Qui crée une promotion ?** Un droit dédié, et le responsable d'une unité de gestion pour son
   magasin ? Ou l'officine seule pour tous ses magasins ?
5. **Les lots « 2 + 1 » sont-ils attendus en phase 1** ou peuvent-ils attendre la phase 2 ?
6. **Une promotion doit-elle pouvoir viser un rayon entier** dès la phase 1, ou la liste d'articles
   suffit-elle pour commencer ?

---

## Sources

- [Winpharma — API Push Promotions, document client V3](https://www.winpharma.com/wp-content/uploads/Push_Promotions_Clients-V3.pdf)
- [Le Moniteur des pharmacies — les nouvelles expertises des logiciels de gestion officinale](https://www.lemoniteurdespharmacies.fr/revues/le-moniteur-des-pharmacies/article/n-3189/les-nouvelles-expertises-des-logiciels-de-gestion-officinale.html)
- [Le Moniteur des pharmacies — LGO : les dernières fonctionnalités](https://www.lemoniteurdespharmacies.fr/business/economie/achat/lgo-les-dernieres-fonctionnalites)
- [Itekpharma — synchronisation LGO (stocks, prix, promotions)](https://www.itekpharma.com/en/our-turnkey-solution/pharmacy-software-synchronization/)
- [Odoo 17 — Discount and loyalty programs](https://www.odoo.com/documentation/17.0/applications/sales/sales/products_prices/loyalty_discount.html)
- [Cybrosys — Odoo 17, remises, fidélité et cartes cadeaux](https://www.cybrosys.com/odoo/odoo-books/v17/sales/discount-loyalty-and-gift-card/)
- [Broadleaf — Promotion stacking done right](https://broadleafcommerce.com/blog/promotion-stacking-done-right-what-most-platforms-get-wrong/)
- [Voucherify — Promotion suppression](https://www.voucherify.io/blog/promotion-suppression-why-preventing-discounts-matters)
- [promotions-discount-engine — classes de cumul, allocation au centime, trace d'explication](https://github.com/riya0920/promotions-discount-engine)
- [Politique pharmaceutique nationale de Côte d'Ivoire (décret 94-667, décret 2016-714)](https://dap.ci/wp-content/uploads/2021/06/POLITIQUE-PHARMACEUTIQUE-NATIONAL.pdf)
- [Loi n°2015-533 relative à l'exercice de la pharmacie (Côte d'Ivoire)](https://www.droitci.info/files/812.03.14-Loi-n--2015-533-du-20-juillet-2015-relative-a-l-exercice-de-la-pharmacie.pdf)
- [Autorité de la concurrence — recommandations sur la distribution du médicament (2013)](https://www.autoritedelaconcurrence.fr/sites/default/files/MED_fiche1_reco_19dec13.pdf)
- [CJUE — interdiction des offres promotionnelles sur les médicaments](http://curia.europa.eu/juris/document/document.jsf?docid=231845&doclang=fr)
- [DGCCRF — affichage des prix des médicaments en pharmacie](https://www.economie.gouv.fr/dgccrf/les-fiches-pratiques/affichage-des-prix-des-medicaments-en-pharmacie-les-regles-connaitre)
- [INC — le prix des médicaments](https://www.inc-conso.fr/content/le-prix-des-medicaments)
