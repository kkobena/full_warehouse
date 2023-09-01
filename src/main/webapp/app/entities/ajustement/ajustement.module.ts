import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { SharedModule } from 'app/shared/shared.module';
import { AjustementComponent } from './ajustement.component';
import { AjustementDetailComponent } from './ajustement-detail.component';
import { ajustementRoute } from './ajustement.route';
import { NgSelectModule } from '@ng-select/ng-select';
import { FinalyseComponent } from './finalyse/finalyse.component';
import { AjustementEnCoursComponent } from './ajustement-en-cours/ajustement-en-cours.component';
import { ListAjustementComponent } from './list-ajustement/list-ajustement.component';

@NgModule({
  imports: [SharedModule, NgSelectModule, RouterModule.forChild(ajustementRoute)],
  declarations: [AjustementComponent, AjustementDetailComponent, FinalyseComponent, AjustementEnCoursComponent, ListAjustementComponent],
})
export class WarehouseAjustementModule {}
