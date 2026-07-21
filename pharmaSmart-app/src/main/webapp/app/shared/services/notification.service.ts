import { Injectable, signal } from '@angular/core';

export type NotificationSeverity = 'success' | 'info' | 'warn' | 'error';

export interface ToastMessage {
  id: number;
  severity: NotificationSeverity;
  summary: string;
  detail: string;
  /** Durée d'affichage en millisecondes. */
  life: number;
}

/**
 * Service de notifications de l'application.
 *
 * **L'API publique (`success` / `info` / `warning` / `error` / `show` / `clear`) est
 * inchangée** : les ~100 appelants existants n'ont pas été touchés. Seule
 * l'implémentation a changé — `MessageService` de PrimeNG a laissé place à un signal
 * interne, consommé par `<app-toast-host />` qui rend des `<ngb-toast>`
 * (cf. plan de migration §7.1).
 *
 * Le retrait automatique est géré par `NgbToast` via son `[delay]`, qui appelle
 * `dismiss(id)` — inutile de programmer un `setTimeout` ici.
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly _messages = signal<readonly ToastMessage[]>([]);

  /** Consommé par `ToastHostComponent`. */
  readonly messages = this._messages.asReadonly();

  private nextId = 1;

  success(message: string, title?: string): void {
    this.push('success', message, title ?? 'Succès', 3000);
  }

  info(message: string, title?: string): void {
    this.push('info', message, title ?? 'Information', 3000);
  }

  warning(message: string, title?: string): void {
    this.push('warn', message, title ?? 'Avertissement', 4000);
  }

  error(message: string, title?: string): void {
    this.push('error', message, title ?? 'Erreur', 5000);
  }

  show(severity: NotificationSeverity, message: string, title?: string, life = 3000): void {
    this.push(severity, message, title ?? '', life);
  }

  clear(): void {
    this._messages.set([]);
  }

  /** Retire une notification donnée. Appelé par l'hôte à l'expiration ou à la fermeture. */
  dismiss(id: number): void {
    this._messages.update(messages => messages.filter(message => message.id !== id));
  }

  private push(severity: NotificationSeverity, detail: string, summary: string, life: number): void {
    const id = this.nextId++;
    this._messages.update(messages => [...messages, { id, severity, summary, detail, life }]);
  }
}
