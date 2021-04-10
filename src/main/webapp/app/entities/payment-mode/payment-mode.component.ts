import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { JhiEventManager, JhiParseLinks } from 'ng-jhipster';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { IPaymentMode } from 'app/shared/model/payment-mode.model';

import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { PaymentModeService } from './payment-mode.service';
import { PaymentModeDeleteDialogComponent } from './payment-mode-delete-dialog.component';

@Component({
  selector: 'jhi-payment-mode',
  templateUrl: './payment-mode.component.html',
})
export class PaymentModeComponent implements OnInit, OnDestroy {
  paymentModes: IPaymentMode[];
  eventSubscriber?: Subscription;
  itemsPerPage: number;
  links: any;
  page: number;
  predicate: string;
  ascending: boolean;

  constructor(
    protected paymentModeService: PaymentModeService,
    protected eventManager: JhiEventManager,
    protected modalService: NgbModal,
    protected parseLinks: JhiParseLinks
  ) {
    this.paymentModes = [];
    this.itemsPerPage = ITEMS_PER_PAGE;
    this.page = 0;
    this.links = {
      last: 0,
    };
    this.predicate = 'id';
    this.ascending = true;
  }

  loadAll(): void {
    this.paymentModeService
      .query({
        page: this.page,
        size: this.itemsPerPage,
        sort: this.sort(),
      })
      .subscribe((res: HttpResponse<IPaymentMode[]>) => this.paginatePaymentModes(res.body, res.headers));
  }

  reset(): void {
    this.page = 0;
    this.paymentModes = [];
    this.loadAll();
  }

  loadPage(page: number): void {
    this.page = page;
    this.loadAll();
  }

  ngOnInit(): void {
    this.loadAll();
    this.registerChangeInPaymentModes();
  }

  ngOnDestroy(): void {
    if (this.eventSubscriber) {
      this.eventManager.destroy(this.eventSubscriber);
    }
  }

  trackId(index: number, item: IPaymentMode): number {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.id!;
  }

  registerChangeInPaymentModes(): void {
    this.eventSubscriber = this.eventManager.subscribe('paymentModeListModification', () => this.reset());
  }

  delete(paymentMode: IPaymentMode): void {
    const modalRef = this.modalService.open(PaymentModeDeleteDialogComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.paymentMode = paymentMode;
  }

  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'id') {
      result.push('id');
    }
    return result;
  }

  protected paginatePaymentModes(data: IPaymentMode[] | null, headers: HttpHeaders): void {
    const headersLink = headers.get('link');
    this.links = this.parseLinks.parse(headersLink ? headersLink : '');
    if (data) {
      for (let i = 0; i < data.length; i++) {
        this.paymentModes.push(data[i]);
      }
    }
  }
}
