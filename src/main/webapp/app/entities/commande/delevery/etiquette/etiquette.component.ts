import { AfterViewInit, Component, ElementRef, inject, OnDestroy, viewChild } from '@angular/core';
import { IDelivery } from '../../../../shared/model/delevery.model';
import { saveAs } from 'file-saver';
import { DATE_FORMAT_DD_MM_YYYY_HH_MM_SS } from '../../../../shared/util/warehouse-util';
import { DeliveryService } from '../delivery.service';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { FormsModule } from '@angular/forms';
import { KeyFilterModule } from 'primeng/keyfilter';
import { InputTextModule } from 'primeng/inputtext';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { SpinerService } from '../../../../shared/spiner.service';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Card } from 'primeng/card';

@Component({
  selector: 'jhi-etiquette-delevery',
  templateUrl: './etiquette.component.html',
  styleUrls: ['../../../common-modal.component.scss'],
  imports: [
    WarehouseCommonModule,
    KeyFilterModule,
    ButtonModule,
    FormsModule,
    InputTextModule,
    Card
  ]
})
export class EtiquetteComponent implements AfterViewInit, OnDestroy {
  isSaving = false;
  entity?: IDelivery;
  header = 'Impression des étiquettes';
  protected startAt = 1;
  private readonly entityService = inject(DeliveryService);

  spinner = inject(SpinerService);
  private destroy$ = new Subject<void>();
  private readonly activeModal = inject(NgbActiveModal);
  private readonly startInput = viewChild.required<ElementRef>('startInput');


  ngAfterViewInit(): void {
    setTimeout(() => {
      this.startInput().nativeElement.focus();
    }, 100);
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
    this.activeModal.dismiss();
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
      }
    });
  }
}
