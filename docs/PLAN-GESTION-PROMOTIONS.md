# Plan — Gestion des promotions

> État : **proposition**. Rien n'est implémenté, rien n'est tranché. Les chiffres de ce document
> ont été mesurés le 2026-11-17 sur la base de recette `pharma_smart_e2e` (1 127 produits,
> 21 219 ventes) et sur le code à la même date. Relu et corrigé le 2026-11-26 (cycle de vie,
> prix fixe par produit, plancher de marge, packs).

## Décisions

Tranchées le 2026-11-27 et rassemblées au §8 : le pharmacien décide de ce qu'il promeut (prix
administré et vente à perte **signalés, non refusés**) ; les promotions s'appliquent au carnet et aux
lignes non remboursées d'une vente assurance. Les invariants 3 et 4 du §5.3 sont devenus des
signalements.

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
| `RemiseClient` | Un taux unique sur un montant remisable — **retirée** le 2026-11-27 (V2.1.66) |
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
- pas de **seuil de quantité**, pas d'**offre N+M** (« 2 + 1 »), pas de **pack** (plusieurs
  articles vendus ensemble à un prix global) ;
- pas de **priorité** ni de **règle de cumul** ;
- pas d'**application automatique** : il faut la choisir ;
- pas de **restitution** — aucun rapport ne dit ce qu'une opération a coûté ni fait vendre ;
- pas d'**historique du prix de vente** : seul `FournisseurProduitPriceHistory` existe, côté achat.
  Ce dernier point décide du §5.1.

### 2.3 Trois défauts mesurés du mécanisme actuel

**a. La remise réappliquée n'était pas idempotente.** *(Corrigé en phase 0, 2026-11-26.)* La
première lecture de ce plan affirmait que la remise n'était appliquée qu'une fois, sur la foi de
quatre appels à `processProductDiscount` commentés dans `SalesLineServiceImpl`. C'était faux : ces
appels étaient du code mort. Chaque mutation du panier passe par
`SalesManagerImpl#finalizeSaleUpdate`, qui recalcule la vente et réapplique la grille à **toutes**
les lignes (`SaleAmountCalculator#proccessDiscount`). Le vrai défaut était ailleurs :
`processProductDiscount` posait une remise quand une grille couvrait la ligne, mais n'**effaçait**
rien quand aucune ne la couvrait, et levait une exception sur une vente sans remise ; et
`removeRemise` remettait le montant à zéro en laissant le **taux** sur la ligne — taux que la FNE
déclare. Le point d'accroche du moteur de promotions est donc déjà là : `finalizeSaleUpdate`.

**b. Le chemin d'import ne porte pas de taux.** *(Corrigé en phase 0.)* `buildSaleLineFromDTO` pose `discount_amount`
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
c'est une question à trancher avec votre conseil, pas dans un plan technique. (À titre de
comparaison seulement : la CJUE admet qu'un État interdise les promotions sur les médicaments, à
condition d'épargner la parapharmacie.)

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

### 4.2 Odoo — programmes de promotion séparés des listes de prix

Odoo distingue deux objets : la **liste de prix** (un prix pour un segment, durable) et le
**programme de promotion / fidélité** (daté, conditionnel). Les types natifs sont « Promotion »,
« Code de réduction » et « Buy X Get Y », ce dernier implémenté comme un **crédit** : X articles
achetés créditent un droit, échangeable contre Y. La remise « sur le moins cher » n'est **pas**
native et passe par des modules tiers.

La leçon utile est la séparation : le prix du catalogue et la mécanique promotionnelle ne sont pas
le même objet et ne vivent pas dans la même table.

### 4.3 Les moteurs du commerce de détail — priorité, classes de cumul, plancher de marge

La littérature des moteurs de promotion converge sur un petit nombre de règles :

- chaque promotion déclare **explicitement** avec quoi elle se cumule ; l'ordre d'application vient
  d'une **priorité**, jamais de la date de création ;
- une promotion **exclusive** s'applique seule ;
- piège documenté : regrouper toutes les promotions sans classe déclarée dans un même seau les rend
  mutuellement exclusives **par accident** ;
- pour une offre N+M, **l'unité offerte est la moins chère** du groupe, et une offre incomplète ne
  remise rien ;
- une remise globale répartie sur plusieurs lignes s'**alloue** ligne par ligne, et le reste
  d'arrondi va sur une seule ligne — jamais un arrondi par ligne ;
- le vrai risque est le **cumul** : l'empilement d'une offre N+M et d'un pourcentage dépasse vite 60 % de
  remise effective. D'où le **plancher de marge** comme garde-fou, et une **trace d'explication**
  par ligne.

### 4.4 Ce que j'en retiens pour Pharma-Smart

1. **Peu de types, bien définis.** Pourcentage, montant, prix fixe. Les offres N+M et les packs
   viennent après.
2. **Un humain valide, les dates pilotent.** Le statut porte la décision humaine ; « à venir »,
   « en cours » et « terminée » se lisent sur les dates (§5.2).
