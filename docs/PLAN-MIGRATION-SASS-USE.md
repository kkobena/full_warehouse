# Plan — Sortir de `@import` (Sass) et dédupliquer les styles partagés

**Statut :** **S0, S1 et S2 (quasi complet) implémentés et vérifiés** (build + comptage
de sélecteurs témoins, cf. §10). Sur S2, `table-common` (S1), `kpi-strip`, `data-card`,
`dashboard-common`, `pharma-nav`, `pharma-nav-tabs`, `form-styles`, `action-bar` et
`pharma-toolbar` sont faits. `modal-theme` n'est plus en `@import` (converti en `@use`,
cf. §10.4) mais reste dupliqué par `@include` — nature différente, cf. §10.4. S3, S4
restent à faire pour le reste du reliquat — une tentative de migration automatique
globale avait été faite puis **intégralement annulée** (§4) ; l'approche retenue est
désormais le découpage manuel lot par lot du §6.
**Périmètre :** `pharmaSmart-app/src/main/webapp/**/*.scss`
**Origine :** l'avertissement `Sass @import rules are deprecated and will be removed in
Dart Sass 3.0.0.` émis à chaque build

---

## 1. Résumé

Deux problèmes distincts partagent la même cause, et se résolvent par le même travail :

| Problème | Symptôme mesuré |
| --- | --- |
| **`@import` déprécié** | 1005 avertissements par build ; Dart Sass 3.0 cassera la compilation |
| **Duplication des styles partagés** | `.pharma-toolbar-filters` apparaît **3180 fois** dans le bundle |

La cause commune : des partials qui **émettent du CSS *et* exportent des membres**
(variables, mixins), consommés par la portée globale de `@import`.

**Conclusion de l'analyse : on ne peut pas migrer vers `@use` sans d'abord corriger
l'architecture SCSS.** Le blocage n'est pas syntaxique.

---

## 2. État des lieux

### 2.1 Volumétrie

- **510 directives `@import`** dans ~230 fichiers SCSS (368 fichiers `.scss` au total).
- Les cibles, par fréquence :

| Partial | Importé | Poids source | Blocs de règles CSS | Variables |
| --- | --- | --- | --- | --- |
| `app/shared/scss/table-common` | 128× | 17 Ko | ~138 | 124 |
| `app/shared/scss/pharma-toolbar` | 99× | 28 Ko | ~192 | 170 |
| `app/shared/scss/modal-theme` | 55× (2 chemins) | 6,6 Ko | — | — |
| `app/shared/scss/dashboard-common` | 39× | 16 Ko | — | — |
| `app/shared/scss/pharma-nav` | 43× (2 chemins) | 8 Ko | ~54 | 47 |
| `app/shared/scss/kpi-strip` | 28× | 2,5 Ko | — | — |
| `app/shared/scss/form-styles` | 44× (2 chemins) | 11 Ko | — | — |

### 2.2 Les avertissements

Relevé sur un build de développement complet :

| Dépréciation | Occurrences |
| --- | --- |
| `Sass @import rules are deprecated` | **1005** |
| `Global built-in functions are deprecated` | 97 |
| `darken() is deprecated` | 52 |
| `lighten() is deprecated` | 44 |

**Tous proviennent de notre code.** Le log ne cite `node_modules` que 9 fois, et jamais
pour du SCSS : Angular tait déjà les dépréciations internes aux dépendances. Il n'y a donc
pas de « bruit de bibliothèque » à écarter — tout est actionnable, et tout est à nous.

### 2.3 La duplication

Chaque `styleUrl` de composant Angular est une **unité de compilation séparée**. Un partial
qui émet du CSS est donc recopié intégralement dans chaque composant qui l'importe.
Mesuré dans le bundle construit (37 Mo de JS + CSS) :

```
.pharma-toolbar-filters : 3180 occurrences
.pharma-filter-search   : 3150 occurrences
.pharma-table-wrapper   :  348 occurrences
```

> ⚠ **`@use` ne corrige pas ce point.** Il déduplique *à l'intérieur* d'une unité de
> compilation, pas entre elles. Après migration, ce serait toujours 3180. Ne pas attendre
> de gain de taille de la seule migration syntaxique.

---

## 3. Configuration actuelle

```jsonc
// angular.json → architect.build.options
"stylePreprocessorOptions": {
  "includePaths": ["pharmaSmart-app/src/main/webapp"]
}
```

C'est ce chemin qui permet `@import 'app/shared/scss/table-common'` depuis n'importe où.
Toute commande `sass` ou `sass-migrator` lancée à la main doit reprendre ce `--load-path`.

Le builder expose aussi `stylePreprocessorOptions.sass.silenceDeprecations`,
`fatalDeprecations` et `futureDeprecations` (schéma vérifié dans
`@angular/build`).

---

## 4. Tentative de migration automatique — et pourquoi elle a échoué

`sass-migrator` 2.6.1 a été installé et exécuté ainsi, par lots de 40 fichiers
(la ligne de commande Windows sature au-delà) :

```bash
npx sass-migrator module --migrate-deps --load-path=. <fichiers>
```

**Résultat : 311 fichiers sur 359 réécrits, puis build cassé (≈300 erreurs).**
L'arborescence a été restaurée depuis une archive `tar` prise avant l'opération, et le CSS
produit re-vérifié identique à l'octet près.

Deux causes, toutes deux architecturales :

### 4.1 Portée globale implicite

`app/features/sales/shared/styles/_sales-common.scss:15` utilise `$gray-50` **sans jamais
l'importer**. La variable vient d'un autre partial, que le composant consommateur a importé
plus tôt. `@import` n'a qu'une portée globale ; `@use` isole chaque module.

Le migrateur a tenté de traduire cette dépendance implicite par une configuration :

```scss
@use 'app/shared/scss/…' with ($gray-50: #f9fafb);   // invalide
```

d'où l'erreur répétée `$with key: #f9fafb is not a string`.

### 4.2 Imports tardifs de fichiers mixtes

`_sales-common.scss` importe `app/shared/scss/search-section` **après** avoir déjà émis du
CSS, pour en consommer le mixin `search-section()`. Le migrateur refuse de remonter cet
import — cela réordonnerait la cascade — et propose exactement le remède du §5 :

> *Splitting this stylesheet into one containing mixins and one emitting CSS will allow the
> migrator to safely migrate it.*

L'option `--unsafe-hoist` existe mais ne résout que 4.2, pas 4.1.

---

## 5. Le correctif : scinder les partials partagés

Pour chaque partial de `app/shared/scss/` :

1. **Extraire les membres** (variables, mixins) dans un fichier qui **n'émet aucun CSS**.
   Celui-ci devient `@use`-able sans effet de bord et peut être chargé partout.
2. **Déplacer les règles CSS** vers les styles globaux d'`angular.json`, ou vers un partial
   dédié importé une seule fois.
3. **Supprimer l'`@import` du composant** : il n'existait que pour injecter ce CSS.

Bénéfices cumulés :

- la majorité des 510 `@import` disparaît, sans migration ;
- le reliquat devient migrable mécaniquement par `sass-migrator` ;
- la duplication ×3180 s'effondre ;
- les dépendances implicites du §4.1 deviennent explicites.

### 5.1 Obstacles identifiés

| Obstacle | Où | Traitement |
| --- | --- | --- |
| **41 `::ng-deep`** dans `_pharma-toolbar.scss` | + 3 dans `_pharma-nav.scss`, 3 dans `form-styles`, 2 dans `modal-theme`, 2 dans `dashboard-common` | En feuille globale, `::ng-deep` n'a plus de sens : à retirer, ce qui change la spécificité |
| **Encapsulation** | Tous | `.pharma-toolbar` compilé dans un composant devient `.pharma-toolbar[_ngcontent-xxx]` ; en global il s'applique partout. Effet voulu pour une classe partagée, mais à vérifier composant par composant |
| `_table-common.scss` : **0 `::ng-deep`** | — | Le meilleur candidat pilote |

### 5.2 `vendor.scss` et `global.scss` restent en `@import`

**Bootstrap 5.3 ne supporte pas `@use` avec configuration.** La personnalisation par
variables `!default` déclarées *avant* l'import est le seul mécanisme officiel jusqu'à
Bootstrap 6. Ces deux fichiers conserveront donc `@import` — et leurs quelques
avertissements — indépendamment de tout le reste.

---

## 6. Découpage proposé

| Lot | Contenu |
| --- | --- |
| **S0 — Silencing provisoire** | `stylePreprocessorOptions.sass.silenceDeprecations: ["import"]` dans `angular.json`, avec commentaire pointant vers ce document. Ne corrige rien, mais 1005 lignes cessent de noyer les avertissements utiles. **Réversible en une ligne.** |
| **S1 — Pilote `table-common`** | Scinder le partial le plus importé (128×) et sans `::ng-deep`. Mesurer le gain réel sur le bundle avant de généraliser. |
| **S2 — Généralisation** | `pharma-toolbar` (le plus lourd, 41 `::ng-deep`), `dashboard-common`, `form-styles`, `modal-theme`, `pharma-nav`, `kpi-strip`. |
| **S3 — Migration `@use`** | Sur le reliquat, avec `sass-migrator module --migrate-deps --load-path=.` par lots de 40. |
| **S4 — Fonctions de couleur** | `darken()` / `lighten()` (96 occurrences) → `color.adjust()` / `color.scale()` via `sass-migrator color`. `map-merge` → `map.merge`. |

S1 est la seule étape qui produit une mesure : elle décide si S2 vaut l'investissement.

---

## 7. Méthode de vérification

La même qu'au décommissionnement de Bootswatch, et elle a fait ses preuves :

1. Archiver l'arborescence SCSS avant toute opération
   (`find . -name "*.scss" -print0 | tar --null -cf sauvegarde.tar -T -`).
2. Construire, copier `pharmaSmart-app/target/classes/static/styles.css`.
3. Modifier.
4. Reconstruire, `diff` les deux `styles.css`. **Objectif : zéro écart** — sauf pour S1/S2,
   où la taille doit chuter alors que le rendu reste identique.

Pour S1 et S2, `styles.css` seul ne suffit pas : les styles de composants sont émis dans
les chunks JS. Compter aussi les occurrences d'un sélecteur témoin
(`grep -oh "\.pharma-table-wrapper" *.js *.css | wc -l`) avant et après.

---

## 8. Note sur `sass-migrator`

Installé en `devDependencies` lors de la tentative du §4. Il n'est utile qu'aux lots S3 et
S4. **À retirer si ces lots sont repoussés** — l'objectif du projet est de réduire le
nombre de dépendances, pas d'en ajouter une qui dort.

---

## 9. Hors périmètre

- Migration de Bootstrap vers le système de modules (attend Bootstrap 6).
- `_pharma-nav.scss` et ses variables `$pharma-*` : troisième famille de couleurs de
  navigation, cf. [PLAN-HARMONISATION-NAVBAR-SIDEBAR.md](PLAN-HARMONISATION-NAVBAR-SIDEBAR.md) §9.
- Élagage du thème yeti internalisé, cf.
  [PLAN-DECOMMISSIONNEMENT-BOOTSWATCH.md](PLAN-DECOMMISSIONNEMENT-BOOTSWATCH.md).

---

## 10. Suivi d'implémentation

### 10.1 S0 — Silencing provisoire (fait)

`stylePreprocessorOptions.sass.silenceDeprecations: ["import"]` ajouté dans `angular.json`
(`architect.build.options`). Les avertissements `@import` déprécié sont désormais tus ; les
autres dépréciations (`darken()`, `lighten()`, `map-merge`, cf. §2.2) restent visibles et
sont du ressort de S4.

### 10.2 S1 — Pilote `table-common` (fait, mesuré)

`app/shared/scss/_table-common.scss` ne contient plus que les 12 variables `$pharma-*`
(aucun CSS, donc `@import`/`@use` de ce fichier n'a plus d'effet de duplication). Les ~780
lignes de règles CSS ont été déplacées vers un nouveau fichier
`content/scss/table-common-global.scss`, déclaré une seule fois dans
`angular.json` → `architect.build.options.styles`.

**Aucun des ~50 composants qui importaient `app/shared/scss/table-common` n'a eu besoin
d'être modifié** : le chemin d'import est inchangé, seul son contenu a changé de nature
(variables uniquement). C'est délibéré — ça limite le risque de régression par rapport à
une réécriture des imports dans chaque composant.

**Mesure (build `ng build --configuration development`, comptage sur
`pharmaSmart-app/target/classes/static/*.js` + `*.css`) :**

| Sélecteur témoin | Avant | Après |
| --- | --- | --- |
| `.pharma-table-wrapper` | 348 | **1** |
| `.pharma-toolbar-filters` (hors périmètre S1, `pharma-toolbar`) | 3180 | 3180 (inchangé, attendu) |

Le build passe sans erreur, seuls les avertissements hors périmètre S0 subsistent
(`darken()`/`lighten()`/`map-merge`, cf. §2.2 et lot S4).

**Point de vigilance reporté à la vérification visuelle manuelle :** les règles de
`table-common-global.scss` ne portent plus l'attribut d'encapsulation Angular
(`[_ngcontent-xxx]`) puisqu'elles sont désormais globales — cf. §5.1. `_table-common.scss`
ne comportait aucun `::ng-deep`, ce qui en faisait le candidat le plus sûr ; aucune
adaptation de spécificité n'a été nécessaire.

### 10.3 S2 — Généralisation (en cours)

Trois partials supplémentaires traités selon la même méthode (variables `!default`
conservées dans le fichier d'origine, CSS déplacé dans un fichier `content/scss/<nom>-global.scss`
chargé une seule fois via `angular.json`) :

| Partial | Importeurs | Points de vigilance traités |
| --- | --- | --- |
| `app/shared/scss/_kpi-strip.scss` | ~20 (pages de rapports) | Variables `$_ks-*` **conservées** dans le fichier d'origine : consommées directement par `app/shared/ui/kpi-strip/kpi-item.component.scss` et `kpi-strip.component.scss` (Design System). Une première tentative avait vidé le fichier entièrement — build cassé (`Undefined variable`), corrigé en réintroduisant les 6 variables. |
| `app/shared/scss/_data-card.scss` | 1 direct (`app/shared/ui/card/card.component.scss`, le composant `app-card` du Design System — très largement utilisé) + transitif via `dashboard-common` | 1 `::ng-deep .p-select` retiré (bloc imbriqué, devient un simple sélecteur descendant en global — sans effet sur le rendu, cf. §5.1). |
| `app/shared/scss/dashboard-common.scss` | ~39 (dashboards, rapports) | Le bloc `:host { display: block; animation: fadeIn 0.5s ease-out; }` **reste local** : `:host` n'a de sens qu'en style de composant, il ne peut pas être globalisé — poids négligeable (2 déclarations), dupliqué comme avant. L'ancien `@import 'data-card'` interne est supprimé (le CSS de `.data-card` est désormais fourni indépendamment par `data-card-global.scss`). 1 `::ng-deep .pharma-table { … }` top-level retiré (même raisonnement que ci-dessus). |

**Mesure (même méthode qu'en §10.2) :** le CSS de ces trois partials n'apparaît plus
qu'une fois dans `styles.css` (15 occurrences de la sous-chaîne `.kpi-strip` pour les 6
classes distinctes qui la contiennent, 29 pour `.data-card`, 47 pour `.kpi-card` — un
seul jeu de règles, contre une copie par composant importateur auparavant). Build
(`ng build --configuration development`) sans erreur après correction du point kpi-strip
ci-dessus.

### 10.3bis S2 — Suite : `pharma-nav`, `form-styles`, `pharma-toolbar`, `action-bar`

Quatre partials supplémentaires, dont les deux plus lourds du projet (`pharma-toolbar` à
99 imports/41 `::ng-deep`, `form-styles` à 44 imports) :

| Partial | Importeurs | Points de vigilance traités |
| --- | --- | --- |
| `app/shared/scss/_pharma-nav-tabs.scss` | `_pharma-nav.scss` (transitif) + `app/shared/ui/nav-tabs/nav-tabs.component.scss` (via `:host ::ng-deep { @import … }`, pour atteindre son contenu projeté) | Variables `!default` conservées. Une fois le CSS global, `.pharma-nav-tabs-container` s'applique déjà au contenu projeté sans percer quoi que ce soit : le `:host ::ng-deep` de `nav-tabs.component.scss` devient inoffensif (il n'importe plus que des variables). |
| `app/shared/scss/_pharma-nav.scss` | ~43 | Variables (sans `!default`) conservées : `app/features/commande/_commande-shared.scss` les consomme directement sans les redéclarer (« Prérequis : @import pharma-nav avant ce fichier »). 3 blocs `::ng-deep { … }` **autonomes, sans sélecteur parent** — dépliés sans changement de comportement (un `::ng-deep` sans rien avant lui compile déjà en CSS 100% global, cf. §5.1). Ancien `@import 'pharma-nav-tabs'` retiré (désormais global indépendamment). |
| `app/shared/scss/form-styles.scss` | ~44 | Aucune variable SCSS dans ce fichier (vérifié) : le fichier d'origine est désormais **vide**. 3 `::ng-deep` retirés : un bloc imbriqué dans `.modal-body { ::ng-deep { … } }` (déplié, devient descendant direct de `.modal-body`), un bloc autonome (`.p-autocomplete` overrides) et un `::ng-deep .p-card { … }` simple. |
| `app/shared/scss/_action-bar.scss` | `_pharma-toolbar.scss` (transitif) + `app/shared/ui/action-bar/action-bar.component.scss` (même schéma `:host ::ng-deep` que `pharma-nav-tabs`) | Variables `!default` conservées. 1 bloc `::ng-deep` imbriqué dans `.su-action-bar__filters`, déplié. |
| `app/shared/scss/_pharma-toolbar.scss` | ~99 (le plus lourd du projet) | Variables (sans `!default`) conservées. **41 `::ng-deep`**, tous de la forme `.pharma-xxx { ::ng-deep .cible { … } }` (jamais en tête sans ancêtre) : dépliés un par un en gardant l'ancêtre `.pharma-*` (nom de classe spécifique au projet, sans risque de collision globale). Ancien `@import 'action-bar'` retiré (désormais global indépendamment). |

