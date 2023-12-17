import { Component } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { IAuthority } from '../../shared/model/authority.model';
import { PrivillegeService } from './privillege.service';

@Component({
  templateUrl: './menu-delete-dialog.component.html',
})
export class MenuDeleteDialogComponent {
  authority?: IAuthority;

  constructor(protected privillegeService: PrivillegeService, public activeModal: NgbActiveModal) {}

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(name: string): void {
    this.privillegeService.deleteRole(name).subscribe(() => {
      this.activeModal.close();
    });
  }
}
