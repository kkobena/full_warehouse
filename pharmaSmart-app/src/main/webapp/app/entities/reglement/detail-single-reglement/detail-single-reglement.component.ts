import {ChangeDetectionStrategy, Component, inject, OnInit} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {NgbActiveModal, NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {InvoicePaymentItem, Reglement} from '../model/reglement.model';
import {ReglementService} from '../reglement.service';
import {HttpResponse} from '@angular/common/http';
import {CommonModule} from '@angular/common';
import {
  ButtonComponent,
  CardComponent,
  DataTableComponent,
  IconFieldComponent,
  KpiItemComponent,
  KpiStripComponent
} from '../../../shared/ui';

@Component({
  selector: 'app-detail-single-reglement',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonComponent,
    CardComponent,
    DataTableComponent,
    IconFieldComponent,
    KpiItemComponent,
    KpiStripComponent
  ],
  templateUrl: './detail-single-reglement.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./detail-single-reglement.component.scss'],
})
export class DetailSingleReglementComponent implements OnInit {
  modalService = inject(NgbModal);
  activeModal = inject(NgbActiveModal);
  reglementService = inject(ReglementService);
  reglement: Reglement | null = null;
  protected datas: InvoicePaymentItem[] = [];
  protected scrollHeight = 'calc(100vh - 350px)';

  cancel(): void {
    this.activeModal.dismiss();
  }

  ngOnInit(): void {
    if (this.reglement) {
      this.reglementService.getItems(this.reglement.id).subscribe((res: HttpResponse<InvoicePaymentItem[]>) => {
        this.datas = res.body || [];
      });
    }
  }
}
