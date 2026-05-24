import { Injectable, signal } from '@angular/core';

export type SalesManagementTab = 'journal' | 'en-cours' | 'presales' | 'devis' | 'annulations' | 'vente-depot' | 'avoirs' | 'kpi' | 'retour-client';

export interface SalesToolbarParams {
  typeVente: string[];
  search: string | null;
  global: boolean;
  fromDate: Date;
  toDate: Date;
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
  fromDate: new Date(),
  toDate: new Date(),
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
