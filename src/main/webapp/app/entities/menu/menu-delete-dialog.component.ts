import {Component} from '@angular/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';


import {IMenu} from 'app/shared/model/menu.model';
import {MenuService} from './menu.service';

@Component({
  templateUrl: './menu-delete-dialog.component.html',
})
export class MenuDeleteDialogComponent {
  menu?: IMenu;

  constructor(protected menuService: MenuService, public activeModal: NgbActiveModal) {
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.menuService.delete(id).subscribe(() => {

      this.activeModal.close();
    });
  }
}
