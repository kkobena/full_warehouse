import {
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  input,
  output,
  signal,
  untracked,
  viewChild
} from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { ButtonModule } from "primeng/button";
import { TooltipModule } from "primeng/tooltip";
import { InputTextModule } from "primeng/inputtext";
import { forkJoin } from "rxjs";

import { IOrderLine } from "../../../../../shared/model/order-line.model";
import { ILot } from "../../../../../shared/model/lot.model";
import { DeliveryService } from "../../../../../entities/commande/delevery/delivery.service";
import { CommandeService } from "../../../../../entities/commande/commande.service";
import { LotService } from "../../../../../entities/commande/lot/lot.service";
import { NotificationService } from "../../../../../shared/services/notification.service";
import { ErrorService } from "../../../../../shared/error.service";

@Component({
  selector: "app-reception-sequential",
  templateUrl: "./reception-sequential.component.html",
  styleUrls: ["./reception-sequential.component.scss"],
  host: { "(window:keydown)": "onKeydown($event)" },
  imports: [CommonModule, FormsModule, ButtonModule, TooltipModule, InputTextModule]
})
export class ReceptionSequentialComponent {
  orderLines   = input.required<IOrderLine[]>();
  showLotBtn   = input<boolean>(false);
  /** Pré-remplissage lot depuis un scan DataMatrix (fourni par commande-received quand lotAutoCreated=false). */
  lotPrefill   = input<{ numLot: string; expiry: string } | null>(null);

  lineChanged       = output<IOrderLine>();
  allLinesProcessed = output<void>();

  // ── Étape active ─────────────────────────────────────────────────────────
  protected readonly step = signal<"qty" | "lot">("qty");

  // ── Navigation ───────────────────────────────────────────────────────────
  protected readonly ignoreComplete = signal(true);
  protected readonly currentLineId  = signal<number | null>(null);

  // ── Saisie quantités (signals pour que afterStock et pmpPreview soient réactifs)
  protected readonly draftQty = signal<number | null>(null);
  protected readonly draftUg  = signal<number>(0);
  protected readonly saving   = signal(false);

  // ── Saisie lot inline ────────────────────────────────────────────────────
  protected readonly lineLots    = signal<ILot[]>([]);
  protected draftLotNum    = "";
  protected draftLotExpiry = "";
  protected draftLotQty:  number | null = null;
  protected draftLotUg:   number | null = null;
  protected readonly lotSaving     = signal(false);
  protected readonly expiryWarning = signal<"none" | "soon" | "critical">("none");
  protected readonly lotJustAdded  = signal(false);

  /** Pré-remplissage en attente, appliqué quand on entre en step lot. */
  private pendingPrefill: { numLot: string; expiry: string } | null = null;
  /** Garde contre les réinitialisations en double dues aux mises à jour du parent. */
  private _lastLineId: number | null | undefined = undefined;

  // ── Computed ─────────────────────────────────────────────────────────────
  protected readonly visibleLines = computed(() => {
    const lines = this.orderLines();
    if (!this.ignoreComplete()) return lines;
    return lines.filter(l => !this.isComplete(l));
  });

  protected readonly currentLine = computed<IOrderLine | null>(() => {
    const id  = this.currentLineId();
    const all = this.orderLines();
    if (id != null) {
      const found = all.find(l => l.id === id);
      if (found) return found;
    }
    return this.visibleLines()[0] ?? null;
  });

  protected readonly currentIndex = computed(() => {
    const line = this.currentLine();
    if (!line) return -1;
    return this.visibleLines().findIndex(l => l.id === line.id);
  });

  protected readonly hasPrev = computed(() => this.currentIndex() > 0);
  protected readonly hasNext = computed(() => this.currentIndex() < this.visibleLines().length - 1);

  protected readonly progress = computed(() => {
    const all = this.orderLines();
    if (!all.length) return 0;
    return Math.round((all.filter(l => this.isComplete(l)).length / all.length) * 100);
  });

  protected readonly afterStock = computed(() => {
    const line = this.currentLine();
    if (!line) return 0;
    return (line.initStock ?? 0) + (this.draftQty() ?? 0) + (this.draftUg() ?? 0);
  });

