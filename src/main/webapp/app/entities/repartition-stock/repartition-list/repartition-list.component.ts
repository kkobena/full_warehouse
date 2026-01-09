import { Component, inject, input, Input, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { IRepartitionStockProduit } from '../repartition-stock.model';
import { RepartitionStockService } from '../repartition-stock.service';
import { FormsModule } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';

@Component({
  selector: 'jhi-repartition-list',
  templateUrl: './repartition-list.component.html',
  styleUrls: ['./repartition-list.component.scss'],
  imports: [CommonModule, TableModule, ButtonModule, TagModule, FormsModule],
})
export class RepartitionListComponent implements OnInit {
  readonly searchTerm = input('', { alias: 'searchTerm' });
  readonly fromDate = input<Date | undefined>(undefined, { alias: 'fromDate' });
  readonly toDate = input<Date | undefined>(undefined, { alias: 'toDate' });

  protected repartitionService = inject(RepartitionStockService);
  protected modalService = inject(NgbModal);
  protected rowData: IRepartitionStockProduit[] = [];
  protected loading = false;
  protected totalItems = 0;
  protected itemsPerPage = 20;
  protected page = 0;

  ngOnInit(): void {
    this.loadAll();
  }

  onSearch(): void {
    this.page = 0;
    this.loadAll();
  }

  protected loadAll(): void {
    this.loading = true;
    this.repartitionService
      .query({
        page: this.page,
        size: this.itemsPerPage,
        searchTerm: this.searchTerm(),
        dateDebut: DATE_FORMAT_ISO_DATE(this.fromDate()),
        dateFin: DATE_FORMAT_ISO_DATE(this.toDate()),
      })
      .subscribe({
        next: (res: HttpResponse<IRepartitionStockProduit[]>) => {
          this.onSuccess(res.body, res.headers);
        },
        error: () => {
          this.loading = false;
        },
      });
  }

  protected onPageChange(event: any): void {
    this.page = event.page;
    this.itemsPerPage = event.rows;
    this.loadAll();
  }

  private onSuccess(data: IRepartitionStockProduit[] | null, headers: any): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.rowData = data ?? [];
    this.loading = false;
  }
}
