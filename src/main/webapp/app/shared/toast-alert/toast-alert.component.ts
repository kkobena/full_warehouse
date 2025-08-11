import { Component, inject } from '@angular/core';
import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';

export type Severity = 'primary' | 'secondary' | 'success' | 'info' | 'warn' | 'danger' | 'help';

@Component({
  selector: 'jhi-toast-alert',
  providers: [MessageService],
  template: ` <p-toast position="center" />`,
  imports: [Toast],
})
export class ToastAlertComponent {
  private readonly messageService = inject(MessageService);

  show(title: string, message: string, severity: Severity): void {
    this.messageService.add({
      severity: severity,
      summary: title,
      detail: message,
    });
  }

  showError(message?: string, title?: string): void {
    this.messageService.add({
      severity: 'error',
      summary: title || 'Error',
      detail: message || "Une erreur s'est produite.",
    });
  }

  showInfo(message: string, title?: string): void {
    this.messageService.add({
      severity: 'info',
      summary: title || 'Information',
      detail: message,
    });
  }

  showWarn(message: string, title?: string): void {
    this.messageService.add({
      severity: 'warn',
      summary: title || 'Avertissement',
      detail: message,
    });
  }
}
