import {Component, computed, input, output} from '@angular/core';


export type AppButtonSeverity =
  'primary'
  | 'secondary'
  | 'success'
  | 'info'
  | 'warn'
  | 'danger'
  | 'help'
  | 'contrast';

export type AppButtonSize = 'small' | 'normal' | 'large';

/**
 * Bouton du Design System — rend du Bootstrap 5 natif.
 *
 * Les severities PrimeNG sont mappées sur les classes `.btn-*` de Bootstrap. `help` et
 * `contrast` n'existent pas dans Bootstrap : elles sont ajoutées à `$theme-colors`
 * (cf. `content/scss/vendor.scss`), donc `.btn-help` / `.btn-outline-help` sont générées
 * comme les autres, avec leurs états hover/active dérivés.
 *
 * La palette Bootstrap étant alignée sur celle d'Aura (`_pharma-bootstrap-palette.scss`),
 * le rendu reste celui de `p-button` — c'est ce qui permet de migrer écran par écran
 * sans que l'app devienne bicolore.
 *
 * @example
 * <app-button label="Enregistrer" icon="pi pi-save" (clicked)="save()" />
 * <app-button icon="pi pi-trash" [iconOnly]="true" severity="danger" ariaLabel="Supprimer" />
 * <app-button label="Annuler" severity="secondary" [text]="true" />
 */
