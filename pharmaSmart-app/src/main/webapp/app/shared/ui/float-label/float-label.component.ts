import { Component, input } from '@angular/core';

let nextId = 0;

/**
 * Libellé flottant du Design System — remplace `p-floatlabel`.
 * S'appuie sur `.form-floating` de Bootstrap 5.
 *
 * Reproduit la variante `on` de `p-floatlabel` : le libellé occupe la place du texte au
 * repos, puis remonte sur la bordure haute dès que le champ est rempli ou reçoit le focus.
 *
 * ⚠ Ne s'appuie pas sur `.form-floating` de Bootstrap : ses sélecteurs exigent un
 * `.form-control` ou `.form-select` en **enfant direct**, alors que nos champs sont des
 * composants (`app-select`, `pharma-date-picker`, `app-input-number`). La règle ne
 * s'appliquait donc jamais et le libellé restait figé. La détection d'état est portée par
 * `float-label.component.scss`.
 *
 * ⚠ Pour un `<input>` natif, le champ doit porter un `placeholder` — fût-il un espace —
 * sinon `:placeholder-shown` ne peut pas distinguer le champ vide du champ rempli.
 *
 * @example
 * <app-float-label label="Nom du produit" inputId="produit-nom">
 *   <input id="produit-nom" class="form-control" placeholder=" " [(ngModel)]="nom" />
 * </app-float-label>
 *
 * @example Avec un champ du Design System
 * <app-float-label label="Client" inputId="client">
 *   <app-select [items]="clients" bindLabel="fullName" inputId="client" [(ngModel)]="clientId" />
 * </app-float-label>
 */
@Component({
  selector: 'app-float-label',
  template: `
    <ng-content />
    <label [for]="inputId()">{{ label() }}</label>
  `,
  styleUrl: './float-label.component.scss',
})
export class FloatLabelComponent {
  readonly label = input<string>('');

  /** Doit correspondre à l'`id` du champ projeté. Auto-généré si non fourni. */
  readonly inputId = input<string>(`app-float-label-${nextId++}`);
}
