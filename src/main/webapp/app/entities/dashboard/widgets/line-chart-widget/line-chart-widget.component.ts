import { Component, Input, OnInit, ViewChild, ElementRef, AfterViewInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, ChartData, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'jhi-line-chart-widget',

  imports: [CommonModule],
  template: `
    <div class="line-chart-widget">
      <canvas #chartCanvas></canvas>
    </div>
  `,
  styles: [
    `
      .line-chart-widget {
        height: 100%;
        position: relative;
      }

      canvas {
        max-height: 100%;
      }
    `,
  ],
})
export class LineChartWidgetComponent implements OnInit, AfterViewInit {
  @ViewChild('chartCanvas', { static: false }) chartCanvas!: ElementRef<HTMLCanvasElement>;
  @Input() config: any;

  private chart: Chart | null = null;
  isReady = signal<boolean>(false);

  ngOnInit(): void {
    // Component initialized
  }

  ngAfterViewInit(): void {
    this.createChart();
  }

  createChart(): void {
    if (!this.chartCanvas) return;

    const ctx = this.chartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    // Demo data
    const labels = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun'];
    const data = [65000, 78000, 85000, 92000, 88000, 95000];

    const chartData: ChartData<'line'> = {
      labels,
      datasets: [
        {
          label: 'CA Mensuel',
          data,
          borderColor: '#2196F3',
          backgroundColor: 'rgba(33, 150, 243, 0.1)',
          borderWidth: 2,
          fill: true,
          tension: 0.4,
        },
      ],
    };

    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: chartData,
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: false,
          },
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              callback: value => {
                return new Intl.NumberFormat('fr-FR', {
                  notation: 'compact',
                  compactDisplay: 'short',
                }).format(Number(value));
              },
            },
          },
        },
      },
    };

    this.chart = new Chart(ctx, config);
    this.isReady.set(true);
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }
}
