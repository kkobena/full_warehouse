import { Component, input } from '@angular/core';

/**
 * Enveloppe de champ de formulaire — libellé, message d'erreur et texte d'aide.
 *
 * @example
 * <app-form-field label="Nom du produit" [required]="true" [error]="nomErreur()">
 *   <app-input [(ngModel)]="nom" />
 * </app-form-field>
 */
@Component({
  selector: 'app-form-field',
  template: `
    <div class="form-field" [class.has-error]="error()">
      @if (label()) {
        <label class="form-field-label">
          {{ label() }}
          @if (required()) {
            <span class="text-danger" aria-hidden="true">*</span>
          }
        </label>
      }

      <div class="form-field-control">
        <ng-content />
      </div>

      @if (error()) {
        <small class="form-field-error text-danger" role="alert">{{ error() }}</small>
      }

      @if (hint() && !error()) {
        <small class="form-field-hint text-muted">{{ hint() }}</small>
      }
    </div>
  `,
  styles: `
    .form-field {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
      margin-bottom: 1rem;
    }

    .form-field-label {
      margin-bottom: 0.25rem;
      font-weight: 500;
    }

    .form-field-control {
      width: 100%;
    }

    .form-field-error,
    .form-field-hint {
      display: block;
      margin-top: 0.25rem;
      font-size: 0.875rem;
    }
  `,
})
export class FormFieldComponent {
  readonly label = input<string>('');

  readonly required = input<boolean>(false);

  /** Message d'erreur ; masque le `hint` tant qu'il est renseigné. */
  readonly error = input<string>('');

  readonly hint = input<string>('');
}
