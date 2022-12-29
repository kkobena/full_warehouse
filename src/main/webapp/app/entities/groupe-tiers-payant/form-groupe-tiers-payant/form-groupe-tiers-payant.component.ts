import {Component, OnInit} from '@angular/core';
import {UntypedFormBuilder, Validators} from '@angular/forms';
import {GroupeTiersPayant, IGroupeTiersPayant} from 'app/shared/model/groupe-tierspayant.model';
import {ErrorService} from 'app/shared/error.service';
import {DynamicDialogConfig, DynamicDialogRef} from 'primeng/dynamicdialog';
import {ConfirmationService, MessageService} from 'primeng/api';
import {GroupeTiersPayantService} from 'app/entities/groupe-tiers-payant/groupe-tierspayant.service';
import {Observable} from 'rxjs';
import {HttpResponse} from '@angular/common/http';

@Component({
  selector: 'jhi-form-groupe-tiers-payant',
  templateUrl: './form-groupe-tiers-payant.component.html',
  providers: [MessageService, ConfirmationService],
})
export class FormGroupeTiersPayantComponent implements OnInit {
  entity?: IGroupeTiersPayant;
  isSaving = false;
  isValid = true;
  editForm = this.fb.group({
    id: [],
    name: [null, [Validators.required]],
    adresse: [],
    telephone: [],
    telephoneFixe: [],
  });

  constructor(
    protected errorService: ErrorService,
    private fb: UntypedFormBuilder,
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
    protected groupeTiersPayantService: GroupeTiersPayantService,
    private messageService: MessageService
  ) {
  }

  ngOnInit(): void {
    this.entity = this.config.data.entity;
    if (this.entity) {
      this.updateForm(this.entity);
    }
  }

  updateForm(groupeTiersPayant: IGroupeTiersPayant): void {
    this.editForm.patchValue({
      id: groupeTiersPayant.id,
      name: groupeTiersPayant.name,
      adresse: groupeTiersPayant.adresse,
      telephone: groupeTiersPayant.telephone,
      telephoneFixe: groupeTiersPayant.telephoneFixe,
    });
  }

  cancel(): void {
    this.ref.close();
  }

  save(): void {
    this.isSaving = true;
    const groupeTiersPayant = this.createFromForm();
    if (groupeTiersPayant.id !== undefined && groupeTiersPayant.id) {
      this.subscribeToSaveResponse(this.groupeTiersPayantService.update(groupeTiersPayant));
    } else {
      this.subscribeToSaveResponse(this.groupeTiersPayantService.create(groupeTiersPayant));
    }
  }

  protected createFromForm(): IGroupeTiersPayant {
    return {
      ...new GroupeTiersPayant(),
      id: this.editForm.get(['id'])!.value,
      name: this.editForm.get(['name'])!.value,
      adresse: this.editForm.get(['adresse'])!.value,
      telephone: this.editForm.get(['telephone'])!.value,
      telephoneFixe: this.editForm.get(['telephoneFixe'])!.value,
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IGroupeTiersPayant>>): void {
    result.subscribe(
      res => this.onSaveSuccess(res.body),
      error => this.onSaveError(error)
    );
  }

  protected onSaveSuccess(groupeTiersPayant: IGroupeTiersPayant | null): void {
    this.isSaving = false;
    this.ref.close(groupeTiersPayant);
  }

  protected onSaveError(error: any): void {
    this.isSaving = false;
    if (error.error && error.error.errorKey) {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(translatedErrorMessage => {
        this.messageService.add({severity: 'error', summary: 'Erreur', detail: translatedErrorMessage});
      });
    } else {
      this.messageService.add({severity: 'error', summary: 'Erreur', detail: 'Erreur interne du serveur.'});
    }
  }
}
