# Plan — Journal des remises, promotions et UG

## Décisions (2026-11-27)

- Le journal couvre trois natures : les **promotions**, les **remises produit** (grilles) et les
  **UG** (unités gratuites).
- Il se consulte à deux endroits :
  - un **sous-menu de « Gestion Courante »**, placé juste après « Ventes » et avant « Mouvements
    Caisse » ;
  - un **onglet du module Promotions**, qui reprend la partie promotions du journal.
- Il s'exporte en **PDF, CSV et Excel**, depuis l'écran et depuis le menu Exports
  (`features/exports`), sous deux formes :
  - l'**édition** `JOURNAL_REMISES`, mise en forme et non programmable ;
  - l'**export de données** `LIGNES_REMISEES`, une ligne par ligne de vente, programmable.

## 1. Ce que chaque nature veut dire

| Nature | Ce que c'est | Où c'est enregistré |
|---|---|---|
| Promotion | Une remise posée par le moteur : pourcentage, montant, prix fixe, seuil en montant, pack, **et les unités offertes d'une offre N+M**, qui relèvent du chantier promotion | `sales_line.promotion_id`, `promotion_libelle`, `quantite_promo`, `discount_amount` |
| Remise produit | La remise de la grille choisie sur la vente (`RemiseProduit`, code remise du produit) | `sales.remise_id`, `sales_line.taux_remise`, `discount_amount` |
| UG vendues | Les unités reçues **gratuitement du fournisseur** et vendues normalement. La cliente les paie au prix normal : ce n'est **pas** une remise faite à la cliente, c'est un CA sans coût d'achat | `sales_line.quantity_ug`, valeur `quantity_ug × regular_unit_price` |

**Les unités offertes d'une offre N+M** sont dans le journal des promotions. Chaque ligne y montre
les unités payées, les unités offertes (`quantite_promo`) et leur valeur, qui est la remise de
l'offre. La synthèse donne leur total par offre. Elles ne se confondent jamais avec les UG
fournisseur.

**Le journal des UG vendues** montre ce que les UG ont rapporté, et non un manque à gagner :
- les unités UG et leur CA TTC net. La part de remise qui les touche est
  `quantity_ug × prix × taux_remise`, comme `remise_ug_produit` dans les états ;
- leur HT net ;
- le **coût d'achat évité** (`quantity_ug × cost_amount`) ;
- la **marge des UG** : leur HT net, puisque leur coût est nul ;
- la marge de la ligne entière avec et sans la gratuité des UG. Les états actuels comptent le coût
  d'achat sur toute la quantité (`quantity_requested × cost_amount`), UG comprises. **C'est un
  défaut à corriger**, décrit dans [PLAN-COUT-ACHAT-UG.md](PLAN-COUT-ACHAT-UG.md). D'ici là, le
  journal montre l'écart, qui disparaîtra quand la correction sera faite.

Les états comptables connaissent déjà ces unités comme `montantRemiseUg`, et la déclaration de CA
peut les exclure (`exclureUg`).

### Le défaut à corriger d'abord : la part de chaque remise sur une ligne cumulée

Une ligne qui cumule une promotion cumulable et la remise produit ne garde que la **somme** des deux
dans `discount_amount`. Le moteur calcule bien les deux parts (`remiseCaissier(ligne) +
choix.remise()`), mais il ne les enregistre pas. Le journal ne pourrait donc pas dire ce qu'a coûté
la promotion et ce qu'a coûté la grille. Recalculer ces parts après coup serait faux dès qu'une
grille change.

La correction : une colonne `sales_line.remise_promotion`, remplie par le moteur quand il pose une
promotion et remise à zéro quand il l'efface. La part de la remise produit se déduit :
`discount_amount − remise_promotion`. La part des promotions de groupe (offre, pack, seuil) est le
montant que la répartition attribue à la ligne.

Pas de reprise des données (l'application n'est pas en production) : une ligne promue déjà
enregistrée, sans valeur, est imputée en entier à la promotion.

## 2. Le périmètre des ventes

