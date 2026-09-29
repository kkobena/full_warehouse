import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  Directive,
  effect,
  ElementRef,
  forwardRef,
  inject,
  Injector,
  input,
  OnDestroy,
  output,
  viewChild,
} from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { GridItemHTMLElement, GridStack, GridStackNode } from 'gridstack';
import { DashboardItem, GridPosition, WidgetAvailability } from '../../models/dashboard.model';
import { WidgetTileComponent } from '../widget-tile/widget-tile.component';
import { WidgetExpandedModalComponent } from '../widget-expanded-modal/widget-expanded-modal.component';

export const GRID_COLUMNS = 12;

/**
 * Grille du dashboard. Angular crée et détruit les tuiles ({@code @for}) ; GridStack ne fait que
 * les placer. Chaque tuile s'enregistre auprès de la grille par {@link DashboardGridItemDirective},
 * et la grille renvoie les positions qu'elle calcule : le store reste la seule source de vérité.
 */
@Component({
  selector: 'app-dashboard-grid',
  imports: [forwardRef(() => DashboardGridItemDirective), WidgetTileComponent],
  template: `
    <div #gridEl class="grid-stack" [class.is-editing]="editable()">
      @for (item of items(); track item.id) {
        <div class="grid-stack-item" [appDashboardGridItem]="item">
          <div class="grid-stack-item-content">
            <app-widget-tile
              [item]="item"
              [editMode]="editable()"
              [availability]="availability().get(item.id) ?? 'INCONNU'"
              (remove)="removeItem.emit(item.id)"
              (openSettings)="openSettings.emit(item.id)"
              (duplicate)="duplicateItem.emit(item.id)"
              (expand)="expand(item, $event)"
              (paramsChange)="paramsChange.emit({ id: item.id, params: $event })"
            />
          </div>
        </div>
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
    }
    .grid-stack {
      min-height: 120px;
    }
    .grid-stack.is-editing {
      background-image: radial-gradient(var(--bs-border-color) 1px, transparent 1px);
      background-size: calc(100% / 12) 88px;
      border-radius: var(--bs-border-radius-lg);
    }
    .grid-stack-item-content {
      overflow: hidden;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardGridComponent implements AfterViewInit, OnDestroy {
  readonly items = input.required<DashboardItem[]>();
  readonly availability = input.required<Map<string, WidgetAvailability>>();
  readonly editable = input(false);

  /** Positions recalculées par la grille (tassement compris). */
  readonly positionsChange = output<GridPosition[]>();
  /** Déplacement ou redimensionnement à la souris. */
  readonly userChanged = output<void>();
  readonly removeItem = output<string>();
  readonly openSettings = output<string>();
  readonly duplicateItem = output<string>();
  readonly paramsChange = output<{ id: string; params: Record<string, string> }>();

  private readonly modal = inject(NgbModal);
  private readonly injector = inject(Injector);
  private readonly gridEl = viewChild.required<ElementRef<HTMLElement>>('gridEl');
  private grid: GridStack | null = null;
  /** Tuiles rendues avant la création de la grille : les enfants s'initialisent avant le parent. */
  private readonly pending = new Set<HTMLElement>();

  constructor() {
    effect(() => {
      const editable = this.editable();
      this.grid?.setStatic(!editable);
    });
  }

  ngAfterViewInit(): void {
    const grid = GridStack.init(
      {
        column: GRID_COLUMNS,
        cellHeight: 80,
        margin: 8,
        float: false,
        animate: true,
        staticGrid: !this.editable(),
        // les contrôles de l'en-tête (réglages, menu, agrandir) ne déclenchent pas le déplacement
        draggable: {
          handle: '.widget-tile-header',
          cancel: 'input,textarea,button,select,option,app-select,app-button,app-split-button,.ng-select,.dropdown-menu',
        },
      },
      this.gridEl().nativeElement,
    );
    this.grid = grid;
    this.pending.forEach(el => this.makeWidget(el));
    this.pending.clear();

    grid.on('change', (_event: Event, nodes: GridStackNode[]) => this.positionsChange.emit(toPositions(nodes)));
    grid.on('added', () => this.emitAllPositions());
    grid.on('dragstop', () => this.userChanged.emit());
    grid.on('resizestop', () => this.userChanged.emit());
    this.emitAllPositions();
  }

  ngOnDestroy(): void {
    this.grid?.destroy(false);
    this.grid = null;
  }

  /**
   * Ouvre la tuile en grand. L'injecteur de la grille est transmis à la fenêtre : sans lui, le
   * widget n'y trouverait pas la période du tableau de bord (DashboardContext).
   */
  protected expand(item: DashboardItem, asTable: boolean): void {
    const ref = this.modal.open(WidgetExpandedModalComponent, { size: 'xl', centered: true, injector: this.injector });
    const dialog = ref.componentInstance as WidgetExpandedModalComponent;
    dialog.item.set(asTable ? { ...item, params: { ...item.params, viz: 'TABLE' } } : item);
    dialog.availability.set(this.availability().get(item.id) ?? 'INCONNU');
  }

  register(el: HTMLElement): void {
    if (!this.grid) {
      this.pending.add(el);
      return;
    }
    this.makeWidget(el);
  }

  unregister(el: HTMLElement): void {
    this.pending.delete(el);
    // Angular retire lui-même l'élément du DOM : GridStack n'a qu'à oublier le nœud
    this.grid?.removeWidget(el, false, false);
  }

  private makeWidget(el: HTMLElement): void {
    // GridStack.init adopte déjà les tuiles présentes dans le DOM
    if (!(el as GridItemHTMLElement).gridstackNode) {
      this.grid?.makeWidget(el);
    }
  }

  private emitAllPositions(): void {
    if (this.grid) {
      this.positionsChange.emit(toPositions(this.grid.engine.nodes));
    }
  }
}

/** Pose les attributs {@code gs-*} lus par GridStack et enregistre la tuile auprès de la grille. */
@Directive({
  selector: '[appDashboardGridItem]',
  host: {
    '[attr.gs-id]': 'item().id',
    '[attr.gs-x]': 'item().x',
    '[attr.gs-y]': 'item().y',
    '[attr.gs-w]': 'item().w',
    '[attr.gs-h]': 'item().h',
  },
})
export class DashboardGridItemDirective implements AfterViewInit, OnDestroy {
  readonly item = input.required<DashboardItem>({ alias: 'appDashboardGridItem' });

  private readonly grid = inject(DashboardGridComponent);
  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef);

  ngAfterViewInit(): void {
    this.grid.register(this.el.nativeElement);
  }

  ngOnDestroy(): void {
    this.grid.unregister(this.el.nativeElement);
  }
}

export function toPositions(nodes: GridStackNode[]): GridPosition[] {
  return nodes
    .filter(n => n.id != null)
    .map(n => ({ id: String(n.id), x: n.x ?? 0, y: n.y ?? 0, w: n.w ?? 1, h: n.h ?? 1 }));
}
