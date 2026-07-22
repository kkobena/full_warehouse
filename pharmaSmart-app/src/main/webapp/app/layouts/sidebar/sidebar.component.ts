import { Component, DestroyRef, effect, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { Router, RouterModule } from "@angular/router";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { fromEvent } from "rxjs";
import { AccountService } from "app/core/auth/account.service";
import { LoginService } from "app/login/login.service";
import { NavItem } from "../navbar/navbar-item.model";
import {
  faBars,
  faChevronDown,
  faChevronRight,
  faServer,
  faSlidersH,
  faUserCircle
} from "@fortawesome/free-solid-svg-icons";
import { Theme, ThemeService } from "../../core/theme/theme.service";
import { NgbModal, NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import { AppSettingsDialogComponent } from "../../shared/settings/app-settings-dialog.component";
import { Authority } from "../../config/authority.constants";
import { LayoutService } from "../../core/config/layout.service";
import { environment } from "environments/environment";
import { NavigationService } from "../../core/config/navigation.service";
import { TauriPrinterService } from "../../shared/services/tauri-printer.service";
import { AlertBadgeService } from "../../shared/services/alert-badge.service";
import { NavStore } from "app/core/store/nav.store";
import { FaIconComponent } from "@fortawesome/angular-fontawesome";

@Component({
  selector: "jhi-sidebar",
  imports: [CommonModule, RouterModule, FormsModule, NgbTooltip, FaIconComponent],
  templateUrl: "./sidebar.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ["./sidebar.component.scss"]
})
export default class SidebarComponent implements OnInit {
  protected account = inject(AccountService).trackCurrentAccount();
  protected version = "";
  protected readonly isMobileSignal = signal(window.innerWidth <= 768);
  navItems: NavItem[] = [];
  expandedItems = new Set<string>();
  protected layoutService = inject(LayoutService);
  private readonly loginService = inject(LoginService);
  private readonly router = inject(Router);
  private readonly themeService = inject(ThemeService);
  private readonly modalService = inject(NgbModal);
  private readonly navigationService = inject(NavigationService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  protected readonly alertBadgeService = inject(AlertBadgeService);
  private readonly navStore = inject(NavStore);

  themes: Theme[];
  selectedTheme: string;

  readonly faBars = faBars;
  readonly faChevronDown = faChevronDown;
  readonly faChevronRight = faChevronRight;
  readonly faUserCircle = faUserCircle;
  readonly faSlidersH = faSlidersH;

  constructor() {
    const { VERSION } = environment;
    if (VERSION) {
      this.version = VERSION.toLowerCase().startsWith("v") ? VERSION : `v${VERSION}`;
    }


    fromEvent(window, "resize")
      .pipe(takeUntilDestroyed(inject(DestroyRef)))
      .subscribe(() => this.isMobileSignal.set(window.innerWidth <= 768));

    effect(() => {
      // Reactive : rebuilt when account, navTree (store) or alert counts change
      this.navStore.navTree(); // déclenche la réactivité quand le store se charge
      const items = this.buildNavItem();
      const ruptureCount = this.alertBadgeService.ruptureCount();
      const urgentCount = this.alertBadgeService.urgentCount();
      const peremptionCount = this.alertBadgeService.peremptionCount();
      this.applyNavBadges(items, ruptureCount, urgentCount, peremptionCount);
      this.navItems = items;
    });
  }

  ngOnInit(): void {
    this.alertBadgeService.init();
  }

  protected isMobile(): boolean {
    return this.isMobileSignal();
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
      this.toggleSidebar();
      this.expandItem(label);
    } else {
      this.toggleItem(label);
    }
  }

  protected onMenuItemClick(clickHandler?: () => void): void {
    if (clickHandler) clickHandler();
    if (!this.isCollapsed()) this.toggleSidebar();
  }

  protected onSubmenuItemClick(clickHandler?: () => void): void {
    if (clickHandler) clickHandler();
    if (!this.isCollapsed()) this.toggleSidebar();
  }

  protected changeTheme(themeName: string): void {
    this.selectedTheme = themeName;
    this.themeService.setTheme(themeName);
  }

  protected login(): void {
    this.router.navigate(["/login"]);
  }

  protected logout(): void {
    this.loginService.logout();
    this.router.navigate([""]);
  }

  protected openAppSettings(): void {
    this.modalService.open(AppSettingsDialogComponent, { size: "lg", backdrop: "static", centered: true });
  }

  protected openConfigEditor(): void {
    void this.router.navigate(["/app-config"]);
  }

  protected get isTauriAdmin(): boolean {
    const account = this.account();
    return this.tauriPrinterService.isRunningInTauri() &&
      !!account &&
      this.navigationService.hasAnyAuthority(Authority.ADMIN, account.authorities);
  }

  protected hasAnyAuthority(authorities: string[] | string): boolean {
    const userIdentity = this.account();
    if (!userIdentity) return false;
    if (!Array.isArray(authorities)) authorities = [authorities];
    return userIdentity.authorities.some((authority: string) => authorities.includes(authority));
  }

  private buildNavItem(): NavItem[] {
    const account = this.account();

    if (account) {
      const accountItems: NavItem[] = [
        { label: "Menu horizontal", faIcon: faBars, click: () => this.layoutService.toggleLayout() },
        { label: "Se déconnecter", faIcon: "sign-out-alt", click: () => this.logout() }
      ];
      if (this.navigationService.hasAnyAuthority(Authority.ADMIN, account.authorities) && this.tauriPrinterService.isRunningInTauri()) {
        accountItems.unshift({
          label: "Configuration avancée",
          faIcon: faSlidersH,
          click: () => this.openConfigEditor()
        });
      }
      return this.navigationService.buildNavItemsFromStore({ additionalAccountMenuItems: accountItems });
    }

    const additionalAccountMenuItems: NavItem[] = [
      { label: "Menu vertical", faIcon: faBars, click: () => this.layoutService.toggleLayout() },
      { label: "Se connecter", faIcon: "sign-out-alt", click: () => this.login() }
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
   * Applique les mêmes badges que la navbar sur les items du sidebar :
   * - /commande       → max(ruptureCount, urgentCount) — danger
   * - /gestion-peremption → peremptionCount — danger
   * - Parents         → somme propagée des enfants
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
        const total = item.children.reduce((sum, c) => sum + (c.badge ?? 0), 0);
        item.badge = total > 0 ? total : undefined;
        item.badgeSeverity = total > 0 ? "danger" : undefined;
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

