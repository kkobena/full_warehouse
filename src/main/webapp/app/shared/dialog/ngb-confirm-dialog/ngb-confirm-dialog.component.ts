import { Component, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-ngb-confirm-dialog-content',
  template: `
    <div class="confirm-dialog">
      <div class="confirm-header">
        <div class="confirm-icon-wrapper">
          <i [class]="icon"></i>
        </div>
        <h5 class="confirm-title">{{ header }}</h5>
      </div>
      <div class="confirm-body">
        <div [innerHTML]="safeMessage"></div>
      </div>
      <div class="confirm-footer">
        <button type="button" class="confirm-btn confirm-btn-reject" (click)="activeModal.dismiss('reject')">
          <i class="pi pi-times"></i>
          <span>Non</span>
        </button>
        <button type="button" class="confirm-btn confirm-btn-accept"
                (click)="accept()"
                (keydown.enter)="onEnterKey($event)"
                ngbAutofocus>
          <i class="pi pi-check"></i>
          <span>Oui</span>
        </button>
      </div>
    </div>
  `,
  styles: `
    .confirm-dialog {
      border-radius: 12px;
      overflow: hidden;
    }

    .confirm-header {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 1rem 1.25rem;
      background: linear-gradient(135deg, #f0f7fa 0%, #e8f4f8 100%);
      border-bottom: 1px solid #d1e3ed;
    }

    .confirm-icon-wrapper {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: rgba(91, 137, 166, 0.15);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      i {
        font-size: 1.1rem;
        color: #5b89a6;
      }
    }

    .confirm-title {
      margin: 0;
      font-size: 1.05rem;
      font-weight: 600;
      color: #1f2937;
    }

    .confirm-body {
      padding: 1.25rem;
      font-size: 0.925rem;
      color: #374151;
      line-height: 1.5;
    }

    .confirm-footer {
      display: flex;
      justify-content: flex-end;
      gap: 0.5rem;
      padding: 0.75rem 1.25rem;
      background: #f9fafb;
      border-top: 1px solid #e5e7eb;
    }

    .confirm-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.375rem;
      padding: 0.4rem 1rem;
      border-radius: 6px;
      font-size: 0.875rem;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s ease;
      border: 1px solid;

      i { font-size: 0.85rem; }

      &:focus-visible {
        outline: 2px solid #5b89a6;
        outline-offset: 2px;
      }
    }

    .confirm-btn-reject {
      color: #d9534f;
      border-color: #d9534f;
      background: transparent;

      &:hover {
        background: #d9534f;
        color: #fff;
      }
    }

    .confirm-btn-accept {
      color: #5b89a6;
      border-color: #5b89a6;
      background: transparent;

      &:hover {
        background: #5b89a6;
        color: #fff;
      }
    }
  `,
})
export class NgbConfirmDialogContentComponent {
  readonly activeModal = inject(NgbActiveModal);
  private readonly sanitizer = inject(DomSanitizer);
  header = '';
  icon = 'pi pi-info-circle';
  safeMessage: SafeHtml = '';

  /**
   * Garde pour empêcher l'acceptation immédiate par un Enter résiduel.
   * Quand le modal s'ouvre en réponse à un Enter (ex: dans le champ de paiement),
   * le ngbAutofocus met le focus sur le bouton "Oui" pendant que l'événement clavier
   * est encore en cours. Ce délai empêche l'acceptation accidentelle.
   */
  private ready = false;

  constructor() {
    setTimeout(() => this.ready = true, 200);
  }

  set message(value: string) {
    // bypassSecurityTrustHtml car le contenu est généré en interne (pas d'input utilisateur)
    this.safeMessage = this.sanitizer.bypassSecurityTrustHtml(value);
  }

  accept(): void {
    if (!this.ready) {
      return;
    }
    this.activeModal.close('accept');
  }

  onEnterKey(event: Event): void {
    if (!this.ready) {
      event.preventDefault();
      event.stopPropagation();
    }
  }
}
