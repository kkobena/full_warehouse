import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { AppSplitButtonItem, BadgeComponent, ButtonComponent, SkeletonComponent, SplitButtonComponent, ToolbarComponent } from 'app/shared/ui';
import { WidgetDefinition } from '../../models/dashboard.model';
import { DASHBOARD_TEMPLATES, DashboardTemplate, instantiateTemplate } from '../../models/dashboard-templates';
import { DashboardStore } from '../../data-access/store/dashboard.store';
import { DashboardFacade } from '../../data-access/facades/dashboard.facade';
import { DashboardContext } from '../../data-access/dashboard-context';
import { DashboardGridComponent } from '../../ui/dashboard-grid/dashboard-grid.component';
import { AddWidgetPanelComponent } from '../../ui/add-widget-panel/add-widget-panel.component';
import { DashboardContextBarComponent } from '../../ui/context-bar/dashboard-context-bar.component';

/** Éditeur du dashboard personnalisable (`/dashboard` et `/dashboard/:id`). */
@Component({
  selector: 'app-dashboard-editor',
  imports: [
    ToolbarComponent,
    ButtonComponent,
    SplitButtonComponent,
    BadgeComponent,
    SkeletonComponent,
    DashboardGridComponent,
    AddWidgetPanelComponent,
    DashboardContextBarComponent,
  ],
  providers: [DashboardStore, DashboardContext, DashboardFacade],
  templateUrl: './dashboard-editor.component.html',
  styleUrl: './dashboard-editor.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardEditorComponent {
  /** Paramètre de route `:id`, lié par withComponentInputBinding. */
  readonly id = input<string>();

  protected readonly facade = inject(DashboardFacade);
  protected readonly store = this.facade.store;
  protected readonly panelVisible = signal(false);

  protected readonly title = computed(() => this.store.layout()?.name ?? 'Nouveau tableau de bord');
  protected readonly saveMenu = computed<AppSplitButtonItem[]>(() => [
    { label: 'Enregistrer sous…', icon: 'pi pi-copy', command: () => this.facade.saveAs() },
    {
      label: 'Définir comme mon accueil',
      icon: 'pi pi-home',
      disabled: !this.facade.canOverwrite() || this.facade.isHome(),
      command: () => this.facade.setAsHome(),
    },
    ...(this.facade.isAdmin()
      ? [{ label: "Accueil d'un rôle…", icon: 'pi pi-users', disabled: !this.store.isSaved(), command: () => this.facade.assignToRole() }]
      : []),
  ]);

  /** Modèles dont au moins un widget est autorisé à l'utilisateur. */
  protected readonly templates = computed(() =>
    DASHBOARD_TEMPLATES.filter(t => instantiateTemplate(t, this.store.allowed()).length > 0),
  );

  protected useTemplate(template: DashboardTemplate): void {
    this.facade.newFromTemplate(template);
  }

  constructor() {
    effect(() => {
      const raw = this.id();
      const id = raw != null && /^\d+$/.test(raw) ? Number(raw) : null;
      untracked(() => this.facade.init(id));
    });
  }

  protected onAdd(definition: WidgetDefinition): void {
    this.facade.addWidget(definition);
    this.panelVisible.set(false);
  }

  protected openPanel(): void {
    this.store.setEditMode(true);
    this.panelVisible.set(true);
  }

  /** Appelé par la garde de sortie. */
  confirmLeave(): Promise<boolean> {
    return this.facade.confirmLeave();
  }
}
