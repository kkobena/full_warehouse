import { Component, computed, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { CommonModule } from "@angular/common";
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from "@angular/forms";
import { NgbDateStruct, NgbNavModule, NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import { FournisseurApApiService } from "../../data-access/services/fournisseur-ap-api.service";
import {
  ICompteFournisseurAP,
  IFournisseurAPSummary,
  ILigneFournisseurAP,
  IReglementBL,
  StatutFournisseur,
  StatutLigne
} from "../../data-access/models";
import { formatCurrency } from "app/shared/utils/format-utils";
import { NGB_DATE_TO_ISO, TODAY_NGB_DATE } from "app/shared/util/warehouse-util";
import { NotificationService } from "app/shared/services/notification.service";
import { NgbConfirmDialogService } from "app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { BlobDownloadService } from "../../../../shared/services/blob-download.service";
import {
  AppTableLazyLoadEvent,
  BadgeComponent,
  ButtonComponent,
  DataTableComponent,
  FloatLabelComponent,
  IconFieldComponent,
  InputNumberComponent,
  SelectComponent,
  ToolbarComponent
} from "../../../../shared/ui";
import { PharmaDatePickerComponent } from "../../../../shared/date-picker/pharma-date-picker.component";

@Component({
  selector: "app-comptes-fournisseurs",
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    NgbNavModule,
    ButtonComponent,
    DataTableComponent,
    ToolbarComponent,
    BadgeComponent,
    InputNumberComponent,
    FloatLabelComponent,
    SelectComponent,
    IconFieldComponent,
    PharmaDatePickerComponent,
    NgbTooltip
  ],
  templateUrl: "./comptes-fournisseurs.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./comptes-fournisseurs.component.scss"
})
export class ComptesFournisseursComponent implements OnInit {
  // ── State ──────────────────────────────────────────────────────────────────
  comptes = signal<ICompteFournisseurAP[]>([]);
  summary = signal<IFournisseurAPSummary | null>(null);
  lignes = signal<ILigneFournisseurAP[]>([]);
  selectedFournisseur = signal<ICompteFournisseurAP | null>(null);

  isLoading = signal(false);
  isLoadingLignes = signal(false);
  isSaving = signal(false);
  isExporting = signal(false);

  panelOpen = signal(false);
  activeTab = signal<string>("commandes");
  searchText = signal("");

  selectedLigne = signal<ILigneFournisseurAP | null>(null);
  ligneReglements = signal<IReglementBL[]>([]);
  isLoadingLigneReglements = signal(false);
  filtreStatutLignes = signal<StatutLigne | null>(null);
  totalLignes = signal(0);
  readonly PAGE_SIZE = 10;

  fromDate = signal<NgbDateStruct | null>(null);
  toDate = signal<NgbDateStruct | null>(null);

  showHint = signal<boolean>(localStorage.getItem("ap-hint-dismissed") !== "1");

  // ── Signal Forms ───────────────────────────────────────────────────────────
  montantSaisi = signal(0);

  solde = computed(() => this.selectedFournisseur()?.solde ?? 0);
  validMontant = computed(() => this.montantSaisi() > 0 && this.montantSaisi() <= this.solde());

  readonly modeReglementOptions = [
    { label: "Espèces", value: "CASH" },
    { label: "Chèque", value: "CH" },
    { label: "Virement", value: "VIREMENT" },
    { label: "Carte bancaire", value: "CB" }
   // { label: "Traite/Effet", value: "TRAITE" }
  ];

  readonly reglementForm = inject(FormBuilder).group({
    montant: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(1)],
      nonNullable: true
    }),
    dateReglement: new FormControl<NgbDateStruct | null>(TODAY_NGB_DATE()),
    modeReglement: new FormControl<string | null>("CH", {
      validators: [Validators.required],
      nonNullable: true
    }),
    reference: new FormControl<string | null>(null, {
      validators: [Validators.required],
      nonNullable: true
    }),
    commentaire: new FormControl<string | null>(null)
  });

  formatCurrency = formatCurrency;

  private readonly api = inject(FournisseurApApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly notification = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly blobDownload = inject(BlobDownloadService);
  constructor() {
    this.reglementForm
      .get("montant")!
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(val => this.montantSaisi.set(val ?? 0));
  }

  ngOnInit(): void {
    this.loadSummary();
    this.loadComptes();
  }

  // ── Data loading ───────────────────────────────────────────────────────────
  loadSummary(): void {
    this.api.getSummary().subscribe({
      next: res => this.summary.set(res.body)
    });
  }

  loadComptes(): void {
    this.isLoading.set(true);
    const from = this.fromDate() ? (NGB_DATE_TO_ISO(this.fromDate()!) ?? undefined) : undefined;
    const to = this.toDate() ? (NGB_DATE_TO_ISO(this.toDate()!) ?? undefined) : undefined;
    this.api.getComptes(from, to).subscribe({
      next: res => {
        this.comptes.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  // ── Master/detail ──────────────────────────────────────────────────────────
  onTabChange(tab: string | number): void {
    const id = String(tab);
    this.activeTab.set(id);
    if (id === "regler") {
      this.reglementForm.reset({
        montant: this.solde(),
        dateReglement: TODAY_NGB_DATE(),
        modeReglement: "CH",
        reference: null,
        commentaire: null
      });
    }
  }

  selectLigne(ligne: ILigneFournisseurAP): void {
    this.selectedLigne.set(ligne);
    this.activeTab.set("reglements-bl");
    this.ligneReglements.set([]);
    const fournisseur = this.selectedFournisseur();
    if (!fournisseur) return;
    this.isLoadingLigneReglements.set(true);
    this.api.getReglementsBl(fournisseur.fournisseurId, ligne.commandeId).subscribe({
      next: res => {
        this.ligneReglements.set(res.body ?? []);
        this.isLoadingLigneReglements.set(false);
      },
      error: () => this.isLoadingLigneReglements.set(false)
    });
  }

  reglerLigne(ligne: ILigneFournisseurAP): void {
    this.selectedLigne.set(ligne);
    this.reglementForm.reset({
      montant: ligne.restantDu,
      dateReglement: TODAY_NGB_DATE(),
      modeReglement: "CH",
      reference: null,
      commentaire: null
    });
    this.activeTab.set("regler");
  }

  regleTout(): void {
    this.selectedLigne.set(null);
    this.reglementForm.patchValue({ montant: this.solde() });
    this.activeTab.set("regler");
  }

  selectFournisseur(compte: ICompteFournisseurAP): void {
    this.selectedFournisseur.set(compte);
    this.lignes.set([]);
    this.selectedLigne.set(null);
    this.filtreStatutLignes.set(null);
    this.totalLignes.set(0);
    this.activeTab.set("commandes");
    this.panelOpen.set(true);
    this.loadLignes(0);
  }

  loadLignes(page: number): void {
    const fournisseur = this.selectedFournisseur();
    if (!fournisseur) return;
    this.isLoadingLignes.set(true);
    this.api.getLignes(fournisseur.fournisseurId, page, this.PAGE_SIZE, this.filtreStatutLignes()).subscribe({
      next: res => {
        const body = res.body;
        this.lignes.set(body);
        this.totalLignes.set(Number(res.headers.get("X-Total-Count")) ?? 0);
        this.isLoadingLignes.set(false);
      },
      error: () => this.isLoadingLignes.set(false)
    });
  }

  onPage(event: AppTableLazyLoadEvent): void {
    const page = Math.floor(event.first / event.rows);
    this.loadLignes(page);
  }

  setFiltreStatut(statut: StatutLigne | null): void {
    this.filtreStatutLignes.set(this.filtreStatutLignes() === statut ? null : statut);
    this.loadLignes(0);
  }

  closeDetail(): void {
    this.panelOpen.set(false);
    this.activeTab.set("commandes");
    this.selectedFournisseur.set(null);
    this.selectedLigne.set(null);
  }

  // ── Règlement ──────────────────────────────────────────────────────────────
  saveReglement(): void {
    if (this.reglementForm.invalid || !this.validMontant()) return;
    const fournisseur = this.selectedFournisseur();
    if (!fournisseur) return;

    const val = this.reglementForm.getRawValue();
    const modeLabel = this.modeReglementOptions.find(o => o.value === val.modeReglement)?.label ?? val.modeReglement;
    const ligne = this.selectedLigne();
    const blInfo = ligne ? ` — BL ${ligne.numBon}` : "";
    const message = `Confirmer le règlement de ${this.formatCurrency(val.montant!)} FCFA par ${modeLabel}${blInfo} pour ${fournisseur.fournisseurName} ?`;

    this.confirmDialog.onConfirm(
      () => this.doSaveReglement(),
      "Confirmer le règlement",
      message
    );
  }

  private doSaveReglement(): void {
    const fournisseur = this.selectedFournisseur()!;
    const val = this.reglementForm.getRawValue();

    this.isSaving.set(true);
    this.api
      .enregistrerReglement(fournisseur.fournisseurId, {
        montant: val.montant!,
        dateReglement: NGB_DATE_TO_ISO(val.dateReglement ?? TODAY_NGB_DATE()),
        modeReglement: val.modeReglement!,
        reference: val.reference!,
        commentaire: val.commentaire ?? undefined,
        commandeId: this.selectedLigne()?.commandeId
      })
      .subscribe({
        next: () => {
          this.isSaving.set(false);
          this.notification.success("Règlement enregistré avec succès.");
          this.activeTab.set("commandes");
          this.selectedLigne.set(null);
          this.loadSummary();
          this.loadComptes();
          this.loadLignes(0);
        },
        error: () => {
          this.isSaving.set(false);
          this.notification.error("Erreur lors de l'enregistrement du règlement.");
        }
      });
  }

  // ── Export ──────────────────────────────────────────────────────────────────
  exportPdfGlobal(): void {
    this.isExporting.set(true);
    const from = this.fromDate() ? (NGB_DATE_TO_ISO(this.fromDate()!) ?? undefined) : undefined;
    const to = this.toDate() ? (NGB_DATE_TO_ISO(this.toDate()!) ?? undefined) : undefined;
    this.api.exportComptesAsPdf(from, to).subscribe({
      next: blob => {
        this.isExporting.set(false);
        this.blobDownload.downloadPdf(blob, 'comptes-fournisseurs');
      },
      error: () => {
        this.isExporting.set(false);
        this.notification.error("Erreur lors de la génération du PDF.");
      }
    });
  }

  exportPdfFournisseur(): void {
    const fournisseur = this.selectedFournisseur();
    if (!fournisseur) return;
    this.isExporting.set(true);
    this.api.exportFournisseurAsPdf(fournisseur.fournisseurId).subscribe({
      next: blob => {
        this.isExporting.set(false);
        this.blobDownload.downloadPdf(blob, `compte-${fournisseur.fournisseurCode}`);
      },
      error: () => {
        this.isExporting.set(false);
        this.notification.error("Erreur lors de la génération du PDF.");
      }
    });
  }


  // ── Hint ───────────────────────────────────────────────────────────────────
  dismissHint(): void {
    localStorage.setItem("ap-hint-dismissed", "1");
    this.showHint.set(false);
  }

  // ── Helpers ────────────────────────────────────────────────────────────────
  isSelected(c: ICompteFournisseurAP): boolean {
    return this.selectedFournisseur()?.fournisseurId === c.fournisseurId;
  }

  getFiltered(): ICompteFournisseurAP[] {
    const q = this.searchText().toLowerCase();
    if (!q) return this.comptes();
    return this.comptes().filter(
      c =>
        c.fournisseurName?.toLowerCase().includes(q) ||
        c.fournisseurCode?.toLowerCase().includes(q) ||
        c.phone?.includes(q) ||
        c.mobile?.includes(q)
    );
  }

  statutSeverity(statut: StatutFournisseur): "success" | "warn" | "danger" {
    if (statut === "A_JOUR") return "success";
    if (statut === "EN_RETARD") return "warn";
    return "danger";
  }

  statutLabel(statut: StatutFournisseur): string {
    if (statut === "A_JOUR") return "À jour";
    if (statut === "EN_RETARD") return "En retard";
    return "Critique";
  }

  statutTooltip(statut: StatutFournisseur): string {
    if (statut === "A_JOUR") return "Toutes les commandes sont dans les délais";
    if (statut === "EN_RETARD") return "Au moins une commande dépasse la date d'échéance";
    return "Commandes très en retard (délai critique dépassé)";
  }

  ligneSeverity(statut: StatutLigne): string {
    if (statut === "EN_RETARD") return "danger";
    if (statut === "PARTIEL") return "warn";
    if (statut === "REGLE") return "success";
    return "secondary";
  }

  ligneStatutLabel(statut: StatutLigne): string {
    switch (statut) {
      case "EN_ATTENTE": return "En attente";
      case "PARTIEL": return "Partiel";
      case "REGLE": return "Réglé";
      case "EN_RETARD": return "En retard";
    }
  }

  ligneStatutTooltip(statut: StatutLigne): string {
    switch (statut) {
      case "EN_ATTENTE": return "Commande reçue, paiement non encore effectué";
      case "PARTIEL": return "Paiement partiel enregistré — solde restant";
      case "REGLE": return "Commande intégralement réglée";
      case "EN_RETARD": return "Date d'échéance dépassée, aucun paiement enregistré";
    }
  }
}
