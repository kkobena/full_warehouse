import { Component } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { ICommande } from 'app/shared/model/commande.model';
import { CommandeService } from './commande.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';

@Component({
    templateUrl: './commande-delete-dialog.component.html',
    imports: [WarehouseCommonModule, FormsModule]
})
export class CommandeDeleteDialogComponent {
  commande?: ICommande;

  constructor(
    protected commandeService: CommandeService,
    public activeModal: NgbActiveModal,
  ) {}

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.commandeService.delete(id).subscribe(() => {
      this.activeModal.close();
    });
  }
}
