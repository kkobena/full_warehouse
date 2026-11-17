# Plan — Hiérarchie des magasins : rattacher un dépôt à son officine

> **Statut : analyse et plan, rien n'est implémenté.**
> Objet : ajouter une relation récursive sur `Magasin` (un dépôt appartient à une officine, une
> officine peut porter plusieurs sous-magasins), avec des utilisateurs affectés à l'un ou l'autre.

## Décisions prises

| Question | Décision |
|---|---|
| La vente d'un dépôt appartient-elle au dépôt ou à l'officine ? | **Au dépôt.** |
| Comment l'officine approvisionne-t-elle le dépôt ? | **Le dépôt vient acheter à l'officine** — c'est la vente dépôt, qui existe déjà. |
| À quoi sert la hiérarchie au-delà du dépôt ? | À une **gestion autonome par unité**, la parapharmacie par exemple. |
| Le pharmacien doit-il se connecter au dépôt pour en voir les mouvements ? | **Non.** Un privilège lui donne la vue sur tous les magasins qu'il supervise. |
| Une unité de gestion encaisse-t-elle ? | **Oui**, sa propre caisse. |
| Une unité de gestion commande-t-elle ? | **Oui**, ses propres commandes fournisseur. |
| Un dépôt commande-t-il ? | **Non.** L'officine commande pour lui. |
| La marchandise remise au dépôt est-elle un CA ? | **Non — c'est de la consignation.** Le CA naît à la revente, par l'import des ventes du dépôt. |
| Comment l'argent du dépôt rentre-t-il ? | Un **règlement dépôt**, en entrée dans les mouvements de caisse. |
| Le client est-il partagé ? | **Oui, au sein d'un même lieu physique** : le même client achète en parapharmacie et à l'officine. |
| Que suit-on d'un dépôt ? | **Principalement ses mouvements de stock.** |

---

## 1. Pourquoi ce rattachement n'est pas une commodité technique

Le besoin exprimé — « un dépôt appartiendra à un magasin principal » — correspond exactement à ce
que la réglementation ivoirienne impose. Ce n'est pas une généralisation prudente du modèle, c'est
la transcription d'un lien juridique qui existe déjà.

- La **loi n° 2015-533 du 20 juillet 2015** relative à l'exercice de la pharmacie prévoit en son
  article 29 qu'un pharmacien peut être autorisé à ouvrir un **dépôt** dans les localités dépourvues
  d'officine.
- Le dossier d'ouverture d'un dépôt exige une **copie de la licence de l'officine dont dépendra le
  dépôt**. Le rattachement est donc nommé, unique et opposable : le dépôt n'existe
  administrativement qu'adossé à une officine précise.
- L'article 44 impose un **pharmacien responsable** par établissement pharmaceutique.
- Un pharmacien ne peut être propriétaire que d'**une seule officine**, en Côte d'Ivoire comme à
  l'étranger.
- Le ministère de la Santé recense **863 officines et 521 dépôts** dans le secteur privé, chaque
  dépôt dépendant d'une officine et desservant une zone jugée non viable pour une officine.

**Trois conséquences directes sur la conception :**

1. Le rattachement est **un attribut du magasin**, pas une commodité d'affichage. Il doit être
   persisté et contraint.
2. L'arbre des **établissements** est profond d'un niveau : officine → dépôts. La règle « une seule
   officine par pharmacien » interdit l'étage supérieur.
3. Avec 521 dépôts pour 863 officines, ce n'est pas un cas marginal : **c'est une installation sur
   deux**.

---

## 2. Deux axes à ne pas confondre

C'est le point de conception le plus délicat, et il vient de l'élargissement demandé : la
parapharmacie.

Un **dépôt** et un **rayon parapharmacie** ne sont pas la même chose, même si on veut gérer les
deux « à part » :

| | Dépôt | Parapharmacie |
|---|---|---|
| Lieu | autre bâtiment, autre localité | même local |
| Existence juridique | **établissement distinct**, licence propre | aucune : c'est l'officine |
| Identité fiscale (`fnePointOfSale`) | **celle de l'officine** | celle de l'officine |
| Pharmacien responsable | le sien | celui de l'officine |
| Stock | le sien | le sien |
| Compte de résultat | le sien | le sien |

Les deux dernières lignes sont communes, les quatre premières s'opposent. C'est exactement la
distinction que les ERP posent entre une **entité juridique**, un **site** et un **centre de
profit / département** : Dynamics 365 réserve le bilan à l'entité juridique, rattache le site à
celle-ci par un établissement, et traite le département comme une unité opérationnelle pouvant
porter un compte de résultat sans exister juridiquement. SAP va dans le même sens : les hiérarchies
de centres de profit sont **indépendantes** de la structure juridique.

**Un troisième axe, indépendant des deux premiers.** Les décisions prises le révèlent : la
parapharmacie, qui n'est *pas* un établissement, commande en propre ; le dépôt, qui *en est* un, ne
commande pas. La capacité à commander ne se déduit donc **ni du type, ni de la nature juridique** —
c'est un attribut à part entière.

Ce n'est pas un caprice, les deux situations sont cohérentes : le dépôt est distant, tenu par un
non-pharmacien, sans compte chez le grossiste, et s'approvisionne auprès de son officine ; la
parapharmacie est sous l'œil du titulaire mais achète de la cosmétique à des fournisseurs qui ne
sont pas ceux du médicament.

```
commandeEnPropre : true  → la parapharmacie
commandeEnPropre : false → le dépôt, approvisionné par son parent
```

**Conséquence pour le modèle.** Une seule relation récursive sur `Magasin` suffit, mais chaque
nœud doit déclarer sa **nature** :

```
OFFICINE        établissement, racine, licence, FNE propre
DEPOT           établissement rattaché, licence propre, FNE propre
UNITE_GESTION   pas un établissement : stock et résultat propres, identité de son parent
```

`TypeMagasin` porte déjà `OFFICINE` / `DEPOT` ; lui ajouter la troisième valeur suffit.

**Sur la FNE, la règle est plus simple que l'axe juridique ne le laissait croire.** Le dépôt a bien
sa licence propre dans le monde administratif, mais il ne certifie rien dans ce logiciel : il ne
vend pas au comptoir, ses reventes remontent par import et sont du chiffre d'affaires de
l'officine. La parapharmacie non plus n'a pas d'identité propre.

> **Seule l'officine racine porte une identité FNE.**

C'est un invariant plus sûr qu'un drapeau par nœud : au lieu de demander « ce magasin est-il un
établissement distinct ? » à chaque appel, on résout la racine, et `fnePointOfSale` n'a tout
simplement pas à être renseignable ailleurs. Les deux magasins de la base de recette l'ont vide,
il n'y a donc rien à nettoyer.

La nature juridique garde une utilité — rattachement administratif, pharmacien responsable,
documents de licence — mais elle ne pilote **pas** la certification. Le vrai discriminant dans le
logiciel est le tableau de la §2.1.

### 2.1 Tous les nœuds ne vivent pas autant dans le système

C'est la synthèse des décisions, et elle allège considérablement le chantier. Un dépôt sert
**principalement à retracer des mouvements de stock** ; une parapharmacie est un point de vente
complet.

| | Officine | Parapharmacie | Dépôt |
|---|---|---|---|
| Stock suivi | oui | oui | **oui** |
| Ventes saisies au comptoir | oui | oui | **non — importées** |
| Caisse | oui | oui | **non** |
| Clients | partagés avec ses unités | partagés avec l'officine | **aucun** |
| Commandes fournisseur | oui | oui | **non** |
| Utilisateurs affectés | oui | oui | **non** (lecture par supervision) |
| Identité FNE | **la sienne, et la seule** | celle de l'officine | celle de l'officine |

