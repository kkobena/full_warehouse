# Plan — Harmonisation navbar / sidebar

**Statut :** N0 à N4 **livrés** le 15/08/2026.
**Périmètre :** `pharmaSmart-app/src/main/webapp/app/layouts/navbar/` et `layouts/sidebar/`
**Décision de palette :** la navbar adopte **les couleurs de la sidebar**
**Prérequis :** lots L0→L5 de [PLAN-RELOOKAGE-SIDEBAR-WORKDAY.md](PLAN-RELOOKAGE-SIDEBAR-WORKDAY.md), livrés

> **Écarts constatés à l'implémentation**
>
> - **Badges unifiés dès ce jalon** : `applyNavBadges()` est remonté dans `NavigationService` et
>   lit lui-même les compteurs d'`AlertBadgeService`. Les deux barres affichent désormais
>   strictement les mêmes badges — la sidebar gagne au passage le badge `facturationOverdueCount`
>   sur `/facturation`, qu'elle n'affichait pas (§5).
> - **F2 traité ici** (`isAccountMenu` par `id === 'account'`) : le déclencheur de la navbar en a
>   besoin, il ne pouvait pas attendre N3.
> - **Token supplémentaire** `--pharma-nav-accent-soft` (`rgba(52,152,219,.2)`), la teinte de survol
>   du rail, qui n'est pas dérivable en CSS d'une couleur opaque.
> - **Caret de la navbar** : le `::after` Bootstrap de `.dropdown-toggle` disparaît avec
>   `NgbDropdown` ; remplacé par un `fa-icon` chevron qui pivote à l'ouverture, comme celui du rail.
> **Écarts constatés au lot N3**
>
> - **La bascule de mode est traitée par le service**, pas déléguée au composant : elle ne dépend
>   d'aucun état local. `NavigationService` injecte `LayoutService` et dérive le libellé du mode
>   courant. `NavMenuActions` se réduit donc aux cinq actions qui, elles, diffèrent entre les deux
>   barres ou touchent au routeur et aux modales.
> - **Bug de libellé corrigé au passage** : la sidebar annonçait « Menu vertical » sur sa bascule
>   quand l'utilisateur n'était pas connecté — alors qu'elle *est* le mode vertical. Le libellé
>   annonce désormais toujours le mode vers lequel on bascule. Un test le verrouille.
> - **La sidebar gagne « Guide des fonctionnalités »** (administrateurs) : l'entrée était câblée en
>   dur dans le seul gabarit de la navbar. Versée dans l'arbre sous l'id `cahier-recette`, elle
>   apparaît dans les deux barres — c'est l'objectif d'une source unique, mais c'est visible.
> - **Entrées simples de la navbar** : le gabarit branche désormais sur `routerLink` et rend un
>   `<button>` pour une action sans cible. Sans ça, `cahier-recette` — première entrée d'action au
>   premier niveau — aurait réintroduit le défaut A2 par une autre porte.
> - **Nettoyage induit** : `isAdmin`, `hasAnyAuthority` et quatre icônes deviennent inutilisés dans
>   les composants et sont supprimés, en plus d'`active-menu.directive.ts` (D2).

---

## 1. Objectif

Deux barres de navigation coexistent, choisies par l'utilisateur via `LayoutService.toggleLayout()` :
la navbar horizontale et la sidebar verticale. Elles affichent **le même arbre** (`NavStore` →
`NavigationService`) mais ne partagent ni gabarit, ni logique de badges, ni palette.

Ce plan les aligne sur trois axes :

1. **Couleurs** — un jeu de tokens `--pharma-nav-*` en `:root`, aux valeurs de la sidebar.
2. **Comportement** — le panneau de sous-menu du flyout, réutilisé par la navbar.
3. **Logique** — `buildNavItem()` et `applyNavBadges()`, aujourd'hui dupliqués, remontés dans
   `NavigationService`.

---

## 2. État des lieux de la navbar

### 2.1 Fichiers

| Fichier | Rôle | Taille |
| --- | --- | --- |
| `navbar.component.html` | Gabarit | 128 l. |
| `navbar.component.ts` | Logique (build du menu, badges, actions) | 219 l. |
| `navbar.component.scss` | Styles | 209 l. |
| `navbar-item.model.ts` | Modèle `NavItem` — **partagé** avec la sidebar | 43 l. |
| `active-menu.directive.ts` | Directive `jhiActiveMenu` | 29 l. |

### 2.2 Lacunes constatées

**Accessibilité — le plus sérieux**

