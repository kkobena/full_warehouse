# Plan — sous-menus `pharma-nav-sidebar` pilotés par la base

État au 22 août 2026.

## Le constat : la donnée existe déjà, elle est simplement recopiée

Les 19 écrans à sous-menu vertical écrivent en dur, dans leur gabarit, un libellé et une icône qui
sont **déjà en base** et **déjà servis par l'API**.

Comptabilité, côté base (`V1.7.8__nav_comptabilite_module.sql`) :

```sql
'comptabilite.balance', 'Balance caisse', 'pi pi-calculator', …, 10, 3, 'SECTION'
```

Le même écran, côté gabarit :

```html
<ng-container ngbNavItem="balance">
  <a class="pharma-nav-vertical-link" ngbNavLink>
    <i class="pi pi-calculator"></i>
    <span>Balance caisse</span>
```

Et côté composant :

```ts
protected readonly showBalance = this.ability.canSignal('display', 'comptabilite.balance');
```

Trois informations sur quatre viennent donc déjà de la base — le **code** sert d'identifiant de
permission, l'**ordre** et le **rattachement** structurent l'arbre. Seuls le **libellé** et
l'**icône** sont recopiés à la main, à côté d'une valeur qui les contredira au premier changement.

**Le pharmacien qui renomme « Balance caisse » depuis l'écran d'administration ne verra rien
changer** dans le sous-menu : le libellé édité vit en base, celui qui s'affiche est dans le HTML
compilé.

### Ce qui est déjà acquis

| brique | état |
|---|---|
| Entité `NavItem` (`libelle`, `icon`, `code`, `ordre`, `requiredFeature`) | en place |
| `targetType = SECTION` pour les onglets intra-page | en place |
| `NavItemServiceImpl` renvoie les `SECTION` **même** `canDisplay = false` — pour distinguer « non configuré » de « interdit » | en place |
| `NavStore.navTree()` — arbre complet en signal | en place |
| `AbilityService.canSignal('display', code)` | en place, déjà utilisé par les 19 écrans |
| Édition du libellé : `PATCH /api/admin/nav/items/{id}/libelle` + écran `nav-manager` | en place |
| Filtrage par module de licence (`requiredFeature`) | en place |

### Ce qui manque

1. **Libellé et icône dupliqués** dans 19 gabarits, pour environ 95 onglets.
2. **L'icône n'est pas éditable** — seul le libellé l'est.
3. **L'ordre de la base est ignoré** : les onglets s'affichent dans l'ordre du HTML. Réordonner
   dans `nav-manager` ne déplace rien.
4. **Aucun composant partagé** : la structure `pharma-nav-sidebar` → `-card` → `-header` →
   `-content` est recopiée 19 fois, avec ses variantes.

## Avancement

| écran | état |
|---|---|
| `comptabilite-layout` | **fait** — `<app-nav-sidebar>`, libellés dynamiques, repli. Sert de référence. |
| `sales-home` | **hors périmètre**, décision du 22 août 2026. Son menu a des besoins propres — titre déjà dynamique selon le mode, classes `sales-sidebar`/`sale-type-link`, thème conditionnel — que le composant générique ne couvre pas. C'est de lui qu'est repris le mécanisme de repli. |
| les 17 autres | à faire |

Acquis au passage :

- `app-nav-sidebar` — enveloppe (grille, carte, en-tête, outlet), avec repli optionnel
  (`collapsible` / `collapsed` bidirectionnel).
- `app-nav-section-link` — icône et libellé lus dans `NavStore`, repli sur les valeurs du gabarit.
- `NavStore.node(code)` — index par code, reconstruit à chaque changement d'arbre.
- 23 tests sur ce périmètre, dont ceux qui verrouillent la projection des `ngbNavItem` et la
  lecture du libellé en base.

## Volumétrie

