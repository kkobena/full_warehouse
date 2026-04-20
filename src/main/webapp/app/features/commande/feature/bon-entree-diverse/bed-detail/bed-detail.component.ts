import { Component, computed, DestroyRef, inject, input, OnInit, output, signal, viewChild } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { finalize, switchMap } from "rxjs/operators";
import { ButtonModule } from "primeng/button";
import { TooltipModule } from "primeng/tooltip";
import { InputTextModule } from "primeng/inputtext";
import { InputGroup } from "primeng/inputgroup";
import { InputGroupAddon } from "primeng/inputgroupaddon";
import { TagModule } from "primeng/tag";
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import {
  AllCommunityModule,
  CellValueChangedEvent,
  ClientSideRowModelModule,
  ColDef,
  GetRowIdFunc,
  GridReadyEvent,
  ModuleRegistry,
  themeAlpine
} from "ag-grid-community";
import { AgGridAngular } from "ag-grid-angular";
import { IBed, IBedLigne, MotifBed, MOTIFS_BED } from "../data-access/bed.model";
import { BedService } from "../data-access/bed.service";
import { BedLineActionsComponent } from "../ui/bed-line-actions/bed-line-actions.component";
import { BedValidateModalComponent, BedValidateResult } from "../ui/bed-header-form/bed-validate-modal.component";
import {
  CommandeProductSearchComponent
} from "app/features/commande/ui/commande-product-search/commande-product-search.component";
import { ProduitSearch } from "app/shared/model/produit.model";
import { NotificationService } from "app/shared/services/notification.service";
import { NgbConfirmDialogService } from "app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";

ModuleRegistry.registerModules([AllCommunityModule, ClientSideRowModelModule]);

@Component({
  selector: "app-bed-detail",
  templateUrl: "./bed-detail.component.html",
  styleUrls: ["./bed-detail.component.scss"],
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    TooltipModule,
    InputTextModule,
    InputGroup,
    InputGroupAddon,
    TagModule,
    AgGridAngular,
    CommandeProductSearchComponent
  ]
})
export class BedDetailComponent implements OnInit {
  readonly bed = input.required<IBed>();
  readonly retour = output<void>();
  readonly bedChange = output<IBed>();

  readonly isBrouillon = computed(() => !this.bed()?.id || this.bed()?.orderStatus === "REQUESTED");
  readonly isNew = computed(() => !this.bed()?.id);
  readonly lignes = computed(() => this.bed()?.lignes ?? []);
  readonly totalGross = computed(() =>
    this.lignes().reduce((s, l) => s + (l.quantite ?? 0) * (l.prixAchat ?? 0), 0)
  );

  // ── AG Grid ────────────────────────────────────────────────────────────────
  readonly theme = themeAlpine;
  protected readonly gridContext: { componentParent: BedDetailComponent } = { componentParent: this };

  protected readonly defaultColDef: ColDef<IBedLigne> = {
    resizable: true,
    sortable: false,
    suppressHeaderMenuButton: true
  };

  protected readonly getRowId: GetRowIdFunc<IBedLigne> = p => String(p.data.id);

  protected get columnDefs(): ColDef<IBedLigne>[] {
    const brouillon = this.isBrouillon();
    return [
      {
        field: "codeCip",
        headerName: "Code",
        width: 120,
        cellStyle: { fontFamily: "monospace", fontSize: "12px" }
      },
      {
        field: "produitLibelle",
        headerName: "Produit",
        flex: 2,
        minWidth: 150
      },
      {
        field: "quantite",
        headerName: "Qté",
        width: 90,
        type: "numericColumn",
        editable: () => brouillon,
        cellEditor: "agNumberCellEditor"
      },
      {
        field: "prixAchat",
        headerName: "P.A (F)",
        width: 120,
        type: "numericColumn",
        editable: () => brouillon,
        cellEditor: "agNumberCellEditor",
        valueFormatter: p => (p.value != null ? Number(p.value).toLocaleString("fr-FR") : "—")
      },
      {
        colId: "total",
        headerName: "Total (F)",
        width: 130,
        type: "numericColumn",
        valueGetter: p => (p.data?.quantite ?? 0) * (p.data?.prixAchat ?? 0),
        valueFormatter: p => (p.value != null ? Number(p.value).toLocaleString("fr-FR") : "—")
      },
      {
        colId: "actions",
        headerName: "",
        width: 70,
        sortable: false,
        cellRenderer: BedLineActionsComponent
      }
    ];
  }

  // ── Ajout produit ──────────────────────────────────────────────────────────
  readonly selectedProduit = signal<ProduitSearch | null>(null);
  readonly addingLigne = signal(false);
  protected ligneQuantite = 1;

  readonly productSearch = viewChild<CommandeProductSearchComponent>("productSearch");

  private readonly bedService = inject(BedService);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly modalService = inject(NgbModal);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  getMotifLabel(motif: MotifBed | undefined): string {
    return MOTIFS_BED.find(m => m.value === motif)?.label ?? motif ?? "—";
  }

  getStatutLabel(statut: string | undefined): string {
    return statut === "CLOSED" ? "Validé" : statut === "REQUESTED" ? "En cours" : (statut ?? "");
  }

