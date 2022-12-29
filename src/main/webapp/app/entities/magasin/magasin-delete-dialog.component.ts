import {Component} from '@angular/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';


import {IMagasin} from 'app/shared/model/magasin.model';
import {MagasinService} from './magasin.service';

@Component({
  templateUrl: './magasin-delete-dialog.component.html',
})
export class MagasinDeleteDialogComponent {
  magasin?: IMagasin;

  constructor(protected magasinService: MagasinService, public activeModal: NgbActiveModal) {
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.magasinService.delete(id).subscribe(() => {

      this.activeModal.close();
    });
  }
}
