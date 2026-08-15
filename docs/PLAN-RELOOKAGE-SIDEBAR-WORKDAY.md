# Relookage du menu latéral : rail d'icônes + flyout de sous-menu

**Statut : implémenté** — lots L0 à L5 livrés le 15/08/2026
**Périmètre :** `pharmaSmart-app/src/main/webapp/app/layouts/sidebar/`

> Ce document a d'abord été un plan. Il décrit désormais **ce qui a été livré**.
> Les écarts entre le plan initial et l'implémentation sont consignés au §9 —
> ils sont volontaires et motivés.

---

## 1. Le pattern retenu

Vocabulaire, pour lever l'ambiguïté du mot « popup » :

| Nom | Ce que c'est |
| --- | --- |
| Dropdown | Petite liste verticale sous le déclencheur |
| **Flyout** | Carte flottante qui sort **latéralement** du déclencheur |
| Mega-menu | Grande carte, contenu en **grille** de tuiles |
| Drawer | Panneau pleine hauteur qui **glisse** depuis un bord |
| Modal | Boîte centrée qui **bloque** le reste de l'écran |

**Retenu : un flyout à un seul niveau, en liste.** Le rail d'icônes reste visible en permanence ;
un clic sur une entrée ouvre une carte flottante à sa droite, contenant la liste des sous-entrées.
Pas de seconde colonne en cascade, pas de grille de tuiles.

```
  RAIL           PANNEAU
 ┌────┐  ┌──────────────────────┐
 │ 🏠 │  │ 🛒 Ventes        (12)│  ← en-tête : icône + libellé + badge cumulé
 │ 🛒◀┼──┤ ──────────────────── │
 │ 📦 │  │ GESTION              │  ← groupLabel
 │ 👥 │  │ 🛒  Nouvelle vente   │
 │ 📊 │  │ 📋  Ventes du jour   │
 │ ⚙  │  │ ──────────────────── │  ← divider
 └────┘  │ SUIVI                │
         │ 📊  Statistiques     │
         │ ↩   Avoirs         3 │  ← badge par ligne
         │ 🕐  Historique       │
         └──────────────────────┘
```

Un seul panneau ouvert à la fois. Il se ferme au clic extérieur, sur `Escape`, et à la navigation.

---

## 2. Ce qui existait avant

### 2.1 Comportement d'origine

Le composant avait **deux modes** pilotés par `layoutService.sidebarCollapsed()` :

- **Sidebar étendue (280 px)** — clic sur un parent → sous-menu en **accordéon inline**, qui
  poussait le reste de la liste vers le bas.
- **Sidebar réduite (70 px)** — **survol** d'un parent → flyout à droite, en
  `position: absolute; left: 70px; top: 0`.

Le flyout visé existait donc déjà à moitié : la cible n'était pas une invention, c'était le mode
réduit généralisé et corrigé.

### 2.2 Défauts corrigés

| # | Défaut | Corrigé par |
| --- | --- | --- |
| 1 | Deux blocs de balisage quasi identiques (~35 l. chacun), avec une incohérence : `onSubmenuItemClick(child.click)` d'un côté, `child.click()` brut de l'autre | Gabarit unique dans `sidebar-flyout` (L1/L2) |
| 2 | **Flyout clippé** : `position: absolute` dans une `.sidebar` en `overflow: hidden`, avec `.sidebar-nav` en `overflow-y: auto` — coupé dès que le parent était bas dans la liste | CDK Overlay, rendu en fin de `<body>` (L2) |
| 3 | Flyout au **survol uniquement** — inutilisable au clavier et au tactile | Clic comme déclencheur principal (L2), clavier complet (L3) |
| 4 | `top: 0` en dur — aucun repositionnement en cas de débordement bas | 3 positions de repli CDK (L2) |
| 5 | Accordéon inline coûteux en hauteur sur un arbre dynamique | Supprimé (L2) |
| 6 | `onMenuItemClick()` **repliait la sidebar** après toute navigation | Repli réservé au mobile (L4) |
| 7 | `onParentMenuClick()` dépliait toute la sidebar en mode réduit | Le clic ouvre le panneau, point (L2) |
| 8 | Gabarit limité à 2 niveaux alors que `mapNodesToNavItems` est récursif | 3ᵉ niveau aplati (L1) |
| 9 | `expandedItems: Set<string>` non réactif | `openMenuId` en signal (L2) |
| 10 | État clé par **libellé** — menus homonymes confondus | Champ `id` sur `NavItem` (L0) |
| 11 | Aucune fermeture au clic extérieur ni sur `Escape` hors mobile | `overlayOutsideClick` + `Escape` (L2/L3) |
| 12 | Palette figée en dur | Tokens CSS dans le flyout (L1) — voir §9.5 |

