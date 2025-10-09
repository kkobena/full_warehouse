import { Component, inject, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { InvoicePaymentItem, Reglement } from '../model/reglement.model';
import { InputTextModule } from 'primeng/inputtext';
import { PanelModule } from 'primeng/panel';
import { TableModule } from 'primeng/table';
import { ReglementService } from '../reglement.service';
import { HttpResponse } from '@angular/common/http';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { CommonModule } from '@angular/common';
import { Card } from 'primeng/card';

@Component({
  selector: 'jhi-detail-single-reglement',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    InputTextModule,
    PanelModule,
    TableModule,
    IconField,
    InputIcon,
    Button,
    Tag,
    Card,
  ],
  templateUrl: './detail-single-reglement.component.html',
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
