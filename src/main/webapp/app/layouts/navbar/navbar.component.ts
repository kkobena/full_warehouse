import { Component, effect, inject, OnInit, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { environment } from 'environments/environment';
import { AccountService } from 'app/core/auth/account.service';
import { LoginService } from 'app/login/login.service';
import { NavItem } from './navbar-item.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import {
  faBars,
  faKeyboard,
  faServer
} from '@fortawesome/free-solid-svg-icons';
import { Theme, ThemeService } from '../../core/theme/theme.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AppSettingsDialogComponent } from '../../shared/settings/app-settings-dialog.component';
import { ShortcutsHelpDialogComponent } from '../../shared/shortcuts/shortcuts-help-dialog.component';
import { LayoutService } from '../../core/config/layout.service';
import { NavigationService } from '../../core/config/navigation.service';

@Component({
  selector: 'jhi-navbar',
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
  imports: [RouterModule, WarehouseCommonModule]
})
export default class NavbarComponent implements OnInit {
  protected isNavbarCollapsed = signal(true);
  protected version = '';
  protected account = inject(AccountService).trackCurrentAccount();
  protected navItems: NavItem[] = [];
  protected menuStock: string[] = [];

  private loginService = inject(LoginService);
  private router = inject(Router);
  private themeService = inject(ThemeService);
  private modalService = inject(NgbModal);
  private navigationService = inject(NavigationService);
  protected layoutService = inject(LayoutService);
  themes: Theme[];
  selectedTheme: string;



  changeTheme(themeName: string): void {
    this.selectedTheme = themeName;
    this.themeService.setTheme(themeName);
  }

  constructor() {
    this.menuStock = ['gestion-entree', 'commande', 'gestion-stock', 'produit'];
    const { VERSION } = environment;
    if (VERSION) {
      this.version = VERSION.toLowerCase().startsWith('v') ? VERSION : `v${VERSION}`;
    }
    effect(() => {
      this.navItems = this.buildNavItem();
    });
  }

  ngOnInit(): void {
    this.themes = this.themeService.getThemes();

  }

  protected collapseNavbar(): void {
    this.isNavbarCollapsed.set(true);
  }

  protected login(): void {
    this.router.navigate(['/login']);
  }

  protected logout(): void {
    this.collapseNavbar();
    this.loginService.logout();
    this.router.navigate(['']);
  }

  protected toggleNavbar(): void {
    this.isNavbarCollapsed.update(isNavbarCollapsed => !isNavbarCollapsed);
  }

  protected openAppSettings(): void {
    this.modalService.open(AppSettingsDialogComponent, { size: 'lg', backdrop: 'static' });
  }

  protected openShortcutsHelp(): void {
    this.modalService.open(ShortcutsHelpDialogComponent, { size: 'xl', backdrop: 'static', scrollable: true });
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

  protected isAccountMenu(item: NavItem): boolean {
    return item.faIcon === 'user' || item.label.toLowerCase().includes('account') || item.label.toLowerCase().includes('compte');
  }

  private buildNavItem(): NavItem[] {
    const account = this.account();

    // Authenticated user menu items
    if (account) {
      return this.navigationService.buildNavItems({
        additionalAccountMenuItems: [
          {
            label: 'Raccourcis Clavier',
            faIcon: faKeyboard,
            click: () => this.openShortcutsHelp()
          },
          {
            label: 'Menu vertical',
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
        label: 'Raccourcis Clavier',
        faIcon: faKeyboard,
        click: () => this.openShortcutsHelp()
      },
      {
        label: 'Menu vertical',
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
