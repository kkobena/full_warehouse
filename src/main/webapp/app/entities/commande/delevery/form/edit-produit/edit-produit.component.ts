import { Component, OnInit, inject } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { DialogService, DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { FournisseurProduit, IFournisseurProduit } from '../../../../../shared/model/fournisseur-produit.model';
import { HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProduitService } from '../../../../produit/produit.service';
import { ErrorService } from '../../../../../shared/error.service';
import { ITva } from '../../../../../shared/model/tva.model';
import { IRayon } from '../../../../../shared/model/rayon.model';
import { ITypeEtiquette } from '../../../../../shared/model/type-etiquette.model';
import { IDeliveryItem } from '../../../../../shared/model/delivery-item';
import { RayonService } from '../../../../rayon/rayon.service';
import { TvaService } from '../../../../tva/tva.service';
import { TypeEtiquetteService } from '../../../../type-etiquette/type-etiquette.service';
import { IProduit } from '../../../../../shared/model/produit.model';
import { IDelivery } from '../../../../../shared/model/delevery.model';
import { WarehouseCommonModule } from '../../../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RouterModule } from '@angular/router';
import { RippleModule } from 'primeng/ripple';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { InputMaskModule } from 'primeng/inputmask';
import { ToastModule } from 'primeng/toast';
import { KeyFilterModule } from 'primeng/keyfilter';
import { Select } from 'primeng/select';

@Component({
  selector: 'jhi-edit-produit',
  templateUrl: './edit-produit.component.html',
  providers: [MessageService, DialogService, ConfirmationService],
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    RouterModule,
    RippleModule,
    DynamicDialogModule,
    FormsModule,
    ReactiveFormsModule,
    InputTextModule,
    DropdownModule,
    ToastModule,
    KeyFilterModule,
    InputMaskModule,
    Select,
  ],
})
export class EditProduitComponent implements OnInit {
  private fb = inject(UntypedFormBuilder);
  ref = inject(DynamicDialogRef);
  config = inject(DynamicDialogConfig);
  private messageService = inject(MessageService);
  protected produitService = inject(ProduitService);
  protected errorService = inject(ErrorService);
  protected rayonService = inject(RayonService);
  protected tvaService = inject(TvaService);
  protected typeEtiquetteService = inject(TypeEtiquetteService);

  appendTo = 'body';
  deliveryItem: IDeliveryItem | null;
  delivery: IDelivery | null;
  tvas: ITva[] = [];
  rayons: IRayon[] = [];
  etiquettes: ITypeEtiquette[] = [];
  isSaving = false;
  isValid = true;
  disabledFournisseur = false;
  fournisseurPrduit: IFournisseurProduit | null;
  produit: IProduit;
  editForm = this.fb.group({
    id: [],
    tvaId: [null, [Validators.required]],
    typeEtiquetteId: [null, [Validators.required]],
    codeCip: [null, [Validators.required, Validators.minLength(6), Validators.maxLength(8)]],
    rayonId: [null, [Validators.required]],
    prixAchat: [null, [Validators.required]],
    prixUni: [null, [Validators.required]],
    codeEan: [],
    expirationDate: [],
    cmuAmount: [],
    principal: [],
  });

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

  save(): void {
    this.isSaving = true;
    this.subscribeToSaveResponse(this.produitService.updateProduitFournisseurFromCommande(this.createFromForm()));
  }

  ngOnInit(): void {
    this.deliveryItem = this.config.data.deliveryItem;
    this.delivery = this.config.data.delivery;
    this.produitService.findFournisseurProduit(this.deliveryItem.fournisseurProduitId).subscribe({
      next: (res: HttpResponse<IFournisseurProduit>) => {
        this.fournisseurPrduit = res.body;
        this.produit = this.fournisseurPrduit.produit;
        this.updateForm(this.fournisseurPrduit);
        this.populate();
      },
    });
  }

  updateForm(produitFournisseur: IFournisseurProduit): void {
    const initialFormData = this.deliveryItem;
    this.editForm.patchValue({
      id: produitFournisseur.id,
      prixUni: initialFormData.orderUnitPrice,
      prixAchat: initialFormData.orderCostAmount,
      codeCip: initialFormData.fournisseurProduitCip,
      principal: produitFournisseur.principal,
      codeEan: this.produit.codeEan,
      expirationDate: this.produit.expirationDate,
      cmuAmount: this.produit.cmuAmount,
      tvaId: this.produit.tvaId,
      rayonId: this.produit.rayonId,
      typeEtiquetteId: this.produit.typeEtiquetteId,
    });
  }

  populate(): void {
    this.typeEtiquetteService.query().subscribe((res: HttpResponse<ITypeEtiquette[]>) => {
      this.etiquettes = res.body || [];
    });
    this.tvaService.query().subscribe((res: HttpResponse<ITva[]>) => {
      this.tvas = res.body || [];
    });
    this.rayonService
      .query({
        page: 0,
        size: 9999,
      })
      .subscribe((res: HttpResponse<IRayon[]>) => {
        this.rayons = res.body || [];
      });
  }

  handlePrixAchatInput(event: any): void {
    const value = Number(event.target.value);
    const unitPrice = Number(this.editForm.get(['prixUni'])!.value);
    this.isValid = value < unitPrice;
  }

  handlePrixUnitaireInput(event: any): void {
    const value = Number(event.target.value);
    const costAmount = Number(this.editForm.get(['prixAchat'])!.value);
    this.isValid = costAmount < value;
  }

  cancel(): void {
    this.ref.close();
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<{}>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: error => this.onSaveError(error),
    });
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
    this.ref.close({ success: true });
  }

  protected onSaveError(error: any): void {
    console.error(error.error.detail);
    this.isSaving = false;
    if (error.error?.errorKey) {
      this.errorService.getErrorMessageTranslation(error.error?.errorKey).subscribe({
        next: translatedErrorMessage => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: translatedErrorMessage,
          });
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: error.error.detail,
          });
        },
      });
    } else {
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: error.error.detail,
      });
    }
  }

  protected createFromForm(): IFournisseurProduit {
    return {
      ...new FournisseurProduit(),
      id: this.editForm.get(['id'])!.value,
      prixUni: this.editForm.get(['prixUni'])!.value,
      prixAchat: this.editForm.get(['prixAchat'])!.value,
      codeCip: this.editForm.get(['codeCip'])!.value,
      fournisseurId: this.delivery.fournisseurId,
      principal: this.editForm.get(['principal'])!.value,
      produitId: this.produit.id,
      produit: {
        codeEan: this.editForm.get(['codeEan'])!.value,
        expirationDate: this.editForm.get(['expirationDate'])!.value,
        cmuAmount: this.editForm.get(['cmuAmount'])!.value,
        tvaId: this.editForm.get(['tvaId'])!.value,
        rayonId: this.editForm.get(['rayonId'])!.value,
        typeEtiquetteId: this.editForm.get(['typeEtiquetteId'])!.value,
        id: this.produit.id,
      },
    };
  }
}
