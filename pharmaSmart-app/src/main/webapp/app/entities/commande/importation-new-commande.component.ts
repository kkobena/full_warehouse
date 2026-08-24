import { Component, inject, OnInit, viewChild, ChangeDetectionStrategy, signal } from "@angular/core";
import { IFournisseur } from "../../shared/model/fournisseur.model";
import { CommandeService } from "./commande.service";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
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
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, FileUploadComponent, ButtonComponent, FournisseurSelectComponent, CardComponent, SpinnerComponent, SelectComponent]
})
export class ImportationNewCommandeComponent implements OnInit {
  header = "";
  protected readonly isSaving = signal(false);
  fournisseurSelectedId!: number;
  modelSelected!: string;
  protected readonly models = signal<any[] | undefined>(undefined);
  protected readonly file = signal<any | undefined>(undefined);
  commandeResponse!: ICommandeResponse | null;
  private readonly commandeService = inject(CommandeService);
  private readonly spinner = viewChild.required<SpinnerComponent>("spinner");
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly activeModal = inject(NgbActiveModal);

  constructor() {
    this.models.set([
      { label: "LABOREX", value: "LABOREX" },
      { label: "COPHARMED", value: "COPHARMED" },
      { label: "DPCI", value: "DPCI" },
      { label: "TEDIS", value: "TEDIS" },
      { label: "Cip  quantité", value: "CIP_QTE" },
      { label: "Cip quantité prix achat", value: "CIP_QTE_PA" }
    ]);
  }

  ngOnInit(): void {
  }

  protected onFournisseurSelected(f: IFournisseur | null): void {
    this.fournisseurSelectedId = f?.id ?? null!;
  }

  protected save(): void {
    this.isSaving.set(true);
    const formData: FormData = new FormData();
    const file = this.file();

    formData.append("commande", file, file.name);
    this.spinner().show();
    this.commandeService
      .uploadNewCommande(this.fournisseurSelectedId, this.modelSelected, formData)
      .pipe(
        finalize(() => {
          this.spinner().hide();
          this.isSaving.set(false);
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
    this.file.set(files[0] ?? null);
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  protected isValidForm(): boolean {
    return !!this.file() && !!this.modelSelected && !!this.fournisseurSelectedId;
  }

  private onCommonError(error: HttpErrorResponse): void {
    this.notificationService.error(this.errorService.getErrorMessage(error));
  }
}
