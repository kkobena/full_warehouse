import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbDateStruct, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import dayjs, { Dayjs } from 'dayjs/esm';

import { PharmaDatePickerComponent } from 'app/shared/date-picker/pharma-date-picker.component';
import { CheckboxComponent, SelectComponent } from 'app/shared/ui';
import { ISO_TO_NGB_DATE, NGB_DATE_TO_ISO } from 'app/shared/util/warehouse-util';
import { Granularite, Periode, PeriodePredefinie, RequetePilotage, TypeComparaison } from '../../models/pilotage.model';

const FORMAT_ISO = 'YYYY-MM-DD';

/** Bornes d'une période prédéfinie, calculées à partir d'aujourd'hui. */
export function calculerPeriode(predefinie: Exclude<PeriodePredefinie, 'PERSONNALISEE'>, aujourdhui: Dayjs = dayjs()): Periode {
  const debutDuTrimestre = aujourdhui.startOf('month').subtract(aujourdhui.month() % 3, 'month');
  const [du, au]: [Dayjs, Dayjs] = {
    MOIS_EN_COURS: [aujourdhui.startOf('month'), aujourdhui.endOf('month')],
    MOIS_PRECEDENT: [aujourdhui.subtract(1, 'month').startOf('month'), aujourdhui.subtract(1, 'month').endOf('month')],
    TRIMESTRE_EN_COURS: [debutDuTrimestre, debutDuTrimestre.add(2, 'month').endOf('month')],
    ANNEE_EN_COURS: [aujourdhui.startOf('year'), aujourdhui.endOf('year')],
    ANNEE_PRECEDENTE: [aujourdhui.subtract(1, 'year').startOf('year'), aujourdhui.subtract(1, 'year').endOf('year')],
    DOUZE_MOIS_GLISSANTS: [aujourdhui.startOf('month').subtract(11, 'month'), aujourdhui.endOf('month')],
  }[predefinie] as [Dayjs, Dayjs];
  return { du: du.format(FORMAT_ISO), au: au.format(FORMAT_ISO) };
}

/** Période, comparaison et découpage communs à tous les onglets du pilotage. */
@Component({
  selector: 'app-barre-periode',
  imports: [FormsModule, NgbTooltip, SelectComponent, CheckboxComponent, PharmaDatePickerComponent],
  templateUrl: './barre-periode.component.html',
  styleUrl: './barre-periode.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BarrePeriodeComponent {
  readonly requete = input.required<RequetePilotage>();
  readonly requeteChange = output<RequetePilotage>();

  protected readonly periodes: { libelle: string; valeur: PeriodePredefinie }[] = [
    { libelle: 'Mois en cours', valeur: 'MOIS_EN_COURS' },
    { libelle: 'Mois précédent', valeur: 'MOIS_PRECEDENT' },
    { libelle: 'Trimestre en cours', valeur: 'TRIMESTRE_EN_COURS' },
    { libelle: 'Année en cours', valeur: 'ANNEE_EN_COURS' },
    { libelle: 'Année précédente', valeur: 'ANNEE_PRECEDENTE' },
    { libelle: '12 mois glissants', valeur: 'DOUZE_MOIS_GLISSANTS' },
    { libelle: 'Personnalisée', valeur: 'PERSONNALISEE' },
  ];

  /** L'utilisateur voit l'onglet Objectifs : il peut s'y comparer. */
  readonly avecObjectifs = input(false);

  protected readonly comparaisons = computed<{ libelle: string; valeur: TypeComparaison }[]>(() => [
    { libelle: 'Même période N-1', valeur: 'MEME_PERIODE_N_1' },
    { libelle: 'Période précédente', valeur: 'PERIODE_PRECEDENTE' },
    { libelle: 'Année N-k', valeur: 'ANNEE_N_MOINS_K' },
    ...(this.avecObjectifs() ? [{ libelle: 'Objectifs', valeur: 'OBJECTIF' as const }] : []),
    { libelle: 'Aucune', valeur: 'AUCUNE' },
  ]);

  protected readonly annees = [2, 3, 4, 5].map(k => ({ libelle: `N-${k}`, valeur: k }));

  protected readonly granularites: { libelle: string; valeur: Granularite }[] = [
    { libelle: 'Jour', valeur: 'JOUR' },
    { libelle: 'Semaine', valeur: 'SEMAINE' },
    { libelle: 'Mois', valeur: 'MOIS' },
    { libelle: 'Trimestre', valeur: 'TRIMESTRE' },
    { libelle: 'Année', valeur: 'ANNEE' },
  ];

  protected readonly du = computed(() => ISO_TO_NGB_DATE(this.requete().du));
  protected readonly au = computed(() => ISO_TO_NGB_DATE(this.requete().au));

  // `app-select` émet sa sélection sans type : la liste qu'on lui donne garantit la valeur.
  protected choisirPeriode(selection: unknown): void {
    const predefinie = selection as PeriodePredefinie | null;
    if (!predefinie) {
      return;
    }
    const bornes = predefinie === 'PERSONNALISEE' ? {} : calculerPeriode(predefinie);
    this.modifier({ predefinie, ...bornes });
  }

  protected choisirDu(date: NgbDateStruct | null): void {
    this.modifierBorne('du', date);
  }

  protected choisirAu(date: NgbDateStruct | null): void {
    this.modifierBorne('au', date);
  }

  protected choisirComparaison(selection: unknown): void {
    const comparaison = selection as TypeComparaison | null;
    if (comparaison) {
      this.modifier({ comparaison, anneesEnArriere: comparaison === 'ANNEE_N_MOINS_K' ? 2 : 1 });
    }
  }

  protected choisirAnnees(selection: unknown): void {
    const anneesEnArriere = selection as number | null;
    if (anneesEnArriere) {
      this.modifier({ anneesEnArriere });
    }
  }

  protected choisirADate(aDate: boolean): void {
    this.modifier({ aDate });
  }

  protected choisirGranularite(selection: unknown): void {
    const granularite = selection as Granularite | null;
    if (granularite) {
      this.modifier({ granularite });
    }
  }

  private modifierBorne(borne: 'du' | 'au', date: NgbDateStruct | null): void {
    const iso = NGB_DATE_TO_ISO(date);
    if (iso) {
      this.modifier({ predefinie: 'PERSONNALISEE', [borne]: iso });
    }
  }

  private modifier(changement: Partial<RequetePilotage>): void {
    this.requeteChange.emit({ ...this.requete(), ...changement });
  }
}
