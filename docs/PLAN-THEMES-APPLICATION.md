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
