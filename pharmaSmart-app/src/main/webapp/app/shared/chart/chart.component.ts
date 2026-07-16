import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  input,
  NgZone,
  OnDestroy,
  output,
  untracked,
  viewChild,
} from '@angular/core';
import { ActiveElement, Chart, ChartType, Plugin, registerables } from 'chart.js';

Chart.register(...registerables);

export interface ChartSelectEvent {
  originalEvent: MouseEvent;
  element: ActiveElement;
  dataset: ActiveElement[];
}

/**
 * Wrapper Chart.js maison remplaçant `p-chart` (déprécié en PrimeNG v22, supprimé en v24
 * au profit de l'offre payante PrimeUI PRO Charts).
 *
 * API compatible avec l'ancien composant PrimeNG — migration :
 * ```html
 * <p-chart type="bar" [data]="config.data" [options]="config.options" height="300" />
 * devient
 * <app-chart type="bar" [data]="config.data" [options]="config.options" height="300" />
 * ```
 * Les hauteurs/largeurs numériques sans unité (`height="300"`) sont interprétées en pixels.
 */
@Component({
  selector: 'app-chart',
  template: `
    <div class="app-chart" [style.width]="containerWidth()" [style.height]="containerHeight()">
      <canvas #canvas role="img" [attr.aria-label]="ariaLabel()" (click)="onCanvasClick($event)"></canvas>
    </div>
  `,
  styles: `
    .app-chart {
      position: relative;
      width: 100%;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChartComponent implements AfterViewInit, OnDestroy {
  readonly type = input.required<ChartType>();
  readonly data = input<any>();
  readonly options = input<any>();
  readonly plugins = input<Plugin[]>([]);
  readonly width = input<string>();
  readonly height = input<string>();
  readonly responsive = input(true);
  readonly ariaLabel = input<string>();
  readonly dataSelect = output<ChartSelectEvent>();

  private readonly canvasRef = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private readonly zone = inject(NgZone);
  private chart: Chart | null = null;
  private currentType: ChartType | null = null;
  private initialized = false;

  protected readonly containerWidth = computed(() => this.normalizeSize(this.width()));
  protected readonly containerHeight = computed(() => this.normalizeSize(this.height()));

  constructor() {
    effect(() => {
      const type = this.type();
      const data = this.data();
      const options = this.options();
      const plugins = this.plugins();
      if (this.initialized) {
        untracked(() => this.render(type, data, options, plugins));
      }
    });
  }

  ngAfterViewInit(): void {
    this.initialized = true;
    this.render(this.type(), this.data(), this.options(), this.plugins());
  }

  ngOnDestroy(): void {
    this.destroyChart();
  }

  /** Instance Chart.js sous-jacente (équivalent de `UIChart.chart`). */
  get chartInstance(): Chart | null {
    return this.chart;
  }

  /** Redessine le graphique avec les données courantes. */
  refresh(): void {
    this.zone.runOutsideAngular(() => this.chart?.update());
  }

  /** Détruit puis recrée entièrement le graphique. */
  reinit(): void {
    this.destroyChart();
    this.render(this.type(), this.data(), this.options(), this.plugins());
  }

  getBase64Image(): string | undefined {
    return this.chart?.toBase64Image();
  }

  protected onCanvasClick(event: MouseEvent): void {
    if (!this.chart) {
      return;
    }
    const elements = this.chart.getElementsAtEventForMode(event, 'nearest', { intersect: true }, false);
    if (elements.length) {
      const dataset = this.chart.getElementsAtEventForMode(event, 'dataset', { intersect: true }, false);
      this.dataSelect.emit({ originalEvent: event, element: elements[0], dataset });
    }
  }

  private render(type: ChartType, data: any, options: any, plugins: Plugin[]): void {
    if (!data) {
      this.destroyChart();
      return;
    }
    this.zone.runOutsideAngular(() => {
      if (this.chart && this.currentType === type) {
        this.chart.data = data;
        this.chart.options = options ?? {};
        this.chart.update();
        return;
      }
      this.chart?.destroy();
      this.chart = new Chart(this.canvasRef().nativeElement, {
        type,
        data,
        options: options ?? {},
        plugins,
      });
      this.currentType = type;
    });
  }

  private destroyChart(): void {
    if (this.chart) {
      this.chart.destroy();
      this.chart = null;
      this.currentType = null;
    }
  }

  /** `"300"` → `"300px"` ; `"400px"` / `"80%"` inchangés ; `undefined` → null. */
  private normalizeSize(value: string | undefined): string | null {
    if (!value) {
      return null;
    }
    return /^\d+(\.\d+)?$/.test(value) ? `${value}px` : value;
  }
}
