import {ChangeDetectionStrategy, Component, DestroyRef, inject, input, OnInit, output, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {CommonModule} from '@angular/common';
import {HttpClient} from '@angular/common/http';
import {FormArray, FormBuilder, FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';
import {NgbDateStruct} from '@ng-bootstrap/ng-bootstrap';
import {Subject} from 'rxjs';
import {SERVER_API_URL} from 'app/app.constants';
import {ISO_TO_NGB_DATE, NGB_DATE_TO_ISO} from 'app/shared/util/warehouse-util';
import {IDci} from 'app/features/dci/models/dci.model';
import {BadgeComponent, ButtonComponent, CardComponent, InputComponent, InputNumberComponent, SelectSearchComponent, SwitchComponent} from '../../../shared/ui';
import {PharmaDatePickerComponent} from '../../../shared/date-picker/pharma-date-picker.component';
import {NotificationService} from '../../../shared/services/notification.service';
import {CustomerService} from '../customer.service';
import {IAllergie, IDossierSante} from '../customer-fiche.model';

/**
 * Onglet « Santé » de la fiche client (docs/PLAN-FICHE-CLIENT.md, lot 2) : allergies, pathologies,
 * grossesse, allaitement, poids. Une allergie à une molécule déclenche l'alerte à la vente.
 */
@Component({
  selector: 'app-dossier-sante-tab',
  templateUrl: './dossier-sante-tab.component.html',
  styles: `
    .fiche-champ-dci {
      flex: 1 1 280px;
      min-width: 280px;
    }

    .fiche-note {
      white-space: pre-line;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    BadgeComponent,
    ButtonComponent,
    CardComponent,
    InputComponent,
    InputNumberComponent,
    SelectSearchComponent,
    SwitchComponent,
    PharmaDatePickerComponent
  ]
})
export class DossierSanteTabComponent implements OnInit {
  readonly customerId = input.required<number>();
  readonly canEdit = input<boolean>(false);
  /** Émis après enregistrement, pour rafraîchir les pastilles de l'en-tête. */
  readonly saved = output<IDossierSante>();

  protected readonly dossier = signal<IDossierSante | null>(null);
  protected readonly editing = signal(false);
  protected readonly saving = signal(false);
  protected readonly dcis = signal<IDci[]>([]);
  protected readonly pathologieSaisie = signal('');
  /** Puits abonné derrière `[typeahead]` : sans lui, ng-select refiltre localement les résultats serveur. */
  protected readonly typeaheadSink$ = new Subject<string>();

  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly customerService = inject(CustomerService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly form = this.fb.group({
    allergies: this.fb.array<FormGroup>([]),
    pathologies: this.fb.control<string[]>([]),
    grossesse: this.fb.control(false),
    dateTerme: this.fb.control<NgbDateStruct | null>(null),
    allaitement: this.fb.control(false),
    poidsKg: this.fb.control<number | null>(null),
    note: this.fb.control('')
  });

  ngOnInit(): void {
    this.typeaheadSink$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
    this.customerService
      .dossierSante(this.customerId())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(dossier => this.dossier.set(dossier));
  }

  protected get allergies(): FormArray<FormGroup> {
    return this.form.controls.allergies;
  }

  protected modifier(): void {
    const d = this.dossier();
    this.allergies.clear();
    (d?.allergies ?? []).forEach(a => this.allergies.push(this.ligneAllergie(a)));
    this.form.patchValue({
      pathologies: [...(d?.pathologies ?? [])],
      grossesse: d?.grossesse ?? false,
      dateTerme: ISO_TO_NGB_DATE(d?.dateTerme),
      allaitement: d?.allaitement ?? false,
      poidsKg: d?.poidsKg ?? null,
      note: d?.note ?? ''
    });
    this.editing.set(true);
  }

  protected annuler(): void {
    this.editing.set(false);
  }

  protected ajouterAllergie(): void {
    this.allergies.push(this.ligneAllergie({}));
  }

  protected retirerAllergie(index: number): void {
    this.allergies.removeAt(index);
  }

  protected ajouterPathologie(): void {
    const libelle = this.pathologieSaisie().trim();
    const actuelles = this.form.controls.pathologies.value ?? [];
    if (libelle && !actuelles.includes(libelle)) {
      this.form.controls.pathologies.setValue([...actuelles, libelle]);
    }
    this.pathologieSaisie.set('');
  }

  protected retirerPathologie(libelle: string): void {
    this.form.controls.pathologies.setValue((this.form.controls.pathologies.value ?? []).filter(p => p !== libelle));
  }

  protected rechercherDci(search: string): void {
    this.http
      .get<IDci[]>(`${SERVER_API_URL}api/dci`, {params: {page: 0, size: 20, search: search ?? ''}})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(dcis => this.dcis.set(dcis));
  }

  protected enregistrer(): void {
    const v = this.form.getRawValue();
    const dossier: IDossierSante = {
      allergies: this.allergies.getRawValue().map(a => ({
        id: a.id ?? undefined,
        dciId: a.dci?.id ?? undefined,
        libelle: a.dci ? undefined : a.libelle || undefined,
        reaction: a.reaction || undefined
      })),
      pathologies: v.pathologies ?? [],
      grossesse: !!v.grossesse,
      dateTerme: v.grossesse ? NGB_DATE_TO_ISO(v.dateTerme) ?? undefined : undefined,
      allaitement: !!v.allaitement,
      poidsKg: v.poidsKg ?? undefined,
      note: v.note || undefined
    };
    this.saving.set(true);
    this.customerService
      .updateDossierSante(this.customerId(), dossier)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: enregistre => {
          this.saving.set(false);
          this.dossier.set(enregistre);
          this.editing.set(false);
          this.saved.emit(enregistre);
          this.notificationService.success('Dossier santé enregistré');
        },
        error: err => {
          this.saving.set(false);
          this.notificationService.error(err?.error?.message ?? "Erreur lors de l'enregistrement du dossier santé");
        }
      });
  }

  private ligneAllergie(a: IAllergie): FormGroup {
    const dci = a.dciId ? ({id: a.dciId, libelle: a.dciLibelle} as IDci) : null;
    if (dci && !this.dcis().some(d => d.id === dci.id)) {
      this.dcis.update(liste => [dci, ...liste]);
    }
    return this.fb.group({
      id: this.fb.control<number | null>(a.id ?? null),
      dci: this.fb.control<IDci | null>(dci),
      libelle: this.fb.control(a.libelle ?? ''),
      reaction: this.fb.control(a.reaction ?? '')
    });
  }


}
