import { Component, signal, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import { ICellRendererAngularComp } from "ag-grid-angular";
import { ICellRendererParams } from "ag-grid-community";
import { IOrderLine } from "../../../../../shared/model/order-line.model";
import { ILot } from "../../../../../shared/model/lot.model";

@Component({
  selector: "app-lot-expand-cell",
  imports: [CommonModule, NgbTooltip],
  template: `
    <div class="lec-wrap">
      @if (hasLot()) {
        <i
          class="lec-chevron pi"
          [class.pi-chevron-down]="expanded()"
          [class.pi-chevron-right]="!expanded()"
          [class.lec-open]="expanded()"
          [class.lec-warn]="!expanded() && !isComplete()"
          [class.lec-ok]="!expanded() && isComplete()"
          [ngbTooltip]="expanded() ? 'Fermer la saisie' : isComplete() ? 'Lots complets ✓' : 'Saisir lots'"
          placement="left"
          (click)="onToggle($event)"
        ></i>

        @for (lot of lots(); track lot.id) {
          <span class="lec-badge">{{ lot.numLot }}</span>
        }
        @if (lots().length === 0 && !expanded()) {
          <span class="lec-none">Aucun lot</span>
        }
      } @else {
        <span class="lec-dash" ngbTooltip="Pas de gestion de lot pour ce produit" placement="left">—</span>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.Eager,
  styles: [`
    .lec-wrap {
      display: flex; align-items: center; gap: 4px;
      height: 100%; padding: 0 2px;
    }
    .lec-chevron {
      font-size: 0.78rem; flex-shrink: 0; cursor: pointer;
      transition: color .15s;
      &:hover { opacity: 0.7; }
    }
    .lec-open  { color: #0d6efd; }
    .lec-warn  { color: #f59e0b; }
    .lec-ok    { color: #16a34a; }
    .lec-badge {
      font-size: 0.65rem; font-family: monospace; padding: 0 3px;
      background: #e0f2fe; border-radius: 3px; color: #0369a1;
      white-space: nowrap;
    }
    .lec-none { font-size: 0.7rem; color: #9ca3af; font-style: italic; }
    .lec-dash { font-size: 0.8rem; color: #d1d5db; }
  `]
})
export class LotExpandCellComponent implements ICellRendererAngularComp {
  protected expanded   = signal(false);
  protected lots       = signal<ILot[]>([]);
  protected isComplete = signal(false);
  protected hasLot     = signal(false);

  private params!: ICellRendererParams;

  agInit(params: ICellRendererParams): void {
    this.params = params;
    this.sync(params);
  }

  refresh(params: ICellRendererParams): boolean {
    this.sync(params);
    return true;
  }

  protected onToggle(event: MouseEvent): void {
    event.stopPropagation();
    this.params.context.componentParent.onToggleLotExpand(this.params.data);
  }

  private sync(params: ICellRendererParams): void {
    const line = params.data as IOrderLine;
    this.hasLot.set(line.gestionLot !== false);
    this.expanded.set(!!params.data.__expanded);
    this.lots.set(line.lots ?? []);
    const total   = line.quantityReceivedTmp ?? line.quantityReceived ?? line.quantityRequested ?? 0;
    const covered = (line.lots ?? []).reduce((s, l) => s + (l.quantityReceived ?? 0), 0);
    this.isComplete.set(total > 0 && covered >= total);
  }
}
