import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PharmaDatePickerComponent } from 'app/shared/date-picker/pharma-date-picker.component';
import { AppPillOption, PillSelectorComponent } from 'app/shared/ui/pill-selector/pill-selector.component';
import { JournalRemisesFiltresStore } from '../../data-access/journal-remises-filtres.store';
import { NATURE_VENTE_LIBELLES, NatureVente, ORIGINE_LIBELLES, OrigineRemise } from '../../models/journal-remises.model';

/**
 * La barre de filtres d'un onglet du journal, à projeter dans `[toolbarFilters]`. Elle écrit dans
 * l'état partagé des onglets ; rien ne se recharge avant « Afficher ».
 */
@Component({
  selector: 'app-journal-filtres',
  imports: [FormsModule, PharmaDatePickerComponent, PillSelectorComponent],
  template: `
    <div class="pharma-toolbar-filters flex-wrap gap-2">
      <pharma-date-picker
        (ngModelChange)="filtres.du.set($event)"
        [id]="idPrefixe() + '-du'"
        [labelPosition]="'inline'"
        [maxDate]="filtres.au()"
        [ngModel]="filtres.du()"
        label="Du"
      />
      <pharma-date-picker
        (ngModelChange)="filtres.au.set($event)"
        [id]="idPrefixe() + '-au'"
        [labelPosition]="'inline'"
        [minDate]="filtres.du()"
        [ngModel]="filtres.au()"
        label="Au"
      />
      <app-pill-selector
        (ngModelChange)="filtres.natureVente.set($event)"
        [items]="naturesVente"
        [ngModel]="filtres.natureVente()"
        ariaLabel="Nature de la vente"
      />
      @if (avecOrigine()) {
        <app-pill-selector
          (ngModelChange)="filtres.origine.set($event)"
          [items]="origines"
          [ngModel]="filtres.origine()"
          ariaLabel="Origine de la remise"
        />
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JournalFiltresComponent {
  /** Préfixe des identifiants de champ : deux onglets ouverts n'en partagent pas. */
  readonly idPrefixe = input.required<string>();
  /** L'origine qualifie une remise : elle n'a pas de sens pour les UG ni pour la synthèse. */
  readonly avecOrigine = input(false);

  protected readonly filtres = inject(JournalRemisesFiltresStore);

  protected readonly naturesVente: AppPillOption[] = [
    { label: 'Toutes ventes', value: null },
    ...(Object.keys(NATURE_VENTE_LIBELLES) as NatureVente[]).map(n => ({ label: NATURE_VENTE_LIBELLES[n], value: n })),
  ];

  protected readonly origines: AppPillOption[] = [
    { label: 'Toutes origines', value: null },
    ...(Object.keys(ORIGINE_LIBELLES) as OrigineRemise[]).map(o => ({
      label: o === 'CUMUL' ? 'Cumul' : ORIGINE_LIBELLES[o],
      value: o,
    })),
  ];
}