| écran | onglets |
|---|---:|
| `declaration-ca-layout`, `sales-reports` | 12 |
| `finance-reports` | 10 |
| `facturation-layout` | 9 |
| `sales-management-home` | 8 |
| `comptabilite-layout`, `finances-layout`, `commande-home`, `stock-reports` | 5 |
| `depot-home` | 4 |
| `sales-home`, `inventory-home` | 3 |
| 7 autres écrans | 2 |

**19 gabarits, ~95 onglets.**

## Le nœud du problème : le contenu ne peut pas venir de la base

C'est ce qui interdit un simple `@for (section of sections())`. Chaque onglet porte un
`<ng-template ngbNavContent>` qui instancie **un composant Angular précis**, souvent dans un bloc
`@defer` :

```html
<ng-template ngbNavContent>
  @defer (on immediate) { <app-balance-mvt-caisse /> } @loading { <app-skeleton … /> }
</ng-template>
```

Ni le composant ni le bloc `@defer` ne se déduisent d'une chaîne en base — et il ne faut pas
chercher à les y mettre : `@defer` est résolu à la compilation, c'est précisément ce qui découpe le
bundle. Un registre `code → composant` côté front annulerait ce découpage.

**La bonne coupure est donc : la présentation vient de la base, le contenu reste dans le gabarit.**
L'onglet est déclaré en HTML comme aujourd'hui — il porte son `ngbNavItem`, son `ngbNavContent` et
son `@defer` — mais son libellé, son icône, son ordre et sa visibilité viennent du `NavStore`.

## Étape 1 — Un composant de lien, alimenté par le code

Le plus petit pas utile, et celui qui règle à lui seul les points 1 et 2.

```html
<ng-container ngbNavItem="balance">
  <a ngbNavLink appNavSectionLink code="comptabilite.balance"></a>
  <ng-template ngbNavContent> … inchangé … </ng-template>
</ng-container>
```

La directive lit le nœud correspondant dans le `NavStore` et rend l'icône, le libellé et le
chevron. Le gabarit ne connaît plus que le **code** — la seule information qui n'a pas vocation à
changer, puisque c'est elle qui porte les permissions.

**Repli obligatoire.** Si le code est absent de l'arbre — item désactivé, base d'une installation
plus ancienne, arbre pas encore chargé —, la directive doit rendre un libellé de secours fourni par
le gabarit plutôt qu'un lien vide :

```html
<a ngbNavLink appNavSectionLink code="comptabilite.balance" fallbackLabel="Balance caisse"
   fallbackIcon="pi pi-calculator"></a>
```

C'est verbeux, et c'est voulu : un menu vide est un écran inutilisable, alors qu'un libellé périmé
reste exploitable. Ce repli disparaîtra quand les données seront fiables sur toutes les
installations.

**Coût** : une directive, plus une ligne modifiée par onglet — ~95 lignes, mécaniques.

## Étape 1 bis — Le titre de la barre d'outils

Le libellé d'un `nav_item` est affiché à **deux** endroits : dans le lien du sous-menu, et dans la
barre d'outils de l'écran ouvert. Le second a été oublié au premier recensement.

```html
<!-- l'onglet, dans le layout -->            <!-- l'écran ouvert, dans son propre gabarit -->
<span>Historique des ponctions</span>        <app-toolbar title="Historique des ponctions" />
```

93 `<app-toolbar>` portent un titre dans le dépôt, dont une poignée déjà dynamique
(`[title]="titre()"`, alimenté par une entrée du composant).

### La duplication a déjà dérivé

Relevé sur les douze onglets de `declaration-ca`, où les deux valeurs devraient coïncider :

| code | libellé en base | titre de la barre | |
|---|---|---|---|
| `audit` | Contrôle de cohérence | Contrôle de cohérence | identique |
| `parametres` | Unités gratuites | Unités gratuites | identique |
| `ponction-historique` | Historique des ponctions | Historique des ponctions | identique |
| `journal-tp` | Ventes tiers-payant exclues | Ventes tiers-payant exclues | identique |
| `journal-ug` | Unites gratuites vendues | Unités gratuites **vendues** | **accents perdus en base** |
| `ponction` | Ponction | Ponction **du chiffre d'affaires** | **volontairement différent** |

