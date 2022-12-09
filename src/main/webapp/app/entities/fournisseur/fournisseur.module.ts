import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from 'app/shared/shared.module';
import { fournisseurRoute } from './fournisseur.route';
import { FournisseurComponent } from './fournisseur.component';

@NgModule({
  imports: [SharedModule, RouterModule.forChild(fournisseurRoute)],
  declarations: [FournisseurComponent],
})
export class WarehouseFournisseurModule {}
