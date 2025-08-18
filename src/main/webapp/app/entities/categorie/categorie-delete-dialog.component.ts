import { Component, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { ICategorie } from 'app/shared/model/categorie.model';
import { CategorieService } from './categorie.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';

@Component({
  templateUrl: './categorie-delete-dialog.component.html',
  imports: [WarehouseCommonModule, FormsModule]
})
export class CategorieDeleteDialogComponent {
  protected categorieService = inject(CategorieService);
  activeModal = inject(NgbActiveModal);

  categorie?: ICategorie;

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.categorieService.delete(id).subscribe(() => {
      this.activeModal.close();
    });
  }
}