  /** Aperçu PMP : (initStock × PMP_actuel + qteReçue × PA_commande) / (initStock + qteReçue). */
  protected readonly pmpPreview = computed<number | null>(() => {
    const line = this.currentLine();
    if (!line) return null;
    const initStock = line.initStock ?? 0;
    const pmpActuel = line.costAmount ?? 0;
    const qteRecue  = this.draftQty() ?? 0;
    const prixAchat = line.orderCostAmount ?? 0;
    if (qteRecue <= 0 || prixAchat <= 0) return null;
    if (initStock + qteRecue <= 0) return null;
    return Math.round((initStock * pmpActuel + qteRecue * prixAchat) / (initStock + qteRecue));
  });

  /** Sens de variation du PMP par rapport au PMP actuel. */
  protected readonly pmpVariation = computed<"up" | "down" | "stable" | null>(() => {
    const prev = this.currentLine()?.costAmount ?? 0;
    const next = this.pmpPreview();
    if (next == null || prev === 0) return null;
    if (next > prev) return "up";
    if (next < prev) return "down";
    return "stable";
  });

  protected readonly remainingLotQty = computed(() => {
    const line = this.currentLine();
    if (!line) return 0;
    const total = line.quantityReceivedTmp ?? line.quantityReceived ?? 0;
    return Math.max(0, total - this.lineLots().reduce((s, l) => s + (l.quantityReceived ?? 0), 0));
  });

  protected readonly remainingLotUg = computed(() => {
    const line = this.currentLine();
    if (!line) return 0;
    return Math.max(0, (line.freeQty ?? 0) - this.lineLots().reduce((s, l) => s + (l.ugQuantityReceived ?? 0), 0));
  });

  protected readonly showUg = computed(() => (this.currentLine()?.freeQty ?? 0) > 0);

  protected readonly isUgOnlyLotMode = computed(() =>
    this.remainingLotQty() === 0 && this.remainingLotUg() > 0
  );

  protected readonly lotsComplete = computed(() => {
    if (!this.showLotBtn()) return true;
    const line = this.currentLine();
    if (!line || line.gestionLot === false) return true;
    return this.remainingLotQty() === 0 && this.remainingLotUg() === 0;
  });

  /** Lignes éligibles au lot commun : gestion lot, sans lot, quantité reçue > 0, hors ligne courante. */
  protected readonly linesForBatch = computed(() =>
    this.orderLines().filter(l =>
      l.id !== this.currentLine()?.id &&
      l.gestionLot !== false &&
      (l.lots?.length ?? 0) === 0 &&
      (l.quantityReceivedTmp ?? l.quantityReceived ?? 0) > 0
    )
  );

  // ── ViewChildren ─────────────────────────────────────────────────────────
  private readonly qtyInputRef    = viewChild<ElementRef>("qtyInput");
  private readonly numLotInputRef = viewChild<ElementRef>("numLotInput");

