import { AfterViewInit, Component, inject, OnInit, viewChild, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from "@angular/forms";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { HttpErrorResponse } from "@angular/common/http";
import { InputNumber, InputNumberModule } from "primeng/inputnumber";
import { InputTextModule } from "primeng/inputtext";
import { ButtonModule } from "primeng/button";
import { IProduit, Produit } from "app/shared/model/produit.model";
import { TypeProduit } from "app/shared/model/enumerations/type-produit.model";
import { ErrorService } from "app/shared/error.service";
import { ProductsApiService } from "../../data-access/services/products-api.service";
import { NotificationService } from "../../../../shared/services/notification.service";

@Component({
  selector: "app-produit-detail-form-modal",
  templateUrl: "./produit-detail-form-modal.component.html",
  styleUrls: ["./produit-detail-form-modal.component.scss"],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    InputNumberModule,
    InputTextModule,
    ButtonModule
  ]
})
export class ProduitDetailFormModalComponent implements OnInit, AfterViewInit {
  /** Parent product (required) */
  produit!: IProduit;
  /** Existing child product — set for edit mode */
  entity?: IProduit;

  protected isSaving = false;

  private readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(ProductsApiService);
  private readonly fb = inject(FormBuilder);
  private readonly errorService = inject(ErrorService);
  private readonly itemQtyInput = viewChild.required<InputNumber>("itemQty");
  private readonly notificationService = inject(NotificationService);
  protected editForm = this.fb.group({
    id: [null as number | null],
    libelle: [null as string | null, Validators.required],
    itemQty: [null as number | null, [Validators.required, Validators.min(2)]],
    costAmount: [null as number | null, [Validators.required, Validators.min(0)]],
    regularUnitPrice: [null as number | null, [Validators.required, Validators.min(0)]]
  });

  ngOnInit(): void {
    if (this.entity) {
      this.editForm.patchValue({
        id: this.entity.id ?? null,
        libelle: this.entity.libelle ?? null,
        costAmount: this.entity.costAmount ?? null,
        regularUnitPrice: this.entity.regularUnitPrice ?? null,
        itemQty: this.produit?.itemQty ?? null
      });
    } else {
      this.editForm.patchValue({
        libelle: (this.produit.libelle ?? "") + "-DET",
        costAmount: this.produit.itemCostAmount ?? null,
        regularUnitPrice: this.produit.itemRegularUnitPrice ?? null,
        itemQty: this.produit.itemQty ?? null
      });
      this.recalcPrices(this.produit.itemQty ?? null);
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.itemQtyInput().input()?.nativeElement.focus(), 100);

    this.editForm.get("itemQty")!.valueChanges.subscribe(val => this.recalcPrices(val));
  }

  protected save(): void {
    if (this.editForm.invalid) return;
    this.isSaving = true;
    const produit = this.buildFromForm();
    const request = produit.id != null
      ? this.api.updateProduitDetail(produit)
      : this.api.createProduitDetail(produit);
    request.subscribe({
      next: () => this.onSaveSuccess(),
      error: (err: HttpErrorResponse) => this.onSaveError(err)
    });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  private recalcPrices(qty: number | null): void {
    const itemQty = qty ?? this.produit.itemQty;
    if (Number(itemQty) > 0) {
      this.editForm.get("costAmount")!.setValue(
        Number((this.produit.costAmount! / itemQty!).toFixed()), { emitEvent: false }
      );
      this.editForm.get("regularUnitPrice")!.setValue(
        Number((this.produit.regularUnitPrice! / itemQty!).toFixed()), { emitEvent: false }
      );
    } else {
      this.editForm.get("costAmount")!.setValue(null, { emitEvent: false });
      this.editForm.get("regularUnitPrice")!.setValue(null, { emitEvent: false });
    }
  }

  private buildFromForm(): IProduit {
    return {
      ...new Produit(),
      id: this.editForm.get("id")!.value ?? undefined,
      libelle: this.editForm.get("libelle")!.value!,
      itemQty: this.editForm.get("itemQty")!.value!,
      costAmount: this.editForm.get("costAmount")!.value!,
      regularUnitPrice: this.editForm.get("regularUnitPrice")!.value!,
      produitId: this.produit.id,
      typeProduit: TypeProduit.DETAIL,
      quantity: 0,
      netUnitPrice: 0,
      itemCostAmount: 0,
      itemRegularUnitPrice: 0
    };
  }

  private onSaveSuccess(): void {
    this.isSaving = false;
    this.activeModal.close("saved");
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.isSaving = false;
    this.notificationService.error(this.errorService.getErrorMessage(error), "Erreur de decondition");
  }
}
