import { Component, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { IProduit } from 'app/shared/model/produit.model';
import { ProduitService } from './produit.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';

@Component({
  templateUrl: './produit-delete-dialog.component.html',
  imports: [WarehouseCommonModule, FormsModule],
})
export class ProduitDeleteDialogComponent {
  protected produitService = inject(ProduitService);
  activeModal = inject(NgbActiveModal);

  produit?: IProduit;

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.produitService.delete(id).subscribe(() => {
      this.activeModal.close();
    });
  }
}
