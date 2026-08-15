# Décommissionnement de Bootswatch (thème yeti)

**Statut :** **terminé** le 15/08/2026. Phase 1 (retrait de la dépendance) et phase 2
(lots B1 à B4) livrées.
**Périmètre :** `pharmaSmart-app/src/main/webapp/content/scss/`

---

## 1. Phase 1 — Retrait de la dépendance ✅

### 1.1 Ce qui a été fait

`bootswatch` n'était pas une dépendance oubliée : c'était **la couche de thème de
l'application**. Elle fournissait deux choses, désormais internalisées :

| Fichier créé | Remplace | Contenu |
| --- | --- | --- |
| `_pharma-bootstrap-yeti-vars.scss` | `bootswatch/dist/yeti/variables` | Variables Sass, `!default` conservés |
| `_pharma-bootstrap-yeti-tweaks.scss` | `bootswatch/dist/yeti/bootswatch` | 344 lignes de surcharges de composants |

Importés exactement là où yeti l'était : les vars entre `pharma-bootstrap-palette` et
`bootstrap/scss/variables`, les tweaks après `bootstrap/scss/bootstrap`.

Puis `npm uninstall bootswatch`.

### 1.2 Vérification

| Étape | `styles.css` |
| --- | --- |
| Build de référence, avec yeti | 465 047 octets |
| Build avec les partials internalisés, bootswatch encore installé | **identique** |
| Build après `npm uninstall bootswatch` | **identique** |

Zéro octet d'écart. Le portage a été fait **verbatim, sans trier** : le retrait de la
dépendance et l'élagage du thème sont deux décisions distinctes, les mélanger aurait rendu
le diff inexploitable.

### 1.3 Effets de bord traités

