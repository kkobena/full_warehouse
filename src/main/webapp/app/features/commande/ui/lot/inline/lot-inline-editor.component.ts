import { Component, computed, inject, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { ICellRendererAngularComp } from "ag-grid-angular";
import { ICellRendererParams } from "ag-grid-community";
import { IOrderLine } from "../../../../../shared/model/order-line.model";
import { ILot } from "../../../../../shared/model/lot.model";
import { LotService } from "../../../../../entities/commande/lot/lot.service";
import { ButtonModule } from "primeng/button";
import { TooltipModule } from "primeng/tooltip";
import { InputTextModule } from "primeng/inputtext";
import { NotificationService } from "../../../../../shared/services/notification.service";
import { NgbConfirmDialogService } from "../../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { ErrorService } from "../../../../../shared/error.service";

@Component({
  selector: "app-lot-inline-editor",
  imports: [CommonModule, FormsModule, ButtonModule, TooltipModule, InputTextModule],
  template: `
    <div class="lie-wrap">
      <!-- Header -->
      <div class="lie-header">
        <div class="lie-header__info">
          <i class="pi pi-box"></i>
          <span class="lie-produit">{{ line()?.produitLibelle }}</span>
          @if (remainingQty() > 0) {
            <span class="lie-badge lie-badge--warn">{{ remainingQty() }} restante(s)</span>
          } @else {
            <span class="lie-badge lie-badge--ok"><i class="pi pi-check me-1"></i>Complets</span>
          }
        </div>
        <p-button
          icon="pi pi-times"
          [text]="true" [rounded]="true" size="small" severity="secondary"
          pTooltip="Fermer" tooltipPosition="left"
          (onClick)="onClose()"
        />
      </div>

      <!-- Body -->
      <div class="lie-body">

        <!-- Liste des lots existants -->
        @if (lots().length > 0) {
          <table class="lie-table">
            <thead>
              <tr>
                <th>N° Lot</th>
                <th>Date péremption</th>
                <th class="lie-col-r">Qté</th>
                @if (showUg()) { <th class="lie-col-r">UG</th> }
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (lot of lots(); track lot.id) {
                <tr>
                  <td><code class="lie-code">{{ lot.numLot }}</code></td>
                  <td
                    [class.lie-expiry-soon]="isExpirySoon(lot)"
                    [class.lie-expiry-critical]="isExpiryCritical(lot)"
                  >{{ lot.expiryDate | date:'dd/MM/yyyy' }}</td>
                  <td class="lie-col-r">{{ lot.quantityReceived }}</td>
                  @if (showUg()) { <td class="lie-col-r">{{ lot.ugQuantityReceived ?? 0 }}</td> }
                  <td class="lie-col-act">
                    <p-button
                      icon="pi pi-trash"
                      [text]="true" [rounded]="true" size="small" severity="danger"
                      pTooltip="Supprimer ce lot" tooltipPosition="top"
                      (onClick)="onConfirmDelete(lot)"
                    />
                  </td>
                </tr>
              }
            </tbody>
          </table>
        }

        <!-- Formulaire d'ajout — visible uniquement si la quantité n'est pas encore couverte -->
        @if (remainingQty() > 0) {
          <div class="lie-draft">
            <input
              pInputText
              [(ngModel)]="draftNumLot"
              placeholder="N° lot"
              class="lie-field lie-field--numlot"
              (keydown.tab)="$event.stopPropagation()"
              (keydown.enter)="onSaveDraft()"
            />
            <input
              pInputText
              [(ngModel)]="draftExpiry"
              placeholder="jj/MM/AAAA"
              maxlength="10"
              class="lie-field lie-field--expiry"
              [class.lie-expiry-soon]="expiryWarning() === 'soon'"
              [class.lie-expiry-critical]="expiryWarning() === 'critical'"
              (ngModelChange)="onExpiryInput($event)"
              (keydown.tab)="$event.stopPropagation()"
              (keydown.enter)="onSaveDraft()"
            />
            <input
              pInputText
              type="number"
              [(ngModel)]="draftQty"
              [min]="1"
              [max]="remainingQty()"
              placeholder="Qté"
              class="lie-field lie-field--qty"
              (ngModelChange)="onQtyInput($event)"
              (keydown.tab)="$event.stopPropagation()"
              (keydown.enter)="onSaveDraft()"
            />
            @if (showUg()) {
              <input
                pInputText
                type="number"
                [(ngModel)]="draftUg"
                [min]="0"
                placeholder="UG"
                class="lie-field lie-field--ug"
                (keydown.tab)="$event.stopPropagation()"
                (keydown.enter)="onSaveDraft()"
              />
            }
            <p-button
              icon="pi pi-check"
              label="Ajouter"
              size="small"
              severity="success"
              [loading]="saving()"
              [disabled]="!canSave()"
              (onClick)="onSaveDraft()"
            />
          </div>
        } @else if (lots().length > 0) {
          <div class="lie-complete">
            <i class="pi pi-check-circle me-1" style="color:#16a34a"></i>
            Tous les lots sont renseignés — supprimez un lot pour modifier.
          </div>
        }

      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      height: 100%;
      background: #f0f9ff;
      border-top: 2px solid #7dd3fc;
      padding: 4px 10px 4px 16px;
      box-sizing: border-box;
    }
    .lie-wrap { display: flex; flex-direction: column; height: 100%; gap: 4px; }

    .lie-header {
      display: flex; align-items: center; justify-content: space-between;
      padding: 2px 0 4px; border-bottom: 1px solid #bae6fd; flex-shrink: 0;
      &__info { display: flex; align-items: center; gap: 6px; font-size: 0.8rem; font-weight: 600; color: #0369a1; }
    }
    .lie-produit { font-size: 0.79rem; }
    .lie-badge {
      font-size: 0.68rem; padding: 1px 7px; border-radius: 10px;
      &--warn { background: #fef3c7; color: #b45309; font-weight: 700; }
      &--ok   { background: #d1fae5; color: #065f46; }
    }

    .lie-body { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 4px; min-height: 0; }

    .lie-table {
      width: 100%; border-collapse: collapse; font-size: 0.77rem;
      th {
        background: #e0f2fe; padding: 3px 6px; text-align: left;
        font-weight: 600; font-size: 0.71rem; color: #0369a1; white-space: nowrap;
      }
      td { padding: 2px 6px; border-bottom: 1px solid #f0f9ff; font-size: 0.77rem; vertical-align: middle; }
      tr:last-child td { border-bottom: none; }
    }
    .lie-col-r   { text-align: right; }
    .lie-col-act { text-align: right; padding: 0 2px; }
    .lie-code    { font-family: monospace; font-size: 0.77rem; color: #0f172a; }

    .lie-expiry-soon     { color: #b45309; font-weight: 600; }
    .lie-expiry-critical { color: #dc2626; font-weight: 700; }

    .lie-complete {
      font-size: 0.77rem; color: #065f46; font-style: italic; padding: 4px 6px; flex-shrink: 0;
    }

    .lie-draft {
      display: flex; align-items: center; gap: 6px; padding: 4px 0;
      border-top: 1px dashed #93c5fd; flex-shrink: 0; flex-wrap: wrap;

      .lie-field {
        height: 28px; font-size: 0.77rem; padding: 0 6px; border-radius: 4px;
        &--numlot { width: 120px; }
        &--expiry { width: 110px; }
        &--qty    { width: 62px; }
        &--ug     { width: 55px; }
      }
    }
  `]
})
export class LotInlineEditorComponent implements ICellRendererAngularComp {
  private readonly lotService        = inject(LotService);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmDialog     = inject(NgbConfirmDialogService);
  private readonly errorService = inject(ErrorService);
  protected readonly line          = signal<IOrderLine | null>(null);
  protected readonly lots          = signal<ILot[]>([]);
  protected readonly saving        = signal(false);
  protected readonly expiryWarning = signal<"none" | "soon" | "critical">("none");

