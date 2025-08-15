import { AfterViewInit, Component, inject, OnInit, viewChild } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { IProduit } from '../../../shared/model/produit.model';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { PrixReference } from '../model/prix-reference.model';
import { PrixReferenceService } from '../prix-reference.service';
import { Observable } from 'rxjs';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { ErrorService } from '../../../shared/error.service';
import { Select } from 'primeng/select';
import { ITiersPayant } from '../../../shared/model/tierspayant.model';
import { InputNumber } from 'primeng/inputnumber';
import { TiersPayantService } from '../../tiers-payant/tierspayant.service';
import { ProduitService } from '../../produit/produit.service';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { Card } from 'primeng/card';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'jhi-add-prix-form',
  imports: [ButtonModule, ReactiveFormsModule, Select, InputNumber, ToggleSwitch, Card, ToastAlertComponent],
  templateUrl: './add-prix-form.component.html',
   styleUrls: ['../../common-modal.component.scss']
})
export class AddPrixFormComponent implements OnInit, AfterViewInit {
  produit: IProduit | null = null;
  tiersPayant: ITiersPayant | null = null;
  entity: PrixReference | null = null;
  isFromProduit = true;
  protected valeurLabel = 'Prix appliqué';
  protected fb = inject(FormBuilder);
  protected isSaving = false;
  protected typePrix = 'RERERENCE';
  protected tiersPayants: ITiersPayant[] = [];
  protected produits: IProduit[] = [];
  protected pricesType: any[] = [
    {
      code: 'RERERENCE',
      libelle: 'Prix de référence assurance'
    },
    { code: 'POURCENTAGE', libelle: 'Pourcentage appliqué par l\'assureur' }
  ];
  protected editForm = this.fb.group({
    id: new FormControl<number | null>(null, {}),
    valeur: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(5), Validators.max(1000000)],
      nonNullable: true
    }),
    tiersPayantId: new FormControl<number | null>(null, {}),
    produitId: new FormControl<number | null>(null, {}),
    type: new FormControl<string | null>(null, {
      validators: [Validators.required],
      nonNullable: true
    }),
    enabled: new FormControl<boolean | null>(true, {
      validators: [Validators.required],
      nonNullable: true
    })
  });

  private readonly activeModal = inject(NgbActiveModal);
  private readonly entityService = inject(PrixReferenceService);
  private readonly errorService = inject(ErrorService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly produitService = inject(ProduitService);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');

  ngOnInit(): void {
    if (this.isFromProduit) {
      this.getTiersPayants();
    } else {
      this.getProduits();
    }
    if (this.entity !== null && this.entity !== undefined) {
      this.updateForm(this.entity);
    }
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  ngAfterViewInit() {
    /* setTimeout(() => {
       this.editForm.get('type').setValue(this.typePrix);
     }, 30);*/
    this.editForm.get('type').valueChanges.subscribe(value => {
      if (value === 'RERERENCE') {
        this.editForm.get('valeur').setValidators([Validators.required, Validators.min(5), Validators.max(1000000)]);
        this.valeurLabel = 'Prix appliqué';
      } else {
        this.editForm.get('valeur').setValidators([Validators.required, Validators.min(10), Validators.max(100)]);
        this.valeurLabel = 'Pourcentage';
      }
      this.editForm.get('valeur').updateValueAndValidity();
    });
    if (this.isFromProduit) {
      this.editForm.get('tiersPayantId').setValidators([Validators.required]);
      this.editForm.get('tiersPayantId').updateValueAndValidity();
      this.editForm.get('produitId').setValue(this.produit?.id);
    } else {
      this.editForm.get('produitId').setValidators([Validators.required]);
      this.editForm.get('produitId').updateValueAndValidity();
      this.editForm.get('tiersPayantId').setValue(this.tiersPayant?.id);
    }
  }

  protected save(): void {
    this.isSaving = true;
    const prixReference = this.createFromForm();
    if (prixReference.id !== null) {
      this.subscribeToSaveResponse(this.entityService.update(prixReference));
    } else {
      this.subscribeToSaveResponse(this.entityService.create(prixReference));
    }
  }

  protected updateForm(entity: PrixReference): void {
    this.editForm.patchValue({
      id: entity.id,
      type: entity.type,
      tiersPayantId: entity.tiersPayantId,
      valeur: entity.valeur,
      produitId: entity.produitId,
      enabled: entity.enabled
    });
  }

  private createFromForm(): PrixReference {
    return {
      ...new PrixReference(),
      id: this.editForm.get(['id']).value,
      enabled: this.editForm.get(['enabled']).value,
      tiersPayantId: this.editForm.get(['tiersPayantId']).value,
      produitId: this.editForm.get(['produitId']).value,
      type: this.editForm.get(['type']).value,
      valeur: this.editForm.get(['valeur']).value
    };
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<{}>>): void {
    result.pipe(finalize(() => this.isSaving = false)).subscribe({
      next: () => this.onSaveSuccess(),
      error: (err: any) => this.onSaveError(err)
    });
  }

  private onSaveSuccess(): void {
    this.cancel();
  }

  private onSaveError(err: HttpErrorResponse): void {
    this.alert().showError(this.errorService.getErrorMessage(err));
  }

  private getTiersPayants(): void {
    this.tiersPayantService
      .query({
        page: 0,
        size: 9999,
        sort: ['fullName,asc']
      })
      .subscribe({
        next: (res: HttpResponse<ITiersPayant[]>) => {
          this.tiersPayants = res.body || [];
        },
        error: (err) => {
          this.alert().showError(this.errorService.getErrorMessage(err));
        }
      });
  }

  private getProduits(): void {
    this.produitService
      .query({
        page: 0,
        size: 99999,
        sort: ['libelle,asc']
      })
      .subscribe({
        next: (res: HttpResponse<IProduit[]>) => {
          this.produits = res.body || [];
        },
        error: (err) => {
          this.alert().showError(this.errorService.getErrorMessage(err));
        }
      });
  }
}