Le journal retient les ventes **clôturées**, **non annulées**, du chiffre d'affaires de l'officine
(une vente dépôt est un transfert : elle n'y entre pas), du magasin d'exploitation, et datées de la
période choisie. Ce sont les mêmes règles que le bilan d'une promotion. Une vente annulée sort du
journal ; sa contre-passation aussi.

Les filtres :
- caissier, vendeur ;
- nature de la vente (comptant, carnet, assurance) ;
- promotion, origine (promotion, remise produit, cumul des deux) ;
- famille de produit.

## 3. Le backend

### 3.1 Les migrations

`V2.1.68__journal_remises_part_promotion.sql` (phase 1) ajoute la colonne
`sales_line.remise_promotion integer not null default 0`.

La migration du menu (phase 3) :
- crée l'entrée de menu `journal-remises` (niveau 2, `ROUTE`, `/journal-remises`) sous
  `gestion-courante`, à l'ordre qui suit `ventes`. Les éléments suivants (`mvt-caisse`, `exports`…)
  sont décalés d'un cran, sans valeur codée en dur.
- crée ses onglets (`SECTION`) :
  - `journal-remises.synthese` ;
  - `journal-remises.promotions` ;
  - `journal-remises.remises` ;
  - `journal-remises.ug`.
- crée l'onglet `promotions.journal` du module Promotions ;
- donne les droits (`nav_item_role`) à `ROLE_ADMIN` et `ROLE_PHARMACIEN`, export compris,
  comme `V2.1.58` et `V2.1.59` le font pour les promotions.

### 3.2 Le moteur

`MoteurPromotionServiceImpl.poser` enregistre la part de la promotion : la remise de la promotion
seule pour une promotion de ligne, la part répartie pour un groupe. L'effacement initial (étape 1 du
moteur) la remet à zéro. Le retour d'une ligne promue n'est pas touché : il rembourse déjà le net de
la ligne.

### 3.3 Le service `JournalRemisesService`

Un seul calcul, qui alimente l'écran, l'édition et l'export de données :

- `synthese(periode, filtres)` : par nature, le nombre de lignes et de tickets, les unités, le CA
  brut, le montant des remises, le CA net ; le détail par promotion (code, libellé, état, unités
  remisées, remise) et par grille de remise ; pour les UG, les unités, le CA et la remise qui les
  touche.
- `promotions(periode, filtres, page)`, `remisesProduit(...)`, `ug(...)` : le journal ligne par
  ligne. Chaque ligne donne : date et heure, ticket, caissier, vendeur, client, produit (CIP,
  libellé), quantité, prix de référence, origine, taux, montant de la remise (part promotion, part
  remise produit), net, nature de la vente.
- `exporter(format, journal, periode, filtres)` : PDF (gabarit Thymeleaf et Flying Saucer, comme
  les autres états), Excel (une feuille par partie : synthèse, promotions, remises, UG), CSV (une
  section par partie, BOM UTF-8 comme le bilan).

Les requêtes sont en JPQL sur `SalesLine` joint à `Sales`, comme `RequeteExport`, et la pagination
se fait côté serveur.

### 3.4 L'API REST

`/api/journal-remises/synthese`, `/promotions`, `/remises`, `/ug` (paginés), et `/export?format=`.
Les droits sont vérifiés par le `nav_item` `journal-remises` (lecture, export).

### 3.5 Le menu Exports

- `RapportExport.JOURNAL_REMISES`, avec :
  - la rubrique « Ventes » ;
  - les formats PDF, XLSX et CSV ;
  - les filtres : un nouveau `JOURNAL` (tout, promotions, remises, UG), plus `CAISSIER`, `VENDEUR`,
    `NATURE_VENTE` et un nouveau `PROMOTION` ;
  - les droits `journal-remises` et `promotions`.

  Il est produit par `ProducteurRapportsVentes`, qui appelle le même service.
- `ExportDonnees.LIGNES_REMISEES` (rubrique Données, programmable), avec une requête dans
  `RequeteExport` qui donne une ligne par ligne remisée ou porteuse d'UG. Ses colonnes sont : date,
  vente, CIP, produit, famille, quantité, unités UG, prix unitaire, montant TTC, part promotion,
  promotion, part remise produit, remise totale, valeur des UG, prix d'achat unitaire.

## 4. Le frontend

**Les menus suivent la structure et le style des menus existants**, sur le modèle de Mouvements de
caisse :
- une entrée `nav_item` de niveau 2 (`ROUTE`, icône `pi pi-…`), sous un libellé court comme ses
  frères (« Ventes », « Mouvements Caisse ») : **« Journal des remises »** ;
- des onglets `SECTION` de niveau 3, affichés par `app-nav-sidebar` (menu vertical repliable,
  `pharma-nav-vertical-link`, `app-nav-section-link` avec libellé et icône de repli) ;
- chaque onglet est montré selon les droits de sa section (`can_display`) ;
- chaque écran a un `app-toolbar [code]="navCode()" [compact]="true"`, `app-page-layout`,
  `app-card`, `app-data-table`, comme les autres écrans de la Gestion Courante.


- `features/journal-remises/` contient une route `/journal-remises` qui doit être déclarée dans le
  routage des entités. La page utilise `app-nav-sidebar` et les onglets `SECTION`, comme Mouvements
  de caisse : Synthèse, Promotions, Remises produit, UG.
  - Les éléments communs (période et filtres, chiffres clés, tableau paginé, boutons PDF / CSV /
    Excel) sont des composants `ui/`.
  - L'appel à l'API passe par `data-access/journal-remises-api.service.ts`.
