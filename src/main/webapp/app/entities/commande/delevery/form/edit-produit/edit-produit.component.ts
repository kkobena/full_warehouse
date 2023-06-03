import { Component, OnInit } from '@angular/core';
import { UntypedFormBuilder, Validators } from '@angular/forms';
import { IFournisseur } from '../../../../../shared/model/fournisseur.model';
import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { FournisseurService } from '../../../../fournisseur/fournisseur.service';
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

@Component({
  selector: 'jhi-edit-produit',
  templateUrl: './edit-produit.component.html',
  providers: [MessageService, DialogService, ConfirmationService],
})
export class EditProduitComponent implements OnInit {
  appendTo = 'body';
  deliveryItem: IDeliveryItem | null;
  tvas: ITva[] = [];
  rayons: IRayon[] = [];
  etiquettes: ITypeEtiquette[] = [];
  fournisseurs: IFournisseur[] = [];
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
    fournisseurId: [null, [Validators.required]],
    codeEan: [],
    expirationDate: [],
    cmuAmount: [],
    principal: [],
  });

  constructor(
    private fb: UntypedFormBuilder,
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
    private messageService: MessageService,
    private fournisseurService: FournisseurService,
    protected produitService: ProduitService,
    protected errorService: ErrorService,
    protected rayonService: RayonService,
    protected tvaService: TvaService,
    protected typeEtiquetteService: TypeEtiquetteService
  ) {}

  save(): void {
    this.isSaving = true;

    this.subscribeToSaveResponse(this.produitService.updateProduitFournisseurFromCommande(this.createFromForm()));
  }

  ngOnInit(): void {
    this.deliveryItem = this.config.data.deliveryItem;

    if (this.deliveryItem) {
      this.produitService.findFournisseurProduit(this.deliveryItem.fournisseurProduitId).subscribe({
        next: (res: HttpResponse<IFournisseurProduit>) => {
          this.fournisseurPrduit = res.body;
          this.produit = this.fournisseurPrduit.produit;
          this.updateForm(this.fournisseurPrduit);
          this.populate();
        },
      });
    }
  }

  updateForm(produitFournisseur: IFournisseurProduit): void {
    const initialFormData = this.deliveryItem;

    this.editForm.patchValue({
      id: produitFournisseur.id,
      prixUni: initialFormData.orderUnitPrice,
      prixAchat: initialFormData.orderCostAmount,
      codeCip: initialFormData.fournisseurProduitCip,
      fournisseurId: produitFournisseur.fournisseurId,
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
    this.fournisseurService
      .query({
        page: 0,
        size: 9999,
      })
      .subscribe((res: HttpResponse<IFournisseur[]>) => {
        this.fournisseurs = res.body || [];
      });
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

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IFournisseurProduit>>): void {
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
    this.isSaving = false;
    if (error.error) {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(translatedErrorMessage => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: translatedErrorMessage,
        });
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
      fournisseurId: this.editForm.get(['fournisseurId'])!.value,
      principal: this.editForm.get(['principal'])!.value,
      produitId: this.produit.id,
      produit: {
        codeEan: this.produit.codeEan,
        expirationDate: this.produit.expirationDate,
        cmuAmount: this.produit.cmuAmount,
        tvaId: this.produit.tvaId,
        rayonId: this.produit.rayonId,
        typeEtiquetteId: this.produit.typeEtiquetteId,
      },
    };
  }
}
