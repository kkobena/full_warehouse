import { NgModule } from '@angular/core';

import { FormMotifAjustementComponent } from './form-motif-ajustement/form-motif-ajustement.component';
import { ModifAjustementComponent } from './modif-ajustement.component';
import { SharedModule } from '../../shared/shared.module';
import { RouterModule } from '@angular/router';
import { motifAjustementRoute } from './motif-ajustement.route';

@NgModule({
  declarations: [ModifAjustementComponent, FormMotifAjustementComponent],
  entryComponents: [FormMotifAjustementComponent],

  imports: [SharedModule, RouterModule.forChild(motifAjustementRoute)],
})
export class ModifAjustementModule {}
