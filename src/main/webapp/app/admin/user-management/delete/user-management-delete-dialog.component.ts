import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { User } from '../user-management.model';
import { UserManagementService } from '../service/user-management.service';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

@Component({
  standalone: true,
  selector: 'jhi-user-mgmt-delete-dialog',
  templateUrl: './user-management-delete-dialog.component.html',
  imports: [WarehouseCommonModule, FormsModule],
})
export default class UserManagementDeleteDialogComponent {
  user?: User;

  private userService = inject(UserManagementService);
  private activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(login: string): void {
    this.userService.delete(login).subscribe(() => {
      this.activeModal.close('deleted');
    });
  }
}
