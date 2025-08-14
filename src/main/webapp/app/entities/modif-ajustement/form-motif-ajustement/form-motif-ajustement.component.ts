import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ModifAjustementService } from '../motif-ajustement.service';
import { IMotifAjustement, MotifAjustement } from '../../../shared/model/motif-ajustement.model';
import { Observable } from 'rxjs';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { ErrorService } from '../../../shared/error.service';
import { finalize } from 'rxjs/operators';
import { Card } from 'primeng/card';

@Component({
  selector: 'jhi-form-motif-ajustement',
  templateUrl: './form-motif-ajustement.component.html',
  styleUrls: ['../../common-modal.component.scss'],
  imports: [
    FormsModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    ToastAlertComponent,
    Card
  ]
})
export class FormMotifAjustementComponent implements OnInit, AfterViewInit {
  header: string;
  entity?: IMotifAjustement;
  private readonly entityService = inject(ModifAjustementService);
  protected fb = inject(UntypedFormBuilder);
  protected isSaving = false;
  protected editForm = this.fb.group({
    id: [],
    libelle: [null, [Validators.required]]
  });
  private readonly activeModal = inject(NgbActiveModal);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);
  private readonly libelle = viewChild.required<ElementRef>('libelle');

  ngOnInit(): void {
    if (this.entity) {
      this.updateForm(this.entity);
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.libelle().nativeElement.focus();
    }, 100);
  }

  protected updateForm(entity: IMotifAjustement): void {
    this.editForm.patchValue({
      id: entity.id,
      libelle: entity.libelle
    });
  }

  protected save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();
    if (entity.id !== undefined && entity.id !== null) {
      this.subscribeToSaveResponse(this.entityService.update(entity));
    } else {
      this.subscribeToSaveResponse(this.entityService.create(entity));
    }
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<IMotifAjustement>>): void {
    result.pipe(finalize(() => this.isSaving = false)).subscribe({
      next: (response: HttpResponse<IMotifAjustement>) => this.onSaveSuccess(response.body),
      error: (err) => this.onSaveError(err)
    });
  }

  private onSaveSuccess(response: IMotifAjustement | null): void {
    this.activeModal.close(response);
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  private createFromForm(): IMotifAjustement {
    return {
      ...new MotifAjustement(),
      id: this.editForm.get(['id']).value,
      libelle: this.editForm.get(['libelle']).value
    };
  }
}
