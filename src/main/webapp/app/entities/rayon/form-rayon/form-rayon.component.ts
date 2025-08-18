import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { DynamicDialogModule } from 'primeng/dynamicdialog';
import { Observable } from 'rxjs';
import { RayonService } from '../rayon.service';
import { IRayon, Rayon } from '../../../shared/model/rayon.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { KeyFilter } from 'primeng/keyfilter';
import { IMagasin, IStorage } from '../../../shared/model/magasin.model';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../../shared/error.service';
import { Card } from 'primeng/card';
import { Select } from 'primeng/select';
import { StorageService } from '../../storage/storage.service';
import { Storage } from '../../storage/storage.model';

@Component({
  selector: 'jhi-form-rayon',
  templateUrl: './form-rayon.component.html',
  styleUrls: ['../../common-modal.component.scss'],
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    DynamicDialogModule,
    KeyFilter,
    ToastAlertComponent,
    Card,
    Select
  ]
})
export class FormRayonComponent implements OnInit, AfterViewInit {
  header: string = '';
  entity: IRayon;
  magasin: IMagasin;
  protected storages: IStorage[] = [];
  protected fb = inject(UntypedFormBuilder);
  protected isSaving = false;
  protected editForm = this.fb.group({
    id: [],
    code: [null, [Validators.required]],
    storageId: [null, [Validators.required]],
    libelle: [null, [Validators.required]]
  });
  private readonly entityService = inject(RayonService);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly activeModal = inject(NgbActiveModal);
  private readonly errorService = inject(ErrorService);
  private readonly storageService = inject(StorageService);
  private libelleInput = viewChild.required<ElementRef>('libelleInput');

  ngOnInit(): void {
    this.findMagsinStorage(this.magasin.id);
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.libelleInput().nativeElement.focus();
    }, 100);
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

  private updateForm(entity: IRayon): void {
    this.editForm.patchValue({
      id: entity.id,
      code: entity.code,
      libelle: entity.libelle,
      storageId: entity.storageId
    });
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<IRayon>>): void {
    result.subscribe({
      next: (res: HttpResponse<IRayon>) => this.onSaveSuccess(res.body),
      error: err => this.onSaveError(err)
    });
  }

  private onSaveSuccess(response: IRayon | null): void {
    this.activeModal.close(response);
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.isSaving = false;
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  private createFromForm(): IRayon {
    return {
      ...new Rayon(),
      id: this.editForm.get(['id']).value,
      code: this.editForm.get(['code']).value,
      libelle: this.editForm.get(['libelle']).value,
      storageId: this.editForm.get(['storageId']).value
    };
  }

  private findMagsinStorage(magasinId: number): void {
    this.storageService
      .fetchStorages({
        magasinId
      })

      .subscribe((res: HttpResponse<Storage[]>) => {
        this.storages = res.body;

        if (this.entity) {
          this.updateForm(this.entity);
        } else {
          const storage = this.storages.find(s => s.storageType === 'Stockage principal');
          this.editForm.get('storageId')?.setValue(storage?.id);
        }
      });
  }
}
