import { Component, effect, inject, OnInit, signal } from "@angular/core";
import { Router, RouterModule } from "@angular/router";
import { environment } from "environments/environment";
import { AccountService } from "app/core/auth/account.service";
import { LoginService } from "app/login/login.service";
import { NavItem } from "./navbar-item.model";
import { faBars, faServer } from "@fortawesome/free-solid-svg-icons";
import { Theme, ThemeService } from "../../core/theme/theme.service";
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import { AppSettingsDialogComponent } from "../../shared/settings/app-settings-dialog.component";
import { LayoutService } from "../../core/config/layout.service";
import { NavigationService } from "../../core/config/navigation.service";
import { TauriPrinterService } from "../../shared/services/tauri-printer.service";
import { AlertBadgeService } from "../../shared/services/alert-badge.service";
import { WarehouseCommonModule } from "../../shared/warehouse-common/warehouse-common.module";

@Component({
  selector: "jhi-navbar",
  templateUrl: "./navbar.component.html",
  styleUrl: "./navbar.component.scss",
  imports: [RouterModule, WarehouseCommonModule]
})
export default class NavbarComponent implements OnInit {
  protected isNavbarCollapsed = signal(true);
  protected version = "";
  protected account = inject(AccountService).trackCurrentAccount();
  protected navItems: NavItem[] = [];
  protected menuStock: string[] = [];
  protected layoutService = inject(LayoutService);
  private readonly loginService = inject(LoginService);
  private readonly router = inject(Router);
  private readonly themeService = inject(ThemeService);
  private readonly modalService = inject(NgbModal);
  private readonly navigationService = inject(NavigationService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  protected readonly alertBadgeService = inject(AlertBadgeService);
  themes: Theme[];
  selectedTheme: string;

  changeTheme(themeName: string): void {
    this.selectedTheme = themeName;
    this.themeService.setTheme(themeName);
  }

  constructor() {
    this.menuStock = ["gestion-entree", "commande", "gestion-stock", "produit"];
    const { VERSION } = environment;
    if (VERSION) {
      this.version = VERSION.toLowerCase().startsWith("v") ? VERSION : `v${VERSION}`;
    }
    effect(() => {
      // Reactive: rebuilds nav items whenever account, ruptureCount or urgentCount change
      const items = this.buildNavItem();
      const ruptureCount = this.alertBadgeService.ruptureCount();
      const urgentCount = this.alertBadgeService.urgentCount();
      const peremptionCount = this.alertBadgeService.peremptionCount();
      this.applyNavBadges(items, ruptureCount, urgentCount, peremptionCount);
      this.navItems = items;
    });
  }

  ngOnInit(): void {
    this.themes = this.themeService.getThemes();
    // Démarrer le polling des alertes dès que la navbar est initialisée
    this.alertBadgeService.init();
  }

  protected collapseNavbar(): void {
    this.isNavbarCollapsed.set(true);
  }

  protected login(): void {
    this.router.navigate(["/login"]);
  }

  protected logout(): void {
    this.collapseNavbar();
    this.loginService.logout();
    this.router.navigate([""]);
  }

  protected toggleNavbar(): void {
    this.isNavbarCollapsed.update(isNavbarCollapsed => !isNavbarCollapsed);
  }

  protected openAppSettings(): void {
    this.modalService.open(AppSettingsDialogComponent, { size: "lg", backdrop: "static" });
  }

  protected hasAnyAuthority(authorities: string[] | string): boolean {
    const userIdentity = this.account();
    if (!userIdentity) return false;
    if (!Array.isArray(authorities)) authorities = [authorities];
    return userIdentity.authorities.some((authority: string) => authorities.includes(authority));
  }

  protected isAccountMenu(item: NavItem): boolean {
    return item.faIcon === "user" || item.label.toLowerCase().includes("account") || item.label.toLowerCase().includes("compte");
  }

  private buildNavItem(): NavItem[] {
    const account = this.account();

    // Authenticated user menu items
    if (account) {
      return this.navigationService.buildNavItems({
        additionalAccountMenuItems: [
          {
            label: "Menu vertical",
            faIcon: faBars,
            click: () => this.layoutService.toggleLayout()
          },
          {
            label: "Se déconnecter",
            faIcon: "sign-out-alt",
            click: () => this.logout()
          }
        ]
      });
    }

    // Unauthenticated user menu items

    const additionalAccountMenuItems: NavItem[] = [
      {
        label: "Menu vertical",
        faIcon: faBars,
        click: () => this.layoutService.toggleLayout()
      },
      {
        label: "Se connecter",
        faIcon: "sign-out-alt",
        click: () => this.login()
      }
    ];

    if (this.tauriPrinterService.isRunningInTauri()) {
      additionalAccountMenuItems.unshift({
        label: "Paramètres Serveur",
        faIcon: faServer,
        click: () => this.openAppSettings()
      });
    }

    return this.navigationService.buildUnauthenticatedNavItems(additionalAccountMenuItems);
  }

  /**
   * Applique les badges d'alerte sur les nav items concernés :
   * - "Gestion Stock" > "Commandes" (/commande) → max(ruptureCount, urgentCount) badge danger
   * - "Gestion Stock" > "Péremptions" (/gestion-peremption) → peremptionCount badge danger
   * - Menus parents → somme propagée depuis les enfants
   */
  private applyNavBadges(
    items: NavItem[],
    ruptureCount: number,
    urgentCount: number,
    peremptionCount: number
  ): void {
    for (const item of items) {
      if (item.children?.length) {
        this.applyNavBadges(item.children, ruptureCount, urgentCount, peremptionCount);
        const totalChildren = item.children.reduce((sum, c) => sum + (c.badge ?? 0), 0);
        item.badge = totalChildren > 0 ? totalChildren : undefined;
        item.badgeSeverity = totalChildren > 0 ? "danger" : undefined;
      } else {
        if (item.routerLink === "/commande") {
          const total = Math.max(ruptureCount, urgentCount);
          item.badge = total > 0 ? total : undefined;
          item.badgeSeverity = "danger";
        } else if (item.routerLink === "/gestion-peremption") {
          item.badge = peremptionCount > 0 ? peremptionCount : undefined;
          item.badgeSeverity = "danger";
        } else {
          item.badge = undefined;
          item.badgeSeverity = undefined;
        }
      }
    }
  }
}

