import { Component, OnInit, ViewChild } from '@angular/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { IDelivery } from '../../../shared/model/delevery.model';
import { DeliveryService } from './delivery.service';
import { Router } from '@angular/router';
import { ImportDeliveryFormComponent } from './form/import/import-delivery-form.component';
import { ICommandeResponse } from '../../../shared/model/commande-response.model';
import { NgxSpinnerService } from 'ngx-spinner';
import { BonEnCoursComponent } from './bon-en-cours/bon-en-cours.component';
import { ListBonsComponent } from './list-bons/list-bons.component';

@Component({
  selector: 'jhi-delevery',
  templateUrl: './delivery.component.html',
  providers: [ConfirmationService, DialogService, MessageService],
})
export class DeliveryComponent implements OnInit {
  search = '';
  ref?: DynamicDialogRef;
  protected active = 'pending';
  @ViewChild(BonEnCoursComponent)
  private enCoursComponent: BonEnCoursComponent;
  @ViewChild(ListBonsComponent)
  private listBonsComponent: ListBonsComponent;

  constructor(
    protected router: Router,
    private spinner: NgxSpinnerService,
    protected entityService: DeliveryService,
    private dialogService: DialogService
  ) {}

  ngOnInit(): void {}

  onSearch(): void {
    if (this.active === 'pending') {
      this.enCoursComponent.onSearch();
    } else {
      this.listBonsComponent.onSearch();
    }
  }

  onImportNew(): void {
    this.ref = this.dialogService.open(ImportDeliveryFormComponent, {
      header: 'IMPORTATION DE NOUVEAU BON DE LIVRAISON',
      width: '40%',
    });
    this.ref.onClose.subscribe((response: ICommandeResponse) => {
      if (response) {
        this.gotoEntreeStockComponent(response.entity.id);
      }
    });
  }

  gotoEntreeStockComponent(delivery: IDelivery): void {
    this.router.navigate(['/gestion-entree', delivery.id, 'stock-entry']);
  }
}
