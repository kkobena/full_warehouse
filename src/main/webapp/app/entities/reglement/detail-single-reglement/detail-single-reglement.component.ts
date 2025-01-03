import { Component, inject, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { InvoicePaymentItem, Reglement } from '../model/reglement.model';
import { InputTextModule } from 'primeng/inputtext';
import { PanelModule } from 'primeng/panel';
import { PrimeTemplate } from 'primeng/api';
import { TableModule } from 'primeng/table';
import { ReglementService } from '../reglement.service';
import { HttpResponse } from '@angular/common/http';

@Component({
  selector: 'jhi-detail-single-reglement',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, InputTextModule, PanelModule, PrimeTemplate, TableModule],
  templateUrl: './detail-single-reglement.component.html',
  styles: ``,
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
    if (this.reglement && this.reglement.id) {
      this.reglementService.getItems(this.reglement.id).subscribe((res: HttpResponse<InvoicePaymentItem[]>) => {
        this.datas = res?.body || [];
      });
    }
  }
}