| # | Constat | Preuve |
| --- | --- | --- |
| A1 | **Les flèches ne naviguent pas dans les menus déroulants.** `NgbDropdownMenu` récupère ses items via la directive `NgbDropdownItem` ; le gabarit ne pose `ngbDropdownItem` sur aucun `<a class="dropdown-item">`. `menuItems` reste vide, `onKeyDown` n'a rien à déplacer. | `ng-bootstrap-ng-bootstrap-dropdown.mjs:90` — `queries: [{ propertyName: "menuItems", predicate: NgbDropdownItem }]` |
| A2 | **Les actions du menu Compte sont hors du parcours clavier.** « Se déconnecter », « Menu vertical », « Paramètres Serveur », « Configuration avancée » sont des `<a [routerLink]="undefined">` : Angular met alors `routerLinkInput` à `null` et retire le tabindex → un `<a>` sans `href` ni tabindex. Ce sont des actions : il leur faut des `<button>`. | `_router_module-chunk.mjs:233-236` |
| A3 | Aucun `aria-current` sur l'entrée active. | `navbar.component.html` |

**Fonctionnel**

| # | Constat |
| --- | --- |
| F1 | **Le 3ᵉ niveau est perdu** — `mapNodesToNavItems` est récursif (`navigation.service.ts:122`), le gabarit ne descend qu'à `child` (`navbar.component.html:49`). Un `child` porteur de `children` se rend comme une ligne morte, sans `routerLink` ni action. C'est exactement le défaut corrigé côté sidebar au lot L1. |
| F2 | `isAccountMenu()` reconnaît le menu Compte **par sous-chaîne du libellé** (`'account'`, `'compte'`) — fragile, et le libellé est traduit. Depuis le lot L0 l'item porte `id === 'account'`. (`navbar.component.ts:118`) |
| F3 | « Guide des fonctionnalités » est câblé en dur dans le gabarit, hors de `navItems` — invisible pour le `NavStore` et son système de permissions. (`navbar.component.html:107-117`) |
| F4 | `track $index` sur les enfants alors que les `id` existent depuis L0. (`navbar.component.html:49`) |

**Duplication et code mort**

| # | Constat |
| --- | --- |
| D1 | `buildNavItem()` et `applyNavBadges()` sont des quasi-copies de celles de la sidebar — seul `facturationOverdueCount` diffère. Toute règle de badge se modifie à deux endroits. (~60 l. dupliquées) |
| D2 | **`active-menu.directive.ts` est du code mort** : `jhiActiveMenu` n'apparaît dans aucun gabarit du projet. |

**Couleurs**

| # | Constat |
| --- | --- |
| C1 | Les menus déroulants sont repeints par `::ng-deep` (`navbar.component.scss:161`) pour reproduire à la main une règle Bootswatch (`.bg-primary .dropdown-menu`) perdue en quittant `.bg-primary`. Le commentaire du fichier le documente lui-même — c'est du rattrapage fragile. |
| C2 | `.dropdown-item.active` est adossé à `$dark` (`navbar.component.scss:64`), sans rapport avec le token de chrome. |

### 2.3 Palettes en présence

| Surface | Couleurs | Source |
| --- | --- | --- |
| Navbar | `#008cba` / `#007ea7` | `navbar.component.scss:112-114` (teal Bootswatch yeti) |
| Sidebar + flyout | `#2c3e50` / `#34495e` / `#3498db` / `#2ecc71` | `sidebar.component.scss`, `sidebar-flyout.component.scss` |
| Onglets et pills internes aux écrans | `#5b89a6` / `#4a7189` | `shared/scss/_pharma-nav.scss` |
| `$primary` Bootstrap | émeraude Aura | `_pharma-bootstrap-palette.scss` |

**Ce plan unifie les deux premières lignes.** Les pills de `_pharma-nav.scss` relèvent de la
navigation *interne aux écrans*, pas du chrome applicatif : hors périmètre (§8).

---

## 3. Palette cible

Jeu de tokens déclaré en `:root` dans `content/scss/global.scss`, aux **valeurs de la sidebar** :

```scss
:root {
  --pharma-nav-bg:        #2c3e50;              /* fond du chrome (navbar, rail) */
  --pharma-nav-bg-hover:  #34495e;              /* survol d'une entrée du chrome */
  --pharma-nav-panel:     #34495e;              /* fond des panneaux de sous-menu */
  --pharma-nav-fg:        #ecf0f1;              /* texte principal */
  --pharma-nav-fg-muted:  rgba(236, 240, 241, 0.85);
  --pharma-nav-accent:    #3498db;              /* survol dans un panneau, icônes d'en-tête */
  --pharma-nav-active:    #2ecc71;              /* liseré de l'entrée courante */
}
```

