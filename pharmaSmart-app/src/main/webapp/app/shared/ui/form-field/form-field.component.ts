import {ChangeDetectionStrategy, Component, input} from '@angular/core';

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
        <label class="form-field-label" [attr.for]="fieldId() || null">
          {{ label() }}
          @if (required()) {
            <span class="text-danger" aria-hidden="true">*</span>
            <span class="visually-hidden">(obligatoire)</span>
          }
        </label>
      }

      <div class="form-field-control">
        <ng-content />
      </div>

      @if (error()) {
        <small class="form-field-error text-danger" role="alert" [attr.id]="fieldId() ? fieldId() + '-error' : null">{{ error() }}</small>
      }

      @if (hint() && !error()) {
        <small class="form-field-hint text-muted" [attr.id]="fieldId() ? fieldId() + '-hint' : null">{{ hint() }}</small>
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
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormFieldComponent {
  /**
   * Identifiant du contrôle projeté. Il relie le libellé au champ (`for`) et donne leurs ids au message
   * d'erreur (`<fieldId>-error`) et à l'aide (`<fieldId>-hint`), que le champ désigne par
   * `aria-describedby` : un lecteur d'écran rattache alors le message au champ fautif.
   *
   * @example
   * <app-form-field fieldId="phone" label="Téléphone" [error]="erreur" hint="Ex. 07 01 02 03 04">
   *   <input id="phone" [attr.aria-invalid]="!!erreur" [attr.aria-describedby]="erreur ? 'phone-error' : 'phone-hint'" />
   * </app-form-field>
   */
  readonly fieldId = input<string>('');

  readonly label = input<string>('');

  readonly required = input<boolean>(false);

  /** Message d'erreur ; masque le `hint` tant qu'il est renseigné. */
  readonly error = input<string>('');

  readonly hint = input<string>('');
}
