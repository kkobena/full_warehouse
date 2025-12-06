import { Component, Input, OnInit, ViewChild, ElementRef, AfterViewInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, ChartData, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'jhi-bar-chart-widget',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="bar-chart-widget">
      <canvas #chartCanvas></canvas>
    </div>
  `,
  styles: [
    `
      .bar-chart-widget {
        height: 100%;
        position: relative;
      }

      canvas {
        max-height: 100%;
      }
    `,
  ],
})
export class BarChartWidgetComponent implements OnInit, AfterViewInit {
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
    const labels = ['Prod A', 'Prod B', 'Prod C', 'Prod D', 'Prod E'];
    const data = [42000, 38000, 35000, 28000, 25000];

    const chartData: ChartData<'bar'> = {
      labels,
      datasets: [
        {
          label: 'Ventes',
          data,
          backgroundColor: ['#2196F3', '#4CAF50', '#FF9800', '#F44336', '#9C27B0'],
          borderColor: ['#1976D2', '#388E3C', '#F57C00', '#D32F2F', '#7B1FA2'],
          borderWidth: 1,
        },
      ],
    };

    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
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