  // ── Services ─────────────────────────────────────────────────────────────
  private readonly deliveryService     = inject(DeliveryService);
  private readonly commandeService     = inject(CommandeService);
  private readonly lotService          = inject(LotService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService        = inject(ErrorService);

  constructor() {
    // Effet unique — calcule l'ID effectif sans boucle infinie.
    // Écrit currentLineId UNIQUEMENT pour l'initialisation (null → 1ère ligne visible).
    // Quand currentLineId est déjà setté mais la ligne a été filtrée,
    // goNext() / goPrev() gèrent eux-mêmes avec idx=-1.
    effect(() => {
      const lines  = this.visibleLines();   // tracké
      const lineId = this.currentLineId();  // tracké

      untracked(() => {
        // ID effectif : lineId valide → garder, sinon → premier visible
        const effectiveId: number | null =
          (lineId != null && lines.some(l => l.id === lineId))
            ? lineId
            : (lines[0]?.id ?? null);

        // Snap initial uniquement : null → 1ère ligne visible
        // Ce write provoque un 2e déclenchement, mais _lastLineId === effectiveId → return early
        if (lineId == null && effectiveId != null) {
          this.currentLineId.set(effectiveId);
        }

        if (effectiveId === this._lastLineId) return; // même ligne → pas de reset
        this._lastLineId = effectiveId;

        const line = effectiveId != null
          ? (lines.find(l => l.id === effectiveId) ?? null)
          : null;
        this.step.set("qty");
        this.resetQtyDraft(line);
        this.lineLots.set([...(line?.lots ?? [])]);
        this.resetLotDraft();
        this.focusQty();
      });
    });

    // Stocker le pré-remplissage scan pour l'appliquer à l'entrée en step lot
    effect(() => {
      const pf = this.lotPrefill();
      untracked(() => {
        if (!pf) return;
        this.pendingPrefill = pf;
        if (this.step() === "lot") {
          this.applyPrefill(pf);
          this.pendingPrefill = null;
        }
      });
    });
  }

  // ── Clavier global ────────────────────────────────────────────────────────
  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === "F8")  { event.preventDefault(); this.goPrev(); }
    if (event.key === "F9")  { event.preventDefault(); this.goNext(); }
    if (event.key === "F12") { event.preventDefault(); this.onF12(); }
  }

  private onF12(): void {
    if (this.step() === "qty") this.onValidateQty();
    else this.onSaveLot();
  }

  // ── Navigation ────────────────────────────────────────────────────────────
  protected selectLine(line: IOrderLine): void {
    this.currentLineId.set(line.id ?? null);
  }

  protected toggleIgnoreComplete(event: Event): void {
    this.ignoreComplete.set((event.target as HTMLInputElement).checked);
  }

  protected goPrev(): void {
    const lines = this.visibleLines();
    const currentId = this.currentLine()?.id ?? null;
    const idx = currentId != null ? lines.findIndex(l => l.id === currentId) : -1;
    if (idx > 0) this.currentLineId.set(lines[idx - 1].id ?? null);
  }

  protected goNext(): void {
    const lines     = this.visibleLines();
    // Utiliser currentLine() (qui a le fallback) pour déterminer la position courante
    const currentId = this.currentLine()?.id ?? null;
    const idx       = currentId != null ? lines.findIndex(l => l.id === currentId) : -1;

    if (idx >= 0 && idx < lines.length - 1) {
      this.currentLineId.set(lines[idx + 1].id ?? null);
    } else if (idx >= 0 && idx === lines.length - 1) {
      this.allLinesProcessed.emit();
    } else if (idx === -1) {
      // currentLine non trouvée dans visibleLines → chercher la première visible après sa position
      const allLines = this.orderLines();
      const posInAll = currentId != null ? allLines.findIndex(l => l.id === currentId) : -1;
      const nextVisible = lines.find(l => allLines.findIndex(a => a.id === l.id) > posInAll);
      if (nextVisible) {
        this.currentLineId.set(nextVisible.id ?? null);
      } else if (lines.length > 0) {
        this.allLinesProcessed.emit();
      }
    }
  }

  // ── ÉTAPE 1 : Validation quantités ───────────────────────────────────────
  protected onValidateQty(): void {
    const line = this.currentLine();
    if (!line || this.saving()) return;

    line.quantityReceived    = this.draftQty() ?? 0;
    line.quantityReceivedTmp = this.draftQty() ?? 0;
    line.freeQty             = this.draftUg() ?? 0;

    this.saving.set(true);
    this.deliveryService.updateQuantityReceived(line).subscribe({
      next: () => {
        if ((this.draftUg() ?? 0) > 0) {
          this.commandeService.updateQuantityUG(line).subscribe({
            next:  () => { this.saving.set(false); this.afterQtySaved(line); },
            error: err => { this.saving.set(false); this.notificationService.error(this.errorService.getErrorMessage(err), "UG"); }
          });
        } else {
          this.saving.set(false);
          this.afterQtySaved(line);
        }
      },
      error: err => { this.saving.set(false); this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur"); }
    });
  }

  private afterQtySaved(line: IOrderLine): void {
    this.lineChanged.emit(line);
    // Aller en step lot si : lots incomplets OU lots existants (permettre modification)
    const hasExistingLots = this.lineLots().length > 0;
    const needLots = this.showLotBtn() && line.gestionLot !== false &&
      (!this.lotsComplete() || hasExistingLots);
    if (needLots) {
      this.lineLots.set([...(line.lots ?? [])]);
      this.resetLotDraft();
      if (this.pendingPrefill) {
        this.applyPrefill(this.pendingPrefill);
        this.pendingPrefill = null;
      }
      this.step.set("lot");
      setTimeout(() => this.focusNumLot(), 60);
    } else {
      this.goNext();
    }
  }

  /** Accès direct au step lot depuis l'étape quantités. */
  protected goToLotStep(): void {
    this.step.set("lot");
    setTimeout(() => this.focusNumLot(), 60);
  }

  // ── ÉTAPE 2 : Saisie lot inline ──────────────────────────────────────────
  protected recomputeLotUg(): void {
    const qty    = this.draftLotQty;
    const remQty = this.remainingLotQty();
    const remUg  = this.remainingLotUg();
    if (remUg === 0 || !qty || qty <= 0) {
      this.draftLotUg = remUg > 0 ? remUg : null;
      return;
    }
    this.draftLotUg = qty >= remQty ? remUg : (Math.round(remUg * qty / remQty) || 0);
  }

  protected onLotExpiryInput(value: string): void {
    this.draftLotExpiry = this.autoFormatExpiry(value);
    this.expiryWarning.set(this.calcExpiryWarning(this.draftLotExpiry));
  }

  protected onSaveLot(): void {
    const line = this.currentLine();
    if (!line || this.lotSaving()) return;

    if (!this.draftLotNum.trim()) {
      this.notificationService.error("Le numéro de lot est obligatoire", "Lot"); return;
    }
    const expiryDate = this.formatExpiryForSave(this.draftLotExpiry);
    if (!expiryDate) {
      this.notificationService.error("Format invalide. Utilisez MM/AAAA (ex: 06/2028)", "Lot"); return;
    }
    const qty = this.isUgOnlyLotMode() ? 0 : (this.draftLotQty ?? 0);
    if (!this.isUgOnlyLotMode() && qty <= 0) {
      this.notificationService.error("La quantité doit être supérieure à 0", "Lot"); return;
    }
    if (qty > this.remainingLotQty()) {
      this.notificationService.error(`Qté (${qty}) dépasse le restant (${this.remainingLotQty()})`, "Lot"); return;
    }
    const ug = this.draftLotUg ?? 0;
    if (ug > this.remainingLotUg()) {
      this.notificationService.error(`UG (${ug}) dépasse le restant (${this.remainingLotUg()})`, "Lot"); return;
    }
    if (this.lineLots().some(l => l.numLot === this.draftLotNum.trim())) {
      this.notificationService.error(`Lot "${this.draftLotNum}" déjà enregistré pour cette ligne`, "Doublon"); return;
    }

    this.lotSaving.set(true);
    this.lotService.addLot({
      numLot: this.draftLotNum.trim(),
      expiryDate,
      quantityReceived:   qty,
      ugQuantityReceived: ug,
      receiptItemId: line.orderLineId
    }).subscribe({
      next: res => {
        const updated = [...this.lineLots(), res.body!];
        this.lineLots.set(updated);
        line.lots = updated;
        this.lotSaving.set(false);
        this.lineChanged.emit(line);

        this.lotJustAdded.set(true);
        setTimeout(() => this.lotJustAdded.set(false), 1800);

        if (this.lotsComplete()) {
          setTimeout(() => this.goNext(), 600);
        } else {
          this.resetLotDraft();
          setTimeout(() => this.focusNumLot(), 50);
        }
      },
      error: err => {
        this.lotSaving.set(false);
        this.notificationService.error(this.errorService.getErrorMessage(err), "Lot");
      }
    });
  }

  /** Appliquer le lot courant à toutes les lignes sans lot ayant une quantité reçue. */
  protected onApplyLotToAll(): void {
    const expiryDate = this.formatExpiryForSave(this.draftLotExpiry);
    if (!this.draftLotNum.trim() || !expiryDate) return;
    const targets = this.linesForBatch();
    if (!targets.length) return;

    const numLot = this.draftLotNum.trim();
    this.lotSaving.set(true);

    forkJoin(
      targets.map(l => this.lotService.addLot({
        numLot,
        expiryDate,
        quantityReceived:   l.quantityReceivedTmp ?? l.quantityReceived ?? 0,
        ugQuantityReceived: l.freeQty ?? 0,
        receiptItemId: l.orderLineId
      }))
    ).subscribe({
      next: results => {
        targets.forEach((l, i) => {
          const body = results[i].body!;
          l.lots = [...(l.lots ?? []), body];
          this.lineChanged.emit(l);
        });
        this.lotSaving.set(false);
        this.notificationService.success(`Lot appliqué à ${results.length} ligne(s)`, "Lot commun");
      },
      error: err => {
        this.lotSaving.set(false);
        this.notificationService.error(this.errorService.getErrorMessage(err), "Lot commun");
      }
    });
  }

  protected onDeleteLot(lot: ILot): void {
    if (!lot.id) return;
    this.lotService.remove(lot.id).subscribe({
      next: () => {
        const updated = this.lineLots().filter(l => l.id !== lot.id);
        this.lineLots.set(updated);
        const line = this.currentLine();
        if (line) { line.lots = updated; this.lineChanged.emit(line); }
        this.resetLotDraft();
      },
      error: err => this.notificationService.error(this.errorService.getErrorMessage(err), "Lot")
    });
  }

  protected onSkipLots(): void {
    this.step.set("qty");
    this.goNext();
  }

  // ── Helpers template ──────────────────────────────────────────────────────
  protected isComplete(line: IOrderLine): boolean {
    const rec = line.quantityReceivedTmp ?? line.quantityReceived ?? 0;
    const cmd = line.quantityRequested ?? 0;
    if (cmd === 0 || rec < cmd) return false;
    if (this.showLotBtn() && line.gestionLot !== false) {
      const lotQty = (line.lots ?? []).reduce((s, l) => s + (l.quantityReceived ?? 0), 0);
      return lotQty >= rec;
    }
    return true;
  }

  protected isPcbAlert(): boolean {
    const pcb = this.currentLine()?.qteColis;
    if (!pcb || pcb <= 1) return false;
    const qty = this.draftQty() ?? 0;
    return qty > 0 && (qty % pcb) !== 0;
  }

  protected formatLotExpiry(lot: ILot): string {
    const d = lot.expiryDate as unknown as string | undefined;
    if (!d) return "—";
    const m = d.match(/^(\d{4})-(\d{2})/);
    return m ? `${m[2]}/${m[1]}` : d;
  }

  protected canSaveLot(): boolean {
    if (!this.draftLotNum.trim() || !this.draftLotExpiry || this.lotSaving()) return false;
    if (!this.formatExpiryForSave(this.draftLotExpiry)) return false;
    if (this.isUgOnlyLotMode()) return (this.draftLotUg ?? 0) > 0;
    return (this.draftLotQty ?? 0) > 0;
  }

  protected canApplyToAll(): boolean {
    return !!this.draftLotNum.trim() &&
      !!this.formatExpiryForSave(this.draftLotExpiry) &&
      this.linesForBatch().length > 0 &&
      !this.lotSaving();
  }

  // ── Privé ─────────────────────────────────────────────────────────────────
  private resetQtyDraft(line: IOrderLine | null): void {
    this.draftQty.set(line ? (line.quantityReceivedTmp ?? line.quantityReceived ?? line.quantityRequested ?? null) : null);
    this.draftUg.set(line?.freeQty ?? 0);
  }

  private resetLotDraft(): void {
    this.draftLotNum    = "";
    this.draftLotExpiry = "";
    this.expiryWarning.set("none");
    const rem   = this.remainingLotQty();
    const remUg = this.remainingLotUg();
    this.draftLotQty = rem   > 0 ? rem   : null;
    this.draftLotUg  = remUg > 0 ? remUg : null;
  }

  private applyPrefill(pf: { numLot: string; expiry: string }): void {
    this.draftLotNum = pf.numLot;
    this.onLotExpiryInput(pf.expiry);
    setTimeout(() => this.focusNumLot(), 50);
  }

  private focusQty(): void {
    setTimeout(() => {
      const el = this.qtyInputRef()?.nativeElement as HTMLInputElement | undefined;
      if (el) { el.focus(); el.select(); }
    }, 60);
  }

  private focusNumLot(): void {
    const el = this.numLotInputRef()?.nativeElement as HTMLInputElement | undefined;
    if (el) { el.focus(); el.select(); }
  }

  /** Sélectionne tout le contenu d'un input au focus (pour ne pas avoir à effacer avant de saisir). */
  protected selectOnFocus(event: FocusEvent): void {
    (event.target as HTMLInputElement)?.select();
  }

  private autoFormatExpiry(value: string): string {
    const d = value.replace(/\D/g, "").slice(0, 6);
    if (d.length <= 2) return d;
    return `${d.slice(0, 2)}/${d.slice(2)}`;
  }

  protected formatExpiryForSave(value: string): string | null {
    const m = value.match(/^(\d{1,2})\/(\d{4})$/);
    if (!m) return null;
    const mo = parseInt(m[1], 10), yr = parseInt(m[2], 10);
    if (mo < 1 || mo > 12 || yr < 2000) return null;
    const lastDay = new Date(yr, mo, 0).getDate();
    return `${yr}-${String(mo).padStart(2, "0")}-${String(lastDay).padStart(2, "0")}`;
  }

  private calcExpiryWarning(value: string): "none" | "soon" | "critical" {
    const iso = this.formatExpiryForSave(value);
    if (!iso) return "none";
    const months = (new Date(iso).getTime() - Date.now()) / (1000 * 60 * 60 * 24 * 30.44);
    if (months < 3) return "critical";
    if (months < 6) return "soon";
    return "none";
  }
}
