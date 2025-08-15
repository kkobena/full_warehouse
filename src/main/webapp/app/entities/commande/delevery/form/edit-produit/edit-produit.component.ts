import { Component, inject, OnInit, viewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { FournisseurProduit, IFournisseurProduit } from '../../../../../shared/model/fournisseur-produit.model';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { ProduitService } from '../../../../produit/produit.service';
import { ErrorService } from '../../../../../shared/error.service';
import { ITva } from '../../../../../shared/model/tva.model';
import { IRayon } from '../../../../../shared/model/rayon.model';
import { RayonService } from '../../../../rayon/rayon.service';
import { TvaService } from '../../../../tva/tva.service';
import { IProduit } from '../../../../../shared/model/produit.model';
import { WarehouseCommonModule } from '../../../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RouterModule } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { InputMaskModule } from 'primeng/inputmask';
import { KeyFilterModule } from 'primeng/keyfilter';
import { Select } from 'primeng/select';
import { IOrderLine } from '../../../../../shared/model/order-line.model';
import { ICommande } from '../../../../../shared/model/commande.model';
import { ToastAlertComponent } from '../../../../../shared/toast-alert/toast-alert.component';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Card } from 'primeng/card';

@Component({
  selector: 'jhi-edit-produit',
  templateUrl: './edit-produit.component.html',
  styleUrls: ['../../../../common-modal.component.scss'],
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    InputTextModule,
    KeyFilterModule,
    InputMaskModule,
    Select,
    ToastAlertComponent,
    Card
  ]
})
export class EditProduitComponent implements OnInit {
  deliveryItem: IOrderLine | null;
  delivery: ICommande | null;
  header: string | null = null;
  protected fb = inject(UntypedFormBuilder);
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
    expirationDate: []
    //    principal: [],
  });
  private readonly produitService = inject(ProduitService);
  private readonly rayonService = inject(RayonService);
  private readonly tvaService = inject(TvaService);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);
  private readonly activeModal = inject(NgbActiveModal);

  save(): void {
    this.isSaving = true;
    this.subscribeToSaveResponse(this.produitService.updateProduitFournisseurFromCommande(this.createFromForm()));
  }

  ngOnInit(): void {

    this.produitService.findFournisseurProduit(this.deliveryItem.fournisseurProduitId).subscribe({
      next: (res: HttpResponse<IFournisseurProduit>) => {
        this.fournisseurPrduit = res.body;
        this.produit = this.fournisseurPrduit.produit;
        this.updateForm(this.fournisseurPrduit);
        this.populate();
      }
    });
  }

  protected updateForm(produitFournisseur: IFournisseurProduit): void {
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
      rayonId: this.produit.rayonId
    });
  }

  protected populate(): void {
    this.tvaService.query().subscribe((res: HttpResponse<ITva[]>) => {
      this.tvas = res.body || [];
    });
    this.rayonService
      .query({
        page: 0,
        size: 9999
      })
      .subscribe((res: HttpResponse<IRayon[]>) => {
        this.rayons = res.body || [];
      });
  }

  protected handlePrixAchatInput(event: any): void {
    this.validatePrices(Number(event.target.value), Number(this.editForm.get(['prixUni']).value));
  }

  protected handlePrixUnitaireInput(event: any): void {
    this.validatePrices(Number(this.editForm.get(['prixAchat']).value), Number(event.target.value));
  }

  cancel(): void {
    this.activeModal.close();
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<{}>>): void {
    result.pipe(finalize(() => (this.isSaving = false))).subscribe({
      next: () => this.onSaveSuccess(),
      error: error => this.onSaveError(error)
    });
  }

  private onSaveSuccess(): void {
    this.activeModal.close({ success: true });
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  private createFromForm(): IFournisseurProduit {
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
        id: this.produit.id
      }
    };
  }

  private validatePrices(prixAchat: number, prixUni: number): void {
    this.isValid = prixAchat < prixUni;
  }
}

