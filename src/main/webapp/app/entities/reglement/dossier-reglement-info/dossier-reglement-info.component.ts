import { Component, input } from '@angular/core';
import { DossierFactureProjection } from '../model/reglement-facture-dossier.model';
import { FieldsetModule } from 'primeng/fieldset';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

@Component({
    selector: 'jhi-dossier-reglement-info',
    imports: [FieldsetModule, WarehouseCommonModule],
    templateUrl: './dossier-reglement-info.component.html'
})
export class DossierReglementInfoComponent {
  readonly dossierFactureProjection = input<DossierFactureProjection | null>(null);
  readonly totalApayer = input<number | null>(null);
  readonly monnaie = input<number | null>(null);

  get montantApayer(): number {
    return this.totalApayer() || this.dossierFactureProjection().montantTotal - this.dossierFactureProjection().montantDetailRegle;
  }
}