---

## 3. Architecture livrée

### 3.1 Fichiers

```
layouts/sidebar/
├── sidebar.component.{ts,html,scss}          # rail seul, ~145 l. de SCSS mort supprimées
├── sidebar.component.spec.ts                 # L5
└── sidebar-flyout/
    ├── sidebar-flyout.component.{ts,html,scss}
    ├── sidebar-flyout.component.spec.ts      # L5
    └── flyout-item.directive.ts              # option navigable au clavier
```

Fichiers touchés hors du dossier :

| Fichier | Modification |
| --- | --- |
| `layouts/navbar/navbar-item.model.ts` | Champ `id` + helper `navItemIdFromLabel()` |
| `core/config/navigation.service.ts` | Alimente `id` depuis `n.code` du `NavStore` |
| `layouts/navbar/navbar.component.{ts,html}` | `id` sur les littéraux, `track item.id` |
| `core/config/layout.service.{ts,spec.ts}` | Rail réduit par défaut |
| `content/scss/global.scss` | `z-index` du conteneur d'overlay CDK |
| `angular.json` | Feuilles `overlay-prebuilt.css` et `a11y-prebuilt.css` |

### 3.2 Sélecteurs

- Composant panneau : **`app-sidebar-flyout`**
- Directive d'item : **`appFlyoutItem`**
- Le rail est passé de `jhi-sidebar` à **`app-sidebar`**. Sans impact : il est monté par un
  `router-outlet` nommé (`main.component.html`), son sélecteur n'apparaît dans aucun gabarit.

### 3.3 Le composant panneau

`SidebarFlyoutComponent` est **présentationnel** et `OnPush` : il reçoit `item`, `autoFocus` et
`fullscreen` en entrée, émet `navigate` et `close`. Aucun accès au routeur ni au store — testable
seul, réutilisable par la navbar horizontale si on veut homogénéiser plus tard.

Un `computed` aplatit l'arbre en `FlyoutRow[]` :

```ts
export type FlyoutRow =
  | { kind: 'divider'; key: string }
  | { kind: 'group';   key: string; label: string }
  | { kind: 'link';    key: string; item: NavItem };
```

Un enfant porteur de `children` devient un intertitre suivi de ses propres enfants : le 3ᵉ niveau
publié par le back-office n'est pas perdu, et aucune cascade n'est introduite.

### 3.4 Ancrage — CDK Overlay

`@angular/cdk` était déjà une dépendance, mais **aucun overlay CDK n'était utilisé dans
l'application** : les feuilles de style du CDK n'étaient pas chargées. Deux ajouts ont été
nécessaires (§9.3).

```ts
readonly flyoutPositions: ConnectedPosition[] = [
  { originX: "end",   originY: "top",    overlayX: "start", overlayY: "top",    offsetX: 8 },
  { originX: "end",   originY: "bottom", overlayX: "start", overlayY: "bottom", offsetX: 8 },
  { originX: "start", originY: "top",    overlayX: "end",   overlayY: "top",    offsetX: -8 },
];
```

