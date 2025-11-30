import {
  AfterViewInit,
  Component,
  ElementRef,
  inject,
  OnDestroy,
  OnInit,
  signal,
  TemplateRef,
  ViewChild
} from '@angular/core';
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
  WidgetType
} from 'app/shared/model/dashboard-layout.model';

// Widget Components

@Component({
  selector: 'jhi-customizable-dashboard',
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    CardModule,
    SelectModule,
    ToolbarModule,
    InputTextModule,
    Tag

  ],
  templateUrl: './customizable-dashboard.component.html',
  styleUrl: './customizable-dashboard.component.scss'
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

  // Form values
  layoutName = '';
  layoutDescription = '';
  layoutScope: DashboardScope = DashboardScope.PRIVATE;

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
    { label: 'Alertes Stock', value: WidgetType.STOCK_ALERTS, icon: 'pi-exclamation-triangle' }
  ];

  selectedWidgetType: WidgetType | null = null;

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
        acceptWidgets: true
      },
      this.gridContainer.nativeElement
    );

    // Load current layout into grid
    if (this.currentLayout()?.config) {
      this.loadLayoutIntoGrid(this.currentLayout()!.config!);
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
      const widget: GridStackWidget = {
        x: item.x,
        y: item.y,
        w: item.w,
        h: item.h,
        id: item.id,
        content: this.createWidgetContent(item.widget)
      };

      this.grid!.addWidget(widget);
    });
  }

  createWidgetContent(widgetConfig: IWidgetConfig): string {
    // For now, return a simple HTML structure
    // In production, this would be rendered using Angular components
    return `
      <div class="grid-stack-item-content">
        <div class="widget-header">
          <span class="widget-title">${widgetConfig.title}</span>
          <button class="widget-remove-btn" onclick="removeWidget(this)">
            <i class="pi pi-times"></i>
          </button>
        </div>
        <div class="widget-body">
          <jhi-${widgetConfig.type.toLowerCase()}-widget></jhi-${widgetConfig.type.toLowerCase()}-widget>
        </div>
      </div>
    `;
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

  addWidget(content: TemplateRef<any>): void {
    this.selectedWidgetType = null;
    this.modalService.open(content, { size: 'lg', centered: true });
  }

  confirmAddWidget(modal: any): void {
    if (!this.selectedWidgetType || !this.grid) return;

    const widgetConfig: IWidgetConfig = {
      type: this.selectedWidgetType,
      title: this.getWidgetLabel(this.selectedWidgetType)
    };

    const widget: GridStackWidget = {
      x: 0,
      y: 0,
      w: 4,
      h: 3,
      id: `widget-${Date.now()}`,
      content: this.createWidgetContent(widgetConfig)
    };

    this.grid.addWidget(widget);
    modal.close();
    this.selectedWidgetType = null;
  }

  getWidgetLabel(type: WidgetType): string {
    return this.availableWidgets.find(w => w.value === type)?.label || type;
  }

  saveLayout(content: TemplateRef<any>): void {
    this.layoutName = '';
    this.layoutDescription = '';
    this.layoutScope = DashboardScope.PRIVATE;
    this.modalService.open(content, { size: 'lg', centered: true });
  }

  confirmSaveLayout(modal: any): void {
    if (!this.grid || !this.layoutName) return;

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
            title: 'Widget'
          }
        });
      }
    });

    const config: ILayoutConfig = {
      items,
      gridOptions: {
        column: 12,
        cellHeight: 80,
        margin: 10
      }
    };

    const layout: IDashboardLayoutParsed = {
      name: this.layoutName,
      description: this.layoutDescription,
      scope: this.layoutScope,
      isDefault: false,
      config
    };

    const layoutToSave = this.dashboardLayoutService.stringifyLayout(layout);

    this.dashboardLayoutService.create(layoutToSave).subscribe({
      next: (res: HttpResponse<IDashboardLayout>) => {
        modal.close();
        this.layoutName = '';
        this.layoutDescription = '';
        this.loadLayouts();
      },
      error: () => {
        alert('Erreur lors de la sauvegarde du layout');
      }
    });
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
      }
    });
  }

  loadDefaultLayout(): void {
    this.dashboardLayoutService.getDefault().subscribe({
      next: (res: HttpResponse<IDashboardLayout>) => {
        if (res.body) {
          this.currentLayout.set(this.dashboardLayoutService.parseLayout(res.body));
        }
      },
      error: () => {
        // No default layout
      }
    });
  }

  openLoadLayoutModal(content: TemplateRef<any>): void {
    this.modalService.open(content, { size: 'lg', centered: true });
  }

  loadLayout(layout: IDashboardLayout, modal: any): void {
    const parsed = this.dashboardLayoutService.parseLayout(layout);
    this.currentLayout.set(parsed);

    if (parsed.config) {
      this.loadLayoutIntoGrid(parsed.config);
    }

    modal.close();
  }

  deleteLayout(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce layout ?')) {
      this.dashboardLayoutService.delete(id).subscribe({
        next: () => {
          this.loadLayouts();
        },
        error: () => {
          alert('Erreur lors de la suppression');
        }
      });
    }
  }
}
