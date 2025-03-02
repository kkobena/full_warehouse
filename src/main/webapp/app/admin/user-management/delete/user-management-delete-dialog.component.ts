import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { User } from '../user-management.model';
import { UserManagementService } from '../service/user-management.service';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

@Component({
  selector: 'jhi-user-mgmt-delete-dialog',
  templateUrl: './user-management-delete-dialog.component.html',
  imports: [WarehouseCommonModule, FormsModule],
})
export default class UserManagementDeleteDialogComponent {
  user?: User;

  private readonly userService = inject(UserManagementService);
  private readonly activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(login: string): void {
    this.userService.delete(login).subscribe(() => {
      this.activeModal.close('deleted');
    });
  }
}
