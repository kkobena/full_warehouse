# Plan — Thèmes de couleur pour toute l'application

> Rédigé le 2026-10-03, **mis à jour le 2026-10-03 : les six décisions D1 à D6 sont tranchées** (§4), et la **barre de titre du mode Tauri** est intégrée
> au périmètre.
> Étend à l'application entière ce qui a été fait pour l'espace de vente
> ([PLAN-STYLES-COMPTOIR.md](PLAN-STYLES-COMPTOIR.md)) : des **jetons de couleur par thème**, posés sur `<html>`, que les composants
> lisent au lieu de coder une couleur. **Aucun code n'est modifié par ce document.**

## 1. La demande

Un utilisateur choisit un thème ; six familles de composants en prennent la couleur : **barre de navigation** (`navbar`),
**barre latérale** (`sidebar`), **`app-toolbar`**, **en-tête des modales**, **infobulles**, **en-tête des tableaux**
(`app-data-table`) — **plus la barre de titre dessinée par l'application en mode Tauri** (`shared/titlebar`), qui est le « header » de la
fenêtre bureau et doit suivre la même couleur (§2.2, §5.5). Cinq thèmes :

| # | Thème | Couleur de départ | Hex |
|---|---|---|---|
| 1 | Actuel | l'habillage d'aujourd'hui (voir §2) | — |
| 2 | Vente comptant | accent de la vente comptant | `#047857` |
| 3 | Vente assurance | accent de la vente assurance | `#2a74a0` |
| 4 | Prévente comptant | accent de la prévente / proforma comptant | `#4f4aa8` |
| 5 | Prévente carnet | accent de la prévente / proforma carnet | `#6f6420` |

Vocabulaire (décision D1) : les trois choix existants (`menthe`, `ardoise`, `clair`) ne sont pas de vrais thèmes, ils ne changent que la
couleur de fond de page ; ce plan les appelle **fonds de page** et les laisse tels quels. « Thème » désigne désormais les cinq choix
ci-dessus.

Faisabilité : **oui, et à coût raisonnable**. Les composants dépendent aujourd'hui de **six points de couleur seulement** (§2.2),
ce qui rend la bascule vers des jetons étroite. Le gros du risque est ailleurs : les couleurs codées en dur dans le reste de
l'application, qui ne suivront pas (§7).

## 2. État des lieux (relevé dans le code le 2026-10-03)

### 2.1 Ce qui existe déjà

- `core/theme/theme.service.ts` : trois thèmes, **`menthe`** (défaut), **`ardoise`**, **`clair`**, posés en `data-theme` sur `<html>`, mémorisés
  dans `localStorage` (`pharmasmart_theme`), choisis depuis un sous-menu de `navigation.service.ts` (ligne 140).
- `content/scss/_pharma-themes.scss` : une table `$pharma-themes` → jetons `--pharma-app-bg / surface / border / text / accent…`,
  les jetons Bootstrap (`--bs-primary`, liens) et Aura (`--p-primary-*`), plus une régénération à la compilation des boutons et
  composants Bootstrap quand l'accent diffère de `$primary` (`pharma-theme-accent-components`).
- **Les trois « thèmes » existants sont en fait trois fonds de page**, tous clairs : ils ne diffèrent que par la couleur de fond et un accent
  voisin (`#0f766e` menthe, `#047857` pour ardoise et clair). Ils ne touchent pas à la couleur du « chrome » (barres, en-têtes).
- Les infobulles sont **déjà** au thème : `--bs-tooltip-bg: color-mix(accent 25 %, #1e293b)`, blanc dessus (≥ 7:1).
- L'espace de vente a ses propres accents par type de vente (`--comptoir-accent-*`), indépendants du thème.

### 2.2 Ce qui n'est PAS au thème : les six points de couleur

| Composant | Où la couleur est décidée | Valeur actuelle | Gabarits concernés |
|---|---|---|---|
| Barre de navigation | `content/scss/global.scss:1376-1388` — `:root { --pharma-nav-* }`, constantes ; lues par `layouts/navbar`, `layouts/sidebar`, `layouts/shared/nav-flyout`, `shared/titlebar` (Tauri) | ardoise bleutée `#2c3e50`, dégradé `#34506b → #2f5b6b`, liseré arc-en-ciel | toutes les pages |
| Barre latérale | idem (`--pharma-nav-gradient-v`, `--pharma-nav-rainbow-v`) ; `shared/ui/nav-sidebar` | dégradé vertical `#243b55 → #1f4e5a` | toutes les pages (mode sidebar) |
| `app-toolbar` | `content/scss/pharma-toolbar-global.scss` + `app/shared/scss/_pharma-toolbar.scss` — **`$pharma-primary`, variable SCSS figée à la compilation** | dégradé du primaire vert | 92 gabarits `<app-toolbar>` (77 `pharma-toolbar`) |
| En-tête de modale | `app/shared/scss/_modal-theme.scss:22` et `:202` — **`linear-gradient(#2e7d97 → #1b5e75) !important`** | bleu acier | 129 fichiers (`modal-header`) |
| En-tête de tableau | `content/scss/table-common-global.scss` (~91 et ~116) — **`linear-gradient(#6b9ab8 → #5b89a6) !important`** ; ag-grid : thème alpine dans `global.scss:546` et `:765`, avec 16 fichiers qui le surchargent | bleu acier clair | 172 gabarits `pharma-table-head`, 160 `app-data-table` |
| **Barre de titre Tauri** | `app/shared/titlebar/titlebar.component.scss` : haut de dégradé **codé en dur** (`--titlebar-bg-top: #22303d`), bas = `--pharma-nav-bg`, texte = `--pharma-nav-fg*`, icône = `--pharma-nav-accent`. Fenêtre en `decorations: false` (`src-tauri/tauri.conf.json`) : il n'y a **pas de barre native**, celle de l'application est la seule | `#22303d → #2c3e50` ; boutons de fenêtre : voiles blancs 10 / 15 %, **fermeture `#e81123` / `#c50f1f`** (convention Windows) | 1 composant, monté par `layouts/main` quand `isRunningInTauri()` ; fixe, 32 px, `z-index` 9999 |
| Infobulle | `_pharma-themes.scss` (fin) | **déjà au thème** | partout |

### 2.3 Constats qui orientent le plan

1. **Le « thème actuel » n'est pas une couleur mais un assemblage de quatre sources** : ardoise bleutée (barres), bleu acier
   (modales et tableaux), vert primaire (toolbar), accent teal (liens, infobulles). Il faut donc le **décrire tel quel** dans la nouvelle
   table, sans le « corriger », pour que choisir « Actuel » ne change rien à l'écran.
2. **L'en-tête de tableau actuel ne tient pas le contraste** : texte blanc 13 px gras sur `#5b89a6` = **3,77:1** (haut du dégradé `#6b9ab8`
   = 3,03:1), seuil 4,5:1. Même constat que pour « TOTAUX » dans l'espace de vente. À traiter dans le thème 1 (voir D5).
3. **Le thème 2 reprend l'accent d'ardoise et de clair** (`#047857`) : seul le chrome (barres, en-têtes) change. Le chrome est donc
   **un axe à part** des fonds de page existants (D1, décidé).
4. **89 fichiers SCSS** utilisent `$pharma-primary` à la compilation et **16 fichiers** codent en dur une couleur de la famille
   (`#0f766e`, `#047857`, `#2e7d97`, `#00a86b`). Ils ne suivront pas un changement de thème : hors périmètre de la phase 1, mais
   visibles à côté d'un chrome qui change (§7).
5. **La barre de titre Tauri suit déjà les jetons `--pharma-nav-*`** pour le bas de son dégradé, le texte et l'icône : elle changera donc de
   couleur dès que ces jetons seront thémés. Il lui manque seulement son **haut de dégradé** (`#22303d`, en dur) et **la règle d'empilement** que son
   fichier documente : titlebar `L = 18 %` < rail `24 %` < navbar `32 %`, pour que la barre de fenêtre « ancre la pile » sans rupture à l'angle.
   Un thème qui ne garde pas cet ordre montrerait une arête. Le code contient aussi deux schémas alternatifs **commentés** (clair, sombre),
   à ne pas ressusciter.
