import { Component, Input } from '@angular/core';
import { DossierFactureProjection } from '../model/reglement-facture-dossier.model';
import { FieldsetModule } from 'primeng/fieldset';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

@Component({
    selector: 'jhi-dossier-reglement-info',
    imports: [FieldsetModule, WarehouseCommonModule],
    templateUrl: './dossier-reglement-info.component.html'
})
export class DossierReglementInfoComponent {
  @Input() dossierFactureProjection: DossierFactureProjection | null = null;
  @Input() totalApayer: number | null = null;
  @Input() monnaie: number | null = null;

  get montantApayer(): number {
    return this.totalApayer || this.dossierFactureProjection.montantTotal - this.dossierFactureProjection.montantDetailRegle;
  }
}
