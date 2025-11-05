import { Component, input } from '@angular/core';
import { DossierFactureProjection } from '../model/reglement-facture-dossier.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

@Component({
  selector: 'jhi-dossier-reglement-info',
  imports: [WarehouseCommonModule],
  templateUrl: './dossier-reglement-info.component.html',
  styleUrl: './dossier-reglement-info.component.scss'
})
export class DossierReglementInfoComponent {
  readonly dossierFactureProjection = input<DossierFactureProjection | null>(null);
  readonly totalApayer = input<number | null>(null);
  readonly monnaie = input<number | null>(null);

  get montantApayer(): number {
    return this.totalApayer() || this.dossierFactureProjection().montantTotal - this.dossierFactureProjection().montantDetailRegle;
  }
}
