import { Component, inject, OnDestroy, OnInit } from '@angular/core';
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
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
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
export class EtiquetteComponent implements OnInit, OnDestroy {
  isSaving = false;
  entity?: IDelivery;
  startAt = 1;
  entityService = inject(DeliveryService);
  ref = inject(DynamicDialogRef);
  config = inject(DynamicDialogConfig);
  spinner = inject(NgxSpinnerService);
  private destroy$ = new Subject<void>();

  constructor() {}

  ngOnInit(): void {
    this.entity = this.config.data.entity;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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
    this.entityService.printEtiquette(this.entity.id, { startAt: this.startAt }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (blod: Blob) => {
        this.spinner.hide();
        saveAs(blod, this.entity.receiptReference + '_' + DATE_FORMAT_DD_MM_YYYY_HH_MM_SS());
        this.cancel();
      },
      error: () => {
        this.spinner.hide();
        this.isSaving = false;
      },
    });
  }
}
