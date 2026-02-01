import { Component, ChangeDetectionStrategy, input, output, contentChild, TemplateRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';

/**
 * Reusable data table component wrapping PrimeNG table
 * Supports lazy loading, pagination, and custom templates
 * 
 * @example
 * <app-data-table
 *   [data]="products()"
 *   [loading]="isLoading()"
 *   [totalRecords]="total()"
 *   (lazyLoad)="onLazyLoad($event)"
 * >
 *   <ng-template #header>
 *     <tr>
 *       <th>Name</th>
 *       <th>Price</th>
 *     </tr>
 *   </ng-template>
 *   <ng-template #body let-product>
 *     <tr>
 *       <td>{{ product.name }}</td>
 *       <td>{{ product.price }}</td>
 *     </tr>
 *   </ng-template>
 * </app-data-table>
 */
@Component({
  selector: 'app-data-table',
  imports: [CommonModule, TableModule],
  template: `
    <p-table
      [value]="data()"
      [loading]="loading()"
      [lazy]="lazy()"
      [paginator]="paginator()"
      [rows]="rows()"
      [totalRecords]="totalRecords()"
      [rowsPerPageOptions]="rowsPerPageOptions()"
      [showCurrentPageReport]="showCurrentPageReport()"
      [currentPageReportTemplate]="currentPageReportTemplate()"
      [globalFilterFields]="globalFilterFields()"
      [filterDelay]="filterDelay()"
      [scrollable]="scrollable()"
      [scrollHeight]="scrollHeight()"
      [virtualScroll]="virtualScroll()"
      [virtualScrollItemSize]="virtualScrollItemSize()"
      [class]="customClass()"
      (onLazyLoad)="lazyLoad.emit($event)"
      (onRowSelect)="rowSelect.emit($event)"
      (onRowUnselect)="rowUnselect.emit($event)"
    >
      <ng-content />
    </p-table>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DataTableComponent<T = any> {
  /** Array of data to display */
  data = input.required<T[]>();
  
  /** Show loading spinner */
  loading = input<boolean>(false);
  
  /** Enable lazy loading */
  lazy = input<boolean>(false);
  
  /** Show pagination */
  paginator = input<boolean>(true);
  
  /** Number of rows per page */
  rows = input<number>(10);
  
  /** Total number of records (for lazy loading) */
  totalRecords = input<number>(0);
  
  /** Options for rows per page dropdown */
  rowsPerPageOptions = input<number[]>([10, 25, 50]);
  
  /** Show current page report */
  showCurrentPageReport = input<boolean>(true);
  
  /** Template for current page report */
  currentPageReportTemplate = input<string>('Affichage de {first} à {last} sur {totalRecords} résultats');
  
  /** Fields for global filter */
  globalFilterFields = input<string[]>([]);
  
  /** Delay for filter in ms */
  filterDelay = input<number>(300);
  
  /** Enable scrolling */
  scrollable = input<boolean>(false);
  
  /** Scroll height */
  scrollHeight = input<string>('400px');
  
  /** Enable virtual scroll */
  virtualScroll = input<boolean>(false);
  
  /** Item size for virtual scroll */
  virtualScrollItemSize = input<number>(46);
  
  /** Custom CSS class */
  customClass = input<string>('');
  
  /** Lazy load event */
  lazyLoad = output<TableLazyLoadEvent>();
  
  /** Row select event */
  rowSelect = output<any>();
  
  /** Row unselect event */
  rowUnselect = output<any>();
}
