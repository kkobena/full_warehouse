import { ChangeDetectionStrategy, Component, computed, effect, inject, input, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AbilityService } from 'app/core/auth/ability.service';
import { AccountService } from 'app/core/auth/account.service';
import { IDashboardLayout } from 'app/shared/model/dashboard-layout.model';
import { ButtonComponent, SkeletonComponent, ToolbarComponent } from 'app/shared/ui';
import { AllowedWidget } from '../../models/dashboard.model';
import { parseLayoutConfig } from '../../models/layout-config';
import { DEFAULT_CONTEXT } from '../../models/period';
import { widgetAvailability } from '../../models/widget-availability';
import { DashboardContext } from '../../data-access/dashboard-context';
import { DashboardWidgetApiService } from '../../data-access/services/dashboard-widget-api.service';
import { DashboardGridComponent } from '../../ui/dashboard-grid/dashboard-grid.component';
import { DashboardContextBarComponent } from '../../ui/context-bar/dashboard-context-bar.component';

/**
 * Dashboard personnalisable en lecture seule : l'accueil de l'utilisateur quand il en a choisi un.
 * La période s'y change le temps de la consultation, sans modifier le dashboard enregistré.
 */
@Component({
  selector: 'app-dashboard-view',
  imports: [ToolbarComponent, SkeletonComponent, ButtonComponent, DashboardGridComponent, DashboardContextBarComponent],
  providers: [DashboardContext],
  template: `
    <div class="pharma-smart-content p-2">
      <app-toolbar icon="pi pi-th-large" [title]="layout().name ?? 'Tableau de bord'" [compact]="true">
        <ng-container ngProjectAs="[toolbarFilters]">
          <app-dashboard-context-bar />
        </ng-container>
        <ng-container ngProjectAs="[toolbarActions]">
          @if (canEdit()) {
            <app-button label="Personnaliser" icon="pi pi-pencil" size="small" [outlined]="true" (clicked)="edit()" />
          }
        </ng-container>
      </app-toolbar>

      @if (config().items.length === 0) {
        <p class="text-center text-body-secondary py-5">Ce tableau de bord est vide.</p>
      } @else if (allowed() === null) {
        <app-skeleton height="320px" width="100%" />
      } @else {
        <app-dashboard-grid [items]="config().items" [availability]="availability()" [editable]="false" />
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardViewComponent {
  readonly layout = input.required<IDashboardLayout>();

  private readonly context = inject(DashboardContext);
  private readonly router = inject(Router);
  private readonly ability = inject(AbilityService);
  private readonly account = inject(AccountService).trackCurrentAccount();
  /** null tant que les droits ne sont pas connus : la grille attend, plutôt que d'afficher des refus. */
  protected readonly allowed = toSignal(
    inject(DashboardWidgetApiService)
      .allowed()
      .pipe(catchError(() => of<AllowedWidget[]>([]))),
    { initialValue: null },
  );

  protected readonly config = computed(() => parseLayoutConfig(this.layout().layoutConfig).config);
  protected readonly availability = computed(() => {
    const allowed = this.allowed() ?? [];
    return new Map(this.config().items.map(i => [i.id, widgetAvailability(i.widgetKey, allowed)]));
  });
  /** Seul le propriétaire, s'il a accès à l'éditeur, personnalise son accueil. */
  protected readonly canEdit = computed(
    () => this.layout().userLogin === this.account()?.login && this.ability.can('access', 'dashboard-perso'),
  );

  protected edit(): void {
    this.router.navigate(['/dashboard', this.layout().id]);
  }

  constructor() {
    // la période enregistrée avec le dashboard est celle de l'ouverture
    effect(() => {
      const settings = this.config().context ?? DEFAULT_CONTEXT;
      untracked(() => this.context.apply(settings));
    });
  }
}
