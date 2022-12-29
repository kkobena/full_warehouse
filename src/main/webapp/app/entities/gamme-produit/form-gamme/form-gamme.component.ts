import {HttpResponse} from '@angular/common/http';
import {Component, OnInit} from '@angular/core';
import {UntypedFormBuilder, Validators} from '@angular/forms';
import {MessageService} from 'primeng/api';
import {DynamicDialogConfig, DynamicDialogRef} from 'primeng/dynamicdialog';
import {Observable} from 'rxjs';
import {GammeProduitService} from '../gamme-produit.service';
import {GammeProduit, IGammeProduit} from '../../../shared/model/gamme-produit.model';

@Component({
  selector: 'jhi-form-gamme',
  templateUrl: './form-gamme.component.html',
})
export class FormGammeComponent implements OnInit {
  isSaving = false;
  gamme?: IGammeProduit;
  editForm = this.fb.group({
    id: [],
    code: [],
    libelle: [null, [Validators.required]],
  });

  constructor(
    protected entityService: GammeProduitService,
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
    private fb: UntypedFormBuilder,
    private messageService: MessageService
  ) {
  }

  ngOnInit(): void {
    this.gamme = this.config.data.gamme;
    if (this.gamme !== null && this.gamme !== undefined) {
      this.updateForm(this.gamme);
    }
  }

  save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();
    if (entity.id !== undefined && entity.id !== null) {
      this.subscribeToSaveResponse(this.entityService.update(entity));
    } else {
      this.subscribeToSaveResponse(this.entityService.create(entity));
    }
  }

  cancel(): void {
    this.ref.destroy();
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IGammeProduit>>): void {
    result.subscribe(
      (res: HttpResponse<IGammeProduit>) => this.onSaveSuccess(res.body),
      () => this.onSaveError()
    );
  }

  protected onSaveSuccess(response: IGammeProduit | null): void {
    this.messageService.add({severity: 'info', summary: 'Information', detail: 'Enregistrement effectué avec succès'});
    this.ref.close(response);
  }

  protected onSaveError(): void {
    this.isSaving = false;
    this.messageService.add({severity: 'error', summary: 'Erreur', detail: 'Enregistrement a échoué'});
  }

  private updateForm(entity: IGammeProduit): void {
    this.editForm.patchValue({
      id: entity.id,
      code: entity.code,
      libelle: entity.libelle,
    });
  }

  private createFromForm(): IGammeProduit {
    return {
      ...new GammeProduit(),
      id: this.editForm.get(['id'])!.value,
      code: this.editForm.get(['code'])!.value,
      libelle: this.editForm.get(['libelle'])!.value,
    };
  }
}