6. L'application a **deux dispositions** (`layout.toggle`, « Menu horizontal / vertical ») : barre de navigation en haut, ou rail latéral. Les deux
   consomment les mêmes jetons ; les deux sont à vérifier sous chaque thème.
7. Les en-têtes de tableau et de modale utilisent `!important` : le passage aux jetons doit retirer ces `!important` sous peine de
   figer la couleur.

## 3. Les cinq thèmes, mesurés

Texte blanc sur la couleur, sur sa version assombrie (78 % de la couleur, 22 % de noir : fin de dégradé) et sur le fond de barre dérivé
(45 % de la couleur, 55 % de `#1e293b`) ; seuil WCAG AA : 4,5:1.

| Thème | Couleur | Blanc / couleur | Blanc / assombrie | Blanc / fond de barre | Texte foncé sur teinte à 14 % |
|---|---|---|---|---|---|
| 1 Actuel (accent teal) | `#0f766e` | 5,47 | 7,83 | 9,62 | — |
| 1 Actuel (modale) | `#2e7d97` | 4,67 | 6,85 | 8,83 | — |
| 1 Actuel (tableau) | `#5b89a6` | **3,77** ✗ | 5,70 | 7,88 | — |
| 2 Comptant | `#047857` | 5,48 | 7,85 | 9,69 | 6,44 |
| 3 Assurance | `#2a74a0` | 5,12 | 7,40 | 9,23 | 6,14 |
| 4 Prévente comptant | `#4f4aa8` | 7,30 | 9,86 | 10,90 | 7,96 |
| 5 Prévente carnet | `#6f6420` | 5,97 | 8,38 | 10,04 | 6,90 |

**Empilement des trois bandes (navbar, rail, barre de titre Tauri).** Pour les quatre thèmes dérivés, une seule couleur de barre B (accent 45 % +
`#1e293b`) donne les trois niveaux : **navbar = B éclaircie de 12 %, rail = B, barre de titre = B assombrie de 30 %**. L'ordre de luminosité
`titlebar < rail < navbar` tient par construction (vérifié) et le blanc dessus reste ≥ 6,6:1 :

| Thème | Luminance navbar > rail > barre de titre | Blanc / navbar | Blanc / rail | Blanc / barre de titre |
|---|---|---|---|---|
| Actuel (`#34506b`, `#243b55`, `#22303d`) | 0,075 > 0,042 > 0,028 | 8,38 | 11,46 | 13,48 |
| 2 Comptant | 0,101 > 0,058 > 0,029 | 6,94 | 9,69 | 13,27 |
| 3 Assurance | 0,109 > 0,064 > 0,032 | 6,62 | 9,23 | 12,85 |
| 4 Prévente comptant | 0,088 > 0,046 > 0,024 | 7,64 | 10,90 | 14,27 |
| 5 Prévente carnet | 0,099 > 0,055 > 0,028 | 7,04 | 10,04 | 13,54 |

Les pourcentages sont une **proposition de départ**, à
affiner en phase 3 en regardant le rendu.

Les quatre nouveaux thèmes tiennent le blanc sur la couleur **sans réglage**. Seule la ligne « Actuel (tableau) » échoue : correction minimale,
**`#527b95 → #4d748d`** (4,54 et 5,00), visuellement très proche.

## 4. Décisions

### 4.1 Tranchées (2026-10-03)

| | Question | Décision |
|---|---|---|
| **D1** | Rapport aux trois choix existants (`menthe`, `ardoise`, `clair`). | **On les garde tels quels, comme fonds de page** : ils ne sont pas de vrais thèmes, ils changent seulement la couleur de fond. Les cinq thèmes forment un **second axe**, le **chrome** (`data-chrome`), qui ne touche ni à `data-theme` ni à la clé `pharmasmart_theme`. Combinaisons : 3 fonds × 5 thèmes = 15, toutes vérifiées par le garde-fou de contraste (§6). Le menu actuel « Thème » gagne un libellé qui dit ce qu'il fait (proposition : « Fond de page »), à confirmer. |
| **D2** | Où mémoriser le choix. | **Par poste** (`localStorage`, comme `ThemeService`) : aucun backend, aucune migration, aucun endpoint. La mémorisation par compte est **écartée** pour l'instant. **Le thème par défaut sera choisi après les essais** : d'ici là, le défaut est `actuel`, donc rien ne change pour qui ne touche pas au sélecteur. Le défaut tient dans **une seule constante** (`DEFAULT_CHROME`), à changer en une ligne le jour venu. |
| **D3** | Thèmes 4 et 5 = palette des préventes et proformas (indigo `#4f4aa8`, olive `#6f6420`), prévue pour ne pas confondre un document avec une vente. | **L'écran de vente garde ses propres accents** (par type de vente et par document) **dans un premier temps** : le thème ne touche que le chrome autour. On jugera à l'usage s'il faut aligner l'écran de vente sur le thème ; c'est une option de la phase 7, pas un préalable. Le sélecteur signale que ces deux thèmes « peuvent ressembler à l'écran de prévente ». |

Décisions D4 à D6 (2026-10-03) :

