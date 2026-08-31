import {AfterViewInit, Component, ElementRef, inject, OnDestroy, OnInit, signal, viewChild, ChangeDetectionStrategy} from '@angular/core';
import {ReactiveFormsModule, UntypedFormBuilder, Validators} from '@angular/forms';
import TranslateDirective from '../../../shared/language/translate.directive';
import {Customer, ICustomer} from '../../../shared/model/customer.model';
import {IClientTiersPayant, ITiersPayant} from '../../../shared/model';
import {TiersPayantService} from '../../tiers-payant/tierspayant.service';
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
import {takeUntil} from 'rxjs/operators';
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
    SelectSearchComponent
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
  complementaireStepComponent = viewChild<ComplementaireStepComponent>('complementaireStep');
  fb = inject(UntypedFormBuilder);
  editForm = this.fb.group({
    id: [],
    firstName: [null, [Validators.required]],
    lastName: [null, [Validators.required]],
    tiersPayantId: [null, [Validators.required]],
    taux: [null, [Validators.required, Validators.min(0), Validators.max(100)]],
    num: [null, [Validators.required]],
    phone: [],
    email: [],
    adresse: [],
    sexe: [],
    datNaiss: [],
    remiseId: [],
  });
  readonly tiersPayantService = inject(TiersPayantService);
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

  searchTiersPayant(query: string): void {
    this.loadTiersPayants(query);
  }

  loadTiersPayants(search?: string): void {
    const query: string = search || '';

    this.tiersPayantService
      .query({
        page: 0,
        size: 10,
        type: this.commonService.categorie(),
        search: query,
      })
      .pipe(takeUntil(this.destroy$))
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
    }
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
      },
      (resp: ITiersPayant) => {
        if (resp) {
          this.tiersPayants().push(resp);
          this.editForm.patchValue({tiersPayantId: resp});
          this.addToAlreadyAdded(resp);
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
