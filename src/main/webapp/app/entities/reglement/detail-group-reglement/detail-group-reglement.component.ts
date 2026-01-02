import { Component, inject, OnInit } from '@angular/core';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ReglementService } from '../reglement.service';
import { InvoicePaymentItem, Reglement } from '../model/reglement.model';
import { HttpResponse } from '@angular/common/http';
import { InputTextModule } from 'primeng/inputtext';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { CommonModule } from '@angular/common';
import { Button } from 'primeng/button';

@Component({
  selector: 'jhi-detail-group-reglement',
  imports: [CommonModule, FormsModule, InputTextModule, ReactiveFormsModule, TableModule, IconField, InputIcon, Button],
  templateUrl: './detail-group-reglement.component.html',
  styleUrls: ['./detail-group-reglement.component.scss'],
})
export class DetailGroupReglementComponent implements OnInit {
  modalService = inject(NgbModal);
  activeModal = inject(NgbActiveModal);
  reglementService = inject(ReglementService);
  reglement: Reglement | null = null;
  protected datas: InvoicePaymentItem[] = [];
  protected reglements: Reglement[] = [];
  protected selectedItem: Reglement | null = null;
  protected scrollHeight = 'calc(100vh - 350px)';

  cancel(): void {
    this.activeModal.dismiss();
  }

  ngOnInit(): void {
    if (this.reglement && this.reglement.id) {
      this.reglementService.getGroupItems(this.reglement.id).subscribe((res: HttpResponse<Reglement[]>) => {
        this.reglements = res.body || [];
      });
    }
  }

  onRowSelect(re: Reglement) {
    this.selectedItem = re;
    this.reglementService.getItems(re.id).subscribe((res: HttpResponse<InvoicePaymentItem[]>) => {
      this.datas = res.body || [];
    });
  }
}
