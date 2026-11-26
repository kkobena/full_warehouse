/**
 * Lint des feuilles de style du front (`npm run lint:styles`).
 *
 * Base : `stylelint-config-standard-scss`. La mise en forme relève de Prettier ; ce fichier coupe les
 * règles de pure présentation et garde celles qui signalent une erreur ou une écriture dépréciée.
 *
 * @type {import('stylelint').Config}
 */
export default {
  extends: ['stylelint-config-standard-scss'],
  ignoreFiles: ['**/node_modules/**', '**/target/**', '**/build/**', 'pharmaSmart-app/src/main/resources/**'],
  rules: {
    // --- Angular ---
    // `::ng-deep`, `:host` et `:host-context` sont des sélecteurs du framework.
    'selector-pseudo-element-no-unknown': [true, { ignorePseudoElements: ['ng-deep'] }],
    'selector-pseudo-class-no-unknown': [true, { ignorePseudoClasses: ['host', 'host-context'] }],
    // Sélecteurs d'éléments personnalisés (`app-card`, `ngb-*`…).
    'selector-type-no-unknown': [true, { ignore: ['custom-elements'] }],

    // --- Conventions du projet ---
    // Classes en kebab-case, avec les éléments et modificateurs BEM (`app-icon-field__icon`, `app-icon-field--overlay`).
    'selector-class-pattern': [
      '^[a-z][a-z0-9]*(-[a-z0-9]+)*(__[a-z0-9]+(-[a-z0-9]+)*)?(--[a-z0-9]+(-[a-z0-9]+)*)?$',
      { message: 'Classe attendue en kebab-case, éléments et modificateurs BEM admis (%s)' },
    ],
    // Animations nommées en camelCase (`slideDown`, `wifi-pulse`…) : les deux écritures coexistent.
    'keyframes-name-pattern': null,
    // Identifiants de champ hérités du générateur (`#field_tel`), repris par les tests e2e.
    'selector-id-pattern': null,
    // Variables privées d'un partiel préfixées d'un souligné (`$_ks-primary`).
    'scss/dollar-variable-pattern': ['^_?[a-z][a-z0-9]*(-[a-z0-9]+)*$', { message: 'Variable attendue en kebab-case (%s)' }],
    // Sass accepte `@import` après des règles (contrairement au CSS).
    'no-invalid-position-at-import-rule': null,
    // Repli volontaire : `background: <couleur>` puis `background: <dégradé>` pour un navigateur qui ignorerait le second.
    'declaration-block-no-duplicate-properties': [true, { ignore: ['consecutive-duplicates-with-different-values'] }],
    // `currentColor` garde sa casse usuelle ; les noms de polices dans les variables Sass aussi.
    'value-keyword-case': ['lower', { ignoreKeywords: ['currentColor'], ignoreProperties: ['/^\\$font-family/'] }],
    // Police d'icônes : pas de famille générique de repli possible.
    'font-family-no-missing-generic-family-keyword': [true, { ignoreFontFamilies: ['primeicons'] }],
    'number-max-precision': 6,

    // --- Présentation : laissée à Prettier et aux habitudes de chaque fichier ---
    'rule-empty-line-before': null,
    'at-rule-empty-line-before': null,
    'comment-empty-line-before': null,
    'declaration-empty-line-before': null,
    'custom-property-empty-line-before': null,
    'scss/double-slash-comment-empty-line-before': null,
    'scss/dollar-variable-empty-line-before': null,
    // Un `//` vide sépare les paragraphes d'un commentaire.
    'scss/comment-no-empty': null,

    // --- Notation des couleurs et des requêtes média ---
    // La correction automatique réécrirait aussi les fonctions Sass (`rgba($couleur, 0.5)` → `rgb($couleur / 50%)`),
    // que Sass ne comprend pas : les deux notations restent admises.
    'color-function-notation': null,
    'color-function-alias-notation': null,
    'alpha-value-notation': null,
    'media-feature-range-notation': null,
    'color-hex-length': null,

    // --- Dépréciations Sass ---
    // `darken()`, `lighten()`, `map-get()`… disparaîtront avec Dart Sass 3 (remplacés par `sass:color`, `sass:map`) :
    // signalés sans bloquer, le temps d'une migration à part.
    'scss/no-global-function-names': [true, { severity: 'warning' }],
  },
};