  protected draftNumLot = "";
  protected draftExpiry = "";
  protected draftQty: number | null = null;
  protected draftUg: number | null = null;

  protected readonly remainingQty = computed(() => {
    const line = this.line();
    const total = line?.quantityReceivedTmp ?? line?.quantityReceived ?? line?.quantityRequested ?? 0;
    return total - this.lots().reduce((s, l) => s + (l.quantityReceived ?? 0), 0);
  });

  protected readonly showUg = computed(() => (this.line()?.freeQty ?? 0) > 0);

  protected canSave(): boolean {
    return !!this.draftNumLot && !!this.draftExpiry && (this.draftQty ?? 0) > 0 && !this.saving();
  }

  private params!: ICellRendererParams;

  agInit(params: ICellRendererParams): void {
    this.params = params;
    const line = params.data.__line as IOrderLine;
    this.line.set(line);
    this.lots.set([...(line.lots ?? [])]);
    const total = line.quantityReceivedTmp ?? line.quantityReceived ?? line.quantityRequested ?? 0;
    const covered = (line.lots ?? []).reduce((s, l) => s + (l.quantityReceived ?? 0), 0);
    this.draftQty = Math.max(1, total - covered);
  }

  refresh(): boolean {
    return false;
  }

  protected onQtyInput(value: number | null): void {
    const max = this.remainingQty();
    if ((value ?? 0) > max) this.draftQty = max;
  }

