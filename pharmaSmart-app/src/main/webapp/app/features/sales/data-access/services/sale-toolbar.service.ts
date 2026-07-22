import { Injectable, signal } from '@angular/core';
import { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';

export type SalesManagementTab = 'journal' | 'en-cours' | 'presales' | 'devis' | 'annulations' | 'vente-depot' | 'avoirs' | 'kpi' | 'retour-client';

function todayNgb(): NgbDateStruct {
  const d = new Date();
  return { year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate() };
}

export interface SalesToolbarParams {
  typeVente: string[];
  search: string | null;
  global: boolean;
  fromDate: NgbDateStruct;
  toDate: NgbDateStruct;
  fromHour: string;
  toHour: string;
  selectedUserId: number | null;
  selectedCassierId: number | null;
  activeTab: SalesManagementTab;
}

const DEFAULT_PARAMS: SalesToolbarParams = {
  typeVente: [],
  search: null,
  global: true,
  fromDate: todayNgb(),
  toDate: todayNgb(),
  fromHour: '01:00',
  toHour: '23:59',
  selectedUserId: null,
  selectedCassierId: null,
  activeTab: 'journal',
};

@Injectable({ providedIn: 'root' })
export class SaleToolbarService {
  readonly params = signal<SalesToolbarParams>({ ...DEFAULT_PARAMS });
  readonly avoirSaleRef = signal<string | null>(null);

  update(partial: Partial<SalesToolbarParams>): void {
    this.params.update(current => ({ ...current, ...partial }));
  }

  navigateToAvoirs(saleRef: string): void {
    this.avoirSaleRef.set(saleRef);
    this.update({ activeTab: 'avoirs' });
  }

  clearAvoirSaleRef(): void {
    this.avoirSaleRef.set(null);
  }

  reset(): void {
    this.params.set({ ...DEFAULT_PARAMS });
    this.avoirSaleRef.set(null);
  }
}