- Le module Promotions passe à deux onglets (`app-nav-sidebar`) : la liste actuelle
  (`promotions.liste`) et `promotions.journal`, qui réutilise le journal des promotions.
- Dans `features/exports`, l'édition et l'export de données apparaissent d'eux-mêmes par le
  catalogue. Il faut seulement ajouter les codes et les deux nouveaux filtres au modèle
  (`exports.model.ts`), et leurs options.
- Les choix suivent les règles du projet : Design System maison, signaux, `@if` / `@for`, et un
  `[items]` de ng-select toujours alimenté par un signal.

## 5. Les phases

1. **Le socle** : la colonne `remise_promotion` et le moteur qui la remplit. Essais unitaires (ligne
   promue, ligne cumulée, offre N+M avec ses unités offertes, pack) et intégration.

   > **Livrée le 2026-11-27.** `V2.1.68` ajoute la colonne. `SalesLine.remisePromotion` porte la part
   > de la promotion, et `getRemiseProduit()` le reste de `discountAmount`. `MoteurPromotionServiceImpl`
   > la renseigne dans `poser` : la remise de la promotion seule sur une ligne cumulée, toute la remise
   > de la ligne pour une offre, un pack ou un seuil. `effacer` la remet à zéro. La ligne de
   > contre-passation d'une annulation la reprend en négatif (`SalesLineServiceImpl.cloneSalesLine`),
   > et la fusion de deux produits l'additionne (`ProduitMergeServiceImpl`). Essais : le moteur (ligne
   > cumulée 285 + 150, effacement, offre N+M, offre sur deux articles, pack) et l'intégration
   > (`PromotionIntegrationTest` : 380 de promotion et 200 de remise produit, puis 400 et 0 quand la
   > remise produit est retirée).
2. **Le service et l'API** : synthèse, les trois journaux, les exports PDF / Excel / CSV. Essais
   d'intégration PostgreSQL : une vente cumulée est répartie, une vente annulée et une vente dépôt
   sont exclues, les filtres s'appliquent, les unités offertes d'une offre N+M sont dans le journal
   des promotions et non dans celui des UG, les UG sont valorisées et leur marge calculée, les
   totaux des exports égalent ceux de l'écran.

   > **Livrée le 2026-11-27.** Paquet `service/remise` : `JournalRemisesService` et, dans `impl`,
   > `JournalRemisesSpecifications` (le périmètre et chaque filtre en `Specification<SalesLine>`,
   > la part de la promotion écrite une fois en Criteria), `JournalRemisesRequetes` (une requête
   > Criteria en `Tuple`, dont chaque colonne porte un nom), `LignesJournal` (les règles de calcul,
   > en un seul endroit, qui lisent les colonnes par leur nom), `TableauxJournal` (les mêmes
   > tableaux pour les trois formats), `JournalRemisesExports` (PDF par le socle des rapports,
   > `getCommonParameters` et `exportReportToPdf`, sur le gabarit `reports/journal-remises/main` ;
   > classeur et CSV à plusieurs tableaux par `ReportExcelExportService` et `CsvExportService`).
   > Les méthodes de service se nomment par un verbe : `calculerSynthese`, `listerLignes`,
   > `exporter`, `nommerFichier`. API `/api/journal-remises/synthese`,
   > `/lignes?nature=` (paginée) et `/export?format=` (droit `EXPORT`), sous les droits
   > `journal-remises` ou `promotions.journal`.
   >
   > Règles retenues :
   > - la remise des UG est la part de la remise de la ligne au prorata des unités ;
   > - la marge des UG est leur HT net ; la ligne donne aussi la marge comptabilisée (coût sur toute
   >   la quantité, cf. [PLAN-COUT-ACHAT-UG.md](PLAN-COUT-ACHAT-UG.md)) et la marge réelle ;
   > - les unités offertes d'une offre N+M sont calculées par promotion : M par groupe complet de
   >   N+M ;
   > - un ticket se reconnaît au couple jour et numéro, le numéro repartant chaque jour ;
   > - la période est obligatoire et ne dépasse pas un an (366 jours).
   >
   > La migration du menu (`V2.1.69__menu_journal_remises.sql`) est livrée en même temps, parce que
   > les droits de l'API doivent exister en base. Essais d'intégration
   > (`JournalRemisesIntegrationTest`, 15) : ligne cumulée dans les deux journaux, ligne promue
   > antérieure à la part, filtre d'origine, unités offertes d'une offre 2+1 hors UG, marge et
   > remise des UG, vente annulée et vente dépôt exclues, période, CSV, classeur, PDF, nom de
   > fichier, place du sous-menu et onglets.

