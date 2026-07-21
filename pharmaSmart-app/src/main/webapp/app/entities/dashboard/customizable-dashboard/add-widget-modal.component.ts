import { Component, inject, Input, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { ButtonComponent } from 'app/shared/ui';

import { WidgetType } from 'app/shared/model/dashboard-layout.model';

interface WidgetOption {
  label: string;
  value: WidgetType;
  icon: string;
  description: string;
}

@Component({
  selector: 'jhi-add-widget-modal',
  imports: [CommonModule, ButtonComponent],
  templateUrl: './add-widget-modal.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './add-widget-modal.component.scss',
})
export class AddWidgetModalComponent {
  activeModal = inject(NgbActiveModal);

  selectedWidgetType: WidgetType | null = null;

  availableWidgets: WidgetOption[] = [
    {
      label: 'Carte KPI',
      value: WidgetType.KPI_CARD,
      icon: 'pi-calculator',
      description: 'Affiche une métrique clé avec icône',
    },
    {
      label: 'Graphique Ligne',
      value: WidgetType.LINE_CHART,
      icon: 'pi-chart-line',
      description: 'Évolution dans le temps',
    },
    {
      label: 'Graphique Barres',
      value: WidgetType.BAR_CHART,
      icon: 'pi-chart-bar',
      description: 'Comparaison de valeurs',
    },
    {
      label: 'Graphique Circulaire',
      value: WidgetType.PIE_CHART,
      icon: 'pi-chart-pie',
      description: 'Distribution en pourcentage',
    },
    {
      label: 'Tableau',
      value: WidgetType.TABLE,
      icon: 'pi-table',
      description: 'Données tabulaires',
    },
    {
      label: 'Top Produits',
      value: WidgetType.TOP_PRODUCTS,
      icon: 'pi-star-fill',
      description: 'Les produits les plus vendus',
    },
    {
      label: 'Alertes Stock',
      value: WidgetType.STOCK_ALERTS,
      icon: 'pi-exclamation-triangle',
      description: 'Alertes de stock bas',
    },
  ];

  dismiss(): void {
    this.activeModal.dismiss();
  }

  selectWidget(type: WidgetType): void {
    this.selectedWidgetType = type;
  }

  addWidget(): void {
    if (!this.selectedWidgetType) {
      return;
    }

    this.activeModal.close(this.selectedWidgetType);
  }
}
