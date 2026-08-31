import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  inject,
  OnInit,
  signal
} from "@angular/core";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {NavigationEnd, Router, RouterModule} from "@angular/router";
import {CommonModule} from "@angular/common";
import {filter, fromEvent} from "rxjs";
import {AccountService} from "app/core/auth/account.service";
import {LoginService} from "app/login/login.service";
import {NavItem} from "../navbar/navbar-item.model";
import {faBars, faChevronRight, faUserCircle} from "@fortawesome/free-solid-svg-icons";
import {NgbModal, NgbTooltip} from "@ng-bootstrap/ng-bootstrap";
import {CdkConnectedOverlay, CdkOverlayOrigin, ConnectedPosition} from "@angular/cdk/overlay";
import {NavFlyoutComponent} from "../shared/nav-flyout/nav-flyout.component";
import {AppSettingsDialogComponent} from "../../shared/settings/app-settings-dialog.component";
import {Authority} from "../../config/authority.constants";
import {LayoutService} from "../../core/config/layout.service";
import {environment} from "environments/environment";
import {NavigationService} from "../../core/config/navigation.service";
import {TauriPrinterService} from "../../shared/services/tauri-printer.service";
import {AlertBadgeService} from "../../shared/services/alert-badge.service";
import {NavStore} from "app/core/store/nav.store";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";

/** Délai avant qu'un survol ne bascule vers un autre menu déjà ouvert. */
const HOVER_OPEN_DELAY_MS = 120;

@Component({
  selector: "app-sidebar",
  imports: [
    CommonModule,
    RouterModule,
    NgbTooltip,
    FaIconComponent,
    CdkOverlayOrigin,
    CdkConnectedOverlay,
    NavFlyoutComponent
  ],
  templateUrl: "./sidebar.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ["./sidebar.component.scss"],
  host: {
    "(document:keydown.escape)": "onEscape()"
  }
})
export default class SidebarComponent implements OnInit {

  readonly navItems = signal<NavItem[]>([]);
  /** Identifiant du seul menu ouvert, ou `null`. */
  readonly openMenuId = signal<string | null>(null);
  /** L'ouverture courante vient-elle du clavier ? Conditionne le vol de focus. */
  readonly openedViaKeyboard = signal(false);
  readonly faBars = faBars;
  readonly faChevronRight = faChevronRight;
  readonly faUserCircle = faUserCircle;
  /**
   * Positions du flyout, par ordre de préférence : à droite aligné en haut,
   * puis aligné en bas si le panneau déborderait sous l'écran, puis à gauche
   * si la place manque à droite. Le CDK bascule automatiquement.
   */
  readonly flyoutPositions: ConnectedPosition[] = [
    {originX: "end", originY: "top", overlayX: "start", overlayY: "top", offsetX: 8},
    {originX: "end", originY: "bottom", overlayX: "start", overlayY: "bottom", offsetX: 8},
    {originX: "start", originY: "top", overlayX: "end", overlayY: "top", offsetX: -8}
  ];
  protected account = inject(AccountService).trackCurrentAccount();
  protected readonly version = signal("");
  protected readonly isMobileSignal = signal(window.innerWidth <= 768);
  protected layoutService = inject(LayoutService);
  protected readonly alertBadgeService = inject(AlertBadgeService);
  private readonly loginService = inject(LoginService);
  private readonly router = inject(Router);
  private readonly modalService = inject(NgbModal);
  private readonly navigationService = inject(NavigationService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly navStore = inject(NavStore);
  private hoverOpenTimer?: number;

  constructor() {
    const {VERSION} = environment;
    if (VERSION) {
      this.version.set(VERSION.toLowerCase().startsWith("v") ? VERSION : `v${VERSION}`);
    }


    const destroyRef = inject(DestroyRef);

    fromEvent(window, "resize")
      .pipe(takeUntilDestroyed(destroyRef))
      .subscribe(() => this.isMobileSignal.set(window.innerWidth <= 768));

    // Une navigation déclenchée hors du panneau (lien profond, bouton retour…)
    // doit aussi le refermer.
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntilDestroyed(destroyRef)
      )
      .subscribe(() => this.closeMenu());

    destroyRef.onDestroy(() => this.cancelHoverTimers());

    effect(() => {
      // Reactive : rebuilt when account, navTree (store) or alert counts change
      this.navStore.navTree(); // déclenche la réactivité quand le store se charge
      const items = this.navigationService.buildNavItems({
        onLogin: () => this.login(),
        onLogout: () => this.logout(),
        onOpenConfigEditor: () => this.openConfigEditor(),
        onOpenAppSettings: () => this.openAppSettings()
      });
      // `applyNavBadges` lit les compteurs d'alerte : appelée dans l'effet, la
      // lecture y est tracée et l'effet reste abonné à chacun d'eux.
      this.navigationService.applyNavBadges(items);
      this.navItems.set(items);
    });
  }

