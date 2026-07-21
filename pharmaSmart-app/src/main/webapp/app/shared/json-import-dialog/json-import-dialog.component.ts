import {ChangeDetectionStrategy, Component, inject, signal} from "@angular/core";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {ButtonComponent, CardComponent, FileUploadComponent} from "../ui";

/**
 * Import d'un fichier JSON de tiers-payants.
 */
@Component({
  selector: "app-json-import-dialog",
  templateUrl: "./json-import-dialog.component.html",
  styleUrls: ["./json-import-dialog.component.scss"],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [ButtonComponent, CardComponent, FileUploadComponent]
})
export class JsonImportDialogComponent {
  accept = ".json";

  /** Fichier retenu, en attente de confirmation par le pied de la modale. */
  protected readonly selectedFile = signal<File | null>(null);

  /** Motif de rejet remonté par app-file-upload. */
  protected readonly error = signal<string>("");

  private readonly activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss("cancel");
  }

  protected onFilesSelected(files: File[]): void {
    this.error.set("");
    this.selectedFile.set(files[0] ?? null);
  }

  protected onRejected(reason: string): void {
    this.selectedFile.set(null);
    this.error.set(reason);
  }

  protected upload(): void {
    const file = this.selectedFile();
    if (!file) {
      return;
    }

    // Le nom de champ `importjson` est celui attendu par TiersPayantService.uploadJsonData.
    const formData: FormData = new FormData();
    formData.append("importjson", file, file.name);
    this.activeModal.close(formData);
  }
}