  getStatutSeverity(statut: string | undefined): "success" | "warn" | "secondary" {
    return statut === "CLOSED" ? "success" : statut === "REQUESTED" ? "warn" : "secondary";
  }

  // ── Produit sélectionné ────────────────────────────────────────────────────

  onProductSelected(produit: ProduitSearch | null): void {
    this.selectedProduit.set(produit);
  }

  onProductScanned(produit: ProduitSearch): void {
    this.onProductSelected(produit);
  }

  // ── Validation ─────────────────────────────────────────────────────────────

  onValider(): void {
    const bed = this.bed();
    if (!bed?.id) return;
    const modalRef = this.modalService.open(BedValidateModalComponent, { centered: true, size: "lg", backdrop: 'static' });
    const instance = modalRef.componentInstance as BedValidateModalComponent;
    instance.bed = bed;
    instance.formMotif.set(bed.motifBed ?? null);
    modalRef.result.then(
      (result: BedValidateResult) => {
        this.bedService
          .validate(bed.id!, { orderDate: bed.orderDate!, motif: result.motif, fournisseurId: result.fournisseurId, commentaire: result.commentaire })
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: validated => {
              this.bedChange.emit(validated);
              this.notificationService.success(`${validated.receiptReference} validé — stock crédité`, "Succès");
            },
            error: () => this.notificationService.error("Erreur lors de la validation", "Erreur")
          });
      },
      () => {
      }
    );
  }

  // ── Ajout ligne (crée le BED au premier ajout) ─────────────────────────────

  onAjouterLigne(): void {
    const produit = this.selectedProduit();
    if (!produit || this.ligneQuantite <= 0) {
      this.notificationService.error("Sélectionnez un produit et une quantité valide", "Validation");
      return;
    }
    const fp = produit.fournisseurProduit;
    if (!fp) {
      this.notificationService.error("Ce produit n'a pas de fournisseur principal configuré", "Erreur");
      return;
    }

    const ligne: IBedLigne = {
      fournisseurProduitId: fp.id,
      quantite: this.ligneQuantite,
      prixAchat: fp.prixAchat ?? 0,
      prixVente: fp.prixUni
    };

    this.addingLigne.set(true);

    if (!this.bed()?.id) {
      this.bedService
        .create({})
        .pipe(
          switchMap(created => {
            this.bedChange.emit(created);
            return this.bedService.addLigne(created.id!, created.orderDate!, ligne);
          }),
          finalize(() => this.addingLigne.set(false)),
          takeUntilDestroyed(this.destroyRef)
        )
        .subscribe({
          next: updated => {
            this.bedChange.emit(updated);
            this.resetAddForm();
          },
          error: () => this.notificationService.error("Erreur lors de l'ajout de la ligne", "Erreur")
        });
    } else {
      const bed = this.bed();
      this.bedService
        .addLigne(bed.id!, bed.orderDate!, ligne)
        .pipe(
          finalize(() => this.addingLigne.set(false)),
          takeUntilDestroyed(this.destroyRef)
        )
        .subscribe({
          next: updated => {
            this.bedChange.emit(updated);
            this.resetAddForm();
          },
          error: () => this.notificationService.error("Erreur lors de l'ajout de la ligne", "Erreur")
        });
    }
  }

  private resetAddForm(): void {
    this.selectedProduit.set(null);
    this.ligneQuantite = 1;
    this.productSearch()?.reset();
  }

  // ── AG Grid ────────────────────────────────────────────────────────────────

  onGridReady(_e: GridReadyEvent<IBedLigne>): void {
  }

  onCellValueChanged(e: CellValueChangedEvent<IBedLigne>): void {
    const bed = this.bed();
    const ligne = e.data;
    if (!bed?.id || !ligne.id) return;
    const val = Number(e.newValue);
    if (e.colDef.field === "quantite" && val <= 0) return;
    if (e.colDef.field === "prixAchat" && val < 0) return;

    const dto: IBedLigne = { ...ligne, [e.colDef.field!]: val };
    this.bedService
      .updateLigne(bed.id!, bed.orderDate!, ligne.id!, ligne.orderDate!, dto)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => this.bedChange.emit(updated),
        error: () => this.notificationService.error("Erreur lors de la mise à jour", "Erreur")
      });
  }

  // ── Suppression ligne ──────────────────────────────────────────────────────

  confirmDeleteLigne(ligne: IBedLigne): void {
    const bed = this.bed();
    if (!bed?.id) return;
    this.confirmDialog.onConfirm(
      () => {
        this.bedService
          .removeLigne(bed.id!, bed.orderDate!, ligne.id!, ligne.orderDate!)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              const updated: IBed = {
                ...bed,
                lignes: (bed.lignes ?? []).filter(l => l.id !== ligne.id),
                grossAmount: (bed.grossAmount ?? 0) - (ligne.quantite ?? 0) * (ligne.prixAchat ?? 0)
              };
              this.bedChange.emit(updated);
            },
            error: () => this.notificationService.error("Erreur lors de la suppression", "Erreur")
          });
      },
      "Supprimer la ligne",
      `Supprimer la ligne "${ligne.produitLibelle}" du BED ?`
    );
  }
}
