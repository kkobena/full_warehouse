import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Component, inject, OnInit, viewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { ILaboratoire } from '../../../shared/model/laboratoire.model';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { Card } from 'primeng/card';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../../shared/error.service';
import { finalize } from 'rxjs/operators';
import { IConfiguration } from '../../../shared/model/configuration.model';
import { ConfigurationService } from '../../../shared/configuration.service';
import { Checkbox } from 'primeng/checkbox';
import { Textarea } from 'primeng/textarea';
import { KeyFilter } from 'primeng/keyfilter';

@Component({
  selector: 'jhi-form-laboratoire',
  templateUrl: './form-paramettre.component.html',
  styleUrls: ['../../common-modal.component.scss'],
  imports: [
    FormsModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    ToastAlertComponent,
    Card,
    Checkbox,
    Textarea,
    KeyFilter
  ]
})
export class FormParamettreComponent implements OnInit {
  header = '';
  entity: IConfiguration | null = null;
  protected fb = inject(UntypedFormBuilder);
  protected isSaving = false;
  protected editForm = this.fb.group({
    name: [Validators.required],
    description: [null, [Validators.required]],
    value: [null, [Validators.required]]
  });
  private readonly activeModal = inject(NgbActiveModal);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);
  private readonly configurationService = inject(ConfigurationService);

  ngOnInit(): void {

    this.updateForm(this.entity);

  }


  protected updateForm(entity: IConfiguration): void {
    const value = entity.valueType === 'BOOLEAN' ? entity.value === '1' : entity.value;
    this.editForm.patchValue({
      value,
      name: entity.name,
      description: entity.description
    });
  }

  protected save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();
    this.subscribeToSaveResponse(this.configurationService.update(entity));
  }


  protected cancel(): void {
    this.activeModal.dismiss();
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<ILaboratoire>>): void {
    result.pipe(finalize(() => this.isSaving = false)).subscribe({
        next: (res: HttpResponse<ILaboratoire>) => this.onSaveSuccess(res.body),
        error: (err) => this.onSaveError(err)
      }
    );
  }

  private onSaveSuccess(response: ILaboratoire | null): void {
    this.activeModal.close(response);
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  private createFromForm(): IConfiguration {
    return {
      ...this.entity,
      description: this.editForm.get(['description']).value,
      value: this.editForm.get(['value']).value
    };
  }
}
