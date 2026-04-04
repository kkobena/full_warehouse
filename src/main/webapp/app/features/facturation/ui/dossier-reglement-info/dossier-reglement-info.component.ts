import { Component, input } from '@angular/core';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { IDossierFactureProjection } from '../../data-access/models';

@Component({
  selector: 'app-dossier-reglement-info',
  imports: [WarehouseCommonModule],
  templateUrl: './dossier-reglement-info.component.html',
})
export class DossierReglementInfoComponent {
  readonly dossierFactureProjection = input<IDossierFactureProjection | null>(null);
  readonly totalApayer = input<number | null>(null);
  readonly monnaie = input<number | null>(null);

  get montantApayer(): number {
    const p = this.dossierFactureProjection();
    if (!p) return 0;
    return this.totalApayer() ?? (p.montantTotal - p.montantDetailRegle);
  }
}
