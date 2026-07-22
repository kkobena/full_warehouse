import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { forkJoin } from "rxjs";

import { Dci, IProduit, Produit } from "app/shared/model/produit.model";
import { ProduitService } from "app/entities/produit/produit.service";
import { TypeProduit } from "app/shared/model/enumerations/type-produit.model";
import { IFournisseur } from "app/shared/model/fournisseur.model";
import { IRayon } from "app/shared/model/rayon.model";
import { IFamilleProduit } from "app/shared/model/famille-produit.model";
import { ITva } from "app/shared/model/tva.model";
import { CodeRemise } from "app/shared/model/remise.model";
import { IFormProduit } from "app/shared/model/form-produit.model";
import { IGammeProduit } from "app/shared/model/gamme-produit.model";
import { ILaboratoire } from "app/shared/model/laboratoire.model";
import { ITiersPayant } from "app/shared/model/tierspayant.model";
import { RayonService } from "app/entities/rayon/rayon.service";
import { LaboratoireProduitService } from "app/entities/laboratoire-produit/laboratoire-produit.service";
import { FormeProduitService } from "app/entities/forme-produit/forme-produit.service";
import { FamilleProduitService } from "app/entities/famille-produit/famille-produit.service";
import { GammeProduitService } from "app/entities/gamme-produit/gamme-produit.service";
import { TvaService } from "app/entities/tva/tva.service";
import { RemiseService } from "app/entities/remise/remise.service";
import { DciService } from "app/entities/dci/dci.service";
import { TiersPayantService } from "app/entities/tiers-payant/tierspayant.service";
import { NgbModal, NgbNavModule, NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import {
  ButtonComponent,
  DataTableComponent,
  KeyFilterDirective,
  SelectComponent,
  SelectSearchComponent,
  SwitchComponent,
  ToolbarComponent,
} from "app/shared/ui";
import { CommonModule, NgClass } from "@angular/common";
import { STATUT_LEGAL_OPTIONS } from "app/shared/model/enumerations/statut-legal.model";
import { CLASSE_CRITICITE_OPTIONS } from "app/shared/model/enumerations/classe-criticite.model";
import { ProduitFournisseursTabComponent } from "../produit-fournisseurs-tab/produit-fournisseurs-tab.component";
import {
  ProduitFournisseursCreationComponent
} from "../produit-fournisseurs-creation/produit-fournisseurs-creation.component";
import { ProduitPrixCreationComponent } from "../produit-prix-creation/produit-prix-creation.component";
import { PrixReferenceService } from "../prix-reference/prix-reference.service";
import { PrixReference } from "../prix-reference/model/prix-reference.model";
import { AddPrixFormComponent } from "../prix-reference/add-prix-form/add-prix-form.component";
import { NgbConfirmDialogService } from "app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { NotificationService } from "../../../../shared/services/notification.service";
import { ErrorService } from "../../../../shared/error.service";
import { HttpErrorResponse } from "@angular/common/http";
import { FournisseurApiService } from "../../../partners/data-access/services/fournisseur-api.service";

@Component({
  selector: "app-produit-form",
  templateUrl: "./produit-form.component.html",
  styleUrl: "./produit-form.component.scss",
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    NgbNavModule,
    NgClass,
    ButtonComponent,
    DataTableComponent,
    KeyFilterDirective,
    SelectComponent,
    SelectSearchComponent,
    SwitchComponent,
    ToolbarComponent,
    ProduitFournisseursTabComponent,
    ProduitFournisseursCreationComponent,
    ProduitPrixCreationComponent,
    NgbTooltip
  ]
})
export class ProduitFormComponent implements OnInit {
  protected isSaving = false;
  protected isLoading = true;
  protected activeTab = "essentiel";

  /** Produit courant (signal) — alimenté après chargement, rechargé après refresh */
  protected readonly currentProduit = signal<IProduit | null>(null);

