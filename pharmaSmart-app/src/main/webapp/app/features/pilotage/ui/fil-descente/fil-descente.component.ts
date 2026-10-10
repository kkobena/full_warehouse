import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ButtonComponent } from 'app/shared/ui';
import { EtapeDescente } from '../../models/pilotage.model';

/** Fil d'Ariane de la descente (« Tout › Antalgiques › Doliprane 1000 ») : un clic remonte à ce niveau. */
@Component({
  selector: 'app-fil-descente',
  imports: [ButtonComponent],
  template: `
    <nav aria-label="Chemin de descente">
      <ol class="fil-descente">
        <li>
          <app-button (clicked)="remonter.emit(0)" [disabled]="!chemin().length" [text]="true" label="Tout" size="small" />
        </li>
        @for (etape of chemin(); track $index) {
          <li [attr.aria-current]="$last ? 'location' : null">
            <span aria-hidden="true" class="fil-descente-separateur">›</span>
            <app-button (clicked)="remonter.emit($index + 1)" [disabled]="$last" [label]="etape.libelle" [text]="true" size="small" />
          </li>
        }
      </ol>
    </nav>
  `,
  styles: `
    .fil-descente {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.25rem;
      margin: 0;
      padding: 0;
      list-style: none;

      li {
        display: flex;
        align-items: center;
        gap: 0.25rem;
      }
    }

    .fil-descente-separateur {
      color: var(--pharma-text-muted);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FilDescenteComponent {
  readonly chemin = input.required<readonly EtapeDescente[]>();
  /** Nombre d'étapes à garder : 0 remonte tout en haut. */
  readonly remonter = output<number>();
}