3. **Le cumul se déclare, il ne se devine pas.** Par défaut : pas de cumul.
4. **Un plancher de marge**, contrôlé à la création sur le prix d'achat du fournisseur principal.
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

  > **TVA sur le net — corrigé le 2026-11-27.** La TVA se calculait sur le brut, remise comprise :
  > HT et TVA étaient surévalués sur toute ligne remisée (remise produit ou promotion). Elle porte
  > désormais sur le net de ligne (`SaleAmountCalculator.computeTva`, `tva_embeded`, ticket des non
  > remboursés), et les états (rapport TVA, balance, tableau du pharmacien, rapport d'activité,
  > synthèses, historique produit) calculent leur HT sur le net et rendent `montantTva`
  > (migration `V2.1.67__tva_sur_le_net.sql`). Pas de reprise : les montants enregistrés des ventes
  > déjà clôturées gardent l'ancien calcul ; les états, recalculés des lignes, sont justes partout.
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
                  (phase 2 : PROMO_OFFRE_NM | PROMO_PACK)
  valeur          int nullable (taux en %, ou montant en F ; prix du pack en F pour PROMO_PACK ;
                  nul pour PROMO_PRIX_FIXE, dont le prix est par produit)
  date_debut, date_fin     date, bornes incluses
  statut          BROUILLON | VALIDEE | SUSPENDUE | ANNULEE
  priorite        int, défaut 0
  cumulable       bool, défaut false
  quantite_min    int, défaut 1
  quantite_max_par_vente   int nullable   -- « maximum N par ticket »
  quantite_max_totale      int nullable   -- « dans la limite de N unités »
  created_at, created_by, updated_at

promotion_magasin   (promotion_id, magasin_id)                 -- PK composite
promotion_produit   (promotion_id, produit_id, prix_promo)     -- PK (promotion_id, produit_id) ;
                                                               -- prix_promo renseigné pour
                                                               -- PROMO_PRIX_FIXE seulement

sales_line.promotion_id   FK nullable
```

**Le statut porte la décision, les dates portent l'état.** Le statut ne change que par un geste
humain : `BROUILLON` (en préparation, jamais appliquée), `VALIDEE` (appliquée entre ses dates),
`SUSPENDUE` (validée mais arrêtée à la main, réactivable), `ANNULEE` (définitive, non
réactivable). « À venir », « en cours » et « terminée » ne sont **pas stockés** : ils se calculent
sur `date_debut` / `date_fin` comparées à la date de la vente. Conséquence directe : **aucun travail
planifié** n'est nécessaire pour ouvrir ou fermer une promotion. Sur une installation de bureau
(Tauri), le serveur peut être éteint à minuit ; un travail planifié manqué laisserait une promotion
appliquée après son échéance. Le moteur, lui, lit la date à chaque vente.

Pourquoi cette forme, et pas une autre :

- **Le prix fixe est porté par produit.** Une `valeur` unique pour « prix fixe » n'a pas de sens
  dès que le périmètre contient deux articles de prix différents : `prix_promo` vit donc sur
  `promotion_produit`.

- **`promotion_magasin` en table de liaison, pas une colonne.** Une opération de groupement porte
  sur plusieurs magasins ; une opération de la parapharmacie sur un seul. Et le modèle suit celui
  des favoris du comptoir, qui a déjà fait ses preuves : `produit_favori` est unique sur
  `(magasin_id, produit_id)` et son service résout le magasin connecté à chaque lecture **et** à
  chaque écriture. On ne réinvente pas, on imite ce qui marche.
- **Pas de magasin implicite.** Une promotion sans ligne de magasin ne s'applique **nulle part**.
  L'alternative — « aucune ligne = tous les magasins » — fait qu'un oubli devient une promotion
  de tout le réseau.
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
   **strictement supérieur au prix d'achat** du produit chez son **fournisseur principal**
   (`FournisseurProduit.prixAchat` de `fournisseurProduitPrincipal` — c'est le principal qui porte
   les prix, pas l'agence). Contrôlé à la création et à chaque modification. Le `cost_amount` de la
   ligne de vente n'existe pas encore à ce moment-là ; il sert au contrôle a posteriori (§6,
   phase 3), pas à la garde.
5. Une ligne de vente porte **au plus une** promotion.
6. Une promotion `ANNULEE` ne redevient jamais `VALIDEE` — on en crée une nouvelle. C'est la règle
   de Winpharma, et elle évite la résurrection d'une opération dont plus personne ne se souvient.
   Une promotion dont la date de fin est passée ne s'applique plus, quel que soit son statut ; la
   prolonger, c'est en créer une nouvelle.
7. Une promotion ne s'applique **jamais** à une ligne remisée à la main, sauf `cumulable`.
8. Les plafonds (`quantite_max_par_vente`, `quantite_max_totale`) bornent les unités remisées ;
   au-delà, les unités sont vendues au prix de référence sur la même ligne, et la remise de la ligne
   ne porte que sur les unités dans le plafond.

### 5.4 Le moteur

`MoteurPromotionService.appliquerLesPromotions(Sales)` — **une fonction du panier**, pas un geste.

```
appliquerLesPromotions(vente) :
    1. effacer ce que le moteur a posé   (lignes dont promotion_id est renseigné)
    2. lire les promotions candidates    (magasin figé de la vente, statut VALIDEE,
                                          date de la vente entre date_debut et date_fin)
    3. pour chaque ligne :
         parmi celles dont le périmètre contient le produit et dont quantite_min est atteinte,
         retenir la plus avantageuse pour le client,
         en départageant les ex æquo par priorite DESC, puis id ASC
    4. poser taux_remise, discount_amount, discount_unit_price, promotion_id
    5. recalculer les montants de la vente (processDiscountCommonAmounts), puis l'arrondi caisse
```

**Avantage d'abord, priorité ensuite.** La priorité ne sert donc qu'à départager deux promotions
de même effet : c'est assumé. Le client n'a jamais intérêt à ce qu'une promotion moins favorable
l'emporte, et le pharmacien qui veut forcer une opération la rend exclusive plutôt que de jouer sur
la priorité.

Cinq points non négociables :

- **Appelé à chaque mutation du panier** : ajout, changement de quantité, changement de prix,
  retrait. Le point d'appel existe : `SalesManagerImpl#finalizeSaleUpdate` (et
  `deleteSaleLineById` pour le retrait), qui réappliquent déjà la grille de remise à toutes les
  lignes. Le moteur s'y branche à côté de `proccessDiscount`, et il est **idempotent** : il efface
  avant de recalculer, comme `processProductDiscount` le fait depuis la phase 0.
