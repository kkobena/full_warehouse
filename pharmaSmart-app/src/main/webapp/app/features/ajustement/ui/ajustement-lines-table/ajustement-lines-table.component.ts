import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { IAjustement } from '../../../../shared/model/ajustement.model';
import {
  BadgeComponent,
  ButtonComponent,
  DataTableComponent,
  EditableCellComponent,
  HeaderCheckboxComponent,
  IconFieldComponent,
  InputNumberComponent,
  RowCheckboxComponent,
} from '../../../../shared/ui';

@Component({
  selector: 'app-ajustement-lines-table',
  templateUrl: './ajustement-lines-table.component.html',
  styleUrl: './ajustement-lines-table.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    DataTableComponent,
    BadgeComponent,
    NgbTooltip,
    IconFieldComponent,
    InputNumberComponent,
    EditableCellComponent,
    HeaderCheckboxComponent,
    RowCheckboxComponent,
  ],
})
export class AjustementLinesTableComponent {
  readonly lines    = input.required<IAjustement[]>();
  readonly loading  = input(false);

  readonly deleteLine      = output<IAjustement>();
  readonly deleteSelection = output<IAjustement[]>();
  readonly updateQty       = output<{ line: IAjustement; absQty: number }>();
  readonly searchChange    = output<string>();

  protected search = '';
  protected selectedRows: IAjustement[] = [];

  protected directionLabel(qtyMvt: number): string {
    return (qtyMvt ?? 0) >= 0 ? 'ENTRÉE' : 'SORTIE';
  }

  protected directionSeverity(qtyMvt: number): 'success' | 'danger' {
    return (qtyMvt ?? 0) >= 0 ? 'success' : 'danger';
  }

  protected directionIcon(qtyMvt: number): string {
    return (qtyMvt ?? 0) >= 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down';
  }

  protected absQty(qtyMvt: number): number {
    return Math.abs(qtyMvt ?? 0);
  }

  protected onQtyChange(line: IAjustement, qty: number): void {
    if (qty !== 0) {
      this.updateQty.emit({ line, absQty: qty });
    }
  }

  protected onSearch(): void {
    this.searchChange.emit(this.search);
  }

  protected onDeleteSelection(): void {
    this.deleteSelection.emit(this.selectedRows);
    this.selectedRows = [];
  }
}
