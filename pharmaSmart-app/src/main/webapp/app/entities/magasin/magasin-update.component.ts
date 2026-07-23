import { Component, inject, OnInit, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';

import { FormControl, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Observable } from 'rxjs';

import { IMagasin, Magasin } from 'app/shared/model/magasin.model';
import { MagasinService } from './magasin.service';
import { ButtonComponent, CardComponent, ToolbarComponent } from '../../shared/ui';
import { ErrorService } from '../../shared/error.service';
import { NotificationService } from "../../shared/services/notification.service";

@Component({
  selector: 'app-magasin-update',
  templateUrl: './magasin-update.component.html',
  styleUrl: './magasin-update.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [RouterModule, ReactiveFormsModule, ButtonComponent, CardComponent, ToolbarComponent]
})
export class MagasinUpdateComponent implements OnInit {
  protected fb = inject(UntypedFormBuilder);
  protected isSaving = false;
  protected editForm = this.fb.group({
    id: [],
    name: [null, [Validators.required]],
    fullName: [null, [Validators.required]],
    phone: [null, [Validators.required]],
    address: [],
    note: [],
    fneSecretKey: [],
    fnePointOfSale: [],
    registre: [],
    welcomeMessage: [],
    compteContribuable: [],
    numComptable: [],
    registreImposition: [],
    email: new FormControl(null, {
      validators: [Validators.minLength(5), Validators.maxLength(254), Validators.email],
    }),
  });
  private readonly magasinService = inject(MagasinService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly errorService = inject(ErrorService);
  private readonly notificationService = inject(NotificationService);
  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ magasin }) => {
      this.updateForm(magasin);
    });
  }

  protected updateForm(magasin: IMagasin): void {
    this.editForm.patchValue({
      id: magasin.id,
      email: magasin.email,
      name: magasin.name,
      fullName: magasin.fullName,
      compteContribuable: magasin.compteContribuable,
      numComptable: magasin.numComptable,
      registreImposition: magasin.registreImposition,
      phone: magasin.phone,
      address: magasin.address,
      note: magasin.note,
      registre: magasin.registre,
      welcomeMessage: magasin.welcomeMessage,
      fnePointOfSale: magasin.fnePointOfSale,
      fneSecretKey: magasin.fneSecretKey,
    });
  }

  protected previousState(): void {
    window.history.back();
  }

  protected save(): void {
    this.isSaving = true;
    const magasin = this.createFromForm();
    if (magasin.id !== undefined) {
      this.subscribeToSaveResponse(this.magasinService.update(magasin));
    } else {
      this.subscribeToSaveResponse(this.magasinService.create(magasin));
    }
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<IMagasin>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: err => this.onSaveError(err),
    });
  }

  private onSaveSuccess(): void {
    this.isSaving = false;
    this.previousState();
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.isSaving = false;
    this.notificationService.error(this.errorService.getErrorMessage(error));
  }

  private createFromForm(): IMagasin {
    return {
      ...new Magasin(),
      id: this.editForm.get(['id']).value,
      email: this.editForm.get(['email']).value,
      name: this.editForm.get(['name']).value,
      fullName: this.editForm.get(['fullName']).value,
      phone: this.editForm.get(['phone']).value,
      address: this.editForm.get(['address']).value,
      note: this.editForm.get(['note']).value,
      registre: this.editForm.get(['registre']).value,
      welcomeMessage: this.editForm.get(['welcomeMessage']).value,
      compteContribuable: this.editForm.get(['compteContribuable']).value,
      numComptable: this.editForm.get(['numComptable']).value,
      registreImposition: this.editForm.get(['registreImposition']).value,
      fneSecretKey: this.editForm.get(['fneSecretKey']).value,
      fnePointOfSale: this.editForm.get(['fnePointOfSale']).value,
    };
  }
}
