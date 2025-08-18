import { Component, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { IAuthority } from '../../shared/model/authority.model';
import { PrivillegeService } from './privillege.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';

@Component({
  templateUrl: './menu-delete-dialog.component.html',
  imports: [WarehouseCommonModule, FormsModule]
})
export class MenuDeleteDialogComponent {
  activeModal = inject(NgbActiveModal);
  authority?: IAuthority;
  protected privillegeService = inject(PrivillegeService);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(name: string): void {
    this.privillegeService.deleteRole(name).subscribe(() => {
      this.activeModal.close();
    });
  }
}
