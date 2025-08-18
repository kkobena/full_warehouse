import { inject, Injectable } from '@angular/core';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ISalesLine } from '../../../shared/model/sales-line.model';
import { translateSalesLabel } from '../selling-home/sale-helper';
import { TranslateService } from '@ngx-translate/core';

@Injectable({ providedIn: 'root' })
export class ForceStockService {
  private readonly translate = inject(TranslateService);

  handleForceStock(
    qytMvt: number,
    message: string,
    confimDialog: ConfirmDialogComponent,
    addProduitFn: (qytMvt: number) => void,
    updateProduitQtyBoxFn: () => void
  ): void {
    this.confirmForceStock(qytMvt, message, confimDialog, addProduitFn, updateProduitQtyBoxFn);
  }

  onUpdateConfirmForceStock(
    salesLine: ISalesLine,
    message: string,
    confimDialog: ConfirmDialogComponent,
    processQtyRequestedFn: (salesLine: ISalesLine) => void,
    updateProduitQtyBoxFn: () => void
  ): void {
    confimDialog.onConfirm(
      () => {
        processQtyRequestedFn(salesLine);
      },
      this.translateLabel('forcerStockHeader'),
      message,
      null,
      () => {
        updateProduitQtyBoxFn();
      }
    );
  }

  private confirmForceStock(
    qytMvt: number,
    message: string,
    confimDialog: ConfirmDialogComponent,
    addProduitFn: (qytMvt: number) => void,
    updateProduitQtyBoxFn: () => void
  ): void {
    confimDialog.onConfirm(
      () => {
        addProduitFn(qytMvt);
      },
      this.translateLabel('forcerStockHeader'),
      message,
      null,
      () => {
        updateProduitQtyBoxFn();
      }
    );
  }

  private translateLabel(label: string): string {
    return translateSalesLabel(this.translate, label);
  }
}
