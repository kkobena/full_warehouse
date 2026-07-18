import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { VoSalesService } from '../../../../entities/sales/service/vo-sales.service';
import { SalesService } from '../../../../entities/sales/sales.service';
import { UtilisationCleSecurite } from '../../../../entities/action-autorisation/utilisation-cle-securite.model';

/**
 * AuthorizationModalComponent
 *
 * Modal for requesting authorization for protected actions (delete product, apply discount)
 * Requires a user with the specific privilege to enter their security key
 *
 * @example
 * const modalRef = this.modalService.open(AuthorizationModalComponent);
 * modalRef.componentInstance.saleId = 123;
 * modalRef.componentInstance.privilege = 'PR_SUPPRIME_PRODUIT_VENTE';
 * modalRef.componentInstance.saleType = 'COMPTANT';
 * modalRef.result.then(authorized => { ... });
 */
@Component({
  selector: 'app-authorization-modal',
  imports: [CommonModule, FormsModule, ButtonModule, InputTextModule, PasswordModule],
  template: `
    <div class="authorization-modal">
      <div class="modal-header">
        <h4 class="modal-title">
          <i class="pi pi-shield"></i>
          Autorisation requise
        </h4>
      </div>

      <div class="modal-body">
        <div class="alert alert-warning">
          <i class="pi pi-exclamation-triangle"></i>
          <span>Cette action nécessite une autorisation.</span>
        </div>

        <div class="privilege-info">
          <strong>Privilège requis:</strong>
          <span class="privilege-badge">{{ getPrivilegeLabel(privilege) }}</span>
        </div>

        <form (ngSubmit)="authorize()" #authForm="ngForm">
          <div class="form-group">
            <label for="securityKey">Clé de sécurité *</label>
            <p-password
              id="securityKey"
              [(ngModel)]="securityKey"
              name="securityKey"
              placeholder="Entrez votre clé de sécurité"
              [feedback]="false"
              [toggleMask]="true"
              required
              fluid="true"
            />
          </div>

          <div class="form-group">
            <label for="comment">Commentaire (optionnel)</label>
            <input
              fluid="true"
              pInputText
              id="comment"
              [(ngModel)]="comment"
              name="comment"
              placeholder="Motif de l'autorisation"
              class="w-full"
            />
          </div>

          @if (errorMessage) {
            <div class="alert alert-danger">
              <i class="pi pi-times-circle"></i>
              {{ errorMessage }}
            </div>
          }
        </form>
      </div>

      <div class="modal-footer">
        <p-button label="Annuler" severity="secondary" [outlined]="true" (onClick)="cancel()" [disabled]="isSaving" />
        <p-button
          label="Autoriser"
          severity="success"
          icon="pi pi-check"
          (onClick)="authorize()"
          [disabled]="!securityKey || isSaving"
          [loading]="isSaving"
        />
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.Eager,
  styles: [
    `
      .authorization-modal {
        min-width: 400px;
      }

      .modal-header {
        display: flex;
        align-items: center;
        padding: 1.5rem;
        border-bottom: 1px solid #dee2e6;

        .modal-title {
          margin: 0;
          display: flex;
          align-items: center;
          gap: 0.75rem;
          font-size: 1.25rem;
          font-weight: 600;

          i {
            color: #f59e0b;
          }
        }
      }

      .modal-body {
        padding: 1.5rem;

        .alert {
          display: flex;
          align-items: center;
          gap: 0.75rem;
          padding: 0.75rem 1rem;
          margin-bottom: 1.5rem;
          border-radius: 6px;

          &.alert-warning {
            background-color: #fef3c7;
            color: #92400e;
            border: 1px solid #fbbf24;
          }

          &.alert-danger {
            background-color: #fee2e2;
            color: #991b1b;
            border: 1px solid #ef4444;
          }

          i {
            font-size: 1.25rem;
          }
        }

        .privilege-info {
          display: flex;
          align-items: center;
          gap: 0.75rem;
          margin-bottom: 1.5rem;
          padding: 0.75rem;
          background-color: #f3f4f6;
          border-radius: 6px;

          strong {
            color: #374151;
          }

          .privilege-badge {
            padding: 0.25rem 0.75rem;
            background-color: #3b82f6;
            color: white;
            border-radius: 9999px;
            font-size: 0.875rem;
            font-weight: 500;
          }
        }

        .form-group {
          margin-bottom: 1.25rem;

          label {
            display: block;
            margin-bottom: 0.5rem;
            font-weight: 500;
            color: #374151;
          }
        }
      }

      .modal-footer {
        display: flex;
        justify-content: flex-end;
        gap: 0.75rem;
        padding: 1rem 1.5rem;
        border-top: 1px solid #dee2e6;
        background-color: #f9fafb;
      }
    `,
  ],
})
export class AuthorizationModalComponent {
  // Inputs (set by parent when opening modal)
  saleId?: number;
  saleType: 'COMPTANT' | 'ASSURANCE' | 'CARNET' = 'COMPTANT';
  privilege: string = '';

  // Form state
  securityKey: string = '';
  comment: string = '';
  isSaving: boolean = false;
  errorMessage: string = '';

  // Services
  private readonly activeModal = inject(NgbActiveModal);
  private readonly voSalesService = inject(VoSalesService);
  private readonly salesService = inject(SalesService);

  cancel(): void {
    this.activeModal.dismiss('cancel');
  }

  authorize(): void {
    if (!this.securityKey) {
      this.errorMessage = 'La clé de sécurité est requise';
      return;
    }

    this.isSaving = true;
    this.errorMessage = '';

    const authRequest: UtilisationCleSecurite = {
      entityId: this.saleId!,
      privilege: this.privilege,
      actionAuthorityKey: this.securityKey,
      commentaire: this.comment || undefined,
    };

    // Pour ASSURANCE et CARNET, utiliser voSalesService, sinon salesService
    const service = this.saleType === 'ASSURANCE' || this.saleType === 'CARNET' ? this.voSalesService : this.salesService;

    service.authorizeAction(authRequest).subscribe({
      next: () => {
        this.isSaving = false;
        this.activeModal.close(true);
      },
      error: error => {
        this.isSaving = false;
        this.errorMessage = this.extractErrorMessage(error);
      },
    });
  }

  getPrivilegeLabel(privilege: string): string {
    switch (privilege) {
      case 'PR_SUPPRIME_PRODUIT_VENTE':
        return 'Suppression de produit';
      case 'PR_AJOUTER_REMISE_VENTE':
        return 'Application de remise';
      case 'PR_MODIFIER_PRIX_VENTE':
        return 'Modification de prix';
      case 'PR_ANNULER_VENTE':
        return 'Annulation de vente';
      default:
        return privilege;
    }
  }

  private extractErrorMessage(error: any): string {
    if (error?.error?.message) {
      return error.error.message;
    }
    if (error?.message) {
      return error.message;
    }
    return "Une erreur est survenue lors de l'autorisation";
  }
}
