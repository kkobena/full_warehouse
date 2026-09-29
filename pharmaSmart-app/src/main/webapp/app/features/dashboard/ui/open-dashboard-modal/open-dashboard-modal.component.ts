import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { BadgeComponent, ButtonComponent } from 'app/shared/ui';
import { IDashboardLayout } from 'app/shared/model/dashboard-layout.model';
import { NgbConfirmDialogService } from 'app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { NotificationService } from 'app/shared/services/notification.service';
import { DashboardLayoutApiService } from '../../data-access/services/dashboard-layout-api.service';
import { errorMessage } from '../../data-access/error-message';

/** Liste des dashboards ouvrables : les siens et les publics des autres. Ferme sur le layout choisi. */
@Component({
  selector: 'app-open-dashboard-modal',
  imports: [ButtonComponent, BadgeComponent],
  template: `
    <div class="modal-header">
      <h2 class="modal-title fs-5"><i class="pi pi-folder-open me-2" aria-hidden="true"></i>Ouvrir un tableau de bord</h2>
      <button type="button" class="btn-close" aria-label="Fermer" (click)="activeModal.dismiss()"></button>
    </div>

    <div class="modal-body">
      @if (loading()) {
        <p class="text-center text-body-secondary py-4"><i class="pi pi-spin pi-spinner me-2" aria-hidden="true"></i>Chargement…</p>
      } @else {
        <ul class="list-group">
          @for (layout of layouts(); track layout.id) {
            <li class="list-group-item d-flex align-items-center gap-3">
              <div class="flex-fill min-w-0">
                <div class="fw-semibold">{{ layout.name }}</div>
                @if (layout.description) {
                  <div class="small text-body-secondary">{{ layout.description }}</div>
                }
                <div class="d-flex gap-2 mt-1">
                  @if (isMine(layout)) {
                    <app-badge [label]="layout.scope === 'PUBLIC' ? 'Public' : 'Privé'" severity="info" />
                  } @else {
                    <app-badge [label]="'Partagé par ' + layout.userLogin" severity="secondary" />
                  }
                  @if (isMine(layout) && layout.isDefault) {
                    <app-badge label="Mon accueil" severity="success" />
                  }
                </div>
              </div>
              <app-button icon="pi pi-folder-open" label="Ouvrir" size="small" [outlined]="true" (clicked)="activeModal.close(layout)" />
              @if (isMine(layout)) {
                <app-button
                  icon="pi pi-trash"
                  size="small"
                  severity="danger"
                  [outlined]="true"
                  [iconOnly]="true"
                  ariaLabel="Supprimer"
                  (clicked)="remove(layout)"
                />
              }
            </li>
          } @empty {
            <li class="list-group-item text-center text-body-secondary py-4">Aucun tableau de bord enregistré.</li>
          }
        </ul>
      }
    </div>

    <div class="modal-footer">
      <app-button label="Fermer" severity="secondary" [text]="true" (clicked)="activeModal.dismiss()" />
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OpenDashboardModalComponent implements OnInit {
  readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(DashboardLayoutApiService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notification = inject(NotificationService);

  /** Posé par l'appelant : sert à distinguer ses dashboards de ceux des autres. */
  currentLogin = '';
  /** Posé par l'appelant : dashboard ouvert, qu'on ne supprime pas sous ses pieds. */
  openedId: number | null = null;

  protected readonly layouts = signal<IDashboardLayout[]>([]);
  protected readonly loading = signal(true);

  ngOnInit(): void {
    this.reload();
  }

  protected isMine(layout: IDashboardLayout): boolean {
    return layout.userLogin === this.currentLogin;
  }

  protected remove(layout: IDashboardLayout): void {
    if (layout.id === this.openedId) {
      this.notification.warning('Ce tableau de bord est ouvert : ouvrez-en un autre avant de le supprimer.');
      return;
    }
    this.confirmDialog.onConfirm(
      () =>
        this.api.delete(layout.id!).subscribe({
          next: () => this.reload(),
          error: (err: HttpErrorResponse) => this.notification.error(errorMessage(err)),
        }),
      'Supprimer le tableau de bord',
      `Supprimer définitivement « ${layout.name} » ?`,
    );
  }

  private reload(): void {
    this.loading.set(true);
    this.api.query().subscribe({
      next: res => {
        this.layouts.set((res.body ?? []).filter(l => l.componentKey === 'CUSTOM'));
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.notification.error(errorMessage(err));
      },
    });
  }
}
