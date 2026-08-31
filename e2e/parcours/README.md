# Parcours

Un fichier par scénario du cahier de recette, rangé par module :

```
parcours/
├── ventes/vte-01-ajout-produit.spec.ts
├── commandes/…
└── …
```

Chaque fichier se rattache à un identifiant du modèle (`cahier-recette.model.ts`) :

```ts
import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

scenario('VTE-01', async ({ etape, page }) => {
  await page.goto('/ventes');

  await etape(1, async () => { … });
  await etape(2, async () => { … });
  await etape(3, async () => { … });

  // L'assertion de §3.1 : elle traduit le `resultatAttendu` du modèle et garantit que la
  // capture montre bien ce que sa légende annonce.
  await expect(page.getByRole('row', { name: /PARACETAMOL/ })).toBeVisible();
});
```

Règles d'écriture — elles décident du coût de la phase 2 (§8.4 du plan) :

- **Sélecteurs par rôle et libellé** (`getByRole`, `getByLabel`) avant tout. Un `data-testid`
  seulement là où le libellé est ambigu ou instable — grilles AG Grid, lignes répétées.
- **Aucune temporisation fixe.** Pas de `waitForTimeout` : Playwright attend l'état stable.
  Une capture prise après un `sleep` arbitraire montre un écran à moitié chargé une fois sur cinq.
- **L'assertion se place DANS l'étape, avant la capture**, et porte sur ce que la légende
  annonce. Placée après la dernière étape, elle ne protège plus aucune image : celles-ci ont
  déjà été prises. C'est le défaut qu'a révélé le premier parcours écrit — `CLI-05` passait au
  vert en photographiant une liste non filtrée.
- **Ancrer l'assertion sur ce qui distingue l'état attendu.** « Une ligne KOUASSI existe
  quelque part » est vrai avant comme après la recherche ; « la première ligne contient
  KOUASSI » ne l'est qu'après. La seconde forme attend aussi le rafraîchissement, puisque
  `expect` réessaie — une assertion bien choisie remplace toute temporisation.
- **Parcours courts et indépendants** : sans intégration continue pendant la phase 1, un écran
  remanié ne doit en casser qu'un seul.

## Les ancrages posés dans l'application

Aucun `data-testid` à ce jour. Le seul ajout consenti est l'`inputId` d'un `app-select` ou
d'un `app-multi-select`, là où le composant n'offrait **aucune prise** : ng-select ne porte ni
rôle ni libellé accessible tant qu'on ne lui donne pas cet identifiant, qu'il pose ensuite sur
son `<input>` interne — et que `<label for>` peut alors désigner. L'ancrage sert donc autant
au parcours qu'au lecteur d'écran.

| Écran | `inputId` posé |
| --- | --- |
| Tableau de bord CA (`dashboard-ca`) | `dcaPeriode` |
| Synthèse des ventes (`sales-summary`) | `ssTypeVente` |
| Top produits (`top-products`) | `tpLimite` |
| Alertes de stock (`stock-alerts`) | `saTypeAlerte` |
| Segmentation clients (`customer-segmentation`) | `segClassification` |
| Performance fournisseurs (`supplier-performance`) | `perfFiltre` |
| Vente dépôt (`vente-depot`) | `depotSelect` |

Nommage : préfixe de l'écran, puis le rôle du filtre. Rien à inventer côté parcours —
`choisirDansSelect(page, 'dcaPeriode', '30 derniers jours')`.

## Le jeu de démonstration se complète avec les parcours

Un écran vide n'est pas une capture : il fait passer une fonctionnalité pour cassée. Chaque
fois qu'un parcours bute sur un écran désert, c'est `scripts/demo-data/` qu'on complète —
jamais l'assertion qu'on affaiblit.

Complété de cette façon :

| Manque | Ce que l'écran montrait | Correctif |
| --- | --- | --- |
| aucun inventaire | les trois onglets d'`/inventaire` vides | `11_inventaires.sql` : un clôturé avec ses écarts qualifiés, un en cours à 75 %, un planning tournant |
| aucun achat daté du jour | « Achats fournisseurs » vide sur l'accueil, qui s'ouvre pourtant sur la journée | `05_commandes.sql` : le rang le plus récent tombe sur `CURRENT_DATE`, et il est CLÔTURÉ |
| réceptions toutes vieilles de plus d'un mois | « Achats 30 jours » à zéro partout, y compris dans la performance fournisseur | statuts **entrelacés** sur les 180 jours au lieu d'être empilés par ancienneté |
| factures sans groupe de tiers payant | créances TP à zéro sur l'accueil et dans les rapports | `14_facturation.sql` recopie le groupe du tiers payant sur la facture |
| carnets couverts à 70 % | une vente carnet laissait un reste à payer, que ne pratique aucune officine | `04_clients.sql` : les contrats CARNET passent à 100 %, et le porteur n'a plus de second payeur |
| aucun stupéfiant ni psychotrope au catalogue | le refus de retour (`VTE-21`) ne pouvait pas se produire : la règle existait, aucun produit ne la déclenchait | `03_produits.sql` classe TRAMADOL en STUPEFIANTS, DIAZEPAM et BROMAZEPAM en PSO |
| aucune clé de sécurité | les autorisations superviseur (`VTE-46`) étaient injouables | `08_caisses.sql` pose la clé « 1234 » sur le compte administrateur |
| matricules tiers payant instables d'un chargement à l'autre | les parcours citaient un assuré qui, le lendemain, n'avait plus les mêmes organismes : les montants du manuel devenaient faux sans qu'aucun test ne le dise | `04_clients.sql` fige cinq **cas nommés** (un, deux ou trois organismes, carnet, plafonné) et échoue au chargement s'il n'a pas pu les poser |
| plafond calé sur la consommation générée | les montants du plafonnement changeaient à chaque chargement, donc incitables dans le manuel | `13b_plafonds.sql` fige un cas rond : CNAM plafonne à 50 000, l'assuré en a consommé 35 000 |
| 550 produits sur 600 typés `DETAIL` | `DETAIL` ne veut pas dire « vendu à l'unité » : c'est un DÉCONDITIONNÉ, l'unité issue d'une boîte, qui porte un `parent_id` et ne se commande jamais. Typés ainsi, une boîte de lait infantile ou un tube d'ARNICA étaient écartés du calcul de classe ABC (`ClassificationCriticiteService`) et des suggestions de réappro (`SemoisCalculationService`), et rangés sous le filtre « Déconditionnés » du catalogue | `03_produits.sql` : les 550 produits délivrés tels quels passent en `PACKAGE` ; seuls les 25 vrais déconditionnés gardent `DETAIL`. Deux contrôles ajoutés : tout `DETAIL` a une boîte parente, et `item_qty > 1` n'est exigé que d'un produit déconditionnable |
| causes d'écart tirées de l'`id` de la ligne | avec une poignée de lignes en écart, l'écran « Analyse des écarts » n'affichait que trois causes sur cinq | `11_inventaires.sql` répartit les causes sur un RANG, les cinq sont toujours représentées |

