import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {CommonModule} from '@angular/common';
import {ButtonComponent, CardComponent, FileUploadComponent} from '../../../shared/ui';

@Component({
  selector: 'app-file-upload-dialog',
  templateUrl: './file-upload-dialog.component.html',
  styleUrls: ['./file-upload-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, ButtonComponent, CardComponent, FileUploadComponent],
})
export class FileUploadDialogComponent {
  accept = '.txt,.csv,.xls,.xlsx';

  /** Fichier retenu, en attente de confirmation par le pied de la modale. */
  protected readonly selectedFile = signal<File | null>(null);

  /** Motif de rejet remonté par app-file-upload (taille, etc.). */
  protected readonly error = signal<string>('');

  private readonly activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss('cancel');
  }

  protected onFilesSelected(files: File[]): void {
    this.error.set('');
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

    const formData: FormData = new FormData();
    formData.append('importcsv', file, file.name);
    this.activeModal.close(formData);
  }
}