3. **Le menu et l'écran** : la migration de menu (ordre et droits), la page aux quatre onglets,
   l'onglet du module Promotions. Essais de l'écran.

   > **Livrée le 2026-11-27.** Dossier `features/journal-remises`, route `/journal-remises`. Le même
   > modèle que le Retraitement du CA : `app-nav-sidebar`, un onglet par `SECTION`, chacun chargé par
   > `@defer`.
   >
   > Les onglets :
   > - **Synthèse** : un bandeau `app-kpi-strip` (remises accordées, part des promotions, part des
   >   remises produit, marge des UG), les tableaux par nature, les UG, par promotion et par grille.
   >   Quand les UG creusent un écart de marge, un avertissement en donne le montant
   >   ([PLAN-COUT-ACHAT-UG.md](PLAN-COUT-ACHAT-UG.md)).
   > - **Promotions, Remises produit, UG vendues** : un seul composant (`JournalRemisesLignesComponent`),
   >   paginé par le serveur. Son bandeau vient de la synthèse, calculée sur toute la période.
   >
   > Les filtres :
   > - période, nature de la vente et origine (sauf pour les UG) ;
   > - un état partagé (`JournalRemisesFiltresStore`), fourni par l'écran et non à la racine : la
   >   période choisie suit d'un onglet à l'autre ;
   > - ils ne s'appliquent qu'à « Afficher ».
   >
   > L'export passe par un bouton à menu (PDF, Excel, CSV), visible seulement avec le droit `EXPORT`
   > de l'onglet. Depuis la synthèse, il porte tout ; depuis un journal, ce journal seul.
   >
   > Le module Promotions devient un écran à deux onglets (`PromotionLayoutComponent`) : la liste,
   > inchangée, puis le journal des promotions, qui réutilise le même composant.
   >
   > Pas encore dans l'écran : les filtres caissier, vendeur, promotion et famille, que l'API accepte
   > déjà.
4. **Les exports dans le menu Exports** : l'édition, l'export de données, les filtres `JOURNAL` et
   `PROMOTION`. Essais du catalogue et du générateur.

   > **Livrée le 2026-11-27.**
   >
   > **L'édition `RapportExport.JOURNAL_REMISES`** :
   > - rubrique Ventes, en PDF, XLSX et CSV ;
   > - droits `journal-remises` et `promotions.journal`, comme l'API de l'écran ;
   > - produite par `ProducteurRapportsVentes`, qui appelle `JournalRemisesService.exporter` : le
   >   fichier est celui de l'écran ;
   > - réglages :
   >   - `JOURNAL` : une partie, ou rien pour la synthèse et les trois journaux ;
   >   - `NATURE_VENTE`, `CAISSIER`, `VENDEUR` ;
   >   - `PROMOTION`.
   >
   > **L'export de données `ExportDonnees.LIGNES_REMISEES`** :
   > - rubrique Données, programmable ;
   > - une ligne par ligne de vente portant une promotion, une remise ou des UG, sur le CA de
   >   l'officine comme les autres exports de données ;
   > - la part de la promotion suit la règle du journal (une ligne promue sans part enregistrée lui
   >   impute toute sa remise) ;
   > - filtres : famille, nature, vendeur, caissier, promotion, magasin.
   >
   > **Le filtre `PROMOTION`** :
   > - ses options sont les promotions non brouillons, les plus récentes d'abord ;
   > - son identifiant passe en `Long`, avec une nouvelle valeur `TypeValeurFiltre.ENTIER_LONG`.
   >
   > Le menu Exports étant piloté par le catalogue du serveur, le front n'a reçu que les nouveaux
   > codes, dans `exports.model.ts`.
   >
   > **Essais :**
   > - les essais génériques des exports de données (chaque export se lit, chaque filtre
   >   s'applique) couvrent `LIGNES_REMISEES` ;
   > - `ProducteurRapportsVentesTest` : édition complète, partie et filtres transmis ;
   > - `JournalRemisesIntegrationTest` : parts d'une ligne cumulée et valeur des UG dans l'export de
   >   données, filtre promotion, options sans brouillon.
5. **La recette** : des parcours pour le journal (consulter, filtrer, exporter), l'onglet
   Promotions, l'export depuis le menu Exports et un export de données programmé. Ils sont inscrits
   au cahier de recette.

## 6. Pièges

- **La part de la promotion sur une ligne cumulée** : sans la colonne de la phase 1, toute
  répartition affichée serait inventée.
- **Les UG ne sont pas une remise** : les additionner aux remises fausserait le « total des
  remises accordées ». La synthèse les présente à part.
- **L'ordre du menu** : il faut décaler les frères par rapport à `ventes`, et non poser une valeur
  fixe. `V2.1.42` a déjà déplacé `exports` dans Gestion Courante.
- **La TVA** : le journal montre des montants TTC. Le HT d'une ligne remisée est celui du net
  (`V2.1.67`).
- **Le magasin** : c'est celui de la vente, pas celui de l'utilisateur. La règle est la même que
  pour le bilan.
