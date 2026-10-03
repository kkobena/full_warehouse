import {AfterViewInit, Component, ElementRef, inject, OnDestroy, OnInit, signal, viewChild, ChangeDetectionStrategy} from '@angular/core';
import {ReactiveFormsModule, UntypedFormBuilder, Validators} from '@angular/forms';
import TranslateDirective from '../../../shared/language/translate.directive';
import {Customer, ICustomer} from '../../../shared/model/customer.model';
import {IClientTiersPayant, ITiersPayant} from '../../../shared/model';
import {TiersPayantService} from '../../tiers-payant/tierspayant.service';
import {CustomerService} from '../customer.service';
import {HttpResponse} from '@angular/common/http';
import {AssureFormStepService} from './assure-form-step.service';
import {CommonService} from './common.service';
import {ComplementaireStepComponent} from './complementaire-step.component';
import {DateNaissDirective} from '../../../shared/date-naiss.directive';
import {CommonModule} from '@angular/common';
import {showCommonModal} from '../../sales/selling-home/sale-helper';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {FormTiersPayantComponent} from '../../tiers-payant/form-tiers-payant/form-tiers-payant.component';
import {Subject} from 'rxjs';
import {finalize, takeUntil} from 'rxjs/operators';
import {PharmaDatePickerComponent} from '../../../shared/date-picker/pharma-date-picker.component';
import {ISO_TO_NGB_DATE, NGB_DATE_TO_ISO} from '../../../shared/util/warehouse-util';
import {
  CardComponent,
  KeyFilterDirective,
  RadioComponent,
  SelectSearchComponent
} from '../../../shared/ui';

@Component({
  selector: 'jhi-assure-step',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslateDirective,
    ComplementaireStepComponent,
    DateNaissDirective,
    CardComponent,
    KeyFilterDirective,
    RadioComponent,
    SelectSearchComponent,
    PharmaDatePickerComponent
  ],
  templateUrl: './assure-step.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ['./assured-form-step.component.scss'],
})
export class AssureStepComponent implements OnInit, AfterViewInit, OnDestroy {
  entity?: ICustomer;
  /**
   * Le puits de frappe branché sur `[typeahead]` de `app-select-search`.
   *
   * Sans lui, ng-select refiltre LOCALEMENT les options sur leur `bindLabel` après la
   * recherche serveur : taper « CNPS » interrogeait bien l'API, qui renvoyait la CAISSE
   * NATIONALE DE PREVOYANCE SOCIALE — dont le libellé ne contient pas « CNPS » — et la liste
   * affichait « Aucun résultat ». Le champ paraissait vide alors que la réponse était là.
   * Branché ET ABONNÉ : ng-select ne renonce à son filtrage local que si le sujet a au moins
   * un observateur — un sujet passé sans abonnement le laisse filtrer comme avant, et le
   * défaut reste entier. D'où l'abonnement vide de `ngOnInit`.
   */
  protected readonly typeaheadSink$ = new Subject<string>();
  minLength = 2;
  tiersPayant!: ITiersPayant | null;
  protected readonly tiersPayants = signal<ITiersPayant[]>([]);
  tiersPayantAlreadyAdded = signal<IClientTiersPayant[]>([]);

