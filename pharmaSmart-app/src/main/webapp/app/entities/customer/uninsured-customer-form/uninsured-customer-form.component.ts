import { AfterViewInit, Component, ElementRef, inject, OnDestroy, OnInit, viewChild, ChangeDetectionStrategy, signal } from '@angular/core';
import { Customer, ICustomer } from 'app/shared/model/customer.model';
import { ErrorService } from 'app/shared/error.service';
import { AbstractControl, FormsModule, ReactiveFormsModule, UntypedFormBuilder, ValidationErrors, Validators } from '@angular/forms';
import { CustomerService, IControleClient } from 'app/entities/customer/customer.service';
import { catchError, debounceTime, finalize, switchMap, takeUntil } from 'rxjs/operators';
import { forkJoin, Observable, of, Subject } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { NotificationService } from '../../../shared/services/notification.service';
import { NgbConfirmDialogService } from '../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { showCommonModal } from '../../sales/selling-home/sale-helper';
import { AssureFormStepComponent } from '../assure-form-step/assure-form-step.component';
import { DateNaissDirective } from '../../../shared/date-naiss.directive';
import { ButtonComponent, CardComponent, CheckboxComponent, FormFieldComponent, KeyFilterDirective, RadioComponent } from '../../../shared/ui';

/** Un champ texte ne se contente pas d'être « non vide » : des espaces seuls ne font pas un nom. */
const nonVide = (control: AbstractControl): ValidationErrors | null =>
  (control.value ?? '').toString().trim().length === 0 ? { required: true } : null;

