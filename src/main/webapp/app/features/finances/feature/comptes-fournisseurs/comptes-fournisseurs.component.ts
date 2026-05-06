import { Component, computed, DestroyRef, inject, OnInit, signal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { CommonModule } from "@angular/common";
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from "@angular/forms";
import { NgbNavModule } from "@ng-bootstrap/ng-bootstrap";
import { ButtonModule } from "primeng/button";
import { TableModule } from "primeng/table";
import { ToolbarModule } from "primeng/toolbar";
import { InputTextModule } from "primeng/inputtext";
import { TooltipModule } from "primeng/tooltip";
import { TagModule } from "primeng/tag";
import { DatePicker } from "primeng/datepicker";
import { InputNumber } from "primeng/inputnumber";
import { FloatLabel } from "primeng/floatlabel";
import { SelectModule } from "primeng/select";
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
import { DATE_FORMAT_ISO_DATE } from "app/shared/util/warehouse-util";
import { IconField } from "primeng/iconfield";
import { InputIcon } from "primeng/inputicon";

@Component({
  selector: "app-comptes-fournisseurs",
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    NgbNavModule,
    ButtonModule,
    TableModule,
    ToolbarModule,
    InputTextModule,
    TooltipModule,
    TagModule,
    DatePicker,
    InputNumber,
    FloatLabel,
    SelectModule,
    IconField,
    InputIcon
  ],
  templateUrl: "./comptes-fournisseurs.component.html",
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

  panelOpen = signal(false);
  activeTab = signal<string>("commandes");
  searchText = signal("");

  selectedLigne = signal<ILigneFournisseurAP | null>(null);
  ligneReglements = signal<IReglementBL[]>([]);
  isLoadingLigneReglements = signal(false);
  filtreStatutLignes = signal<StatutLigne | null>(null);
  totalLignes = signal(0);
  readonly PAGE_SIZE = 10;

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
  ];

  readonly reglementForm = inject(FormBuilder).group({
    montant: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(1)],
      nonNullable: true
    }),
    dateReglement: new FormControl<Date | null>(new Date()),
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

  constructor() {
    // Signal Forms — synchronise le signal depuis le contrôle montant
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
    this.api.getComptes().subscribe({
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
        dateReglement: new Date(),
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
      dateReglement: new Date(),
      modeReglement: "CASH",
      reference: null,
      commentaire: null
    });
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

  onPage(event: { first: number; rows: number }): void {
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
    this.isSaving.set(true);
    this.api
      .enregistrerReglement(fournisseur.fournisseurId, {
        montant: val.montant!,
        dateReglement: DATE_FORMAT_ISO_DATE(val.dateReglement ?? new Date()),
        modeReglement: val.modeReglement!,
        reference: val.reference!,
        commentaire: val.commentaire ?? undefined
      })
      .subscribe({
        next: () => {
          this.isSaving.set(false);
          this.activeTab.set("commandes");
          this.loadSummary();
          this.loadComptes();
          this.loadLignes(0);
        },
        error: () => this.isSaving.set(false)
      });
  }

  // ── Hint ───────────────────────────────────────────────────────────────────
  dismissHint(): void {
    localStorage.setItem("ap-hint-dismissed", "1");
    this.showHint.set(false);
  }

  // ── Helpers ────────────────────────────────────────────────────────────────
  getFiltered(): ICompteFournisseurAP[] {
    const q = this.searchText().toLowerCase();
    if (!q) return this.comptes();
    return this.comptes().filter(
      c =>
        c.fournisseurName?.toLowerCase().includes(q) ||
        c.fournisseurCode?.toLowerCase().includes(q) ||
        c.phone?.includes(q)
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

  ligneSeverity(statut: StatutLigne): string {
    if (statut === "EN_RETARD") return "danger";
    if (statut === "PARTIEL") return "warn";
    if (statut === "REGLE") return "success";
    return "secondary";
  }

  ligneStatutLabel(statut: StatutLigne): string {
    switch (statut) {
      case "EN_ATTENTE":
        return "En attente";
      case "PARTIEL":
        return "Partiel";
      case "REGLE":
        return "Réglé";
      case "EN_RETARD":
        return "En retard";
    }
  }
}
