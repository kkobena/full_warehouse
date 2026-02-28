import { inject, Injectable, NgZone } from '@angular/core';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { NgbConfirmDialogContentComponent } from './ngb-confirm-dialog.component';

/**
 * Expose la même API (onConfirm, onWarn) pour faciliter la migration progressive.
 *
 * Usage :
 *   readonly confirmDialog = inject(NgbConfirmDialogService);
 *   this.confirmDialog.onConfirm(() => { ... }, 'Titre', 'Message');
 */
@Injectable({ providedIn: 'root' })
export class NgbConfirmDialogService {
  private readonly modalService = inject(NgbModal);
  private readonly zone = inject(NgZone);
  private activeModalRef: NgbModalRef | null = null;

  onConfirm(acceptHandler: () => void, header?: string, message?: string, icon?: string, rejectHandler?: () => void): void {
    this.openModal(
      { header, message, icon },
      () => acceptHandler(),
      () => rejectHandler?.(),
    );
  }

  onWarn(rejectHandler: () => void, message?: string, header?: string, icon?: string): void {
    this.openModal(
      { header, message, icon: icon || 'pi pi-exclamation-triangle' },
      () => rejectHandler(),
      () => rejectHandler(),
    );
  }

  private openModal(
    config: { header?: string; message?: string; icon?: string },
    onAccept: () => void,
    onReject: () => void,
  ): void {
    // Fermer uniquement le modal confirm précédent (pas les autres modals NgbModal)
    if (this.activeModalRef) {
      this.activeModalRef.dismiss('replaced');
      this.activeModalRef = null;
    }

    // NgZone.run garantit la change detection quand appelé depuis un effect() Angular
    this.zone.run(() => {
      const modalRef = this.modalService.open(NgbConfirmDialogContentComponent, {
        backdrop: 'static',
        centered: true,
        windowClass: 'confirm-dialog-modal',
      });
      this.activeModalRef = modalRef;

      const instance = modalRef.componentInstance as NgbConfirmDialogContentComponent;
      instance.header = config.header || '';
      instance.message = config.message || '';
      if (config.icon) {
        instance.icon = config.icon;
      }

      modalRef.result.then(
        () => {
          if (this.activeModalRef === modalRef) {
            this.activeModalRef = null;
          }
          onAccept();
        },
        () => {
          if (this.activeModalRef === modalRef) {
            this.activeModalRef = null;
          }
          onReject();
        },
      );
    });
  }
}