  commonService = inject(CommonService);
  assureFormStepService = inject(AssureFormStepService);
  firstName = viewChild.required<ElementRef>('firstName');
  private readonly numInput = viewChild<ElementRef>('numInput');
  complementaireStepComponent = viewChild<ComplementaireStepComponent>('complementaireStep');
  fb = inject(UntypedFormBuilder);
  editForm = this.fb.group({
    id: [],
    firstName: [null, [Validators.required]],
    lastName: [null, [Validators.required]],
    tiersPayantId: [null, [Validators.required]],
    taux: [null, [Validators.required, Validators.min(0), Validators.max(100)]],
    num: [null, [Validators.required, (c: any) => this.erreurNumeroUtilise(c)]],
    dateFinValidite: [null],
    phone: [],
    email: [],
    adresse: [],
    sexe: [],
    datNaiss: [],
    remiseId: [],
  });
  readonly tiersPayantService = inject(TiersPayantService);
  private readonly customerService = inject(CustomerService);
  /** Recherche d'organisme en cours : sans retour visuel, la saisie paraît bloquée. */
  protected readonly chargementTiersPayants = signal(false);
  /** Dossiers de même nom et prénom : un avertissement, jamais un blocage. */
  protected readonly homonymes = signal<string[]>([]);
  /** Dernier numéro refusé par le contrôle anticipé (organisme, numéro, dossier en cause). */
  private refusNumero: { tiersPayantId: number; num: string; titulaire: string } | null = null;
  /** Dernier trio nom, prénom, matricule refusé : un client identique existe déjà. */
  private refusIdentite: { firstName: string; lastName: string; num: string; titulaire: string } | null = null;
  readonly modalService = inject(NgbModal);
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    // Abonnement volontairement vide : il n'existe que pour donner un observateur au sujet
    // (voir `typeaheadSink$`). Les termes sont traités par `(searched)`.
    this.typeaheadSink$.pipe(takeUntil(this.destroy$)).subscribe();
    const entity = this.assureFormStepService.assure();
    if (entity) {
      this.updateForm(entity);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewInit(): void {
    this.focusAndInitComplementaire(this.firstName().nativeElement, this.assureFormStepService.assure());
  }

  /**
   * Contrôle anticipé, à la sortie des champs nom, prénom et numéro : le numéro de carte déjà utilisé
   * pour cet organisme (avec le nom du dossier en cause) et les homonymes sont signalés AVANT d'avoir
   * rempli le reste du formulaire, plutôt qu'au rejet de l'enregistrement.
   */
  protected controlerSaisie(): void {
    const valeur = this.editForm.value;
    const tiersPayantId = valeur.tiersPayantId?.id;
    const num = (valeur.num ?? '').toString().trim();
    const firstName = (valeur.firstName ?? '').toString().trim();
    const lastName = (valeur.lastName ?? '').toString().trim();
    if (!(tiersPayantId && num) && !(firstName && lastName)) {
      return;
    }
    this.customerService
      .controlerAssure({tiersPayantId, num, firstName, lastName, excludeId: valeur.id ?? undefined})
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: controle => {
          this.homonymes.set(controle.homonymes ?? []);
          this.refusNumero = controle.numeroDejaUtilise ? {tiersPayantId, num, titulaire: controle.titulaireDuNumero ?? ''} : null;
          this.refusIdentite = controle.dossierExistant ? {firstName, lastName, num, titulaire: controle.titulaireDuDossier ?? ''} : null;
          this.editForm.get('num')?.updateValueAndValidity();
        },
        // Le contrôle est une aide : son échec ne doit jamais empêcher la saisie (le serveur contrôle à l'enregistrement).
        error: () => this.homonymes.set([])
      });
  }

  private erreurNumeroUtilise(champ: any): {numeroUtilise?: string; dossierExistant?: string} | null {
    const num = (champ.value ?? '').toString().trim();
    const refus = this.refusNumero;
    const tiersPayantId = champ.parent?.get('tiersPayantId')?.value?.id;
    const erreurs: {numeroUtilise?: string; dossierExistant?: string} = {};
    if (refus && refus.tiersPayantId === tiersPayantId && refus.num === num) {
      erreurs.numeroUtilise = refus.titulaire;
    }
    const identite = this.refusIdentite;
    const nom = (champ.parent?.get('firstName')?.value ?? '').toString().trim();
    const prenom = (champ.parent?.get('lastName')?.value ?? '').toString().trim();
    if (identite && identite.num === num && identite.firstName.toLowerCase() === nom.toLowerCase() && identite.lastName.toLowerCase() === prenom.toLowerCase()) {
      erreurs.dossierExistant = identite.titulaire;
    }
    return Object.keys(erreurs).length > 0 ? erreurs : null;
  }

  searchTiersPayant(query: string): void {
    this.loadTiersPayants(query);
  }

  loadTiersPayants(search?: string): void {
    const query: string = search || '';
    this.chargementTiersPayants.set(true);

    this.tiersPayantService
      .query({
        page: 0,
        size: 10,
        type: this.commonService.categorie(),
        search: query,
      })
      .pipe(
        finalize(() => this.chargementTiersPayants.set(false)),
        takeUntil(this.destroy$)
      )
      .subscribe((res: HttpResponse<ITiersPayant[]>) => {
        const alreadyAddedIds = this.tiersPayantAlreadyAdded().map(tp => tp.tiersPayantId ?? tp.tiersPayant?.id);
        this.tiersPayants.set(res.body.filter(tp => !alreadyAddedIds.includes(tp.id)));
        if (this.tiersPayants().length === 0) {
          this.tiersPayants().push({id: null, fullName: 'Ajouter un nouveau tiers-payant'});
        }
      });
  }

  /**
   * `(selectionChange)` d'app-select-search émet la **valeur** sélectionnée, là où le
   * `(onSelect)` de `p-autocomplete` émettait un objet `{ value, originalEvent }`.
   * Lire `event.value` renvoyait donc toujours `undefined`.
   */
  onSelectTiersPayant(tiersPayant: any): void {
    if (tiersPayant?.id === null) {
      this.addTiersPayantAssurance();
    } else if (tiersPayant) {
      this.tiersPayant = tiersPayant;
      this.commonService.setCategorieTiersPayant(this.tiersPayant.categorie);
      this.addToAlreadyAdded(tiersPayant);
      this.suggererTaux(tiersPayant);
      this.controlerSaisie();
      this.focaliserNumero();
    }
  }

  /** Taux standard proposé quand l'organisme en a un ET que le champ est vide : une valeur déjà saisie n'est jamais écrasée. */
  protected tauxSuggere = signal<number | null>(null);

  private suggererTaux(tiersPayant: ITiersPayant): void {
    const taux = tiersPayant.tauxCouvertureDefaut;
    const champ = this.editForm.get('taux');
    if (taux != null && champ && (champ.value === null || champ.value === '')) {
      champ.patchValue(taux);
      this.tauxSuggere.set(taux);
    } else {
      this.tauxSuggere.set(null);
    }
  }

  /** Le numéro de carte se saisit juste après l'organisme : le curseur s'y place sans passer par la souris. */
  private focaliserNumero(): void {
    // Après la fermeture de la liste de recherche, qui reprend le focus au moment de la sélection.
    setTimeout(() => this.numInput()?.nativeElement.focus());
  }

  private addToAlreadyAdded(tiersPayant: ITiersPayant): void {
    const current = this.tiersPayantAlreadyAdded();
    if (!current.some(tp => (tp.tiersPayantId ?? tp.tiersPayant?.id) === tiersPayant.id)) {
      this.tiersPayantAlreadyAdded.set([...current, {tiersPayantId: tiersPayant.id, tiersPayant}]);
    }
  }

  createFromForm(): ICustomer {
    const formValue = this.editForm.value;
    return {
      ...new Customer(),
      id: formValue.id,
      firstName: formValue.firstName,
      lastName: formValue.lastName,
      email: formValue.email,
      phone: formValue.phone,
      type: 'ASSURE',
      num: formValue.num,
      dateFinValidite: NGB_DATE_TO_ISO(formValue.dateFinValidite),
      datNaiss: formValue.datNaiss,
      sexe: formValue.sexe,
      tiersPayantId: formValue.tiersPayantId?.id,
      taux: formValue.taux,
      tiersPayant: formValue.tiersPayantId,
      tiersPayants: this.buildComplementaires(),
    };
  }

  addTiersPayantAssurance(): void {
    showCommonModal(
      this.modalService,
      FormTiersPayantComponent,
      {
        entity: null,
        categorie: this.assureFormStepService.typeAssure(),
        title: 'FORMULAIRE DE CREATION DE TIERS-PAYANT',
        modeExpress: true,
      },
      (resp: ITiersPayant) => {
        if (resp) {
          this.tiersPayants().push(resp);
          this.editForm.patchValue({tiersPayantId: resp});
          this.addToAlreadyAdded(resp);
          this.focaliserNumero();
        }
      },
      'xl',
      'modal-dialog-80',
    );
  }

  protected updateForm(customer: ICustomer): void {
    this.editForm.patchValue({
      id: customer.id,
      firstName: customer.firstName,
      lastName: customer.lastName,
      email: customer.email,
      phone: customer.phone,
      num: customer.num,
      dateFinValidite: ISO_TO_NGB_DATE(customer.dateFinValidite),
      datNaiss: customer.datNaiss,
      sexe: customer.sexe,
      tiersPayantId: customer.tiersPayant,
      taux: customer.taux,
    });
    if (customer.tiersPayant) {
      this.addToAlreadyAdded(customer.tiersPayant);
    }
  }

  private buildComplementaires(): IClientTiersPayant[] {
    if (this.complementaireStepComponent()) {
      return this.complementaireStepComponent().createFromForm();
    }
    return [];
  }

  private focusAndInitComplementaire(element: any, entity: ICustomer | null): void {
    setTimeout(() => {
      if (element) {
        element.focus();
      }
      if (this.complementaireStepComponent()) {
        this.complementaireStepComponent().initForm(entity);
      }
    }, 100);
  }
}
