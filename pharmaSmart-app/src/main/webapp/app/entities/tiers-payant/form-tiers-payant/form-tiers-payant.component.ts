import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  OnDestroy,
  OnInit,
  viewChild, signal } from "@angular/core";
import {LowerCasePipe} from "@angular/common";
import {FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators} from "@angular/forms";
import {ErrorService} from "app/shared/error.service";
import {TiersPayantService} from "app/entities/tiers-payant/tierspayant.service";
import {
  GroupeTiersPayantService
} from "app/entities/groupe-tiers-payant/groupe-tierspayant.service";
import {ITiersPayant, ModelFacture, TiersPayant} from "app/shared/model/tierspayant.model";
import {IGroupeTiersPayant} from "app/shared/model/groupe-tierspayant.model";
import {HttpResponse} from "@angular/common/http";
import {Observable, Subject, takeUntil} from "rxjs";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {NotificationService} from "../../../shared/services/notification.service";
import {NgbConfirmDialogService} from "../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import {currencySymbol} from "app/shared/utils/format-utils";
import {
  BadgeComponent,
  ButtonComponent,
  CardComponent,
  InputNumberComponent,
  KeyFilterDirective,
  SelectComponent,
  SelectSearchComponent,
  SwitchComponent
} from "../../../shared/ui";

