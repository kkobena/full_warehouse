import {signal, Component, inject, input, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';
import { BadgeComponent, DataTableComponent } from 'app/shared/ui';
import { IRepartitionStockProduit } from '../../../../../../entities/repartition-stock/repartition-stock.model';
import { RepartitionStockService } from '../../../../../../entities/repartition-stock/repartition-stock.service';
import { NGB_DATE_TO_ISO } from 'app/shared/util/warehouse-util';

@Component({
  selector: 'app-repartition-list',
  templateUrl: './repartition-list.component.html',
  styleUrls: ['./repartition-list.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, DataTableComponent, BadgeComponent, FormsModule],
})
export class AppRepartitionListComponent implements OnInit {
  readonly searchTerm = input('');
  readonly fromDate = input<NgbDateStruct | null>(null);
  readonly toDate = input<NgbDateStruct | null>(null);
  readonly typeRepartition = input<string | null>(null);
  readonly userId = input<number | null>(null);
  readonly storageId = input<number | null>(null);

  protected repartitionService = inject(RepartitionStockService);
  protected readonly rowData = signal([]);
  protected readonly loading = signal(false);
  protected readonly totalItems = signal(0);
  protected readonly itemsPerPage = signal(20);
  protected readonly page = signal(0);

  ngOnInit(): void {
    this.loadAll();
  }

  onSearch(): void {
    this.page.set(0);
    this.loadAll();
  }

  protected loadAll(): void {
    this.loading.set(true);
    const type = this.typeRepartition();
    this.repartitionService
      .query({
        page: this.page(),
        size: this.itemsPerPage(),
        searchTerm: this.searchTerm(),
        dateDebut: NGB_DATE_TO_ISO(this.fromDate()),
        dateFin: NGB_DATE_TO_ISO(this.toDate()),
        typeRepartition: type && type !== 'TOUT' ? type : undefined,
        userId: this.userId() ?? undefined,
        storageId: this.storageId() ?? undefined,
      })
      .subscribe({
        next: (res: HttpResponse<IRepartitionStockProduit[]>) => {
          this.onSuccess(res.body, res.headers);
        },
        error: () => {
          this.loading.set(false);
        },
      });
  }

  protected onPageChange(event: { first: number; rows: number }): void {
    this.itemsPerPage.set(event.rows);
    this.page.set(Math.floor(event.first / event.rows));
    this.loadAll();
  }

  private onSuccess(data: IRepartitionStockProduit[] | null, headers: any): void {
    this.totalItems.set(Number(headers.get('X-Total-Count')));
    this.rowData.set(data ?? []);
    this.loading.set(false);
  }
}
