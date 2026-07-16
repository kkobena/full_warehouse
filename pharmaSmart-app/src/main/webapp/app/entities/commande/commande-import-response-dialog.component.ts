import { Component, inject, OnDestroy } from "@angular/core";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { ICommandeResponse } from "../../shared/model/commande-response.model";
import { CommandeService } from "./commande.service";
import { ButtonModule } from "primeng/button";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { CommonModule } from "@angular/common";
import { BlobDownloadService } from "../../shared/services/blob-download.service";

@Component({
  selector: "app-commande-import-response-dialog",
  templateUrl: "./commande-import-response-dialog.component.html",
  styleUrls: ["./commande-import-response-dialog.component.scss"],
  imports: [ButtonModule, CommonModule]
})
export class CommandeImportResponseDialogComponent implements OnDestroy {
  responseCommande?: ICommandeResponse;
  hiddenInfo = true;
  private activeModal = inject(NgbActiveModal);
  private commandeService = inject(CommandeService);
  private destroy$ = new Subject<void>();
  private readonly blobDownloadService = inject(BlobDownloadService);

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  onClickLink(): void {
    this.commandeService
      .getRuptureCsv(this.responseCommande.reference)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: blod => {
          this.blobDownloadService.downloadCsv(blod, `${this.responseCommande.reference}`);
          this.hiddenInfo = false;
        }
      });
  }
}
