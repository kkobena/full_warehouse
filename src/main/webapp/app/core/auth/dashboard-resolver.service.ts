import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { EMPTY, Observable, tap } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { DashboardLayoutService } from 'app/entities/dashboard/dashboard-layout.service';
import { IDashboardLayout } from 'app/shared/model/dashboard-layout.model';

/**
 * Résout et met en cache le layout de dashboard de l'utilisateur connecté.
 *
 * Logique de résolution (côté backend via GET /api/dashboard-layouts/resolved) :
 *  1. Layout personnel (user isDefault=true)
 *  2. Layout par rôle  (authority isDefault=true)
 *  3. null             → HomeComponent affiche DefaultDashboard
 *
 * Cache frontend (signal) :
 *  - Chargé une fois après la connexion via init()
 *  - resolvedLayout() retourne le layout en cache ou null si non chargé
 *  - reset() vide le cache à la déconnexion
 */
@Injectable({ providedIn: 'root' })
export class DashboardResolverService {

  private readonly layoutService = inject(DashboardLayoutService);
  private readonly router = inject(Router);

  /** null = pas encore chargé ou aucun layout configuré */
  private readonly _resolvedLayout = signal<IDashboardLayout | null>(null);
  private readonly _loaded = signal(false);

  /** Layout résolu (null = DefaultDashboard). */
  readonly resolvedLayout = this._resolvedLayout.asReadonly();

  /** True une fois que la résolution a été effectuée (chargé ou absence confirmée). */
  readonly loaded = this._loaded.asReadonly();

  /**
   * Indique si le layout résolu est une redirection de route.
   * Si true → utiliser routePath() pour la navigation.
   */
  readonly isRoute = computed(() => this._resolvedLayout()?.isRoute === true);

  /**
   * Route Angular à utiliser si isRoute=true.
   * Ex: '/sales-home/prevente', '/caissier', '/commande'
   */
  readonly routePath = computed(() =>
    this.isRoute() ? (this._resolvedLayout()?.name ?? '/') : null
  );

  /**
   * Charge le layout résolu depuis le backend et le met en cache dans le signal.
   * Idempotent : si déjà chargé (_loaded=true), ne refait pas l'appel HTTP.
   * Appelé une fois dans HomeComponent.ngOnInit() après authentification.
   */
  init(): Observable<IDashboardLayout | null> {
    if (this._loaded()) {
      return EMPTY;
    }

    return this.layoutService.getResolved().pipe(
      map(response => response.body),
      tap(layout => {
        console.log(layout,'*************************************************');
        this._resolvedLayout.set(layout);
        this._loaded.set(true);
      }),
      catchError(() => {
        // En cas d'erreur réseau : pas de crash, DefaultDashboard affiché
        this._resolvedLayout.set(null);
        this._loaded.set(true);
        return EMPTY;
      })
    );
  }

  /**
   * Vide le cache signal.
   * À appeler lors de la déconnexion (AccountService.logout()).
   */
  reset(): void {
    this._resolvedLayout.set(null);
    this._loaded.set(false);
  }

  /**
   * Force un rechargement (après setAsDefault, par exemple).
   * Remet _loaded à false puis rappelle init().
   */
  reload(): Observable<IDashboardLayout | null> {
    this._loaded.set(false);
    return this.init();
  }
}
