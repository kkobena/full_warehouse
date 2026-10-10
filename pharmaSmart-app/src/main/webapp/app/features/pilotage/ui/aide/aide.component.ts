import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';

/** Icône ⓘ qui explique un chiffre ou une colonne : infobulle au survol et au clavier, texte lu par les lecteurs d'écran. */
@Component({
  selector: 'app-aide',
  imports: [NgbTooltip],
  template: `
    <button [attr.aria-label]="'Aide : ' + texte()" [ngbTooltip]="texte()" class="aide" container="body" placement="top" type="button">
      <i aria-hidden="true" class="pi pi-info-circle"></i>
    </button>
  `,
  styles: `
    .aide {
      display: inline-flex;
      align-items: center;
      margin-left: 0.25rem;
      padding: 0;
      border: 0;
      background: none;
      color: var(--pharma-text-muted);
      font-size: 0.75rem;
      vertical-align: middle;
      cursor: help;

      &:hover,
      &:focus-visible {
        color: var(--p-primary-color);
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AideComponent {
  readonly texte = input.required<string>();
}
