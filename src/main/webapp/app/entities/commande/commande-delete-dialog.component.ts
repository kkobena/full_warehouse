import { Component, inject } from '@angular/core';
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
  protected commandeService = inject(CommandeService);
  activeModal = inject(NgbActiveModal);

  commande?: ICommande;

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.commandeService.delete(id).subscribe(() => {
      this.activeModal.close();
    });
  }
}
