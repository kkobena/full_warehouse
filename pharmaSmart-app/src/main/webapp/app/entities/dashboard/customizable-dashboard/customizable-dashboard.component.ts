import { AfterViewInit, Component, ElementRef, inject, OnDestroy, OnInit, signal, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpResponse } from '@angular/common/http';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

// PrimeNG
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { SelectModule } from 'primeng/select';
import { ToolbarModule } from 'primeng/toolbar';
import { InputTextModule } from 'primeng/inputtext';
import { Tag } from 'primeng/tag';

// GridStack
import { GridStack, GridStackWidget } from 'gridstack';

// Services and Models
import { DashboardLayoutService } from '../dashboard-layout.service';
import {
  DashboardScope,
  IDashboardLayout,
  IDashboardLayoutParsed,
  IGridStackItem,
  ILayoutConfig,
  IWidgetConfig,
  WidgetType,
} from 'app/shared/model/dashboard-layout.model';

// Modal Components
import { SaveLayoutModalComponent } from './save-layout-modal.component';
import { LoadLayoutModalComponent } from './load-layout-modal.component';
import { AddWidgetModalComponent } from './add-widget-modal.component';

@Component({
  selector: 'jhi-customizable-dashboard',
  imports: [CommonModule, FormsModule, ButtonModule, CardModule, SelectModule, ToolbarModule, InputTextModule, Tag],
  templateUrl: './customizable-dashboard.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './customizable-dashboard.component.scss',
})
export default class CustomizableDashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  private dashboardLayoutService = inject(DashboardLayoutService);
  private modalService = inject(NgbModal);

  @ViewChild('gridContainer', { static: false }) gridContainer!: ElementRef;

  // Signals
  layouts = signal<IDashboardLayout[]>([]);
  currentLayout = signal<IDashboardLayoutParsed | null>(null);
  isLoading = signal<boolean>(false);
  isEditMode = signal<boolean>(false);

  // GridStack
  private grid: GridStack | null = null;

  // Available widget types
  availableWidgets = [
    { label: 'Carte KPI', value: WidgetType.KPI_CARD, icon: 'pi-calculator' },
    { label: 'Graphique Ligne', value: WidgetType.LINE_CHART, icon: 'pi-chart-line' },
    { label: 'Graphique Barres', value: WidgetType.BAR_CHART, icon: 'pi-chart-bar' },
    { label: 'Graphique Circulaire', value: WidgetType.PIE_CHART, icon: 'pi-chart-pie' },
    { label: 'Tableau', value: WidgetType.TABLE, icon: 'pi-table' },
    { label: 'Top Produits', value: WidgetType.TOP_PRODUCTS, icon: 'pi-star-fill' },
    { label: 'Alertes Stock', value: WidgetType.STOCK_ALERTS, icon: 'pi-exclamation-triangle' },
  ];

  ngOnInit(): void {
    this.loadLayouts();
    this.loadDefaultLayout();
  }

  ngAfterViewInit(): void {
    this.initializeGrid();
  }

  ngOnDestroy(): void {
    this.grid?.destroy(false);
  }

  initializeGrid(): void {
    if (!this.gridContainer) return;

    this.grid = GridStack.init(
      {
        cellHeight: 80,
        margin: 10,
        column: 12,
        animate: true,
        float: false,
        acceptWidgets: true,
      },
      this.gridContainer.nativeElement,
    );

    // Load current layout into grid
    if (this.currentLayout()?.config) {
      this.loadLayoutIntoGrid(this.currentLayout().config);
    }

    // Enable/disable drag based on edit mode
    this.updateGridEditMode();
  }

  loadLayoutIntoGrid(config: ILayoutConfig): void {
    if (!this.grid) return;

    // Clear existing widgets
    this.grid.removeAll();

    // Add widgets to grid
    config.items.forEach(item => {
      // Create widget element
      const el = document.createElement('div');
      el.className = 'grid-stack-item';
      el.setAttribute('gs-x', item.x.toString());
      el.setAttribute('gs-y', item.y.toString());
      el.setAttribute('gs-w', item.w.toString());
      el.setAttribute('gs-h', item.h.toString());
      el.setAttribute('gs-id', item.id);

      // Create content wrapper
      const content = document.createElement('div');
      content.className = 'grid-stack-item-content';
      content.innerHTML = this.createWidgetContent(item.widget);

      el.appendChild(content);
      this.grid.addWidget(el);
    });
  }

  createWidgetContent(widgetConfig: IWidgetConfig): string {
    // Return pure HTML without Angular components
    const iconClass = this.getWidgetIcon(widgetConfig.type);
    return `
      <div class="widget-header">
        <span class="widget-title">${widgetConfig.title}</span>
        <button class="widget-remove-btn" title="Supprimer" onclick="this.closest('.grid-stack-item').remove()">
          <i class="pi pi-times"></i>
        </button>
      </div>
      <div class="widget-body">
        <div class="widget-placeholder">
          <i class="pi ${iconClass}"></i>
          <p>${widgetConfig.title}</p>
          <small>${widgetConfig.type}</small>
        </div>
      </div>
    `;
  }

  getWidgetIcon(type: WidgetType): string {
    const widget = this.availableWidgets.find(w => w.value === type);
    return widget?.icon || 'pi-question-circle';
  }

  toggleEditMode(): void {
    this.isEditMode.update(v => !v);
    this.updateGridEditMode();
  }

  updateGridEditMode(): void {
    if (!this.grid) return;

    if (this.isEditMode()) {
      this.grid.enable();
    } else {
      this.grid.disable();
    }
  }

  addWidget(): void {
    const modalRef = this.modalService.open(AddWidgetModalComponent, { size: 'lg', centered: true });

    modalRef.result.then(
      (widgetType: WidgetType) => {
        if (widgetType && this.grid) {
          const widgetConfig: IWidgetConfig = {
            type: widgetType,
            title: this.getWidgetLabel(widgetType),
          };

          // Create widget element
          const el = document.createElement('div');
          el.className = 'grid-stack-item';
          el.setAttribute('gs-x', '0');
          el.setAttribute('gs-y', '0');
          el.setAttribute('gs-w', '4');
          el.setAttribute('gs-h', '3');
          el.setAttribute('gs-id', `widget-${Date.now()}`);

          // Create content wrapper
          const content = document.createElement('div');
          content.className = 'grid-stack-item-content';
          content.innerHTML = this.createWidgetContent(widgetConfig);

          el.appendChild(content);
          this.grid.addWidget(el);
        }
      },
      () => {
        // Modal dismissed
      },
    );
  }

  getWidgetLabel(type: WidgetType): string {
    return this.availableWidgets.find(w => w.value === type)?.label || type;
  }

  saveLayout(): void {
    const modalRef = this.modalService.open(SaveLayoutModalComponent, { size: 'lg', centered: true });

    modalRef.result.then(
      (result: { name: string; description: string; scope: DashboardScope }) => {
        if (!this.grid) return;

        const items: IGridStackItem[] = [];

        // Get all widgets from grid
        this.grid.getGridItems().forEach((el, index) => {
          const node = el.gridstackNode;
          if (node) {
            items.push({
              x: node.x || 0,
              y: node.y || 0,
              w: node.w || 4,
              h: node.h || 3,
              id: node.id || `widget-${index}`,
              widget: {
                type: WidgetType.KPI_CARD,
                title: 'Widget',
              },
            });
          }
        });

        const config: ILayoutConfig = {
          items,
          gridOptions: {
            column: 12,
            cellHeight: 80,
            margin: 10,
          },
        };

        const layout: IDashboardLayoutParsed = {
          name: result.name,
          description: result.description,
          scope: result.scope,
          isDefault: false,
          config,
        };

        const layoutToSave = this.dashboardLayoutService.stringifyLayout(layout);

        this.dashboardLayoutService.create(layoutToSave).subscribe({
          next: (res: HttpResponse<IDashboardLayout>) => {
            this.loadLayouts();
          },
          error() {
            alert('Erreur lors de la sauvegarde du layout');
          },
        });
      },
      () => {
        // Modal dismissed
      },
    );
  }

  loadLayouts(): void {
    this.isLoading.set(true);
    this.dashboardLayoutService.query().subscribe({
      next: (res: HttpResponse<IDashboardLayout[]>) => {
        this.layouts.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.layouts.set([]);
        this.isLoading.set(false);
      },
    });
  }

  loadDefaultLayout(): void {
    this.dashboardLayoutService.getDefault().subscribe({
      next: (res: HttpResponse<IDashboardLayout>) => {
        if (res.body) {
          this.currentLayout.set(this.dashboardLayoutService.parseLayout(res.body));
        }
      },
      error() {
        // No default layout
      },
    });
  }

  openLoadLayoutModal(): void {
    const modalRef = this.modalService.open(LoadLayoutModalComponent, { size: 'lg', centered: true });
    const componentInstance = modalRef.componentInstance as LoadLayoutModalComponent;

    componentInstance.layouts = this.layouts;
    componentInstance.isLoading = this.isLoading;

    modalRef.result.then(
      (result: IDashboardLayout | { action: string; id: number }) => {
        if ('action' in result && result.action === 'delete') {
          this.deleteLayout(result.id);
        } else {
          const layout = result as IDashboardLayout;
          const parsed = this.dashboardLayoutService.parseLayout(layout);
          this.currentLayout.set(parsed);

          if (parsed.config) {
            this.loadLayoutIntoGrid(parsed.config);
          }
        }
      },
      () => {
        // Modal dismissed
      },
    );
  }

  deleteLayout(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce layout ?')) {
      this.dashboardLayoutService.delete(id).subscribe({
        next: () => {
          this.loadLayouts();
        },
        error() {
          alert('Erreur lors de la suppression');
        },
      });
    }
  }
}