| aucune remise sur les ventes | « Analyse des remises » (`RPT-14`) s'ouvrait sur 199 F et une seule vente : la grille 5/10/15 % existait, aucune vente ne l'appliquait | `09_ventes.sql` remise une vente au comptant sur huit, sur toutes ses lignes ; les ventes tiers payant en sont exclues, la part payeur se calculant sur le montant conventionné |
| aucun bon d'ajustement | « Démarque & Ajustements » (`RPT-36`) était vide sur toute période : les 387 ajustements du tableau de bord sont des mouvements d'inventaire, qui ne passent pas par `ajust` | `16b_ajustements.sql` : trente bons sur six mois, sorties motivées, quelques entrées et un bon en attente, pour que le filtrage du rapport se démontre |
| aucun produit sous son seuil | le troisième compteur des alertes de stock (`RPT-18`) restait à zéro : le seuil du catalogue vaut 5 ou 10, le stock se compte en dizaines | `07_stock.sql` relève le SEUIL de quinze produits au-dessus de leur stock — on n'abaisse pas le stock, que la comptabilité à deux niveaux lie aux emplacements |
| la journée du chargement exclue un lundi sur sept | l'officine ferme le lundi ; une démonstration lancée ce jour-là n'avait ni caisse ouverte, ni vente du jour, ni tableau de bord Caissier | `08_caisses.sql` et `09_ventes.sql` gardent toujours `j = 0`, quel que soit le jour de la semaine |

Chaque complément est accompagné d'un contrôle dans `99_verification.sql` : le manque ne peut
pas revenir en silence.

**Le stock, lui, se consomme.** Les parcours de vente vendent pour de bon : à force de
rejouer, le produit qu'ils utilisent finit à zéro et l'ajout au panier échoue — un échec qui
ressemble à s'y méprendre à une régression d'interface. C'est la deuxième raison de restaurer
l'instantané avant chaque campagne, et de préférer, dans les parcours, des produits bien
approvisionnés.

Restent découverts, faute de données — les scénarios correspondants attendent leur script :

| Écran | Scénarios en attente |
| --- | --- |
| Réapprovisionnement automatique SEMOIS (« VMM · Non initialisée ») | `ACH-26`, `ACH-27` |
| Retours fournisseurs et avoirs | `ACH-49` à `ACH-59` |
| Répartition de stock rayon / réserve | `ACH-69`, `ACH-70`, `ACH-72` |
| Avoirs clients | `VTE-27` |

Complétés depuis, sur demande : **remises** (grille produit et codes remise sur les produits),
**plafonds de prise en charge** (calés sur la consommation réelle des assurés, dans
`13b_plafonds.sql` — après le calcul des consommations, sans quoi ils seraient écrasés) et
**tarifs négociés** (prix de référence et pourcentage par tiers payant).

La remise attachée au CLIENT n'a délibérément pas été alimentée : elle subsiste dans le modèle
et son écran d'administration, mais plus rien ne la lit au calcul d'une vente. Des données
qu'aucun écran n'exerce n'auraient fait qu'égarer.

## L'espace de travail vente, onglet par onglet

`/sales-home/gestion` regroupe huit onglets, et chacun a désormais ses parcours :

| Onglet | Scénarios illustrés |
| --- | --- |
| Journal des ventes | `VTE-17`, `VTE-18`, et le point de départ des retours et de la réimpression (`VTE-06`) |
| Ventes en cours | `VTE-14`, `VTE-15`, `VTE-16` |
| Pré-ventes | `VTE-07`, `VTE-08`, `VTE-09` |
| Proformas | `VTE-10`, `VTE-11`, `VTE-12`, `VTE-13` |
| Annulations | `VTE-19` |
| Ventes dépôt | `VTE-29` |
| Avoirs clients | `VTE-27`, `VTE-28` |
| Retours clients | `VTE-20` à `VTE-26` |

Deux chemins d'accès méritaient d'être notés, parce qu'ils ne sont pas là où on les cherche :

