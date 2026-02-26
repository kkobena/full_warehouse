import { Injectable, signal } from '@angular/core';

export type SalesManagementTab = 'journal' | 'en-cours' | 'presales' | 'devis';

export interface SalesToolbarParams {
  typeVente: string;
  search: string | null;
  global: boolean;
  fromDate: Date;
  toDate: Date;
  fromHour: string;
  toHour: string;
  selectedUserId: number | null;
  activeTab: SalesManagementTab;
}

const DEFAULT_PARAMS: SalesToolbarParams = {
  typeVente: 'TOUT',
  search: null,
  global: true,
  fromDate: new Date(),
  toDate: new Date(),
  fromHour: '01:00',
  toHour: '23:59',
  selectedUserId: null,
  activeTab: 'journal',
};

@Injectable({ providedIn: 'root' })
export class SaleToolbarService {
  readonly params = signal<SalesToolbarParams>({ ...DEFAULT_PARAMS });

  update(partial: Partial<SalesToolbarParams>): void {
    this.params.update(current => ({ ...current, ...partial }));
  }

  reset(): void {
    this.params.set({ ...DEFAULT_PARAMS });
  }
}
