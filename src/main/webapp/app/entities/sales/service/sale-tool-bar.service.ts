import { Injectable, signal, WritableSignal } from '@angular/core';
import { ToolBarParam } from './tool-bar-param.model';

@Injectable({
  providedIn: 'root',
})
export class SaleToolBarService {
  toolBarParam: WritableSignal<ToolBarParam> = signal<ToolBarParam>({
    typeVente: 'TOUT',
    search: null,
    global: true,
    fromDate: new Date(),
    toDate: new Date(),
    fromHour: '01:00',
    toHour: '23:59',
    selectedUserId: null,
    activeTab: 'ventes-terminees',
  });

  updateToolBarParam(param: ToolBarParam): void {
    this.toolBarParam.set(param);
  }
}