  // Listes de référence
  protected formeProduits: IFormProduit[] = [];
  protected familleProduits: IFamilleProduit[] = [];
  protected laboratoires: ILaboratoire[] = [];
  protected gammes: IGammeProduit[] = [];
  protected tvas: ITva[] = [];
  protected fournisseurs: IFournisseur[] = [];
  protected rayons: IRayon[] = [];
  protected remisesCodes: CodeRemise[] = [];
  protected dcis: Dci[] = [];

  // Prix de référence assurance
  protected prixReferences: PrixReference[] = [];
  protected isLoadingPrix = false;
  protected tiersPayants: ITiersPayant[] = [];

  // FormArrays création uniquement
  readonly fournisseursSupplementaires = new FormArray<FormGroup>([]);
  readonly prixReferenceCreationArray = new FormArray<FormGroup>([]);

  // Options statiques
  protected readonly statutLegalOptions = STATUT_LEGAL_OPTIONS;
  protected readonly classeCriticiteOptions = CLASSE_CRITICITE_OPTIONS;
  private readonly fb = inject(FormBuilder);
  private readonly produitService = inject(ProduitService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly rayonService = inject(RayonService);
  private readonly laboratoireService = inject(LaboratoireProduitService);
  private readonly formeProduitService = inject(FormeProduitService);
  private readonly fournisseurService = inject(FournisseurApiService);
  private readonly familleService = inject(FamilleProduitService);
  private readonly gammeProduitService = inject(GammeProduitService);
  private readonly tvaService = inject(TvaService);
  private readonly remiseService = inject(RemiseService);
  private readonly dciService = inject(DciService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly modalService = inject(NgbModal);
  private readonly prixReferenceService = inject(PrixReferenceService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  protected editForm = this.fb.group({
    id: this.fb.control<number | null>(null),

    // ── Onglet 1 : Essentiel ──────────────────────────────────────────────
    codeCip: this.fb.control<string | null>(null, [Validators.required]),
    fournisseurId: this.fb.control<number | null>(null, [Validators.required]),
    libelle: this.fb.control<string | null>(null, [Validators.required]),
    nomCommercial: this.fb.control<string | null>(null),
    costAmount: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
    regularUnitPrice: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)]),
    tvaId: this.fb.control<number | null>(null, [Validators.required]),
    familleId: this.fb.control<number | null>(null, [Validators.required]),
    rayonId: this.fb.control<number | null>(null, [Validators.required]),
    deconditionnable: this.fb.control<boolean>(false),
    itemQty: this.fb.control<number | null>(null),
    itemCostAmount: this.fb.control<number | null>(null),
    itemRegularUnitPrice: this.fb.control<number | null>(null),

    // ── Onglet 2 : Classification ─────────────────────────────────────────
    laboratoireId: this.fb.control<number | null>(null),
    formeId: this.fb.control<number | null>(null),
    gammeId: this.fb.control<number | null>(null),
    dciId: this.fb.control<number | null>(null),
    codeEanLaboratoire: this.fb.control<string | null>(null),

    // ── Onglet 3 : Réglementation ─────────────────────────────────────────
    statutLegal: this.fb.control<string | null>("SANS_LISTE"),
    gestionLot: this.fb.control<boolean>(false),
    thermosensible: this.fb.control<boolean>(false),
    remisable: this.fb.control<boolean>(false),
    remiseCode: this.fb.control<string | null>(null),
    estMedicamentEssentiel: this.fb.control<boolean>(false),
    estProduitGarde: this.fb.control<boolean>(false),

    // ── Onglet 4 : Approvisionnement ─────────────────────────────────────
    classeCriticite: this.fb.control<string | null>("B"),
    isClassificationOverridden: this.fb.control<boolean>(false),
    qtyAppro: this.fb.control<number | null>(null),
    qtySeuilMini: this.fb.control<number | null>(null),
    stockReassort: this.fb.control<number | null>(null),
    seuilMini: this.fb.control<number | null>(null),
    stockMaxi: this.fb.control<number | null>(null),
    // FormArrays rattachés pour propager leur validité au formulaire parent
    fournisseurRows: this.fournisseursSupplementaires,
    prixRows: this.prixReferenceCreationArray
  });

