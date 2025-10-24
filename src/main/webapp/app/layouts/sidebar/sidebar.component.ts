import { Component, effect, inject, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AccountService } from 'app/core/auth/account.service';
import { LoginService } from 'app/login/login.service';
import { NavItem } from '../navbar/navbar-item.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import {
  faServer,
  faBars,
  faChevronDown,
  faChevronRight,
  faUserCircle
} from '@fortawesome/free-solid-svg-icons';
import { Theme, ThemeService } from '../../core/theme/theme.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AppSettingsDialogComponent } from '../../shared/settings/app-settings-dialog.component';
import { LayoutService } from '../../core/config/layout.service';
import { environment } from 'environments/environment';
import { NavigationService } from '../../core/config/navigation.service';

@Component({
  selector: 'jhi-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, WarehouseCommonModule, FormsModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export default class SidebarComponent implements OnInit {
  protected account = inject(AccountService).trackCurrentAccount();
  protected version = '';
  navItems: NavItem[] = [];
  expandedItems = new Set<string>();

  private loginService = inject(LoginService);
  private router = inject(Router);
  private themeService = inject(ThemeService);
  private modalService = inject(NgbModal);
  private navigationService = inject(NavigationService);
  protected layoutService = inject(LayoutService);

  themes: Theme[];
  selectedTheme: string;


  readonly faBars = faBars;
  readonly faChevronDown = faChevronDown;
  readonly faChevronRight = faChevronRight;
  readonly faUserCircle = faUserCircle;

  constructor() {
    const { VERSION } = environment;
    if (VERSION) {
      this.version = VERSION.toLowerCase().startsWith('v') ? VERSION : `v${VERSION}`;
    }
    effect(() => {
      this.navItems = this.buildNavItem();
    });
  }

  ngOnInit(): void {
   // this.themes = this.themeService.getThemes();
  }

  protected isCollapsed(): boolean {
    return this.layoutService.isSidebarCollapsed();
  }

  protected toggleSidebar(): void {
    this.layoutService.toggleSidebarCollapsed();
  }

  protected toggleItem(label: string): void {
    if (this.expandedItems.has(label)) {
      this.expandedItems.delete(label);
    } else {
      this.expandedItems.add(label);
    }
  }

  protected expandItem(label: string): void {
    this.expandedItems.add(label);
  }

  protected collapseItem(label: string): void {
    this.expandedItems.delete(label);
  }

  protected isExpanded(label: string): boolean {
    return this.expandedItems.has(label);
  }

  protected onParentMenuHover(label: string, isEntering: boolean): void {
    if (isEntering) {
      this.expandItem(label);
    } else {
      this.collapseItem(label);
    }
  }

  protected onParentMenuClick(label: string): void {
    if (this.isCollapsed()) {
      // If sidebar is collapsed, expand it and show the menu
      this.toggleSidebar();
      this.expandItem(label);
    } else {
      // If sidebar is expanded, toggle the menu item
      this.toggleItem(label);
    }
  }

  protected onMenuItemClick(clickHandler?: () => void): void {
    if (clickHandler) {
      clickHandler();
    }
    // Collapse sidebar if it's expanded when clicking a menu item without children
    if (!this.isCollapsed()) {
      this.toggleSidebar();
    }
  }

  protected onSubmenuItemClick(clickHandler?: () => void): void {
    if (clickHandler) {
      clickHandler();
    }
    // Collapse sidebar if it's expanded when clicking a submenu item
    if (!this.isCollapsed()) {
      this.toggleSidebar();
    }
  }

  protected changeTheme(themeName: string): void {
    this.selectedTheme = themeName;
    this.themeService.setTheme(themeName);
  }

  protected login(): void {
    this.router.navigate(['/login']);
  }

  protected logout(): void {
    this.loginService.logout();
    this.router.navigate(['']);
  }

  protected openAppSettings(): void {
    this.modalService.open(AppSettingsDialogComponent, { size: 'lg', backdrop: 'static' });
  }

  protected hasAnyAuthority(authorities: string[] | string): boolean {
    const userIdentity = this.account();
    if (!userIdentity) {
      return false;
    }
    if (!Array.isArray(authorities)) {
      authorities = [authorities];
    }
    return userIdentity.authorities.some((authority: string) => authorities.includes(authority));
  }

  private buildNavItem(): NavItem[] {
    const account = this.account();

    // Authenticated user menu items
    if (account) {
      return this.navigationService.buildNavItems({
        additionalAdminMenuItems: [
          {
            label: 'Paramètres Serveur',
            faIcon: faServer,
            click: () => this.openAppSettings()
          }
        ],
        additionalAccountMenuItems: [
          {
            label: 'Menu horizontal',
            faIcon: faBars,
            click: () => this.layoutService.toggleLayout()
          },
          {
            label: 'Se déconnecter',
            faIcon: 'sign-out-alt',
            click: () => this.logout()
          }
        ]
      });
    }

    // Unauthenticated user menu items
    return this.navigationService.buildUnauthenticatedNavItems([
      {
        label: 'Paramètres Serveur',
        faIcon: faServer,
        click: () => this.openAppSettings()
      },
      {
        label: 'Menu horizontal',
        faIcon: faBars,
        click: () => this.layoutService.toggleLayout()
      },
      {
        label: 'Se connecter',
        faIcon: 'sign-out-alt',
        click: () => this.login()
      }
    ]);
  }
}
