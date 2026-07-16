import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild } from "@angular/core";
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from "@angular/forms";
import { GroupeTiersPayant, IGroupeTiersPayant } from "app/shared/model/groupe-tierspayant.model";
import { ErrorService } from "app/shared/error.service";
import { GroupeTiersPayantService } from "app/entities/groupe-tiers-payant/groupe-tierspayant.service";
import { Observable } from "rxjs";
import { HttpResponse } from "@angular/common/http";
import { ButtonModule } from "primeng/button";
import { InputTextModule } from "primeng/inputtext";
import { RippleModule } from "primeng/ripple";
import { KeyFilterModule } from "primeng/keyfilter";
import { OrdreTrisFacture } from "../../../shared/model/tierspayant.model";
import { TiersPayantService } from "../../tiers-payant/tierspayant.service";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { LowerCasePipe } from "@angular/common";
import { Card } from "primeng/card";
import { Select } from "primeng/select";
import { InputNumber } from "primeng/inputnumber";
import { ToggleSwitch } from "primeng/toggleswitch";
import { NotificationService } from "../../../shared/services/notification.service";
import { Toast } from "primeng/toast";

@Component({
  selector: "app-form-groupe-tiers-payant",
  templateUrl: "./form-groupe-tiers-payant.component.html",
  styleUrls: ["./form-groupe-tiers-payant.component.scss"],
  imports: [
    FormsModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    KeyFilterModule,
    LowerCasePipe,
    Card,
    Select,
    InputNumber,
    ToggleSwitch,
    Toast
  ]
})
export class FormGroupeTiersPayantComponent implements OnInit, AfterViewInit {
  header = "";
  entity?: IGroupeTiersPayant;
  protected ordreTrisFacture: OrdreTrisFacture[] = [];
  protected readonly periodicitesOptions = [
    { label: "Mensuel", value: "MENSUEL" },
    { label: "Quinzainière", value: "QUINZAINE" },
    { label: "Bimensuel", value: "BIMENSUEL" }
  ];
  protected isSaving = false;
  protected isValid = true;
  protected fb = inject(UntypedFormBuilder);
  protected name = viewChild.required<ElementRef>("name");
  protected editForm = this.fb.group({
    id: [],
    name: [null, [Validators.required]],
    telephone: [null, [Validators.required]],
    email: [],
    adresse: [],
    telephoneFixe: [],
    ordreTrisFacture: [],
    delaiReglement: [30],
    periodiciteFactureDefinitive: [null],
    inclureFacturationAutoDefinitive: [true],
    periodiciteFactureProvisoire: [null],
    inclureFacturationAutoProvisoire: [true]
  });
  private readonly errorService = inject(ErrorService);
  private readonly groupeTiersPayantService = inject(GroupeTiersPayantService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly notificationService = inject(NotificationService);

  ngOnInit(): void {
    if (this.entity) {
      this.updateForm(this.entity);
    }
    this.loadOrdreTrisFacture();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.name().nativeElement.focus();
    }, 100);
  }

  loadOrdreTrisFacture(): void {
    this.tiersPayantService.getOrdreTrisFacture().subscribe(res => {
      this.ordreTrisFacture = res.body || [];
    });
  }

  updateForm(groupeTiersPayant: IGroupeTiersPayant): void {
    this.editForm.patchValue({
      id: groupeTiersPayant.id,
      name: groupeTiersPayant.name,
      telephone: groupeTiersPayant.telephone,
      email: groupeTiersPayant.email,
      adresse: groupeTiersPayant.adresse,
      telephoneFixe: groupeTiersPayant.telephoneFixe,
      ordreTrisFacture: groupeTiersPayant.ordreTrisFacture,
      delaiReglement: groupeTiersPayant.delaiReglement ?? 30,
      periodiciteFactureDefinitive: groupeTiersPayant.periodiciteFactureDefinitive ?? null,
      inclureFacturationAutoDefinitive: groupeTiersPayant.inclureFacturationAutoDefinitive ?? true,
      periodiciteFactureProvisoire: groupeTiersPayant.periodiciteFactureProvisoire ?? null,
      inclureFacturationAutoProvisoire: groupeTiersPayant.inclureFacturationAutoProvisoire ?? true
    });
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  save(): void {
    this.isSaving = true;
    const groupeTiersPayant = this.createFromForm();
    if (groupeTiersPayant.id !== undefined && groupeTiersPayant.id) {
      this.subscribeToSaveResponse(this.groupeTiersPayantService.update(groupeTiersPayant));
    } else {
      this.subscribeToSaveResponse(this.groupeTiersPayantService.create(groupeTiersPayant));
    }
  }

  protected createFromForm(): IGroupeTiersPayant {
    return {
      ...new GroupeTiersPayant(),
      id: this.editForm.get(["id"]).value,
      name: this.editForm.get(["name"]).value,
      telephone: this.editForm.get(["telephone"]).value,
      email: this.editForm.get(["email"]).value,
      adresse: this.editForm.get(["adresse"]).value,
      telephoneFixe: this.editForm.get(["telephoneFixe"]).value,
      ordreTrisFacture: this.editForm.get(["ordreTrisFacture"]).value,
      delaiReglement: this.editForm.get(["delaiReglement"]).value,
      periodiciteFactureDefinitive: this.editForm.get(["periodiciteFactureDefinitive"]).value,
      inclureFacturationAutoDefinitive: this.editForm.get(["inclureFacturationAutoDefinitive"]).value,
      periodiciteFactureProvisoire: this.editForm.get(["periodiciteFactureProvisoire"]).value,
      inclureFacturationAutoProvisoire: this.editForm.get(["inclureFacturationAutoProvisoire"]).value
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IGroupeTiersPayant>>): void {
    result.subscribe({
      next: res => this.onSaveSuccess(res.body),
      error: error => this.onSaveError(error)
    });
  }

  protected onSaveSuccess(groupeTiersPayant: IGroupeTiersPayant | null): void {
    this.isSaving = false;
    this.notificationService.success("Opération effectuée avec succès");
    this.activeModal.close(groupeTiersPayant);
  }

  protected onSaveError(error: any): void {
    this.isSaving = false;
    this.notificationService.error(this.errorService.getErrorMessage(error));
  }
}
