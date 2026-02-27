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
    // Workaround bug PrimeNG 20 : getElementToFocus() cherche '.p-confirm-dialog-accept'
    // mais la classe réelle du bouton est '.p-confirmdialog-accept-button'.
    // Le sélecteur ne match jamais → focus par défaut sur reject.
    // On observe l'apparition du bouton accept et on force le focus dessus.
    this.focusAcceptButton();
  }

  onWarn(rejectHandler: () => void, message?: string, header?: string, icon?: string): void {
    this.confirmationService.confirm({
      message: message || this.message(),
      header: header || this.header(),
      icon: icon || 'pi pi-exclamation-triangle',
      acceptVisible: false,
      rejectButtonProps: rejectWarningButtonProps(),
      reject: () => rejectHandler(),
    });
  }

  /**
   * Workaround bug PrimeNG 20 : getElementToFocus() n'est jamais appelé par p-dialog.
   * Le p-dialog.focus() fait un setTimeout(focus_premier_element, 150ms) sur le footer,
   * ce qui focus toujours le reject (premier bouton dans le DOM).
   * On force le focus sur accept APRÈS ce timeout de 150ms.
   */
  private focusAcceptButton(): void {
    setTimeout(() => {
      const btn = document.querySelector('.p-confirmdialog-accept-button') as HTMLButtonElement;
      btn?.focus();
    }, 200);
  }
}