  protected get isTauriAdmin(): boolean {
    const account = this.account();
    return this.tauriPrinterService.isRunningInTauri() &&
      !!account &&
      this.navigationService.hasAnyAuthority(Authority.ADMIN, account.authorities);
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


  protected toggleMenu(item: NavItem, event?: MouseEvent): void {
    this.cancelHoverTimers();
    this.openedViaKeyboard.set(event?.detail === 0);
    this.openMenuId.update(id => (id === item.id ? null : item.id));
  }

  protected closeMenu(): void {
    this.cancelHoverTimers();
    this.openMenuId.set(null);
  }

  /** Ferme le panneau ouvert et rend le focus à son déclencheur. */
  protected onEscape(): void {
    const id = this.openMenuId();
    if (!id) {
      return;
    }
    this.closeMenu();
    this.focusTrigger(id);
  }

  /** Flèche droite sur un déclencheur : ouvre et entre dans le panneau. */
  protected onTriggerArrowRight(item: NavItem): void {
    this.cancelHoverTimers();
    this.openedViaKeyboard.set(true);
    this.openMenuId.set(item.id);
  }

  protected onTriggerEnter(item: NavItem): void {
    this.cancelHoverTimers();
    if (!this.openMenuId() || this.openMenuId() === item.id) {
      return;
    }
    this.hoverOpenTimer = window.setTimeout(() => {
      this.openedViaKeyboard.set(false);
      this.openMenuId.set(item.id);
    }, HOVER_OPEN_DELAY_MS);
  }

  /** Traversée rapide du rail : annule la bascule en attente. */
  protected onTriggerLeave(): void {
    this.cancelHoverTimers();
  }

  /**
   * Navigation depuis le panneau : on referme le panneau, sans toucher au rail.
   * Sur mobile, la sidebar déployée recouvre l'écran et doit se retirer.
   */
  protected onFlyoutNavigate(): void {
    this.closeMenu();
    if (this.isMobile() && !this.isCollapsed()) {
      this.toggleSidebar();
    }
  }

  /**
   * Le panneau n'a pas de backdrop, pour qu'un clic sur une autre entrée du
   * rail bascule directement de menu. Il faut donc ignorer les clics sur les
   * déclencheurs : sans ça, le clic fermerait ici puis rouvrirait via
   * `toggleMenu`, et le menu ne se fermerait jamais.
   */
  protected onOverlayOutsideClick(event: MouseEvent): void {
    const target = event.target as HTMLElement | null;
    if (target?.closest("[data-flyout-trigger]")) {
      return;
    }
    this.closeMenu();
  }

  /**
   * Navigation depuis une entrée simple du rail. Le rail ne se replie plus
   * après un clic : l'utilisateur perdait son menu à chaque navigation. Sur
   * mobile en revanche, la sidebar recouvre l'écran et doit se retirer.
   */
  protected onMenuItemClick(clickHandler?: () => void): void {
    if (clickHandler) {
      clickHandler();
    }
    this.closeMenu();
    if (this.isMobile() && !this.isCollapsed()) {
      this.toggleSidebar();
    }
  }

  protected login(): void {
    this.router.navigate(["/login"]);
  }

  protected logout(): void {
    this.loginService.logout();
    this.router.navigate([""]);
  }

  protected openAppSettings(): void {
    this.modalService.open(AppSettingsDialogComponent, {
      size: "lg",
      backdrop: "static",
      centered: true
    });
  }

  protected openConfigEditor(): void {
    void this.router.navigate(["/app-config"]);
  }


  private cancelHoverTimers(): void {
    if (this.hoverOpenTimer !== undefined) {
      window.clearTimeout(this.hoverOpenTimer);
      this.hoverOpenTimer = undefined;
    }
  }

  /**
   * L'identifiant vient du back-office (`code` du NavNode) : on compare la
   * valeur de l'attribut plutôt que de l'injecter dans un sélecteur, ce qui
   * évite d'avoir à l'échapper.
   */
  private focusTrigger(menuId: string): void {
    const triggers = document.querySelectorAll<HTMLElement>("[data-flyout-trigger]");
    for (const trigger of Array.from(triggers)) {
      if (trigger.dataset["flyoutTrigger"] === menuId) {
        trigger.focus();
        return;
      }
    }
  }


}

