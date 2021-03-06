import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { WarehouseSharedModule } from 'app/shared/shared.module';
import { groupeTiersPayantRoute } from './groupe-tiers-payant.route';
import { GroupeTiersPayantComponent } from './groupe-tiers-payant.component';
import { FormGroupeTiersPayantComponent } from './form-groupe-tiers-payant/form-groupe-tiers-payant.component';

@NgModule({
  imports: [WarehouseSharedModule, RouterModule.forChild(groupeTiersPayantRoute)],
  declarations: [GroupeTiersPayantComponent, FormGroupeTiersPayantComponent],
  entryComponents: [FormGroupeTiersPayantComponent],
})
export class WarehouseGroupeTiersPayantModule {}
