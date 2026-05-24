import { Component, inject, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { IResponseCommande } from '../../shared/model/response-commande.model';
import { IResponseCommandeItem } from '../../shared/model/response-commande-item.model';
import { ICommande } from '../../shared/model/commande.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';

@Component({
  templateUrl: './commande-en-cours-response-dialog.component.html',
  styleUrls: ['../common-modal.component.scss', './commande-en-cours-response-dialog.component.scss'],
  imports: [WarehouseCommonModule],
})
export class CommandeEnCoursResponseDialogComponent implements OnInit {
  header = 'VERIFICATION COMMANDE';
  activeModal = inject(NgbActiveModal);
  responseCommande?: IResponseCommande;
  commande?: ICommande;
  responseCommandeItem: IResponseCommandeItem[] = [];
  responseCommandeItemNonPrisEnComte: IResponseCommandeItem[] = [];
  responseCommandeItemMoitieLivrer: IResponseCommandeItem[] = [];
  classCss = 'col-sm-3';
  classCssNon = 'col-sm-9';

  ngOnInit(): void {
    if (this.responseCommande) {
      this.responseCommandeItem = this.responseCommande.items.filter(e => e.quantitePriseEnCompte > 0);
      this.responseCommandeItemNonPrisEnComte = this.responseCommande.items.filter(e => e.quantitePriseEnCompte < e.quantite);
      this.responseCommandeItemMoitieLivrer = this.responseCommande.items.filter(
        e => e.quantitePriseEnCompte > 0 && e.quantitePriseEnCompte < e.quantite,
      );
    }
    this.getCardClass();
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  private getCardClass(): void {
    if (this.responseCommandeItem.length > 0 && this.responseCommandeItemNonPrisEnComte.length > 0) {
      this.classCss = 'col-sm-3';
    } else {
      this.classCss = 'col-sm-8';
    }
  }
}
