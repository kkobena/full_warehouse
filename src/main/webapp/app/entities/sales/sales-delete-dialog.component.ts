import { Component } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ISales } from 'app/shared/model/sales.model';
import { SalesService } from './sales.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';

@Component({
  templateUrl: './sales-delete-dialog.component.html',
  standalone: true,
  imports: [WarehouseCommonModule, FormsModule],
})
export class SalesDeleteDialogComponent {
  sales?: ISales;

  constructor(
    protected salesService: SalesService,
    public activeModal: NgbActiveModal,
  ) {}

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.salesService.delete(id).subscribe(() => {
      this.activeModal.close();
    });
  }
}
