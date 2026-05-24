import { Component, inject, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';
import { NavApiService, INavRole, IAuthority } from "app/core/data-access/nav-api.service";
import { NotificationService } from 'app/shared/services/notification.service';
import { showCommonModal } from 'app/entities/sales/selling-home/sale-helper';
import { RoleFormComponent } from './ui/role-form.component';
import { InputText } from "primeng/inputtext";
import { NgbConfirmDialogService } from "../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";

const PREDEFINED = new Set([
  'ROLE_ADMIN', 'ROLE_USER', 'ROLE_CAISSIER',
  'ROLE_VENDEUR', 'ROLE_RESPONSABLE_COMMANDE', 'ROLE_PHARMACIEN',
]);

@Component({
  selector: 'app-role',
  templateUrl: './role.component.html',
  styleUrl: './role.component.scss',
  imports: [FormsModule, ButtonModule, TableModule, ToolbarModule, TooltipModule, InputText]
})
export class RoleComponent implements OnInit {
  private readonly navApi      = inject(NavApiService);
  private readonly notif       = inject(NotificationService);
  private readonly modalService = inject(NgbModal);

  /** Émis quand l'utilisateur veut gérer les autorisations d'un rôle. */
  readonly managePermissions = output<string>();

  protected readonly roles   = signal<IAuthority[]>([]);
  protected readonly loading = signal(false);

  // ── Édition inline du libellé ────────────────────────────────────────────
  protected readonly editingName = signal<string | null>(null);
  protected editingLibelle = '';

  private readonly confirmDialog = inject(NgbConfirmDialogService);
  ngOnInit(): void {
    this.loadRoles();
  }

  protected loadRoles(): void {
    this.loading.set(true);
    this.navApi.getAllRoles().subscribe({
      next: roles => { this.roles.set(roles); this.loading.set(false); },
      error: () => { this.notif.error('Impossible de charger les rôles.'); this.loading.set(false); },
    });
  }

  protected isPredefined(name?: string): boolean {
    return PREDEFINED.has(name ?? '');
  }

  // ── Création ─────────────────────────────────────────────────────────────
  protected openCreateForm(): void {
    showCommonModal(
      this.modalService,
      RoleFormComponent,
      { existingNames: this.roles().map(r => r.name ?? '') },
      (result: { name: string; libelle: string }) => {
        this.loadRoles();
        this.notif.success(`Rôle ${result.name} créé.`);
      },
      'lg',
    );
  }

  // ── Édition libellé ───────────────────────────────────────────────────────
  protected startEdit(role: INavRole): void {
    this.editingName.set(role.name ?? null);
    this.editingLibelle = role.libelle ?? '';
  }

  protected saveEdit(role: INavRole): void {
    const libelle = this.editingLibelle.trim();
    if (!libelle || libelle === role.libelle) { this.cancelEdit(); return; }
    this.navApi.updateRoleLibelle(role.name!, libelle).subscribe({
      next: () => {
        this.roles.update(list => list.map(r => r.name === role.name ? { ...r, libelle } : r));
        this.cancelEdit();
        this.notif.success('Libellé mis à jour.');
      },
      error: () => this.notif.error('Erreur lors de la mise à jour.'),
    });
  }

  protected cancelEdit(): void {
    this.editingName.set(null);
    this.editingLibelle = '';
  }

  // ── Suppression ───────────────────────────────────────────────────────────
  protected deleteRole(role: INavRole): void {
    this.confirmDialog.onConfirm(
      () => {
        this.navApi.deleteRole(role.name!).subscribe({
          next: () => {
            this.loadRoles();
            this.notif.success(`Rôle ${role.name} supprimé.`);
          },
          error: () => this.notif.error('Erreur lors de la suppression.'),
        });
      },
      'Supprimer le rôle',
      `Confirmer la suppression du rôle <strong>${role.libelle ?? role.name}</strong> ? Cette action est irréversible.`,
      'pi pi-trash',
    );
  }
}
