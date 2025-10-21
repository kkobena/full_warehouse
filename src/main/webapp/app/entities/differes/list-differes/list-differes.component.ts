import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { Button } from 'primeng/button';
import { FormsModule } from '@angular/forms';
import { Toolbar } from 'primeng/toolbar';
import { PrimeNG } from 'primeng/config';
import { CommonModule } from '@angular/common';
import { DiffereService } from '../differe.service';
import { SelectModule } from 'primeng/select';
import { ClientDiffere } from '../model/client-differe.model';
import { StatutDiffere } from '../model/statut-differe';
import { Differe } from '../model/differe.model';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Tooltip } from 'primeng/tooltip';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { RouterModule } from '@angular/router';
import { CardModule } from 'primeng/card';
import { Tag } from 'primeng/tag';
import { TranslateService } from '@ngx-translate/core';
import { Subject, Subscription } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DiffereSummary } from '../model/differe-summary.model';

@Component({
  selector: 'jhi-list-differes',
  imports: [Button, FormsModule, Toolbar, CommonModule, SelectModule, CardModule, TableModule, Tooltip, RouterModule, Tag],
  templateUrl: './list-differes.component.html',
  styleUrls: ['./list-differes.component.scss']
})
export class ListDifferesComponent implements OnInit, OnDestroy {
  primngtranslate: Subscription;
  protected page = 0;
  protected totalItems = 0;
  protected loading!: boolean;
  protected readonly  hideStatusFilter = true;
  protected loadingBtn = false;
  protected loadingPdf = false;
  protected clients: ClientDiffere[] = [];
  protected summary: DiffereSummary | null = null;
  protected typesDifferes = [
    {
      id: StatutDiffere.PAYE,
      label: 'Soldé'
    },
    {
      id: StatutDiffere.IMPAYE,
      label: 'En cours'
    }
  ];
  protected customerId: number = null;
  protected statut: StatutDiffere = StatutDiffere.IMPAYE;
  protected data: Differe[] = [];
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected readonly primeNGConfig = inject(PrimeNG);
  private readonly differeService = inject(DiffereService);
  private translate = inject(TranslateService);
  private destroy$ = new Subject<void>();

  constructor() {
    this.translate.use('fr');
    this.primngtranslate = this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
  }
 protected onStatutChange(evt:any  ): void {

    this.onSerch();
  }
  protected onChange(evt:any  ): void {

    this.onSerch();
  }

  ngOnInit(): void {
    this.fetchClients();
    const params = this.differeService.differeParams();
    if (params) {
      this.customerId = params.customerId;
      this.statut = params.statut;
      // this.modelStartDate = params.fromDate;
      //  this.modelEndDate = params.toDate;
    }
    this.onSerch();
  }

  ngOnDestroy(): void {
    this.primngtranslate.unsubscribe();
    this.destroy$.next();
    this.destroy$.complete();
  }


  protected exportPdf(): void {
    this.loadingPdf = true;
    this.differeService.exportListToPdf(this.buildQueryParams()).pipe(takeUntil(this.destroy$)).subscribe({
      next: (blob: Blob) => {
        this.loadingPdf = false;
        const blobUrl = URL.createObjectURL(blob);
        window.open(blobUrl);
      },
      error: () => {
        this.loadingPdf = false;
      }
    });
  }

  protected lazyLoading(event: TableLazyLoadEvent): void {
    if (event) {
      this.loadingBtn = true;
      this.page = event.first / event.rows;
      this.loading = true;
      this.differeService
        .query({
          page: this.page,
          size: event.rows,
          ...this.buildQueryParams()
        }).pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (res: HttpResponse<Differe[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => {
            this.loading = false;
            this.loadingBtn = false;
          }
        });
    }
  }

  protected loadData(): void {
    this.loadingBtn = true;
    const pageToLoad: number = this.page;
    this.loading = true;
    this.differeService
      .query({
        page: pageToLoad,
        size: this.itemsPerPage,
        ...this.buildQueryParams()
      }).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: HttpResponse<Differe[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => {
          this.loading = false;
          this.loadingBtn = false;
        }
      });
  }

  protected onSerch(): void {
    this.loadData();
    this.loadDiffereSummary();
  }

  private fetchClients(): void {
    this.differeService.findClients().pipe(takeUntil(this.destroy$)).subscribe({
      next: res => {
        this.clients = res.body;
      },
      error: () => {
        this.clients = [];
      }
    });
  }

  private onSuccess(data: Differe[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.data = data || [];
    this.loading = false;
    this.loadingBtn = false;
  }

  private buildQueryParams(): any {
    const params: any = {};
    if (this.customerId) {
      params.customerId = this.customerId;
    }
    if (this.statut) {
      params.paymentStatuses = [this.statut];
    }
    /* if (this.modelStartDate) {
      params.fromDate = DATE_FORMAT_ISO_DATE(this.modelStartDate);
    }
    if (this.modelEndDate) {
      params.toDate = DATE_FORMAT_ISO_DATE(this.modelEndDate);
    }*/
    this.differeService.setParams({
      customerId: this.customerId,
      statut: this.statut
      // fromDate: this.modelStartDate,
      // toDate: this.modelEndDate,
    });
    return params;
  }

  private loadDiffereSummary(): void {
    this.differeService.getDiffereSummary(this.buildQueryParams()).pipe(takeUntil(this.destroy$)).subscribe({
      next: (res: HttpResponse<DiffereSummary>) => {
        this.summary = res.body;
      },
      error: () => {
        this.summary = null;
      }
    });
  }
}
