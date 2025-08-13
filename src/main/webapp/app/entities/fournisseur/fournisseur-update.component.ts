import { Component, inject, OnInit, viewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { FournisseurService } from './fournisseur.service';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Fournisseur, IFournisseur } from '../../shared/model/fournisseur.model';
import { ButtonModule } from 'primeng/button';
import { FileUploadModule } from 'primeng/fileupload';
import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { TooltipModule } from 'primeng/tooltip';
import { IGroupeFournisseur } from '../../shared/model/groupe-fournisseur.model';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ToastAlertComponent } from '../../shared/toast-alert/toast-alert.component';
import { ErrorService } from '../../shared/error.service';
import { Select } from 'primeng/select';
import { Card } from 'primeng/card';
import { CommonModule } from '@angular/common';
import { GroupeFournisseurService } from '../groupe-fournisseur/groupe-fournisseur.service';

@Component({
  selector: 'jhi-fournisseur-update',
  templateUrl: './fournisseur-update.component.html',
  styleUrls: ['./form-modal.component.scss'],
  imports: [
    CommonModule,
    ButtonModule,
    FileUploadModule,
    FormsModule,
    ReactiveFormsModule,
    InputTextModule,
    KeyFilterModule,
    TooltipModule,
    ToastAlertComponent,
    Select,
    Card,
  ],
})
export class FournisseurUpdateComponent implements OnInit {
  fournisseur?: IFournisseur;
  groupes: IGroupeFournisseur[] = [];
  header: string = '';
  isSaving = false;
  protected fb = inject(UntypedFormBuilder);
  protected editForm = this.fb.group({
    id: [],
    code: [null, [Validators.required]],
    libelle: [null, [Validators.required]],
    addresspostale: [],
    phone: [],
    mobile: [],
    groupeFournisseurId: [],
  });
  private readonly entityService = inject(FournisseurService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);
  private readonly groupeFournisseurService = inject(GroupeFournisseurService);
  ngOnInit(): void {
    this.fetchGroupFournisseur();
    if (this.fournisseur) {
      this.updateForm(this.fournisseur);
    }
  }

  protected updateForm(entity: IFournisseur): void {
    this.editForm.patchValue({
      id: entity.id,
      code: entity.code,
      libelle: entity.libelle,
      groupeFournisseurId: entity.groupeFournisseurId,
      addresspostale: entity.addressePostal,
      phone: entity.phone,
      mobile: entity.mobile,
    });
  }

  protected save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();
    if (entity.id) {
      this.subscribeToSaveResponse(this.entityService.update(entity));
    } else {
      this.subscribeToSaveResponse(this.entityService.create(entity));
    }
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  private onSaveSuccess(): void {
    this.isSaving = false;
    this.activeModal.close();
  }

  private onSaveError(err: HttpErrorResponse): void {
    this.isSaving = false;
    this.alert().showError(this.errorService.getErrorMessage(err));
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<IFournisseur>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: err => this.onSaveError(err),
    });
  }

  private createFromForm(): IFournisseur {
    return {
      ...new Fournisseur(),
      id: this.editForm.get(['id'])!.value,
      code: this.editForm.get(['code'])!.value,
      libelle: this.editForm.get(['libelle'])!.value,
      groupeFournisseurId: this.editForm.get(['groupeFournisseurId'])!.value,
      addressePostal: this.editForm.get(['addresspostale'])!.value,
      phone: this.editForm.get(['phone'])!.value,
      mobile: this.editForm.get(['mobile'])!.value,
    };
  }

  private fetchGroupFournisseur(): void {
    this.groupeFournisseurService
      .query({
        page: 0,
        size: 9999,
      })
      .subscribe(res => {
        this.groupes = res.body || [];
      });
  }
}
