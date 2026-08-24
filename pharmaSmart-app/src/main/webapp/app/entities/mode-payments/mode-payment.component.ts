import { Component, inject, OnInit, ChangeDetectionStrategy, signal } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { NgbModal, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { IPaymentMode } from '../../shared/model/payment-mode.model';
import { ModePaymentService } from './mode-payment.service';
import {
  AppTableLazyLoadEvent,
  ButtonComponent,
  DataTableComponent,
  IconFieldComponent,
  SelectableRowDirective,
} from '../../shared/ui';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { showCommonModal } from '../sales/selling-home/sale-helper';
import { ModePaymentUpdateComponent } from './mode-payment-update.component';

@Component({
  selector: 'app-mode-payment',
  templateUrl: './mode-payment.component.html',
  styleUrls: ['./mode-payment.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonComponent, DataTableComponent, SelectableRowDirective, IconFieldComponent, NgbTooltip],
})
export class ModePaymentComponent implements OnInit {
  protected readonly paymentModes = signal<IPaymentMode[] | undefined>(undefined);
  protected readonly totalItems = signal(0);
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected readonly page = signal(0);
  protected readonly loading = signal<boolean | undefined>(undefined);

  private readonly modalService = inject(NgbModal);
  private readonly modePaymentService = inject(ModePaymentService);

  ngOnInit(): void {
    this.loadPage();
  }

  protected search(event: any): void {
    this.loadPage(0, event.target.value);
  }

  protected loadPage(page?: number, search?: string): void {
    const pageToLoad: number = page || this.page();
    this.loading.set(true);
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

  protected lazyLoading(event: AppTableLazyLoadEvent): void {
    if (event) {
      this.page.set(event.first / event.rows);
      this.loading.set(true);
      this.modePaymentService
        .query({
          page: this.page(),
          size: event.rows,
        })
        .subscribe({
          next: (res: HttpResponse<IPaymentMode[]>) => this.onSuccess(res.body, res.headers, this.page()),
          error: () => this.onError(),
        });
    }
  }

  protected onEdit(entity: IPaymentMode): void {
    showCommonModal(
      this.modalService,
      ModePaymentUpdateComponent,
      {
        entity,
        title: 'Modification de ' + entity.libelle,
      },
      () => {
        this.loadPage(0);
      },
      'lg',
    );
  }

  private onSuccess(data: IPaymentMode[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems.set(Number(headers.get('X-Total-Count')));
    this.page.set(page);
    this.paymentModes.set(data || []);
    this.loading.set(false);
  }

  private onError(): void {
    this.loading.set(false);
  }
}