Le panneau est rendu dans le conteneur d'overlay en fin de `<body>` : le clipping du §2.2-2
disparaît, et `cdkConnectedOverlayFlexibleDimensions` + `cdkConnectedOverlayPush` règlent le
débordement bas.

**Pas de backdrop** : un clic sur une autre entrée du rail bascule ainsi directement de menu.
En contrepartie, `overlayOutsideClick` doit ignorer les clics portés par un déclencheur — sans
cela le panneau se fermerait puis se rouvrirait aussitôt via `toggleMenu`, et ne se fermerait
jamais :

```ts
protected onOverlayOutsideClick(event: MouseEvent): void {
  const target = event.target as HTMLElement | null;
  if (target?.closest("[data-flyout-trigger]")) { return; }
  this.closeMenu();
}
```

> **Pourquoi pas `NgbDropdown` ?** ng-bootstrap impose sa structure `.dropdown-menu` et ses styles
> d'items — on se serait battu contre lui pour l'en-tête, les groupes et les badges. Le CDK ne
> fournit que le positionnement, ce qu'on voulait ici.

### 3.5 État d'ouverture

```ts
readonly openMenuId = signal<string | null>(null);
readonly openedViaKeyboard = signal(false);

protected toggleMenu(item: NavItem, event?: MouseEvent): void {
  this.cancelHoverTimers();
  this.openedViaKeyboard.set(event?.detail === 0);
  this.openMenuId.update(id => (id === item.id ? null : item.id));
}
```

`event.detail === 0` distingue une activation clavier (`Entrée`/`Espace` sur un `<button>`) d'un
vrai clic souris — c'est ce qui conditionne le vol de focus (§4).

### 3.6 Identité stable des items

`NavItem` porte désormais un `id` **obligatoire** :

- alimenté par `n.code` du `NavNode` dans `mapNodesToNavItems`, avec repli
  `navItemIdFromLabel()` (slug) ;
- clés en dur pour les entrées construites par le front : `account`, `account.settings`,
  `account.password`, `account.cash-register`, `account.logout`, `account.login`,
  `layout.toggle`, `app-config`, `server-settings`.

Six constructions littérales ont été complétées, dans `navigation.service.ts`,
`sidebar.component.ts` et `navbar.component.ts`. La navbar horizontale suit le même modèle et
utilise `track item.id`.

---

## 4. Accessibilité

C'était le point le plus faible de l'existant (survol seul).

| Exigence | Mise en œuvre |
| --- | --- |
| Déclencheur | `<button>` réel, `aria-haspopup="menu"`, `aria-expanded`, `data-flyout-trigger` |
| Panneau | `role="menu"`, `aria-label` = libellé du parent ; lignes en `role="menuitem"` |
| Ouverture clavier | `Entrée`, `Espace`, `→` sur le déclencheur |
| Parcours interne | `FocusKeyManager` — `withWrap().withVerticalOrientation().withHomeAndEnd().withTypeAhead()` |
| Roving tabindex | Une seule ligne atteignable par `Tab`, synchronisée sur `keyManager.change` |
| `Tab` | **Ferme** le panneau (`keyManager.tabOut`) — un menu ne piège pas la tabulation |
| Fermeture | `Escape`, `←`, clic extérieur, `NavigationEnd` |
| Retour du focus | `cdkTrapFocus` (ouverture clavier) ou `focusTrigger()` explicite (ouverture souris) |
| Survol | Complément du clic, jamais unique moyen — voir §9.1 |

**Le focus n'est capturé que si l'ouverture vient du clavier** : `[cdkTrapFocusAutoCapture]="autoFocus()"`.
Voler le focus quand l'utilisateur ouvre à la souris serait déroutant. `CdkTrapFocus` ne restaure
le focus à la destruction que s'il l'a capturé ; pour les ouvertures souris, un écouteur
`document:keydown.escape` sur la sidebar ferme et refocalise le déclencheur.

