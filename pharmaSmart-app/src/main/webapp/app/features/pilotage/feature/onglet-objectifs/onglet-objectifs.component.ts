import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { AbilityService } from 'app/core/auth/ability.service';
import { PillSelectorComponent } from 'app/shared/ui';
import { RequetePilotage } from '../../models/pilotage.model';
import { AncresSectionsComponent } from '../../ui/ancres-sections/ancres-sections.component';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { SaisieObjectifsComponent } from './saisie-objectifs.component';
import { SuiviObjectifsComponent } from './suivi-objectifs.component';

/** Onglet « Objectifs » : suis-je en avance ou en retard ? Objectifs mensuels d'une année civile, suivis et saisis. */
@Component({
  selector: 'app-onglet-objectifs',
  imports: [FormsModule, PillSelectorComponent, AncresSectionsComponent, SuiviObjectifsComponent, SaisieObjectifsComponent],
  template: `
    <div class="onglet-sections">
      <h2 class="visually-hidden">Objectifs</h2>
      <app-ancres-sections [ancres]="ancres()">
        <app-pill-selector [items]="annees()" [ngModel]="annee()" (ngModelChange)="annee.set($event)" ariaLabel="Année des objectifs" />
      </app-ancres-sections>
      <app-suivi-objectifs [chargement]="suivi.isLoading()" [erreur]="!!suivi.error()" [suivi]="suivi.value() ?? null" />
      @if (peutModifier()) {
        <app-saisie-objectifs [annee]="annee()" (enregistre)="suivi.reload()" />
      }
    </div>
  `,
  styleUrl: '../../ui/section-onglet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OngletObjectifsComponent {
  readonly requete = input.required<RequetePilotage>();

  private readonly api = inject(PilotageApiService);

  protected readonly peutModifier = inject(AbilityService).canSignal('edit', 'pilotage.objectifs');
  /** L'année de la fin de la période de la barre, modifiable ici : les objectifs se lisent par année civile. */
  protected readonly annee = linkedSignal(() => Number(this.requete().au.slice(0, 4)));
  private readonly anneeCourante = signal(new Date().getFullYear());
  protected readonly annees = computed(() => {
    const courante = this.anneeCourante();
    return [courante - 1, courante, courante + 1].map(annee => ({ label: String(annee), value: annee }));
  });
  protected readonly suivi = rxResource({ params: () => this.annee(), stream: ({ params }) => this.api.suivreObjectifs(params) });
  protected readonly ancres = computed(() => [
    { id: 'objectifs-suivi', libelle: 'Suivi' },
    ...(this.peutModifier() ? [{ id: 'objectifs-saisie', libelle: 'Saisie' }] : []),
  ]);
}
