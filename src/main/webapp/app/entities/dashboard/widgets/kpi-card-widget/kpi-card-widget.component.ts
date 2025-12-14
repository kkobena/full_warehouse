import { Component, Input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'jhi-kpi-card-widget',

  imports: [CommonModule],
  template: `
    <div class="kpi-card">
      <div class="kpi-label">{{ label() }}</div>
      <div class="kpi-value">{{ formatValue(value()) }}</div>
      @if (trend()) {
        <div class="kpi-trend" [class.positive]="trend()! > 0" [class.negative]="trend()! < 0">
          <i class="pi" [ngClass]="trend()! > 0 ? 'pi-arrow-up' : 'pi-arrow-down'"></i>
          <span>{{ Math.abs(trend()!) }}%</span>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .kpi-card {
        height: 100%;
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        text-align: center;
      }

      .kpi-label {
        font-size: 0.875rem;
        color: #6c757d;
        margin-bottom: 0.5rem;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }

      .kpi-value {
        font-size: 2rem;
        font-weight: 700;
        color: #212529;
        margin-bottom: 0.5rem;
      }

      .kpi-trend {
        font-size: 0.875rem;
        display: flex;
        align-items: center;
        gap: 0.25rem;
      }

      .kpi-trend.positive {
        color: #28a745;
      }

      .kpi-trend.negative {
        color: #dc3545;
      }
    `,
  ],
})
export class KpiCardWidgetComponent implements OnInit {
  @Input() config: any;

  label = signal<string>('KPI');
  value = signal<number>(0);
  trend = signal<number | null>(null);
  Math = Math;

  ngOnInit(): void {
    if (this.config) {
      this.label.set(this.config.label || 'KPI');
      this.value.set(this.config.value || 0);
      this.trend.set(this.config.trend || null);
    }

    // Demo data
    if (!this.config) {
      this.label.set('CA du Jour');
      this.value.set(1250000);
      this.trend.set(12.5);
    }
  }

  formatValue(value: number): string {
    return new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  }
}
