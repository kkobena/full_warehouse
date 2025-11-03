import { inject, Injectable, signal, WritableSignal } from '@angular/core';
import { IMagasin } from '../../../shared/model/magasin.model';
import { MagasinService } from '../../magasin/magasin.service';
import { HttpResponse } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class DepotAgreeService {
  private magasinService = inject(MagasinService);
  selectedDepot: WritableSignal<IMagasin | null> = signal<IMagasin | null>(null);
  depots: WritableSignal<IMagasin[]> = signal<IMagasin[]>([]);

  constructor() {
    this.loadAllDepots();
  }

  setSelectedDepot(depot: IMagasin | null): void {
    this.selectedDepot.set(depot);
  }

  loadAllDepots(): void {
    this.magasinService.fetchAllDepots({
      types: ['DEPOT_AGGREE']
    }).subscribe((res: HttpResponse<IMagasin[]>) => {
      this.depots.set(res.body || []);
    });
  }
}