Le dépôt n'est donc **pas** un point de vente dans le système : c'est un **lieu de stock** dont on
suit les entrées (remises de l'officine) et les sorties (ventes importées). Toute la mécanique
d'exploitation — caisse, comptoir, clients, commandes — ne le concerne pas.

**Deux conséquences pratiques :**

- La phase 2 (portée des données) a beaucoup moins à faire pour le dépôt que pour la
  parapharmacie : il n'y a ni caisse ni commande à cloisonner, seulement du stock, qui l'est déjà.
- Le client est partagé **par lieu physique**, et non globalement : officine et parapharmacie
  partagent leur clientèle parce qu'elles sont au même endroit. Le dépôt n'ayant pas de clients,
  la question de la confidentialité entre deux lieux ne se pose pas — ce qui écarte le seul risque
  que j'avais signalé sur ce point.

---

## 3. État des lieux : ce qui existe déjà, mesuré

Le point le plus important est que **la moitié du chantier est déjà faite**, et qu'une autre moitié
qu'on croit faite ne l'est pas.

### 3.1 Ce qui existe

| Élément | État |
|---|---|
| `Magasin.typeMagasin` | `OFFICINE` / `DEPOT` — le type existe, **le lien de rattachement manque** |
| `AppUser.magasin` | `@ManyToOne` **`@NotNull`** — *les utilisateurs sont déjà affectés à un magasin* |
| `Sales.magasin` | `@NotNull` — **les ventes sont déjà estampillées** |
| `Storage.magasin` | `@ManyToOne`, avec `UNIQUE (storage_type, magasin_id)` et `UNIQUE (name, magasin_id)` |
| `Magasin.fnePointOfSale` | identité fiscale **par magasin** — déjà prévue pour plusieurs sites |
| `Magasin.primaryStorage` | emplacement principal par magasin — utilisé par la vente dépôt |
| `Feature.MULTI_DEPOT` | déclarée dans la licence |
| `AppConfigurationService.getMagasin()` | `userService.getUser().getMagasin()` — utilisé dans ~52 endroits |

### 3.2 La vente dépôt : un mouvement à deux jambes, déjà en place

`VenteDepot` est une vente à part entière (`SaleDepotExtensionImpl`) qui :

- porte `CategorieChiffreAffaire.CA_DEPOT`, donc **sort du chiffre d'affaires** de l'officine ;
- **débite** le stock de l'officine, par l'emplacement principal de l'utilisateur connecté ;
- **crédite** le `primaryStorage` du dépôt à la finalisation
  (`updateDepotStockOnSaleFinalization`), avec une transaction d'inventaire dédiée.

C'est donc **déjà un mouvement à deux jambes**, et la décision ne demande aucun développement :
elle acte l'existant.

**Mais ce n'est pas un transfert : c'est une vente.** Le dépôt est un lieu physique distinct qui
vient acheter à l'officine pour aller revendre. Le code le dit — la vente dépôt se clôt sur :

```java
c.setPaymentStatus(PaymentStatus.IMPAYE);
c.setRestToPay(c.getAmountToBePaid());
```

Elle crée donc une **créance sur le dépôt**. Mesuré sur la base de recette : **53 ventes dépôt,
toutes `IMPAYE`, 3 780 485 F CFA dus.**

Le dépôt a par conséquent une **double nature**, et c'est le point à retenir de cette section :

| Vu comme | Il est | Ce qui en découle |
|---|---|---|
| un magasin | un lieu dont l'officine suit le stock | réassort, inventaire, portée de supervision |
| un client | un acheteur à crédit | une créance, un compte à suivre, des règlements |

Les deux sont vrais en même temps, et aucun des deux ne suffit. Le modéliser **seulement** comme
un magasin perdrait la créance ; **seulement** comme un client perdrait le stock, qui est déjà
suivi aujourd'hui.

### 3.3 Deux modèles économiques, et le code fait la moitié de chacun

Il n'existe aujourd'hui **aucun règlement de vente dépôt** : elles se clôturent impayées. La suite
à donner est l'une de ces deux-ci — et ce ne sont pas deux variantes d'une même fonctionnalité,
ce sont deux modèles qui s'excluent.

| | **A — le dépôt est un revendeur** | **B — le dépôt est un point de vente avancé** |
|---|---|---|
| La marchandise | devient la sienne à l'achat | reste celle de l'officine |
| La vente dépôt est | une **vente à crédit** | un **mouvement de stock** |
| Le CA de l'officine, c'est | ce qu'elle vend **au** dépôt | ce que le dépôt vend **au détail** |
| Ses reventes au détail | ne regardent pas l'officine | doivent être **importées** |
| Il faut donc bâtir | un **règlement de créance** | un **import des ventes du dépôt** |
| La demande vue par le réassort | les ventes au dépôt | les ventes importées |

**Le code fait les deux à moitié, et c'est incohérent :**

- il **crée une créance** (`PaymentStatus.IMPAYE`, `restToPay` = montant total) → modèle A ;
- il **exclut `CA_DEPOT` du chiffre d'affaires** en comptant sur les ventes importées pour le
  porter, comme le documente `CategorieChiffreAffaire` → modèle B.

Les 3 780 485 F CFA de créances de la base de recette sont le symptôme visible de ce partage :
sous le modèle B elles ne devraient pas exister ; sous le modèle A, c'est le traitement du CA qui
est faux, et l'officine déclare moins qu'elle ne vend.

#### Tranché : le modèle B, pour une raison qui invalide A

**Rien ne prouve que tout ce qui part au dépôt est vendu.** Compter la remise comme un CA
reviendrait à déclarer du chiffre sur des boîtes encore en rayon chez le dépôt — et à le déclarer
une seconde fois si elles reviennent ou si elles sont finalement vendues par l'officine.

C'est l'argument décisif, et il disqualifie le modèle A que j'avais recommandé : j'avais traité
`IMPAYE` comme la preuve d'une vente ferme, alors que c'est le signe d'une opération **non encore
dénouée**. Le mot « dépôt » le disait d'ailleurs : en langue commerciale, c'est de la
**consignation** — la marchandise reste celle du déposant jusqu'à la revente.

La règle retenue :

| Événement | Effet |
|---|---|
| Remise au dépôt (vente dépôt) | mouvement de stock, **aucun CA** |
| Revente par le dépôt, **importée** | **entrée en CA** |
| Remise de l'argent par le dépôt | **règlement dépôt**, en entrée de caisse |

**Ce qu'il faut bâtir**, dans cet ordre :

1. **L'import des ventes du dépôt** — **à bâtir**, et non à alimenter (§3.4 bis).
2. **Le règlement dépôt**, comme type de transaction financière en `CategorieTransaction.ENTREE`,
   exactement parallèle à `REGLEMENT_DIFFERE` et `REGLEMENT_TIERS_PAYANT` — le motif existe, il
   n'y a pas de mécanique à inventer.
3. **Un compte dépôt** réconciliant trois colonnes : remis, vendu (importé), réglé. L'écart est
   soit du stock encore chez le dépôt, soit un impayé — et aujourd'hui rien ne distingue les deux.

**Deux choses à revoir en conséquence :**

- `TypeFinancialTransaction.VENTES_DEPOTS` est classé en `CategorieTransaction.VENTES`. Sous la
  consignation ce n'est pas une vente : à reclasser, sous peine de montrer en caisse un chiffre
  qui n'existe pas.
- `PaymentStatus.IMPAYE` et `restToPay` = montant total décrivent une créance de vente ferme. Ce
  n'est pas cela : les 3 780 485 F CFA sont la **valeur de la marchandise en consignation**, pas
  une dette échue. À remodeler avec le compte dépôt.

*Observation jointe.* `restToPay` est posé deux fois à la clôture : d'abord depuis le DTO, puis
écrasé par le montant total. La valeur transmise par l'écran est donc perdue.

Les ventes du dépôt, elles, reviennent aujourd'hui marquées `imported = true` et **comptent dans le
CA** — la documentation de `CategorieChiffreAffaire` le dit explicitement. Autrement dit, le dépôt
vend hors du système et ses ventes sont réintégrées après coup.

**C'est ce que la décision « la vente du dépôt appartient au dépôt » change** : le dépôt doit
pouvoir vendre *dans* le système, sous son propre magasin. `imported` devient alors un chemin
hérité, à conserver pour l'historique, mais qui n'est plus la voie normale.

### 3.4 Ce qui manque

**La portée des données n'est pas appliquée.** Mesuré :

| Entité | Porte un magasin ? | Les requêtes filtrent-elles dessus ? |
|---|---|---|
| `Sales` | **oui** (`@NotNull`) | **non** — 0 mention dans `SalesRepository` |
| `StockProduit` / `Storage` | oui | oui — 20 mentions |
| `Commande` | non — mais rattachée à l'`AppUser` | — |
| `CashRegister` | non — mais rattachée à l'`AppUser` | — |
| `Customer` | non | — |

La vente est le cas instructif : **l'information est là, elle n'est jamais utilisée**. Cloisonner
les ventes ne demande donc pas de migration de données, seulement des requêtes qui lisent une
colonne déjà remplie. C'est une bien meilleure nouvelle que ce que laissait croire mon premier
relevé, qui portait sur les requêtes et non sur les entités.

**Un motif se répète.** `CashRegister` et `Commande` ne portent pas de magasin, mais tous deux
sont rattachés à un `AppUser`, lequel porte un magasin obligatoire. Le magasin d'une caisse ou
d'une commande est donc **déductible aujourd'hui**, sans rien ajouter.

Cela a une conséquence rassurante et un piège :

- *Rassurant* : la décision « la parapharmacie encaisse et commande séparément » fonctionne
  structurellement dès que son personnel est affecté à son magasin. Ce qui manque n'est pas le
  cloisonnement, c'est la **restitution** — aucun état de caisse ni aucune liste de commandes ne
  groupe par magasin.
- *Piège* : la déduction passe par l'utilisateur, et **un utilisateur peut être réaffecté**. Le
  jour où un préparateur passe de la parapharmacie à l'officine, tout son historique de caisses et
  de commandes changerait de magasin rétroactivement. Il faut donc **figer le magasin sur le
  document à sa création**, comme `Sales` le fait déjà — et non le déduire à la lecture.

**Autres manques :**

- `CashRegister` et `Commande` **ne figent pas le magasin** (voir ci-dessus).
- `Feature.MULTI_DEPOT` est déclarée et testée, mais **ne garde aucun code applicatif**.
- `LicenseService` lie la licence au **premier `OFFICINE` par ordre d'id** ; `LicensePayload` porte
  un `magasinName` unique.
- `MagasinResource` expose `hasDepot()` — un **booléen**, qui suppose un dépôt unique.
- Le magasin **ne circule pas dans le jeton JWT** : il est relu à chaque requête depuis
  l'utilisateur. C'est une chance pour ce chantier, rien à invalider.

### 3.4 bis Le mécanisme d'import n'existe pas

Je l'avais annoncé comme « un socle existant qu'il suffit d'alimenter ». C'est faux, et la nuance
change le coût de la phase 4.

Le drapeau `imported` et la règle de CA existent bien. Mais **le seul code qui pose
`imported = true`** est `ImportationVenteService`, et ce service :

- interroge un **système hérité par HTTP** — `legacyUrl + "/api/v1//whareouse-vno"` — jour par jour,
  avec une pause de 300 ms entre deux lots. C'est un outil de **reprise de données**, pas un import
  de ventes de dépôt ;
- **ne touche pas au stock** : ni entrée, ni sortie, aucune mention de `Storage` ni de
  `StockProduit` ;
- estampille le magasin depuis **l'utilisateur qui importe** (`c.setMagasin(c.getUser().getMagasin())`),
  donc l'officine — jamais le dépôt ;
- avale toute erreur : `catch (Exception e) { log.debug(" {}", e); }`. Une vente qui échoue
  disparaît sans trace, au niveau `debug`.

Zéro vente importée dans les quatre bases, ce qui est cohérent : l'outil pointe vers un système qui
n'est plus là.

**Deux conséquences pour la phase 4 :**

- L'import des ventes du dépôt est **à construire**, à partir d'un export du dépôt. Son format est
  une question ouverte : c'est le dépôt qui exporte.
- Il ne faut **pas réutiliser le drapeau `imported`** pour le distinguer. Il désigne déjà les
  ventes reprises de l'ancien système ; les deux populations se mélangeraient. Le **magasin** est
  le bon discriminant, et il est déjà porté par `Sales`.

### 3.5 La demande du dépôt est invisible au réassort de l'officine

La vue matérialisée qui alimente la VMM filtre ainsi :

```sql
WHERE s.statut = 'CLOSED' AND s.canceled = false
  AND s.ca = 'CA'          -- les ventes dépôt (CA_DEPOT) sont exclues
```

L'exclusion est **délibérée et juste** : un transfert vers le dépôt n'est pas une demande, et la
demande réelle est censée arriver autrement — par les ventes du dépôt réimportées, qui portent
`ca = 'CA'` et comptent donc dans la VMM. La documentation de `CategorieChiffreAffaire` le dit.

Le mécanisme repose donc entièrement sur l'import des ventes du dépôt. Mesuré sur la base de
recette :

| catégorie | importée | ventes | unités |
|---|---|---|---|
| `CA` | non | 21 142 | 94 775 |
| `CA_DEPOT` | non | 53 | **607** |

**Aucune vente importée.** 607 unités sont parties au dépôt, et pas une n'est comptée comme
demande nulle part. Le réassort de l'officine sous-estime son besoin d'exactement ce que le dépôt
consomme — et plus le dépôt vend, plus l'officine manque, sans que rien ne le signale.

Que ce soit vrai d'une installation réelle dépend d'une seule chose : importe-t-elle réellement les
ventes de son dépôt ? Si oui le compte est juste ; sinon le réassort tourne aveugle.

#### La règle est bonne, c'est l'alimentation qui manque

Le choix de la consignation (§3.3) tranche ceci sans rien changer au filtre. Sous la
consignation, la vraie demande est la **revente** par le dépôt, pas la remise : compter la remise
ferait commander deux fois la même boîte, une fois quand elle part au dépôt et une fois quand elle
s'y vend.

> `ca = 'CA'` — importées comprises, remises au dépôt exclues — **est la règle juste.**

Je m'étais trompé en annonçant qu'elle devrait changer. Le défaut n'est pas dans la vue : il est
que **l'import n'existe pas**. 607 unités sont parties, aucune revente n'est remontée, et la VMM
ne peut pas inventer ce qu'on ne lui donne pas.

Cela renforce la priorité de l'import : sans lui, le réassort de l'officine est structurellement
aveugle à tout ce que le dépôt écoule.

**Deux points à tenir quand le réassort deviendra par magasin (phase 5) :**

- *L'officine commande pour le dépôt*, donc son réassort doit voir la demande de **toute sa
  descendance**, pas seulement la sienne. La VMM globale d'aujourd'hui le fait par accident ; il
  faudra le faire exprès.
- *Les ventes importées doivent porter le magasin du dépôt.* `SaleServiceImpl` pose aujourd'hui le
  magasin de l'utilisateur qui importe — donc l'officine. Tant que c'est le cas, filtrer par
  magasin ne séparerait rien, et le double compte reviendrait par cette porte.

### 3.6 Un défaut FNE **actif aujourd'hui**

`FneServiceImpl` choisit l'établissement à certifier ainsi :

```java
Magasin magasin = magasinRepository.findAll().getFirst();
...
fneInvoice.setEstablishment(magasin.getName());
fneInvoice.setPointOfSale(magasin.getFnePointOfSale());
```

`findAll()` n'impose **aucun ordre**. Vérifié sur la base de recette, qui contient déjà deux
magasins :

| ctid | id | name | type |
|---|---|---|---|
| (0,2) | 5 | **PHARMA SMART DEPOT** | DEPOT |
| (0,12) | 1 | Pharma Smart | OFFICINE |

Le dépôt sort **en premier**. Sur cette base, une facture normalisée serait certifiée au nom de
`PHARMA SMART DEPOT`, avec un point de vente vide.

Ce n'est pas une conséquence du chantier : **c'est le cas dès qu'il existe plus d'un magasin**, ce
qui est déjà vrai. Le correctif est d'une ligne — lier à l'officine racine — et ne dépend d'aucune
phase. C'est la démonstration la plus nette de la §2 : sans la question « cet établissement est-il
distinct ? », le système certifie sous le mauvais nom sans rien signaler.

### 3.7 Un défaut de réassort, mesuré, indépendant de ce chantier

La base de recette contient déjà `Pharma Smart` (OFFICINE) et `PHARMA SMART DEPOT` (DEPOT), ce
dernier portant **111 produits et 583 unités**.

Or la vue `v_semois_suggestion` joint `storage` **sans filtrer sur le magasin**, là où le chemin
Java filtre sur `EntityConstant.DEFAULT_MAGASIN = 1` :

| produit | vue SEMOIS | officine seule | dépôt |
|---|---|---|---|
| DOLIPRANE ADULTES | **530** | 500 | 30 |
| INSPRA 50 mg | **54** | 48 | 6 |
| LAIT 1ER AGE 400G | **14** | 10 | 4 |

Même famille de défaut que celui corrigé en §6.3 de l'analyse commande pour la quarantaine, restée
ouverte sur la dimension magasin. **111 produits, 583 unités.** L'impact est faible aujourd'hui —
un seul de ces produits a une quantité à commander non nulle — mais la divergence mord précisément
quand le stock devient bas, c'est-à-dire quand le réassort sert à quelque chose.

**Ce défaut se corrige seul, sans rien de ce plan** (phase 0).

---

## 4. Comment les autres logiciels traitent ce besoin

### 4.1 Odoo — la récursivité est sur la société, pas sur l'entrepôt

C'est le précédent le plus proche, et il valide la proposition.

- `stock.warehouse` **n'a pas** de champ « entrepôt parent ». La hiérarchie des entrepôts se fait
  par l'arbre des *emplacements*, et la mutualisation du stock passe par un entrepôt *virtuel*
  dont l'emplacement sert de parent à ceux des entrepôts enfants.
- `res.company`, en revanche, **a bien un parent** : sociétés mère et filles. Odoo 19 y a même
  ajouté la notion de *branch*, une sous-entité d'une société.

Autrement dit, Odoo place la récursivité là où elle est **organisationnelle**, et laisse
l'approvisionnement à un autre arbre. C'est exactement la séparation proposée ici : `Magasin` porte
le parent, `Storage` reste l'emplacement.

**La supervision y est résolue par les « sociétés autorisées ».** Chaque utilisateur porte une
liste `company_ids` — les sociétés auxquelles il a accès — et un sélecteur permet de basculer sans
se reconnecter. Les règles d'enregistrement s'appuient dessus, typiquement
`['|', ('company_id','=',False), ('company_id','in',company_ids)]`. Dans les versions anciennes, un
domaine `child_of` rendait visibles les enregistrements des sociétés filles depuis la mère.

**C'est précisément ce que vous demandez** : le pharmacien n'a pas à se connecter au dépôt, il le
voit parce que le dépôt est sous son officine. Odoo le fait par une liste explicite ; nous pouvons
le faire par le rattachement, ce qui est plus simple et moins faillible — la liste ne peut pas
diverger de l'arbre si elle *est* l'arbre.

*Réserve :* le comportement parent/enfant d'Odoo a varié selon les versions, et plusieurs sources
signalent que les règles d'enregistrement seules ne suffisent pas pour les opérations croisées
entre société mère et fille. À prendre comme une direction, pas comme une spécification.

### 4.2 Les ERP généralistes — trois niveaux, pas un

Dynamics 365 et SAP distinguent systématiquement :

| Niveau | Rôle | Notre équivalent |
|---|---|---|
| **Entité juridique** | bilan, contrats, déclarations | l'officine |
| **Site / établissement** | opérations, rattaché à l'entité juridique | le dépôt |
| **Centre de profit / département** | compte de résultat interne, **sans existence juridique** | la parapharmacie |

Dynamics note qu'un département « peut porter une responsabilité de résultat » et « servir de
centre de profit », et traite un magasin physique comme un type d'unité opérationnelle distinct.
SAP insiste sur l'indépendance des hiérarchies de centres de profit vis-à-vis de la structure
juridique.

C'est la justification externe de la §2 : la parapharmacie relève du troisième niveau, pas du
deuxième.

### 4.3 Les LGO français — données centralisées, droits par site

Le marché est concentré sur `id.` (ex-LGPI, Pharmagest) et Winpharma, environ 77 % des officines.

- **LGPI / id.** est présenté comme le choix des structures multi-sites.
- **Winpharma** propose `winGroupe`, pour les **commandes groupées avec suivi multi-sites**.
- **Pharmony One** se décrit comme nativement multi-site : *données centralisées, droits par
  officine, déploiement rapide d'un nouveau site*.

Le motif dominant est **une base unique, un cloisonnement par droits** — jamais une base par site.

### 4.4 Le motif relationnel

Pour un arbre **peu profond, rarement modifié et souvent lu**, la liste d'adjacence (`parent_id`)
est le choix standard ; les tables de fermeture ne se justifient qu'à partir d'une dizaine de
niveaux, au prix d'une amplification d'écriture. PostgreSQL **18.1** est en service, donc la clause
`CYCLE` de `WITH RECURSIVE` est disponible si une requête récursive devient nécessaire — à notre
profondeur, une jointure simple suffira.

---

## 5. Conception retenue

### 5.1 Le lien

```sql
ALTER TABLE magasin ADD COLUMN parent_id INTEGER REFERENCES magasin (id);
CREATE INDEX idx_magasin_parent ON magasin (parent_id);
ALTER TABLE magasin ADD CONSTRAINT magasin_parent_pas_soi_meme CHECK (parent_id IS DISTINCT FROM id);
```

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_id")
private Magasin parent;

@OneToMany(mappedBy = "parent")
private Set<Magasin> sousMagasins = new HashSet<>();
```

### 5.2 Les invariants

| Invariant | Où |
|---|---|
| Un `DEPOT` et une `UNITE_GESTION` **doivent** avoir un parent | service, pas `NOT NULL` — l'existant n'en a pas |
| Une `OFFICINE` **ne doit pas** avoir de parent | service ; la loi interdit la grappe d'officines |
| Seule la **racine** porte une identité FNE | `fnePointOfSale` non renseignable ailleurs |
| Pas de cycle | `CHECK` pour l'auto-référence, contrôle applicatif au-delà |
| Profondeur maximale 2 | contrôle applicatif, avec un message qui explique *pourquoi* |

Je recommande de **ne pas** poser `parent_id NOT NULL` : la migration devrait alors deviner le
parent des dépôts existants. Elle le fera (phase 1), mais une installation peut avoir plusieurs
officines historiques ; mieux vaut une anomalie visible qu'une migration qui tranche en silence.

### 5.3 Deux portées, pas une

C'est la réponse à l'exigence de supervision, et elle évite le piège d'un cloisonnement qui
empêcherait le titulaire de faire son travail.

| Portée | Qui | Sur quoi |
|---|---|---|
| **Portée d'exploitation** (lecture + écriture) | tout utilisateur | **son** magasin uniquement |
| **Portée de supervision** (lecture seule) | détenteur du privilège | **son magasin et toute sa descendance** |

Le pharmacien titulaire est affecté à l'officine et détient le privilège : il voit les mouvements
du dépôt et de la parapharmacie **sans s'y connecter**, et sans pouvoir y saisir. Un préparateur du
dépôt ne voit que le dépôt.

Deux propriétés qui valent d'être notées :

- La supervision **découle de l'arbre**, elle ne se paramètre pas magasin par magasin. Rattacher un
  nouveau dépôt suffit à le rendre visible au titulaire — rien à ne pas oublier.
- Elle est **en lecture seule**. Un titulaire qui veut saisir dans le dépôt bascule explicitement
  de magasin ; l'écriture reste toujours attribuée à un magasin unique et identifiable.

Le privilège s'inscrit dans le dispositif `nav_item_role` existant plutôt que dans un mécanisme
nouveau.

### 5.4 La portée, domaine par domaine

| Domaine | Portée | Justification |
|---|---|---|
| Stock, inventaires, réassort | **par magasin** | réalités physiques distinctes |
| Ventes | **par magasin** | décision prise ; `Sales.magasin` est déjà renseigné |
| Caisses | **par magasin** | à condition d'ajouter `magasin_id` à `CashRegister` |
| Produits, fournisseurs, tarifs | **globaux** | un dépôt n'a pas son propre référentiel |
| Clients, tiers-payants | **par lieu physique** | le même client achète en parapharmacie et à l'officine ; un dépôt n'en a pas |
| Commandes fournisseur | **par magasin si `commandeEnPropre`**, sinon portées par le parent | décisions prises : la parapharmacie commande seule, l'officine commande pour le dépôt |
| Paliers RFA, francos | **sur la racine** | le compte grossiste appartient au titulaire de la licence |
| Chiffre d'affaires, déclarations | **remontantes** | consolidation à l'officine, ventilée par magasin |

**Commande et palier ne se calculent pas au même niveau**, et c'est la subtilité de la décision
« la parapharmacie commande séparément ». Le bon de commande est saisi par magasin ; mais si
l'officine et la parapharmacie commandent au même grossiste, c'est **un seul compte** chez lui —
celui du titulaire de la licence. Le palier RFA et le franco de port doivent donc cumuler sur
l'établissement, exactement comme ils cumulent déjà sur le **groupe** d'agences d'un grossiste.
Séparer les bons sans cumuler les paliers ferait perdre de la remise sans que personne ne le voie.

En pratique le cas restera rare — une parapharmacie s'approvisionne surtout en cosmétique, chez
d'autres fournisseurs — mais la règle doit être posée, car c'est de l'argent.

Le CA mérite une précision, car la décision le modifie : aujourd'hui les ventes du dépôt entrent
dans le CA de l'officine par le canal `imported`, et les ventes *au* dépôt en sortent via
`CA_DEPOT`. Une fois le dépôt vendant sous son propre magasin, la règle devient lisible :
**`CA_DEPOT` reste hors CA** (c'est un transfert interne, pas une vente), et le CA se consolide en
sommant les magasins de l'arbre.

---

## 6. Le plan, par phases

Chaque phase est livrable seule et ne change pas le comportement d'une installation mono-magasin.

**État et priorités** — les numéros disent l'ordre logique, pas l'ordre de travail :

| Phase | Priorité | État |
|---|---|---|
| 0 — Les deux défauts actifs | — | ✅ faite |
| 1 — Le lien et les natures | — | ✅ faite |
| 2 — Les deux portées | — | ✅ **faite** — chantiers A, B et C |
| 3 — Les utilisateurs | — | ✅ **faite** — réaffectation et `MULTI_DEPOT` |
| 5 — Le réassort par magasin | — | ✅ **faite** — configuration, batch, vue et sélecteur |
| 4 — Boucler le circuit du dépôt | — | ⏸ écartée : règlement par l'écran de caisse existant |
| 6 — Transfert suggéré | — | ⏸ **pas une priorité** : écartée tant qu'il n'y a pas de parapharmacie |

### Phase 0 — Les deux défauts actifs — ✅ **faite**

`V2.1.53` pour la vue SEMOIS, et la résolution de l'officine dans `FneServiceImpl`. Mesuré avant /
après sur la base de recette : **111 lignes en désaccord vue/Java → 0**. Tests ajoutés :
`FneCertificationMagasinTest` (3 cas, là où la FNE n'en avait aucun) et un cas de non-régression
dans `SemoisStockActuelIntegrationTest` — 500 unités au dépôt contre 13 à l'officine, dans l'esprit
du test de quarantaine voisin. Les deux ont été vérifiés en rétablissant temporairement l'ancien
code : ils tombent.

*À savoir pour les vérifications de ce genre* : Maven ne supprime pas de `target/classes` un
fichier retiré des sources. Une première vérification a donc été **fausse** — le test passait sans
la migration, Flyway lisant l'ancienne copie. Il faut retirer le fichier des deux endroits.

#### Ce qu'il fallait faire *(conservé pour mémoire)*

1. **FNE** : remplacer `magasinRepository.findAll().getFirst()` par la résolution de l'officine,
   et un test qui échoue si l'établissement certifié n'est pas l'officine. À faire **en premier** :
   c'est de la certification fiscale, et le défaut est présent dès aujourd'hui. Sans hiérarchie,
   « l'officine » se résout déjà par `typeMagasin = OFFICINE` : le correctif ne dépend d'aucune
   phase.
2. **Réassort** : ajouter le filtre magasin à `v_semois_suggestion`, comme le fait déjà le chemin
   Java, avec un test qui confronte les deux lectures — le harnais
   `SemoisStockActuelIntegrationTest` existe déjà pour cela.

### Phase 1 — Le lien et les natures — ✅ **faite**

`V2.1.51` : `parent_id` avec clé étrangère, index, `CHECK` d'auto-référence, et
`commande_en_propre`. Les dépôts existants sont rattachés **si et seulement s'il y a exactement une
officine** — sinon la colonne reste nulle et rien n'est deviné. Vérifié sur la base de recette : le
dépôt prend `parent_id = 1` et `commande_en_propre = false`.

`Magasin` porte `parent` (en `LAZY` délibérément : l'entité est chargée en `EAGER` par `AppUser`),
`commandeEnPropre` et `estRacine()`. `TypeMagasin` gagne `UNITE_GESTION`.

`MagasinService` tient les invariants et expose `resoudreOfficineRacine` — la remontée est **bornée**
plutôt que supposée acyclique, un parent mal posé à la main ne devant pas faire tourner le serveur.

**Deux pièges rencontrés, et ce qu'ils ont changé :**

- *Le défaut du DTO écrasait la donnée.* `commandeEnPropre` en `boolean` valait `true` quand le
  champ était absent du JSON — et l'écran des dépôts ne l'envoie pas. Chaque modification d'un
  dépôt l'aurait rendu commandeur, silencieusement. Le champ est devenu `Boolean` : `null` veut
  dire « non renseigné », et le service reprend alors l'existant, ou le défaut de la nature à la
  création. Un test tient ce cas.
- *Pas de sélecteur de parent.* Le plan le prévoyait ; avec une seule officine il n'offrirait qu'un
  choix et n'ajouterait que du bruit. Le rattachement implicite du service couvre le cas, et le
  message d'erreur demande de préciser dès qu'il y a deux officines. Le modèle front porte
  `parentId` pour le jour où un écran en aura besoin.

**14 tests** (`MagasinHierarchieTest`) : ce qui est refusé, ce qui est accepté, la remontée à la
racine.

**Au passage** : `TypeMagasin.DEPOT_AGGREE` existe dans le modèle front et **pas** côté backend.
Aucun écran ne le propose ; marqué `@deprecated` plutôt que retiré, faute d'avoir vérifié toutes
les données historiques.

#### Ce qu'il fallait faire *(conservé pour mémoire)*

1. Migration : `parent_id`, index, `CHECK` d'auto-référence.
2. Entité `Magasin` : `parent` / `sousMagasins` ; `TypeMagasin.UNITE_GESTION` ;
   `commandeEnPropre` ; résolution de la racine.
3. Rattachement des dépôts existants : **s'il existe exactement une officine**, les rattacher à
   elle ; sinon laisser `NULL` et le signaler au démarrage.
4. `MagasinService` : les invariants de §5.2, et `resoudreOfficineRacine(magasin)` — c'est elle
   que la FNE, la licence et les paliers RFA doivent interroger.
5. `LicenseService` : lier à l'officine **racine** et non au premier `OFFICINE` par id.
6. Écran d'administration : choisir le parent et la nature à la création.

**À ce stade rien ne change fonctionnellement.**

### Phase 2 — Les deux portées

#### Chantier A — Figer le magasin sur les documents — ✅ **fait**

`V2.1.51` : `magasin_id` sur `cash_register` et `commande`, en quatre temps — colonne nullable,
reprise depuis `user.magasin`, `SET NOT NULL`, clé étrangère et index. Éprouvée sur la base de
recette : 944 caisses et 768 commandes rattachées, **aucun nul**. `app_user.magasin_id` étant
`NOT NULL` avec zéro orphelin, le `SET NOT NULL` tient.

L'estampille est posée **aux 10 points de création** — 9 pour `Commande`, 1 pour `CashRegister`.

**Le choix du magasin à la création d'un utilisateur** y a été rattaché, parce que c'est le même
geste : `UserService` faisait hériter le magasin du créateur, et **aucun écran ne permettait de
choisir**. Une parapharmacie pouvait donc exister sans que personne puisse y travailler — tous les
comptes naissaient à l'officine, et seul un compte de la parapharmacie aurait pu en créer un autre.
`resoudreLeMagasinDAffectation` accepte un magasin **dans la portée du créateur** et refuse le
reste ; le champ venant du client, il ne se croit pas sur parole. Nouveau point d'entrée
`GET /api/magasins/affectables`, et un sélecteur côté front **masqué s'il n'y a qu'un magasin** —
et masqué aussi en modification, la réaffectation n'étant pas implémentée.

**Ce que la vérification a appris, et qui ne se devinait pas :**

- *L'estampille devait vivre dans `create()`, pas au niveau REST.* Il existe une **délégation** :
  `checkIfCashRegisterIsOpen` fait ouvrir la caisse d'un caissier **par l'admin**
  (`openCashRegister(admin, caissier)`). Le magasin doit être celui du caissier. Estampiller depuis
  l'utilisateur connecté aurait donné celui de l'admin — silencieusement.
- *Le vrai risque d'une colonne `NOT NULL` est l'écriture, pas la jointure.* Audit demandé :
  **3 `INSERT` natifs** dans `cash_register` (essais de `declaration_ca`) énuméraient leurs
  colonnes et auraient violé la contrainte. Aucune vue n'utilise ces tables, les 4 fonctions qui
  les citent sont en lecture seule à colonnes nommées, et aucune projection n'est étoilée.
- *600 erreurs au premier balayage, aucune dans le code de production* : des `Commande` bâties par
  les **fabriques d'essai**. 16 complétées avec la même règle qu'en production.
- *Un essai à moi polluait les suivants.* `SecurityContextHolder` est un `ThreadLocal` et Surefire
  réutilise le fil : l'authentification posée sans `clearContext()` faisait tomber
  `SaleServiceImplTest`, sur un sujet sans rapport. `@AfterEach` obligatoire.

**10 essais** (`CaisseMagasinFigeTest` 2, `AffectationUtilisateurTest` 8), les deux premiers vérifiés
en retirant l'estampille : ils tombent.

**Décision : aucun utilisateur ne s'affecte à un dépôt.** Il n'y a rien à y saisir — les ventes au
dépôt se font à l'officine, ses reventes entrent par import — et un compte affecté là y ouvrirait
des sessions de caisse dans un établissement qui n'a pas de caisse.

Le contrôle porte sur le magasin **retenu**, et non sur celui qui est demandé : sinon l'héritage
serait une faille, un créateur lui-même affecté à un dépôt y faisant naître tous ses comptes sans
rien demander. Et la liste des magasins d'affectation n'en propose aucun : le front n'offre pas ce
que le backend refuse.

#### Chantier B — Le point de résolution et le privilège — ✅ **fait**

`PorteeMagasinService` rend les deux notions, et **les sépare** :

- `resoudreLeMagasinDExploitation()` — là où j'écris. Un seul, celui de mon compte.
- `resoudreLaPorteeDeSupervision()` — ce que je peux lire : mon magasin, plus ses rattachés si je
  détiens le droit.
- `superviseDAutresMagasins()` — pour les écrans qui masquent un sélecteur inutile.

`V2.1.51` pose le droit `supervision.magasins` en `nav_item` de type **ACTION** — le type des droits
sans écran, dont 32 existent déjà. Il se donne donc par l'écran d'administration des rôles, sans
mécanique nouvelle, et l'administrateur passe de toute façon par construction.

**La migration n'accorde le droit à personne.** C'est à l'officine de décider quels rôles
supervisent ; l'accorder d'office élargirait l'accès de quelqu'un sans que personne l'ait demandé.

**Le point à ne pas manquer : le droit n'élargit que la lecture.** Le magasin d'exploitation ne
bouge pas, superviseur ou non. Les confondre donnerait à un titulaire le pouvoir d'encaisser au nom
de sa parapharmacie sans l'avoir voulu — un essai tient précisément cette frontière. La bascule
explicite pour écrire ailleurs reste la phase 3.

Sans le droit, les rattachés ne sont **même pas interrogés** : la question ne se pose pas, et un
essai le vérifie. La descente est bornée, comme les remontées du chantier A.

**7 essais** (`PorteeMagasinTest`). Rien ne consomme encore ce service : le chantier est livrable
et **inerte**, comme la phase 1.

#### Chantier C — La restitution par magasin — ✅ **fait**

**Les listes sont cloisonnées** par la portée de lecture : caisses (`specialisationMagasins`) et
commandes (`CommandeFilterDTO.magasinIds`). Le filtre s'appuie sur le magasin **figé** du document,
non sur celui de l'utilisateur qui relit — c'est ce qui rend le cloisonnement stable dans le temps.

`filterCommandeLines` est couvert volontairement : sans cela, **l'identifiant d'une commande d'un
autre magasin aurait suffi à en lire les lignes**. C'est une fuite, pas une commodité.

**L'écran de supervision** (`gestion-caisse`) gagne un sélecteur de magasin et une colonne magasin,
tous deux **masqués quand la portée ne contient qu'un magasin** — le cas courant d'une officine
seule, où ils n'ajouteraient que du bruit.

**Deux règles fixées par essai, parce qu'elles ne se devinent pas :**

- *Une portée vide ne filtre rien.* Voulu : les travaux planifiés n'ont pas d'utilisateur connecté,
  et leur imposer une portée les priverait de tout. Le revers est qu'un oubli ne se voit pas — une
  liste dont on ne pose pas la portée rend tout. Le contrat est donc écrit noir sur blanc.
- *Le magasin choisi est INTERSECTÉ avec la portée.* Hors portée, l'intersection est vide et la
  liste l'est aussi. Rendre toute la portée aurait ignoré le filtre ; rendre le magasin demandé
  aurait ouvert une porte.

**Mesuré sur la base de recette** : 944 caisses sur 944 et 768 commandes sur 768 restent visibles.
Le cloisonnement ne change rien aujourd'hui — le point à vérifier plutôt qu'à supposer, C étant le
premier chantier visible.

**4 essais** (`CloisonnementListesTest`).

#### Ce qui reste de la phase 2

1. Un point de résolution unique rendant, pour l'utilisateur courant, son magasin d'exploitation et
   l'ensemble de sa portée de supervision — sur le modèle de `@RequiresNavAccess`, qui a fait ses
   preuves. Les 52 points d'appel de `getMagasin()` sont le point d'accroche.
2. Le privilège de supervision et son rattachement `nav_item_role`.
3. Application domaine par domaine : **la vente d'abord**, puisque `Sales.magasin` est déjà
   renseigné et qu'aucune migration de données n'est nécessaire.
4. **Figer le magasin** sur `CashRegister` et `Commande` à la création, plutôt que de le déduire
   de l'utilisateur : colonne, valeur posée à la création, migration qui remplit l'historique
   depuis `user.magasin` — c'est la seule fois où la déduction est légitime, l'historique n'ayant
   pas d'autre source.
5. Les états de caisse et les listes de commandes groupent et filtrent par magasin.
6. Paliers RFA et francos : cumuler sur la **racine**, non sur le magasin (§5.4).
7. Les écrans de supervision : un sélecteur de magasin **masqué s'il n'y en a qu'un**, et une
   colonne magasin dans les listes de mouvements.

**C'est la phase longue.**

### Phase 3 — Les utilisateurs — ✅ **faite**

**La réaffectation d'un compte existant.** `updateUser` ignorait purement et simplement le magasin ;
il passe maintenant par les **mêmes contrôles que la création** — portée du demandeur, jamais un
dépôt — car une modification qui n'aurait pas ces contrôles serait une porte dérobée vers ce que la
création refuse. Le sélecteur s'ouvre en modification, toujours masqué s'il n'y a qu'un magasin.

Un magasin absent ou inchangé **ne déplace personne** : un écran qui n'envoie pas le champ, comme
la modification de son propre profil, ne doit pas réaffecter qui que ce soit.

**Une caisse ouverte bloque la réaffectation.** Cela découle d'une règle déjà posée — une caisse ne
se réaffecte pas : déplacer son titulaire laisserait une caisse ouverte dans le magasin qu'il vient
de quitter, encaissant sous une identité qui n'y travaille plus. Le caissier fait son billetage,
puis il change de magasin. Une caisse **fermée** n'empêche rien, sans quoi un caissier resterait
prisonnier de son magasin pour avoir encaissé une fois.

Ce qu'il a déjà fait ne bouge pas : ses caisses et commandes passées portent le magasin d'alors.
C'est tout l'intérêt de l'estampille figée du chantier A — l'historique dit où la recette a été
faite, pas où son auteur travaille aujourd'hui.

**`Feature.MULTI_DEPOT` ne gardera rien, et c'est une décision.** Le multi-magasins **ne se vend
pas séparément** (2026-11-16). La fonction reste donc couverte d'office.

Un garde-fou avait été écrit — le second magasin et les suivants l'exigeaient — puis **retiré** une
fois la décision connue : un contrôle qui ne peut jamais refuser est pire que pas de contrôle, car
il laisse croire à une règle qui n'existe pas, et quelqu'un finirait par « terminer le travail » en
basculant le drapeau. La consigne est écrite sur l'énumération elle-même, avec ce qu'il faudrait
faire si la décision changeait : passer `optional = true` **et** écrire le contrôle à la création du
**second** magasin, jamais du premier.

**5 essais** ajoutés à `AffectationUtilisateurTest` (13 au total), dont 4 tombent si l'on retire la
ligne qu'ils gardent.

#### Ce qui n'a pas été fait, et pourquoi

Les deux autres points du plan — **bascule de magasin en session** et **affectation multiple en
écriture** — n'ont **aucun sujet aujourd'hui** : `app_user.magasin_id` est unique et `NOT NULL`,
donc personne n'écrit sur plusieurs magasins. La bascule suppose l'affectation multiple, et
l'affectation multiple était déjà conditionnée à un besoin avéré. Les construire maintenant
reviendrait à bâtir un sélecteur pour un ensemble à un élément.

### Phase 4 — Boucler le circuit du dépôt — ⏸ **écartée pour le moment**

**Décision : pas d'import des ventes du dépôt**, et le règlement dépôt se saisit par l'écran de
mouvement de caisse existant, en **entrée de caisse**. Rien à développer : le formulaire propose
déjà le type, `getTypeName` le traduit, le sens se déduit de la catégorie du type, et le montant
atteint le tiroir, le total crédité et le ticket Z.

Ce que ce choix laisse de côté, pour mémoire et sans jugement :

- **Le mouvement n'est pas distinguable** d'une entrée de caisse ordinaire. Il s'ajoute aux 46
  entrées de la base de recette, et seul le commentaire libre du caissier le nomme. Aucun état ne
  pourra isoler « ce que les dépôts ont réglé ».
- **Le dépôt qui règle n'est pas enregistré**, donc le compte dépôt — remis / vendu / réglé — reste
  hors d'atteinte.
- **Les ventes dépôt restent impayées.** Les 53 ventes de la base de recette portent
  `PaymentStatus.IMPAYE` pour 3 780 485 F CFA, et une entrée de caisse ne les acquitte pas. La
  créance affichée continuera de croître à mesure des remises au dépôt.

Si ces trois points deviennent gênants, le chantier décrit ci-dessous reprend tel quel.

#### Ce qu'il faudrait faire *(conservé pour mémoire)*

Le dépôt ne vend pas *dans* le système : il n'y a ni caisse, ni client, ni comptoir à lui. Ce qui
rentre, ce sont ses ventes importées et son argent.

1. **L'import des ventes du dépôt**, estampillées de **son** magasin, et **décrémentant son
   stock** — c'est ce qui le fait vivre dans le système et ce qui nourrit le réassort de l'officine.
   À bâtir : le mécanisme n'existe pas (§3.4 bis).
2. **Le règlement dépôt** en `CategorieTransaction.ENTREE`.
3. **Le compte dépôt** : remis / vendu / réglé, l'écart étant du stock ou un impayé.
4. Reclasser `TypeFinancialTransaction.VENTES_DEPOTS`, aujourd'hui rangé en `VENTES` alors que la
   remise n'est pas une vente.
5. Reprendre `PaymentStatus.IMPAYE` et `restToPay`, qui décrivent une créance de vente ferme.

### Phase 5 — Le réassort par magasin — ✅ **faite** *(= A10 de l'analyse commande)*

Cinq chantiers, tous livrés.

**A — La configuration porte son magasin.** `semois_configuration` gagne `magasin_id`, clé
`(produit_id, magasin_id)` (V2.1.53). Les **paramètres** sont dupliqués vers les autres magasins
commandeurs — classe, délais, fréquence, marge, exclusions ; les **valeurs calculées** ne le sont
pas. Recopier la VMM de l'officine dans une parapharmacie inventerait des chiffres, et une
suggestion bâtie dessus ferait commander au hasard avec l'aplomb d'un calcul. Le batch les remplit
à son passage, et une ligne sans calcul ne suggère rien.

Côté Java, `produit` passe de `@OneToOne` à `@ManyToOne` — le laisser rendrait une ligne au hasard
dès la seconde — et `createDefault` exige désormais un magasin, ce qui a mis le compilateur sur les
26 points à reprendre.

**B — Le calcul est paramétré.** Les `EntityConstant.DEFAULT_MAGASIN` ont disparu du code de
production. À la place, `magasinDuReassort()` résout **l'officine**, et s'arrête s'il n'y en a pas
plutôt que de se rabattre sur le premier magasin venu.

**C — Le batch itère.** Sur les magasins qui **commandent en propre**, non sur « les magasins
actifs » comme l'écrivait ce plan : un dépôt ne commande pas, lui calculer un réassort produirait
des bons qu'aucun fournisseur n'attend. Le nettoyage des suggestions vides reste conditionné à un
passage effectif — décision que gardait un essai, traduite au pluriel plutôt que changée en
passant.

**D — La vue et le sélecteur.** `v_semois_suggestion` tient une ligne par (produit, magasin) et lit
le stock **du magasin de la configuration** (V2.1.55). L'identité de l'entité devient composée :
s'en tenir au produit ferait confondre par Hibernate la ligne de l'officine et celle de la
parapharmacie. Les sept requêtes du repository portent leur magasin, sans défaut. L'écran de
réassort porte un sélecteur, masqué tant qu'il n'y a qu'un magasin commandeur.

**E — `search_produits_json` exige son magasin.** Le `DEFAULT 1` traîné depuis V1.0.5 est retiré
(V2.1.54). Tous les appels le passaient déjà : le défaut ne servait personne, il attendait qu'on
l'oublie.

#### Ce que la phase a révélé

**Une parapharmacie ne pouvait pas exister.** Le contrôle `magasin_type_magasin_check`, posé par
V1.0.1, n'acceptait que `'OFFICINE'` et `'DEPOT'`. `UNITE_GESTION` existait côté Java depuis la
phase 1 : tout ce qui repose sur lui — `commande_en_propre`, l'affectation d'un utilisateur
ailleurs qu'à l'officine, la portée de supervision — était écrit, éprouvé par des essais unitaires,
et **impossible à exercer pour de vrai**. Trouvé en voulant créer une parapharmacie pour éprouver
la duplication du réassort. Corrigé, et fondu dans la migration de hiérarchie.

**La vue doublait.** Une fois `semois_configuration` par magasin, la vue jointe sans magasin rend
une ligne **par configuration**. Mesuré sur le jeu de recette : une parapharmacie dont le batch a
calculé la VMM fait passer la vue de 675 à 1 350 lignes pour les mêmes 675 produits. Le défaut
n'attend pas la création du magasin mais le premier calcul de sa VMM — le batch de la nuit suffit.

#### Les migrations ont été fusionnées

L'installation de référence s'arrête à **2.1.47** : rien au-delà n'a jamais tourné ailleurs que sur
les bases de recette. Trois réécritures successives de la même vue, et quatre montées de la
hiérarchie des magasins, ont donc été fondues chacune en un seul script, et la numérotation reprise
de façon contiguë jusqu'à **V2.1.55**. Épreuve : 162 migrations rejouées depuis zéro sur un
PostgreSQL neuf.

### Phase 6 — Transfert suggéré — ⏸ **pas une priorité**

Écartée. Elle n'a personne entre qui transférer tant qu'il n'y a pas de parapharmacie, et la mesure
ci-dessous dit ce qu'elle rapporterait aujourd'hui : une unité. Le contenu reste pour mémoire.

Avant de suggérer une commande, regarder si un autre magasin a un excédent au-delà de son
objectif, et proposer un transfert plutôt qu'une ligne de commande.

**Décision : les dépôts sont exclus du transfert.** Un dépôt est un lieu physique distinct, et la
marchandise n'y va que dans un sens. Le transfert ne concerne donc que les magasins **partageant un
même lieu** — l'officine et sa parapharmacie.

Cette exclusion **referme un trou de ce plan** : j'avais écrit que le transfert serait « matérialisé
par une vente dépôt, puisqu'elle fait déjà le mouvement ». C'était faux dans le sens utile. Une
vente dépôt va de l'officine *vers* le dépôt ; un dépôt en excédent aurait exigé un mouvement de
**retour** qui n'existe nulle part. Les dépôts étant hors du périmètre, la question ne se pose plus.

**Ce qui manque quand même : le mouvement lui-même.** Il n'existe aucun déplacement de stock entre
magasins ni entre emplacements. `DeplacementStockType` déclare bien `MOUVEMENT_STOCK_IN` et
`MOUVEMENT_STOCK_OUT`, mais cet enum est **employé nulle part** — ni backend, ni front, ni
migration. C'est le vestige d'une intention. Le mouvement reste à bâtir, mais il est simple : même
lieu, ni argent, ni vente, ni FNE.

**Deux dépendances :** la notion d'« excédent au-delà de son objectif » suppose un objectif par
magasin, que `semois_configuration` ne porte pas (aucune colonne magasin) — donc la **phase 5**
d'abord. Et la valeur mesurée est nulle tant qu'il n'y a pas de parapharmacie : sur la base de
recette, **1 produit** sur les 76 à commander est aussi tenu au dépôt, pour **1 unité**
transférable. Le recouvrement est petit par construction, un dépôt tenant une gamme étroite de
produits qui tournent vite — ceux que l'officine garde justement bien approvisionnés.

---

## 7. Pièges identifiés

- **L'identité fiscale.** Ni la parapharmacie ni le dépôt ne certifient : tout part sous le point
  de vente FNE de **l'officine racine**.
  Rien aujourd'hui n'empêche de renseigner `fnePointOfSale` sur n'importe quel magasin, et le
  service FNE prend déjà le mauvais (§3.6). C'est le risque le plus sérieux de l'ensemble, et il
  est silencieux.
- **La licence.** `LicenseService` lie au premier `OFFICINE` par ordre d'id ; un dépôt créé avant
  l'officine déplacerait la liaison. Une ligne, mais une ligne qui bloque le démarrage.
- **`hasDepot()` booléen.** Deux appels front en dépendent ; il devient faux dès qu'on raisonne par
  rattachement.
- **Les séquences de numérotation.** À vérifier avant la phase 4 : une numérotation de facture
  partagée entre deux établissements est un problème fiscal, pas un problème d'affichage.
- **Les indicateurs de pilotage.** `CategorieChiffreAffaire` et `IndicateurPilotage` documentent la
  règle « importées comprises, `CA_DEPOT` exclues ». La phase 4 la change : à reprendre en même
  temps, sous peine d'un CA faux pendant une version.
- **Le jeu de démonstration.** Il contient déjà un dépôt avec du stock. Toute phase qui change la
  portée changera les chiffres des parcours e2e — à relire, pas à subir.
- **Hibernate.** `Magasin` est en cache de second niveau et chargé en `EAGER` par `AppUser` ;
  ne pas rendre `sousMagasins` EAGER.

---

## 8. Ce que je ne ferais pas

- **Le groupement de plusieurs officines.** Interdit par la règle « une officine par pharmacien »,
  et relevant d'une architecture multi-tenant.
- **Une table de fermeture.** Inutile à profondeur 2.
- **Mettre le rattachement sur `Storage`.** Confond l'emplacement physique et l'établissement.
- **Une liste explicite de magasins autorisés par utilisateur**, à la manière d'Odoo, tant que la
  portée dérivée de l'arbre suffit : une liste peut diverger de la hiérarchie, l'arbre non.
- **Donner l'écriture avec la supervision.** Une vente doit toujours être attribuable à un magasin
  unique, choisi explicitement.

---

## 9. Questions ouvertes

**Tranchées :**

1. ~~La vente d'un dépôt appartient-elle au dépôt ou à l'officine ?~~ **Au dépôt.**
2. ~~Une unité de gestion a-t-elle sa propre caisse ?~~ **Oui** — `CashRegister` porte donc le
   magasin, pas l'établissement.
3. ~~Une unité de gestion commande-t-elle ? Et le dépôt ?~~ **La parapharmacie oui, le dépôt non.**
   D'où `commandeEnPropre`, indépendant de la nature du nœud (§2).
4. ~~Le client est-il partagé ?~~ **Oui, au sein d'un même lieu physique.** Le dépôt n'ayant pas de
   clients, aucune donnée de santé ne circule entre deux lieux.
5. ~~Modèle A ou modèle B pour le dépôt ?~~ **Consignation** (§3.3) : le CA naît à l'import des
   reventes, l'argent rentre par un règlement dépôt.
6. ~~Comment la demande du dépôt remonte-t-elle au réassort de l'officine ?~~ **Par l'import de ses
   reventes** (§3.5). La règle `ca = 'CA'` est déjà la bonne ; ce qui manque est l'alimentation,
   d'où la priorité de la phase 4.

**Encore ouvertes :**

7. ~~Le titulaire doit-il pouvoir saisir dans le dépôt ?~~ **Il n'y a rien à y saisir.** Les ventes
   au dépôt se saisissent à l'officine, et les reventes du dépôt entrent par import. La portée de
   supervision est donc **en lecture seule, sans exception**, et aucun basculement de magasin n'est
   nécessaire pour un dépôt.
8. ~~Combien de magasins dans les installations réelles ?~~ **De 0 à 10** (2026-11-16) — le
   zéro se lisant « aucun magasin en plus de l'officine », qui reste le cas courant.

   Trois conséquences, toutes rassurantes :

   - **Le sélecteur est la bonne forme.** À dix éléments au plus, une liste déroulante suffit :
     ni recherche, ni pagination, ni chargement différé. Ce qui est déjà en place convient.
   - **Les bornes posées tiennent.** La remontée de hiérarchie est bornée à 2 niveaux pour
     l'affectation et 3 pour la portée ; un arbre d'au plus dix nœuds, profond d'un niveau en
     pratique, y entre largement.
   - **La phase 5 ne change pas de nature.** Dupliquer `semois_configuration` par magasin reste
     une migration de clé ordinaire : au plus dix fois le volume actuel, soit quelques milliers de
     lignes. Le coût est dans la reprise et les appels à adapter, pas dans la volumétrie.

**Plus aucune question ouverte.**

---

## Sources

- [Loi n° 2015-533 du 20 juillet 2015 relative à l'exercice de la pharmacie (Côte d'Ivoire)](https://www.droitci.info/files/812.03.14-Loi-n--2015-533-du-20-juillet-2015-relative-a-l-exercice-de-la-pharmacie.pdf)
- [e-Licence CEPICI — pièces exigées pour l'ouverture d'un dépôt de produits pharmaceutiques](https://elicence.cepici.ci/resultat/237eMzk1NA==/detail)
- [Ordre National des Pharmaciens de Côte d'Ivoire — Code de déontologie](https://ordrepharmacien.ci/code-de-deontologie/)
- [Ministère de la Santé / DAP — Cartographie de l'approvisionnement pharmaceutique en RCI](http://dap.ci/wp-content/uploads/2021/06/Rapport-carto-approv-RCI-VF-070617.pdf)
- [Odoo multicompany : sociétés mère et filles](https://odootricks.tips/about/building-blocks/odoo-multicompany/odoo-multicompany-parent-child/)
- [Odoo 19 — multi-société, règles inter-sociétés et reporting consolidé](https://octurasolutions.com/resources/odoo-19-multi-company-setup-intercompany-rules-and-consolidated-reporting)
- [Odoo — règles d'enregistrement et droits d'accès](https://www.odooskillz.com/blog/odoo-skillz-insights-1/odoo-security-record-rules-access-rights-tutorial-2026-382)
- [Odoo — Manage warehouses and locations](https://www.odoo.com/documentation/latest/applications/inventory_and_mrp/inventory/warehouses_storage/inventory_management/warehouses_locations.html)
- [Microsoft Dynamics 365 — Organizations and organizational hierarchies](https://learn.microsoft.com/en-us/dynamics365/fin-ops-core/fin-ops/organization-administration/organizations-organizational-hierarchies)
- [Loganconsulting — Organizational hierarchy for pharmaceutical companies with D365](https://www.loganconsulting.com/blog/planning-organizational-hierarchy-for-pharmaceutical-companies-with-dynamics-365-finance/)
- [SAP Community — Use of the profit center in a multinational pharma company](https://community.sap.com/t5/enterprise-resource-planning-blog-posts-by-members/use-of-the-profit-center-in-a-multinational-pharma-company/ba-p/13287858)
- [Comparatif des logiciels de gestion d'officine (LGO)](https://www.lonasante.com/logiciel-pharmacie/)
- [Hierarchical data in PostgreSQL : ltree vs adjacency list vs closure table](https://dev.to/dowerdev/implementing-hierarchical-data-structures-in-postgresql-ltree-vs-adjacency-list-vs-closure-table-2jpb)
