import { AfterViewInit, Component, DestroyRef, inject, OnInit } from "@angular/core";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { ButtonModule } from "primeng/button";
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from "@angular/forms";
import { PrixReference } from "../model/prix-reference.model";
import { PrixReferenceService } from "../prix-reference.service";
import { Observable } from "rxjs";
import { HttpErrorResponse, HttpResponse } from "@angular/common/http";
import { Select } from "primeng/select";
import { InputNumber } from "primeng/inputnumber";
import { ToggleSwitch } from "primeng/toggleswitch";
import { finalize } from "rxjs/operators";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { NgbConfirmDialogService } from "../../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { NotificationService } from "../../../../../shared/services/notification.service";
import { IProduit, ITiersPayant } from "../../../../../shared/model";
import { ErrorService } from "../../../../../shared/error.service";
import { TiersPayantService } from "../../../../../entities/tiers-payant/tierspayant.service";
import { ProduitService } from "../../../../../entities/produit/produit.service";
import { formatNumber } from "../../../../../shared/utils/format-utils";

const PriceTypes = {
  REFERENCE: "REFERENCE",
  POURCENTAGE: "POURCENTAGE",
  MIXED_REFERENCE_POURCENTAGE: "MIXED_REFERENCE_POURCENTAGE"
} as const;

type PriceType = (typeof PriceTypes)[keyof typeof PriceTypes];

@Component({
  selector: "app-add-prix-form",
  imports: [ButtonModule, ReactiveFormsModule, Select, InputNumber, ToggleSwitch],
  templateUrl: "./add-prix-form.component.html",
  styleUrls: ["add-prix-form.scss"]
})
export class AddPrixFormComponent implements OnInit, AfterViewInit {
  // Component Inputs
  produit: IProduit | null = null;
  tiersPayant: ITiersPayant | null = null;
  entity: PrixReference | null = null;
  isFromProduit = true;

  // Template accessible properties
  protected isSaving = false;
  protected tiersPayants: ITiersPayant[] = [];
  protected produits: IProduit[] = [];
  protected pricesType: { code: PriceType; libelle: string }[] = [
    {
      code: PriceTypes.REFERENCE,
      libelle: "Prix de référence assurance"
    },
    { code: PriceTypes.POURCENTAGE, libelle: "Pourcentage appliqué par l'assureur" },
    { code: PriceTypes.MIXED_REFERENCE_POURCENTAGE, libelle: "Pourcentage appliqué au prix de référence" }
  ];

  // Form definition
  protected editForm = inject(FormBuilder).group({
    id: new FormControl<number | null>(null),
    price: new FormControl<number | null>(null),
    rate: new FormControl<number | null>(null),
    tiersPayantId: new FormControl<number | null>(null),
    produitId: new FormControl<number | null>(null),
    type: new FormControl<PriceType | null>(PriceTypes.REFERENCE, {
      validators: [Validators.required],
      nonNullable: true
    }),
    enabled: new FormControl<boolean | null>(true, {
      validators: [Validators.required],
      nonNullable: true
    })
  });

