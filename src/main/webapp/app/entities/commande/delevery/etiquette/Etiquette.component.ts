import { Component, OnInit } from '@angular/core';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { IDelivery } from '../../../../shared/model/delevery.model';
import { saveAs } from 'file-saver';
import { DATE_FORMAT_DD_MM_YYYY_HH_MM_SS } from '../../../../shared/util/warehouse-util';
import { DeliveryService } from '../delivery.service';

@Component({
  selector: 'jhi-form-lot',
  templateUrl: './etiquette.component.html',
})
export class EtiquetteComponent implements OnInit {
  isSaving = false;
  entity?: IDelivery;
  startAt: number = 1;

  constructor(protected entityService: DeliveryService, public ref: DynamicDialogRef, public config: DynamicDialogConfig) {}

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
    this.entityService.printEtiquette(this.entity?.id!, { startAt: this.startAt }).subscribe({
      next: (blod: Blob) => {
        saveAs(blod, this.entity.receiptRefernce + '_' + DATE_FORMAT_DD_MM_YYYY_HH_MM_SS());
        this.cancel();
      },
      error: () => (this.isSaving = false),
    });
  }
}
