import { inject, Injectable } from '@angular/core';
import { IProduit } from '../../../shared/model/produit.model';
import { ProduitService } from '../../produit/produit.service';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ISalesLine } from '../../../shared/model/sales-line.model';
import { DeconditionService } from '../../decondition/decondition.service';
import { Decondition, IDecondition } from '../../../shared/model/decondition.model';
import { ErrorService } from '../../../shared/error.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { showCommonError, translateSalesLabel } from '../selling-home/sale-helper';
import { TranslateService } from '@ngx-translate/core';

@Injectable({ providedIn: 'root' })
export class DeconditionnementService {
  private produitService = inject(ProduitService);
  private decondtionService = inject(DeconditionService);
  private errorService = inject(ErrorService);
  private modalService = inject(NgbModal);
  private readonly translate = inject(TranslateService);

  handleDeconditionnement(
    qytMvt: number,
    produit: IProduit,
    confimDialog: ConfirmDialogComponent,
    item?: ISalesLine | null,
    addProduitFn?: (qytMvt: number) => void,
    processQtyRequestedFn?: (item: ISalesLine) => void,
    updateProduitQtyBoxFn?: () => void
  ): void {
    this.produitService.find(produit.produitId).subscribe(res => {
      const prod = res.body;
      if (prod && prod.totalQuantity > 0) {
        this.confirmDeconditionnement(
          confimDialog,
          item,
          prod,
          qytMvt,
          addProduitFn,
          processQtyRequestedFn,
          updateProduitQtyBoxFn
        );
      } else {
        showCommonError(this.modalService, this.translateLabel('stockInsuffisant'));
      }
    });
  }

  private confirmDeconditionnement(
    confimDialog: ConfirmDialogComponent,
    item: ISalesLine | null,
    produit: IProduit,
    qytMvt: number,
    addProduitFn?: (qytMvt: number) => void,
    processQtyRequestedFn?: (item: ISalesLine) => void,
    updateProduitQtyBoxFn?: () => void
  ): void {
    confimDialog.onConfirm(
      () => {
        const qtyDetail = produit.itemQty;
        if (qtyDetail) {
          const qtyDecondtionner = Math.round(qytMvt / qtyDetail);
          this.decondtionService.create(this.createDecondition(qtyDecondtionner, produit.id)).subscribe({
            next: () => {
              if (item) {
                processQtyRequestedFn(item);
              } else {
                addProduitFn(qytMvt);
              }
            },
            error: error => {
              if (error.error && error.error.status === 500) {
                showCommonError(this.modalService, 'Erreur applicative');
              } else {
                showCommonError(this.modalService, this.errorService.getErrorMessage(error));
              }
            }
          });
        }
      },
      this.translateLabel('deconditionnementHeader'),
      this.translateLabel('deconditionnementMessage'),
      null,
      () => {
        updateProduitQtyBoxFn();
      }
    );
  }

  private createDecondition(qtyDeconditione: number, produitId: number): IDecondition {
    return {
      ...new Decondition(),
      qtyMvt: qtyDeconditione,
      produitId
    };
  }

  private translateLabel(label: string): string {
    return translateSalesLabel(this.translate, label);
  }
}
