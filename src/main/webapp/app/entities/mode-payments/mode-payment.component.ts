import { Component, inject, OnInit } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { IPaymentMode } from '../../shared/model/payment-mode.model';
import { ModePaymentService } from './mode-payment.service';
import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Tooltip } from 'primeng/tooltip';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { showCommonModal } from '../sales/selling-home/sale-helper';
import { ModePaymentUpdateComponent } from './mode-payment-update.component';

@Component({
  selector: 'jhi-mode-payment',
  templateUrl: './mode-payment.component.html',
  styleUrls: ['./mode-payment.component.scss'],
  imports: [ButtonModule, ToolbarModule, TableModule, Tooltip, IconField, InputIcon, InputText],
})
export class ModePaymentComponent implements OnInit {
  protected paymentModes?: IPaymentMode[];
  protected totalItems = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = 0;
  protected loading!: boolean;

  private readonly modalService = inject(NgbModal);
  private readonly modePaymentService = inject(ModePaymentService);

  ngOnInit(): void {
    this.loadPage();
  }

  protected search(event: any): void {
    this.loadPage(0, event.target.value);
  }

  protected loadPage(page?: number, search?: string): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.modePaymentService
      .query({
        page: pageToLoad,
        size: this.itemsPerPage,
        search: search || null,
      })
      .subscribe({
        next: (res: HttpResponse<IPaymentMode[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }

  protected lazyLoading(event: TableLazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.modePaymentService
        .query({
          page: this.page,
          size: event.rows,
        })
        .subscribe({
          next: (res: HttpResponse<IPaymentMode[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError(),
        });
    }
  }

  protected onEdit(entity: IPaymentMode): void {
    showCommonModal(
      this.modalService,
      ModePaymentUpdateComponent,
      {
        entity: entity,
        title: 'Modification de ' + entity.libelle,
      },
      () => {
        this.loadPage(0);
      },
      'lg',
    );
  }

  private onSuccess(data: IPaymentMode[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.paymentModes = data || [];
    this.loading = false;
  }

  private onError(): void {
    this.loading = false;
  }
}
