import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { WarehouseSharedModule } from 'app/shared/shared.module';
import { groupeFournisseurRoute } from './groupe-fournisseur.route';
import { GroupeFournisseurComponent } from './groupe-fournisseur.component';

@NgModule({
  imports: [WarehouseSharedModule, RouterModule.forChild(groupeFournisseurRoute)],
  declarations: [GroupeFournisseurComponent],
})
export class WarehouseGroupeFournisseurModule {}
