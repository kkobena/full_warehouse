import { Component, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { FileUploadHandlerEvent, FileUploadModule } from 'primeng/fileupload';
import { ButtonModule } from 'primeng/button';
import { CommonModule } from '@angular/common';
import { Card } from 'primeng/card';

@Component({
  selector: 'jhi-file-upload-dialog',
  templateUrl: './file-upload-dialog.component.html',
  styleUrls: ['./file-upload-dialog.component.scss'],
  imports: [CommonModule, FileUploadModule, ButtonModule, Card],
})
export class FileUploadDialogComponent {
  private readonly activeModal = inject(NgbActiveModal);

  onUpload(event: FileUploadHandlerEvent): void {
    const formData: FormData = new FormData();
    const file = event.files[0];
    formData.append('importcsv', file, file.name);
    this.activeModal.close(formData);
  }

  cancel(): void {
    this.activeModal.dismiss('cancel');
  }
}
