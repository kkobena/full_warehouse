import { Component, signal, ChangeDetectionStrategy } from '@angular/core';
import { NgbNav, NgbNavContent, NgbNavItem, NgbNavLink, NgbNavOutlet } from '@ng-bootstrap/ng-bootstrap';
import { NavManagerComponent } from '../nav-manager/nav-manager.component';
import { RoleComponent } from '../role/role.component';

@Component({
  selector: 'app-access-managment-home',
  templateUrl: './access-managment-home.component.html',
  styleUrl: './access-managment-home.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    NgbNav, NgbNavContent, NgbNavItem, NgbNavLink, NgbNavOutlet,
    NavManagerComponent,
    RoleComponent,
  ],
})
export class AccessManagmentHomeComponent {
  protected active = 'roles';

  /** Rôle dont on veut gérer les autorisations (passé à nav-manager). */
  protected readonly pendingRole = signal<string | null>(null);

  openPermissions(roleName: string): void {
    this.pendingRole.set(roleName);
    this.active = 'autorisations';
  }
}
