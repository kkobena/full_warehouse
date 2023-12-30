import { Component, OnInit } from '@angular/core';
import { DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { IDelivery } from '../../../../shared/model/delevery.model';
import { saveAs } from 'file-saver';
import { DATE_FORMAT_DD_MM_YYYY_HH_MM_SS } from '../../../../shared/util/warehouse-util';
import { DeliveryService } from '../delivery.service';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { FormsModule } from '@angular/forms';
import { KeyFilterModule } from 'primeng/keyfilter';
import { InputTextModule } from 'primeng/inputtext';

@Component({
  standalone: true,
  selector: 'jhi-etiquette-delevery',
  templateUrl: './etiquette.component.html',
  imports: [
    WarehouseCommonModule,
    KeyFilterModule,
    ButtonModule,
    NgxSpinnerModule,
    RippleModule,
    DynamicDialogModule,
    FormsModule,
    InputTextModule,
  ],
})
export class EtiquetteComponent implements OnInit {
  isSaving = false;
  entity?: IDelivery;
  startAt: number = 1;

  constructor(
    protected entityService: DeliveryService,
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
    private spinner: NgxSpinnerService,
  ) {}

  ngOnInit(): void {
    this.entity = this.config.data.entity;
  }

  save(): void {
    this.isSaving = true;
    this.printEtiquette();
  }

  cancel(): void {
    this.isSaving = false;
    this.ref.destroy();
  }

  private printEtiquette(): void {
    this.spinner.show();
    this.entityService.printEtiquette(this.entity.id, { startAt: this.startAt }).subscribe({
      next: (blod: Blob) => {
        this.spinner.hide();
        saveAs(blod, this.entity.receiptRefernce + '_' + DATE_FORMAT_DD_MM_YYYY_HH_MM_SS());
        this.cancel();
      },
      error: () => {
        this.spinner.hide();
        this.isSaving = false;
      },
    });
  }
}