**Mesure :** build (`ng build --configuration development`) sans erreur. Comptage sur le
bundle construit :

| Sélecteur témoin | Avant migration | Après |
| --- | --- | --- |
| `.pharma-toolbar-filters` | 3180 | **21** |
| `.pharma-toolbar-actions` | (non mesuré isolément avant) | 29 |
| `.su-action-bar` | — | 2 |
| `.pharma-nav-tabs-container` | — | 18 |

### 10.4 `modal-theme` — @import retiré (S3 ciblé), déduplication non traitée

**Distinction importante, qui ne ressortait pas assez clairement des versions
précédentes de cette section :** `modal-theme` posait deux problèmes indépendants, un
seul a été résolu ici.

**Ce qui a été fait — sortir de `@import` (S3) :** contrairement aux partials des §10.2
et §10.3, `_modal-theme.scss` ne définit qu'un `@mixin modal-theme { … }` ; toutes ses
variables (`$pharma-primary`, `$white`, `$light-bg`, `$border-color`) sont déclarées
**à l'intérieur** du mixin, donc jamais exposées à la portée globale — le fichier n'a
donc aucune des deux causes structurelles du §4 (pas de dépendance implicite entre
partials, pas d'import tardif après émission de CSS). Remplacer
`@import 'app/shared/scss/modal-theme';` par
`@use 'app/shared/scss/modal-theme' as *;` est donc mécanique et sûr — d'ailleurs une
quinzaine de composants l'avaient déjà fait spontanément avant cette intervention, sans
incident. Les ~62 fichiers restants ont été convertis par substitution automatisée, avec
une correction de suivi : `@use` doit précéder toute autre règle (y compris un
`@import` d'un autre partial comme `table-common` ou `form-styles` sur la ligne
précédente) — une quinzaine de fichiers avaient le mixin importé après un autre
`@import`, la ligne `@use` a été remontée en tête de fichier. Build (`ng build`)
vérifié sans erreur, 0 `@import` restant vers `modal-theme`.

**Ce qui n'a PAS été fait — dédupliquer le CSS du mixin (S2) :** chacun des ~60
`@include modal-theme;` continue de recopier le corps du mixin dans son unité de
compilation. Ce n'est **pas** la même situation que `table-common`/`pharma-toolbar` :
partout ailleurs, les `::ng-deep` retirés étaient soit déjà globaux une fois compilés
par Angular, soit bornés par un nom de classe spécifique au projet (`.pharma-toolbar`,
`.su-action-bar`…) — les globaliser ne changeait donc rien au ciblage. Ici,
`@include modal-theme;` est systématiquement appelé dans `:host { … }`, qui compile en
`[_nghost-xxx] .modal-header { … }` : la portée est bornée **à l'instance de
composant**, sur des noms de classes Bootstrap génériques (`.modal-header`,
`.modal-body`, `.modal-footer`, `.card`). Globaliser tel quel appliquerait ce thème à
*tout* `.modal-header`/`.card` de l'application — y compris des modales qui n'incluent
pas ce mixin — ce qui serait un changement de comportement, pas une simple
déduplication. Un traitement sûr demanderait d'ajouter une classe marqueur (ex.
`.pharma-modal`) au gabarit des ~60 composants concernés, puis de scoper le CSS global
sur cette classe plutôt que sur `:host` — une modification qui touche les templates
TS/HTML, pas seulement le SCSS, et donc d'une autre nature que le reste de ce document.
Laissé en dette technique acceptée : le fichier ne pèse que 6,6 Ko sources, le gain
potentiel est sans commune mesure avec celui de `pharma-toolbar` (28 Ko) et le risque de
régression (templates de 60 composants à vérifier un par un) est disproportionné par
rapport au gain.





