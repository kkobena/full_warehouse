import { Component, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { IResponseCommande } from '../../shared/model/response-commande.model';
import { IResponseCommandeItem } from '../../shared/model/response-commande-item.model';
import { ICommande } from '../../shared/model/commande.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';

@Component({
    templateUrl: './commande-en-cours-response-dialog.component.html',
    imports: [WarehouseCommonModule]
})
export class CommandeEnCoursResponseDialogComponent implements OnInit {
  responseCommande?: IResponseCommande;
  commande?: ICommande;
  responseCommandeItem: IResponseCommandeItem[] = [];
  responseCommandeItemNonPrisEnComte: IResponseCommandeItem[] = [];
  responseCommandeItemMoitieLivrer: IResponseCommandeItem[] = [];
  classCss = 'col-sm-3';
  classCssNon = 'col-sm-9';

  constructor(public activeModal: NgbActiveModal) {}

  ngOnInit(): void {
    this.getItem();
    this.getItemRupture();

    this.getCardClass();
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  getCardClass(): void {
    if (this.responseCommandeItem.length > 0 && this.responseCommandeItemNonPrisEnComte.length > 0) {
      this.classCss = 'col-sm-3';
    } else {
      this.classCss = 'col-sm-8';
    }
  }

  getItem(): void {
    if (this.responseCommande) {
      this.responseCommandeItem = this.responseCommande.items.filter(e => e.quantitePriseEnCompte > 0);
    }
  }

  getItemRupture(): void {
    if (this.responseCommande) {
      this.responseCommandeItemNonPrisEnComte = this.responseCommande.items.filter(e => e.quantitePriseEnCompte < e.quantite);
    }
  }

  getItemLivreMoitie(): IResponseCommandeItem[] {
    if (this.responseCommande) {
      this.responseCommandeItemMoitieLivrer = this.responseCommande.items.filter(
        e => e.quantitePriseEnCompte > 0 && e.quantitePriseEnCompte < e.quantite,
      );
    }
    return [];
  }
}
