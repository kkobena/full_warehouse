import {ChangeDetectionStrategy, Component, inject, OnDestroy, OnInit} from "@angular/core";
import {FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators} from "@angular/forms";
import {ErrorService} from "../../../shared/error.service";
import {
  ClientTiersPayant,
  IClientTiersPayant
} from "../../../shared/model/client-tiers-payant.model";
import {ICustomer, ITiersPayant} from "../../../shared/model";
import {Observable, Subject} from "rxjs";
import {finalize, takeUntil} from "rxjs/operators";
import {TiersPayantService} from "../../tiers-payant/tierspayant.service";
import {CustomerService} from "../customer.service";
import {HttpResponse} from "@angular/common/http";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {NotificationService} from "../../../shared/services/notification.service";
import {
  ButtonComponent,
  CardComponent,
  KeyFilterDirective,
  SelectSearchComponent,
  SwitchComponent
} from '../../../shared/ui';
import TranslateDirective from "../../../shared/language/translate.directive";

@Component({
  selector: "app-customer-tiers-payant",
  imports: [
    FormsModule,
    ReactiveFormsModule,
    ButtonComponent,
    CardComponent,
    KeyFilterDirective,
    SelectSearchComponent,
    SwitchComponent,
    TranslateDirective
  ],
  templateUrl: "./customer-tiers-payant.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ["./customer-tiers-payant.component.scss"]
})
export class CustomerTiersPayantComponent implements OnInit, OnDestroy {
  title = "";
  entity: IClientTiersPayant | null = null;
  customer: ICustomer | null = null;

  protected fb = inject(UntypedFormBuilder);
  protected minLength = 3;
  protected editForm = this.fb.group({
    id: [],
    tiersPayant: [null, [Validators.required]],
    taux: [null, [Validators.required, Validators.min(5), Validators.max(100)]],
    num: [null, [Validators.required]],
    plafondConso: [],
    plafondJournalier: [],
    plafondAbsolu: []
  });
  protected isSaving = false;
  protected isValid = true;
  protected tiersPayants: ITiersPayant[] = [];
  private readonly errorService = inject(ErrorService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly customerService = inject(CustomerService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly notificationService = inject(NotificationService);
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    if (this.entity) {
      this.updateForm(this.entity);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  save(): void {
    this.isSaving = true;
    const clientTiersPayant = this.createFromForm();
    if (clientTiersPayant.id !== undefined && clientTiersPayant.id) {
      this.subscribeToSaveResponse(this.customerService.updateTiersPayant(clientTiersPayant));
    } else {
      this.subscribeToSaveResponse(this.customerService.addTiersPayant(clientTiersPayant));
    }
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  searchTiersPayant(query: string): void {
    this.loadTiersPayants(query);
  }

  loadTiersPayants(search?: string): void {
    const query: string = search || "";
    this.tiersPayantService
      .query({
        page: 0,
        size: 10,
        type: "ASSURANCE",
        search: query
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe((res: HttpResponse<ITiersPayant[]>) => {
        this.tiersPayants = res.body!;
        if (this.tiersPayants.length === 0) {
          this.tiersPayants.push({id: null, fullName: "Ajouter un nouveau tiers-payant"});
        }
      });
  }

  protected updateForm(clientTiersPayant: IClientTiersPayant): void {
    this.editForm.patchValue({
      id: clientTiersPayant.id,
      num: clientTiersPayant.num,
      tiersPayant: clientTiersPayant.tiersPayant,
      plafondConso: clientTiersPayant.plafondConso,
      plafondJournalier: clientTiersPayant.plafondJournalier,
      plafondAbsolu: clientTiersPayant.plafondAbsolu,
      taux: clientTiersPayant.taux
    });
  }

  protected createFromForm(): IClientTiersPayant {
    return {
      ...new ClientTiersPayant(),
      customerId: this.customer.id,
      id: this.editForm.get("id")?.value,
      num: this.editForm.get("num")?.value,
      tiersPayantId: this.editForm.get("tiersPayant")?.value?.id,
      plafondConso: this.editForm.get("plafondConso")?.value,
      plafondJournalier: this.editForm.get("plafondJournalier")?.value,
      plafondAbsolu: this.editForm.get("plafondAbsolu")?.value,
      taux: this.editForm.get("taux")?.value,
      priorite: this.computePriorite()
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ICustomer>>): void {
    result.pipe(finalize(() => (this.isSaving = false))).subscribe({
      next: (res: HttpResponse<ICustomer>) => this.onSaveSuccess(res.body),
      error: (error: any) => this.onSaveError(error)
    });
  }

  protected onSaveSuccess(iCustomer: ICustomer | null): void {
    this.isSaving = false;
    this.activeModal.close(iCustomer);
  }

  protected onSaveError(error: any): void {
    if (error.error?.errorKey) {
      this.errorService
        .getErrorMessageTranslation(error.error.errorKey)
        .pipe(takeUntil(this.destroy$))
        .subscribe(translatedErrorMessage => {
          this.notificationService.error(translatedErrorMessage);
        });
    } else {
      this.notificationService.error("Erreur interne du serveur.");
    }
  }

  private computePriorite(): number {
    return this.customer.tiersPayants.length > 0 ? this.customer.tiersPayants.length : 0;
  }
}
