import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal, NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { Subject } from 'rxjs';
import { IDci } from 'app/features/dci/models/dci.model';
import { DciApiService } from 'app/features/dci/data-access/services/dci-api.service';
import { ProduitService } from 'app/entities/produit/produit.service';
import { IProduit } from 'app/shared/model/produit.model';
import { ButtonComponent, CardComponent, InputComponent, InputNumberComponent, SelectSearchComponent } from 'app/shared/ui';
import { PharmaDatePickerComponent } from 'app/shared/date-picker/pharma-date-picker.component';
import { ErrorService } from 'app/shared/error.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { ISO_TO_NGB_DATE, NGB_DATE_TO_ISO } from 'app/shared/util/warehouse-util';
import { CustomerService } from '../customer.service';
import { ITraitementChronique, ITraitementChroniqueSaisie } from '../customer-fiche.model';

/**
 * Déclaration ou modification d'un traitement chronique. La molécule suffit : tout produit qui la
 * contient le renouvelle. Le produit imposé ne sert qu'au patient non substituable.
 */
@Component({
  selector: 'app-traitement-chronique-form',
  templateUrl: './traitement-chronique-form.component.html',
  styleUrls: ['./traitement-chronique-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonComponent,
    CardComponent,
    InputComponent,
    InputNumberComponent,
    SelectSearchComponent,
    PharmaDatePickerComponent,
  ],
})
export class TraitementChroniqueFormComponent implements OnInit {
  /** Renseignés à l'ouverture de la modale. */
  customerId!: number;
  traitement: ITraitementChronique | null = null;

  protected readonly dcis = signal<IDci[]>([]);
  protected readonly produits = signal<IProduit[]>([]);
  protected readonly saving = signal(false);
  protected readonly dciTypeahead$ = new Subject<string>();
  protected readonly produitTypeahead$ = new Subject<string>();

  protected readonly activeModal = inject(NgbActiveModal);
  private readonly fb = inject(FormBuilder);
  private readonly dciApiService = inject(DciApiService);
  private readonly produitService = inject(ProduitService);
  private readonly customerService = inject(CustomerService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  protected readonly form = this.fb.group({
    dci: this.fb.control<IDci | null>(null),
    produit: this.fb.control<IProduit | null>(null),
    dosage: this.fb.control<string | null>(null),
    posologie: this.fb.control<string | null>(null),
    dureeJours: this.fb.control<number | null>(30, [Validators.required, Validators.min(1)]),
    dateOrdonnance: this.fb.control<NgbDateStruct | null>(null),
    dateFinOrdonnance: this.fb.control<NgbDateStruct | null>(null),
    note: this.fb.control<string | null>(null),
  });

  ngOnInit(): void {
    const t = this.traitement;
    if (!t) {
      return;
    }
    const dci = t.dciId ? ({ id: t.dciId, libelle: t.dciLibelle } as IDci) : null;
    const produit = t.produitId ? ({ id: t.produitId, libelle: t.produitLibelle } as IProduit) : null;
    this.dcis.set(dci ? [dci] : []);
    this.produits.set(produit ? [produit] : []);
    this.form.patchValue({
      dci,
      produit,
      dosage: t.dosage,
      posologie: t.posologie,
      dureeJours: t.dureeJours,
      dateOrdonnance: t.dateOrdonnance ? ISO_TO_NGB_DATE(t.dateOrdonnance) : null,
      dateFinOrdonnance: t.dateFinOrdonnance ? ISO_TO_NGB_DATE(t.dateFinOrdonnance) : null,
      note: t.note,
    });
  }

  protected rechercherDci(search: string): void {
    this.dciApiService.search(search ?? '').subscribe(dcis => this.dcis.set(dcis));
  }

  protected rechercherProduit(search: string): void {
    this.produitService
      .queryLite({ page: 0, size: 20, search: search ?? '' })
      .subscribe(res => this.produits.set(res.body ?? []));
  }

  protected enregistrer(): void {
    const v = this.form.getRawValue();
    if (!v.dci && !v.produit) {
      this.notificationService.error('Indiquez la molécule du traitement, ou le produit imposé.');
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const saisie: ITraitementChroniqueSaisie = {
      dciId: v.dci?.id ?? null,
      produitId: v.produit?.id ?? null,
      dosage: v.dosage,
      posologie: v.posologie,
      dureeJours: v.dureeJours!,
      dateOrdonnance: v.dateOrdonnance ? NGB_DATE_TO_ISO(v.dateOrdonnance) : null,
      dateFinOrdonnance: v.dateFinOrdonnance ? NGB_DATE_TO_ISO(v.dateFinOrdonnance) : null,
      note: v.note,
      actif: this.traitement?.actif ?? true,
    };
    const requete = this.traitement
      ? this.customerService.modifierTraitement(this.customerId, this.traitement.id, saisie)
      : this.customerService.declarerTraitement(this.customerId, saisie);
    this.saving.set(true);
    requete.subscribe({
      next: traitement => this.activeModal.close(traitement),
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.notificationService.error(this.errorService.getErrorMessage(err, "Le traitement n'a pas pu être enregistré"));
      },
    });
  }
}
