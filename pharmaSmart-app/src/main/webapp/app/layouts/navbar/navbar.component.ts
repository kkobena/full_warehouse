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
import {filter, fromEvent} from "rxjs";
import {environment} from "environments/environment";
import {AccountService} from "app/core/auth/account.service";
import {LoginService} from "app/login/login.service";
import {NavItem} from "./navbar-item.model";
import {faChevronDown, faSlidersH} from "@fortawesome/free-solid-svg-icons";
import {NgbCollapse, NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {CdkConnectedOverlay, CdkOverlayOrigin, ConnectedPosition} from "@angular/cdk/overlay";
import {NavFlyoutComponent} from "../shared/nav-flyout/nav-flyout.component";
import {AppSettingsDialogComponent} from "../../shared/settings/app-settings-dialog.component";
import {Authority} from "../../config/authority.constants";
import {NavigationService} from "../../core/config/navigation.service";
import {TauriPrinterService} from "../../shared/services/tauri-printer.service";
import {AlertBadgeService} from "../../shared/services/alert-badge.service";
import {NavStore} from "app/core/store/nav.store";
import {CommonModule} from "@angular/common";
import TranslateDirective from "../../shared/language/translate.directive";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";

/** Délai avant qu'un survol ne bascule vers un autre menu déjà ouvert. */
const HOVER_OPEN_DELAY_MS = 120;

@Component({
  selector: "app-navbar",
  templateUrl: "./navbar.component.html",
  styleUrl: "./navbar.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterModule,
    CommonModule,
    TranslateDirective,
    FaIconComponent,
    NgbCollapse,
    CdkOverlayOrigin,
    CdkConnectedOverlay,
    NavFlyoutComponent
  ],
  host: {
    "(document:keydown.escape)": "onEscape()"
  }
})
export default class NavbarComponent implements OnInit {
  /** Identifiant du seul menu ouvert, ou `null`. */
  readonly openMenuId = signal<string | null>(null);
  /** L'ouverture courante vient-elle du clavier ? Conditionne le vol de focus. */
  readonly openedViaKeyboard = signal(false);

  /**
   * Positions du panneau : sous le déclencheur, aligné à gauche, puis à droite
   * si le menu est proche du bord (la navbar aligne ses entrées avec `ms-auto`),
   * puis au-dessus si la place manque en bas.
   */
  readonly flyoutPositions: ConnectedPosition[] = [
    {originX: "start", originY: "bottom", overlayX: "start", overlayY: "top", offsetY: 4},
    {originX: "end", originY: "bottom", overlayX: "end", overlayY: "top", offsetY: 4},
    {originX: "start", originY: "top", overlayX: "start", overlayY: "bottom", offsetY: -4}
  ];

  protected readonly faSlidersH = faSlidersH;
  protected readonly faChevronDown = faChevronDown;
  protected isNavbarCollapsed = signal(true);
  protected readonly isMobileSignal = signal(window.innerWidth <= 768);
  protected readonly version = signal("");
  protected account = inject(AccountService).trackCurrentAccount();
  // Signaux et non propriétés mutées : `navItems` est affecté depuis un `effect()` — le store de
  // navigation se charge après le premier rendu — et rien n'y salit la vue. Sous `OnPush`, le menu
  // serait resté vide ou figé sur son premier état.
  protected readonly navItems = signal<NavItem[]>([]);
  protected readonly alertBadgeService = inject(AlertBadgeService);
  private hoverOpenTimer?: number;
  private readonly loginService = inject(LoginService);
  private readonly router = inject(Router);
  private readonly modalService = inject(NgbModal);
  private readonly navigationService = inject(NavigationService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly navStore = inject(NavStore);

  constructor() {

    const {VERSION} = environment;
    if (VERSION) {
      this.version.set(VERSION.toLowerCase().startsWith("v") ? VERSION : `v${VERSION}`);
    }
    const destroyRef = inject(DestroyRef);

    fromEvent(window, "resize")
      .pipe(takeUntilDestroyed(destroyRef))
      .subscribe(() => this.isMobileSignal.set(window.innerWidth <= 768));

    // Une navigation déclenchée hors du panneau doit aussi le refermer.
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntilDestroyed(destroyRef)
      )
      .subscribe(() => this.closeMenu());

    destroyRef.onDestroy(() => this.cancelHoverTimers());

    effect(() => {
      // Reactive: rebuilds nav items whenever account, navTree (store) or alert counts change
      this.navStore.navTree(); // déclenche la réactivité quand le store se charge
      const items = this.navigationService.buildNavItems({
        onLogin: () => this.login(),
        onLogout: () => this.logout(),
        onOpenConfigEditor: () => this.openConfigEditor(),
        onOpenAppSettings: () => this.openAppSettings(),
        onOpenCahierRecette: () => this.openCahierRecette()
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
  }

  protected toggleNavbar(): void {
    this.isNavbarCollapsed.update(isNavbarCollapsed => !isNavbarCollapsed);
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

  protected openCahierRecette(): void {
    void this.router.navigate(["/cahier-recette"]);
  }

  protected isMobile(): boolean {
    return this.isMobileSignal();
  }

  /** Entrée simple du premier niveau : action éventuelle, puis repli mobile. */
  protected onLeafClick(item: NavItem): void {
    item.click?.();
    this.collapseNavbar();
  }

  /** Le menu Compte porte un identifiant stable depuis l'ajout de `NavItem.id`. */
  protected isAccountMenu(item: NavItem): boolean {
    return item.id === "account";
  }

  /**
   * Ouvre le panneau de l'entrée, ou le referme s'il l'était déjà.
   * `event.detail === 0` distingue une activation clavier (Entrée/Espace) d'un
   * vrai clic souris : seule la première justifie de déplacer le focus dans le
   * panneau.
   */
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

  /** Flèche bas sur un déclencheur : ouvre et entre dans le panneau. */
  protected onTriggerArrowDown(item: NavItem): void {
    this.cancelHoverTimers();
    this.openedViaKeyboard.set(true);
    this.openMenuId.set(item.id);
  }

  /**
   * Survol : ne sert qu'à **changer** de menu quand un panneau est déjà ouvert,
   * comme dans une barre de menus classique. Même règle que la sidebar.
   */
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

  protected onTriggerLeave(): void {
    this.cancelHoverTimers();
  }

  /** Navigation depuis le panneau : on referme, et la navbar mobile avec. */
  protected onFlyoutNavigate(): void {
    this.closeMenu();
    this.collapseNavbar();
  }

  /**
   * Sans backdrop, un clic sur une autre entrée bascule directement de menu.
   * Il faut donc ignorer les clics portés par un déclencheur, sinon le panneau
   * se fermerait ici puis se rouvrirait via `toggleMenu`.
   */
  protected onOverlayOutsideClick(event: MouseEvent): void {
    const target = event.target as HTMLElement | null;
    if (target?.closest("[data-flyout-trigger]")) {
      return;
    }
    this.closeMenu();
  }

  private cancelHoverTimers(): void {
    if (this.hoverOpenTimer !== undefined) {
      window.clearTimeout(this.hoverOpenTimer);
      this.hoverOpenTimer = undefined;
    }
  }

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