@Component({
  selector: 'app-uninsured-customer-form',
  templateUrl: './uninsured-customer-form.component.html',
  styleUrls: ['./uninsured-customer-component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ReactiveFormsModule, ButtonComponent, CardComponent, CheckboxComponent, DateNaissDirective, FormFieldComponent, KeyFilterDirective, RadioComponent],
})
export class UninsuredCustomerFormComponent implements OnInit, AfterViewInit, OnDestroy {
  title: string | null = null;
  entity?: ICustomer;
  protected readonly isSaving = signal(false);
  /** Résultat du dernier contrôle anticipé : client identique (bloquant) et clients voisins (avertissement). */
  protected readonly controle = signal<IControleClient | null>(null);
  /** Date de naissance et sexe : facultatifs, repliés par défaut pour ne pas alourdir la création au comptoir. */
  protected readonly complementsOuverts = signal(false);
  protected firstName = viewChild.required<ElementRef>('firstName');
  /** Chiffres, avec un + initial pour un numéro étranger. */
  protected readonly phoneFilter = /^\+?\d*$/;
  protected fb = inject(UntypedFormBuilder);
  protected editForm = this.fb.group({
    id: [],
    firstName: [null, [nonVide, Validators.maxLength(100)]],
    lastName: [null, [nonVide, Validators.maxLength(100)]],
    // Facultatif : beaucoup de clients comptant n'en donnent pas, et un numéro de complaisance pollue le fichier.
    // Un numéro étranger se saisit avec son indicatif (+223…).
    phone: [null, [Validators.pattern(/^\+?\d{6,14}$/), Validators.maxLength(15)]],
    email: [null, [Validators.email, Validators.maxLength(100)]],
    // Pour le dossier santé (alertes d'âge, grossesse) : à saisir dès la création, ou plus tard sur la fiche.
    datNaiss: [null],
    sexe: [null],
    // Consentements explicites, jamais cochés d'office.
    consentementSms: [false],
    consentementEmail: [false],
  });
  private readonly controle$ = new Subject<void>();
  private destroy$ = new Subject<void>();
  private readonly errorService = inject(ErrorService);
  private readonly customerService = inject(CustomerService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly modalService = inject(NgbModal);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);

  ngOnInit(): void {
    if (this.entity) {
      this.updateForm(this.entity);
      this.complementsOuverts.set(!!(this.entity.datNaiss || this.entity.sexe));
    }
    // Contrôle anticipé : le client identique ou voisin est signalé AVANT l'enregistrement, pas au rejet.
    this.controle$
      .pipe(
        debounceTime(400),
        switchMap(() => {
          const valeur = this.editForm.value;
          const phone = (valeur.phone ?? '').toString().trim();
          const firstName = (valeur.firstName ?? '').toString().trim();
          const lastName = (valeur.lastName ?? '').toString().trim();
          if (phone.length < 8 && !(firstName.length >= 2 && lastName.length >= 2)) {
            return of(null);
          }
          // Une aide : son échec ne doit jamais empêcher la saisie (le serveur contrôle à l'enregistrement).
          return this.customerService
            .controlerClientComptant({ phone, firstName, lastName, excludeId: valeur.id ?? undefined })
            .pipe(catchError(() => of(null)));
        }),
        takeUntil(this.destroy$),
      )
      .subscribe(resultat => this.controle.set(resultat));
    ['firstName', 'lastName', 'phone'].forEach(champ =>
      this.editForm
        .get(champ)
        ?.valueChanges.pipe(takeUntil(this.destroy$))
        .subscribe(() => this.controle$.next()),
    );
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.controle$.complete();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.firstName().nativeElement.focus();
    }, 100);
  }

  /** Message d'erreur d'un champ ; vide tant qu'il n'a pas été touché ou qu'il est valide. */
  protected erreur(champ: string): string {
    const controle = this.editForm.get(champ);
    if (!controle || !controle.invalid || !(controle.dirty || controle.touched)) {
      return '';
    }
    if (controle.errors?.required) {
      return 'Ce champ est requis.';
    }
    if (controle.errors?.email) {
      return 'Adresse e-mail invalide.';
    }
    if (controle.errors?.pattern) {
      return 'Numéro invalide : chiffres uniquement (6 à 14). Pour un numéro étranger, commencez par + et l\'indicatif.';
    }
    if (controle.errors?.invalidDate || controle.errors?.outOfRange) {
      return 'Veuillez saisir une date valide.';
    }
    if (controle.errors?.maxlength) {
      return `Au plus ${controle.errors.maxlength.requiredLength} caractères.`;
    }
    return '';
  }

  protected describedBy(champ: string, aide?: string): string | null {
    return this.erreur(champ) ? `${champ}-error` : aide ? `${champ}-hint` : null;
  }

  protected get modeCreation(): boolean {
    return !this.editForm.get('id')?.value;
  }

  /** Les champs voisins ne se proposent à la sélection qu'en création : en modification, il n'y a rien à rattraper. */
  protected selectionner(client: ICustomer): void {
    this.activeModal.close(client);
  }

  /**
   * Le client présente finalement une carte d'assuré : l'identité déjà saisie est transmise au
   * formulaire assuré. S'il est enregistré, c'est ce client qui est rendu à l'appelant ; sinon on
   * revient ici, saisie intacte.
   */
  protected basculerVersAssure(): void {
    const valeur = this.editForm.value;
    showCommonModal(
      this.modalService,
      AssureFormStepComponent,
      {
        entity: null,
        typeAssure: 'ASSURANCE',
        header: 'FORMULAIRE DE CREATION DE CLIENT ',
        prefill: {
          firstName: valeur.firstName,
          lastName: valeur.lastName,
          phone: valeur.phone,
          email: valeur.email,
          datNaiss: valeur.datNaiss,
          sexe: valeur.sexe,
          ayantDroits: [],
        },
      },
      (assure: ICustomer) => {
        if (assure) {
          this.activeModal.close(assure);
        }
      },
      'xl',
      'modal-dialog-80',
    );
  }

  save(): void {
    if (this.editForm.invalid || this.controle()?.clientExistant) {
      this.editForm.markAllAsTouched();
      return;
    }
    this.isSaving.set(true);
    const customer = this.createFromForm();
    if (customer.id !== undefined && customer.id) {
      customer.type = 'STANDARD';
      this.subscribeToSaveResponse(this.customerService.updateUninsuredCustomer(customer));
    } else {
      this.subscribeToSaveResponse(this.customerService.createUninsuredCustomer(customer));
    }
  }

  updateForm(customer: ICustomer): void {
    this.editForm.patchValue({
      id: customer.id,
      firstName: customer.firstName,
      lastName: customer.lastName,
      email: customer.email,
      phone: customer.phone,
      datNaiss: customer.datNaiss,
      sexe: customer.sexe,
    });
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  /**
   * Garde de fermeture (voir `showCommonModal`) : on ne ferme pas pendant l'enregistrement — la requête
   * continuerait sans que l'on sache si le client est créé — et une saisie commencée se confirme.
   */
  confirmerFermeture(): boolean | Promise<boolean> {
    if (this.isSaving()) {
      return false;
    }
    if (!this.editForm.dirty) {
      return true;
    }
    return new Promise<boolean>(resolve =>
      this.confirmDialog.onConfirm(
        () => resolve(true),
        'Annuler sans enregistrer ?',
        'Les informations saisies seront perdues.',
        undefined,
        () => resolve(false),
      ),
    );
  }

  private createFromForm(): ICustomer {
    const formValue = this.editForm.value;
    return {
      ...new Customer(),
      id: formValue.id,
      firstName: (formValue.firstName ?? '').trim(),
      lastName: (formValue.lastName ?? '').trim(),
      email: formValue.email ? formValue.email.trim() : formValue.email,
      phone: formValue.phone ? formValue.phone.trim() : formValue.phone,
      datNaiss: formValue.datNaiss || undefined,
      sexe: formValue.sexe || undefined,
      type: 'STANDARD',
    };
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<ICustomer>>): void {
    result
      .pipe(
        finalize(() => this.isSaving.set(false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (res: HttpResponse<ICustomer>) => this.onSaveSuccess(res.body),
        error: (error: any) => this.onSaveError(error),
      });
  }

  private onSaveSuccess(customer: ICustomer | null): void {
    const valeur = this.editForm.value;
    if (!customer?.id || !this.modeCreation) {
      this.activeModal.close(customer);
      return;
    }
    // Le consentement se demande au moment où le contact est saisi ; son échec n'annule pas la création.
    const demandes: Observable<unknown>[] = [];
    if (valeur.consentementSms && valeur.phone) {
      demandes.push(this.customerService.enregistrerConsentement(customer.id, 'SMS', true));
    }
    if (valeur.consentementEmail && valeur.email) {
      demandes.push(this.customerService.enregistrerConsentement(customer.id, 'EMAIL', true));
    }
    if (demandes.length === 0) {
      this.activeModal.close(customer);
      return;
    }
    forkJoin(demandes)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.activeModal.close(customer),
        error: () => {
          this.notificationService.warning('Client créé, mais le consentement n\'a pas pu être enregistré : à reprendre depuis sa fiche.', 'Consentement');
          this.activeModal.close(customer);
        },
      });
  }

  private onSaveError(error: any): void {
    if (error.error?.errorKey) {
      this.notificationService.error(this.errorService.getErrorMessage(error), 'Erreur');
    } else {
      this.notificationService.error('Erreur interne du serveur.', 'Erreur');
    }
  }
}
