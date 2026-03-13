import {Component, DestroyRef, ElementRef, inject, OnInit, Renderer2, signal} from '@angular/core';
import {CommonModule, DatePipe} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {Button} from 'primeng/button';
import {Select} from 'primeng/select';
import {InputText} from 'primeng/inputtext';
import {InputNumber} from 'primeng/inputnumber';
import {DatePicker} from 'primeng/datepicker';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {StorageService} from '../../../../entities/storage/storage.service';
import {RayonService} from '../../../../entities/rayon/rayon.service';
import {FamilleProduitService} from '../../../../entities/famille-produit/famille-produit.service';
import {IStorage} from '../../../../shared/model/magasin.model';
import {IRayon} from '../../../../shared/model/rayon.model';
import {IFamilleProduit} from '../../../../shared/model/famille-produit.model';
import {
  INVENTORY_CATEGORIES,
  InventoryCategoryInfo,
  InventoryCategoryType,
  StoreInventoryCreateRecord,
} from '../../models';
import {InventoryApiService} from '../../data-access/services/inventory-api.service';
import {InventoryStore} from '../../data-access/store/inventory.store';
import {PrimeNG} from "primeng/config";
import {TranslateService} from "@ngx-translate/core";
import {NgxSpinnerService} from "ngx-spinner";

@Component({
  selector: 'app-inventory-create-modal',
  imports: [CommonModule, ReactiveFormsModule, Button, Select, InputText, InputNumber, DatePicker],
  templateUrl: './inventory-create-modal.component.html',
  styleUrl: './inventory-create-modal.component.scss',
  providers: [DatePipe],
})
export class InventoryCreateModalComponent implements OnInit {
  readonly activeModal = inject(NgbActiveModal);
  form!: FormGroup;
  categories = INVENTORY_CATEGORIES;
  groupedCategories = [
    {
      label: 'Périmètre',
      items: INVENTORY_CATEGORIES.filter(c => c.group === 'scope'),
    },
    {
      label: 'Thématique',
      items: INVENTORY_CATEGORIES.filter(c => c.group === 'thematic'),
    },
  ];
  storages = signal<IStorage[]>([]);
  rayons = signal<IRayon[]>([]);
  familles = signal<IFamilleProduit[]>([]);
  selectedCategory = signal<InventoryCategoryInfo | null>(null);
  loading = signal(false);
  errorMessage = signal<string | null>(null);
  private readonly api = inject(InventoryApiService);
  private readonly store = inject(InventoryStore);
  private readonly spinner = inject(NgxSpinnerService);
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly translate = inject(TranslateService);
  private readonly fb = inject(FormBuilder);
  private readonly storageService = inject(StorageService);
  private readonly rayonService = inject(RayonService);
  private readonly familleProduitService = inject(FamilleProduitService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly renderer = inject(Renderer2);
  private readonly elementRef = inject(ElementRef);
  private readonly datePipe = inject(DatePipe);

  constructor() {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
  }

  ngOnInit(): void {
    this.buildForm();
    this.loadStorages();
    this.loadFamilles();

    this.form.get('inventoryCategory')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((cat: InventoryCategoryType) => {
        const info = this.categories.find(c => c.value === cat) ?? null;
        this.selectedCategory.set(info);
        this.handleCategoryChange(info);
      });

    this.form.get('storage')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((storageId: number) => {
        if (storageId && this.selectedCategory()?.needsRayon) {
          this.loadRayons(storageId);
        } else {
          this.rayons.set([]);
          this.form.get('rayon')!.setValue(null);
        }
      });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();

    const record: StoreInventoryCreateRecord = {
      inventoryCategory: value.inventoryCategory,
      description: value.description ?? undefined,
      storage: value.storage ?? undefined,
      rayon: value.rayon ?? undefined,
      famillyId: value.famillyId ?? undefined,
      dateFrom: this.datePipe.transform(value.dateFrom, 'yyyy-MM-dd'),
      dateTo: this.datePipe.transform(value.dateTo, 'yyyy-MM-dd'),
      alerteJours: value.alerteJours ?? undefined,
    };
    this.loading.set(true);
    this.errorMessage.set(null);
    this.spinner.show();
    this.api.create(record)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: resp => {
          this.spinner.hide();
          this.loading.set(false);
          this.store.emitEvent('INVENTORY_CREATED', resp.body);
          this.activeModal.close();
        },
        error: err => {
          this.spinner.hide();
          this.loading.set(false);
          this.errorMessage.set(err?.error?.message ?? err?.message ?? "Erreur lors de la création de l'inventaire");
        },
      });
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  getCategoryLabel(value: InventoryCategoryType): string {
    return this.categories.find(c => c.value === value)?.label ?? value;
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

  private buildForm(): void {
    this.form = this.fb.group({
      inventoryCategory: [null, Validators.required],
      description: [null, Validators.required],
      storage: [null],
      rayon: [null],
      famillyId: [null],
      dateFrom: [null],
      dateTo: [null],
      alerteJours: [90],
    });
  }

  private handleCategoryChange(info: InventoryCategoryInfo | null): void {
    this.form.patchValue({
      storage: null,
      rayon: null,
      famillyId: null,
      dateFrom: null,
      dateTo: null,
      alerteJours: 90
    });
    this.rayons.set([]);

    const storageCtrl = this.form.get('storage')!;
    const rayonCtrl = this.form.get('rayon')!;
    const famillyCtrl = this.form.get('famillyId')!;
    const dateFromCtrl = this.form.get('dateFrom')!;
    const dateToCtrl = this.form.get('dateTo')!;
    const alerteCtrl = this.form.get('alerteJours')!;

    storageCtrl.clearValidators();
    rayonCtrl.clearValidators();
    famillyCtrl.clearValidators();
    dateFromCtrl.clearValidators();
    dateToCtrl.clearValidators();
    alerteCtrl.clearValidators();

    if (info?.needsStorage) {
      storageCtrl.setValidators(Validators.required);
    }
    if (info?.needsRayon) {
      rayonCtrl.setValidators(Validators.required);
    }
    if (info?.needsFamilly) {
      famillyCtrl.setValidators(Validators.required);
    }
    if (info?.needsDateRange) {
      dateFromCtrl.setValidators(Validators.required);
      dateToCtrl.setValidators(Validators.required);
    }
    if (info?.needsAlerteJours) {
      alerteCtrl.setValidators([Validators.required, Validators.min(1)]);
    }

    [storageCtrl, rayonCtrl, famillyCtrl, dateFromCtrl, dateToCtrl, alerteCtrl].forEach(c => c.updateValueAndValidity());
  }

  private loadStorages(): void {
    this.storageService
      .fetchUserStorages()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: resp => this.storages.set(resp.body ?? []),
        error: () => this.storages.set([]),
      });
  }

  private loadRayons(storageId: number): void {
    this.rayonService
      .query({storageId, size: 9999})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: resp => this.rayons.set(resp.body ?? []),
        error: () => this.rayons.set([]),
      });
  }

  private loadFamilles(): void {
    this.familleProduitService
      .query({size: 9999})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: resp => this.familles.set(resp.body ?? []),
        error: () => this.familles.set([]),
      });
  }
}
