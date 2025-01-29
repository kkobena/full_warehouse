import { Component, ViewChild } from '@angular/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { IDelivery } from '../../../shared/model/delevery.model';
import { DeliveryService } from './delivery.service';
import { Router, RouterModule } from '@angular/router';
import { ImportDeliveryFormComponent } from './form/import/import-delivery-form.component';
import { ICommandeResponse } from '../../../shared/model/commande-response.model';
import { BonEnCoursComponent } from './bon-en-cours/bon-en-cours.component';
import { ListBonsComponent } from './list-bons/list-bons.component';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { CardModule } from 'primeng/card';
import { ToolbarModule } from 'primeng/toolbar';
import { FormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { PanelModule } from 'primeng/panel';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';

@Component({
  selector: 'jhi-delevery',
  templateUrl: './delivery.component.html',
  providers: [ConfirmationService, DialogService, MessageService],
  imports: [
    WarehouseCommonModule,
    BonEnCoursComponent,
    ListBonsComponent,
    ButtonModule,
    CardModule,
    RouterModule,
    RippleModule,
    DynamicDialogModule,
    ToolbarModule,
    FormsModule,
    InputTextModule,
    PanelModule,
    IconField,
    InputIcon,
  ],
})
export class DeliveryComponent {
  search = '';
  ref?: DynamicDialogRef;
  protected active = 'pending';
  @ViewChild(BonEnCoursComponent)
  private enCoursComponent: BonEnCoursComponent;
  @ViewChild(ListBonsComponent)
  private listBonsComponent: ListBonsComponent;

  constructor(
    protected router: Router,
    protected entityService: DeliveryService,
    private dialogService: DialogService,
  ) {}

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
        this.onSearch();
        //  this.gotoEntreeStockComponent(response.entity.id);
      }
    });
  }

  gotoEntreeStockComponent(delivery: IDelivery): void {
    this.router.navigate(['/gestion-entree', delivery.id, 'stock-entry']);
  }
}