- **Idempotent et borné à ce qu'il a posé.** Une remise humaine (`promotion_id` nul,
  `discount_amount > 0`) n'est jamais écrasée.
- **Le magasin lu est celui de la vente**, pas celui de l'utilisateur connecté. `Sales.magasin` est
  estampillé depuis le caissier (`SaleCommonService:248`), et la délégation
  `openCashRegister(admin, caissier)` a déjà prouvé que les deux diffèrent.
- **La date lue est celle de la vente**, pas `LocalDate.now()` : une vente en attente reprise le
  lendemain, ou une vente différée, ne doit pas changer de prix entre-temps.
- **Déterministe** : priorité puis identifiant. Jamais la date de création, jamais l'ordre d'un
  `Set` — un `findFirst()` sur un ensemble rend un résultat au hasard.

### 5.5 La portée magasin

Trois affirmations, chacune vérifiable :

- **L'écran de création préselectionne le magasin de la session** et ne propose que les magasins de
  la portée de l'utilisateur (`PorteeMagasinService.resoudreLaPorteeDeSupervision`). Le sélecteur
  est **masqué s'il n'y en a qu'un** — la convention déjà posée pour les favoris, la gestion de
  caisse et le réassort.
- **Le responsable de l'unité de gestion crée les promotions de son unité**, s'il détient le droit.
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
- **En rayon** : une promotion que la cliente ne voit pas ne fait rien vendre. Le module
  d'étiquettes (`service/etiquette`) doit pouvoir imprimer une étiquette « PROMO » portant le prix
  de référence barré, le prix remisé et la date de fin — et, pour un pack, l'étiquette à code-barres
  à coller sur l'emballage (§5.8).

### 5.7 Retour et annulation d'une ligne promue

On rembourse **ce que la cliente a payé**, jamais le prix de référence : le montant remboursé d'une
ligne promue est son prix remisé (`discount_unit_price`), et l'avoir (`RetourClientService`,
`AvoirClientService`) le reprend tel quel. Une annulation de vente annule la remise avec elle ; le
compte des unités imputées sur `quantite_max_totale` est diminué d'autant.

Retourner une partie d'une ligne N+M ou un seul article d'un pack ne doit pas faire gagner la
cliente : le remboursement suit la remise **répartie** sur la ligne retournée (§5.8), pas son prix
catalogue. Sinon acheter un pack puis en rendre un article devient une façon de payer moins.

> **Tranché et livré le 2026-11-27** (`RetourClientServiceImpl.controlerLesPromotions`) : pas de
> répartition, des règles simples. Une ligne promue se retourne **en entier ou pas du tout**, au montant
> payé (montant de la ligne moins sa remise) ; un **pack**, avec tous ses articles, ou pas du tout — et
> pas s'il contient un article qui ne se reprend pas (statut légal). Une ligne promue d'une **vente à
> crédit** (carnet, lignes non remboursées d'une assurance) ou **cumulée avec la remise produit** ne se
> retourne pas : on annule la vente et on la ressaisit. La fenêtre de retour l'affiche (case « Toute la
> ligne » / « Tout le pack », motif du refus) et le serveur le contrôle. Au passage : le montant affiché
> par la fenêtre multipliait le remboursable de toute la ligne par la quantité retournée.

### 5.8 Les packs : plusieurs articles vendus ensemble

En officine, « pack » recouvre trois réalités qu'il ne faut pas confondre :

| Cas | Exemple | Traitement |
|---|---|---|
| **Coffret du fabricant** | Coffret solaire avec son propre EAN | Aucun : c'est un produit comme un autre |
| **Pack virtuel** | Shampooing + après-shampooing = 8 000 F au lieu de 9 500 F | Une mécanique de promotion, `PROMO_PACK` |
| **Pack monté par l'officine** | Trois articles emballés sous film, avec une étiquette | Un **code-barres qui déclenche un pack virtuel** |

**Le pack monté par l'officine n'est pas un article stocké.** En faire un produit assemblé
(composants sortis du stock, pack entré) casserait :

- les **lots et la FEFO** — quelle péremption pour le pack ? celle du composant le plus court, qu'il
  faudrait suivre à part ;
- l'**inventaire**, qui compterait des composants absents des rayons ;
- la **TVA**, dès que le pack mêle un article à 0 % et un article à 18 % : une seule ligne ne peut
  porter qu'un taux ;
- la **FNE** et la **déclaration de CA**, qui veulent une ligne par produit.

Il ne faut pas non plus le confondre avec le déconditionnement (`Produit.parent` /
`Produit.produits`, une boîte et son détail) : un pack réunit des articles **différents**.

**Conception** : l'emballage physique n'est qu'un raccourci de saisie.

```
promotion_pack_composant (promotion_id, produit_id, quantite)   -- PK (promotion_id, produit_id)
promotion.code_barre_pack   varchar nullable, unique             -- imprimé sur l'étiquette du pack
```

- **Au scan** du code de pack, le comptoir ajoute les N lignes des composants (une par produit, à
  la quantité du pack), puis le moteur applique le prix du pack. Sans scan, le moteur reconnaît
  aussi bien le pack si les composants sont saisis un à un.
- **Stock, lots, TVA, FNE, CA** : rien ne change, chaque article garde sa ligne et sort de son
  propre stock en FEFO.
- **Pack incomplet** : il ne remise rien. **Deux packs complets** : le prix du pack s'applique deux
  fois ; les unités en surplus restent au prix de référence.
