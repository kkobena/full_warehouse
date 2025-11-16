import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { AfterViewInit, Component, DestroyRef, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { DynamicDialogModule } from 'primeng/dynamicdialog';
import { Observable } from 'rxjs';
import { PosteService } from '../poste.service';
import { IPoste, Poste } from '../../../shared/model/poste.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../../shared/error.service';
import { Card } from 'primeng/card';
import { Checkbox } from 'primeng/checkbox';

@Component({
  selector: 'jhi-form-poste',
  templateUrl: './form-poste.component.html',
  styleUrl: './form-poste.component.scss',
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    DynamicDialogModule,
    ToastAlertComponent,
    Card,
    Checkbox,
  ],
})
export class FormPosteComponent implements OnInit, AfterViewInit {
  title: string = '';
  entity: IPoste;
  protected fb = inject(UntypedFormBuilder);
  protected isSaving = false;
  protected editForm = this.fb.group({
    id: [],
    name: [null, [Validators.required, Validators.maxLength(100)]],
    posteNumber: [null, [Validators.maxLength(20)]],
    address: [
      null,
      [Validators.required, Validators.pattern(/^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/)],
    ],
    customerDisplay: [false],
    customerDisplayPort: [null, [Validators.maxLength(10)]],
  });
  private readonly entityService = inject(PosteService);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly activeModal = inject(NgbActiveModal);
  private readonly errorService = inject(ErrorService);
  private readonly destroyRef = inject(DestroyRef);
  private nameInput = viewChild.required<ElementRef>('nameInput');

  ngOnInit(): void {
    if (this.entity) {
      this.updateForm(this.entity);
    }

    // Set up conditional validation for posteNumber based on customerDisplay
    this.setupConditionalValidation();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.nameInput().nativeElement.focus();
    }, 100);
  }

  protected save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();
    this.subscribeToSaveResponse(this.entityService.create(entity));
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  private updateForm(entity: IPoste): void {
    this.editForm.patchValue({
      id: entity.id,
      name: entity.name,
      posteNumber: entity.posteNumber,
      address: entity.address,
      customerDisplay: entity.customerDisplay,
      customerDisplayPort: entity.customerDisplayPort,
    });
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<void>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: err => this.onSaveError(err),
    });
  }

  private onSaveSuccess(): void {
    this.activeModal.close();
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.isSaving = false;
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  private createFromForm(): IPoste {
    return {
      ...new Poste(),
      id: this.editForm.get(['id']).value,
      name: this.editForm.get(['name']).value,
      posteNumber: this.editForm.get(['posteNumber']).value,
      address: this.editForm.get(['address']).value,
      customerDisplay: this.editForm.get(['customerDisplay']).value,
      customerDisplayPort: this.editForm.get(['customerDisplayPort']).value,
    };
  }

  private setupConditionalValidation(): void {
    const customerDisplayControl = this.editForm.get('customerDisplay');

    // Update validators initially
    this.updateConditionalValidators(customerDisplayControl?.value);

    // Subscribe to customerDisplay changes
    customerDisplayControl?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(isEnabled => {
      this.updateConditionalValidators(isEnabled);
    });
  }

  private updateConditionalValidators(customerDisplayEnabled: boolean): void {
    const posteNumberControl = this.editForm.get('posteNumber');
    const customerDisplayPortControl = this.editForm.get('customerDisplayPort');

    if (customerDisplayEnabled) {
      // When customer display is enabled, both fields are required
      posteNumberControl?.setValidators([Validators.required, Validators.maxLength(20)]);
      customerDisplayPortControl?.setValidators([Validators.required, Validators.maxLength(10)]);
    } else {
      // When customer display is disabled, both fields are optional
      posteNumberControl?.setValidators([Validators.maxLength(20)]);
      customerDisplayPortControl?.setValidators([Validators.maxLength(10)]);
    }

    // Update validation state
    posteNumberControl?.updateValueAndValidity();
    customerDisplayPortControl?.updateValueAndValidity();
  }
}
