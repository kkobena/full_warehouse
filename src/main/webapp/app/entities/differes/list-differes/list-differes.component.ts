import { Component, inject, OnInit } from '@angular/core';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { FloatLabel } from 'primeng/floatlabel';
import { FormsModule } from '@angular/forms';
import { Toolbar } from 'primeng/toolbar';
import { PrimeNG } from 'primeng/config';
import { CommonModule } from '@angular/common';
import { DiffereService } from '../differe.service';
import { SelectModule } from 'primeng/select';
import { ClientDiffere } from '../model/client-differe.model';
import { StatutDiffere } from '../model/statut-differe';
import { Differe } from '../model/differe.model';
import { TableModule } from 'primeng/table';
import { Tooltip } from 'primeng/tooltip';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { LazyLoadEvent } from 'primeng/api';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { RouterModule } from '@angular/router';
import { CardModule } from 'primeng/card';
import { Tag } from 'primeng/tag';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';

@Component({
  selector: 'jhi-list-differes',
  imports: [
    Button,
    DatePicker,
    FloatLabel,
    FormsModule,
    Toolbar,
    CommonModule,
    SelectModule,
    CardModule,
    TableModule,
    Tooltip,
    RouterModule,
    Tag,
  ],
  templateUrl: './list-differes.component.html',
})
export class ListDifferesComponent implements OnInit {
  primngtranslate: Subscription;
  protected page = 0;
  protected totalItems = 0;
  protected loading!: boolean;
  protected today = new Date();
  protected modelStartDate: Date = new Date();
  protected modelEndDate: Date = new Date();
  protected loadingBtn = false;
  protected loadingPdf = false;
  protected clients: ClientDiffere[] = [];
  protected typesDifferes = [
    {
      id: StatutDiffere.PAYE,
      label: 'Soldé',
    },
    {
      id: StatutDiffere.IMPAYE,
      label: 'En cours',
    },
  ];
  protected customerId: number = null;
  protected statut: StatutDiffere = StatutDiffere.IMPAYE;
  protected data: Differe[] = [];
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected readonly primeNGConfig = inject(PrimeNG);
  private readonly differeService = inject(DiffereService);
  private translate = inject(TranslateService);

  constructor() {
    this.translate.use('fr');
    this.primngtranslate = this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
  }

  ngOnInit(): void {
    this.fetchClients();
    const params = this.differeService.differeParams();
    if (params) {
      this.customerId = params.customerId;
      this.statut = params.statut;
      this.modelStartDate = params.fromDate;
      this.modelEndDate = params.toDate;
    }
    this.loadData();
  }

  onChange(event: any): void {
    this.customerId = event.value;
    this.onSerch();
  }

  onStatutChange(event: any): void {
    this.statut = event.value;
    this.onSerch();
  }

  protected lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.loadingBtn = true;
      this.page = event.first / event.rows;
      this.loading = true;
      this.differeService
        .query({
          page: this.page,
          size: event.rows,
          ...this.buildQueryParams(),
        })
        .subscribe({
          next: (res: HttpResponse<Differe[]>) => this.onSuccess(res.body, res.headers, this.page),
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
      .query({
        page: pageToLoad,
        size: this.itemsPerPage,
        ...this.buildQueryParams(),
      })
      .subscribe({
        next: (res: HttpResponse<Differe[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => {
          this.loading = false;
          this.loadingBtn = false;
        },
      });
  }

  protected onSerch(): void {
    this.loadData();
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
    if (this.modelStartDate) {
      params.fromDate = DATE_FORMAT_ISO_DATE(this.modelStartDate);
    }
    if (this.modelEndDate) {
      params.toDate = DATE_FORMAT_ISO_DATE(this.modelEndDate);
    }
    this.differeService.setParams({
      customerId: this.customerId,
      statut: this.statut,
      fromDate: this.modelStartDate,
      toDate: this.modelEndDate,
    });
    return params;
  }
}
