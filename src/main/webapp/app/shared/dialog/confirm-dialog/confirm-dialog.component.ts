import { Component, inject, input } from '@angular/core';
import { ConfirmationService } from 'primeng/api';
import { acceptButtonProps, rejectButtonProps, rejectWarningButtonProps } from '../../util/modal-button-props';

@Component({
  selector: 'jhi-confirm-dialog',
  template: '',
})
export class ConfirmDialogComponent {
  message = input<string>();
  header = input<string>();
  icon = input<string>('pi pi-info-circle');
  acceptHandler = input<() => void>();
  rejectHandler = input<() => void>();
  style = input<Record<string, any>>({ width: '40vw' });
  private readonly confirmationService = inject(ConfirmationService);

  private get accept(): HTMLButtonElement {
    return document.querySelector('.ws-dialog .p-confirmdialog-accept-button');
  }

  onConfirm(acceptHandler: () => void, header?: string, message?: string, icon?: string, rejectHandler?: () => void): void {
    this.confirmationService.confirm({
      message: message || this.message(),
      header: header || this.header(),
      icon: icon || this.icon(),
      acceptButtonProps: acceptButtonProps(),
      rejectButtonProps: rejectButtonProps(),
      defaultFocus: 'accept',
      accept: () => acceptHandler(),
      reject() {
        if (rejectHandler) {
          rejectHandler();
        }
      },
    });
    
    // Forcer le focus après le rendu
    setTimeout(() => {
      const buttons = document.querySelectorAll('.ws-dialog .p-button');
      // Le premier bouton devrait être Accept (car acceptButtonProps est en premier)
      const acceptButton = buttons[0] as HTMLButtonElement;
      if (acceptButton) {
        acceptButton.focus();
      }
    }, 100);
  }

  onWarn(rejectHandler: () => void, message?: string, header?: string, icon?: string): void {
    this.confirmationService.confirm({
      message: message || this.message(),
      header: header || this.header(),
      icon: icon || 'pi pi-exclamation-triangle',
      acceptVisible: false,
      rejectButtonProps: rejectWarningButtonProps(),
      defaultFocus: 'accept',
      reject: () => rejectHandler(),
    });
    
    // Forcer le focus sur le bouton après l'ouverture complète du dialog
    setTimeout(() => {
      const button = document.querySelector('.ws-dialog .p-confirmdialog-reject-button') as HTMLButtonElement;
      if (button) {
        button.focus();
        setTimeout(() => button.focus(), 50);
      }
    }, 300);
  }
}
