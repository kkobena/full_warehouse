import { Component, inject, OnInit, viewChild, ChangeDetectionStrategy } from "@angular/core";
import { IFournisseur } from "../../shared/model/fournisseur.model";
import { CommandeService } from "./commande.service";
import { NgbActiveModal, NgbModal } from "@ng-bootstrap/ng-bootstrap";
import { ErrorService } from "../../shared/error.service";
import { ICommandeResponse } from "../../shared/model/commande-response.model";
import { HttpErrorResponse } from "@angular/common/http";
import { FormsModule } from "@angular/forms";
import { ButtonComponent, CardComponent, FileUploadComponent, SelectComponent } from "../../shared/ui";
import { FournisseurSelectComponent } from "../../features/partners/ui/fournisseur-select/fournisseur-select.component";
import { finalize } from "rxjs/operators";
import { NotificationService } from "../../shared/services/notification.service";
import { SpinnerComponent } from "../../shared/spinner/spinner.component";
import { CommonModule } from "@angular/common";


@Component({
  selector: "app-importation-new-commande",
  templateUrl: "./importation-new-commande.component.html",
  styleUrls: ["./form-import-new.scss"],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, FormsModule, FileUploadComponent, ButtonComponent, FournisseurSelectComponent, CardComponent, SpinnerComponent, SelectComponent]
})
export class ImportationNewCommandeComponent implements OnInit {
  header = "";
  protected isSaving = false;
  fournisseurSelectedId!: number;
  modelSelected!: string;
  models: any[];
  file: any;
  commandeResponse!: ICommandeResponse | null;
  private readonly commandeService = inject(CommandeService);
  protected readonly modalService = inject(NgbModal);
  private readonly spinner = viewChild.required<SpinnerComponent>("spinner");
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly activeModal = inject(NgbActiveModal);

  constructor() {
    this.models = [
      { label: "LABOREX", value: "LABOREX" },
      { label: "COPHARMED", value: "COPHARMED" },
      { label: "DPCI", value: "DPCI" },
      { label: "TEDIS", value: "TEDIS" },
      { label: "Cip  quantité", value: "CIP_QTE" },
      { label: "Cip quantité prix achat", value: "CIP_QTE_PA" }
    ];
  }

  ngOnInit(): void {
  }

  protected onFournisseurSelected(f: IFournisseur | null): void {
    this.fournisseurSelectedId = f?.id ?? null!;
  }

  protected save(): void {
    this.isSaving = true;
    const formData: FormData = new FormData();
    const file = this.file;

    formData.append("commande", file, file.name);
    this.spinner().show();
    this.commandeService
      .uploadNewCommande(this.fournisseurSelectedId, this.modelSelected, formData)
      .pipe(
        finalize(() => {
          this.spinner().hide();
          this.isSaving = false;
        })
      )
      .subscribe({
        next: res => {
          this.commandeResponse = res.body;
          this.cancel();
        },
        error: error => {
          this.onCommonError(error);
        }
      });
  }

  protected onFilesSelected(files: File[]): void {
    this.file = files[0] ?? null;
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  protected isValidForm(): boolean {
    return !!this.file && !!this.modelSelected && !!this.fournisseurSelectedId;
  }

  private onCommonError(error: HttpErrorResponse): void {
    this.notificationService.error(this.errorService.getErrorMessage(error));
  }
}
