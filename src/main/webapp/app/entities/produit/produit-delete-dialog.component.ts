import { Component } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { IProduit } from 'app/shared/model/produit.model';
import { ProduitService } from './produit.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';

@Component({
    templateUrl: './produit-delete-dialog.component.html',
    imports: [WarehouseCommonModule, FormsModule]
})
export class ProduitDeleteDialogComponent {
  produit?: IProduit;

  constructor(
    protected produitService: ProduitService,
    public activeModal: NgbActiveModal,
  ) {}

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.produitService.delete(id).subscribe(() => {
      this.activeModal.close();
    });
  }
}
