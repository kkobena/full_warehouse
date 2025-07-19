import {Component, inject, input} from '@angular/core';
import {ConfirmationService} from "primeng/api";
import {acceptButtonProps, rejectButtonProps} from "../../util/modal-button-props";

@Component({
  selector: 'jhi-confirm-dialog',
  template: '',
})
export class ConfirmDialogComponent {
  private readonly confirmationService = inject(ConfirmationService);
  message = input<string>();
  header = input<string>();
  icon = input<string>('pi pi-info-circle');
  acceptHandler = input<() => void>();
  rejectHandler = input<() => void>();
  style = input<{ [klass: string]: any }>({width: '40vw'});

  onConfirm(
    acceptHandler: () => void,
    header?: string,
    message?: string,
    icon?: string,
    rejectHandler?: () => void
  ): void {
    this.confirmationService.confirm({
      message: message || this.message(),
      header: header || this.header(),
      icon: icon || this.icon(),
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      defaultFocus: 'accept',
      accept: () => acceptHandler(),
      reject: () => {
        if (rejectHandler) {
          rejectHandler();
        }
      }
    });
    setTimeout(() => {
      this.accept?.focus();
    }, 200);
  }

  onWarn(
    acceptHandler: () => void,
    message?: string,
    header?: string,
    icon?: string
  ): void {
    this.confirmationService.confirm({
      message: message || this.message(),
      header: header || this.header(),
      icon: icon || 'pi pi-exclamation-triangle',
      acceptButtonProps: acceptButtonProps(),
      rejectButtonProps: rejectButtonProps(),
      defaultFocus: 'accept',
      accept: () => acceptHandler(),
    });
    setTimeout(() => {
      this.accept?.focus();
    }, 200);
  }

  private get accept(): HTMLButtonElement {
    return document.querySelector(
      '.ws-dialog .p-confirmdialog-accept-button'
    ) as HTMLButtonElement;
  }
}