@Component({
  selector: "app-form-tiers-payant",
  templateUrl: "./form-tiers-payant.component.html",
  styleUrls: ["./form-tiers-payant.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    LowerCasePipe,
    BadgeComponent,
    ButtonComponent,
    CardComponent,
    InputNumberComponent,
    KeyFilterDirective,
    SelectComponent,
    SelectSearchComponent,
    SwitchComponent
  ]
})
export class FormTiersPayantComponent implements OnInit, AfterViewInit, OnDestroy {
  entity?: ITiersPayant;
  title?: string;
  categorie?: string | null = null;
  /**
   * Création « à chaud » depuis une vente ou une fiche client : l'identité de l'organisme et rien de plus.
   * Le paramétrage de la facturation et des plafonds se complète ensuite depuis la gestion des tiers payants.
   */
  modeExpress = false;
  protected fb = inject(UntypedFormBuilder);
  protected name = viewChild.required<ElementRef>("name");
  protected readonly isSaving = signal(false);
  protected isValid = true;
  /** Unité affichée à côté des montants. */
  protected readonly devise = " " + currencySymbol();
  private readonly libellesCategorie: Record<string, string> = {ASSURANCE: "Assurance", CARNET: "Carnet", DEPOT: "Dépôt"};
  protected readonly groupeTiersPayants = signal<IGroupeTiersPayant[]>([]);
  /** Sujet requis par `[typeahead]` ; les termes sont traités par `(searched)` (même montage que le formulaire assuré). */
  protected readonly saisieGroupe$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();
  /** Groupe déjà rattaché à l'organisme : il doit rester dans la liste, sinon son libellé disparaît dès qu'une recherche l'écarte. */
  private groupeCourant?: IGroupeTiersPayant;
  protected readonly modelFacture = signal<ModelFacture[]>([]);
  protected readonly periodicitesOptions = [
    {label: "Mensuel", value: "MENSUEL"},
    {label: "Quinzainière", value: "QUINZAINE"},
    {label: "Bimensuel", value: "BIMENSUEL"}
  ];
  protected editForm = this.fb.group({
    id: [],
    name: [null, [Validators.required]],
    fullName: [null, [Validators.required]],
    telephone: [null, [Validators.required]],
    email: [null, [Validators.email]],
    ncc: [],
    tauxCouvertureDefaut: [null, [Validators.min(0), Validators.max(100)]],
    groupeTiersPayantId: [],
    codeOrganisme: [],
    montantMaxParFcture: [],
    nbreBordereaux: [1],
    plafondConso: [],
    plafondAbsolu: [],
    modelFacture: [],
    toBeExclude: [],
    plafondConsoClient: [],
    plafondJournalierClient: [],
    plafondAbsoluClient: [],
    delaiReglement: [30],
    periodiciteFactureDefinitive: [null],
    inclureFacturationAutoDefinitive: [true],
    periodiciteFactureProvisoire: [null],
    inclureFacturationAutoProvisoire: [true]
  });
  private readonly errorService = inject(ErrorService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly groupeTiersPayantService = inject(GroupeTiersPayantService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);

  ngOnInit(): void {
    this.saisieGroupe$.pipe(takeUntil(this.destroy$)).subscribe();
    if (this.entity) {
      this.updateForm(this.entity);
    }
    this.loadModelFacture();
    this.populate().then(r => {
      this.groupeTiersPayants.set(this.avecGroupeCourant(r));
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.saisieGroupe$.complete();
  }

  /** Recherche des groupes côté serveur : la liste complète ne se parcourt plus à l'œil quand les groupes se multiplient. */
  protected rechercherGroupes(terme: string): void {
    this.groupeTiersPayantService
      .query({search: terme ?? ""})
      .pipe(takeUntil(this.destroy$))
      .subscribe(res => this.groupeTiersPayants.set(this.avecGroupeCourant(res.body ?? [])));
  }

  private avecGroupeCourant(groupes: IGroupeTiersPayant[]): IGroupeTiersPayant[] {
    const courant = this.groupeCourant;
    return courant && !groupes.some(groupe => groupe.id === courant.id) ? [courant, ...groupes] : groupes;
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.name().nativeElement.focus();
    }, 100);
  }

  async populate(): Promise<IGroupeTiersPayant[]> {
    return await this.groupeTiersPayantService.queryPromise({search: ""});
  }

  loadModelFacture(): void {
    this.tiersPayantService.getModelFacture().subscribe(res => {
      this.modelFacture.set(res.body);
    });
  }

  /** Libellé de la catégorie, affiché en en-tête : c'est elle qui décide des sections présentes. */
  protected get libelleCategorie(): string {
    return this.libellesCategorie[this.categorie ?? ""] ?? "";
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  /**
   * Garde de fermeture (voir `showCommonModal`) : une saisie commencée ne part pas sur un Échap
   * involontaire. Sans modification, on ferme sans rien demander.
   */
  confirmerFermeture(): boolean | Promise<boolean> {
    if (!this.editForm.dirty || this.isSaving()) {
      return true;
    }
    return new Promise<boolean>(resolve =>
      this.confirmDialog.onConfirm(
        () => resolve(true),
        "Abandonner la saisie",
        "Les informations saisies seront perdues. Fermer le formulaire ?",
        undefined,
        () => resolve(false)
      )
    );
  }

  save(): void {
    this.isSaving.set(true);
    const tiersPayant = this.createFromForm();
    if (tiersPayant.id !== undefined && tiersPayant.id) {
      this.subscribeToSaveResponse(this.tiersPayantService.update(tiersPayant));
    } else {
      this.subscribeToSaveResponse(this.tiersPayantService.create(tiersPayant));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ITiersPayant>>): void {
    result.subscribe({
      next: (res: HttpResponse<ITiersPayant>) => this.onSaveSuccess(res.body),
      error: (res: any) => this.onSaveError(res)
    });
  }

  protected onSaveSuccess(tiersPayant: ITiersPayant | null): void {
    this.isSaving.set(false);
    this.activeModal.close(tiersPayant);
  }

  protected onSaveError(error: any): void {
    this.isSaving.set(false);
    this.notificationService.error(this.errorService.getErrorMessage(error));
  }

  private updateForm(tiersPayant: ITiersPayant): void {
    this.groupeCourant = tiersPayant.groupeTiersPayant as IGroupeTiersPayant | undefined;
    this.editForm.patchValue({
      id: tiersPayant.id,
      name: tiersPayant.name,
      fullName: tiersPayant.fullName,
      telephone: tiersPayant.telephone,
      email: tiersPayant.email,
      groupeTiersPayantId: tiersPayant.groupeTiersPayant?.id,
      codeOrganisme: tiersPayant.codeOrganisme,
      tauxCouvertureDefaut: tiersPayant.tauxCouvertureDefaut,
      montantMaxParFcture: tiersPayant.montantMaxParFcture,
      nbreBordereaux: tiersPayant.nbreBordereaux,
      plafondConso: tiersPayant.plafondConso,
      plafondAbsolu: tiersPayant.plafondAbsolu,
      categorie: tiersPayant.categorie,
      modelFacture: tiersPayant.modelFacture,
      ordreTrisFacture: tiersPayant.ordreTrisFacture,
      toBeExclude: tiersPayant.toBeExclude,
      plafondConsoClient: tiersPayant.plafondConsoClient,
      plafondJournalierClient: tiersPayant.plafondJournalierClient,
      plafondAbsoluClient: tiersPayant.plafondAbsoluClient,
      ncc: tiersPayant.ncc,
      delaiReglement: tiersPayant.delaiReglement ?? 30,
      periodiciteFactureDefinitive: tiersPayant.periodiciteFactureDefinitive ?? null,
      inclureFacturationAutoDefinitive: tiersPayant.inclureFacturationAutoDefinitive ?? true,
      periodiciteFactureProvisoire: tiersPayant.periodiciteFactureProvisoire ?? null,
      inclureFacturationAutoProvisoire: tiersPayant.inclureFacturationAutoProvisoire ?? true
    });
  }

  private createFromForm(): ITiersPayant {
    return {
      ...new TiersPayant(),
      id: this.editForm.get(["id"]).value,
      name: this.editForm.get(["name"]).value,
      fullName: this.editForm.get(["fullName"]).value,
      telephone: this.editForm.get(["telephone"]).value,
      email: this.editForm.get(["email"]).value,
      nbreBordereaux: this.editForm.get(["nbreBordereaux"]).value,
      categorie: this.categorie,
      plafondAbsolu: this.editForm.get(["plafondAbsolu"]).value,
      plafondConso: this.editForm.get(["plafondConso"]).value,
      montantMaxParFcture: this.editForm.get(["montantMaxParFcture"]).value,
      codeOrganisme: this.editForm.get(["codeOrganisme"]).value,
      groupeTiersPayantId: this.editForm.get(["groupeTiersPayantId"]).value,
      modelFacture: this.editForm.get(["modelFacture"]).value,
      toBeExclude: this.editForm.get(["toBeExclude"]).value,
      plafondConsoClient: this.editForm.get(["plafondConsoClient"]).value,
      plafondJournalierClient: this.editForm.get(["plafondJournalierClient"]).value,
      plafondAbsoluClient: this.editForm.get(["plafondAbsoluClient"]).value,
      ncc: this.editForm.get(["ncc"]).value,
      tauxCouvertureDefaut: this.editForm.get(["tauxCouvertureDefaut"]).value,
      delaiReglement: this.editForm.get(["delaiReglement"]).value,
      periodiciteFactureDefinitive: this.editForm.get(["periodiciteFactureDefinitive"]).value,
      inclureFacturationAutoDefinitive: this.editForm.get(["inclureFacturationAutoDefinitive"]).value,
      periodiciteFactureProvisoire: this.editForm.get(["periodiciteFactureProvisoire"]).value,
      inclureFacturationAutoProvisoire: this.editForm.get(["inclureFacturationAutoProvisoire"]).value
    };
  }
}