- **Répartition de la remise** — c'est le point délicat. La remise du pack (somme des prix de
  référence des composants − prix du pack) est **allouée au prorata** du montant de chaque ligne, en
  francs entiers, par la méthode du plus fort reste : chaque ligne reçoit la partie entière de sa
  quote-part, et les francs restants vont aux lignes de plus forte partie décimale. **Pas de
  `Math.ceil` ligne par ligne**, qui remiserait plus que le pack. Chaque ligne reçoit ensuite son
  `taux_remise` équivalent (§7.3) et le même `promotion_id`.
- **Invariants propres** : chaque composant est à prix libre (invariant 3), le plancher de marge
  porte sur le **pack entier** (prix du pack > somme des prix d'achat des composants), et un
  composant ne peut appartenir qu'à un pack à la fois dans une même vente.
- **Le pack et l'offre N+M sont deux mécaniques sœurs** : « acheter cet ensemble » contre « acheter
  N fois le même article ». Elles partagent la même allocation de remise et arrivent ensemble, en
  phase 2.

---

## 6. Le plan par phases

### Phase 0 — Les défauts qui gêneraient le moteur — **faite le 2026-11-26**

1. **Remise idempotente.** `processProductDiscount` remet montant **et** taux à zéro quand aucune
   grille ne couvre la ligne, et ne lève plus d'exception sur une vente sans remise ou un article
   sans code. Les quatre appels commentés (code mort, §2.3a) sont supprimés.
   `SaleAmountCalculator#computeRemiseProduit` recalcule les totaux une fois après la boucle, au lieu
   d'une fois par ligne.
2. **`removeRemise` efface aussi le taux** des lignes (`SaleCommonService`). Avant, une remise
   retirée laissait `taux_remise` sur la ligne : la FNE et les procédures de restitution, qui
   multiplient par `sl.taux_remise`, la comptaient encore.
3. **Le taux sur le chemin d'import.** `SaleLineDTO` porte `tauxRemise` (en fraction, comme
   `sales_line.taux_remise`) ; `buildSaleLineFromDTO` le reprend, ou le déduit de
   `discountAmount / salesAmount` quand la source ne le fournit pas (§2.3b).
4. **L'arrondi de caisse après le retrait d'une ligne.** Le retrait (vente au comptant et vente
   dépôt) recalculait le montant à payer sans l'arrondir au multiple de 5, contrairement à l'ajout et
   à la modification. C'est le second arrondi du §2.3c ; il est maintenant appliqué sur les trois
   chemins.

Essais : `SalesLineServiceBaseImplTest` (remise effacée, vente sans remise, réapplication
idempotente, taux importé) `SaleCommonServiceTest` (retrait de la remise), `SaleServiceImplTest` et `SalesManagerImplTest`
(arrondi après retrait).

### Phase 1 — Le socle : une promotion simple, datée, par magasin