  // Injected services
  private readonly activeModal = inject(NgbActiveModal);
  private readonly entityService = inject(PrixReferenceService);
  private readonly errorService = inject(ErrorService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly produitService = inject(ProduitService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);

  ngOnInit(): void {
    this.loadInitialData();
    if (this.entity) {
      this.updateForm(this.entity);
    }
    this.setupConditionalValidators();
  }

  ngAfterViewInit(): void {
    this.subscribeToTypeChanges();
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  protected save(): void {
    if (this.editForm.invalid) {
      this.notificationService.error("Formulaire invalide", "Erreur");
      return;
    }

    this.isSaving = true;
    const prixReference = this.createFromForm();
    if (prixReference.type !== PriceTypes.POURCENTAGE && prixReference.price > this.produit?.regularUnitPrice) {
      const message = `Le prix que vous avez saisi  <span class="fs-4 fw-semibold text-danger"> (${formatNumber(prixReference.price)})</span> est supérieur au prix de vente au public <span class="fs-4 fw-semibold text-success">(${formatNumber(this.produit?.regularUnitPrice)})</span>. Voulez-vous continuer ?`;

      this.confirmDialog.onConfirm(
        () => this.onConfirmSave(prixReference),
        "Confirmation",
        message
      );
    } else {
      this.onConfirmSave(prixReference);
    }
  }

  private onConfirmSave(prixReference: PrixReference): void {
    const saveObservable = prixReference.id ? this.entityService.update(prixReference) : this.entityService.create(prixReference);
    this.subscribeToSaveResponse(saveObservable);
  }

  protected updateForm(entity: PrixReference): void {
    this.editForm.patchValue({
      id: entity.id,
      type: entity.type as PriceType,
      tiersPayantId: entity.tiersPayantId,
      price: entity.price,
      produitId: entity.produitId,
      enabled: entity.enabled
    });
  }

  private createFromForm(): PrixReference {
    return {
      ...new PrixReference(),
      ...this.editForm.getRawValue()
    };
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<{}>>): void {
    result.pipe(finalize(() => (this.isSaving = false))).subscribe({
      next: () => this.onSaveSuccess(),
      error: (err: any) => this.onSaveError(err)
    });
  }

  private onSaveSuccess(): void {
    this.activeModal.close("saved");
  }

  private onSaveError(err: HttpErrorResponse): void {
    this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur");
  }

  protected get shouldShowRateControl(): boolean {
    const type = this.editForm.get("type")?.value;
    return type === PriceTypes.POURCENTAGE || type === PriceTypes.MIXED_REFERENCE_POURCENTAGE;
  }

  protected get shouldShowPriceControl(): boolean {
    const type = this.editForm.get("type")?.value;
    return type !== PriceTypes.POURCENTAGE;
  }

  private loadInitialData(): void {
    if (this.isFromProduit) {
      this.getTiersPayants();
    } else {
      this.getProduits();
    }
  }

  private setupConditionalValidators(): void {
    if (this.isFromProduit) {
      this.editForm.get("tiersPayantId")?.setValidators([Validators.required]);
      this.editForm.get("produitId")?.setValue(this.produit?.id ?? null);
    } else {
      this.editForm.get("produitId")?.setValidators([Validators.required]);
      this.editForm.get("tiersPayantId")?.setValue(this.tiersPayant?.id ?? null);
    }
    this.editForm.updateValueAndValidity();
  }

  private subscribeToTypeChanges(): void {
    this.editForm
      .get("type")
      ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(value => {
        this.updateValidatorsBasedOnType(value);
      });
    this.editForm
      .get("tiersPayantId")
      ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(value => {
        this.updateValidatorsBasedOnType(this.editForm.get("type").value);
      });
  }

  private updateValidatorsBasedOnType(type: PriceType | null): void {
    const priceControl = this.editForm.get("price");
    const rateControl = this.editForm.get("rate");

    /* if (!priceControl || !rateControl) {
       return;
     }*/

    priceControl?.clearValidators();
    rateControl?.clearValidators();

    if (type === PriceTypes.POURCENTAGE || type === PriceTypes.MIXED_REFERENCE_POURCENTAGE) {
      rateControl.setValidators([Validators.required, Validators.min(0), Validators.max(100)]);
    }
    if (type !== PriceTypes.POURCENTAGE) {
      priceControl.setValidators([Validators.required]);
    }

    priceControl.updateValueAndValidity();
    rateControl.updateValueAndValidity();
  }

  private getTiersPayants(): void {
    this.tiersPayantService
      .query({
        page: 0,
        size: 9999,
        sort: ["fullName,asc"]
      })
      .subscribe({
        next: (res: HttpResponse<ITiersPayant[]>) => {
          this.tiersPayants = res.body || [];
        },
        error: (err: HttpErrorResponse) => this.onSaveError(err)
      });
  }

  private getProduits(): void {
    this.produitService
      .query({
        page: 0,
        size: 99999,
        sort: ["libelle,asc"]
      })
      .subscribe({
        next: (res: HttpResponse<IProduit[]>) => {
          this.produits = res.body || [];
        },
        error: (err: HttpErrorResponse) => this.onSaveError(err)
      });
  }
}