Trois commentaires devenus faux, corrigés : `_pharma-bootstrap-palette.scss` (×2, ordre
d'import et provenance du teal) et `split-button.component.ts:77` (référence au fichier
d'origine).

---

## 2. Phase 2 — Élaguer le thème internalisé

Chaque valeur des deux fichiers est un **choix de yeti, pas de Pharma-Smart**. La phase 2
consiste à décider, ligne par ligne, ce que le projet assume.

### 2.1 Le point le plus important : `$min-contrast-ratio`

Bootstrap est à **4.5** (seuil WCAG AA). Yeti l'abaisse à **1.9**. Cette variable pilote
`color-contrast()`, qui choisit texte noir ou blanc sur chaque `.btn-*`, `.bg-*` et
`.badge`.

Contrastes réels des variantes du Design System (palette Aura) :

| Variante | Texte blanc | Texte noir | Choix à 1.9 (actuel) | Choix à 4.5 | WCAG AA |
| --- | --- | --- | --- | --- | --- |
| `.btn-primary` (émeraude `#10b981`) | **2,54** | 8,28 | blanc | noir | ❌ |
| `.btn-success` (`#22c55e`) | **2,28** | 9,22 | blanc | noir | ❌ |
| `.btn-info` (`#0ea5e9`) | **2,77** | 7,58 | blanc | noir | ❌ |
| `.btn-warning` (`#f97316`) | **2,80** | 7,49 | blanc | noir | ❌ |
| `.btn-danger` (`#ef4444`) | **3,76** | 5,58 | blanc | noir | ❌ |
| `.btn-help` (`#a855f7`) | **3,96** | 5,31 | blanc | noir | ❌ |
| `.btn-dark` (`#333`) | 12,63 | 1,66 | blanc | blanc | ✅ |
| `.btn-secondary` (`#cbd5e1`) | 1,48 | 14,14 | noir | noir | ✅ |

**Six variantes sur huit affichent aujourd'hui du texte blanc sous le seuil WCAG AA.**
C'est une régression d'accessibilité héritée, que personne n'a choisie : yeti a été retenu
pour son apparence, avec une palette (teal `#008cba`) qui n'est plus celle du projet.

**Mais la corriger est un changement très visible.** Passer à 4.5 :
- fait basculer six variantes au texte noir ;
- **inverse le sens du survol** — le mixin `button-variant` de Bootstrap dérive la nuance
  de survol de la couleur de texte : assombrissement avec texte blanc, éclaircissement avec
  texte noir. `.btn-primary` passerait de `rgb(14,157,110)` au survol à `rgb(52,196,148)`.

C'est un arbitrage produit, pas une décision technique. Trois options :

| Option | Effet |
| --- | --- |
| **Garder 1.9** | Statu quo. La non-conformité AA reste, à assumer explicitement. |
| **Passer à 4.5** | Conforme. Six variantes changent d'aspect, survol inversé. |
| **Assombrir la palette** | Garder le texte blanc *et* la conformité, en fonçant les couleurs de fond (émeraude 600/700 au lieu de 500). Préserve l'aspect ; demande de revoir l'alignement sur Aura. |

### 2.2 Règles devenues sans objet

Usage réel mesuré dans les gabarits :

| Règle de `_pharma-bootstrap-yeti-tweaks.scss` | Usages | Verdict |
| --- | --- | --- |
| `.progress[value]` | **0** `<progress>` | À supprimer |
| `.blockquote-footer` | **0** | À supprimer |
| `.help-block`, `.form-control-feedback` | **0** | À supprimer (sélecteurs Bootstrap 3) |
| `.bg-dark .dropdown-menu` | **0** `bg-dark` | À supprimer |
| `.bg-primary .dropdown-menu` | 24 `bg-primary` | À vérifier : la navbar utilise `.navbar-chrome`, plus `.bg-primary` |
| `.btn-group .dropdown-toggle.btn-* ~ .dropdown-menu` (19 règles) | — | **`app-split-button` les neutralise déjà** avec `!important` (`split-button.component.ts:84`). Les supprimer permettrait aussi de retirer cette contre-mesure. |
| `.control-label` | 6 | Conserver |
| `.nav-pills`, `.breadcrumb`, `.pagination`, `.list-group`, `.popover-header` | 18 / 19 / 77 / 35 / 20 | Conserver |

Le cas `.btn-group ~ .dropdown-menu` est le plus intéressant : une règle du thème et une
contre-règle du Design System s'annulent mutuellement. Supprimer les deux allège le CSS
**et** un commentaire de 7 lignes dans `split-button.component.ts`.

### 2.3 Incohérences de style héritées

| Variable | Valeur yeti | Tension |
| --- | --- | --- |
| `$card-inner-border-radius: 0` | angles droits | Le projet a choisi les rayons Aura (6 / 4 / 8 px) pour boutons et champs. Les cartes gardent des angles internes carrés. |
| `$badge-padding-x: 1rem` | badges très larges | À confronter aux badges du Design System |
| `$headings-font-weight: 300` | titres très fins | Esthétique yeti, à assumer ou non |
| `$gray-*` | `#eee`, `#888`, `#222`… | Diffèrent des gris Bootstrap (`#e9ecef`, `#6c757d`, `#212529`) sans raison documentée |
| `$blue: #008cba` | teal historique | Abandonné pour la navigation (cf. [PLAN-HARMONISATION-NAVBAR-SIDEBAR.md](PLAN-HARMONISATION-NAVBAR-SIDEBAR.md)) mais alimente encore `$blue-100`…`$blue-900` et la map `$colors` |

---

## 3. Découpage proposé pour la phase 2

| Lot | Contenu | Risque |
| --- | --- | --- |
| **B1 — Code mort** | Supprimer `.progress[value]`, `.blockquote-footer`, `.help-block`, `.form-control-feedback`, `.bg-dark`. Vérifier puis traiter `.bg-primary`. | Faible — 0 usage mesuré |
| **B2 — Annulation mutuelle** | Supprimer les 19 règles `.btn-group … ~ .dropdown-menu` **et** la contre-mesure de `split-button.component.ts` | Faible — se vérifie visuellement sur `app-split-button` |
| **B3 — Contraste** | Arbitrage §2.1, puis application | **Élevé** — décision produit, très visible |
| **B4 — Cohérence** | `$card-inner-border-radius`, `$badge-padding-x`, `$headings-font-weight`, gris | Moyen — diffus |

B3 doit être tranché par le produit, pas par la technique. B4 est un chantier de design
system, à faire avec la migration Aura.

### 3.1 B1 et B2 — livrés

`styles.css` : **465 047 → 461 028 octets** (−4 019). Le diff ne contient **que des
suppressions**, toutes ciblées :

- **B1** — `.bg-primary .dropdown-menu`, `.bg-dark` (bloc entier), `.bg-light .dropdown-menu`,
  `.blockquote-footer`, `.progress[value]`, et `.help-block` / `.form-control-feedback`
  retirés du sélecteur de formulaires. `.checkbox` (1 usage) et `.radio` (14) y sont
  conservés.
- **B2** — les 24 règles compilées issues des six blocs
  `.btn-group .dropdown-toggle.btn-* ~ .dropdown-menu`, **et** la contre-mesure de
  `split-button.component.ts` (13 lignes de `!important` plus 8 lignes de commentaire).
  Vérifié absent du bundle : `dropdown-toggle.btn-info` et `dropdown-toggle ~ .dropdown-menu`
  y comptent désormais 0 occurrence.

> **Changement visuel assumé sur B2.** La contre-mesure forçait `--bs-body-bg`,
> `--bs-border-color` et `--bs-tertiary-bg` ; sans elle, le menu retombe sur les défauts
> Bootstrap (`$dropdown-border-color`, `$dropdown-link-hover-bg`), dont les valeurs
> diffèrent légèrement. Le menu d'un `app-split-button` ressemble désormais à **tous les
> autres menus de l'application**, ce qui n'était pas le cas auparavant. À contrôler de
> visu sur un écran qui en utilise un.

`sass-migrator`, installé pour la tentative décrite dans
[PLAN-MIGRATION-SASS-USE.md](PLAN-MIGRATION-SASS-USE.md) §4, a été désinstallé.

### 3.2 B4 — livré

Trois valeurs retirées de `_pharma-bootstrap-yeti-vars.scss`, la quatrième délibérément
conservée. Diff de `styles.css` : **4 lignes**, 461 028 → 461 085 octets.

| Variable | Avant (yeti) | Après (défaut Bootstrap) | Portée visuelle |
| --- | --- | --- | --- |
| `$card-inner-border-radius` | `0` | `calc(var(--bs-border-radius) - var(--bs-border-width))` | 158 `.card-header`, 15 `.card-footer` |
| `$headings-font-weight` | `300` | `500` | 359 titres (`h1`…`h6` et `.h1`…`.h6`) |
| `$badge-padding-x` | `1rem` | `0.65em` | 833 badges |

> **Le diff CSS ne reflète pas l'impact visuel**, contrairement à B1 et B2. Quatre lignes
> changées, mais elles portent sur plus de 1300 éléments. À contrôler de visu ; chaque
> point se rétablit en remettant une ligne.

Justifications :

- **`$card-inner-border-radius`** — le seul vrai défaut de cohérence, et non un choix
  esthétique. Cohérent chez yeti, où *tous* les rayons valent 0 ; ici la carte avait un
  contour arrondi à 6 px (Aura) et des angles internes carrés, l'en-tête et le pied
  débordant visuellement des coins.
- **`$headings-font-weight`** — la graisse « light » est la signature de yeti, à
  contre-courant du preset Aura (titres en 600) vers lequel le Design System migre.
- **`$badge-padding-x`** — des badges presque en pilule, coûteux en place dans une
  application dense. Les badges de navigation (`.sidebar-badge`, `.navbar-badge`) ne sont
  pas concernés : ils fixent déjà leur propre padding.

#### Les gris : volontairement non touchés

L'échelle `$gray-*` de yeti (`#eee`, `#888`, `#222`…) diffère de celle de Bootstrap
(`#e9ecef`, `#6c757d`, `#212529`…). L'écart est **imperceptible** ; la retirer produirait
un diff massif pour aucun bénéfice visible.

Le vrai problème des gris est ailleurs, et il est sérieux : **14 fichiers redéfinissent
`$gray-*`**, la plupart sans `!default`, avec des échelles incompatibles —
`_pharma-nav.scss` en pose une d'inspiration Tailwind (`$gray-500: #6b7280`) là où yeti
donne `#adb5bd`. Sur 455 usages répartis dans 44 fichiers, le sens de `$gray-500` dépend
de l'ordre des `@import`.

C'est exactement le défaut de portée globale décrit dans
[PLAN-MIGRATION-SASS-USE.md](PLAN-MIGRATION-SASS-USE.md) §4.1. Le corriger suppose de
renommer ces échelles et de reprendre leurs consommateurs : cela relève du lot S1/S2 de ce
plan-là, pas d'un élagage de thème.

### 3.3 B3 — livré (option 3 : foncer la palette)

Des trois issues du §2.1, c'est la troisième qui a été retenue : **conserver le texte blanc
et foncer les fonds** jusqu'à ce qu'il devienne légitime.

Nuance la plus claire de chaque famille Aura dont le ratio avec le blanc atteint 4,5 :

| Severity | Avant | Ratio | Après | Ratio |
| --- | --- | --- | --- | --- |
| `$primary` | emerald-500 `#10b981` | 2,54 ❌ | **emerald-700 `#047857`** | 5,48 ✅ |
| `$success` | green-500 `#22c55e` | 2,28 ❌ | **green-700 `#15803d`** | 5,02 ✅ |
| `$info` | sky-500 `#0ea5e9` | 2,77 ❌ | **sky-700 `#0369a1`** | 5,93 ✅ |
| `$warning` | orange-500 `#f97316` | 2,80 ❌ | **orange-700 `#c2410c`** | 5,18 ✅ |
| `$danger` | red-500 `#ef4444` | 3,76 ❌ | **red-600 `#dc2626`** | 4,83 ✅ |
| `$pharma-help` | purple-500 `#a855f7` | 3,96 ❌ | **purple-600 `#9333ea`** | 5,38 ✅ |

`$secondary`, `$light`, `$dark` et `$pharma-contrast` étaient déjà conformes et n'ont pas
bougé. **Les dix variantes de bouton passent désormais AA**, de 4,83 à 20,17.

Trois fichiers modifiés :

1. `_pharma-bootstrap-palette.scss` — les six severities, avec le tableau de contrastes en
   commentaire.
2. `_pharma-tokens.scss` — `--p-primary-color: var(--p-primary-500)` → `var(--p-primary-700)`.
   **Indispensable** : 44 règles de l'application consomment `var(--p-primary-color)` ;
   sans ce déplacement, elles seraient restées à l'émeraude 500 et l'application aurait eu
   deux primaires. L'échelle numérique `--p-primary-*` reste fidèle à Aura — seul le token
   sémantique bouge.
3. `_pharma-bootstrap-yeti-vars.scss` — `$min-contrast-ratio: 1.9` retiré, retour au défaut
   Bootstrap (4,5).

#### Ce que ce lot garantit, et ce qu'il ne garantit pas

Le seuil étant revenu à 4,5 **et** la palette étant conforme, `color-contrast()` continue
de choisir le blanc — mais désormais parce qu'il le mérite. Toute évolution future de la
palette sera vérifiée à la compilation : une couleur trop claire fera basculer Bootstrap au
texte noir, ce qui se verra immédiatement.

En revanche, **l'aspect général de l'application change** : le primaire passe d'une émeraude
vive à un vert profond.

#### Le bouton d'avertissement, traité à part

Appliquée à `warning`, la recette « foncer le fond » donnait un orange brûlé (`#c2410c`) :
conforme, mais un avertissement cesse de se lire comme tel quand il est aussi sombre qu'une
erreur. La convention établie pour ce cas est l'inverse — **fond clair ambré, texte foncé**.

`.btn-warning` est donc repris dans un nouveau fichier, `_pharma-bootstrap-tweaks.scss`,
distinct du portage yeti et destiné aux surcharges Bootstrap **propres au projet** :

```scss
$pharma-btn-warning-bg: #fbbf24; // --p-amber-400

.btn-warning {
  @include button-variant($pharma-btn-warning-bg, $pharma-btn-warning-bg);
}
```

Résultat compilé : `--bs-btn-bg: #fbbf24`, `--bs-btn-color: #000` (12,58 de ratio),
survol *éclairci* et non assombri — cohérent avec toute variante à texte foncé. La couleur
du texte n'est écrite nulle part : `color-contrast()` la dérive, et retient le noir parce
que le blanc échoue au seuil de 4,5. C'est le mécanisme rétabli par B3 qui travaille pour
nous.

Deux points qu'il a fallu traiter au passage :

- **`$warning` n'est pas modifié** et reste à l'orange foncé. 176 usages de `.text-warning`
  affichent cette couleur en **texte sur fond clair**, où l'ambre tomberait à 1,67 de
  ratio ; l'orange foncé y tient 5,18. `.btn-outline-warning` (2 usages) reste également
  sur `$warning`, ce qui est correct : bordure et libellé ambrés sur blanc seraient
  illisibles.
- **Le bloc `&-warning` de `_pharma-bootstrap-yeti-tweaks.scss` a été supprimé.** Il posait
  `color` et `border-color` en déclarations directes, qui l'emportent sur les custom
  properties générées par `button-variant` — le bouton serait resté à texte blanc et
  bordure orange.

C'est la seule severity où ce traitement s'impose : l'ambre ne fonctionne pas en texte
blanc, et le rouge ou le vert ne fonctionnent pas en fond clair.

---

## 4. Méthode de vérification

Identique à la phase 1, et c'est elle qui l'a rendue sûre :

1. Build, copier `pharmaSmart-app/target/classes/static/styles.css`.
2. Modifier.
3. Rebuild, `diff` les deux fichiers.

Pour B1 et B2, le diff doit montrer **uniquement** la disparition des règles visées. Pour
B3, il sera large par construction — c'est le but ; l'inspection visuelle prend alors le
relais.

---

## 5. Lien avec les autres chantiers

- [PLAN-MIGRATION-SASS-USE.md](PLAN-MIGRATION-SASS-USE.md) — `vendor.scss` et `global.scss`
  garderont `@import` : Bootstrap 5.3 ne supporte pas `@use` avec configuration, et c'est
  précisément ce mécanisme (`!default` avant import) qui fait fonctionner les deux fichiers
  décrits ici.
- [PLAN-HARMONISATION-NAVBAR-SIDEBAR.md](PLAN-HARMONISATION-NAVBAR-SIDEBAR.md) — le teal
  `#008cba` de yeti a été abandonné pour le chrome de navigation, au profit des tokens
  `--pharma-nav-*`.
