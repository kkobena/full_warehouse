import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild, ChangeDetectionStrategy } from "@angular/core";
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from "@angular/forms";
import { CommonModule } from "@angular/common";
import { HttpErrorResponse, HttpResponse } from "@angular/common/http";
import { Observable } from "rxjs";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { ErrorService } from "../../../../shared/error.service";
import { NotificationService } from "../../../../shared/services/notification.service";
import { FournisseurApiService } from "../../data-access/services/fournisseur-api.service";
import { Fournisseur, IFournisseur } from "../../../../shared/model/fournisseur.model";
import { ButtonComponent, CardComponent, KeyFilterDirective, SelectComponent } from "../../../../shared/ui";

@Component({
  selector: "app-fournisseur-form",
  templateUrl: "./fournisseur-form.component.html",
  styleUrl: "./fournisseur-form.component.scss",
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonComponent,
    KeyFilterDirective,
    SelectComponent,
    CardComponent
  ]
})
export class FournisseurFormComponent implements OnInit, AfterViewInit {
  fournisseur: IFournisseur | null = null;
  presetParentId: number | null = null;
  title = "Fournisseur";

  protected parents: IFournisseur[] = [];
  protected isSaving = false;
  protected isAgenceMode = false;
  protected readonly blockSpace = /[^\s]/;

  protected readonly fb = inject(UntypedFormBuilder);
  protected readonly editForm = this.fb.group({
    id: [],
    code: [],
    libelle: [null, [Validators.required]],
    addresspostale: [],
    phone: [],
    mobile: [],
    parentId: [],
    delaiLivraisonJours: [],
    frequenceCommandeJours: [],
    identifiantRepartiteur: [],
    joursCredit: [],
    joursCritique: [],
    palierRfa: [],
    tauxRfa: [],
    urlPharmaMl: [],
    codeOfficePharmaMl: [],
    codeRecepteurPharmaMl: [],
    idRecepteurPharmaMl: []
  });

  private readonly api = inject(FournisseurApiService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly notif = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly libelleInput = viewChild.required<ElementRef>("libelleInput");

  ngOnInit(): void {
    this.isAgenceMode = this.presetParentId != null || (this.fournisseur?.parentId != null);
    this.loadParents();
    if (this.fournisseur) {
      this.updateForm(this.fournisseur);
    } else if (this.presetParentId != null) {
      this.editForm.patchValue({ parentId: this.presetParentId });
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.libelleInput().nativeElement.focus(), 100);
  }

  protected save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();
    const obs: Observable<IFournisseur> = entity.id ? this.api.update(entity) : this.api.create(entity);
    obs.subscribe({
      next: () => {
        this.isSaving = false;
        this.activeModal.close();
      },
      error: (err: HttpErrorResponse) => {
        this.isSaving = false;
        this.notif.error(this.errorService.getErrorMessage(err));
      }
    });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  private loadParents(): void {
    this.api.queryParents({ page: 0, size: 9999 }).subscribe({
      next: (res: HttpResponse<IFournisseur[]>) => {
        this.parents = res.body ?? [];
      }
    });
  }

  private updateForm(f: IFournisseur): void {
    this.editForm.patchValue({
      id: f.id,
      code: f.code,
      libelle: f.libelle,
      parentId: f.parentId ?? null,
      addresspostale: f.addressePostal,
      phone: f.phone,
      mobile: f.mobile,
      identifiantRepartiteur: f.identifiantRepartiteur,
      delaiLivraisonJours: f.delaiLivraisonJours,
      frequenceCommandeJours: f.frequenceCommandeJours,
      joursCredit: f.joursCredit,
      joursCritique: f.joursCritique,
      palierRfa: f.palierRfa,
      tauxRfa: f.tauxRfa,
      urlPharmaMl: f.urlPharmaMl,
      codeOfficePharmaMl: f.codeOfficePharmaMl,
      codeRecepteurPharmaMl: f.codeRecepteurPharmaMl,
      idRecepteurPharmaMl: f.idRecepteurPharmaMl
    });
  }

  private createFromForm(): IFournisseur {
    return {
      ...new Fournisseur(),
      id: this.editForm.get(["id"])!.value,
      code: this.editForm.get(["code"])!.value,
      libelle: this.editForm.get(["libelle"])!.value,
      parentId: this.editForm.get(["parentId"])!.value ?? null,
      addressePostal: this.editForm.get(["addresspostale"])!.value,
      phone: this.editForm.get(["phone"])!.value,
      mobile: this.editForm.get(["mobile"])!.value,
      delaiLivraisonJours: this.editForm.get(["delaiLivraisonJours"])!.value,
      frequenceCommandeJours: this.editForm.get(["frequenceCommandeJours"])!.value,
      identifiantRepartiteur: this.editForm.get(["identifiantRepartiteur"])!.value,
      joursCredit: this.editForm.get(["joursCredit"])!.value ?? null,
      joursCritique: this.editForm.get(["joursCritique"])!.value ?? null,
      palierRfa: this.editForm.get(["palierRfa"])!.value ?? null,
      tauxRfa: this.editForm.get(["tauxRfa"])!.value ?? null,
      urlPharmaMl: this.editForm.get(["urlPharmaMl"])!.value ?? null,
      codeOfficePharmaMl: this.editForm.get(["codeOfficePharmaMl"])!.value ?? null,
      codeRecepteurPharmaMl: this.editForm.get(["codeRecepteurPharmaMl"])!.value ?? null,
      idRecepteurPharmaMl: this.editForm.get(["idRecepteurPharmaMl"])!.value ?? null
    };
  }
}