  // ─── Getters ─────────────────────────────────────────────────────────────

  get isEditMode(): boolean {
    return !!this.editForm.get("id")?.value;
  }

  get isPriceValid(): boolean {
    const cost = Number(this.editForm.get("costAmount")?.value ?? 0);
    const price = Number(this.editForm.get("regularUnitPrice")?.value ?? 0);
    return !cost || !price || cost < price;
  }

  get isItemPriceValid(): boolean {
    const cost = Number(this.editForm.get("itemCostAmount")?.value ?? 0);
    const price = Number(this.editForm.get("itemRegularUnitPrice")?.value ?? 0);
    return !cost || !price || cost < price;
  }

  get isDeconditionnable(): boolean {
    return this.editForm.get("deconditionnable")?.value === true;
  }

  get principalFournisseurLibelle(): string {
    const id = this.editForm.get("fournisseurId")?.value;
    return this.fournisseurs.find(f => f.id === id)?.libelle ?? "";
  }

  get principalCip(): string {
    return this.editForm.get("codeCip")?.value ?? "";
  }

  get principalPrixAchat(): number | null {
    return this.editForm.get("costAmount")?.value ?? null;
  }

  get principalPrixUni(): number | null {
    return this.editForm.get("regularUnitPrice")?.value ?? null;
  }

  get isRemisable(): boolean {
    return this.editForm.get("remisable")?.value === true;
  }

  get isEssentielValid(): boolean {
    const fields = ["codeCip", "fournisseurId", "libelle", "costAmount", "regularUnitPrice", "tvaId", "familleId", "rayonId"];
    return this.isPriceValid && fields.every(f => this.editForm.get(f)?.valid);
  }

