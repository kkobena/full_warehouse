import { Component, Input, OnInit, ViewChild, ElementRef, AfterViewInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, ChartData, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'jhi-pie-chart-widget',

  imports: [CommonModule],
  template: `
    <div class="pie-chart-widget">
      <canvas #chartCanvas></canvas>
    </div>
  `,
  styles: [
    `
      .pie-chart-widget {
        height: 100%;
        position: relative;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      canvas {
        max-height: 100%;
        max-width: 100%;
      }
    `,
  ],
})
export class PieChartWidgetComponent implements OnInit, AfterViewInit {
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
    const labels = ['Espèces', 'Carte Bancaire', 'OM', 'Chèque', 'Virement'];
    const data = [35, 28, 20, 12, 5];

    const chartData: ChartData<'pie'> = {
      labels,
      datasets: [
        {
          data,
          backgroundColor: ['#2196F3', '#4CAF50', '#FF9800', '#F44336', '#9C27B0'],
          borderColor: '#ffffff',
          borderWidth: 2,
        },
      ],
    };

    const config: ChartConfiguration<'pie'> = {
      type: 'pie',
      data: chartData,
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              boxWidth: 12,
              font: {
                size: 11,
              },
            },
          },
          tooltip: {
            callbacks: {
              label: context => {
                return `${context.label}: ${context.parsed}%`;
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
