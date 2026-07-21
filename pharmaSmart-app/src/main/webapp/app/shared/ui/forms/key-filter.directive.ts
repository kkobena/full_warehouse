import { Directive, computed, input } from '@angular/core';

/** Jeux de caractères repris de `pKeyFilter`, limités à ceux réellement utilisés dans l'app. */
export type AppKeyFilterPreset = 'alpha' | 'alphanum' | 'int' | 'pint';

/**
 * Restreint les caractères saisissables dans un `<input>` — remplace `pKeyFilter`.
 *
 * Les motifs acceptent la chaîne vide et les valeurs partielles : le contrôle porte sur
 * ce que **deviendrait** le champ, pas sur le seul caractère tapé. Sans quoi un `int` en
 * cours de frappe (« -" » avant les chiffres) serait rejeté.
 *
 * L'interception passe par `beforeinput`, qui couvre d'un seul geste la frappe, le
 * collage et le glisser-déposer — là où un `keypress` laisse passer les deux derniers.
 *
 * Toujours passer le motif en **liaison**, jamais en attribut statique
 * (`appKeyFilter="alphanum"`) : Angular accepte les deux, mais IntelliJ ne résout pas
 * l'attribut statique vers une entrée signal et signale « Attribute appKeyFilter is not
 * allowed here » sur chaque usage.
 *
 * @example
 * <input [appKeyFilter]="'alphanum'" formControlName="code" class="form-control" />
 * <input [appKeyFilter]="/^[A-Z]*$/" formControlName="trigramme" class="form-control" />
 */
@Directive({
  selector: '[appKeyFilter]',
  host: {
    '(beforeinput)': 'onBeforeInput($event)',
  },
})
export class KeyFilterDirective {
  readonly appKeyFilter = input.required<AppKeyFilterPreset | RegExp>();

  private static readonly PATTERNS: Record<AppKeyFilterPreset, RegExp> = {
    alpha: /^[a-zA-Z]*$/,
    alphanum: /^[a-zA-Z0-9]*$/,
    int: /^-?\d*$/,
    pint: /^\d*$/,
  };

  private readonly pattern = computed(() => {
    const filter = this.appKeyFilter();
    return filter instanceof RegExp ? filter : KeyFilterDirective.PATTERNS[filter];
  });

  protected onBeforeInput(event: InputEvent): void {
    // `data` est nul sur les suppressions et les annulations : rien à filtrer.
    if (event.data === null) {
      return;
    }

    const input = event.target as HTMLInputElement;
    const start = input.selectionStart ?? input.value.length;
    const end = input.selectionEnd ?? start;
    const next = input.value.slice(0, start) + event.data + input.value.slice(end);

    if (!this.pattern().test(next)) {
      event.preventDefault();
    }
  }
}
