import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, NavigationEnd, Router, RouterModule } from '@angular/router';
import { filter, map } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { BreadcrumbService } from './breadcrumb.service';
import { OfficineContextService } from '../../../core/config/officine-context.service';
import { AccountService } from '../../../core/auth/account.service';

export interface Breadcrumb {
  label: string;
  url: string;
}

@Component({
  selector: 'app-breadcrumb',
  imports: [RouterModule],
  template: `
    @if (breadcrumbs().length > 0 || tabCrumb()) {
      <nav aria-label="Fil d'Ariane" class="breadcrumb-nav">
        <ol class="breadcrumb mb-0">
          <li class="breadcrumb-item">
            <a routerLink="/" class="breadcrumb-home">{{ officineContext.officineName() }}</a>
          </li>
          @for (crumb of breadcrumbs(); track $index; let last = $last) {
            <li
              class="breadcrumb-item"
              [class.active]="last && !tabCrumb()"
              [attr.aria-current]="last && !tabCrumb() ? 'page' : null"
            >
              @if (!last || tabCrumb()) {
                <a [routerLink]="crumb.url">{{ crumb.label }}</a>
              } @else {
                {{ crumb.label }}
              }
            </li>
          }
          @if (tabCrumb()) {
            <li class="breadcrumb-item active" aria-current="page">
              {{ tabCrumb()!.label }}
            </li>
          }
        </ol>
      </nav>
    }
  `,
  styles: [`
    .breadcrumb-nav {
      background: var(--bs-tertiary-bg, #f8f9fa);
      border-bottom: 1px solid var(--bs-border-color, #dee2e6);
      padding: 0.35rem 1.25rem;
    }

    .breadcrumb {
      font-size: 0.82rem;
    }

    .breadcrumb-home {
      font-weight: 600;
    }

    .home-sublabel {
      font-weight: 400;
      opacity: 0.7;
      font-size: 0.78rem;
    }
  `],
})
export class BreadcrumbComponent {
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly translateService = inject(TranslateService);
  private readonly breadcrumbService = inject(BreadcrumbService);
  private readonly accountService = inject(AccountService);
  readonly officineContext = inject(OfficineContextService);

  /** Onglet actif poussé dynamiquement par les composants hôtes */
  readonly tabCrumb = this.breadcrumbService.tabCrumb;

  readonly breadcrumbs = toSignal(
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd),
      map(() => this.buildBreadcrumbs(this.activatedRoute.root)),
    ),
    { initialValue: this.buildBreadcrumbs(this.activatedRoute.root) },
  );

  constructor() {
    // Charger le nom de l'officine dès que l'utilisateur est authentifié
    this.accountService.getAuthenticationState().subscribe(account => {
      if (account) {
        this.officineContext.load();
      }
    });
  }

  private buildBreadcrumbs(route: ActivatedRoute, url = '', crumbs: Breadcrumb[] = []): Breadcrumb[] {
    const child = route.children.find(c => c.outlet === 'primary') ?? route.children[0];
    if (!child) return crumbs;

    const segment = child.snapshot.url.map(s => s.path).join('/');
    if (segment) url = `${url}/${segment}`;

    const label = this.resolveLabel(
      child.snapshot.data['breadcrumb'] ?? child.snapshot.data['pageTitle'] ?? child.snapshot.title,
    );
    if (label) crumbs.push({ label, url });

    return this.buildBreadcrumbs(child, url, crumbs);
  }

  /**
   * Résout un libellé : tente la traduction i18n uniquement si la clé ressemble à une clé i18n
   * (contient un point). Retourne le texte brut sinon.
   * Gère aussi le cas "translation-not-found[...]" de ngx-translate.
   */
  resolveLabel(pageTitle: string | undefined): string {
    if (!pageTitle) return '';

    // Texte brut (ne contient pas de point) → pas besoin de traduire
    if (!pageTitle.includes('.')) return pageTitle;

    const translated = this.translateService.instant(pageTitle);

    // ngx-translate retourne "translation-not-found[key]" ou la clé elle-même si absent
    if (!translated || translated === pageTitle || translated.startsWith('translation-not-found')) {
      // Fallback : extraire la dernière partie de la clé (ex: "facturation.factures" → "factures")
      const parts = pageTitle.split('.');
      return parts[parts.length - 1];
    }
    return translated;
  }
}


