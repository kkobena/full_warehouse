import { Component, OnInit, inject } from '@angular/core';
import { IProduit } from '../../../shared/model/produit.model';
import { FournisseurProduit, IFournisseurProduit } from '../../../shared/model/fournisseur-produit.model';
import { ProduitService } from '../produit.service';
import { ErrorService } from '../../../shared/error.service';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';

import { IFournisseur } from '../../../shared/model/fournisseur.model';
import { FournisseurService } from '../../fournisseur/fournisseur.service';
import { HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService, DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ToastModule } from 'primeng/toast';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { InputSwitchModule } from 'primeng/inputswitch';
import { KeyFilterModule } from 'primeng/keyfilter';
import { Select } from 'primeng/select';
import { ToggleSwitch } from 'primeng/toggleswitch';

@Component({
  selector: 'jhi-form-produit-fournisseur',
  templateUrl: './form-produit-fournisseur.component.html',
  providers: [MessageService, DialogService, ConfirmationService],
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ReactiveFormsModule,
    ToastModule,
    InputTextModule,
    DropdownModule,
    ButtonModule,
    RippleModule,
    InputSwitchModule,
    KeyFilterModule,
    DynamicDialogModule,
    Select,
    ToggleSwitch,
  ],
})
export class FormProduitFournisseurComponent implements OnInit {
  protected produitService = inject(ProduitService);
  protected errorService = inject(ErrorService);
  private fb = inject(UntypedFormBuilder);
  ref = inject(DynamicDialogRef);
  config = inject(DynamicDialogConfig);
  protected fournisseurService = inject(FournisseurService);
  private messageService = inject(MessageService);

  produit?: IProduit;
  entity?: IFournisseurProduit;
  isSaving = false;
  isValid = true;
  fournisseurSelectedId!: number;
  fournisseurs: IFournisseur[] = [];
  editForm = this.fb.group({
    id: [],
    prixUni: [null, [Validators.required, Validators.min(1)]],
    prixAchat: [null, [Validators.required, Validators.min(1)]],
    codeCip: [null, [Validators.required, Validators.minLength(6), Validators.maxLength(8)]],
    fournisseurId: [null, [Validators.required]],
    principal: [null, [Validators.required]],
  });

  save(): void {
    this.isSaving = true;
    const produitFournisseur = this.createFrom();
    if (produitFournisseur.id !== undefined && produitFournisseur.id) {
      this.subscribeToSaveResponse(this.produitService.updateProduitFournisseur(produitFournisseur));
    } else {
      this.subscribeToSaveResponse(this.produitService.createProduitFournisseur(produitFournisseur));
    }
  }

  ngOnInit(): void {
    this.produit = this.config.data.produit;
    this.entity = this.config.data.entity;

    if (this.entity) {
      this.updateForm(this.entity);
      if (this.entity.fournisseurId) {
        this.fournisseurSelectedId = this.entity.fournisseurId;
      }
    }

    this.populate();
    if (!this.hasPrincipal() && !this.entity) {
      this.editForm.get('principal').setValue(true);
    } else if (this.entity) {
      this.editForm.get('principal').setValue(this.entity.principal);
    } else {
      this.editForm.get('principal').setValue(false);
    }
  }

  hasPrincipal(): boolean {
    if (this.isEmpty()) {
      return false;
    }
    const principal = this.produit.fournisseurProduits.some(e => e.principal);
    if (principal) {
      return principal;
    }
    return false;
  }

  isEmpty(): boolean {
    const itemLength = this.produit.fournisseurProduits.length;
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
      produitId: this.produit.id,
    });
  }

  cancel(): void {
    this.ref.close();
  }

  populate(): void {
    if (this.entity) {
      this.fournisseurService
        .query({
          page: 0,
          size: 9999,
        })
        .subscribe((res: HttpResponse<IFournisseur[]>) => {
          this.fournisseurs = res.body || [];
        });
    } else {
      this.fournisseurService.query().subscribe((res: HttpResponse<IFournisseur[]>) => {
        this.fournisseurs = res.body || [];
      });
    }
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

  onChange(event: any): void {
    this.fournisseurSelectedId = event.value;
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

  protected createFrom(): IFournisseurProduit {
    return {
      ...new FournisseurProduit(),
      id: this.editForm.get(['id']).value,
      prixUni: this.editForm.get(['prixUni']).value,
      prixAchat: this.editForm.get(['prixAchat']).value,
      codeCip: this.editForm.get(['codeCip']).value,
      fournisseurId: this.editForm.get(['fournisseurId']).value,
      principal: this.editForm.get(['principal']).value,
      produitId: this.produit.id,
    };
  }
}
