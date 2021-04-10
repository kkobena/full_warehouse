import { Component } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { ISalesLine } from 'app/shared/model/sales-line.model';
import { SalesLineService } from './sales-line.service';

@Component({
  templateUrl: './sales-line-delete-dialog.component.html',
})
export class SalesLineDeleteDialogComponent {
  salesLine?: ISalesLine;

  constructor(protected salesLineService: SalesLineService, public activeModal: NgbActiveModal, protected eventManager: JhiEventManager) {}

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.salesLineService.delete(id).subscribe(() => {
      this.eventManager.broadcast('salesLineListModification');
      this.activeModal.close();
    });
  }
}
