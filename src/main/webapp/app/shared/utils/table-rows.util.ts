import { Signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { fromEvent } from 'rxjs';
import { distinctUntilChanged, map, startWith } from 'rxjs/operators';

export interface TableRowsOptions {
  /** Pixels used outside the table scroll body (toolbar, header, paginator, etc.). Default: 260 */
  overhead?: number;
  /** Height of a single data row in pixels. Default: 38 (p-datatable-sm row height) */
  rowHeight?: number;
  /** Minimum number of rows to show. Default: 10 */
  min?: number;
  /** Maximum number of rows to show. Default: 50 */
  max?: number;
}

function compute(overhead: number, rowHeight: number, min: number, max: number): number {
  const available = Math.max(0, window.innerHeight - overhead);
  const raw = Math.floor(available / rowHeight);
  // Snap to nearest multiple of 5 for cleaner page-size options
  const snapped = Math.max(min, Math.floor(raw / 5) * 5);
  return Math.min(max, snapped);
}

/**
 * Injection-context function that returns a reactive `Signal<number>` representing
 * how many rows a p-table can display without scrolling, based on the current viewport height.
 * Re-computes automatically on window resize.
 *
 * Must be called inside an injection context (constructor, field initializer, or inject()).
 */
export function injectTableRows(options?: TableRowsOptions): Signal<number> {
  const overhead = options?.overhead ?? 260;
  const rowHeight = options?.rowHeight ?? 38;
  const min = options?.min ?? 10;
  const max = options?.max ?? 50;

  return toSignal(
    fromEvent(window, 'resize').pipe(
      startWith(null),
      map(() => compute(overhead, rowHeight, min, max)),
      distinctUntilChanged()
    ),
    { initialValue: compute(overhead, rowHeight, min, max) }
  );
}