Deux enseignements opposés, et c'est ce qui rend la question intéressante.

**La dérive est réelle** : `journal-ug` a perdu ses accents en base, personne ne l'a vu — parce que
ce libellé ne s'affiche nulle part aujourd'hui. Dès l'étape 1, il apparaîtra dans le menu, sans
accents, à côté d'une barre d'outils qui les a. Ce n'est pas un argument contre le chantier, c'est
un argument pour : la copie muette pourrit sans bruit.

**Mais les deux libellés n'ont pas le même métier.** Le menu vit dans une colonne étroite, qui se
replie jusqu'à n'afficher qu'une icône : il doit être court. La barre d'outils dispose de toute la
largeur et peut nommer précisément ce qu'on regarde. « Ponction » et « Ponction du chiffre
d'affaires » ne sont pas une négligence, c'est une adaptation au contexte.

### Trois voies

- **A — Un seul libellé, la barre suit le menu.** `app-toolbar` reçoit un `code`, lit le `NavStore`
  et retombe sur son `title` en dur si le code est absent — exactement le contrat de
  `app-nav-section-link`. Simple, cohérent, mais raccourcit les titres d'écran là où ils étaient
  volontairement plus explicites.
- **B — Deux colonnes en base.** `libelle` pour le menu, un nouveau `titre_long` pour la barre,
  ce dernier retombant sur le premier s'il est vide. Respecte la différence de métier, au prix
  d'une migration, d'un champ de plus dans l'écran d'administration, et d'une question à trancher
  pour les 93 barres d'outils.
- **C — Au cas par cas.** `app-toolbar` accepte un `code` **optionnel** : on le renseigne là où les
  deux libellés coïncident, on garde le titre en dur là où ils divergent. Aucun changement de
  schéma, et la divergence reste possible quand elle est voulue.

**C est recommandé** — c'est aussi le mécanisme le moins coûteux à défaire si l'usage montre que B
s'impose. Sur `declaration-ca`, il couvrirait dix onglets sur douze ; les deux autres garderaient
leur titre propre, avec un commentaire disant pourquoi.

### Quel que soit le choix

Corriger `'Unites gratuites vendues'` en base par une migration : c'est une faute de saisie, pas
une divergence assumée.

## Étape 2 — Rendre l'icône éditable

Aujourd'hui `nav-manager` n'édite que le libellé. Sans cette étape, l'icône reste figée en base à
sa valeur d'installation, ce qui est déjà mieux que le HTML mais pas encore éditable.

- Backend : `PATCH /api/admin/nav/items/{id}` acceptant `{ libelle, icon }`, plutôt qu'un second
  endpoint dédié à l'icône — l'existant `/libelle` reste, déprécié.
- Front : un sélecteur d'icônes dans `nav-manager`. La liste des icônes valides est déjà connue :
  `navigation.service.ts` porte la table `primeIconToFa`, qui **est** la liste des icônes rendues
  correctement. Une icône hors de cette table ne s'affiche pas dans la barre principale ; l'écran
  d'administration ne doit donc proposer que celles-là.

