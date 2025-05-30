import {Component, inject, OnInit} from '@angular/core';
import {FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators} from '@angular/forms';
import {DialogService, DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef} from 'primeng/dynamicdialog';
import {ConfirmationService, MessageService} from 'primeng/api';
import {FournisseurProduit, IFournisseurProduit} from '../../../../../shared/model/fournisseur-produit.model';
import {HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {ProduitService} from '../../../../produit/produit.service';
import {ErrorService} from '../../../../../shared/error.service';
import {ITva} from '../../../../../shared/model/tva.model';
import {IRayon} from '../../../../../shared/model/rayon.model';
import {RayonService} from '../../../../rayon/rayon.service';
import {TvaService} from '../../../../tva/tva.service';
import {IProduit} from '../../../../../shared/model/produit.model';
import {WarehouseCommonModule} from '../../../../../shared/warehouse-common/warehouse-common.module';
import {ButtonModule} from 'primeng/button';
import {RouterModule} from '@angular/router';
import {RippleModule} from 'primeng/ripple';
import {InputTextModule} from 'primeng/inputtext';
import {DropdownModule} from 'primeng/dropdown';
import {InputMaskModule} from 'primeng/inputmask';
import {ToastModule} from 'primeng/toast';
import {KeyFilterModule} from 'primeng/keyfilter';
import {Select} from 'primeng/select';
import {IOrderLine} from "../../../../../shared/model/order-line.model";
import {ICommande} from "../../../../../shared/model/commande.model";

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
  deliveryItem: IOrderLine | null;
  delivery: ICommande | null;
  protected fb = inject(UntypedFormBuilder);
  protected appendTo = 'body';

  protected tvas: ITva[] = [];
  protected rayons: IRayon[] = [];
  protected isSaving = false;
  protected isValid = true;
  protected fournisseurPrduit: IFournisseurProduit | null;
  protected produit: IProduit;
  protected editForm = this.fb.group({
    id: [],
    tvaId: [null, [Validators.required]],
    codeCip: [null, [Validators.required, Validators.minLength(6), Validators.maxLength(8)]],
    rayonId: [null, [Validators.required]],
    prixAchat: [null, [Validators.required]],
    prixUni: [null, [Validators.required]],
    codeEan: [],
    expirationDate: [],
    //    principal: [],
  });
  protected produitService = inject(ProduitService);
  protected errorService = inject(ErrorService);
  protected rayonService = inject(RayonService);
  protected tvaService = inject(TvaService);
  private readonly ref = inject(DynamicDialogRef);
  private readonly config = inject(DynamicDialogConfig);
  private readonly messageService = inject(MessageService);

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
      codeCip: initialFormData.produitCip,
      principal: produitFournisseur.principal,
      codeEan: this.produit.codeEan,
      expirationDate: this.produit.expirationDate,
      tvaId: this.produit.tvaId,
      rayonId: this.produit.rayonId,
    });
  }

  populate(): void {
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
    const unitPrice = Number(this.editForm.get(['prixUni']).value);
    this.isValid = value < unitPrice;
  }

  handlePrixUnitaireInput(event: any): void {
    const value = Number(event.target.value);
    const costAmount = Number(this.editForm.get(['prixAchat']).value);
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
    this.ref.close({success: true});
  }

  protected onSaveError(error: any): void {
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
      id: this.editForm.get(['id']).value,
      prixUni: this.editForm.get(['prixUni']).value,
      prixAchat: this.editForm.get(['prixAchat']).value,
      codeCip: this.editForm.get(['codeCip']).value,
      fournisseurId: this.delivery.fournisseurId,
      // principal: this.editForm.get(['principal']).value,
      produitId: this.produit.id,
      produit: {
        codeEan: this.editForm.get(['codeEan']).value,
        expirationDate: this.editForm.get(['expirationDate']).value,
        tvaId: this.editForm.get(['tvaId']).value,
        rayonId: this.editForm.get(['rayonId']).value,
        id: this.produit.id,
      },
    };
  }
}
