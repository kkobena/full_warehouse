import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from 'app/shared/shared.module';
import { groupeFournisseurRoute } from './groupe-fournisseur.route';
import { GroupeFournisseurComponent } from './groupe-fournisseur.component';

@NgModule({
  imports: [SharedModule, RouterModule.forChild(groupeFournisseurRoute)],
  declarations: [GroupeFournisseurComponent],
})
export class WarehouseGroupeFournisseurModule {}