> **Avancement au 2026-11-26 — backend livré, écrans à faire.**
> Fait : migration `V2.1.57__promotion.sql` (tables, `sales_line.promotion_id` + `promotion_libelle`
> figé à la vente, paramètre `APP_PROMOTION_TVA_ZERO_AUTORISEE`, `nav_item` « promotions » sans rôle
> accordé) ; entité `Promotion` ; `PromotionService` (invariants : magasin, article, dates, valeur,
> prix libre, plancher sur le prix d'achat du fournisseur principal, transitions de statut) ;
> `MoteurPromotionService` branché dans `SaleCommonService#upddateCashSaleAmounts` et au retrait
> d'une ligne ; API `/api/promotions`. Écart assumé avec le §5.5 : une promotion vise le seul
> **magasin d'exploitation** de son auteur — la doctrine de `PorteeMagasinService` interdit d'écrire
> ailleurs. La table `promotion_magasin` garde la porte ouverte.
> Écrans (même jour) : Référentiel > Promotions (`features/promotion`), liste filtrable avec
> panneau de détail et formulaire en modale ; au comptoir, montant barré, montant remisé et
> libellé sur la ligne ; libellé « Promo … -montant » imprimé sous l'article sur le ticket.
> Étiquette de rayon (même jour) : type `PROMO` du gabarit commun des usages, donc tous les
> formats de planche et de rouleau, en PDF comme en ZPL ; bandeau « PROMO », prix barré et prix
> remisé sur une ligne, date de fin et seuil de quantité. Refusée pour une promotion qui ne
> s'applique pas au comptoir. `GET /api/promotions/{id}/etiquettes[/zpl]`, bouton « Étiquettes »
> dans l'écran. Pas de code-article : il ne tenait pas sur un rouleau de 30 × 20 mm.
> Plafond total (2026-11-27) : `V2.1.60` ajoute `promotion.quantite_max_totale` et
> `sales_line.quantite_promo` (unités réellement remisées, reprises pour les lignes déjà promues).
> Le compteur somme `quantite_promo` des ventes **clôturées et non annulées** : une annulation rend
> ses unités, un panier en cours ne réserve rien. Le moteur répartit ce qui reste dans l'ordre de
> saisie des lignes ; le plafond par vente vaut désormais pour le **ticket** (deux articles d'une
> même promotion le partagent), plus pour chaque ligne. Atteint, la promotion passe à l'état
> `EPUISEE` : le comptoir revient au prix normal, l'étiquette est refusée, l'écran affiche
> « k / N unités remisées ». Limite assumée : deux caisses qui remplissent leur panier en même
> temps sur les dernières unités peuvent dépasser le plafond de ce que contient l'autre panier.
> Badge « en promotion » (même jour) : `GET /api/promotions/en-vigueur`, ouvert à tous les rôles
> (`@NavAccessExempt`, le caissier n'a pas le menu), rend un article par produit, celui de la
> promotion la plus avantageuse et sans les épuisées, comme le moteur. Pastille « PROMO » avec son
> infobulle dans la liste et le panneau du catalogue ; sur la tuile des favoris, prix barré et prix
> remisé, sauf pour une promotion « dès N » (la tuile n'ajoute qu'une unité). Le badge informe, le
> moteur du serveur reste seul à poser la remise.
> Essais : unitaires `CalculPromotionTest`, `EtatPromotionTest`, `MoteurPromotionServiceImplTest`,
> `PromotionServiceImplTest`, `EtiquettesPromotionTest`, et côté front le badge, son infobulle et la
> tuile des favoris ; intégration `PromotionIntegrationTest` (Testcontainers) du formulaire au
> comptoir : remise posée et écrite, suspension, plafond par ticket, plafond total, épuisement,
> annulation qui rend les unités, contrainte de V2.1.60, JPQL des promotions en vigueur et du badge.
> Le moteur réel est désormais branché dans `SaleServicesUnderTest`, comme en production.
> Parcours de recette : RFD-22 (créer et valider), RFD-23 (appliquée sans saisie), RFD-24 (arrêtée
> d'elle-même à l'échéance — dates reculées en base, ce que voit l'application le lendemain) et
> RFD-25 (étiquettes de rayon), dans `e2e/parcours/produits/` et le cahier de recette. Jeu d'essai
> `scripts/essai-promotion/` (pose et nettoyage) ; chaque parcours supprime sa promotion
> « ESSAI E2E » et vérifie qu'il n'en reste aucune (§7 point 15). Les boutons-icônes de la liste
> portent désormais un libellé accessible.
> Les quatre passent sur la base de recette (conteneur `pharma_smart_postgres`, 27/11/2026).

- Migration : `promotion`, `promotion_magasin`, `promotion_produit`, `sales_line.promotion_id`.
- Domaine : `Promotion`, `PromotionMagasin`, `PromotionProduit` ; DTO **dans leurs propres
  fichiers** ; `PromotionRepository` en JPQL ; `PromotionService` + `PromotionServiceImpl`.
- Moteur : `MoteurPromotionService` + impl, branché aux quatre points de mutation.
- Garde réglementaire : contrôle du prix libre et du plancher de marge **à la création**.
- `nav_item` « promotions » + droit, accordé à `ROLE_ADMIN` et `ROLE_PHARMACIEN` (V2.1.58, V2.1.59) — c'est à l'officine de décider
  qui en dispose, comme pour `supervision.magasins`.
- Écran : liste, création, périmètre produit (recherche au catalogue), magasins, dates, suspension.
  Design System (`app-button`, `app-data-table`, `app-select`), devise via `APP_DEVISE`.
- Comptoir : prix barré, libellé, et recalcul à chaque changement du panier.
- **Pas de travail planifié** : l'état « en cours / terminée » se lit sur les dates (§5.2).
- Plafonds par ticket et totaux (invariant 8) ; étiquette de rayon « PROMO » (§5.6) ; retour au
  prix remisé (§5.7).
- **Portée** : ventes au comptant ; depuis le 2026-11-27, carnet et lignes non remboursées d'une vente assurance (§8).
- Essais : les invariants du §5.3, un essai par règle, et deux parcours de recette — « une
  promotion s'applique sans saisie » et « elle s'arrête d'elle-même à l'échéance ».

### Phase 2 — Les mécaniques que le commerce réclame vraiment

> **Avancement au 2026-11-27 — phase 2 livrée : offre N+M, pack, périmètre par rayon, seuil en montant, cumul avec la remise du caissier.**
> Offre N+M : `V2.1.61` ajoute le type `PROMO_OFFRE_NM` (contrainte : au moins une unité offerte),
> sans colonne nouvelle — `quantite_min` porte N, `valeur` M. `OffreNM` met en commun les unités de
> tous les articles de la promotion, les range du plus cher au moins cher, groupe par N+M et offre
> les M dernières de chaque groupe ; les unités en surplus restent au prix. `RepartitionRemise`
> répartit la remise du groupe au plus fort reste sur ses lignes, au prorata de leur montant — elle
> servira au pack. Le moteur passe en deux temps : les offres d'abord, dans l'ordre de départage,
> qui ne prennent leurs lignes que si elles remisent davantage que les promotions de ligne sur ces
> mêmes lignes ; puis les promotions de ligne sur le reste. Plafonds par ticket et total en groupes
> complets. Plancher de marge sur le prix moyen d'une unité d'un groupe du même article.
> `quantity_ug` n'est pas touché (§7.5). Étiquette : prix normal, et « 2 achetés + 1 offert » en
> mention, sans prix barré ; badge et infobulle disent l'offre en toutes lettres. Formulaire :
> « Unités achetées » / « Unités offertes ». Essais : `OffreNMTest`, `RepartitionRemiseTest`, moteur,
> service, étiquette, intégration (groupe sur deux articles, contrainte de V2.1.61), front ;
> parcours RFD-26 (`scripts/essai-promotion/preparer-offre-nm.sql`).
> Pack (même jour) : `V2.1.62` ajoute le type `PROMO_PACK` (`valeur` = prix du pack),
> `promotion.code_barre_pack` (unique, généré « PK » + 8 chiffres, lisible en Code 39) et
> `promotion_produit.quantite`. **Écart avec le §5.8** : pas de table `promotion_pack_composant`, les
> composants sont les articles de la promotion avec leur quantité — une seule liste d'articles, quelle
> que soit la mécanique. `Pack` compte les packs complets du panier (un incomplet ne remise rien, deux
> complets deux fois, le surplus au prix), et la remise (prix de référence des unités prises − k × prix
> du pack) se répartit au plus fort reste, comme l'offre (`RemiseRepartie`, partagée). Contrôles : au
> moins deux articles différents, prix du pack sous la somme des prix, plancher de marge sur le pack
> entier. Étiquette : celle de l'**emballage**, rendue en réglette (libellé, prix, code-barres), PDF et
> ZPL. Comptoir : `GET /api/promotions/pack?code=` (ouvert à tous les rôles) ; un code scanné qui ne
> désigne aucun article — dans le champ produit comme au scanner global — est cherché comme pack, et
> `PackComptoirService` ajoute ses articles **un par un**, chacun attendant la confirmation du
> précédent (envoyés ensemble, ils créeraient plusieurs ventes). Au comptant seulement. Saisis à la
> main, les articles forment le pack de la même façon. Formulaire : quantité par article, prix du
> pack, économie et alerte de vente à perte. Essais : `PackTest`, moteur, service, étiquette,
> intégration (pack sur deux lignes, lecture par le code), `PackComptoirService` ; parcours RFD-27
> (scan de l'étiquette, `scripts/essai-promotion/preparer-pack.sql`).
> Périmètre par rayon ou famille (même jour) : **un moyen de remplir la liste, pas un périmètre
> dynamique**. Le formulaire propose les rayons du magasin et les familles (avec leur nombre
> d'articles actifs) ; « Ajouter ses articles » verse leurs articles dans la liste, sans doublon, et
> écarte d'office ceux à prix administré, cités dans le bilan. C'est la liste qui s'enregistre : chaque
> article passe les contrôles du §5.3, et un article rangé plus tard dans le rayon n'entre pas dans une
> promotion validée sans avoir été contrôlé — un périmètre résolu à la vente contredirait le principe
> « contrôlé à la création, pas au comptoir » (§3). Aucune migration. « Les retirer » enlève d'un clic
> les articles qui seraient vendus à perte. `GET /api/promotions/perimetre/choix` et
> `/perimetre?rayonId=|familleId=` (droit `promotions`). Essais : intégration (rayon du magasin,
> famille, articles inactifs et administrés écartés, rayon d'un autre magasin) ; parcours RFD-28.
> Seuil en montant (même jour) : `V2.1.63` ajoute le type `PROMO_SEUIL_MONTANT` (`valeur` = taux,
> `promotion.seuil_montant` = montant en F ; contrainte : seuil ≥ 1, taux 1–99 %, pas de plafond en
> unités). **Écart avec la ligne ci-dessous** : pas d'objet de remise au niveau de la vente. Le seuil se
> lit sur le prix de référence des articles de la promotion présents dans le panier (jamais tout le
> ticket : un prix administré n'aide pas à l'atteindre) ; atteint, le taux vaut pour tout ce montant, et
> la remise — arrondie au supérieur une seule fois — se répartit au plus fort reste sur les lignes
> (`SeuilMontant`), comme le §4.3 le demande. Chaque ligne garde sa base de TVA, son `promotion_id` pour
> le rapport, et un article rendu emporte sa part. Dans le moteur, c'est une promotion de groupe comme
> l'offre et le pack : elle ne prend les lignes que si elle fait gagner plus que leurs promotions de
> ligne. Plancher de marge : article par article, au taux plein (le pire cas). Étiquette et badge : prix
> normal et « -10 % dès 20 000 CFA d'achat » — un prix barré mentirait à qui n'atteint pas le seuil.
> Essais : `SeuilMontantTest`, moteur (seuil sur les seuls articles de la promotion, sous le seuil,
> promotion de ligne plus forte), service, étiquette, intégration (seuil franchi au second article,
> contrainte) ; parcours RFD-29 (`scripts/essai-promotion/preparer-seuil-montant.sql`).
> Cumul (même jour) — **décision : cumul avec la remise du caissier seulement, pas entre promotions**
> (invariant 5 maintenu : une promotion par ligne). `V2.1.64` ajoute `promotion.cumulable` (faux par
> défaut ; contrainte : mécaniques de ligne seulement — pourcentage, montant, prix fixe). La remise du
> caissier, c'est la remise produit (grille) de la vente : elle passe d'abord, la promotion cumulable
> s'applique ensuite sur le prix unitaire déjà remisé (`CalculPromotion.remiseCumulee`), et la ligne
> porte la somme des deux (taux FNE recalculé). **Plancher sur la remise cumulée, au comptoir** : la part
> de la promotion est bornée pour que la ligne reste strictement au-dessus du prix d'achat de ses unités ;
> c'est la promotion qui cède, jamais la remise du caissier. Une ligne remisée par le caissier n'entre
> toujours dans aucune offre, aucun pack, aucune remise de panier. (La remise client, qui excluait le
> moteur, a depuis été retirée.)
> Corrigé en chemin, au profit de toutes les mécaniques : (1) le **retrait d'une ligne** relance le
> moteur — un pack amputé ou un panier repassé sous son seuil gardaient leur remise, et avec une
> remise produit les promotions des autres lignes disparaissaient ; (2) **choisir ou retirer la remise
> produit** passe par le recalcul complet — les promotions s'effaçaient jusqu'à la mutation suivante.
> Constat : le front gardait un chemin de « remise manuelle par ligne » (`applyLineDiscount`) qu'aucun
> écran ne déclenchait et que le serveur ignorait. L'officine n'en veut pas — la remise se choisit dans
> la liste des remises créées : ce code mort a été retiré le 2026-11-27. Essais : calcul cumulé et plancher, moteur, service, intégration (remise produit
> choisie puis retirée, promotion non cumulable, contrainte, retrait d'un article de pack) ; suite
> backend complète verte ; parcours RFD-30 (`scripts/essai-promotion/preparer-cumul.sql`).

- **Offre N+M (N achetés, M offerts)**, l'unité offerte étant la moins chère du groupe. Attention :
  **ne pas** réutiliser `SalesLine.quantity_ug`, qui consomme les unités gratuites du fournisseur
  (`stock_produit.qty_ug`) — deux notions, un seul mot (§7.5).
- **Pack** (§5.8) : `promotion_pack_composant`, code-barres de pack et étiquette, allocation de la
  remise au plus fort reste. Partage l'allocation avec l'offre N+M : les deux se livrent ensemble.
- ~~**Périmètre par rayon ou par famille**, en plus de la liste explicite.~~ Livré comme moyen de remplir la liste (voir l'avancement).
- ~~**Seuil en montant** (« −10 % au-delà de 20 000 F »), qui est une promotion de panier et non de
  ligne : elle a besoin d'un objet de remise au niveau vente, pas ligne.~~ Livré, réparti sur les
  lignes plutôt qu'en objet de vente (voir l'avancement).
- ~~**Cumul** : `cumulable` + classes d'exclusion, et un plancher de marge sur la remise **cumulée**.~~
  Livré pour le cumul avec la remise du caissier, sans classes d'exclusion (inutiles tant qu'une ligne
  ne porte qu'une promotion). **Pas de cumul entre promotions** — tranché le 2026-11-27 : une ligne
  porte une seule promotion, la plus avantageuse pour la cliente ; la priorité ne fait que départager
  (invariant 5, §5.4).

### Phase 3 — La restitution — **livrée le 2026-11-27**

> Le détail d'une promotion porte son **bilan** dès son premier jour (`BilanPromotionService`,
> `GET /api/promotions/{id}/bilan`) : le coût de l'opération (somme des `discount_amount` des lignes
> qu'elle a posées — remise du caissier comprise sur une ligne cumulée), les unités remisées, les
> tickets et les clients identifiés distincts ; puis, pour ses articles **toutes ventes confondues**,
> unités, chiffre d'affaires net et marge, comparés à la **période précédente de même durée** (celle
> qui finit la veille de l'opération), au total et article par article. Ventes clôturées, non annulées,
> du chiffre d'affaires (une vente dépôt n'entre pas), dans le magasin de l'utilisateur. Montants TTC ;
> la marge déduit le coût d'achat enregistré sur la ligne à la vente. L'export CSV
> (`/bilan/csv`, droit `EXPORT` du menu « promotions ») donne une ligne par article.
> Limite : la comparaison avec la période précédente ne corrige ni la saison ni les ruptures ; c'est
> une lecture, pas une mesure causale. Essais : intégration (vente clôturée comptée, panier en cours
> ignoré, période de comparaison, export) ; parcours RFD-31.

- Ce que l'opération a coûté (somme des `discount_amount` par `promotion_id`), ce qu'elle a fait
  vendre (quantités, tickets, clients distincts), et l'effet marge par rapport à la période
  précédente.
- Export serveur via `CsvExportService` et le droit `EXPORT` existants.

### Hors périmètre — l'import d'un groupement

Le format Winpharma (§4.1) est celui des groupements français. Tant qu'aucun groupement ivoirien
ne diffuse de fichier de promotions, cet import n'est pas planifié. S'il le devient, la règle est
connue : intégration en `BROUILLON`, validation humaine, et une promotion annulée non réactivable.

---

## 7. Pièges identifiés

1. **Une remise qui ne s'efface pas** (§2.3a, corrigé en phase 0) : tout ce que le moteur pose, il
   doit pouvoir l'effacer au recalcul suivant, montant **et** taux. Un taux orphelin est invisible à
   l'écran, mais la FNE le déclare.
2. **Deux arrondis successifs** (§2.3c) : `ceil` sur la remise, puis multiple de 5 sur l'espèce. Ne
   pas inverser, ne pas en ajouter un troisième. Et une remise **répartie** sur plusieurs lignes
   (pack, offre N+M) ne passe pas par `ceil` ligne à ligne : elle s'alloue au plus fort reste
   (§5.8), sinon la somme des remises dépasse la remise voulue.
3. **La FNE veut un taux** : une promotion en montant, en prix fixe ou en pack doit **calculer** le
   taux équivalent de chaque ligne, sinon la facture normalisée déclare une remise nulle.
4. **Le mot « lot » est déjà pris.** `Lot` et `LotStockLocation` sont les numéros de lot (FEFO).
   La mécanique « 2 + 1 » s'appelle **offre N+M** dans le code comme dans les écrans.
5. **`quantity_ug` est déjà pris.** Il porte les unités gratuites **du fournisseur**, décrémentées
   de `stock_produit.qty_ug` par `StockUpdateService`. Un « 2 + 1 » maison qui l'emploierait
   consommerait le stock gratuit de l'officine pour financer sa propre promotion — invisible au
   bilan, et faux.
6. **`Produit.codeRemise` et `GrilleRemise` ne sont pas un début de promotion.** Les codes 12 à 21,
   typés VO/VNO, sont une convention de grille ; les étendre mélangerait deux sujets. De même,
   `RemiseClient` n'est pas à étendre : elle a été retirée (V2.1.66).
7. **Le prix promotionnel n'est jamais écrit dans `produit.regular_unit_price`** : sans historique
   du prix de vente, le retour en arrière n'est pas prouvable (§5.1).
8. **Le magasin de la vente n'est pas celui de l'utilisateur** (§5.4). La délégation d'ouverture de
   caisse l'a déjà démontré.
9. **Le tiers payant calcule sa part sur le prix.** Remiser une ligne couverte par une assurance
   déplace la charge entre l'assureur et le patient. C'est une question de gestion (§8), pas un
   détail d'implémentation — et c'est pourquoi la phase 1 s'en tient au comptant.
10. **Le stock négatif est une règle de gestion.** Une vente forcée sort la quantité demandée ; une
    promotion ne doit ni la bloquer ni la recalculer.
11. **Une réponse en retard ne doit pas écraser un panier plus récent.** Le comptoir recalcule à
    chaque mutation du panier : chaque réponse doit être rattachée à l'état du panier qui l'a
    demandée, et ignorée si le panier a changé depuis.
12. **Cumuler par défaut** est la première cause documentée de dérive de marge : `cumulable` vaut
    `false`, et le reste.
13. **Pas de moteur de règles générique.** 162 articles à prix libre, de 0 à 10 magasins : une
    liste d'articles et un sélecteur suffisent. Un langage de règles coûterait dix fois son usage.
14. **Une promotion ne dépend pas du client.** C'est de la fidélité, et elle a son propre plan.
15. **Les parcours de recette partagent le compte de l'utilisateur.** Une promotion créée par un
    parcours et laissée active changerait les prix d'une démonstration — tout parcours de promotion
    doit nettoyer derrière lui, et le vérifier.

---

## 8. Questions tranchées (décisions de l'officine, 2026-11-27)

Livrées le même jour (migration `V2.1.65`).

1. **Médicament à prix administré** — c'est au pharmacien de choisir ce qu'il promeut : le logiciel
   **signale, il ne refuse plus**. Badge « Prix administré » dans la liste des articles du formulaire
   et dans le détail, alerte dans la liste des promotions, confirmation à la validation ; un rayon ou
   une famille apporte aussi ses médicaments, signalés. Le réglage `APP_PROMOTION_TVA_ZERO_AUTORISEE`
   est supprimé.
2. **Ventes à crédit** — une vente **carnet** se remise sur toutes ses lignes ; une vente
   **assurance**, sur ses seules lignes **non remboursées** (`SalesLine.nonRembourse`), que le patient
   paie en entier. Le moteur se relance à chaque mutation, y compris au basculement remboursé / non
   remboursé et à l'ajout ou au retrait d'une remise. Les lignes non remboursées s'impriment déjà sur
   leur propre ticket (`TicketNonRembourseBuilder`) : il porte désormais leur remise, et le ticket
   assurance ne la reprend plus. `sales.remise_non_rembourse` garde cette remise pour que la part
   client de la facture du tiers payant (`getPartAssureRembourse`) ne la compte pas.
   Corrigé en chemin : le calcul tiers-payant ne recevait **aucune** remise de ligne
   (`SaleItemInput.discountAmount` restait à zéro) — une remise ne baissait jamais la part du patient,
   remise produit d'une vente carnet comprise.
   **Carnet = vente au comptant à règlement différé** (décision du même jour) : l'organisme est
   facturé sur le montant **remisé** de chaque ligne (grille ou promotion), le client paie le reste du
   net. Auparavant il l'était sur le prix plein, et à 100 % la remise ne profitait à personne.
3. **Vente à perte** — **signalée, pas refusée** (fin de série, péremption proche), aux mêmes endroits
   que le prix administré. Au comptoir, le cumul avec la remise du caissier ne descend pas sous le prix
   d'achat, ni sous le prix que l'officine a accepté pour la promotion seule si celui-ci est plus bas.
4. **Qui crée une promotion** — le droit est posé pour `ROLE_ADMIN` (V2.1.58) et `ROLE_PHARMACIEN`
   (V2.1.59) ; l'administrateur l'attribue à un autre rôle au besoin.
5. et 6. Tranchées par la phase 2 (offre N+M, pack, rayon et famille livrés).
7. **Pack mêlant un médicament et un article à prix libre** — permis, signalé comme en 1.

---

## Sources

- [Winpharma — API Push Promotions, document client V3](https://www.winpharma.com/wp-content/uploads/Push_Promotions_Clients-V3.pdf)
- [Odoo 17 — Discount and loyalty programs](https://www.odoo.com/documentation/17.0/applications/sales/sales/products_prices/loyalty_discount.html)
- [Cybrosys — Odoo 17, remises, fidélité et cartes cadeaux](https://www.cybrosys.com/odoo/odoo-books/v17/sales/discount-loyalty-and-gift-card/)
- [Broadleaf — Promotion stacking done right](https://broadleafcommerce.com/blog/promotion-stacking-done-right-what-most-platforms-get-wrong/)
- [Voucherify — Promotion suppression](https://www.voucherify.io/blog/promotion-suppression-why-preventing-discounts-matters)
- [promotions-discount-engine — classes de cumul, allocation au centime, trace d'explication](https://github.com/riya0920/promotions-discount-engine)
- [Politique pharmaceutique nationale de Côte d'Ivoire (décret 94-667, décret 2016-714)](https://dap.ci/wp-content/uploads/2021/06/POLITIQUE-PHARMACEUTIQUE-NATIONAL.pdf)
- [Loi n°2015-533 relative à l'exercice de la pharmacie (Côte d'Ivoire)](https://www.droitci.info/files/812.03.14-Loi-n--2015-533-du-20-juillet-2015-relative-a-l-exercice-de-la-pharmacie.pdf)
- [CJUE — interdiction des offres promotionnelles sur les médicaments](http://curia.europa.eu/juris/document/document.jsf?docid=231845&doclang=fr)
- [DGCCRF — affichage des prix des médicaments en pharmacie](https://www.economie.gouv.fr/dgccrf/les-fiches-pratiques/affichage-des-prix-des-medicaments-en-pharmacie-les-regles-connaitre)
- [INC — le prix des médicaments](https://www.inc-conso.fr/content/le-prix-des-medicaments)
