import { Injectable, inject } from '@angular/core';
import { MessageService } from 'primeng/api';

export type NotificationSeverity = 'success' | 'info' | 'warn' | 'error';

/**
 * Service de notifications pour l'application
 * Encapsule PrimeNG MessageService pour une API simplifiée
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly messageService = inject(MessageService);

  /**
   * Afficher une notification de succès
   */
  success(message: string, title?: string): void {
    this.messageService.add({
      severity: 'success',
      summary: title || 'Succès',
      detail: message,
      life: 3000,
    });
  }

  /**
   * Afficher une notification d'information
   */
  info(message: string, title?: string): void {
    this.messageService.add({
      severity: 'info',
      summary: title || 'Information',
      detail: message,
      life: 3000,
    });
  }

  /**
   * Afficher une notification d'avertissement
   */
  warning(message: string, title?: string): void {
    this.messageService.add({
      severity: 'warn',
      summary: title || 'Avertissement',
      detail: message,
      life: 4000,
    });
  }

  /**
   * Afficher une notification d'erreur
   */
  error(message: string, title?: string): void {
    this.messageService.add({
      severity: 'error',
      summary: title || 'Erreur',
      detail: message,
      life: 5000,
    });
  }

  /**
   * Afficher une notification personnalisée
   */
  show(severity: NotificationSeverity, message: string, title?: string, life: number = 3000): void {
    this.messageService.add({
      severity,
      summary: title,
      detail: message,
      life,
    });
  }

  /**
   * Effacer toutes les notifications
   */
  clear(): void {
    this.messageService.clear();
  }
}