Conséquences :

- **La sidebar ne change pas d'aspect** : ce sont ses couleurs actuelles, simplement nommées.
  Les valeurs en dur de `sidebar.component.scss` et `sidebar-flyout.component.scss` sont
  remplacées par les tokens.
- **La navbar passe du teal au bleu ardoise.** C'est la surface la plus visible de
  l'application : le changement est franc et doit être assumé.
- `--pharma-navbar-bg` / `--pharma-navbar-bg-hover` (aujourd'hui sur `:host` de la navbar)
  disparaissent au profit de `--pharma-nav-bg` / `--pharma-nav-bg-hover`. Ils ne sont référencés
  que dans `navbar.component.scss` — vérifié.
- Les classes `navbar-dark` et les variables `$navbar-dark-color` de Bootswatch restent valides :
  elles supposent un fond sombre, ce que `#2c3e50` est davantage que `#008cba`.
- C2 se règle au passage : `.dropdown-item.active` s'adosse à `--pharma-nav-accent` au lieu de
  `$dark`.

---

## 4. Réutilisation du panneau

`SidebarFlyoutComponent` est **déjà présentationnel** : il prend `item`, `autoFocus`, `fullscreen`
en entrée, émet `navigate` et `close`, et ne connaît ni le routeur ni le store. C'était l'intention
inscrite au plan du lot L1. Il est réutilisable sans modification de fond.

### 4.1 Déplacement

```
layouts/shared/nav-flyout/
├── nav-flyout.component.{ts,html,scss}   # ex-sidebar-flyout, sélecteur `app-nav-flyout`
├── nav-flyout.component.spec.ts
└── flyout-item.directive.ts              # inchangé
```

La sidebar consomme le composant déplacé ; son propre spec suit.

### 4.2 Seule adaptation nécessaire : la direction

Un input `placement: 'side' | 'below'` (défaut `'side'`) pilote la direction de l'animation
d'entrée — `translateX(-8px)` pour le rail, `translateY(-8px)` pour la navbar. C'est le seul
écart entre les deux usages : tout le reste (en-tête, liste, groupes, séparateurs, badges,
clavier, roving tabindex, aplatissement du 3ᵉ niveau) est identique.

### 4.3 Positions CDK pour la navbar

```ts
readonly flyoutPositions: ConnectedPosition[] = [
  { originX: 'start', originY: 'bottom', overlayX: 'start', overlayY: 'top',    offsetY: 4 },
  { originX: 'end',   originY: 'bottom', overlayX: 'end',   overlayY: 'top',    offsetY: 4 },
  { originX: 'start', originY: 'top',    overlayX: 'start', overlayY: 'bottom', offsetY: -4 },
];
```

Le repli en `end` évite qu'un menu proche du bord droit ne sorte de l'écran — la navbar aligne ses
entrées à droite (`ms-auto`).

### 4.4 Ce que le remplacement de `NgbDropdown` règle

| Lacune | Réglée par |
| --- | --- |
| A1 — flèches inertes | `FocusKeyManager` du panneau (déjà en place) |
| A2 — actions non focalisables | Le panneau rend un `<button>` quand `routerLink` est absent |
| F1 — 3ᵉ niveau perdu | Aplatissement en intertitre + lignes |
| F4 — `track $index` | `track row.key` |
| C1 — `::ng-deep` sur `.dropdown-menu` | Le panneau est un composant : styles encapsulés |

`NgbCollapse` reste nécessaire pour le repli mobile de la navbar ; seuls `NgbDropdown`,
`NgbDropdownMenu` et `NgbDropdownToggle` sortent des imports.

---

## 5. Factorisation de la logique

`buildNavItem()` et `applyNavBadges()` remontent dans `NavigationService`, qui construit déjà les
items :

```ts
buildNavItems(options: {
  layoutToggleLabel: string;      // « Menu vertical » / « Menu horizontal »
  onToggleLayout: () => void;
  onLogout: () => void;
  onLogin: () => void;
  onOpenConfigEditor: () => void;
  onOpenAppSettings: () => void;
}): NavItem[]
```

et l'application des badges devient une méthode unique alimentée par `AlertBadgeService`.

> **Changement de comportement à assumer :** la sidebar n'applique pas aujourd'hui le badge
> `facturationOverdueCount` sur `/facturation` (`sidebar.component.ts:212`), contrairement à la
> navbar. L'unification le lui ajoute. C'est cohérent — les deux barres montrent le même arbre —
> mais c'est un changement visible côté sidebar, pas une simple refactorisation.

Sont également traités dans ce lot : F2 (`isAccountMenu` → `item.id === 'account'`), F3 (« Guide
des fonctionnalités » versé dans `buildNavItems` sous l'id `cahier-recette`, conditionné à
`isAdmin`) et D2 (suppression de `active-menu.directive.ts`).

---

## 6. Découpage en lots

| Lot | Contenu | Fichiers |
| --- | --- | --- |
| **N0 — Tokens** | `--pharma-nav-*` en `:root` ; sidebar et flyout consomment les tokens (aucune régression visuelle) ; navbar bascule sur `--pharma-nav-bg` (changement de teinte) | `global.scss`, `sidebar.component.scss`, `sidebar-flyout.component.scss`, `navbar.component.scss` |
| **N1 — Extraction** | `sidebar-flyout` → `layouts/shared/nav-flyout`, sélecteur `app-nav-flyout`, input `placement` | 4 fichiers déplacés + `sidebar.component.{ts,html}` |
| **N2 — Navbar sur flyout** | `NgbDropdown` → `cdkConnectedOverlay` + `app-nav-flyout`, `openMenuId` en signal, suppression des `::ng-deep .dropdown-menu` | `navbar.component.{ts,html,scss}` |
| **N3 — Factorisation** | `buildNavItems` + badges dans `NavigationService` ; F2, F3, D2 | `navigation.service.ts`, `navbar.component.ts`, `sidebar.component.ts`, suppression de `active-menu.directive.ts` |
| **N4 — Tests** | Voir §7 | `*.spec.ts` |

N0 est livrable seul et donne immédiatement le résultat visuel demandé. N1→N2 forment le second
jalon.

---

## 7. Tests

Lancer avec **`ng test`**, jamais `npx jest`.

- Les 19 tests de `nav-flyout.component.spec.ts` suivent le composant déplacé, sans modification
  de fond ; ajouter la couverture de `placement`.
- `navbar.component.spec.ts` — **21 tests**. Deux régimes dans le même fichier : la logique avec
  `overrideTemplate('')` et un routeur factice, le rendu avec le vrai gabarit et
  `provideRouter([{ path: '**', children: [] }])`, que les `routerLink` exigent. Couvre l'ouverture,
  la fermeture (`Escape`, clic extérieur, `NavigationEnd`), le survol, `isAccountMenu` par `id`, et
  le rendu `<button>` / `<a>` des entrées simples.
- `navigation.service.spec.ts` — **17 tests** : construction des items (anonyme, connecté,
  conditions Tauri/ADMIN), libellé de la bascule de mode, et les cinq règles de badges dont
  l'effacement d'un badge devenu obsolète — le cas qu'une mutation en place peut manquer.

> Rappel du lot L5 : le `FocusKeyManager` du CDK lit `event.keyCode`, que le constructeur
> `KeyboardEvent` ne dérive pas de `key`. Les specs doivent importer les constantes de
> `@angular/cdk/keycodes`, sinon les tests de flèches réussissent sans rien tester.

**Suite existante :** 13 suites échouent déjà (26 tests), sans rapport avec la navigation. Ne pas
les confondre avec une régression de ces lots.

---

## 8. Risques et points d'attention

| Risque | Portée | Parade |
| --- | --- | --- |
| **Choc visuel** : la navbar est la surface la plus visible, elle change franchement de couleur | Élevé | Décision prise et assumée ; N0 est réversible en une ligne (valeurs des tokens) |
| Le badge facturation apparaît côté sidebar (§5) | Moyen | Changement voulu, à signaler aux utilisateurs |
| Navbar repliée sur mobile : le panneau s'ancre sous une entrée d'une pile verticale, à l'étroit | Moyen | Réutiliser l'input `fullscreen` sous 768 px, comme la sidebar |
| `.navbar` sticky à `z-index: 1020`, conteneur d'overlay à 1052 | Faible | Déjà correct : le panneau passe au-dessus |
| Règles résiduelles ciblant `.dropdown-menu` dans la portée navbar | Faible | Grep de contrôle après N2 |
| `_pharma-nav.scss` conserve une 3ᵉ famille de couleurs | Faible | Hors périmètre — navigation interne aux écrans, pas le chrome |

---

## 9. Hors périmètre

- Alignement de `_pharma-nav.scss` (onglets et pills internes aux écrans) sur les tokens
  `--pharma-nav-*`.
- Migration vers les tokens du Design System `app/shared/ui/`.
- Les 13 suites de tests préexistantes en échec.
- Refonte du repli mobile de la navbar (`NgbCollapse`), conservé tel quel.
