import { Component, effect, ElementRef, input, OnDestroy, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { IVenteMois } from '../../models/vente-mois.model';

Chart.register(...registerables);

@Component({
  selector: 'app-produit-historique-tab',
  templateUrl: './produit-historique-tab.component.html',
  styleUrls: ['./produit-historique-tab.scss'],
  imports: [CommonModule],
})
export class ProduitHistoriqueTabComponent implements OnDestroy {
  readonly ventes = input<IVenteMois[]>([]);
  readonly loading = input<boolean>(false);

  private readonly canvasRef = viewChild<ElementRef<HTMLCanvasElement>>('ventesChart');
  private chart?: Chart;

  constructor() {
    effect(() => {
      const data = this.ventes();
      if (data.length > 0) {
        this.buildChart(data);
      }
    });
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  private buildChart(ventes: IVenteMois[]): void {
    const canvas = this.canvasRef()?.nativeElement;
    if (!canvas) return;

    this.chart?.destroy();

    const labels = ventes.map(v => {
      const [year, month] = v.anneeMois.split('-');
      return new Date(+year, +month - 1).toLocaleDateString('fr-FR', { month: 'short', year: '2-digit' });
    });

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels,
        datasets: [
          {
            label: 'Quantité vendue',
            data: ventes.map(v => v.quantiteVendue),
            backgroundColor: 'rgba(99, 102, 241, 0.7)',
            borderColor: 'rgba(99, 102, 241, 1)',
            borderWidth: 1,
            borderRadius: 4,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              afterLabel: (ctx) => {
                const vente = ventes[ctx.dataIndex];
                const ca = (vente.montantCa / 100).toLocaleString('fr-FR', { minimumFractionDigits: 0 });
                return `CA : ${ca} XOF  |  ${vente.nombreVentes} vente(s)`;
              },
            },
          },
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: { precision: 0 },
            title: { display: true, text: 'Quantité' },
          },
        },
      },
    };

    this.chart = new Chart(canvas, config);
  }
}
