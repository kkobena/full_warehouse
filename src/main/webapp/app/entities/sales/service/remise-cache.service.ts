import { Injectable, signal, WritableSignal, inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { GroupRemise, Remise } from '../../../shared/model/remise.model';
import { RemiseService } from '../../remise/remise.service';

@Injectable({
  providedIn: 'root',
})
export class RemiseCacheService {
  private remiseService = inject(RemiseService);

  remises: WritableSignal<GroupRemise[]> = signal<GroupRemise[]>([]);

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {
    // if (this.remises().length === 0) {
    this.remiseService.query().subscribe((res: HttpResponse<Remise[]>) => {
      const groupRemises = res.body
        ?.filter(rem => rem.enable)
        .reduce((acc, remise) => {
          const group = acc.find(g => g.type === remise.type);
          if (group) {
            group.items.push(remise);
          } else {
            acc.push({ type: remise.type, typeLibelle: remise.typeLibelle, items: [remise] });
          }
          return acc;
        }, [] as GroupRemise[]);

      this.remises.set(groupRemises);
    });
    //  }
  }
}