* **un retour part de la VENTE**, par le menu « Actions » du journal — l'onglet « Retours
  clients » ne fait que lister ceux déjà enregistrés ;
* **un avoir ne s'impute pas sur un ticket** : il se CLÔTURE depuis son onglet, quand le
  client repart avec ce qu'on lui devait.

Le comptoir lui-même, hors onglets : `VTE-01` à `VTE-05` (panier, remise, assurance,
encaissement), `VTE-30` (carnet), `VTE-33` et `VTE-34` à `VTE-40` (caisse), `VTE-44`,
`VTE-46`, `VTE-48` à `VTE-61` (autorisations, raccourcis clavier, règlements, différé,
création d'assuré).

## Ce que le module de vente n'illustre pas, et pourquoi

Cinq scénarios sur soixante et un restent sans parcours. Aucun n'est un oubli : chacun bute
sur quelque chose que le manuel doit savoir.

| Scénario | Obstacle |
| --- | --- |
| `VTE-31`, `VTE-32` (tableau de bord des ventes) | le composant `SalesKpiDashboardComponent` existe, l'onglet « Tableau de bord » est déclaré dans la table des libellés — mais il n'est monté NULLE PART. Et ce qu'il affiche ne recouvre pas ce que décrivent les scénarios (CA validé vs annulé, trois axes de répartition) |
| `VTE-35` (ouverture de caisse par allocation d'un responsable) | dépend d'une option de configuration et d'un second utilisateur, à poser dans le jeu de données avant d'être jouable |
| `VTE-45` (écran client) | second afficheur physique : rien à photographier dans un navigateur |
| `VTE-54` (notification email/SMS) | sort de l'écran : il faudrait observer un envoi, pas une page |

Les six autres qui figuraient ici ont été levés depuis, chacun par un travail précis plutôt
que par un contournement : le ticket Z (`VTE-39`) a rejoint les parcours de caisse, les trois
scénarios de stock insuffisant (`VTE-41` à `VTE-43`) ont reçu les ruptures qu'ils exigeaient,
le plafond de carnet (`VTE-50`) son plafond, et l'autorisation de remise (`VTE-47`) s'est
révélée passer par la `RemiseProduit`, bien vivante, et non par la `RemiseClient` abandonnée.

## Des fonctions écrites mais débranchées

Le module Administration compte 23 scénarios, dont 22 illustrés. Le vingt-troisième, `ADM-12`
(« réorganiser ses propres menus »), ne l'est pas — et pas faute de données :

* l'API `saveUserReorder` existe côté front, mais **aucun composant ne l'appelle** ; l'écran
  de réorganisation enregistre l'ordre GLOBALEMENT, pour tous les utilisateurs, et le dit
  lui-même dans son texte d'aide ;
* cet écran de réorganisation est de toute façon inaccessible : l'onglet qui le porte est
  masqué en dur (`showTabReorder = false`).

C'est pourquoi `ADM-11` n'illustre que la moitié de son scénario — la VISIBILITÉ d'un menu
par rôle, qui fonctionne — et non son ordre.

Le catalogue produits en a fourni trois autres, traités différemment selon ce qu'il en
restait :

* **substitution générique** et **tarifs par tiers payant** : écrans complets, gestionnaires
  compris, sans point d'entrée. Ils ont été RATTACHÉS — un onglet pour la première (`REF-12`),
  un bouton de ligne pour les seconds (`REF-54`) ;
* **commande rapide depuis la fiche** : fonctionnalité abandonnée, plus exploitée. Le scénario
  `REF-57` est masqué dans le cahier plutôt que supprimé, pour garder trace de ce qui a existé ;
* **classification ABC** (`REF-13`, `REF-15`) : l'API est là — métriques, distribution,
  journal des surcharges — mais AUCUN écran ne l'appelle. Les deux scénarios sont masqués : le
  guide ne promet pas ce qu'on ne peut pas ouvrir. Seul le verrouillage manuel de la classe
  (`REF-14`) a un écran, dans le formulaire produit.

## Les parcours qui écrivent remettent la base en état

Illustrer la vente, la caisse ou la création d'un utilisateur suppose de les FAIRE. Trois
règles s'appliquent alors :

- **la mise en état se fait hors des étapes.** Ouvrir la caisse pour pouvoir encaisser n'est
  pas une étape du scénario d'encaissement : c'en est le décor. On ne le photographie pas ;
- **ce qui peut être défait l'est**, après la dernière capture : `ADM-01` supprime le compte
  qu'il vient de créer, `ADM-02` rend son rôle d'origine à l'utilisateur, `VTE-01` annule son
  panier. Le parcours redevient ainsi rejouable sans restauration préalable ;
- **ce qui ne peut pas l'être est assumé.** Une vente encaissée ne s'annule pas sans laisser
  de trace ; ces parcours-là comptent sur la restauration de l'instantané avant campagne.

Corollaire : chaque parcours pose l'état dont il a besoin plutôt que de le supposer.
`VTE-05` ouvre la caisse si elle ne l'est pas, `VTE-33` la ferme d'abord — sans quoi l'ordre
alphabétique des fichiers déciderait du résultat.

## Un onglet masqué reste dans la page

Les écrans à onglets (rapports, facturation, achats) gardent les panneaux déjà ouverts dans
le DOM, simplement masqués. Deux conséquences pour les assertions :

- **un `getByRole(...).first()` peut désigner le panneau masqué** — celui du premier onglet,
  affiché à l'arrivée sur l'écran, et jamais celui qu'on vient d'ouvrir ;
- **une même colonne existe deux fois** quand deux onglets partagent leur tableau.

D'où deux réflexes : ancrer sur ce qui **n'appartient qu'à l'onglet visé** (« Panier moyen »
pour la synthèse des ventes, « Δ Rang M-1 » pour le classement des produits), et filtrer les
lignes sur leur visibilité — `page.locator('tbody tr').filter({ visible: true })` — plutôt que
de faire confiance à l'ordre du DOM.

## Le cahier de recette n'est ni exact ni exhaustif

Deux écarts se rencontrent, et se traitent tous deux dans `cahier-recette.model.ts` :

- **il décrit un écran qui a changé** → corriger le scénario ;
- **il passe une fonctionnalité sous silence** → ajouter un scénario.

Le second est le moins visible : rien ne le signale, puisqu'aucun parcours ne bute. Il se
découvre en ouvrant les écrans. Ainsi `CLI-21` — consulter l'historique des règlements
différés — a été ajouté alors que l'onglet existait depuis longtemps : le cahier ne couvrait
que l'export (`CLI-18`) et la saisie (`CLI-15`).

Plus frappant encore : **la connexion n'y figurait pas.** Le premier écran que voit tout
utilisateur, celui sans lequel aucun autre scénario n'a lieu, n'était couvert par aucune
ligne du cahier. D'où `ADM-15` (se connecter) et `ADM-16` (fermer sa session).

Même constat pour l'écran **« Gérer ma licence »**, dont le cahier ne disait pas un mot alors
qu'il porte l'abonnement de l'officine — état, échéance, modules couverts, empreinte du poste,
activation par fichier, historique. D'où `ADM-20` à `ADM-23`. Trois gestes de la grille des
autorisations manquaient de la même façon : renommer un menu (`ADM-17`), désactiver un compte
sans le supprimer (`ADM-18`), accorder ou révoquer une branche entière (`ADM-19`).

## Quand l'écran contredit le modèle, c'est le modèle qu'on corrige

Les scénarios du cahier de recette ont été rédigés avant que l'application n'atteigne son
état actuel : certains décrivent des écrans qui ont changé, ou n'ont jamais existé ainsi.
Écrire un parcours est le premier moment où la documentation est confrontée à la réalité —
et le contrôle de complétude de `scenario()` force cette confrontation, puisqu'il exige que
chaque étape du modèle soit parcourue.

**Corriger `cahier-recette.model.ts`, jamais contourner le contrôle.** Un parcours qui se
plie à une étape fausse produit une capture qui illustre autre chose que sa légende.

Corrections déjà faites de cette façon :

| Scénario | Ce que disait le modèle | Ce que montre l'écran |
| --- | --- | --- |
| `FAC-08` | « ouvrir le tiroir de recherche de factures » | il n'y a pas de tiroir : les filtres sont dans la barre d'outils de l'onglet |
| `FAC-09` | « ouvrir le panneau détail » comme étape distincte | le panneau s'ouvre à la sélection ; ce qu'il apporte, ce sont ses trois onglets |
| `REF-11` | onglet « Lots / péremption » de la fiche produit | l'onglet s'appelle « Stock » ; il classe les lots du premier expirant au dernier |
| `CPT-02` | « Déclaration TVA » calculant TVA collectée, déductible et nette | l'onglet s'appelle « Rapport TVA » et ne couvre que la collecte : rien, dans l'écran, ne touche aux achats |
| `ADM-01` | un compte créé « sans accès tant qu'aucun rôle n'est affecté » | le champ « Droits » est obligatoire : on ne crée pas de compte dont personne ne sait ce qu'il autorise |
| `ADM-02` | « affecter un ou plusieurs rôles » | l'écran n'en accepte qu'UN, et c'est lui qui ouvre les menus |
| `VTE-03` | remise produit **et** remise client, cumulables | seule la grille produit s'applique : plus rien ne lit la remise client au calcul d'une vente |
| `VTE-16` | un bouton « Supprimer » dans la liste des ventes en attente | il n'y en a pas : on reprend la vente, puis on l'annule depuis l'écran de vente |
| `VTE-30` | une vente carnet dont le client règle le reste au comptoir | le carnet couvre 100 % : « À ENCAISSER » tombe à zéro et la caisse n'est pas sollicitée |
| `VTE-28` | un avoir « imputé sur le total » d'une nouvelle vente au moment d'encaisser | rien de tel dans l'écran de vente : un avoir porte sur un PRODUIT et se CLÔTURE depuis l'onglet « Avoirs clients », après vérification du stock |
| `VTE-56` | « choisir les documents à imprimer » avant de valider | ce choix n'existe pas à la finalisation ; ce que l'écran fait, c'est REFUSER un règlement bancaire sans référence |
| `REF-03` | une « mise à jour rapide (détail) », formulaire allégé | il n'en existe pas : ce sont les interrupteurs du panneau de détail qui s'enregistrent sur-le-champ, le suivi des lots en tête |
| `REF-09` | un onglet « Indicateurs » | les indicateurs clés sont une carte de l'onglet Synthèse |
| `REF-12` | un onglet « Génériques » | c'était une fenêtre que rien n'ouvrait ; elle est devenue l'onglet que le modèle décrivait |
| `REF-14` | forcer la classe « avec un motif » | le formulaire n'en demande pas : on choisit la classe et on VERROUILLE le calcul automatique, sinon le prochain passage la reprendrait |
| `REF-43`, `REF-44` | un onglet « Lots / péremption » | les lots vivent dans l'onglet « Stock » ; la saisie manuelle est une fenêtre du menu d'actions |
| `REF-60` | confirmation « pour la mise en veille » | les deux actions groupées confirment, et la confirmation rappelle le nombre de produits |
| `VTE-44` | l'ajout d'un complémentaire, sans plus de précision | les organismes viennent de la fiche client et se chargent tous à la sélection ; la vente peut ensuite en RETIRER un et le rajouter. Et les taux ne se cascadent pas : chacun prend son pourcentage du TOTAL |

## Gestes récurrents

`src/actions.ts` encapsule les particularités du Design System qui, sinon, se paient dans
chaque parcours — et se redécouvrent à chaque fois par un délai dépassé de 90 s.

| Utilitaire | Le piège qu'il évite |
| --- | --- |
| `choisirDansSelect(page, inputId, libelle)` | l'`<input>` d'un ng-select est **invisible** : on ouvre au clavier, qui est aussi le chemin d'un utilisateur sans souris |
| `ouvrirOnglet(page, libellé)` | le texte des onglets se termine par un chevron « › » et porte parfois un compteur (« Factures 37 ») : une correspondance exacte échoue |
| `rechercher(page)` | simple raccourci désormais — voir ci-dessous |
| `saisirDate(page, id, date)` | met la date au format `jj/mm/aaaa` attendu par l'application |
| `cocherDansMultiSelect(page, inputId, libellé)` | un choix multiple **laisse la liste ouverte** : sans le `Escape`, la capture montre le menu déplié par-dessus le tableau |
| `carte(page, titre)` | les tableaux de bord **imbriquent leurs cartes** : filtrer par texte retourne aussi la carte englobante, donc la page entière |
| `saisirQuantite(page, qté)` | l'écran de vente prend le focus et écrit `1` **200 ms après** le choix du produit : saisir avant, c'est se faire écraser |
| `assurerCaisseOuverte(page)` / `assurerCaisseFermee(page)` | l'encaissement exige une caisse ouverte, l'ouverture une caisse fermée : chaque parcours pose l'état dont il a besoin plutôt que de dépendre de son rang |
| `assurerPanierVide(page)` | la vente en cours vit **côté serveur** et se rouvre au chargement suivant : sans ce nettoyage, un parcours démarre avec le panier d'un autre. En boucle, car elles s'empilent |
| `chercherProduit(page, libellé)` | un ng-select fermé n'affiche aucune suggestion, et l'écran donne parfois lui-même le focus au champ — un clic de plus referme la liste |
| `ouvrirJournalDuJour(page)` | le filtre de dates du journal est PARTAGÉ : un parcours qui remonte dans le passé le laisse dans cet état pour les suivants, qui ne trouvent plus la vente qu'ils viennent d'enregistrer |
| `chercherDansSelect(page, id, terme)` | un `app-select-search` n'a AUCUNE option tant qu'on n'a pas tapé : ouvrir la liste ne montre rien, et l'attente expire sur un composant sain |
| `seConnecterEnTantQue(page, login, mdp)` | les autorisations ne se déclenchent que pour un compte DÉPOURVU du privilège : jouées en administrateur, elles ne montreraient rien |
| `ajouterAuPanier(page, qté)` | l'écran donne le focus au champ quantité PUIS y écrit sa valeur par défaut : saisir entre les deux, c'est vendre la quantité par défaut sans le voir |

Chacun a été trouvé par un échec, en écrivant un parcours. Quand un parcours bute plus de
quelques secondes sur un composant du Design System, la cause est plus souvent de cet ordre
qu'une erreur de sélecteur : **inspecter le DOM réel avant d'ajuster à l'aveugle** — deviner
coûte 90 secondes par tentative.

### Les défauts trouvés en écrivant les parcours

Un contournement dans un parcours cache un défaut que les utilisateurs subissent aussi. Tous
ont donc été corrigés à la source, jamais ici. Ils se rangent en trois familles.

**Le Design System, découvert par un parcours qui butait :**

| Défaut | Ce qu'il coûtait à l'utilisateur | Correction |
| --- | --- | --- |
| `app-button` : sous ~1600 px, le libellé visible est réduit à une largeur **nulle** et sort du nom accessible | un lecteur d'écran annonçait « bouton », sans intitulé | `resolvedAriaLabel` retombe désormais toujours sur `label` |
| `pharma-date-picker` : le même `id` sur l'hôte **et** sur l'`<input>` | HTML invalide, `getElementById` ambigu, `<label for>` pouvant désigner l'hôte | l'attribut est retiré de l'hôte |
| `app-select` : cliquer sur la valeur déjà sélectionnée n'ouvrait pas la liste | sur un select étroit, le geste le plus naturel ne faisait rien | `.ng-value` rendu transparent au pointeur (ng-select#2669 : l'arbitrage amont convient à un champ qu'on copie, pas à un filtre) |
| pastille d'alerte de l'accueil animée **sans fin** (`pulse-danger`) | mal des transports ignoré, et un élément qui ne s'immobilise jamais — donc impossible à cliquer pour tout outil qui attend la stabilité | l'animation respecte `prefers-reduced-motion` |
| icônes décoratives sans `aria-hidden` sur l'accueil et le P&L | annoncées par les lecteurs d'écran, qui lisent une police d'icônes | 56 `<i>` marqués `aria-hidden` |
| quatre actions de ligne sans nom accessible (inventaires) | « bouton, bouton, bouton, bouton » — l'infobulle n'est pas un nom accessible | `ariaLabel` repris de l'infobulle, chevron nommé « Déplier le détail » |
| AG Grid en anglais dans une application française | « No Rows To Show », « Page 1 of 3 », menus de filtre anglais | `AG_GRID_LOCALE_FR` (`shared/ui/ag-grid`) posé sur les 7 grilles |
| modale de confirmation : le bouton « Oui » reste **inerte 200 ms** après l'ouverture | un clic rapide ne fait rien, sans le moindre retour — la garde visait un `Entrée` résiduel, pas la souris | la garde ne s'applique plus qu'au clavier |
| fonds de caisse : deux écritures différées (config serveur, `setTimeout`) écrasent la saisie | le montant tapé était remplacé par la valeur par défaut, silencieusement | le préremplissage respecte un champ déjà modifié (`control.dirty`) |
| actions de ligne sans nom accessible (utilisateurs, panier de vente, sélection client) | « bouton, bouton, bouton » | `ariaLabel` repris de l'infobulle |
| « Voulez-vous supprimer **ce** utilisateur » | faute d'accord dans une boîte de dialogue | tournure corrigée |

**Des calculs faux, que rien ne signalait à l'écran :**

| Défaut | Ce qu'il produisait | Correction |
| --- | --- | --- |
| `NB_ECHEANCES_RETARD_SQL` : `+` manquant entre une date et un intervalle | **500** sur `/api/dashboard-ca/summary-finances` ; le bloc « dettes fournisseurs / créances tiers-payants » du tableau de bord CA ne s'affichait pas, sans erreur visible | opérateur rétabli |
| filtre par rayon comparant l'id de l'ASSOCIATION `rayon_produit` à un id de rayon (3 repositories) | filtrer sur « Antibiotiques » ramenait des produits d'un autre rayon — un filtre qui ment | `rayonJoin.get(RayonProduit_.rayon).get(Rayon_.id)`, comme le faisaient déjà `CustomizedProductRepository` et `SalesLineRepository` |
| synthèse des créances : jointure **interne** sur le groupe de tiers payant | toute facture sans groupe disparaissait ; sur une officine qui n'en utilise pas, créances = 0 | jointure externe, et le tiers payant nomme la ligne à défaut de groupe |
| synthèse des créances : `montant_regle` retranché une fois **par ligne de bon** | tranches d'ancienneté NÉGATIVES | agrégation par facture avant la mise en tranches |
| `mv_supplier_performance` ne comptait que les commandes `RECEIVED` | les réceptions finalisées (`CLOSED`) — la quasi-totalité des achats — sortaient des volumes, des délais et du score | migration `V1.9.6` : `IN ('RECEIVED', 'CLOSED')` |
| classement fournisseurs trié sur douze mois, affiché sur trente jours | le premier du palmarès affichait un montant inférieur au deuxième | classement recalculé sur la période affichée (`fournisseursClasses`) |
| ventilation d'inventaire : `(ngModelChange)` déclaré **avant** `[(ngModel)]` | le regroupement partait avec la valeur précédente ; choisir « Par rayon » ne changeait rien | `(selectionChange)`, que `app-select` émet APRÈS l'affectation |
| `nav_item.actif = FALSE` employé pour masquer deux raccourcis de la barre de navigation | le droit `nouvelle-vente` disparaissait AVEC l'item : le bouton « Nouvelle vente » du journal des ventes s'est évanoui, et la route du point de vente a perdu son autorisation | migration `V1.9.7` : l'item reste actif, devient un élément intra-page et n'est plus affiché — et le service garde désormais ces items dans l'arbre même à la racine |
| privilèges de vente : le front nommait `PR_SUPPRIME_PRODUIT_VENTE`, la base `pr-supprimer-ligne-vente` | les quatre privilèges sensibles (suppression de ligne, remise, prix, annulation) ne correspondaient à AUCUN `nav_item` : le contrôle rendait toujours faux, et l'autorisation par clé échouait avec « Vous n'avez pas les autorisations » | `Authority` porte désormais le CODE du `nav_item`, seul identifiant que back et front partagent |
| aucun compte ne portait de clé de sécurité | l'écran d'autorisation était impraticable : `getUserByPwdOrSecurityKey` compare le SHA-256 de la saisie à `action_authority_key`, laissée vide partout | clé de démonstration « 1234 » posée sur `admin` par `08_caisses.sql` |
| retour client : **toute** validation renvoyait 500 | le mouvement de stock était écrit AVANT l'enregistrement du retour, avec l'identifiant de ligne encore nul (`Long.parseLong(null + "")`) ; la fenêtre restait ouverte sur « Une erreur est survenue » | les mouvements sont écrits après le `saveAndFlush`, sur des lignes qui ont leur identifiant |
| retour d'un produit thermosensible ou non conforme : 500 | le mouvement `DESTRUCTION` manquait à la contrainte CHECK de `inventory_transaction` — seuls passaient les retours remis en stock, c'est-à-dire ceux qui ne posent aucune question | migration `V1.9.9` : la liste est reconstruite depuis l'énumération `MouvementProduit` |
| règlement par carte, chèque ou virement SANS référence bancaire | le contrôle existait mais seulement sur le chemin clavier : cliquer « Finaliser » l'évitait, et la vente passait sans preuve de paiement. Le message d'erreur n'était affiché sur aucun des deux écrans concernés | le contrôle s'applique à la construction du règlement, et les trois écrans affichent son refus |
| retour avec échange : la fenêtre se refermait sans un mot | l'avoir venait d'être créé, mais ni son montant ni sa référence n'étaient annoncés — il fallait aller les chercher dans l'onglet des avoirs | le récapitulatif s'affiche aussi pour un échange |
| « Nouveau retour » : bouton grisé pour toujours | sa condition d'activation n'était alimentée par aucun champ de l'écran — une action impossible, offerte en permanence | bouton retiré : le retour part de la vente, par le menu « Actions » du journal |
| compteur `app-input-number` : deux boutons sans nom accessible | « bouton, bouton » de part et d'autre du champ, et aucun outil ne pouvait les désigner | `aria-label` « Augmenter » / « Diminuer », dérivés du libellé du champ |
| actions de ligne sans nom accessible (pré-ventes, proformas, recherche client des devis) | huit boutons muets de plus | `ariaLabel` repris de l'infobulle |
| grille des autorisations : **cocher une case enregistrait l'état PRÉCÉDENT** | `(ngModelChange)` était déclaré AVANT `[(ngModel)]`, et Angular exécute les gestionnaires dans l'ordre de déclaration : le serveur recevait la valeur d'avant le clic. Accorder ou retirer un droit ne changeait donc rien — et la case restait cochée à l'écran, sans que rien ne signale l'échec | les deux liaisons sont inversées sur les sept colonnes |
| choisir un rôle affichait « Aucun item trouvé » | même cause, sur le sélecteur de rôle : `loadRoleItems()` partait avant que `selectedRole` n'ait la nouvelle valeur. Il fallait choisir le rôle DEUX FOIS pour voir ses droits | `(selectionChange)`, émis après l'affectation |
| recherche d'un organisme par son SIGLE : « Aucun résultat » | l'API renvoyait bien la CAISSE NATIONALE DE PREVOYANCE SOCIALE pour « CNPS », mais ng-select refiltrait la réponse sur le libellé affiché, qui ne contient pas le sigle — le champ paraissait vide | un sujet `[typeahead]` **abonné** dans les quatre écrans concernés : ng-select renonce alors à son filtrage local (il exige un observateur, un sujet non abonné ne suffit pas) |
| la frappe d'une fenêtre modale nourrissait le détecteur de code-barres | saisir vite un matricule dans un formulaire déclenchait « Produit non trouvé : CNPS01… » et un bip d'erreur, par-dessus le formulaire | la frappe venant d'une modale ne va plus au scanner (`sales-home` et les trois écouteurs `document`) |
| `[items]` d'un select alimenté par une MÉTHODE (ajout de tiers payant complémentaire) | ng-select compare ses options par référence : un tableau neuf à chaque cycle rend la liste inerte, sans erreur | la liste est calculée une fois dans un signal |
| boutons du bandeau assuré sans nom accessible (retrait et ajout d'un tiers payant, ayant droit) | « bouton, bouton » au lecteur d'écran — l'infobulle n'est pas un nom accessible | `ariaLabel` repris de l'infobulle sur les six boutons |
| vendeur et caissier détenaient les trois privilèges sensibles | la demande d'autorisation ne se déclenchait jamais : le geste passait sans trace, à l'inverse de ce que le contrôle de caisse attend | migration `V1.9.8` : ces privilèges reviennent à l'administrateur et au pharmacien titulaire |
| plafond tiers payant : la consommation du CLIENT était comparée au plafond de l'ORGANISME | le premier se compte en dizaines de milliers, le second en dizaines de millions : **aucun plafonnement ne s'est jamais déclenché**, et la part patient était sous-évaluée d'autant | `ThirdPartyCalculationManagerImpl` lit `getPlafondConsoClient()`, comme le fait déjà `SaleDataService` pour le même champ |
| liste des utilisateurs : les comptes DÉSACTIVÉS n'y figuraient pas | la désactivation était donc sans retour possible par l'interface — le compte disparaissait de l'écran qui aurait permis de le réactiver, et il fallait passer par la base | la spécialisation de l'écran d'administration ne filtre plus sur `activated` ; celle du reste de l'application, si |
| renommer un menu : le TITRE LONG s'effaçait à la validation par `Entrée` | la touche déclenchait l'enregistrement, puis la perte du focus en déclenchait un second — le second partait avec un champ déjà refermé, donc vide | chaque enregistrement vérifie qu'il concerne bien le champ encore en cours d'édition |
| ventes en attente : la liste n'était chargée qu'UNE FOIS, à la construction de l'écran | une vente garée après la première ouverture du panneau n'y apparaissait jamais : le compteur du bouton affichait « 1 » au-dessus de « Aucune vente en attente », et il fallait deviner « Actualiser » | la liste n'est construite que pendant que le tiroir est ouvert, et se recharge donc à chaque ouverture |
| ventes en attente : la liste filtrait sur le VENDEUR, le compteur comptait celles du CAISSIER | dès qu'on encaisse pour un autre vendeur, les deux se contredisaient sans rien pour l'expliquer | la liste s'ouvre sur le caissier, comme le compteur ; le filtre par vendeur reste offert au-dessus |
| deux boutons « En attente » homonymes sur l'écran de vente | garer la vente et voir les ventes garées portaient le même nom : au lecteur d'écran comme à l'automate, seul l'ordre d'apparition les distinguait — et il change avec le compteur | le bouton de la barre d'outils porte son propre nom accessible, « Voir les ventes en attente » |
| titre de barre d'outils rendu en `<span>` | AUCUN écran de l'application n'offrait de titre à la navigation par en-têtes, sur laquelle repose la lecture d'écran | le composant `app-toolbar` rend un `<h1>` ; le style suit la classe, l'apparence ne bouge pas |
| `app-checkbox` et `app-switch` sans nom accessible possible | les cases de sélection d'un tableau et les interrupteurs d'un panneau n'ont pas de libellé visible : « case à cocher » répété autant de fois qu'il y a de lignes | une entrée `ariaLabel` sur les deux composants, posée sur le catalogue produits, la synthèse, les rayons, les fournisseurs et les tarifs |
| mise en veille en masse : échec SILENCIEUX | le décompte n'attendait que les succès — un seul appel en erreur laissait la sélection et la barre d'actions ouvertes, sans message, et l'utilisateur croyait le traitement en cours | le décompte porte sur les réponses reçues, succès et échecs, et les produits en échec sont nommés |
| substitution générique : écran écrit, jamais ouvert | la liste des équivalents d'un produit existait avec son gestionnaire, mais aucune entrée de menu ne la déclenchait — la fonction était inatteignable | elle devient un ONGLET du panneau de détail, après Rayons (REF-12) |
| tarifs d'un tiers payant : la moitié d'un écran inatteignable | le composant sait se lire par produit ou par organisme ; seul le sens « par produit » avait un point d'entrée, et la question d'une négociation annuelle — sur quoi ce payeur tarife-t-il ? — restait sans écran | un bouton « Tarifs produits négociés » sur la liste des tiers payants (REF-54) |
| lot saisi HORS COMMANDE, invisible partout | `LotDTO.toEntity()` le crée `IN_PROGRESS` — l'état d'un lot annoncé sur une commande et pas encore reçu — alors que les écrans ne listent que les lots `AVAILABLE`. La saisie répondait 200, fermait sa fenêtre, et le lot n'apparaissait ni dans l'onglet Stock, ni en FEFO, ni dans les péremptions | un lot hors commande porte sur du stock déjà présent : il est posé `AVAILABLE` |
| deuxième tarif assurance sur le même couple : **500 muet** | la contrainte d'unicité (produit, tiers payant) remontait en erreur serveur brute ; la fenêtre restait ouverte, rien ne s'enregistrait, et rien ne le disait | le service refuse le doublon par une erreur métier nommée, que le formulaire affiche |
| devise « F » écrite en dur dans les gabarits | le symbole ne suivait pas la devise configurée de l'officine | les huit occurrences du module produits passent par le pipe `devise` ; il en reste une cinquantaine ailleurs |
| la DCI absente de la fiche produit | le serveur l'envoyait, l'interface TypeScript ne la déclarait pas : la molécule — ce qui commande la substitution — ne s'affichait nulle part | `dciLibelle` ajouté au modèle et affiché en tête de l'Identification |
| onglet Synthèse : une colonne, 25 indicateurs sur deux lignes chacun | le panneau occupe 62 % de l'écran et en laissait les deux tiers vides, tout en imposant de faire défiler pour atteindre la réglementation | cartes pavées en flex, indicateur sur une seule ligne : 839 px → 524 px, sans valeur tronquée |
| import d'une suggestion : « Chargement des lignes… » affiché **dix secondes** après l'arrivée des lignes | le composant est en `OnPush` et le cache des lignes était un champ ordinaire : la réponse HTTP n'appartient à aucun évènement du gabarit, elle ne marque donc pas la vue à rafraîchir. Les lignes étaient là, le voyant tournait jusqu'au clic suivant | le cache et l'indicateur de chargement passent en SIGNAUX : leur mise à jour marque la vue d'elle-même — mesuré à 213 ms |
| import d'une proposition dans une commande : « **Fournisseur différent** » sur TOUTES les propositions | la commande est adressée à une AGENCE (Laborex Cocody), la proposition appartient au GROSSISTE (Laborex-CI) : comparer les deux identifiants tels quels condamnait précisément les propositions qu'on voulait importer | la comparaison se fait au niveau du principal, et le filtre du modal ne liste plus que les grossistes — une agence ne porte aucune proposition |
| suivi d'un envoi PharmaML introuvable | le panneau — statut, disponibilités annoncées, substitutions proposées, historique des envois — existait avec sa colonne réservée dans le gabarit, mais n'était monté nulle part | il est rattaché à la commande dès qu'elle est transmise (ACH-29, ACH-31) |
| rien ne distinguait une commande déjà transmise dans la liste | on ouvrait la commande pour découvrir son bandeau « Soumise via PharmaML — modifications désactivées » | une pastille PHARMAML sur la ligne, à côté de RELIQUAT |
| « Comparer multi-grossistes » et « Vérifier la disponibilité » consultaient — en apparence | le message émis était une `COMMANDE` : chez GESCOM 3.41.06, la nature d'action `REQ_INFORMATION` et le corps `REQ_INFOS` de la norme sont tous deux refusés, et la seule requête qui aboutit passe commande pour de vrai | les points d'entrée sont masqués (`COMPARAISON_DISPONIBILITE_ACTIVE`), ACH-34 et ACH-35 passent en `hidden` — plus personne ne commande en croyant consulter |

**Quatre fautes de frappe visibles à l'écran et dans les PDF** : « Pamier.Moyen » et
« Chiffres d'affaitre », corrigées dans le gabarit Angular ET dans le modèle Thymeleaf ;
« Prix dé référence » dans la liste des tarifs assurance ; « Enrégistrer » sur l'import de
produits.

Le point à retenir : **la plupart de ces défauts ne provoquaient
aucune erreur.** Un zéro, un classement dans le désordre, un filtre qui ramène autre chose,
un bouton qui ne répond pas — rien qui attire l'œil, tout ce qu'un manuel aurait figé en image.

