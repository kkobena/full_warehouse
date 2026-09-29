import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { catchError, map, of } from 'rxjs';
import { NavApiService } from 'app/core/data-access/nav-api.service';
import { ButtonComponent, SelectComponent } from 'app/shared/ui';

/** Choix du rôle dont ce dashboard devient l'accueil. Réservé à l'administrateur. */
@Component({
  selector: 'app-assign-role-modal',
  imports: [FormsModule, ButtonComponent, SelectComponent],
  template: `
    <div class="modal-header">
      <h2 class="modal-title fs-5"><i class="pi pi-users me-2" aria-hidden="true"></i>Accueil d'un rôle</h2>
      <button type="button" class="btn-close" aria-label="Fermer" (click)="activeModal.dismiss()"></button>
    </div>
    <div class="modal-body d-flex flex-column gap-2">
      <p class="mb-1">
        Les utilisateurs de ce rôle verront « {{ dashboardName }} » à l'accueil, sauf s'ils ont choisi leur propre accueil.
      </p>
      <app-select
        ariaLabel="Rôle"
        placeholder="Choisir un rôle"
        [items]="roles() ?? []"
        [loading]="roles() === null"
        bindLabel="label"
        bindValue="value"
        [ngModel]="role()"
        (ngModelChange)="role.set($event)"
      />
      <div class="form-text">Chaque tuile reste soumise aux droits de celui qui la regarde : un widget non autorisé s'affiche verrouillé.</div>
    </div>
    <div class="modal-footer">
      <app-button label="Annuler" severity="secondary" [text]="true" (clicked)="activeModal.dismiss()" />
      <app-button label="Attribuer" icon="pi pi-check" [disabled]="!role()" (clicked)="activeModal.close(role())" />
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignRoleModalComponent {
  readonly activeModal = inject(NgbActiveModal);

  /** Posé par l'appelant. */
  dashboardName = '';

  protected readonly role = signal<string | null>(null);
  protected readonly roles = toSignal(
    inject(NavApiService)
      .getAllRoles()
      .pipe(
        map(roles => roles.map(r => ({ value: r.name, label: r.libelle || r.name }))),
        catchError(() => of([])),
      ),
    { initialValue: null },
  );
}
