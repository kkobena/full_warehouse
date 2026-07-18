import { Component, inject, input, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { FormsModule } from '@angular/forms';
import { IRepartitionStockProduit } from '../../../../../../entities/repartition-stock/repartition-stock.model';
import { RepartitionStockService } from '../../../../../../entities/repartition-stock/repartition-stock.service';
import { DATE_FORMAT_ISO_DATE } from 'app/shared/util/warehouse-util';

@Component({
  selector: 'app-repartition-list',
  templateUrl: './repartition-list.component.html',
  styleUrls: ['./repartition-list.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, TableModule, ButtonModule, TagModule, FormsModule],
})
export class AppRepartitionListComponent implements OnInit {
  readonly searchTerm = input('');
  readonly fromDate = input<Date | null>(null);
  readonly toDate = input<Date | null>(null);
  readonly typeRepartition = input<string | null>(null);
  readonly userId = input<number | null>(null);
  readonly storageId = input<number | null>(null);

  protected repartitionService = inject(RepartitionStockService);
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
    const type = this.typeRepartition();
    this.repartitionService
      .query({
        page: this.page,
        size: this.itemsPerPage,
        searchTerm: this.searchTerm(),
        dateDebut: DATE_FORMAT_ISO_DATE(this.fromDate()),
        dateFin: DATE_FORMAT_ISO_DATE(this.toDate()),
        typeRepartition: type && type !== 'TOUT' ? type : undefined,
        userId: this.userId() ?? undefined,
        storageId: this.storageId() ?? undefined,
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
