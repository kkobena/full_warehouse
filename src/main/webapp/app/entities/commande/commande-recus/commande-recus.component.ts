import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { DeliveryService } from '../delevery/delivery.service';
import { NgxSpinnerModule } from 'ngx-spinner';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { saveAs } from 'file-saver';
import { IDelivery } from '../../../shared/model/delevery.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';

export type ExpandMode = 'single' | 'multiple';

@Component({
    selector: 'jhi-commande-recus',
    templateUrl: './commande-recus.component.html',
    imports: [
        WarehouseCommonModule,
        ButtonModule,
        TableModule,
        NgxSpinnerModule,
        RouterModule,
        RippleModule,
        DynamicDialogModule,
        TooltipModule,
    ]
})
export class CommandeRecusComponent implements OnInit {
  @Input() search = '';
  @Input() searchByRef = '';
  @Output() selectionLength: EventEmitter<number> = new EventEmitter<number>();
  protected deliveries: IDelivery[] = [];
  protected totalItems = 0;
  protected page = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected predicate!: string;
  protected ascending!: boolean;
  protected ngbPaginationPage = 1;
  protected index = 0;
  protected selectedFilter = 'PENDING';
  protected rowExpandMode: ExpandMode = 'single';
  protected loading!: boolean;
  protected fileDialog = false;
  protected ref!: DynamicDialogRef;

  constructor(
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: NgbModal,
    protected entityService: DeliveryService,
  ) {}

  ngOnInit(): void {
    this.onSearch();
  }

  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'updatedAt') {
      result.push('updatedAt');
    }
    return result;
  }

  exportCSV(delivery: IDelivery): void {
    this.entityService.exportToCsv(delivery.id).subscribe(blod => saveAs(blod));
  }

  exportPdf(delivery: IDelivery): void {
    this.entityService.exportToPdf(delivery.id).subscribe(blod => {
      const blobUrl = URL.createObjectURL(blod);
      window.open(blobUrl);
    });
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.entityService
      .query({
        page: pageToLoad,
        size: this.itemsPerPage,
        search: this.search,
        statut: this.selectedFilter,
        searchByRef: this.searchByRef,
      })
      .subscribe({
        next: (res: HttpResponse<IDelivery[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }

  cancel(): void {
    this.fileDialog = false;
  }

  onSearch(): void {
    this.loadPage(0);
  }

  protected onSuccess(data: IDelivery[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.router.navigate(['/commande'], {
      queryParams: {
        page: this.page,
        size: this.itemsPerPage,
        search: this.search,
        searchByRef: this.searchByRef,
      },
    });
    this.deliveries = data || [];
    this.loading = false;
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }
}