**À décider** : faut-il aussi valider côté serveur ? Une icône inconnue saisie en base ne casse
rien (elle ne s'affiche pas), mais elle est indétectable sans ouvrir l'écran.

## Étape 3 — Respecter l'ordre de la base

Une fois les libellés dynamiques, l'ordre reste le dernier écart entre ce que montre `nav-manager`
et ce que voit l'utilisateur.

`ngbNav` affiche les `ngbNavItem` dans l'ordre du DOM. Deux voies :

- **A — Trier le DOM.** Envelopper les onglets dans un `@for` sur les sections triées, chaque
  itération projetant le bon contenu via un `ng-template` nommé. Faisable mais lourd : chaque
  gabarit devient un dictionnaire `code → template`, et la lisibilité y perd beaucoup.
- **B — Ordonner en CSS.** `.pharma-nav-sidebar-content` est un conteneur flex ; la directive de
  l'étape 1 connaît l'`ordre` du nœud et peut poser `style.order`. Une ligne, aucun changement de
  structure, et l'ordre suit la base.

**B est recommandé.** Sa limite : `order` ne déplace que l'affichage, pas l'ordre de tabulation au
clavier, qui reste celui du DOM. Sur un menu de 5 à 12 entrées, l'écart est acceptable ; il ne le
serait pas sur une liste longue.

## Étape 4 — `<app-nav-sidebar>` dans le Design System

C'est la partie « composant UI » de la demande. Elle est **indépendante** des trois précédentes et
peut se faire avant, après, ou jamais.

### Ce que le composant encapsule

```html
<app-nav-sidebar icon="pi pi-calculator" title="Comptabilité" [(active)]="active">
  <ng-container ngbNavItem="balance"> … </ng-container>
  <ng-container ngbNavItem="taxe-report"> … </ng-container>
</app-nav-sidebar>
```

Il porte la colonne (`col-lg-2 …`), la carte, l'en-tête, et le `[ngbNav]` avec ses options
verticales. Les onglets restent projetés — c'est la contrainte du chapitre précédent.

**Gain réel** : quatre niveaux de `<div>` et six classes disparaissent de chaque gabarit, la
largeur de colonne cesse de varier d'un écran à l'autre (aujourd'hui `col-lg-2` ici, autre chose
ailleurs), et les styles quittent `content/scss/pharma-nav-global.scss` pour l'encapsulation du
composant.

**Difficulté à ne pas sous-estimer** : projeter des `ngbNavItem` à travers un composant. La
directive `ngbNav` doit voir ses `ngbNavItem` comme enfants de contenu — à vérifier sur **un seul
écran** avant de s'engager sur les 19. Si ng-bootstrap ne le permet pas proprement, le repli est un
composant qui ne prend que l'enveloppe (colonne, carte, en-tête) et laisse le `[ngbNav]` dans le
gabarit appelant : moins élégant, mais sans risque.

### Ordre de déploiement

`comptabilite-layout` en premier — 5 onglets, structure canonique, et il sert déjà de référence
dans ce document. Puis les écrans à 2 onglets pour valider les cas dégénérés, puis les gros.

## Séquence recommandée

1. **Étape 4 sur un seul écran** (`comptabilite-layout`), pour lever le doute sur la projection des
   `ngbNavItem`. C'est le seul risque technique du plan ; le lever d'abord évite de bâtir sur une
   hypothèse.
2. **Étape 1** — directive de lien, déployée sur les 19 écrans. C'est elle qui apporte le bénéfice
   demandé : les libellés édités deviennent visibles.
3. **Étape 2** — édition de l'icône, qui n'a de sens qu'une fois l'étape 1 en place.
4. **Étape 3** — ordre, le raffinement le moins urgent.
5. **Étape 4 sur les 18 autres écrans**, une fois le geste rodé.

## Ce que ce plan ne traite pas

- **La barre principale** (`sidebar`/`navbar`) : elle est déjà entièrement dynamique, c'est
  justement ce qui rend l'écart visible.
- **Les libellés des onglets qui n'existent pas en base.** Une passe de vérification est nécessaire
  avant l'étape 1 : chaque `ngbNavItem` doit avoir sa `SECTION`. Les migrations `V1.4.8`, `V1.7.8`
  et `V1.9.2` en ont créé beaucoup, pas forcément toutes. Les manquantes demandent une migration.
- **La traduction.** `libelle` est une colonne unique : rendre les libellés dynamiques ferme la
  porte à `ngx-translate` sur ces entrées. Ce n'est pas une régression — ils sont déjà en français
  en dur — mais c'est une décision à assumer.
- **Le cache.** `NavItem` est en cache de second niveau Hibernate (`@Cache READ_WRITE`). Une
  édition de libellé doit l'invalider, sans quoi le changement n'apparaîtra qu'au redémarrage. À
  vérifier sur l'endpoint existant, qui a peut-être déjà le défaut.
