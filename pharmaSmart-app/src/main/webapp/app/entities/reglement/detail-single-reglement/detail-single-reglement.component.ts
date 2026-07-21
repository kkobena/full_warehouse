import {ChangeDetectionStrategy, Component, inject, OnInit} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {NgbActiveModal, NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {InvoicePaymentItem, Reglement} from '../model/reglement.model';
import {ReglementService} from '../reglement.service';
import {HttpResponse} from '@angular/common/http';
import {CommonModule} from '@angular/common';
import {
  BadgeComponent,
  ButtonComponent,
  DataTableComponent,
  IconFieldComponent
} from '../../../shared/ui';

@Component({
  selector: 'jhi-detail-single-reglement',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    BadgeComponent,
    ButtonComponent,
    DataTableComponent,
    IconFieldComponent
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
