import { Component, Input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

interface TableRow {
  [key: string]: any;
}

@Component({
  selector: 'jhi-table-widget',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="table-widget">
      @if (rows().length === 0) {
        <div class="text-center p-4 text-500">
          <i class="pi pi-inbox" style="font-size: 2rem;"></i>
          <p class="mt-2">Aucune donnée</p>
        </div>
      } @else {
        <div class="table-responsive">
          <table class="table-widget-table">
            <thead>
              <tr>
                @for (col of columns(); track col.field) {
                  <th>{{ col.header }}</th>
                }
              </tr>
            </thead>
            <tbody>
              @for (row of rows(); track $index) {
                <tr>
                  @for (col of columns(); track col.field) {
                    <td>{{ formatValue(row[col.field], col.type) }}</td>
                  }
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .table-widget {
        height: 100%;
        overflow: auto;
      }

      .table-responsive {
        overflow-x: auto;
      }

      .table-widget-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 0.875rem;
      }

      .table-widget-table thead tr {
        background-color: #f8f9fa;
        border-bottom: 2px solid #dee2e6;
      }

      .table-widget-table th {
        padding: 0.75rem 0.5rem;
        text-align: left;
        font-weight: 600;
        color: #495057;
      }

      .table-widget-table td {
        padding: 0.5rem;
        border-bottom: 1px solid #dee2e6;
      }

      .table-widget-table tbody tr:hover {
        background-color: #f8f9fa;
      }
    `,
  ],
})
export class TableWidgetComponent implements OnInit {
  @Input() config: any;

  columns = signal<Array<{ field: string; header: string; type?: string }>>([]);
  rows = signal<TableRow[]>([]);

  ngOnInit(): void {
    if (this.config) {
      this.columns.set(this.config.columns || []);
      this.rows.set(this.config.rows || []);
    }

    // Demo data
    if (!this.config || this.rows().length === 0) {
      this.columns.set([
        { field: 'product', header: 'Produit', type: 'string' },
        { field: 'quantity', header: 'Quantité', type: 'number' },
        { field: 'amount', header: 'Montant', type: 'currency' },
      ]);

      this.rows.set([
        { product: 'Doliprane 1000mg', quantity: 125, amount: 45000 },
        { product: 'Paracétamol 500mg', quantity: 98, amount: 32000 },
        { product: 'Ibuprofène 400mg', quantity: 87, amount: 28500 },
        { product: 'Amoxicilline 500mg', quantity: 76, amount: 25000 },
      ]);
    }
  }

  formatValue(value: any, type?: string): string {
    if (value === null || value === undefined) return '';

    switch (type) {
      case 'number':
        return new Intl.NumberFormat('fr-FR').format(value);
      case 'currency':
        return new Intl.NumberFormat('fr-FR', {
          minimumFractionDigits: 0,
          maximumFractionDigits: 0,
        }).format(value);
      case 'date':
        return new Date(value).toLocaleDateString('fr-FR');
      default:
        return String(value);
    }
  }
}
