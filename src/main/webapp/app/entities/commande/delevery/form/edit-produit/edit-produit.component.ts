import { Component, OnInit } from '@angular/core';
import { UntypedFormBuilder, Validators } from '@angular/forms';
import { IFournisseur } from '../../../../../shared/model/fournisseur.model';
import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { FournisseurService } from '../../../../fournisseur/fournisseur.service';
import { IProduit } from '../../../../../shared/model/produit.model';
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
  produit?: IProduit;
  isValid = true;
  disabledFournisseur = true;
  fournisseurPrduit: IFournisseurProduit | null;
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
    const produitFournisseur = this.createFromForm();
    if (produitFournisseur.id !== undefined && produitFournisseur.id) {
      this.subscribeToSaveResponse(this.produitService.updateProduitFournisseur(produitFournisseur));
    } else {
      this.subscribeToSaveResponse(this.produitService.createProduitFournisseur(produitFournisseur));
    }
  }

  ngOnInit(): void {
    this.produit = this.config.data.produit;
    this.deliveryItem = this.config.data.deliveryItem;

    if (this.deliveryItem) {
      this.produitService.findFournisseurProduit(this.deliveryItem.fournisseurProduitId).subscribe({
        next: (res: HttpResponse<IFournisseurProduit>) => {
          this.fournisseurPrduit = res.body;
          this.updateForm(this.fournisseurPrduit);
          this.populate();
        },
      });
    }
  }

  isEmpty(): boolean {
    const itemLength = this.produit?.fournisseurProduits?.length;
    if (itemLength) {
      return itemLength <= 0;
    }
    return true;
  }

  updateForm(produitFournisseur: IFournisseurProduit): void {
    this.editForm.patchValue({
      id: produitFournisseur.id,
      prixUni: produitFournisseur.prixUni,
      prixAchat: produitFournisseur.prixAchat,
      codeCip: produitFournisseur.codeCip,
      fournisseurId: produitFournisseur.fournisseurId,
      produitId: this.produit?.id,
    });
  }

  populate(): void {
    this.fournisseurService.query().subscribe((res: HttpResponse<IFournisseur[]>) => {
      this.fournisseurs = res.body || [];
    });
    this.typeEtiquetteService.query().subscribe((res: HttpResponse<ITypeEtiquette[]>) => {
      this.etiquettes = res.body || [];
    });
    this.tvaService.query().subscribe((res: HttpResponse<ITva[]>) => {
      this.tvas = res.body || [];
    });
    this.rayonService.query().subscribe((res: HttpResponse<IRayon[]>) => {
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
    if (costAmount >= value) {
      this.isValid = false;
    } else {
      this.isValid = true;
    }
  }

  cancel(): void {
    this.ref.close();
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IFournisseurProduit>>): void {
    result.subscribe({
      next: (res: HttpResponse<IFournisseurProduit>) => this.onSaveSuccess(res.body),
      error: error => this.onSaveError(error),
    });
  }

  protected onSaveSuccess(produitFournisseur: IFournisseurProduit | null): void {
    this.isSaving = false;
    this.ref.close(produitFournisseur);
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
      produitId: this.produit?.id,
    };
  }
}
