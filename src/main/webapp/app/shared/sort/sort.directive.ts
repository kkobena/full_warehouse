import { Directive, input, output } from '@angular/core';
import { SortOrder, SortState, SortStateSignal } from './sort-state';

export interface SortChangeDirective<T> {
  sortChange: any;

  sort(field: T): void;
}

@Directive({
  standalone: true,
  selector: '[jhiSort]',
})
export class SortDirective implements SortChangeDirective<string> {
  readonly sortState = input.required<SortStateSignal>();

  readonly sortChange = output<SortState>();

  sort(field: string): void {
    const { predicate, order } = this.sortState()();
    const toggle = (): SortOrder => (order === 'asc' ? 'desc' : 'asc');
    this.sortChange.emit({ predicate: field, order: field !== predicate ? 'asc' : toggle() });
  }
}
