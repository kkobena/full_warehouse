import { Component, inject, OnInit } from '@angular/core';
import { Button } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CommonModule } from '@angular/common';
import { LazyLoadEvent } from 'primeng/api';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Toolbar } from 'primeng/toolbar';
import { Tooltip } from 'primeng/tooltip';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { ClientDiffere } from '../model/client-differe.model';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { PrimeNG } from 'primeng/config';
import { DiffereService } from '../differe.service';
import { TranslateService } from '@ngx-translate/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ReglementDiffereSummary } from '../model/reglement-differe-summary.model';
import { ReglementDiffere } from '../model/reglement-differe.model';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { FloatLabel } from 'primeng/floatlabel';
import { DatePicker } from 'primeng/datepicker';

@Component({
  selector: 'jhi-reglement-differes',
  imports: [
    Button,
    FormsModule,
    Toolbar,
    CommonModule,
    SelectModule,
    CardModule,
    TableModule,
    Tooltip,
    RouterModule,
    Tag,
    FloatLabel,
    DatePicker,
  ],
  templateUrl: './reglement-differes.component.html',
})
export class ReglementDifferesComponent implements OnInit {
  protected page = 0;
  protected totalItems = 0;
  protected loading!: boolean;
  protected modelStartDate: Date = new Date();
  protected modelEndDate: Date = new Date();
  protected loadingBtn = false;
  protected loadingPdf = false;
  protected clients: ClientDiffere[] = [];
  protected summary: ReglementDiffereSummary | null = null;

  protected customerId: number = null;
  protected data: ReglementDiffere[] = [];
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected readonly primeNGConfig = inject(PrimeNG);
  private readonly differeService = inject(DiffereService);
  private readonly translate = inject(TranslateService);

  constructor() {
    this.translate.use('fr');
     this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
  }

  ngOnInit(): void {
    this.fetchClients();
    const params = this.differeService.differeParams();

    this.customerId = params.customerId;
    this.modelStartDate = params.fromDate || new Date();
    this.modelEndDate = params.toDate || new Date();
    this.onSerch();
  }

  onChange(event: any): void {
    this.customerId = event.value;
    this.onSerch();
  }

  protected exportPdf(): void {
    this.loadingPdf = true;
    this.differeService.exportReglementsToPdf(this.buildQueryParams()).subscribe({
      next: (blob: Blob) => {
        this.loadingPdf = false;
        const blobUrl = URL.createObjectURL(blob);
        window.open(blobUrl);
      },
      error: () => {
        this.loadingPdf = false;
      },
    });
  }

  protected lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.loadingBtn = true;
      this.page = event.first / event.rows;
      this.loading = true;
      this.differeService
        .getReglementsDifferes({
          page: this.page,
          size: event.rows,
          ...this.buildQueryParams(),
        })
        .subscribe({
          next: (res: HttpResponse<ReglementDiffere[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => {
            this.loading = false;
            this.loadingBtn = false;
          },
        });
    }
  }

  protected loadData(): void {
    this.loadingBtn = true;
    const pageToLoad: number = this.page;
    this.loading = true;
    this.differeService
      .getReglementsDifferes({
        page: pageToLoad,
        size: this.itemsPerPage,
        ...this.buildQueryParams(),
      })
      .subscribe({
        next: (res: HttpResponse<ReglementDiffere[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => {
          this.loading = false;
          this.loadingBtn = false;
        },
      });
  }

  protected onSerch(): void {
    this.loadData();
    this.loadDiffereSummary();
  }

  private fetchClients(): void {
    this.differeService.findClients().subscribe({
      next: res => {
        this.clients = res.body;
      },
      error: () => {
        this.clients = [];
      },
    });
  }

  private onSuccess(data: ReglementDiffere[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.data = data;
    this.loading = false;
    this.loadingBtn = false;
  }

  private buildQueryParams(): any {
    const params: any = {};
    if (this.customerId) {
      params.customerId = this.customerId;
    }
    if (this.modelStartDate) {
      params.fromDate = DATE_FORMAT_ISO_DATE(this.modelStartDate);
    }
    if (this.modelEndDate) {
      params.toDate = DATE_FORMAT_ISO_DATE(this.modelEndDate);
    }
    this.differeService.setParams({
      customerId: this.customerId,

      fromDate: this.modelStartDate,
      toDate: this.modelEndDate,
    });
    return params;
  }

  private loadDiffereSummary(): void {
    this.differeService.getReglementDiffereSummary(this.buildQueryParams()).subscribe({
      next: (res: HttpResponse<ReglementDiffereSummary>) => {
        this.summary = res.body;
      },
      error: () => {
        this.summary = null;
      },
    });
  }
}