| | Question | Décision |
|---|---|---|
| **D4** | Le thème fait-il aussi suivre l'accent applicatif (boutons primaires, liens, onglets actifs) ? | **Oui.** Livré **après** le chrome (phase 6, qui n'est plus une option) : le mécanisme existe déjà (`pharma-theme-accent-components` régénère les composants Bootstrap quand l'accent change). **Vigilance** : avec le thème 2, le primaire (`#047857`) devient presque identique au vert « succès » (`#15803d`) ; avant de lier, regarder les écrans où les deux cohabitent (validation et réussite côte à côte) et, au besoin, démarquer le « succès » pour ce thème. |
| **D5** | Corriger l'en-tête de tableau du thème « Actuel » (3,77:1) ? | **Oui**, `#527b95 → #4d748d` (4,54 et 5,00:1) : seule exception à « Actuel = identique à aujourd'hui », assumée et annoncée dans les notes de version. |
| **D6** | Le liseré arc-en-ciel des barres (`--pharma-nav-rainbow`, une marque, pas une couleur de thème). | **Oui, on le garde sur tous les thèmes, avec la possibilité de l'adapter au besoin.** Il devient un jeton `--pharma-chrome-rainbow` (et `-rainbow-v` pour le rail), **identique pour les cinq thèmes au départ** ; si l'un d'eux jure avec lui à l'usage (l'indigo, par exemple), on ne change que la valeur du jeton pour ce thème, sans toucher aux composants. |

Il ne reste **aucune décision en attente** ; restent à confirmer, sans bloquer : le libellé du menu des fonds de page (D1) et le défaut final (phase 5).

## 5. Architecture cible

### 5.1 Jetons
Un seul espace de noms nouveau, **`--pharma-chrome-*`**, qui ne touche ni `--pharma-*` (surface) ni `--comptoir-*` (espace de vente) :

| Jeton | Rôle | Remplace |
|---|---|---|
| `--pharma-chrome-bar` / `-bar-hover` / `-panel` | fond des barres, survol, panneau déroulant | `--pharma-nav-bg`, `-bg-hover`, `-panel` |
| `--pharma-chrome-gradient-h` / `-gradient-v` | dégradé de la barre horizontale / du rail | `--pharma-nav-gradient-h` / `-v` |
| `--pharma-chrome-fg` / `-fg-muted` | texte et icônes sur barre | `--pharma-nav-fg`, `-fg-muted` |
| `--pharma-chrome-accent` / `-accent-soft` / `-active` | entrée active, survol, repère | `--pharma-nav-accent`, `-accent-soft`, `-active` |
| `--pharma-chrome-head-a` / `-head-b` | début et fin du dégradé d'en-tête (modale, tableau, toolbar) | couleurs codées en dur (§2.2) |
| `--pharma-chrome-head-fg` | texte sur en-tête | `#fff` en dur |
| `--pharma-chrome-titlebar-a` / `-titlebar-b` | haut et bas du dégradé de la barre de titre Tauri (`-b` = fond du rail) | `--titlebar-bg-top: #22303d` en dur, `--titlebar-bg-bottom` |
| `--pharma-chrome-rainbow` / `-rainbow-v` | liseré multicolore des barres (D6) | `--pharma-nav-rainbow`, `-rainbow-v` |

Les `--pharma-nav-*` **restent comme alias** (`--pharma-nav-bg: var(--pharma-chrome-bar)`) le temps de la migration : `navbar`,
`sidebar`, `nav-flyout` et `titlebar` n'ont alors rien à changer en phase 1.

### 5.2 Une table, un mixin (comme `$pharma-themes`)
Nouveau partiel `content/scss/_pharma-chrome-themes.scss` : `$pharma-chrome-themes: ('actuel': (...), 'comptant': (accent: #047857), …)`.
Pour les quatre thèmes dérivés, **un seul accent** par entrée et les autres jetons calculés en `color-mix`, sur le modèle des jetons du
comptoir : fond de barre = accent 45 % + `#1e293b`, fin de dégradé = accent 78 % + noir, texte blanc. Pour `actuel`, les valeurs **littérales**
d'aujourd'hui (§2.2). Une boucle émet `:root[data-chrome='…']` ; sans attribut, `actuel`.

### 5.3 Composants : remplacer six endroits, pas cent
| Endroit | Changement |
|---|---|
| `global.scss:1376-1388` | le bloc `:root` devient les valeurs de `actuel` + alias |
| `_modal-theme.scss` (2 règles) | `background: linear-gradient(135deg, var(--pharma-chrome-head-a), var(--pharma-chrome-head-b))`, **sans `!important`** |
| `table-common-global.scss` (2 règles) + thème alpine ag-grid (`global.scss`) | idem ; variables `--ag-header-background-color` / `-foreground-color` lisant les jetons ; vérifier les 16 surcharges locales |
| `pharma-toolbar-global.scss` / `_pharma-toolbar.scss` | `$pharma-primary` remplacé par les jetons (c'est le seul des cinq à dépendre d'une variable SCSS figée) |
| `app/shared/titlebar/titlebar.component.scss` | `--titlebar-bg-top` lit `--pharma-chrome-titlebar-a` ; le reste (bas de dégradé, texte, icône) lit déjà `--pharma-nav-*`, donc rien à changer ; **les boutons de fenêtre gardent leurs voiles blancs, et la fermeture reste rouge** (convention d'OS, pas une couleur de thème) ; supprimer les deux schémas commentés |
| `_pharma-themes.scss` (infobulle) | rien : déjà au thème. À décider : lire `--pharma-chrome-accent` plutôt que `--pharma-accent`, pour que l'infobulle suive le **chrome** |

### 5.4 Service et interface
- `ChromeThemeService` (calqué sur `ThemeService`) : signal, `data-chrome` sur `<html>`, clé `pharmasmart_chrome`, valeur validée contre la liste,
  repli sur `DEFAULT_CHROME` (`actuel` jusqu'au choix du défaut, D2). Une valeur inconnue ou absente donne le défaut, jamais une erreur.
- Un sous-menu « Couleur de l'application » dans `navigation.service.ts`, à côté du sous-menu des fonds de page (renommé, D1), avec une
  pastille de couleur par entrée et la mention « peut ressembler à l'écran de prévente » sur les thèmes 4 et 5 (D3).
- **Pas de flash au chargement** : l'attribut doit être posé avant le premier rendu, dans `main.ts` avant l'amorçage (la CSP de Tauri
  autorise pourtant `'unsafe-inline'` ; `main.ts` reste préférable : un seul endroit, le même en navigateur et sous Tauri).
- La page de connexion prend le thème choisi sur le poste.
- **Plusieurs fenêtres Tauri** : le bouton « Nouvelle fenêtre » de la barre de titre ouvre une autre instance, qui partage le `localStorage`.
  Écouter l'événement `storage` pour que les fenêtres déjà ouvertes suivent un changement fait dans l'une d'elles.

### 5.5 Mode Tauri
- **Une seule barre de titre, celle de l'application** (`decorations: false`) : il n'y a pas de couleur de fenêtre native à accorder, donc rien
  à piloter côté Rust.
- Elle est fixe (32 px, `z-index` 9999) et la zone de contenu se décale de 32 px en mode Tauri (`:host.tauri-mode`). Sa **hauteur ne change pas
  d'un thème à l'autre** ; les mesures de hauteur de l'écran de vente (`HauteurEcranVenteDirective`) la prennent en compte sans changement.
- Contrôle en navigateur : forcer le mode Tauri (script d'initialisation de Playwright) pour que la barre s'affiche, la capturer sous les 5 thèmes
  et mesurer ses contrastes ; **puis une vérification à la main sur l'exécutable** (`npm run tauri:dev`), qu'aucun test navigateur ne remplace.
- Hors périmètre, à décider plus tard : le **fond de la fenêtre avant le premier rendu** (blanc, aucun `backgroundColor` déclaré dans `tauri.conf.json`) et
  les écrans de démarrage `backend-splash` et assistant de configuration, qui codent leurs propres bleus (`#2563eb`).

## 6. Garde-fous

- **Contraste exécuté, pas commenté** : un `theme-contraste.spec.ts` sur le modèle de `comptoir-contraste.spec.ts`, qui **lit le SCSS** (aucune couleur
  recopiée) et échoue si, pour un thème × un fond de page (les 15 combinaisons de D1) : texte blanc sur en-tête / barre < 4,5:1 ; texte d'infobulle < 7:1 ; entrée active
  sur barre < 3:1 ; accent sur teinte (icône) < 3:1.
- **Empilement des bandes** : le même test vérifie, pour les cinq thèmes, `L(barre de titre) < L(rail) < L(navbar)` et le blanc ≥ 4,5:1 sur chacune.
- **Axe en navigateur** : projet Playwright `a11y` existant, rejoué sur un jeu d'écrans représentatif (accueil, liste produits, une modale, l'écran
  de vente) × 5 thèmes (sur le fond de page par défaut, puis un contrôle des deux autres fonds) ; zéro défaut.
- **Non-régression visuelle du thème « Actuel »** : captures avant / après sur ces mêmes écrans ; aucune différence hors en-tête de tableau (D5).
- **Barre de titre Tauri** : capture et contraste sous les 5 thèmes (§5.5) ; bouton de fermeture inchangé.
- **Hauteur et largeur** : la barre ne doit pas changer de taille d'un thème à l'autre (le texte blanc reste le même) ; le test responsive
  existant (`comptoir-responsive.spec.ts`) tourne sous chaque thème.

## 7. Risques

| Risque | Effet | Parade |
|---|---|---|
| 89 fichiers SCSS sur `$pharma-primary` + 16 en dur ne suivent pas | boutons et pastilles vertes à côté d'une barre indigo | mesurer en phase 0 (captures des écrans courants sous thème 4) ; liste de ce qui jure ; traité en phase 6 (accent lié, D4) |
| `!important` sur modales et tableaux | la couleur du thème est ignorée | les retirer en phase 1 ; vérifier qu'aucune surcharge locale ne les rétablit (16 fichiers ag-grid) |
| Confusion thèmes 4 et 5 avec l'écran de prévente | l'utilisateur croit être en prévente | D3 (décidé) : l'écran de vente garde ses accents, libellé dans le sélecteur ; à réévaluer à l'usage (phase 7) |
| Primaire ≈ « succès » avec le thème 2 | boutons de validation et de réussite indiscernables | D4 ; à regarder avant de lier l'accent |
| Thème choisi sur un poste partagé | un caissier change la couleur pour tous ceux qui utilisent ce poste | D2 (décidé : par poste) : accepté ; si cela gêne à l'usage, réserver le réglage à un droit — la mémorisation par compte est écartée pour l'instant |
| Barre de titre Tauri non migrée ou testée seulement en navigateur | arête de couleur au sommet de la fenêtre bureau, ou thème ignoré par la barre | une ligne dédiée en phase 1, le test d'empilement en phase 3, la vérification sur exécutable en phase 4 (§5.5) |
| Plusieurs fenêtres Tauri ouvertes | une fenêtre garde l'ancienne couleur après un changement | écoute de l'événement `storage` (§5.4) |
| Le liseré arc-en-ciel jure avec un thème | barre moins lisible ou criarde | D6 : jeton adaptable par thème, sans toucher aux composants |
| Flash de la couleur d'avant au chargement | scintillement à chaque ouverture | pose de l'attribut avant l'amorçage (§5.4) |
| Combinaisons 3 fonds de page × 5 thèmes = 15 | une combinaison non vérifiée | le garde-fou de contraste les parcourt toutes (D1 : deux axes, décidé) |

## 8. Phases

| Phase | Contenu | Livrable | Critère de sortie | Effort |
|---|---|---|---|---|
| **0** | Décisions D1 à D6 : toutes tranchées (§4) ; inventorier les couleurs de la famille (les 89 + 16 fichiers du §2.3, recoupement possible) ; captures « avant » des écrans de référence | liste d'écrans et captures ; décisions notées ici | décisions prises | 0,5 j |
| **1** | Jetons `--pharma-chrome-*` + thème `actuel` à l'identique ; **six** endroits du §5.3 migrés (**barre de titre Tauri comprise**), `!important` retirés ; alias `--pharma-nav-*` ; jeton du liseré (D6) | SCSS + captures « après » | écran identique (hors D5) ; Jest et axe verts | 2 à 2,5 j |
| **2** | `ChromeThemeService`, sous-menu (et renommage du menu des fonds de page, D1), mémorisation par poste (D2), `DEFAULT_CHROME = 'actuel'`, pose avant amorçage ; tests unitaires | service + menu | choix mémorisé, valeur inconnue → défaut, pas de flash, Jest vert | 1 j |
| **3** | Les quatre thèmes (`comptant`, `assurance`, `prevente-comptant`, `prevente-carnet`) + `theme-contraste.spec.ts` **dont l'empilement navbar / rail / barre de titre** | table SCSS + garde-fou | tous les ratios du §3 tenus par un test ; ordre des bandes tenu pour les 5 thèmes | 1 j |
| **4** | Axe sur 5 thèmes × écrans de référence, **dans les deux dispositions (barre et rail)** ; **barre de titre Tauri** en mode forcé puis sur exécutable ; captures ; relevé de ce qui « jure » ; test responsive sous chaque thème | `docs/` + rapport | zéro défaut ; liste des écarts chiffrée | 1,5 j |
| **5** | **Essais, puis choix du thème par défaut** (D2) : les utilisateurs essayent les cinq thèmes quelques jours sur leur poste ; on relève ceux qu'ils gardent | une ligne changée (`DEFAULT_CHROME`) et le résultat des essais noté ici | un défaut choisi, ou « Actuel » confirmé | 0,5 j + durée des essais |
| **6** | **Accent applicatif lié au thème** (D4, décidé) : boutons primaires, liens, onglets actifs, via `pharma-theme-accent-components` ; examen du couple primaire / « succès » sous le thème 2 | SCSS + captures | contrastes tenus ; primaire et succès discernables | 2 à 3 j |
| **7** *(option)* | Alignement de l'espace de vente sur le thème, **si l'usage montre que c'est nécessaire** (D3) | selon décision | — | à chiffrer |

Total phases 0 à 4 : **environ 6 à 6,5 jours**, sans rien changer pour qui ne touche pas au sélecteur ; avec la phase 6 (D4), **8 à 9,5 jours**. La
phase 5 attend les essais, pas du développement.

## 9. Ce que ce plan ne couvre pas
- Un thème **sombre** : aucun des cinq ne l'est, et les trois fonds de page existants sont tous clairs.
- La mémorisation **par compte utilisateur** (écartée, D2) et la fusion des fonds de page dans les thèmes (écartée, D1).
- Les couleurs de **statut** (succès, danger, avertissement) : elles restent celles de la charte, quel que soit le thème.
- Les **écrans à accents propres** (vente, dépôt, prévente) : leurs accents par type ne changent pas.
- Les graphiques (Chart.js) et les PDF / tickets : hors périmètre.

## 10. Avancement

**Phase 1 — faite le 2026-10-04 (SCSS compilé, rendu non encore vérifié à l'écran).**
- `content/scss/_pharma-chrome-themes.scss` (importé dans `vendor.scss`) : jetons `--pharma-chrome-*` avec le thème `actuel` en valeurs littérales, et
  alias `--pharma-nav-*`. Le bloc `:root` de `global.scss` en a été retiré. `navbar`, `sidebar`, `nav-flyout` n'ont pas changé.
- Branchés sur les jetons : en-tête de modale (`_modal-theme.scss`), en-tête de tableau (`table-common-global.scss`, 2 règles), bandeau d'`app-toolbar`
  (`pharma-toolbar-global.scss`, `.pharma-toolbar-header`), barre de titre Tauri (`titlebar.component.scss`, schémas commentés supprimés).
- D5 appliqué : tableau `#527b95 → #4d748d`.
- **Écarts au plan :**
  - `actuel` a trois dégradés différents (modale `#2e7d97 → #1b5e75`, tableau, toolbar `#5b89a6 → #4a7189`) : en plus de `head-a/b`, deux paires de
    jetons `-table-*` et `-toolbar-*`, qui retombent sur `head-*` quand elles sont absentes (les quatre thèmes dérivés n'auront qu'une paire).
  - Les `!important` des règles d'en-tête sont **conservés** : avec une variable, ils ne figent plus la couleur, et les retirer risque de laisser
    Bootstrap reprendre la main (`.table > :not(caption) > * > *`). À retirer seulement si la phase 3 montre un cas qui résiste.
  - ag-grid : le thème alpine n'a **aucun** en-tête coloré aujourd'hui (gris par défaut) ; le rendre au thème changerait `actuel`. Reporté à la phase 3,
    uniquement sous un thème dérivé.
- **Contrôle du rendu (2026-10-04, pile 4200/9080, barre et rail)** : valeurs calculées dans le navigateur = valeurs d'avant (toolbar `#5b89a6 → #4a7189`,
  barre de titre `#22303d → #2c3e50`, dégradé de la barre `#34506b…`), sauf l'en-tête de tableau `#527b95 → #4d748d` (D5, voulu). Captures d'accueil, catalogue
  produits et point de vente, dans les deux dispositions : aucune rupture visible. Les modales passent par un mixin appliqué par composant : compilation
  vérifiée (`var(--pharma-chrome-head-*)`), pas vue à l'écran.
- **Limites :** pas de captures « avant » (comparaison par valeurs calculées, pas par pixels) ; la barre de titre Tauri n'est contrôlée que par sa valeur
  calculée — forcer `__TAURI_INTERNALS__` fait apparaître l'écran d'attente du backend ; vérification sur exécutable à faire (phase 4).
- Reste de la phase 1 : Jest, axe.

**Phase 2 — faite le 2026-10-04.**
- `core/theme/chrome-theme.ts` (liste des cinq thèmes, `DEFAULT_CHROME = 'actuel'`, clé `pharmasmart_chrome`, lecture tolérante, `applyChrome`) et
  `chrome-theme.service.ts` (signal, `data-chrome`, écoute de l'événement `storage` pour les autres fenêtres). `ThemeService` et sa clé sont intacts.
- `main.ts` pose l'attribut **avant** l'amorçage ; `AppComponent` appelle `loadCurrentChrome()`.
- Menu Compte : l'ancien « Thème » devient **« Fond de page »** (libellé proposé en D1, à confirmer) ; nouveau sous-menu **« Couleur de l'application »**, pastille
  par entrée. **Libellés = noms de couleur seuls** (Bleu acier, Vert, Bleu, Indigo, Olive), décidé le 2026-10-04 : « Vente comptant » ou « Prévente carnet » laissaient croire
  que le thème ne concernait que l'écran de vente ; l'info-bulle « peut ressembler à l'écran de prévente » (D3) est donc retirée du sélecteur. Les identifiants
  internes (`comptant`, `prevente-carnet`…) ne changent pas. Ajout de `swatch` à `NavItem`, rendu par `nav-flyout`.
- Tests : `chrome-theme.service.spec.ts` (7), `navigation.service.spec.ts` complété ; 86 tests verts sur `core/theme`, `core/config/navigation`, `layouts`.
  Vérifié dans le navigateur : attribut `actuel` au chargement, choix mémorisé après rechargement, menu affiché.
- **Sans effet visible pour l'instant** : seul `actuel` existe côté SCSS ; choisir un autre thème pose l'attribut mais ne change rien avant la phase 3.
- Non fait : la page de connexion prend le thème (l'attribut est sur `<html>`, donc oui dès la phase 3, à confirmer en navigateur) ; test de pose avant amorçage.

**Phase 3 — faite le 2026-10-04.**
- `_pharma-chrome-themes.scss` : la table `$pharma-chrome-themes` (quatre accents) et une boucle qui émet `:root[data-chrome='…']`. Un accent par thème, le reste en
  `color-mix` ; les pourcentages sont des variables (`$chrome-bar-accent: 45%`, `-nav-keep: 88%`, `-title-keep: 70%`, `-end-keep: 78%`, `-gradient-keep: 94%`).
  Les thèmes dérivés retirent les paires `table` / `toolbar` propres à `actuel` (une seule paire d'en-tête). ag-grid reçoit son en-tête coloré **sous les thèmes dérivés
  seulement** (`actuel` inchangé).
- **Point de couleur oublié par le plan (le septième)** : `navbar.component.scss` redéfinissait `--pharma-nav-bg` / `-bg-hover` en valeurs fixes (`#3a5269`, `#45617d`) ;
  la barre horizontale ne suivait donc pas. Deux jetons `--pharma-chrome-navbar` / `-navbar-hover` (valeurs d'`actuel` identiques) la rebranchent.
- `core/theme/theme-contraste.ts` + `.spec.ts` : lisent le SCSS (aucune couleur recopiée) et rejouent le calcul. Contrôlent, pour les cinq thèmes : blanc ≥ 4,5:1 sur
  navbar (début, fin, survol), rail (début, fin), barre de titre, en-têtes de modale / tableau / toolbar ; entrée active et repère d'accent ≥ 3:1 ; empilement
  barre de titre < rail < navbar ; infobulle ≥ 7:1 sur les trois fonds de page ; liste du sélecteur = table SCSS ; pastille = accent. 158 tests verts.
- Ratios obtenus : conformes à ceux du §3 (navbar 6,6 à 7,6 ; rail 9,2 à 10,9 ; barre de titre 12,8 à 14,3 ; en-têtes ≥ 5,1).
- **Écart trouvé, non corrigé :** le bandeau d'`app-toolbar` du thème **Actuel** (`#5b89a6`, texte 16 px / 600) donne **3,77:1** — la même couleur que l'ancien en-tête
  de tableau, mais D5 n'a corrigé que le tableau. Le test le tolère (plancher 3,7) en écart connu ; correction possible : `#527b95 → #4d748d`, comme le tableau. **À décider.**
- Vérifié dans le navigateur (barre et rail, cinq thèmes) : couleurs calculées conformes aux accents ; la barre horizontale devient bien indigo (`#4c4f7e`).
  Le thème choisi donne une barre **sourde** (accent à 45 % dans l'ardoise) : le §3 le prévoyait, à juger à l'œil en phase 4–5.

**Retour d'essai (2026-10-04) — thèmes adoucis, aligné sur l'espace de vente.** Les thèmes dérivés étaient « trop saturés et agressifs » face aux espaces de travail
(comptoir, prévente). Changements :
- **En-têtes clairs** (modale, tableau, toolbar, onglets, en-tête de `pharma-nav-sidebar`) : teinte de l'accent à **10 %** sur blanc (5 % en fin de dégradé), texte = accent
  assombri (78 % + noir), filet de 2 px (accent 55 % + blanc), au lieu d'un aplat accent / blanc. Même vocabulaire que l'espace de vente (teinte 14 %, texte foncé, filet),
  en plus léger. Nouveaux jetons : `head-fg` (texte), `head-line` (filet, `box-shadow: inset`, sans effet de mise en page), `head-sep` (séparateur de colonnes),
  `close-filter` (croix des modales), `tab` / `tab-dark` / `tab-tint` (onglets : `.pharma-nav-tabs-container` ne lisait que des variables SCSS figées). `actuel` : valeurs
  inchangées (texte blanc, pas de filet).
- **Barres un peu plus saturées** : accent à **55 %** dans l'ardoise (contre 45 %). L'entrée active des barres passe à `#58d68d` (le vert d'`actuel` éclairci) : à 55 %, la
  barre du thème assurance donnait 2,87:1 avec `#2ecc71`.
- **Différence avec l'espace de vente** : sous un thème autre qu'`actuel`, `.pharma-sales-layout` (sales-home) repose les jetons d'en-tête sur la teinte du **type de vente**
  (comptant, assurance, carnet) : teinte 14 %, filet plein. Les tableaux, bandeaux et onglets de l'espace de vente se distinguent donc du reste de l'application, qui prend la
  teinte plus légère du thème. Les modales (portées par l'overlay, hors de l'espace de vente) suivent toujours le thème.
- `pharma-nav-sidebar` : son en-tête lit les jetons du bandeau d'`app-toolbar`. Les liens gardent leur teinte par position (`--section-hue`, volontairement multicolore) :
  le lien actif reste donc un aplat de sa couleur, quel que soit le thème. **À décider** s'il doit lui aussi prendre l'accent du thème.
- Icône ⓘ des en-têtes de la liste produits : blanche en dur (`.th-info`), invisible sur teinte claire ; elle lit maintenant `head-fg`. D'autres écrans peuvent avoir le même défaut
  (couleurs claires codées en dur dans un en-tête) : relevé à faire en phase 4.
- Garde-fou : texte foncé sur teinte (≥ 4,5:1, 6,5 à 9,1 mesurés), onglet actif sur blanc et sur sa teinte (≥ 4,5:1), entrée active ≥ 3:1. 184 tests verts.

**Suite (2026-10-04).**
- **Bandeau d'`app-toolbar` d'« Actuel » corrigé** : `#5b89a6 → #4a7189` devient `#527b95 → #4d748d` (4,54:1 en début de dégradé), comme le tableau (D5). Deuxième exception à
  « Actuel = identique à aujourd'hui », à annoncer dans les notes de version avec la première. L'écart connu du test est supprimé : plus aucune tolérance.
- **Barre horizontale et rail trop saturés à 55 %** : accent ramené à **40 %** dans l'ardoise (`$chrome-bar-accent`). Blanc sur navbar 6,9 à 7,9:1, sur rail 9,8 à 11,3:1 ;
  entrée active ≥ 3,7:1.

**Deuxième retour d'essai (2026-10-04) — barres adoucies, fond de page et conteneurs.**
- **Barres** : accent ramené de 40 % à **25 %** dans l'ardoise (`$chrome-bar-accent`). 55 % puis 40 % restaient trop saturés.
- **Fond de page suit le thème** (thèmes dérivés seulement) : `--pharma-app-bg` = accent à 6 % dans `#edf1f5` (`$chrome-page-tint`, `$chrome-page-base`), posé par
  `:root[data-chrome='…']` (même spécificité que `data-theme`, gagne par l'ordre d'import). **Conséquence à connaître : sous un thème autre qu'« Actuel », le menu « Fond de page »
  (menthe / ardoise / clair) ne change plus le fond** ; il reste actif sous « Actuel ». Ceci tranche D1 dans le sens demandé le 2026-10-04, à confirmer.
- **Conteneurs sans contour ni élévation** (`.data-card`, `.pharma-toolbar`, `.pharma-nav-sidebar-card`) : sous un thème dérivé, bordure `--pharma-chrome-frame` (accent 28 % dans
  `#cbd5e1`) et ombre `--pharma-elevation-1` (2 au survol de `.data-card`). `.data-card` n'avait aucune bordure et une ombre à 8 %. `actuel` inchangé. Liste volontairement courte ;
  d'autres conteneurs (`.card` nus, panneaux propres à un écran) peuvent encore se fondre dans le fond : à relever écran par écran en phase 4.
- Garde-fou : texte courant ≥ 4,5:1 (7,6 à 7,8) et atténué ≥ 4,5:1 (4,65 à 4,71) sur le fond de page ; carte blanche / fond ≥ 1,12 (1,22 mesuré). 178 tests verts.
- Captures : accueil exclu ; catalogue produits et mouvements de caisse × cinq thèmes × barre / rail, dans le dossier temporaire de la session.

**Troisième retour d'essai (2026-10-04) — barres claires, conteneurs cadrés, `kpi-strip` et `pill-selector`.**
- **Barres claires** (remplace les barres sombres « accent dans de l'ardoise », que 55 %, 40 % puis 25 % n'ont pas suffi à rendre acceptables : le défaut était la
  luminosité, pas seulement la saturation). Sous un thème dérivé : rail = accent à 14 % sur blanc, barre horizontale 9 %, barre de titre 24 % (ordre barre de titre < rail <
  navbar conservé), texte = accent assombri (78 %), entrée active = accent. Nouveau jeton `--pharma-chrome-on` (blanc sous « Actuel », encre sous les thèmes dérivés) : les
  `rgba(255,255,255,x)` et `color: #fff` codés en dur dans `sidebar`, `nav-flyout`, `titlebar` et `navbar` y sont rebranchés (rendu d'« Actuel » identique : mêmes valeurs).
  Les teintes d'icônes de navigation (`--nav-hue-*`, niveau 300 pour fond sombre) passent au niveau 600 assombri sous les thèmes dérivés.
  `navbar-dark` de Bootstrap fixait le texte en blanc : ses variables sont redéfinies par `--pharma-chrome-navlink*`.
- **Conteneurs** : liste élargie à `.card:not(.main)` (155 usages), `.data-card`, `.activity-card`, `.pharma-toolbar`, `.pharma-nav-sidebar-card`, `.list-column`,
  `.detail-column`, `.view-panel` (contour du thème + élévation 1) ; une carte dans une carte n'a pas d'ombre. `.card.main` (racine de page) est exclu : il avait pris un cadre
  autour de toute la page. Fond de page : confirmé par l'utilisateur ; le menu « Fond de page » ne joue donc que sous « Actuel ».
- **`app-pill-selector`** : valeurs d'avant le chantier (`#008cba`, `#888`…) devenues les jetons `--pharma-chrome-pill-*` (`actuel` identique). Thèmes dérivés : piste teintée, pastille
  active en teinte claire + texte foncé (comme les onglets), survol teinté.
- **`app-kpi-strip`** : fond (`--pharma-kpi-bg-*`), filet (`--pharma-kpi-border`) et barre « primaire » (`--pharma-kpi-accent`) lisent le thème ; les barres de statut
  (succès, danger, avertissement, info) gardent leur couleur de charte.
- Garde-fou étendu : texte des barres (navbar, début / fin / survol, rail, barre de titre, panneau) ≥ 4,5:1, texte atténué des barres, pastille (repos, survol, active),
  texte sur le bandeau d'indicateurs ; 276 tests verts sur `core/`, `layouts`, `nav-sidebar`, `pill`, `kpi`, `titlebar`.
- **Non vérifié** : barre de titre Tauri claire (valeur seulement ; ses boutons de fenêtre lisent maintenant `--pharma-chrome-on`, le rouge de fermeture est inchangé) ; modales ; écran
  de vente sous les nouveaux thèmes ; le rail de la capture d'accueil (page d'accueil non rejouée).

**Correctifs du même jour.**
- **Libellés du menu déroulant (flyout) invisibles** : `.flyout-link` fixait son texte à `rgba(236,240,241,.85)` (la valeur d'« Actuel ») et l'en-tête du panneau posait un voile noir à 20 % ;
  sur barre claire, texte clair sur fond clair. Le texte lit maintenant `--pharma-nav-fg-muted`, et les voiles noirs 10 / 20 % du flyout, du rail et de la barre de titre passent par
  `--pharma-chrome-shade` (noir sous « Actuel », encre sous les thèmes dérivés). Les ombres portées et le fond d'overlay restent noirs.
- **Pastille active de `app-pill-selector`** : elle suivait déjà le thème mais ne se distinguait pas de sa piste (teinte 22 % contre 10 %). Teinte portée à 34 % / 26 %, plus un liseré
  intérieur à l'accent. Contraste texte / pastille 4,6 à 6,5:1.
- Garde-fou : 214 tests verts (`core/theme`, `layouts`, `titlebar`).

**Sélecteurs de période et contrôle segmenté migrés vers `app-pill-selector`.** Les blocs `.dashboard-periode-selector` (boutons `.periode-pill`) et `.custom-segment-control`
(curseur coulissant `.segment-slider`) étaient des doublons codés en dur du Design System : ils gardaient le bleu `#008cba` sous tous les thèmes.
- **Migrés** (11 blocs) : `comparative-analysis` (3 vues), `supplier-performance`, `pnl-analytique` (2 sélecteurs), `finance-creances` (tranches, période), `home-base` (période, Pareto,
  Tabulaire / Graphique), `commande-received` (Séquentiel / Grille). Les composants importent `PillSelectorComponent` ; `finance-creances` gagne `FormsModule`.
- **Gardés en HTML** : les cinq liens de l'accueil (« CA avancé », « Marges & Résultat »…), qui sont des `<a routerLink>` et non un choix d'état. Le style `.dashboard-periode-selector`
  / `.periode-pill` lit désormais les mêmes jetons `--pharma-chrome-pill-*` (valeurs d'« Actuel » identiques à `#008cba` / `#5bc0de` / `#888`) : un seul réglage pour les deux.
- **Supprimé** : `.custom-segment-control`, `.segment-track`, `.segment-slider`, `.segment-option` (SCSS global et local de `commande-received`) et leurs requêtes responsive.
- **Perte assumée** : le curseur qui glissait d'une option à l'autre (0,3 s) et les info-bulles `ngbTooltip` de « Séquentiel » / « Grille » (remplacées par le `title` natif portant le libellé).
- Vérifié : compilation des templates (`ngc`) sans erreur ; accueil sous le thème assurance à l'écran. Tests unitaires des composants touchés non confirmés (aucune sortie de Jest sur ce filtre).

**Menu « Fond de page » supprimé (2026-10-04).** Décision de l'utilisateur : le thème de couleur impose le fond de page, le menu menthe / ardoise / clair n'avait plus d'effet sous les thèmes
de couleur. L'entrée est retirée de `NavigationService` (et son test) ; `ThemeService` reste, il pose toujours `data-theme='menthe'` (le fond d'« Actuel »). Ceci clôt D1 :
un seul sélecteur, « Couleur de l'application ». Les deux libellés à confirmer du §4 sont donc caducs.

**Chantier voisin : contenus à placer dans `app-card`** (contour + élévation du Design System, plutôt que des règles globales sur des classes).
- `produit-home` : du hint « premier usage », la barre d'actions groupées et le split panel, dans un `app-card` (`bodyClass="p-0"`, hauteur flexible pour que le split garde son défilement interne).
- `sales-journal` : de `journal-summary-bar` au `app-data-table`, dans un `app-card` ; la barre de synthèse perd son cadre propre et garde un filet inférieur.
- Les autres écrans « sont déjà ok » (retour de l'utilisateur) : non touchés. La règle globale de cadre ne porte plus sur `.list-column`, `.detail-column`, `.view-panel` (ils se placent
  dans une carte) ; elle reste sur `.card`, `.data-card`, `.activity-card`, `.pharma-toolbar` et la carte de `pharma-nav-sidebar`.
- Vérifié : `ngc` sans erreur, 257 tests verts (`core/`, `layouts`), les deux écrans à l'écran sous « Comptant ». Reste à repasser les autres écrans à split panel un par un, au fil de l'usage.

**Chantier `app-card` — passe en masse (2026-10-04).** « 80 % des menus ne sont pas faits » : migration par script (`cartes_masse.py`, hors dépôt) des écrans dont le contenu
principal n'était dans aucune carte.
- **Critère** : racine `.pharma-smart-content`, barre d'outils au premier niveau, contenu avec `app-data-table`, `<table>` ou `split-container`, et **aucune carte** à aucun niveau du contenu.
  Le contenu situé après la barre d'outils est enveloppé dans `<app-card>`.
- **Hors de la carte, toujours** (consigne : « ne mets pas les `app-kpi-item` dans `app-card` ») : bandes `app-kpi-strip` / `app-kpi-item`, bannières `app-*-kpi-banner`, `ngx-spinner`,
  gabarits `ng-template`, modales. Des bandes d'indicateurs avaient d'abord été englobées (`lot-perimes`, `lot-a-detruire`, `avoir`, et les bannières de `differes-home`,
  `facturation-home`, `rapprochement`, `recapitulatif`) : sorties, avec leur `@if` quand il existe. `sales-kpi-dashboard` (tableau de bord d'indicateurs) n'a finalement pas de carte.
- **Padding** : le corps de carte garde son padding par défaut (1 rem) pour les listes ; 0,5 rem (`p-2`) pour les écrans à volets. `p-0` collait tableaux et volets aux bords.
- **Volume** : 35 listes + 7 écrans à volets (+ `produit-home` et `sales-journal` faits à la main) = 43 gabarits, leurs composants gagnent `CardComponent`. Compilation des templates (`ngc`) sans erreur.
- **Non traité (volontairement)** : les éditeurs et formulaires sans tableau (`user-management-update`, `app-config-editor`, `inventory-editor`, `dashboard-editor`, `license-admin`, `facturation-edition`…), les
  tableaux de bord qui ont déjà des cartes, et les écrans sans racine `.pharma-smart-content` (rapports, `commande-*`, `mvt-caisse`…). À regarder un par un.
- **Tests** : Jest `entities|features|admin|home` : 4 suites en échec (`stock-depot`, `achat-depot`, `ajout-perimes` : `ActivatedRoute` sans fournisseur dans le squelette ; `home.component.spec` :
  `zone.js` absent ; `user-management.service.spec` : rôles). Aucun lien visible avec la carte ; non comparé à un état antérieur. E2E non rejoué.

**Phase 4 — avancement (2026-10-04).**
- **Axe sur les thèmes** : `e2e/a11y/themes-chrome.spec.ts` (projet `a11y`, lecture seule, ~6 min). Pour 9 écrans (accueil, catalogue produits, mouvements de caisse, familles, clients, différés,
  journal des ventes, comptoir, modale « nouvelle famille ») × 2 dispositions × 5 thèmes, il relève les éléments en défaut de contraste sous « Actuel » (référence) puis sous chaque thème ;
  **seuls comptent les défauts absents sous « Actuel »**. Résultat : **0 défaut introduit par un thème**. Mesuré : 122 défauts sous « Actuel » contre 104 sous chaque thème de couleur
  (ex. pastilles au repos à 2,98:1 sous « Actuel », corrigées par les thèmes) ; ces défauts communs préexistent (`.last-update-label` 2,88:1, `text-primary` 3,84:1 — ce dernier relève
  de la phase 6, accent applicatif). Limite d'axe : texte sur dégradé non évalué (couvert par `theme-contraste.spec.ts`).
- **Vu à l'écran** : espace de vente avec un panier (comptant, prévente carnet), modale « nouvelle famille » (en-tête clair du thème), page de connexion.
- **Connexion** : seule la barre du haut suit le thème ; le fond bleu dégradé et la carte de la page sont propres à cet écran et ne changent pas. À décider si on les aligne.
- **Reste de la phase 4** : barre de titre Tauri sur l'exécutable (`npm run tauri:dev`) ; test responsive sous chaque thème ; relevé des couleurs figées (89 fichiers `$pharma-primary`,
  16 fichiers en dur) ; regroupements `app-card` à revoir écran par écran ; captures « avant » d'« Actuel » jamais prises.
- **Responsive sous un thème** : `comptoir-responsive.spec.ts` accepte `E2E_CHROME=<thème>`. Rejoué sous **« Comptant »** : 9 tailles sur 9 passent (la page ne défile pas, le règlement reste à l'écran,
  onglets de 44 px). Les autres thèmes ne sont pas rejoués : seule la couleur change d'un thème à l'autre, pas les hauteurs.

**Barres de menu et menus déroulants : conformité WCAG 2.2 AA mesurée (2026-10-04).**
- **Nouveau test navigateur** `e2e/a11y/themes-menus.spec.ts` (projet `a11y`, lecture seule, ~2 min) : 5 thèmes × 2 dispositions, sur ce qui est réellement rendu.
  1.4.3 / 1.4.11 contraste du texte (4,5:1) et des icônes (3:1) **sur les pixels du fond** (texte rendu transparent, page photographiée) — repos, survol de chaque entrée, focus, menu
  déroulant ouvert et sa ligne survolée ; 2.4.7 / 1.4.11 le focus clavier doit ajouter un signe visible à 3:1 de ce qu'il recouvre ; 1.4.1 l'entrée active se distingue autrement que par la
  couleur ; 2.5.8 cibles de 24 × 24 px ; 2.1.1 / 2.1.2 / 2.4.3 clavier (Entrée ouvre, Échap ferme et rend le focus) ; 4.1.2 règles WCAG 2 A/AA d'axe sur les barres et le menu ouvert.
  Acceptation : un thème échoue pour tout défaut absent sous « Actuel » ; **résultat final : 0 défaut, « Actuel » compris** (10 combinaisons).
- **Calcul** (`theme-contraste.spec.ts`) : ajout des 12 icônes colorées de navigation sur barre horizontale, rail, panneau et survol (3:1). A relevé le **lime à 2,99:1 au survol** du thème vert :
  icônes assombries à 78 % (`$nav-hue-keep`) ; le texte des barres prend une encre propre, plus foncée que celle des en-têtes (`$chrome-bar-ink: 66 %`), pour tenir 4,5:1 sur les fonds de survol.
- **Défauts réels trouvés et corrigés :**
  - **Focus clavier** : l'anneau Bootstrap des entrées de la barre horizontale ne donnait que **1,7:1 sous « Actuel »** et le rail n'avait **aucun signe de focus** (2.4.7). Anneau de 2 px
    (`--pharma-chrome-focus` : blanc sous « Actuel », encre sous les thèmes) sur les deux barres. **Changement visible sous « Actuel »**, à annoncer avec les deux corrections de contraste.
  - Pastilles (`.navbar-badge`, `.sidebar-badge`, `.flyout-badge`) : texte blanc explicite (il héritait l'encre du thème).
  - Rail : bloc utilisateur et en-tête du menu déroulant, voile gris trop dense (`--pharma-chrome-panel-head`) ; rôle de l'utilisateur et version, textes semi-transparents rendus opaques ou plus denses.
- **Faux positifs écartés** : texte en dégradé (`background-clip: text`, logo du rail) non mesurable par pixels (les trois teintes sont couvertes par le calcul des icônes) ; pixels de bord des
  pastilles arrondies (zone mesurée resserrée de 10 à 18 %).
- **Boîte de confirmation** (`ngb-confirm-dialog`) : suit le thème (jetons `--pharma-chrome-dlg-*`, valeurs d'« Actuel » inchangées) ; vérifiée à l'écran, non mesurée au contraste.
- Garde-fou unitaire : 449 tests verts (`core/`, `layouts`, `titlebar`).
- **Toujours non couvert** : la barre de titre Tauri (jamais vue à l'écran), 1.4.4 / 1.4.10 (agrandissement à 200 %, reflow), 1.4.12 (espacement du texte), 1.4.13 (contenu au survol), daltonisme des thèmes.

**Barre de titre Tauri — validée par l'utilisateur (2026-10-04)** : « le header Tauri est OK, ça suit bien le thème ». Vérifiée sur l'exécutable ; le point « Tauri sur exécutable » de la phase 4 est clos.
Reste de la phase 4 : relevé des couleurs figées (89 fichiers `$pharma-primary`, 16 en dur), 1.4.4 / 1.4.10 / 1.4.12 / 1.4.13 et daltonisme des thèmes. Puis phases 5 (essais, défaut) et 6 (accent applicatif).

**Phase 6 — accent applicatif, et `shared/ui` + sélecteur de date au thème (2026-10-04).**
- **Accent applicatif** (`_pharma-chrome-themes.scss`, dernière section) : sous un thème dérivé, `--pharma-accent`, `--bs-primary`, `--p-primary-*` (échelle 50 à 800), `--pharma-focus-ring`, les liens et `.btn-primary`,
  `.btn-outline-primary`, cases cochées, pagination, onglets `nav-pills`, `dropdown-menu`, `list-group`, `progress` prennent l'accent du thème. Valeurs calculées à la compilation (Bootstrap veut des composantes RGB).
  Choix d'ingénierie : le **texte** « primaire » (liens, `.text-primary`, `--bs-primary-rgb`) prend l'**encre** de l'accent (78 %), pas l'accent : l'accent seul donnait 3,84:1 (`.text-primary`) sur le fond de page teinté.
  Boutons : survol à 80 % et actif à 66 % de l'accent (`$chrome-btn-hover`, `$chrome-btn-active`).
- **Garde-fou** : bouton primaire (repos 5,1 à 7,3:1 ; survol ; actif), bouton contour, liens (repos, survol, sur blanc et sur fond de page) ≥ 4,5:1 ; accent sur blanc ≥ 3:1. 390 tests de thème verts.
- **Composants `shared/ui`** : tout ce qui lit `--bs-primary` / `--p-primary-*` / `--pharma-accent` suit désormais (boutons, cases, pagination, `float-label`, `input-number`, surlignage de grille…). Couleurs figées rebranchées :
  `app-hint` (`--pharma-chrome-hint-*`), `app-bulk-action-bar` (`--pharma-chrome-bulk-*`), filet de total de `app-data-table` (`--pharma-chrome-rule`). Valeurs d'« Actuel » identiques. Non touchés, car ce sont des
  couleurs de **statut** ou neutres : `detail-field` (vert / orange / rouge), `detail-section`, ombres.
- **Sélecteur de date** (`pharma-date-picker`) : sa palette « identique à la boîte de confirmation » lit maintenant les mêmes jetons `--pharma-chrome-dlg-*` (+ `--pharma-chrome-dlg-accent-hover`). Vu à l'écran, calendrier ouvert.
  **Survol de l'icône calendrier corrigé** : la règle posait `background-color` alors que le dégradé de repos (`background-image`) restait par-dessus, d'où une icône blanche illisible ; le survol est maintenant une teinte
  pleine du thème avec l'icône à la couleur du thème (raccourci `background`).
- **Axe, après ces changements** : toujours **0 défaut introduit par un thème** (122 sous « Actuel », 104 sous chaque thème de couleur) ; `themes-menus` : 0 défaut.
- **À regarder** : thème vert, où le primaire (`#047857`) et le « succès » de la charte (`#15803d`) se ressemblent (validation et réussite côte à côte) ; focus des champs `.form-control` (bordure Bootstrap compilée) ;
  surlignages `ng-select`.
- **Jest `shared/`** : 6 suites en échec (`button` : `.shadow-sm` attendu mais classe `app-btn-raised` ; `float-label` : `.form-floating` absent ; `data-table` : sélecteur de taille de page ; `alert`, `sort`, `sort-by` :
  `zone.js`). Ce sont des attentes de balisage, sans rapport avec une couleur ; non comparées à un état antérieur.

**Boutons : création en `primary`, sévérités nuancées par thème (2026-10-04).**
- 13 boutons de création des barres d'outils passés de `severity="success"` à `primary` (« Nouveau… », « Vente Comptant »…). « Excel » (export) laissé en l'état.
- `secondary`, `success`, `info`, `warning`, `danger`, `help`, `contrast` — pleins et contour — suivent le thème : chaque couleur de la charte est mélangée à 12 % avec l'accent (20 % pour `contrast`), table
  `$pharma-chrome-buttons`. Sens conservé. Deux exceptions où le mélange restait trop proche du primaire (ΔE CIE76 < 25) : `success` du thème vert → `#4d7c0f` (ΔE 38), `info` du thème bleu → `#0f766e` (ΔE 34).
  Garde-fou : contraste texte / fond (repos, survol, actif), contour sur blanc et sur fond de page ≥ 4,5:1, écart au primaire ≥ 25 pour `success`, `info`, `help` ; 542 tests de thème verts. `info` a été nuancé aussi, bien que non demandé :
  laissé tel quel il aurait ressemblé au primaire du thème bleu.
- Organisation des boutons par importance (standards UX et WCAG 2.2) : [PLAN-ORGANISATION-BOUTONS-ACTION.md](PLAN-ORGANISATION-BOUTONS-ACTION.md), **à valider** (six décisions au §8).

**Boutons : une couleur propre à chaque thème, et une sévérité par action (2026-10-04).**
- Les sévérités ne sont plus un simple mélange à 12 % avec l'accent (écarts de 3 à 11 ΔE d'un thème à l'autre : quasi identiques). Palette recalculée sous contraintes : même famille de teintes,
  décalage différent par thème, saturation réduite d'environ 15 %, contraste blanc ≥ 4,6:1 (encre ≥ 7:1 sur ambre), ΔE ≥ 30 au primaire du thème. Nouveau test : **couleur distincte d'un thème à l'autre (ΔE ≥ 12)** pour chaque sévérité.
- 84 boutons alignés sur une sévérité de référence par action (« Rechercher » `info`, « Enregistrer » `primary`, exports `warn`…), table au §10 de [PLAN-ORGANISATION-BOUTONS-ACTION.md](PLAN-ORGANISATION-BOUTONS-ACTION.md).
- 556 tests de thème verts.

**Composant `app-subtab-bar` (2026-10-04)** — les blocs `.su-source-tabs-bar` / `.su-tab` / `.su-tab-badge`, recopiés dans `suggestions-unified` et `bed-home`, deviennent un composant du Design System
(`shared/ui/subtab-bar`, exporté par `shared/ui/index.ts`, ligne ajoutée au `README` du Design System).
- API : `[tabs]` (`id`, `label`, `icon`, `tooltip`, `badge`, `badgeSeverity`), `[active]` / `(activeChange)`, `ariaLabel`, `idPrefix`. Pastille : `null` ou absente = masquée, `0` = affichée.
- Accessibilité (patron ARIA « Tabs », activation automatique) : `role="tablist"` / `tab`, `aria-selected`, tabulation itinérante, flèches, Début, Fin ; anneau de focus net (l'ancien halo à 35 % ne se voyait pas).
- Thème : soulignement `--pharma-chrome-tab`, texte actif et survol `--pharma-chrome-tab-dark`, **fond de survol `--pharma-chrome-tab-tint`**, pastilles `--pharma-chrome-badge-*` (palette des boutons du thème).
  Sous « Actuel », les pastilles prennent les couleurs de la charte : les anciennes (`#22c55e`, `#f59e0b`, `#3b82f6`, blanc dessus) donnaient 2,1 à 3,7:1 ; le texte actif passe de `#5b89a6` (3,8:1) à `#4a7189` (5,2:1).
- Partial `_subtab-bar.scss` : les classes d'onglets sont retirées (il garde `.su-parent-toolbar`, `.su-title`, `.su-header-end`). `retour-fournisseur`, qui n'utilisait que le titre, n'est pas touché.
- Garde-fou : texte actif, texte inactif (`#6b7280`, 4,83:1), texte sur fond de survol et chaque pastille ≥ 4,5:1, pour les cinq thèmes ; 8 tests unitaires du composant ; 604 tests de thème et de composant verts.
- Non vérifié à l'écran : `bed-home` (seul `suggestions-unified` a été capturé).