  protected onExpiryInput(value: string): void {
    const digits = value.replace(/\D/g, "").slice(0, 8);
    let formatted = digits;
    if (digits.length > 2) formatted = `${digits.slice(0, 2)}/${digits.slice(2)}`;
    if (digits.length > 4) formatted = `${digits.slice(0, 2)}/${digits.slice(2, 4)}/${digits.slice(4)}`;
    this.draftExpiry = formatted;

    const parsed = this.parseExpiry(formatted);
    if (!parsed) { this.expiryWarning.set("none"); return; }
    const months = (parsed.getTime() - Date.now()) / (1000 * 60 * 60 * 24 * 30.44);
    this.expiryWarning.set(months < 3 ? "critical" : months < 6 ? "soon" : "none");
  }

  protected onSaveDraft(): void {
    const line = this.line();
    if (!line || !this.draftNumLot || !this.draftExpiry || !(this.draftQty ?? 0)) return;

    const qty = this.draftQty ?? 0;
    const max = this.remainingQty();
    if (qty > max) {
      this.notificationService.error(`La quantité (${qty}) dépasse le restant à affecter (${max})`, "Lot");
      return;
    }
    const expiryDate = this.formatExpiry(this.draftExpiry);
    if (!expiryDate) {
      this.notificationService.error("Format date invalide. Utilisez jj/MM/AAAA (ex: 01/06/2026)", "Lot");
      return;
    }
    if (this.lots().some(l => l.numLot === this.draftNumLot)) {
      this.notificationService.error(`Le lot "${this.draftNumLot}" est déjà enregistré pour cette ligne`, "Lot doublon");
      return;
    }

    this.saving.set(true);
    this.lotService.addLot({
      numLot:             this.draftNumLot,
      expiryDate,
      quantityReceived:   qty,
      ugQuantityReceived: this.draftUg ?? 0,
      receiptItemId:      line.orderLineId,
    }).subscribe({
      next: res => {
        const saved   = res.body!;
        const updated = [...this.lots(), saved];
        this.lots.set(updated);
        this.draftNumLot = "";
        this.draftExpiry = "";
        this.draftUg     = null;
        this.expiryWarning.set("none");
        this.saving.set(false);
        const rem = this.remainingQty();
        this.draftQty = rem > 0 ? rem : null;
        this.params.context.componentParent.onLotSaved(line, updated);
        if (rem <= 0) {
          setTimeout(() => this.params.context.componentParent.onCollapseRow(line), 600);
        }
      },
      error: (err) => {
        this.saving.set(false);
        this.notificationService.error(this.errorService.getErrorMessage(err) ?? "Erreur lors de l'enregistrement du lot", "Lot");
      }
    });
  }

  protected onConfirmDelete(lot: ILot): void {
    this.confirmDialog.onConfirm(
      () => this.doDeleteLot(lot),
      "Suppression de lot",
      `Supprimer le lot <strong>${lot.numLot}</strong> (qté&nbsp;: ${lot.quantityReceived}) ?`,
      "pi pi-trash"
    );
  }

  private doDeleteLot(lot: ILot): void {
    if (!lot.id) return;
    this.lotService.remove(lot.id).subscribe({
      next: () => {
        const updated = this.lots().filter(l => l.id !== lot.id);
        this.lots.set(updated);
        const rem = this.remainingQty();
        this.draftQty = rem > 0 ? rem : 1;
        this.params.context.componentParent.onLotSaved(this.line()!, updated);
      },
    });
  }

  protected onClose(): void {
    this.params.context.componentParent.onCollapseRow(this.line());
  }

  protected isExpirySoon(lot: ILot): boolean {
    if (!lot.expiryDate) return false;
    const months = (new Date(lot.expiryDate).getTime() - Date.now()) / (1000 * 60 * 60 * 24 * 30.44);
    return months >= 3 && months < 6;
  }

  protected isExpiryCritical(lot: ILot): boolean {
    if (!lot.expiryDate) return false;
    const months = (new Date(lot.expiryDate).getTime() - Date.now()) / (1000 * 60 * 60 * 24 * 30.44);
    return months < 3;
  }

  private parseExpiry(value: string): Date | null {
    const m = value.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
    if (!m) return null;
    const day = parseInt(m[1], 10);
    const mo  = parseInt(m[2], 10) - 1;
    const yr  = parseInt(m[3], 10);
    if (day < 1 || day > 31 || mo < 0 || mo > 11 || yr < 2000) return null;
    return new Date(yr, mo, day);
  }

  private formatExpiry(value: string): string | null {
    const m = value.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
    if (!m) return null;
    return `${m[3]}-${m[2].padStart(2, "0")}-${m[1].padStart(2, "0")}`;
  }
}
