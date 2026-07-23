import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  OnInit,
  viewChild
} from "@angular/core";
import {IProduit} from "../../../shared/model";
import {
  FournisseurProduit,
  IFournisseurProduit
} from "../../../shared/model/fournisseur-produit.model";
import {ProduitService} from "../produit.service";
import {ErrorService} from "../../../shared/error.service";
import {FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators} from "@angular/forms";

import {IFournisseur} from "../../../shared/model/fournisseur.model";
import {HttpErrorResponse, HttpResponse} from "@angular/common/http";
import {Observable} from "rxjs";
import {CommonModule} from "@angular/common";
import {NotificationService} from "../../../shared/services/notification.service";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {finalize} from "rxjs/operators";
import {
  FournisseurApiService
} from "../../../features/partners/data-access/services/fournisseur-api.service";
import {
  ButtonComponent,
  CardComponent,
  KeyFilterDirective,
  SelectComponent,
  SwitchComponent
} from "../../../shared/ui";

@Component({
  selector: "app-form-produit-fournisseur",
  templateUrl: "./form-produit-fournisseur.component.html",
  styleUrls: ["./form-produit.scss"],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonComponent,
    KeyFilterDirective,
    SelectComponent,
    SwitchComponent,
    CardComponent
  ]
})
export class FormProduitFournisseurComponent implements OnInit, AfterViewInit {
  header = "";
  produit?: IProduit;
  entity?: IFournisseurProduit;
  protected fb = inject(UntypedFormBuilder);
  protected isSaving = false;
  protected isValid = true;
  protected fournisseurSelectedId!: number;
  protected fournisseurs: IFournisseur[] = [];
  protected editForm = this.fb.group({
    id: [],
    prixUni: [null, [Validators.required, Validators.min(1)]],
    prixAchat: [null, [Validators.required, Validators.min(1)]],
    codeCip: [null, [Validators.required, Validators.minLength(6), Validators.maxLength(8)]],
    fournisseurId: [null, [Validators.required]],
    principal: [false, [Validators.required]],
    qteColis: [1, [Validators.min(1)]],
    qteMinimaleCommande: [0, [Validators.min(0)]]
  });
  private readonly produitService = inject(ProduitService);
  private readonly errorService = inject(ErrorService);
  private readonly fournisseurService = inject(FournisseurApiService);
  private readonly notificationService = inject(NotificationService);
  private fournisseurSelect = viewChild.required("fournisseurSelect", {read: ElementRef<HTMLElement>});
  private readonly activeModal = inject(NgbActiveModal);

  save(): void {
    this.isSaving = true;
    const produitFournisseur = this.createFrom();
    if (produitFournisseur.id !== undefined && produitFournisseur.id) {
      this.subscribeToSaveResponse(this.produitService.updateProduitFournisseur(produitFournisseur));
    } else {
      this.subscribeToSaveResponse(this.produitService.createProduitFournisseur(produitFournisseur));
    }
  }

  ngOnInit(): void {
    if (this.entity) {
      this.updateForm(this.entity);
      if (this.entity.fournisseurId) {
        this.fournisseurSelectedId = this.entity.fournisseurId;
      }
    }

    this.populate();
    if (this.entity && this.produit) {
      this.editForm.get("principal").setValue(this.entity.id === this.produit.fournisseurProduit?.id);
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.fournisseurSelect().nativeElement.querySelector('input')?.focus();
    }, 100);
  }

  updateForm(produitFournisseur: IFournisseurProduit): void {
    this.editForm.patchValue({
      id: produitFournisseur.id,
      prixUni: produitFournisseur.prixUni,
      prixAchat: produitFournisseur.prixAchat,
      codeCip: produitFournisseur.codeCip,
      fournisseurId: produitFournisseur.fournisseurId,
      produitId: this.produit.id,
      principal: this.produit.fournisseurProduit?.id === produitFournisseur.id,
      qteColis: produitFournisseur.qteColis ?? 1,
      qteMinimaleCommande: produitFournisseur.qteMinimaleCommande ?? 0
    });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  protected populate(): void {
    this.fournisseurService
      .queryParents({
        page: 0,
        size: 9999
      })
      .subscribe((res: HttpResponse<IFournisseur[]>) => {
        if (this.entity) {
          this.fournisseurs = res.body || [];
        } else {
          this.fournisseurs = res.body.filter(p => !this.produit?.fournisseurProduits?.some(fp => fp.fournisseurId === p.id)) || [];
        }
      });
  }

  protected handlePrixAchatInput(event: any): void {
    const value = Number(event.target.value);
    const unitPrice = Number(this.editForm.get(["prixUni"]).value);
    this.isValid = value < unitPrice;
  }

  protected handlePrixUnitaireInput(event: any): void {
    const value = Number(event.target.value);
    const costAmount = Number(this.editForm.get(["prixAchat"]).value);
    this.isValid = costAmount < value;
  }

  protected onChange(fournisseurId: number): void {
    this.fournisseurSelectedId = fournisseurId;
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<IFournisseurProduit>>): void {
    result.pipe(finalize(() => (this.isSaving = false))).subscribe({
      next: (res: HttpResponse<IFournisseurProduit>) => this.onSaveSuccess(res.body),
      error: error => this.onSaveError(error)
    });
  }

  private onSaveSuccess(produitFournisseur: IFournisseurProduit | null): void {
    this.activeModal.close({
      ...produitFournisseur,
      principal: this.editForm.get(["principal"]).value
    });
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.notificationService.error(this.errorService.getErrorMessage(error));
  }

  private createFrom(): IFournisseurProduit {
    return {
      ...new FournisseurProduit(),
      id: this.editForm.get(["id"]).value,
      prixUni: this.editForm.get(["prixUni"]).value,
      prixAchat: this.editForm.get(["prixAchat"]).value,
      codeCip: this.editForm.get(["codeCip"]).value,
      fournisseurId: this.editForm.get(["fournisseurId"]).value,
      principal: this.editForm.get(["principal"]).value,
      produitId: this.produit.id,
      qteColis: this.editForm.get(["qteColis"]).value ?? 1,
      qteMinimaleCommande: this.editForm.get(["qteMinimaleCommande"]).value ?? 0
    };
  }
}