`Escape` traité dans le panneau appelle `stopPropagation()` : l'écouteur document ne le traite pas
deux fois.

---

## 5. Rail, responsive et mobile

- **Le rail 70 px est la présentation nominale** : `SIDEBAR_COLLAPSED_KEY` vaut `true` par défaut.
  Les utilisateurs ayant déjà une préférence en `localStorage` la conservent.
- **La sidebar étendue (280 px) reste disponible** via ☰ et **ouvre le même flyout** : plus
  d'accordéon inline, comportement unique dans les deux largeurs.
- **< 768 px** : `loadEffectiveLayoutMode()` force déjà `navbar`, la sidebar n'est pas le chemin
  mobile principal. Si elle s'affiche malgré tout, le panneau passe en plein écran (`.fullscreen`,
  `position: fixed; inset: 0`) avec boutons « retour » et « fermer » dans l'en-tête.
- `main.component.scss` inchangé : le panneau étant hors flux, il ne décale rien.

---

## 6. Style du panneau

- Largeur fixe **280 px**, hauteur max `min(70vh, 560px)`, défilement interne au-delà.
- En-tête : icône + libellé + badge cumulé, sur fond plus sombre.
- Corps : une ligne par enfant — icône 24 px, libellé, badge à droite (plafonné à `99+`).
- Fond `#34495e`, coins `10px`, ombre `0 12px 32px rgba(0,0,0,.35)`.
- Survol : **seul le fond change**. Le décalage de `padding-left` de l'ancien flyout a été
  supprimé — ce glissement du texte était daté et nuisait à la lisibilité.
- Ligne active : liseré gauche `3px #2ecc71`, repère conservé de l'existant.
- Animation d'entrée 140 ms, sous `@media (prefers-reduced-motion: no-preference)`.

---

## 7. Tests (L5)

**31 tests, tous verts.** Lancer avec **`ng test`**, jamais `npx jest`.

| Fichier | Tests | Couvre |
| --- | --- | --- |
| `sidebar-flyout.component.spec.ts` | 19 | Rendu (lignes, intertitres, séparateurs, badges, `<button>` vs `<a>`), aplatissement du 3ᵉ niveau, activation d'une ligne, clavier, plein écran |
| `sidebar.component.spec.ts` | 9 | Bascule d'ouverture, un seul panneau, clavier vs souris, fermetures, survol, non-régression du repli |
| `layout.service.spec.ts` | 3 | Défaut « rail réduit » et respect d'une préférence existante |

**Deux pièges rencontrés, consignés en commentaire dans les specs :**

1. **Le `FocusKeyManager` du CDK lit `event.keyCode`**, que le constructeur `KeyboardEvent` ne
   dérive pas de `key`. Sans le passer explicitement, les tests de flèches réussissent
   silencieusement à ne rien tester. Les specs importent les constantes de `@angular/cdk/keycodes`.
2. **`provideRouter([])` fait planter le worker** (`NG04002`) : cliquer un vrai `routerLink`
   déclenche une navigation réelle. Les specs déclarent une route attrape-tout
   `{ path: '**', children: [] }`.

Le spec de la sidebar utilise `overrideTemplate(SidebarComponent, '')` — la logique d'état se teste
sans monter le CDK Overlay.

> **Suite complète :** 13 suites échouent (26 tests), **toutes préexistantes et sans rapport** —
> aucune ne référence `NavItem`, `NavigationService`, la sidebar ou la navbar.

---

## 8. Configuration ajoutée

`angular.json` — l'ordre compte (voir §9.3) :

```json
"styles": [
  ".../vendor.scss",
  "node_modules/@angular/cdk/overlay-prebuilt.css",
  "node_modules/@angular/cdk/a11y-prebuilt.css",
  ".../global.scss",
  ...
]
```

`content/scss/global.scss` :