@Component({
  selector: 'app-button',
  host: {
    '[class.d-inline-block]': 'true',
  },
  template: `
    <button
      [type]="type()"
      [class]="buttonClasses()"
      [disabled]="disabled() || loading()"
      [attr.aria-label]="resolvedAriaLabel()"
      [attr.aria-busy]="loading() ? 'true' : null"
      (click)="clicked.emit($event)"
    >

      @if (loading()) {
        <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
      } @else if (icon() && iconPos() === 'left') {
        <i [class]="'app-btn-icon ' + icon()" aria-hidden="true"></i>
      }

      @if (!iconOnly() && label()) {
        <span class="app-btn-label">{{ label() }}</span>
      }

      @if (!loading() && icon() && iconPos() === 'right') {
        <i [class]="'app-btn-icon ' + icon()" aria-hidden="true"></i>
      }
    </button>
  `,
  styles: `
    // Rétablit [hidden] sur l'hôte.
    //
    // L'hôte porte .d-inline-block, dont la règle Bootstrap est
    // « display: inline-block !important » : elle écrase la règle [hidden] { display: none }
    // de la feuille du navigateur, et <app-button [hidden]="true"> restait visible.
    // Le !important est donc nécessaire ici, et la spécificité (0,2,0 avec l'attribut
    // d'encapsulation) l'emporte sur celle de .d-inline-block (0,1,0).
    :host([hidden]) {
      display: none !important;
    }

    // Bootstrap n'a pas d'équivalent au bouton carré "icon only" de PrimeNG.
    //
    // Le padding horizontal reprend le padding vertical de Bootstrap : à zéro, l'icône
    // touchait les bords sur les variantes .btn-link (text) qui n'ont pas de fond pour
    // donner l'illusion d'une marge. aspect-ratio garde le bouton carré.
    .app-btn-icon-only {
      aspect-ratio: 1;
      padding-inline: var(--bs-btn-padding-y);
    }


    // Bootstrap force line-height: var(--bs-btn-line-height) = 1.5, ce qui gonfle la
    // boîte de texte du bouton par rapport au reste de l'interface.
    //
    // Contrairement au padding et à la taille de police, .btn-sm / .btn-lg ne
    // redéfinissent PAS --bs-btn-line-height : la règle peut donc porter sur .btn sans
    // risque d'aplatir les trois tailles, et elle couvre ainsi small et large aussi.
    .btn {
      --bs-btn-line-height: inherit;
    }

    // Gabarit de la taille normale.
    //
    // La règle porte sur cette classe et non sur .btn : scopée par Angular, .btn
    // pèserait 0-2-0 et écraserait .btn-sm / .btn-lg (0-1-0), qui redéfinissent
    // justement --bs-btn-padding-x et --bs-btn-font-size. Les trois tailles
    // s'aplatiraient en une seule.
    .app-btn-normal {
      // Le preset Aura ne déclare AUCUNE taille de police sur le bouton par défaut : il
      // hérite du contexte. Seules ses variantes sm et lg en fixent une. Bootstrap, lui,
      // impose font-size: var(--bs-btn-font-size) = 1rem à tout .btn — sur une application
      // dont la police ambiante est plus petite, chaque bouton migré grossissait.
      font-size: inherit;

      // Resserrement horizontal demandé : les boutons rendaient trop larges par rapport
      // au reste de l'interface. On reprend le padding horizontal de la variante sm
      // d'Aura (--p-form-field-sm-padding-x) sans toucher au padding vertical, pour que
      // la hauteur reste alignée sur celle des champs de formulaire.
      --bs-btn-padding-x: 0.625rem;
    }

    // La classe .shadow-sm de Bootstrap (0 .125rem .25rem rgba(0,0,0,.075)) ne se voit
    // pas sur un bouton plein. On reprend l'élévation de PrimeNG, seule à rendre
    // l'attribut raised lisible.
    .app-btn-raised {
      box-shadow: 0 3px 1px -2px rgba(0, 0, 0, 0.2),
      0 2px 2px 0 rgba(0, 0, 0, 0.14),
      0 1px 5px 0 rgba(0, 0, 0, 0.12);
    }


    .app-btn-text:hover:not(:disabled) {
      background-color: rgba(var(--bs-emphasis-color-rgb), 0.075);
    }

    // Intégration dans un .input-group Bootstrap (ex. champ quantité + bouton
    // « ajouter »). Bootstrap cible .input-group > .btn pour uniformiser la
    // hauteur et supprimer les coins arrondis internes, mais app-button
    // interpose ce wrapper entre .input-group et le bouton réel : ces
    // règles ne l'atteignent jamais. On les reproduit ici. Suppose l'usage
    // actuel — app-button toujours en dernière position du groupe.
    :host-context(.input-group) {
      display: flex;
      align-self: stretch;

      .btn {
        height: 100%;
        border-top-left-radius: 0;
        border-bottom-left-radius: 0;
      }
    }

    // Intégration dans un .btn-group Bootstrap (remplace p-buttonGroup). Bootstrap fusionne
    // les coins et le bord partagé via des sélecteurs qui ciblent .btn-group > .btn
    // directement — app-button s'interpose entre les deux, donc ces règles ne matchent
    // jamais. On les reproduit ici, au niveau du wrapper. !important est nécessaire côté
    // rayon : [rounded]="true" pose .rounded-pill, qui est lui-même en !important et
    // regagnerait sinon les coins qu'on vient d'aplatir sur les bords partagés.
    :host-context(.btn-group) {
      &:not(:first-child) .btn {
        margin-left: calc(var(--bs-border-width, 1px) * -1);
        border-top-left-radius: 0 !important;
        border-bottom-left-radius: 0 !important;
      }

      &:not(:last-child) .btn {
        border-top-right-radius: 0 !important;
        border-bottom-right-radius: 0 !important;
      }

      &:hover,
      &:focus-within {
        z-index: 1;
        position: relative;
      }
    }
  `,
})
export class ButtonComponent {
  /** `warn` et `contrast` diffèrent entre les deux vocabulaires ; le reste est identique. */
  private static readonly BOOTSTRAP_VARIANT: Record<AppButtonSeverity, string> = {
    primary: 'primary',
    secondary: 'secondary',
    success: 'success',
    info: 'info',
    warn: 'warning',
    danger: 'danger',
    help: 'help',
    contrast: 'contrast',
  };
  /** Libellé affiché. Ignoré si `iconOnly` est actif (mais sert alors de `aria-label` de repli). */
  readonly label = input<string>('');
  /** Classe d'icône, ex. `pi pi-save`. */
  readonly icon = input<string>('');
  /** Position de l'icône par rapport au libellé. */
  readonly iconPos = input<'left' | 'right'>('left');
  /** Bouton carré ne montrant que l'icône. Pensez à renseigner `label` ou `ariaLabel`. */
  readonly iconOnly = input<boolean>(false);
  /** Couleur du bouton (vocabulaire PrimeNG). */
  readonly severity = input<AppButtonSeverity>('primary');
  readonly size = input<AppButtonSize>('normal');
  /** Contour coloré sur fond transparent (`.btn-outline-*`). */
  readonly outlined = input<boolean>(false);
  /** Sans fond ni bordure (`.btn-link`). Prioritaire sur `outlined`. */
  readonly text = input<boolean>(false);
  /** Coins entièrement arrondis (`.rounded-pill`). */
  readonly rounded = input<boolean>(false);
  /** Ombre portée, calquée sur l'élévation de `p-button`. */
  readonly raised = input<boolean>(false);
  /** Remplace l'icône par un spinner et désactive le bouton. */
  readonly loading = input<boolean>(false);
  readonly disabled = input<boolean>(false);
  readonly type = input<'button' | 'submit' | 'reset'>('button');
  /** Libellé d'accessibilité. Par défaut `label` est utilisé — indispensable en `iconOnly`. */
  readonly ariaLabel = input<string>('');
  /** Classes additionnelles posées sur le `<button>` (ex. `w-100`). */
  readonly buttonClass = input<string>('');
  readonly clicked = output<MouseEvent>();
  protected readonly buttonClasses = computed(() => {
    const variant = ButtonComponent.BOOTSTRAP_VARIANT[this.severity()];
    const classes = ['btn', 'd-inline-flex', 'align-items-center', 'justify-content-center', 'gap-2'];

    if (this.text()) {
      // `.text-*` porte `!important`, donc l'emporte sur la couleur de `.btn-link`.
      classes.push('btn-link', 'text-decoration-none', 'app-btn-text', `text-${variant}`);
    } else {
      classes.push(this.outlined() ? `btn-outline-${variant}` : `btn-${variant}`);
    }

    if (this.size() === 'small') {
      classes.push('btn-sm');
    }
    if (this.size() === 'large') {
      classes.push('btn-lg');
    }
    // Taille normale : on laisse la police du contexte, comme p-button.
    if (this.size() === 'normal') {
      classes.push('app-btn-normal');
    }
    if (this.rounded()) {
      classes.push('rounded-pill');
    }
    if (this.raised()) {
      classes.push('app-btn-raised');
    }
    if (this.iconOnly()) {
      classes.push('app-btn-icon-only');
    }
    if (this.buttonClass()) {
      classes.push(this.buttonClass());
    }

    return classes.join(' ');
  });

  protected readonly resolvedAriaLabel = computed(() => this.ariaLabel() || (this.iconOnly() ? this.label() : '') || null);
}
