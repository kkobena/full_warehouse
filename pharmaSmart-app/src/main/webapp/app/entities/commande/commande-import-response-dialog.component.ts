import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { ICommandeResponse } from "../../shared/model/commande-response.model";
import { ButtonComponent } from "../../shared/ui";
import { CommonModule } from "@angular/common";
import { BlobDownloadService } from "../../shared/services/blob-download.service";

@Component({
  selector: "app-commande-import-response-dialog",
  templateUrl: "./commande-import-response-dialog.component.html",
  styleUrls: ["./commande-import-response-dialog.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonComponent, CommonModule]
})
export class CommandeImportResponseDialogComponent {
  responseCommande?: ICommandeResponse;
  private activeModal = inject(NgbActiveModal);
  private readonly blobDownloadService = inject(BlobDownloadService);

  cancel(): void {
    this.activeModal.dismiss();
  }

  /** Le CSV des lignes non prises en compte arrive avec la réponse d'import (base64) : rien à redemander au serveur. */
  onClickLink(): void {
    const contenu = this.responseCommande?.ruptureCsv;
    if (!contenu) {
      return;
    }
    const octets = Uint8Array.from(atob(contenu), caractere => caractere.charCodeAt(0));
    this.blobDownloadService.downloadCsv(new Blob([octets], { type: "text/csv;charset=utf-8" }), `${this.responseCommande?.reference}`);
  }
}
