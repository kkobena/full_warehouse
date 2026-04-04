import { Component, DestroyRef, effect, inject, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs/operators';
import { HttpHeaders } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ErrorService } from '../../../../shared/error.service';
import { ITEMS_PER_PAGE } from '../../../../shared/constants/pagination.constants';

import { DiffereApiService } from '../../data-access/services/differe-api.service';
import { DiffereStore } from '../../data-access/store/differe.store';
import { IDiffere, IDiffereSearchParams } from '../../data-access/models';

@Component({
  selector: 'app-differe-list',
  imports: [
    CommonModule,
    WarehouseCommonModule,
    TableModule,
    TagModule,
  ],
  templateUrl: './differe-list.component.html',
  styleUrl: './differe-list.component.scss',
})
export class DiffereListComponent {
  readonly searchParams = input<IDiffereSearchParams | null>(null);

  protected loading = false;
  protected page = 0;
  protected readonly itemsPerPage = ITEMS_PER_PAGE;

  protected readonly store = inject(DiffereStore);
  private readonly differeApiService = inject(DiffereApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    effect(() => {
      const params = this.searchParams();
      if (params !== null) {
        this.page = 0;
        this.loadPage(params);
        this.loadSummary(params);
      }
    });
  }

  onRowSelect(differe: IDiffere): void {
    this.store.selectDiffere(differe);
  }

  lazyLoading(event: TableLazyLoadEvent): void {
    const params = this.searchParams();
    if (event && params) {
      this.page = Math.floor((event.first ?? 0) / (event.rows ?? this.itemsPerPage));
      this.loadPage(params, event.rows ?? this.itemsPerPage);
    }
  }

  private loadPage(params: IDiffereSearchParams, rows = this.itemsPerPage): void {
    this.loading = true;
    this.store.setLoading(true);
    this.differeApiService
      .query({ ...params, page: this.page, size: rows })
      .pipe(
        finalize(() => (this.loading = false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: res => this.onSuccess(res.body, res.headers),
        error: err => {
          this.store.setLoading(false);
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Chargement différés');
        },
      });
  }

  private onSuccess(data: IDiffere[] | null, headers: HttpHeaders): void {
    this.store.setDifferes(data ?? [], Number(headers.get('X-Total-Count')));
  }

  private loadSummary(params: IDiffereSearchParams): void {
    this.differeApiService
      .getDiffereSummary(params)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.store.setSummary(res.body),
        error: () => this.store.setSummary(null),
      });
  }
}
