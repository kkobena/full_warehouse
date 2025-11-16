import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { ErrorService } from '../../../shared/error.service';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { KeyFilter } from 'primeng/keyfilter';
import { Card } from 'primeng/card';
import { TvaService } from '../tva.service';
import { ITva, Tva } from '../../../shared/model/tva.model';

@Component({
  selector: 'jhi-form-tva',
  imports: [ToastAlertComponent, Button, FormsModule, InputText, ReactiveFormsModule, KeyFilter, Card],
  templateUrl: './form-tva.component.html',
  styleUrls: ['../../common-modal.component.scss'],
})
export class FormTvaComponent implements OnInit, AfterViewInit {
  header: string = '';
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
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
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
    this.alert().showError(this.errorService.getErrorMessage(error));
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
