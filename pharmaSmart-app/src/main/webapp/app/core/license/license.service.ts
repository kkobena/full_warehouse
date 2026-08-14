import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { catchError, map, Observable, of, tap } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { LicenseAuditEntry, LicenseInfo, OPTIONAL_FEATURES, PublisherContacts } from './license.model';

/**
 * État de licence partagé par la bannière, l'écran d'activation et les gardes de route.
 *
 * Porté par un **signal unique** dans un service racine : la bannière, le toast de connexion et
 * l'écran `licence` doivent afficher exactement la même chose au même instant. Dupliquer
 * l'état dans chaque consommateur ferait apparaître des divergences après une activation.
 *
 * Cf. docs/PLAN-GESTION-LICENCE.md §5.1.
 */
@Injectable({ providedIn: 'root' })
export class LicenseService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(ApplicationConfigService);
  private readonly notification = inject(NotificationService);

  private readonly _license = signal<LicenseInfo | null>(null);

  readonly license = this._license.asReadonly();

  /**
   * L'UI n'a qu'un rôle de confort : **le blocage fait autorité côté serveur**. Un utilisateur qui
   * forcerait ce signal via les outils de développement n'obtiendrait rien de plus qu'un 402.
   */
  readonly isReadOnly = computed(() => this._license()?.readOnly ?? false);

  readonly isDemo = computed(() => this._license()?.demo ?? false);

  readonly showBanner = computed(() => this._license()?.showBanner ?? false);

  readonly support = computed(() => this._license()?.support ?? { phones: [], emails: [] });

  /**
   * `true` si le module est accordé — même règle que `Feature.hasFeature()` côté serveur.
   *
   * Le périmètre couvert d'office l'est toujours ; seuls les modules optionnels (exclusions de CA)
   * exigent d'être listés dans la licence. Une licence ancienne, sans champ `features`, conserve
   * donc tout l'existant sans ouvrir aucune option.
   *
   * Statut non chargé ⇒ tout est accordé : le serveur reste seul juge et opposera un 402.
   */
  hasFeature(feature: string): boolean {
    if (!OPTIONAL_FEATURES.has(feature)) {
      return true;
    }
    const info = this._license();
    return info === null || info.features.includes(feature);
  }

  /**
   * Recharge le statut.
   *
   * Une erreur (401 avant connexion, backend indisponible) ne doit jamais empêcher l'application de
   * fonctionner : on retombe silencieusement sur « statut inconnu », donc pas de bannière et pas de
   * restriction côté UI — c'est le serveur qui tranchera à la première écriture.
   */
  load(): Observable<LicenseInfo | null> {
    return this.http.get<LicenseInfo>(this.config.getEndpointFor('api/license/status')).pipe(
      tap(info => this._license.set(info)),
      catchError(() => {
        this._license.set(null);
        return of(null);
      }),
    );
  }

  /** Appelé à la déconnexion : le statut suivant sera celui de la prochaine session. */
  clear(): void {
    this._license.set(null);
  }

  /**
   * Exigence **B2** — alerte à la connexion.
   *
   * Le seuil de déclenchement n'est pas recopié ici : il est configurable côté serveur
   * (`warning-threshold-days`) et déjà traduit dans le statut. Le dupliquer côté client ferait
   * diverger les deux dès le premier ajustement en production.
   */
  notifyIfExpiring(): void {
    const info = this._license();
    if (!info) {
      return;
    }

    const LONG_ENOUGH_TO_READ_AND_NOTE_A_PHONE_NUMBER = 15_000;

    switch (info.status) {
      case 'EXPIRING_SOON':
      case 'EXPIRING_CRITICAL':
        this.notification.show('warn', this.withSupport(info), 'Abonnement', LONG_ENOUGH_TO_READ_AND_NOTE_A_PHONE_NUMBER);
        break;
      case 'GRACE':
      case 'EXPIRED':
      case 'INVALID':
      case 'MISSING':
      case 'CLOCK_TAMPERED':
      case 'DEMO_QUOTA_REACHED':
        this.notification.show('error', this.withSupport(info), 'Licence', LONG_ENOUGH_TO_READ_AND_NOTE_A_PHONE_NUMBER);
        break;
      default:
        break;
    }
  }

  /**
   * Coordonnées de l'éditeur, servant la demande d'activation.
   *
   * Volontairement tolérant à l'échec : cet écran doit rester utilisable même si la configuration
   * ne renseigne aucun contact, auquel cas la section correspondante ne s'affiche simplement pas.
   */
  publisher(): Observable<PublisherContacts | null> {
    return this.http
      .get<PublisherContacts>(this.config.getEndpointFor('api/license/publisher'))
      .pipe(catchError(() => of(null)));
  }

  /** Empreinte du poste, à transmettre à l'éditeur s'il demande une licence liée au matériel. */
  fingerprint(): Observable<string> {
    return this.http
      .get<{ fingerprint: string }>(this.config.getEndpointFor('api/license/fingerprint'))
      .pipe(map(response => response.fingerprint));
  }

  /** Dépôt du fichier `.lic`. Le statut est rafraîchi avec la réponse : aucun rechargement requis. */
  activate(file: File): Observable<LicenseInfo> {
    const form = new FormData();
    form.append('file', file);
    return this.http
      .post<LicenseInfo>(this.config.getEndpointFor('api/license'), form)
      .pipe(tap(info => this._license.set(info)));
  }

  audit(size = 50): Observable<LicenseAuditEntry[]> {
    const params = new HttpParams().set('page', 0).set('size', size);
    return this.http.get<LicenseAuditEntry[]>(this.config.getEndpointFor('api/license/audit'), { params });
  }

  /**
   * Le message serveur explique la situation ; on y accole le contact du revendeur, pour que
   * l'utilisateur bloqué n'ait pas à chercher qui appeler.
   */
  private withSupport(info: LicenseInfo): string {
    const phone = info.support?.phones?.[0];
    return phone ? `${info.message} Contactez votre revendeur au ${phone}.` : info.message;
  }
}
