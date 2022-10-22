import { Component } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IMenu } from 'app/shared/model/menu.model';
import { MenuService } from './menu.service';

@Component({
  templateUrl: './menu-delete-dialog.component.html',
})
export class MenuDeleteDialogComponent {
  menu?: IMenu;

  constructor(protected menuService: MenuService, public activeModal: NgbActiveModal, protected eventManager: JhiEventManager) {}

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.menuService.delete(id).subscribe(() => {
      this.eventManager.broadcast('menuListModification');
      this.activeModal.close();
    });
  }
}
