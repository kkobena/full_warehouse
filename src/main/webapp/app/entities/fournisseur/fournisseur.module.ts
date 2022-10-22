import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { WarehouseSharedModule } from 'app/shared/shared.module';
import { fournisseurRoute } from './fournisseur.route';
import { FournisseurComponent } from './fournisseur.component';

@NgModule({
  imports: [WarehouseSharedModule, RouterModule.forChild(fournisseurRoute)],
  declarations: [FournisseurComponent],
})
export class WarehouseFournisseurModule {}
