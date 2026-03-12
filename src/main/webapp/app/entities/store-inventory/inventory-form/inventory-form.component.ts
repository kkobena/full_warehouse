import { AfterViewInit, Component, DestroyRef, ElementRef, inject, OnInit, Renderer2, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  CATEGORY_INVENTORY,
  InventoryCategory,
  InventoryCategoryType,
  IStoreInventory,
  StoreInventory,
} from '../../../shared/model/store-inventory.model';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DynamicDialogModule } from 'primeng/dynamicdialog';
import { MessageService } from 'primeng/api';
import { StoreInventoryService } from '../store-inventory.service';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { HttpResponse } from '@angular/common/http';
import { StorageService } from '../../storage/storage.service';
import { Storage } from '../../storage/storage.model';
import { RayonService } from '../../rayon/rayon.service';
import { IRayon, Rayon } from '../../../shared/model/rayon.model';
import { APPEND_TO } from '../../../shared/constants/pagination.constants';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { ToastModule } from 'primeng/toast';
import { TableModule } from 'primeng/table';
import { RouterModule } from '@angular/router';
import { Select } from 'primeng/select';
import { InputText } from 'primeng/inputtext';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Card } from 'primeng/card';

@Component({
  selector: 'jhi-init-inventory',
  templateUrl: './inventory-form.component.html',
  styleUrls: ['./inventory-form.scss'],
  providers: [MessageService],
  imports: [
    WarehouseCommonModule,
    ConfirmDialogModule,
    ButtonModule,
    RippleModule,
    TooltipModule,
    ToastModule,
    NgxSpinnerModule,
    TableModule,
    RouterModule,
    DynamicDialogModule,
    ReactiveFormsModule,
    Select,
    InputText,
    Card,
  ],
})
export class InventoryFormComponent implements OnInit, AfterViewInit {
  protected description = viewChild.required<ElementRef>('description');
  protected isSaving = false;
  protected categories: InventoryCategory[] = CATEGORY_INVENTORY;
  protected storages: Storage[];
  protected rayons: Rayon[];
  protected readonly appendTo = APPEND_TO;
  protected editForm: FormGroup;
  protected entity: IStoreInventory;
  protected fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly messageService = inject(MessageService);
  private readonly storeInventoryService = inject(StoreInventoryService);
  private readonly storageService = inject(StorageService);
  private readonly rayonService = inject(RayonService);
  private readonly spinner = inject(NgxSpinnerService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly renderer = inject(Renderer2);
  private readonly elementRef = inject(ElementRef);

  ngOnInit(): void {
    this.initForm();

    if (this.entity) {
      this.updateForm(this.entity);
      this.loadRayons(this.entity.storage?.id);
    }
    this.populate();
  }

  protected onDropdownShow(event: any): void {
    const modalBody = this.elementRef.nativeElement.querySelector('.modal-body');
    if (modalBody) {
      this.renderer.addClass(modalBody, 'overflow-visible');
    }
  }

  protected onDropdownHide(event: any): void {
    const modalBody = this.elementRef.nativeElement.querySelector('.modal-body');
    if (modalBody) {
      this.renderer.removeClass(modalBody, 'overflow-visible');
    }
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.description().nativeElement.focus();
    }, 100);
  }

  protected save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();
    this.spinner.show();
    if (entity.id) {
      this.subscribeToSaveResponse(this.storeInventoryService.update(entity));
    } else {
      this.subscribeToSaveResponse(this.storeInventoryService.create(entity));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IStoreInventory>>): void {
    result
      .pipe(
        finalize(() => this.spinner.hide()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res: HttpResponse<IStoreInventory>) => this.onSaveSuccess(res.body),
        error: () => this.onSaveError(),
      });
  }

  protected onSelectCategory(evt: any): void {
    const category = evt.value.name as InventoryCategoryType;
    this.manageStorageInput(category);
    this.manageRayonInput(category);
  }

  protected onSelectStrorage(evt: any): void {
    this.loadRayons(evt.value.id);
  }

  private manageRayonInput(category: InventoryCategoryType): void {
    switch (category) {
      case 'MAGASIN':
      case 'STORAGE':
        this.removeRayonControl();
        break;
      case 'RAYON':
        this.addRayonControl();
        break;
    }
  }

  private manageStorageInput(category: InventoryCategoryType): void {
    switch (category) {
      case 'MAGASIN':
        this.removeStorageControl();
        break;
      case 'STORAGE':
      case 'RAYON':
        this.addStorageControl();
        break;
    }
  }

  private onSaveError(): void {
    this.isSaving = false;
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Enregistrement a échoué',
    });
  }

  private onSaveSuccess(response: IStoreInventory | null): void {
    this.activeModal.close(response);
  }

  private createFromForm(): IStoreInventory {
    const inventoryCategory = this.editForm.get(['inventoryCategory']).value.name;
    if (inventoryCategory === 'MAGASIN') {
      return {
        ...new StoreInventory(),
        id: this.editForm.get(['id']).value,
        description: this.editForm.get(['description']).value,
        inventoryCategory,
      };
    }
    return {
      ...new StoreInventory(),
      description: this.editForm.get(['description']).value,
      id: this.editForm.get(['id']).value,
      storage: this.editForm.get(['storage']).value?.id,
      rayon: this.editForm.get(['rayon']).value?.id,
      inventoryCategory,
    };
  }

  private updateForm(entity: IStoreInventory): void {
    this.editForm.patchValue({
      id: entity.id,
      storage: entity.storage.id,
      rayon: entity.rayon.id,
      inventoryCategory: entity.inventoryCategory.name,
      description: entity.description,
    });
  }

  private populate(): void {
    this.storageService.fetchUserStorages()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res: HttpResponse<Storage[]>) => {
          this.storages = res.body || [];
        },
        error: () => this.onSaveError(),
      });
  }

  private loadRayons(storageId: number): void {
    if (storageId) {
      this.rayonService
        .query({storageId, size: 9999})
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (res: HttpResponse<IRayon[]>) => {
            this.rayons = res.body || [];
          },
          error: () => this.onSaveError(),
        });
    }
  }

  private initForm(): void {
    this.editForm = this.fb.group({
      id: new FormControl<number | null>(null, {}),
      description: new FormControl<string | null>(null, {
        validators: [Validators.required, Validators.maxLength(255)],
        nonNullable: true,
      }),
      inventoryCategory: new FormControl<InventoryCategory | null>(this.categories[0], {
        validators: [Validators.required],
        nonNullable: true,
      }),
    });
  }

  private addStorageControl(): void {
    this.editForm.addControl(
      'storage',
      new FormControl<number | null>(null, {
        validators: [Validators.required],
        nonNullable: true,
      }),
    );
  }

  private removeStorageControl(): void {
    this.editForm.removeControl('storage');
  }

  private addRayonControl(): void {
    this.editForm.addControl(
      'rayon',
      new FormControl<number | null>(null, {
        validators: [Validators.required],
        nonNullable: true,
      }),
    );
  }

  private removeRayonControl(): void {
    // this.editForm.get('rayon').reset();
    this.editForm.removeControl('rayon');
  }
}
