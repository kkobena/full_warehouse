import { signal } from '@angular/core';
import { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { AppPillOption } from 'app/shared/ui';
import { NGB_DATE_TO_ISO } from 'app/shared/util/warehouse-util';

/** Raccourci de période : label affiché + nombre de jours en arrière (absent = "Ce mois") */
export interface PeriodShortcut {
  label: string;
  key: string;
  days?: number;
}

export const DEFAULT_PERIOD_SHORTCUTS: PeriodShortcut[] = [
  { label: "Aujourd'hui", key: 'today', days: 0 },
  { label: 'Hier', key: 'yesterday', days: 1 },
  { label: '7 j', key: '7d', days: 7 },
  { label: 'Ce mois', key: 'month' },
  { label: '3 mois', key: '3m', days: 90 },
  { label: '1 an', key: '1y', days: 365 },
];

/**
 * État + logique du filtre "période" (raccourcis en pills + dates Du/Au) — partagé par les
 * onglets produit (ventes, achats, mouvements) pour éviter de dupliquer PERIOD_SHORTCUTS,
 * applyShortcut, onFromDateChange/onToDateChange et le calcul ISO des dates dans chacun.
 *
 * @example
 * protected readonly periodFilter = createPeriodDateFilter({ defaultKey: 'today', onChange: () => this.load() });
 *
 * Template :
 * ```html
 * <app-pill-selector [items]="periodFilter.periodItems" [ngModel]="periodFilter.activePeriod()"
 *                     (selectionChange)="periodFilter.applyShortcut($event)" />
 * <pharma-date-picker [(ngModel)]="periodFilter.fromDate" [maxDate]="periodFilter.toDate"
 *                      (selectionChange)="periodFilter.onFromDateChange($event)" />
 * <pharma-date-picker [(ngModel)]="periodFilter.toDate" [minDate]="periodFilter.fromDate"
 *                      (selectionChange)="periodFilter.onToDateChange($event)" />
 * ```
 * Puis dans `buildParam()` : `...this.periodFilter.dateParams()`.
 */
export function createPeriodDateFilter(options: { shortcuts?: PeriodShortcut[]; defaultKey: string; onChange: () => void }) {
  const shortcuts = options.shortcuts ?? DEFAULT_PERIOD_SHORTCUTS;
  const periodItems: AppPillOption[] = shortcuts.map(s => ({ label: s.label, value: s.key }));

  const toStruct = (date: Date): NgbDateStruct => ({
    year: date.getFullYear(),
    month: date.getMonth() + 1,
    day: date.getDate(),
  });

  const shortcutDate = (daysAgo: number): NgbDateStruct => {
    const d = new Date();
    d.setDate(d.getDate() - daysAgo);
    return toStruct(d);
  };

  const initial = shortcuts.find(s => s.key === options.defaultKey);

  const filter = {
    periodItems,
    activePeriod: signal<string>(options.defaultKey),
    fromDate: initial?.days !== undefined ? shortcutDate(initial.days) : toStruct(new Date()),
    toDate: toStruct(new Date()),
    fromDateStr: '',
    toDateStr: '',

    applyShortcut(key: unknown): void {
      const shortcut = shortcuts.find(s => s.key === key);
      if (!shortcut) {
        return;
      }
      const today = new Date();
      filter.toDate = toStruct(today);
      filter.fromDate = shortcut.days !== undefined ? shortcutDate(shortcut.days) : toStruct(new Date(today.getFullYear(), today.getMonth(), 1));
      filter.fromDateStr = '';
      filter.toDateStr = '';
      filter.activePeriod.set(shortcut.key);
      options.onChange();
    },

    onFromDateChange(date: NgbDateStruct | null): void {
      filter.fromDateStr = NGB_DATE_TO_ISO(date) ?? '';
      filter.activePeriod.set('');
    },

    onToDateChange(date: NgbDateStruct | null): void {
      filter.toDateStr = NGB_DATE_TO_ISO(date) ?? '';
      filter.activePeriod.set('');
      options.onChange();
    },

    /** `fromDate`/`toDate` ISO, prêts à être étalés dans le param de requête. */
    dateParams(): { fromDate: string | null; toDate: string | null } {
      return {
        fromDate: filter.fromDateStr || NGB_DATE_TO_ISO(filter.fromDate),
        toDate: filter.toDateStr || NGB_DATE_TO_ISO(filter.toDate),
      };
    },
  };

  return filter;
}

export type PeriodDateFilter = ReturnType<typeof createPeriodDateFilter>;
