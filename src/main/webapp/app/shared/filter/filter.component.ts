import { Component, Input } from '@angular/core';
import { IFilterOptions } from './filter.model';
import { WarehouseCommonModule } from '../warehouse-common/warehouse-common.module';

@Component({
  standalone: true,
  selector: 'jhi-filter',
  templateUrl: './filter.component.html',
  imports: [WarehouseCommonModule],
})
export class FilterComponent {
  @Input() filters!: IFilterOptions;

  clearAllFilters(): void {
    this.filters.clear();
  }

  clearFilter(filterName: string, value: string): void {
    this.filters.removeFilter(filterName, value);
  }
}
