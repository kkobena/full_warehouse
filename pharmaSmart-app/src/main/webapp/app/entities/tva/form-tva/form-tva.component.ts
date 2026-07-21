import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ErrorService } from '../../../shared/error.service';
import { NotificationService } from '../../../shared/services/notification.service';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent, KeyFilterDirective } from '../../../shared/ui';
import { TvaService } from '../tva.service';
import { ITva, Tva } from '../../../shared/model/tva.model';

@Component({
  selector: 'app-form-tva',
  imports: [ButtonComponent, FormsModule, ReactiveFormsModule, KeyFilterDirective],
  templateUrl: './form-tva.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./form-tva.scss'],
})
export class FormTvaComponent implements OnInit, AfterViewInit {
  header = '';
  private readonly taux = viewChild.required<ElementRef>('taux');
  protected fb = inject(FormBuilder);
  protected editForm = this.fb.group({
    id: new FormControl<number | null>(null, {}),
    taux: new FormControl<string | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),
  });
  protected isSaving = false;
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly tvaService = inject(TvaService);
  private readonly activeModal = inject(NgbActiveModal);

  ngOnInit(): void {}

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.taux().nativeElement.focus();
    }, 100);
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  protected save(): void {
    this.isSaving = true;
    this.subscribeToSaveResponse(this.tvaService.create(this.createFromForm()));
  }

  private onSaveSuccess(): void {
    this.isSaving = false;
    this.activeModal.close();
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.isSaving = false;
    this.notificationService.error(this.errorService.getErrorMessage(error));
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<ITva>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: error => this.onSaveError(error),
    });
  }

  private createFromForm(): ITva {
    return {
      ...new Tva(),
      id: this.editForm.get(['id']).value,
      taux: this.editForm.get(['taux']).value,
    };
  }
}
