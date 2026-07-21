import { Component, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { BadgeComponent, ButtonComponent } from 'app/shared/ui';
import { IResponseCommande } from '../../../../shared/model/response-commande.model';
import { IResponseCommandeItem } from '../../../../shared/model/response-commande-item.model';
import { ICommande } from '../../../../shared/model/commande.model';

@Component({
  selector: 'app-commande-response-dialog',
  templateUrl: './commande-response-dialog.component.html',
  styleUrls: ['./commande-response-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [DecimalPipe, ButtonComponent, BadgeComponent],
})
export class CommandeResponseDialogComponent implements OnInit {
  header = 'VÉRIFICATION COMMANDE';
  responseCommande?: IResponseCommande;
  commande?: ICommande;

  protected responseCommandeItem: IResponseCommandeItem[] = [];
  protected responseCommandeItemNonPrisEnCompte: IResponseCommandeItem[] = [];
  protected responseCommandeItemMoitieLivrer: IResponseCommandeItem[] = [];
  protected extraItems: IResponseCommandeItem[] = [];

  private readonly activeModal = inject(NgbActiveModal);

  ngOnInit(): void {
    const items = this.responseCommande?.items ?? [];
    this.responseCommandeItem = items.filter(e => (e.quantitePriseEnCompte ?? 0) > 0);
    this.responseCommandeItemNonPrisEnCompte = items.filter(
      e => (e.quantitePriseEnCompte ?? 0) < (e.quantite ?? 0),
    );
    this.responseCommandeItemMoitieLivrer = items.filter(
      e => (e.quantitePriseEnCompte ?? 0) > 0 && (e.quantitePriseEnCompte ?? 0) < (e.quantite ?? 0),
    );
    this.extraItems = this.responseCommande?.extraItems ?? [];
  }

  protected close(): void {
    this.activeModal.dismiss();
  }

  protected onDeleteCommande(): void {
    this.activeModal.close('DELETE');
  }
}
