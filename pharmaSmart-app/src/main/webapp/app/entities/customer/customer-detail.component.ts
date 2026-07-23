import {ChangeDetectionStrategy, Component, inject, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {ISales} from 'app/shared/model/sales.model';
import {ISalesLine} from 'app/shared/model/sales-line.model';
import {ICustomer} from 'app/shared/model/customer.model';
import {IAvoirClientDocument} from 'app/shared/model/avoir-client-document.model';
import {CustomerService} from './customer.service';
import {HttpResponse} from '@angular/common/http';
import {MagasinService} from '../magasin/magasin.service';
import {IMagasin} from 'app/shared/model/magasin.model';
import {SalesService} from '../sales/sales.service';
import {Subject} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {NgbNavModule} from '@ng-bootstrap/ng-bootstrap';
import {ButtonComponent, CardComponent, NavTabsComponent} from '../../shared/ui';
import TranslateDirective from "../../shared/language/translate.directive";
import {CommonModule} from "@angular/common";
import {AlertErrorComponent} from "../../shared/alert/alert-error.component";

@Component({
  selector: 'app-customer-detail',

  templateUrl: './customer-detail.component.html',
  styleUrls: ['./customer-detail.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [NgbNavModule, ButtonComponent, NavTabsComponent, TranslateDirective, CommonModule, AlertErrorComponent, CardComponent]
})
export class CustomerDetailComponent implements OnInit, OnDestroy {
  customer: ICustomer | null = null;
  sales: ISales[] = [];
  avoirs: IAvoirClientDocument[] = [];
  selectedRowIndex?: number;
  selectedRowSaleLines?: ISalesLine[] = [];
  saleSelected?: ISales;
  magasin?: IMagasin;
  protected activatedRoute = inject(ActivatedRoute);
  protected customerService = inject(CustomerService);
  protected magasinService = inject(MagasinService);
  protected salesService = inject(SalesService);
  private destroy$ = new Subject<void>();

  get avoirsOuverts(): IAvoirClientDocument[] {
    return this.avoirs.filter(a => a.statut === 'OUVERT');
  }

  get soldeTotalAvoirs(): number {
    return this.avoirsOuverts.reduce((sum, a) => sum + (a.montant ?? 0), 0);
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({customer}) => (this.customer = customer));
    this.loadSales();
    this.loadAvoirs();
    this.selectedRowIndex = 0;
    this.magasinService.findCurrentUserMagasin().then(magasin => {
      this.magasin = magasin;
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  previousState(): void {
    window.history.back();
  }

  loadAvoirs(): void {
    if (!this.customer?.id) {
      return;
    }
    this.customerService
      .avoirsByCustomer(this.customer.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: avoirs => (this.avoirs = avoirs), error: () => {
        }
      });
  }

  openAvoirPdf(avoirId: number): void {
    window.open(`/api/sales/retours/avoirs/${avoirId}/pdf`, '_blank');
  }

  loadSales(): void {
    this.customerService
      .purchases({
        customerId: this.customer.id,
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: HttpResponse<ISales[]>) => this.onSuccess(res.body),
        error: () => this.onError(),
      });
  }

  clickRow(item: ISales): void {
    this.selectedRowIndex = item.id;
    this.selectedRowSaleLines = item.salesLines;
    this.saleSelected = item;
  }

  print(): void {
    if (this.saleSelected !== null && this.saleSelected !== undefined) {
      this.salesService
        .print(this.saleSelected.saleId)
        .pipe(takeUntil(this.destroy$))
        .subscribe(blod => {
          const blobUrl = URL.createObjectURL(blod);
          window.open(blobUrl);
        });
    }
  }

  protected onSuccess(data: ISales[] | null): void {
    this.sales = data || [];
  }

  protected onError(): void {
  }
}
