import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { LaboratoireProduitService } from '../laboratoire-produit.service';
import { ILaboratoire, Laboratoire } from '../../../shared/model/laboratoire.model';
import { ButtonComponent } from '../../../shared/ui';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../../shared/error.service';
import { NotificationService } from '../../../shared/services/notification.service';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-form-laboratoire',
  templateUrl: './form-laboratoire.component.html',
  styleUrls: ['./form-laboratoire.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [FormsModule, ReactiveFormsModule, ButtonComponent],
})
export class FormLaboratoireComponent implements OnInit, AfterViewInit {
  header = '';
  laboratoire: ILaboratoire | null = null;
  protected fb = inject(UntypedFormBuilder);
  protected isSaving = false;
  protected editForm = this.fb.group({
    id: [],
    libelle: [null, [Validators.required]],
  });
  private readonly entityService = inject(LaboratoireProduitService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly libelle = viewChild.required<ElementRef>('libelle');

  ngOnInit(): void {
    if (this.laboratoire) {
      this.updateForm(this.laboratoire);
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.libelle().nativeElement.focus();
    }, 100);
  }

  protected updateForm(entity: ILaboratoire): void {
    this.editForm.patchValue({
      id: entity.id,
      libelle: entity.libelle,
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

  private subscribeToSaveResponse(result: Observable<HttpResponse<ILaboratoire>>): void {
    result.pipe(finalize(() => (this.isSaving = false))).subscribe({
      next: (res: HttpResponse<ILaboratoire>) => this.onSaveSuccess(res.body),
      error: err => this.onSaveError(err),
    });
  }

  private onSaveSuccess(response: ILaboratoire | null): void {
    this.activeModal.close(response);
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.notificationService.error(this.errorService.getErrorMessage(error));
  }

  private createFromForm(): ILaboratoire {
    return {
      ...new Laboratoire(),
      id: this.editForm.get(['id']).value,
      libelle: this.editForm.get(['libelle']).value,
    };
  }
}