```scss
.cdk-overlay-container { z-index: 1052; }
```

---

## 9. Écarts au plan initial

### 9.1 Le survol ne fait que **changer** de menu

Le plan prévoyait une ouverture au survol après 120 ms et une fermeture après 200 ms de grâce.
Livré : **seule la bascule entre menus** est conservée, avec ses 120 ms.

- Ouvrir au survol depuis un rail au repos déclencherait des panneaux au moindre passage de
  souris — le rail est traversé en permanence.
- Fermer au `mouseleave` ferait disparaître un menu ouvert au clic dès que l'utilisateur éloigne
  la souris pour lire.

C'est le comportement d'une barre de menus classique : le premier accès est au clic.
`onTriggerLeave()` se contente d'annuler une bascule en attente.

### 9.2 Roving tabindex écrit directement sur le DOM

`FlyoutItemDirective.setActive()` écrit `element.tabIndex` plutôt que d'exposer un signal lié à
`[attr.tabindex]`. Le panneau désigne la ligne active depuis `ngAfterViewInit`, après vérification
des directives : un binding aurait déclenché NG0100 en mode dev.

### 9.3 Deux feuilles CDK à charger, dans le bon ordre

Le plan supposait le CDK Overlay opérationnel. Il ne l'était pas :

- `overlay-prebuilt.css` était absent — sans lui, `.cdk-overlay-container` n'est pas positionné et
  le panneau s'affiche en bloc statique en bas du `<body>` ;
- `a11y-prebuilt.css` est requis par `cdkTrapFocus`, qui crée ses ancres avec la classe
  `cdk-visually-hidden` ;
- `.cdk-overlay-container` est à `z-index: 1000`, **sous la sidebar (1050)** : le panneau passait
  derrière. Relevé à 1052 — au-dessus du rail, sous les modales ng-bootstrap (1055).
  L'override doit être chargé **après** la feuille prébuilt, à spécificité égale : d'où la position
  des deux CSS CDK avant `global.scss` dans `angular.json`.

### 9.4 `focusTrigger` sans `CSS.escape`

L'identifiant vient du back-office et ne peut pas être concaténé naïvement dans un sélecteur CSS.
`CSS.escape` réglait le problème mais **n'existe pas dans jsdom** — les tests le lèvent en
`ReferenceError`. Retenu : parcourir `querySelectorAll("[data-flyout-trigger]")` et comparer
`dataset.flyoutTrigger`, ce qui supprime le besoin d'échappement.

### 9.5 Tokens CSS limités au panneau

Le plan proposait de déclarer les tokens sur `.sidebar` et sur le panneau. Ils ne sont déclarés que
dans `sidebar-flyout.component.scss`, où ils servent réellement — le panneau vit dans le conteneur
d'overlay et n'hérite pas des variables du rail. En poser sur `.sidebar` sans convertir les ~350
lignes restantes n'aurait laissé que des variables inutilisées.

---

## 10. Limites connues

- **`→` sur un déclencheur dont le panneau est déjà ouvert** ne déplace pas le focus dedans :
  l'instance existe et `autoFocus` n'est pas réévalué. `Tab` y accède. Cas marginal, non traité.
- Le **typeahead** du `FocusKeyManager` n'est armé qu'après le premier passage de l'effet qui
  alimente la requête signal — sans conséquence en usage réel.

---

## 11. Hors périmètre

- Refonte de la navbar horizontale (`layouts/navbar/`) — le composant flyout est conçu pour y être
  réutilisable, mais ce n'est pas dans ce lot.
- Migration complète vers les tokens du Design System `app/shared/ui/`.
- Flyout en cascade à plusieurs colonnes (le « Related Actions » de Workday) — écarté au profit du
  panneau unique.
- Les 13 suites de tests préexistantes en échec (§7).
- Recherche/filtre dans le menu (« command palette ») — extension naturelle maintenant que le
  panneau existe, à chiffrer séparément.
