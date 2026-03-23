import { Component, input, output } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { IAjustement } from '../../../../shared/model/ajustement.model';

@Component({
  selector: 'app-ajustement-lines-table',
  templateUrl: './ajustement-lines-table.component.html',
  styleUrl: './ajustement-lines-table.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    DecimalPipe,
    ButtonModule,
    TableModule,
    TagModule,
    TooltipModule,
    InputTextModule,
    InputNumber,
    IconField,
    InputIcon,
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
