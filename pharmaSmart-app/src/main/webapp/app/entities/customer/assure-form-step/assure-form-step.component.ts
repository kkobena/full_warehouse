import { Component, HostListener, inject, OnDestroy, OnInit, viewChild, ChangeDetectionStrategy, signal } from "@angular/core";
import { ICustomer } from "../../../shared/model";
import { AssureFormStepService } from "./assure-form-step.service";
import { AssureStepComponent } from "./assure-step.component";
import { AyantDroitStepComponent } from "./ayant-droit-step.component";
import { ErrorService } from "../../../shared/error.service";
import { Observable, Subject } from "rxjs";
import { finalize, takeUntil } from "rxjs/operators";
import { HttpResponse } from "@angular/common/http";
import { CustomerService } from "../customer.service";
import { CommonService } from "./common.service";
import { NgbActiveModal, NgbNavChangeEvent } from "@ng-bootstrap/ng-bootstrap";
import { NgbConfirmDialogService } from "../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { NotificationService } from "../../../shared/services/notification.service";
import { NgbNavModule } from "@ng-bootstrap/ng-bootstrap";
import { ButtonComponent, NavTabsComponent } from "../../../shared/ui";

@Component({
  selector: "app-assure-form-step",
  imports: [
    AssureStepComponent,
    AyantDroitStepComponent,
    NgbNavModule,
    ButtonComponent,
    NavTabsComponent
  ],
  templateUrl: "./assure-form-step.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ["./assured-form-step.component.scss"]
})
export class AssureFormStepComponent implements OnInit, OnDestroy {
  header: string;
  entity?: ICustomer;
  /** Identité déjà saisie ailleurs (bascule depuis un client comptant) : préremplit une CRÉATION. */
  prefill?: Partial<ICustomer>;
  protected readonly isSaving = signal(false);
  typeAssure: string | undefined;
  protected readonly activeStep = signal(1);
  ayantDroitStepComponent = viewChild<AyantDroitStepComponent>("ayantDroitStep");
  assureStepComponent = viewChild<AssureStepComponent>("assureStep");
  protected readonly commonService = inject(CommonService);
  protected readonly assureFormStepService = inject(AssureFormStepService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly errorService = inject(ErrorService);
  private readonly customerService = inject(CustomerService);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private destroy$ = new Subject<void>();
  /** Validité du formulaire assuré, mémorisée quand son onglet est quitté (son composant est alors détruit). */
  private assureValideMemorisee = false;
  /** Une saisie a été faite puis l'onglet quitté : la fermeture doit être confirmée. */
  private saisieEntamee = false;
  /** Évite d'empiler deux récapitulatifs (Ctrl+Entrée répété). */
  private recapitulatifOuvert = false;

  ngOnInit(): void {
    this.assureFormStepService.ayantDroitsBrouillon.set([]);
    this.assureFormStepService.ayantDroitsValides.set(true);
    this.commonService.categorieTiersPayant.set(this.typeAssure);
    this.commonService.categorie.set(this.typeAssure);
    this.assureFormStepService.setTypeAssure(this.typeAssure);
    this.assureFormStepService.setAssure(this.entity ?? (this.prefill ? ({ ...this.prefill } as ICustomer) : null));
    this.assureFormStepService.setEdition(!!this.entity);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** Le formulaire assuré est-il complet ? Lu en direct quand son onglet est affiché, sinon mémorisé. */
  protected assureValide(): boolean {
    const composant = this.assureStepComponent();
    return composant ? composant.editForm.valid : this.assureValideMemorisee;
  }

  protected ayantDroitsValides(): boolean {
    const composant = this.ayantDroitStepComponent();
    return composant ? composant.isValidForm() : this.assureFormStepService.ayantDroitsValides();
  }

  /** Pourquoi « Enregistrer » est grisé ; vide quand on peut enregistrer (ou pendant l'enregistrement). */
  protected raisonEnregistrementImpossible(): string {
    if (this.isSaving() || this.peutEnregistrer()) {
      return "";
    }
    return this.assureValide()
      ? "Complétez ou retirez la ligne d'ayant droit incomplète."
      : "Complétez les champs obligatoires de l'assuré pour enregistrer.";
  }

  protected peutEnregistrer(): boolean {
    return this.assureValide() && this.ayantDroitsValides() && !this.isSaving();
  }

  /**
   * Navigation libre entre onglets : on peut consulter l'ayant droit avant d'avoir fini l'assuré (la
   * carte présentée en premier peut être celle du bénéficiaire). Seul l'enregistrement final exige que
   * tout soit complet. L'état de l'onglet quitté est mis de côté avant que son composant soit détruit.
   */
  protected onNavChange(evenement: NgbNavChangeEvent): void {
    this.memoriserEtatDe(evenement.activeId);
  }

  private memoriserEtatDe(etape: number): void {
    if (etape === 1 && this.assureStepComponent()) {
      this.saisieEntamee ||= this.assureStepComponent().editForm.dirty;
      this.assureValideMemorisee = this.assureStepComponent().editForm.valid;
      this.currentCustomerState();
    } else if (etape === 2 && this.ayantDroitStepComponent()) {
      this.saisieEntamee ||= this.ayantDroitStepComponent().editForm.dirty;
      this.ayantDroitStepComponent().saveFormState();
    }
  }

  /**
   * Garde de fermeture (voir `showCommonModal`) : l'assistant couvre deux onglets et jusqu'à trois
   * mutuelles ; un Échap involontaire ne doit pas en effacer la saisie.
   */
  confirmerFermeture(): boolean | Promise<boolean> {
    const enCours =
      this.saisieEntamee || !!this.assureStepComponent()?.editForm.dirty || !!this.ayantDroitStepComponent()?.editForm.dirty;
    if (!enCours || this.isSaving()) {
      return true;
    }
    return new Promise<boolean>(resolve =>
      this.confirmDialog.onConfirm(
        () => resolve(true),
        "Annuler sans enregistrer ?",
        "Les informations saisies seront perdues.",
        undefined,
        () => resolve(false)
      )
    );
  }

  /** Ctrl+Entrée : étape suivante, puis enregistrement. Alt+flèches : changer d'onglet. */
  @HostListener("document:keydown.control.enter", ["$event"])
  protected onRaccourciValider(evenement: Event): void {
    evenement.preventDefault();
    if (this.activeStep() === 1 && this.peutAllerAuxAyantsDroit() && this.assureValide()) {
      this.onGoAyantDroit(2);
    } else if (this.peutEnregistrer()) {
      this.save();
    }
  }

  @HostListener("document:keydown.alt.arrowright", ["$event"])
  protected onRaccourciSuivant(evenement: Event): void {
    if (this.activeStep() === 1 && this.peutAllerAuxAyantsDroit()) {
      evenement.preventDefault();
      this.memoriserEtatDe(1);
      this.activeStep.set(2);
    }
  }

  @HostListener("document:keydown.alt.arrowleft", ["$event"])
  protected onRaccourciPrecedent(evenement: Event): void {
    if (this.activeStep() === 2) {
      evenement.preventDefault();
      this.onGoBackFromAyantDroit(1);
    }
  }

  /** L'onglet des ayants droit n'existe qu'en création, pour une assurance. */
  protected peutAllerAuxAyantsDroit(): boolean {
    return !this.assureFormStepService.isEdition() && this.typeAssure === "ASSURANCE";
  }

  onCompleteAssure(): void {
    this.currentCustomerState();
    if (!this.assureFormStepService.isEdition() && this.ayantDroitStepComponent()) {
      this.ayantDroitStepComponent().saveFormState();
    }
  }

  onGoBackFromAyantDroit(index: number): void {
    this.ayantDroitStepComponent().goBack();
    this.activeStep.set(index);
  }

  currentCustomerState(): void {
    const assureComponent = this.assureStepComponent();
    if (!assureComponent) {
      return;
    }
    const currentAssure = this.assureFormStepService.assure();
    const ayantDroits = currentAssure ? currentAssure.ayantDroits : [];
    this.assureFormStepService.setAssure({
      ...assureComponent.createFromForm(),
      ayantDroits
    });
  }

  onGoAyantDroit(index: number): void {
    this.currentCustomerState();
    this.activeStep.set(index);
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  onSaveError(error: any): void {
    this.notificationService.error(this.errorService.getErrorMessage(error));
  }

  /**
   * Récapitulatif avant la création : un assuré mal saisi se corrige plus difficilement qu'un nom
   * mal orthographié (taux, numéro de carte, bénéficiaires). La modification n'en demande pas.
   */
  save(): void {
    this.onCompleteAssure();
    const customer = this.assureFormStepService.assure();
    if (customer.id) {
      this.enregistrer();
      return;
    }
    if (this.recapitulatifOuvert) {
      return;
    }
    this.recapitulatifOuvert = true;
    this.confirmDialog.onConfirm(
      () => {
        this.recapitulatifOuvert = false;
        this.enregistrer();
      },
      "Créer ce client ?",
      this.recapitulatif(customer),
      "pi pi-question-circle",
      () => (this.recapitulatifOuvert = false)
    );
  }

  private recapitulatif(customer: ICustomer): string {
    const identite = [customer.firstName, customer.lastName].filter(Boolean).join(" ");
    const carte = [customer.tiersPayant?.fullName, customer.num ? `n°${customer.num}` : null, customer.taux != null ? `${customer.taux} %` : null]
      .filter(Boolean)
      .join(" — ");
    const mutuelles = customer.tiersPayants?.length ? ` ; ${customer.tiersPayants.length} assurance(s) complémentaire(s)` : "";
    const ayants = customer.ayantDroits?.length
      ? ` ; ayant(s) droit : ${customer.ayantDroits.map(a => [a.firstName, a.lastName].filter(Boolean).join(" ")).join(", ")}`
      : "";
    return `${identite}${carte ? ` — ${carte}` : ""}${mutuelles}${ayants}.`;
  }

  private enregistrer(): void {
    this.isSaving.set(true);
    const customer = this.assureFormStepService.assure();
    if (customer.id !== undefined && customer.id) {
      customer.type = "ASSURE";
      this.subscribeToSaveResponse(this.customerService.update(customer));
    } else {
      this.subscribeToSaveResponse(this.customerService.create(customer));
    }
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<ICustomer>>): void {
    result
      .pipe(
        finalize(() => (this.isSaving.set(false))),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (res: HttpResponse<ICustomer>) => this.onSaveSuccess(res.body),
        error: (error: any) => this.onSaveError(error)
      });
  }

  private onSaveSuccess(customer: ICustomer | null): void {
    this.activeModal.close(customer);
  }
}