  // ─── Cycle de vie ─────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ produit }) => {
      this.loadReferenceData(produit);
    });

    this.editForm.get("costAmount")?.valueChanges.subscribe(() => this.recalcItemPrices());
    this.editForm.get("regularUnitPrice")?.valueChanges.subscribe(() => this.recalcItemPrices());
    this.editForm.get("itemQty")?.valueChanges.subscribe(qty => this.recalcItemPricesFromQty(qty));
    this.editForm.get("deconditionnable")?.valueChanges.subscribe(checked => this.onDeconditionnable(!!checked));
    this.editForm.get("remisable")?.valueChanges.subscribe(checked => this.onRemisableChange(!!checked));
  }

  /** Mapping tabId → id du premier champ à focuser dans l'onglet */
  private readonly TAB_FIRST_FIELD: Record<string, string> = {
    essentiel: "f_codeCip",
    classification: "f_codeEanLabo",
    reglementation: "f_statutLegal",
    approvisionnement: "f_qtyAppro"
  };

  /** Focus le premier champ de l'onglet activé */
  protected onTabChange(tabId: string): void {
    this.activeTab = tabId;
    this.focusFirstField(tabId);
  }

  /** Focus le premier champ d'un onglet après rendu */
  private focusFirstField(tabId: string, delay = 250): void {
    const fieldId = this.TAB_FIRST_FIELD[tabId];
    if (!fieldId) return;
    setTimeout(() => {
      const el = document.getElementById(fieldId);
      if (!el) return;
      // Pour les éléments natifs (input, textarea, select)
      if (el instanceof HTMLInputElement || el instanceof HTMLTextAreaElement || el instanceof HTMLSelectElement) {
        el.focus();
      } else {
        // Pour les composants du Design System (app-select, etc.) : chercher le premier élément focusable enfant
        const focusable = el.querySelector<HTMLElement>("input, [tabindex]:not([tabindex=\"-1\"]), button, span[role=\"combobox\"]");
        if (focusable) {
          focusable.focus();
        } else {
          (el as HTMLElement).focus();
        }
      }
    }, delay);
  }

  // ─── Onglet Fournisseurs ────────────────────────────────────────────────

  protected onFournisseurRefresh(): void {
    const id = this.editForm.get("id")?.value;
    if (!id) return;
    this.produitService.find(id).subscribe(res => {
      if (res.body) {
        this.currentProduit.set(res.body);
      }
    });
  }

  // ─── Onglet Prix assurance ──────────────────────────────────────────────

  protected loadPrixReferences(): void {
    const id = this.editForm.get("id")?.value;
    if (!id) return;
    this.isLoadingPrix = true;
    this.prixReferenceService.query(id).subscribe({
      next: res => {
        this.prixReferences = res.body ?? [];
        this.isLoadingPrix = false;
      },
      error: () => {
        this.isLoadingPrix = false;
      }
    });
  }

  protected onAddPrix(): void {
    this.openPrixModal(null);
  }

  protected onEditPrix(prix: PrixReference): void {
    this.openPrixModal(prix);
  }

  protected onTogglePrix(prix: PrixReference): void {
    const msg = prix.enabled ? "Désactiver ce tarif ?" : "Activer ce tarif ?";
    this.confirmDialog.onConfirm(
      () => {
        prix.enabled = !prix.enabled;
        this.prixReferenceService.update(prix).subscribe(() => this.loadPrixReferences());
      },
      "Activation / Désactivation",
      msg
    );
  }

  protected onDeletePrix(prix: PrixReference): void {
    this.confirmDialog.onConfirm(
      () => this.prixReferenceService.delete(prix.id!).subscribe(() => this.loadPrixReferences()),
      "Suppression",
      "Supprimer ce tarif assurance ?"
    );
  }

  // ─── Méthodes form ────────────────────────────────────────────────────────

  protected isTabInvalid(fields: string[]): boolean {
    return fields.some(field => {
      const ctrl = this.editForm.get(field);
      return ctrl?.invalid && ctrl?.touched;
    });
  }

  protected onDeconditionnable(checked: boolean): void {
    if (checked) {
      this.editForm.get("itemQty")?.setValidators([Validators.required, Validators.min(1)]);
      this.editForm.get("itemCostAmount")?.setValidators([Validators.required, Validators.min(1)]);
      this.editForm.get("itemRegularUnitPrice")?.setValidators([Validators.required, Validators.min(1)]);
    } else {
      ["itemQty", "itemCostAmount", "itemRegularUnitPrice"].forEach(f => this.editForm.get(f)?.clearValidators());
      this.editForm.patchValue({ itemQty: null, itemCostAmount: null, itemRegularUnitPrice: null });
    }
    ["itemQty", "itemCostAmount", "itemRegularUnitPrice"].forEach(f =>
      this.editForm.get(f)?.updateValueAndValidity()
    );
  }

  protected onRemisableChange(checked: boolean): void {
    if (!checked) {
      this.editForm.get("remiseCode")?.setValue(null);
    }
  }

  protected previousState(): void {
    window.history.back();
  }

  protected save(): void {
    this.isSaving = true;
    const produit = this.buildProduit();
    const isCreating = !produit.id;
    const obs$ = produit.id ? this.produitService.update(produit) : this.produitService.create(produit);
    obs$.subscribe({
      next: (res) => {
        // Le backend retourne { id: <number> } pour la création
        const id: number | undefined = (res.body as any)?.id ?? produit.id ?? undefined;
        this.onSaveSuccess(produit, id, isCreating);
      },
      error: (err: HttpErrorResponse) => this.onSaveError(err)
    });
  }


  // ─── Chargement parallèle ─────────────────────────────────────────────────

  private loadReferenceData(produit: IProduit): void {
    forkJoin({
      tvas: this.tvaService.query({ page: 0, size: 9999 }),
      fournisseurs: this.fournisseurService.queryParents({ page: 0, size: 9999 }),
      rayons: this.rayonService.query({ page: 0, size: 9999 }),
      laboratoires: this.laboratoireService.query(),
      gammes: this.gammeProduitService.query({ page: 0, size: 9999 }),
      familles: this.familleService.query({ page: 0, size: 9999 }),
      formes: this.formeProduitService.query({ page: 0, size: 9999 }),
      remises: this.remiseService.queryCodes(),
      dcis: this.dciService.queryUnpaged(),
      tiersPayants: this.tiersPayantService.query({ page: 0, size: 9999, sort: ["fullName,asc"] })
    }).subscribe({
      next: data => {
        this.tvas = data.tvas.body ?? [];
        this.fournisseurs = data.fournisseurs.body ?? [];
        this.rayons = data.rayons.body ?? [];
        this.laboratoires = data.laboratoires.body ?? [];
        this.gammes = data.gammes.body ?? [];
        this.familleProduits = data.familles.body ?? [];
        this.formeProduits = data.formes.body ?? [];
        this.remisesCodes = data.remises.body ?? [];
        this.dcis = data.dcis.body ?? [];
        this.tiersPayants = data.tiersPayants.body ?? [];
        this.isLoading = false;
        this.updateForm(produit);
        this.focusFirstField(this.activeTab, 300);
        if (produit.id) {
          this.loadPrixReferences();
        }
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  private updateForm(produit: IProduit): void {
    /** Retourne null si la valeur est 0, null ou undefined — évite l'affichage de "0" dans les inputs numériques */
    const nz = (v: number | null | undefined): number | null => (v == null || v === 0 ? null : v);

    this.currentProduit.set(produit);
    this.editForm.patchValue({
      id: produit.id ?? null,
      codeCip: produit.codeCip ?? null,
      fournisseurId: produit.fournisseurId ?? null,
      libelle: produit.libelle ?? null,
      nomCommercial: (produit as any).nomCommercial ?? null,
      costAmount: nz(produit.costAmount),
      regularUnitPrice: nz(produit.regularUnitPrice),
      tvaId: produit.tvaId ?? null,
      familleId: produit.familleId ?? null,
      rayonId: produit.rayonId ?? null,
      deconditionnable: produit.deconditionnable ?? false,
      itemQty: nz(produit.itemQty),
      itemCostAmount: nz(produit.itemCostAmount),
      itemRegularUnitPrice: nz(produit.itemRegularUnitPrice),
      laboratoireId: produit.laboratoireId ?? null,
      formeId: produit.formeId ?? null,
      gammeId: produit.gammeId ?? null,
      dciId: produit.dciId ?? null,
      codeEanLaboratoire: produit.codeEanLaboratoire ?? null,
      statutLegal: produit.statutLegal ?? "SANS_LISTE",
      gestionLot: produit.gestionLot ?? true,
      thermosensible: (produit as any).thermosensible ?? false,
      remisable: (produit as any).remisable ?? false,
      remiseCode: produit.remiseCode ?? null,
      estMedicamentEssentiel: produit.estMedicamentEssentiel ?? false,
      estProduitGarde: produit.estProduitGarde ?? false,
      classeCriticite: produit.classeCriticite ?? "B",
      isClassificationOverridden: (produit as any).isClassificationOverridden ?? false,
      qtyAppro: nz(produit.qtyAppro),
      qtySeuilMini: nz(produit.qtySeuilMini),
      stockReassort: nz(produit.stockReassort),
      seuilMini: nz(produit.seuilMini),
      stockMaxi: nz(produit.stockMaxi)
    });
  }

  private openPrixModal(entity: PrixReference | null): void {
    const produit = this.currentProduit();
    if (!produit) return;
    const modalRef = this.modalService.open(AddPrixFormComponent, { size: "lg", centered: true, backdrop: "static" });
    modalRef.componentInstance.produit = produit;
    modalRef.componentInstance.isFromProduit = true;
    if (entity) {
      modalRef.componentInstance.entity = entity;
    }
    modalRef.result.then(
      () => this.loadPrixReferences(),
      () => {
      }
    );
  }

  private recalcItemPrices(): void {
    const qty = this.editForm.get("itemQty")?.value;
    if (qty && Number(qty) > 0) {
      this.recalcItemPricesFromQty(qty);
    }
  }

  private recalcItemPricesFromQty(qty: number | null): void {
    if (!qty || Number(qty) <= 0) {
      this.editForm.patchValue({ itemCostAmount: null, itemRegularUnitPrice: null }, { emitEvent: false });
      return;
    }
    const cost = Number(this.editForm.get("costAmount")?.value ?? 0);
    const price = Number(this.editForm.get("regularUnitPrice")?.value ?? 0);
    this.editForm.patchValue(
      {
        itemCostAmount: parseFloat((cost / qty).toFixed(2)),
        itemRegularUnitPrice: parseFloat((price / qty).toFixed(2))
      },
      { emitEvent: false }
    );
  }

  private buildProduit(): IProduit {
    const v = this.editForm.getRawValue();
    const base: IProduit = {
      ...new Produit(),
      id: v.id ?? undefined,
      libelle: v.libelle ?? undefined,
      codeCip: v.codeCip ?? undefined,
      fournisseurId: v.fournisseurId ?? undefined,
      costAmount: v.costAmount ?? undefined,
      regularUnitPrice: v.regularUnitPrice ?? undefined,
      tvaId: v.tvaId ?? undefined,
      familleId: v.familleId ?? undefined,
      rayonId: v.rayonId ?? undefined,
      deconditionnable: v.deconditionnable ?? false,
      itemQty: v.itemQty ?? undefined,
      itemCostAmount: v.itemCostAmount ?? undefined,
      itemRegularUnitPrice: v.itemRegularUnitPrice ?? undefined,
      laboratoireId: v.laboratoireId ?? undefined,
      formeId: v.formeId ?? undefined,
      gammeId: v.gammeId ?? undefined,
      dciId: v.dciId ?? undefined,
      codeEanLaboratoire: v.codeEanLaboratoire ?? undefined,
      statutLegal: v.statutLegal ?? undefined,
      gestionLot: v.gestionLot ?? true,
      remiseCode: v.remiseCode ?? undefined,
      estMedicamentEssentiel: v.estMedicamentEssentiel ?? false,
      estProduitGarde: v.estProduitGarde ?? false,
      classeCriticite: v.classeCriticite ?? undefined,
      qtyAppro: v.qtyAppro ?? undefined,
      qtySeuilMini: v.qtySeuilMini ?? undefined,
      stockReassort: v.stockReassort ?? undefined,
      seuilMini: v.seuilMini ?? undefined,
      stockMaxi: v.stockMaxi ?? undefined,
      typeProduit: TypeProduit.PACKAGE,
      ...({ nomCommercial: v.nomCommercial ?? undefined } as any),
      ...({ thermosensible: v.thermosensible ?? false } as any),
      ...({ remisable: v.remisable ?? false } as any),
      ...({ isClassificationOverridden: v.isClassificationOverridden ?? false } as any)
    };
    if (!this.isEditMode) {
      base.fournisseurProduits = this.fournisseursSupplementaires.value.map(fp => ({
        fournisseurId: fp.fournisseurId,
        codeCip: fp.codeCip,
        prixAchat: fp.prixAchat,
        prixUni: fp.prixUni,
        qteColis: fp.qteColis ?? 1,
        qteMinimaleCommande: fp.qteMinimaleCommande ?? 0
      }));
      base.prixReference = this.prixReferenceCreationArray.value.map(pr => ({
        tiersPayantId: pr.tiersPayantId,
        type: pr.type,
        price: pr.price ?? 0,
        rate: pr.rate ?? 0,
        enabled: pr.enabled ?? true
      }));
    }
    return base;
  }

  private onSaveSuccess(produit: IProduit, id: number | undefined, isCreating: boolean): void {
    this.isSaving = false;
    const msg = isCreating
      ? `Le produit "${produit.libelle}" a été créé avec succès.`
      : `Le produit "${produit.libelle}" a été modifié avec succès.`;
    const title = isCreating ? "Produit créé" : "Modification de produit";
    this.notificationService.success(msg, title);
    this.router.navigate(["/produits"], {
      state: { highlightId: id ?? produit.id, highlightCip: produit.codeCip }
    });
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.isSaving = false;
    this.notificationService.error(this.errorService.getErrorMessage(error), "Echec de création de produit");
  }
}
