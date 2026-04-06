import { AfterViewInit, Component, ElementRef, inject, OnInit, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal, NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';
import { TooltipModule } from 'primeng/tooltip';
import { IProduit } from 'app/shared/model/produit.model';
import { ILot } from 'app/shared/model/lot.model';
import { IStorage } from 'app/shared/model/magasin.model';
import { ProductsApiService } from '../../data-access/services/products-api.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { StorageService } from 'app/entities/storage/storage.service';
import { PharmaDatePickerComponent } from 'app/shared/date-picker/pharma-date-picker.component';

@Component({
  selector: 'app-lot-saisie-produit-modal',
  templateUrl: './lot-saisie-produit-modal.component.html',
  styleUrls: ['./lot-saisie-produit-modal.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    InputNumberModule,
    SelectModule,
    TooltipModule,
    PharmaDatePickerComponent,
  ],
})
export class LotSaisieProduitModalComponent implements OnInit, AfterViewInit {
  /** Produit cible — à définir avant l'ouverture du modal */
  produit!: IProduit;

  protected form!: FormGroup;
  protected isSaving = signal(false);
  protected storages = signal<IStorage[]>([]);

  /** Date d'aujourd'hui en NgbDateStruct (minDate péremption) */
  protected todayStruct: NgbDateStruct;
  /** Date d'aujourd'hui en NgbDateStruct (maxDate fabrication) */
  protected todayMaxStruct: NgbDateStruct;

  @ViewChild('numLotInput') private numLotInput!: ElementRef<HTMLInputElement>;

  protected readonly activeModal = inject(NgbActiveModal);
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ProductsApiService);
  private readonly notif = inject(NotificationService);
  private readonly storageService = inject(StorageService);

  constructor() {
    const now = new Date();
    this.todayStruct = { year: now.getFullYear(), month: now.getMonth() + 1, day: now.getDate() };
    this.todayMaxStruct = { ...this.todayStruct };
  }

  ngOnInit(): void {
    this.form = this.fb.group({
      numLot: [null, [Validators.required, Validators.maxLength(20)]],
      expiryDate: [null, [Validators.required]],
      manufacturingDate: [null],
      quantityReceived: [1, [Validators.required, Validators.min(1)]],
      freeQty: [0, [Validators.min(0)]],
      storageId: [null],
    });

    this.storageService.fetchUserStorages().subscribe(res => {
      this.storages.set(res.body ?? []);
    });

    // Revalider la quantité quand le storage change
    this.form.get('storageId')?.valueChanges.subscribe(() => {
      this.form.get('quantityReceived')?.updateValueAndValidity();
    });
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.numLotInput?.nativeElement?.focus(), 100);
  }

  /** Stock disponible pour le storage sélectionné (ou total si aucun sélectionné). */
  get stockDisponible(): number {
    const storageId = this.form?.get('storageId')?.value;
    if (storageId != null && this.produit?.stockProduits?.length) {
      return this.produit.stockProduits.find(sp => sp.storageId === storageId)?.qtyStock ?? 0;
    }
    return this.produit?.totalQuantity ?? 0;
  }

  protected get isQuantiteValide(): boolean {
    const qty = (this.form.get('quantityReceived')?.value ?? 0)
      + (this.form.get('freeQty')?.value ?? 0);
    return qty > 0 && qty <= this.stockDisponible;
  }

  protected save(): void {
    if (this.form.invalid || !this.isQuantiteValide) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSaving.set(true);
    const raw = this.form.getRawValue();

    const lot: ILot = {
      produitId: this.produit.id,
      numLot: raw.numLot,
      expiryDate: this.ngbDateToIso(raw.expiryDate),
      manufacturingDate: raw.manufacturingDate ? this.ngbDateToIso(raw.manufacturingDate) : undefined,
      quantityReceived: raw.quantityReceived,
      freeQty: raw.freeQty ?? 0,
      storageId: raw.storageId ?? undefined,
    };

    this.api.addLotHorsCommande(lot).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.notif.success('Lot enregistré avec succès', 'Saisie lot');
        this.activeModal.close('saved');
      },
      error: (err) => {
        this.isSaving.set(false);
        const msg = err?.error?.detail ?? err?.error?.title ?? 'Erreur lors de la saisie du lot';
        this.notif.error(msg, 'Erreur');
      },
    });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  private ngbDateToIso(d: NgbDateStruct | null): string | undefined {
    if (!d) return undefined;
    return `${d.year}-${String(d.month).padStart(2, '0')}-${String(d.day).padStart(2, '0')}`;
  }
}
