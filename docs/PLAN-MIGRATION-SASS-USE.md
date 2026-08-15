# Plan — Sortir de `@import` (Sass) et dédupliquer les styles partagés

**Statut :** proposition, non implémenté — une tentative de migration automatique a été
faite puis **intégralement annulée** (§4)
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
