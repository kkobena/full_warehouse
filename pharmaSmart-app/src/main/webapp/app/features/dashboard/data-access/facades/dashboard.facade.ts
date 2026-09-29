import { computed, inject, Injectable } from '@angular/core';
import { Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { AccountService } from 'app/core/auth/account.service';
import { DashboardResolverService } from 'app/core/auth/dashboard-resolver.service';
import { NgbConfirmDialogService } from 'app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { NotificationService } from 'app/shared/services/notification.service';
import { DashboardScope, IDashboardLayout } from 'app/shared/model/dashboard-layout.model';
import { WidgetDefinition } from '../../models/dashboard.model';
import { parseLayoutConfig, serializeLayoutConfig } from '../../models/layout-config';
import { DEFAULT_CONTEXT } from '../../models/period';
import { DashboardTemplate, instantiateTemplate } from '../../models/dashboard-templates';
import { DashboardLayoutApiService } from '../services/dashboard-layout-api.service';
import { DashboardWidgetApiService } from '../services/dashboard-widget-api.service';
import { DashboardStore } from '../store/dashboard.store';
import { DashboardContext } from '../dashboard-context';
import { errorMessage } from '../error-message';
import { SaveDashboardModalComponent, SaveDashboardResult } from '../../ui/save-dashboard-modal/save-dashboard-modal.component';
import { OpenDashboardModalComponent } from '../../ui/open-dashboard-modal/open-dashboard-modal.component';
import { WidgetSettingsModalComponent, WidgetSettingsResult } from '../../ui/widget-settings-modal/widget-settings-modal.component';
import { AssignRoleModalComponent } from '../../ui/assign-role-modal/assign-role-modal.component';
import { findWidgetDefinition } from '../../widgets/widget-registry';

export const CUSTOM_COMPONENT_KEY = 'CUSTOM';
const ADMIN_AUTHORITY = 'ROLE_ADMIN';

/** Orchestration de l'éditeur : chargement, enregistrement, accueil. Fournie par l'écran, comme le store. */
@Injectable()
export class DashboardFacade {
  readonly store = inject(DashboardStore);
  readonly context = inject(DashboardContext);

  private readonly layoutApi = inject(DashboardLayoutApiService);
  private readonly widgetApi = inject(DashboardWidgetApiService);
  private readonly modal = inject(NgbModal);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notification = inject(NotificationService);
  private readonly resolver = inject(DashboardResolverService);
  private readonly location = inject(Location);
  private readonly account = inject(AccountService).trackCurrentAccount();

  /** Le dashboard ouvert appartient à l'utilisateur : « Enregistrer » le met à jour au lieu d'en créer un. */
  readonly canOverwrite = computed(() => {
    const layout = this.store.layout();
    return layout?.id != null && layout.userLogin === this.account()?.login;
  });
  readonly isHome = computed(() => this.canOverwrite() && this.store.layout()?.isDefault === true);
  /** L'attribution d'un accueil à un rôle est réservée à l'administrateur (le serveur le vérifie aussi). */
  readonly isAdmin = computed(() => this.account()?.authorities?.includes(ADMIN_AUTHORITY) === true);

  /** Ouvre le dashboard demandé, à défaut l'accueil personnel s'il est personnalisable, sinon un dashboard vierge. */
  init(id: number | null): void {
    this.store.setLoading(true);
    this.widgetApi.allowed().subscribe({
      next: allowed => this.store.setAllowed(allowed),
      error: () => this.store.setAllowed([]),
    });

    const source$ = id != null ? this.layoutApi.find(id) : this.layoutApi.getDefault();
    source$.subscribe({
      next: res => {
        const layout = res.body;
        if (layout && layout.componentKey !== CUSTOM_COMPONENT_KEY) {
          if (id != null) {
            this.notification.warning("Ce tableau de bord n'est pas personnalisable.");
          }
          this.openLayout(null);
          return;
        }
        this.openLayout(layout);
      },
      error: (err: HttpErrorResponse) => {
        this.openLayout(null);
        if (id != null) {
          this.notification.error(errorMessage(err, 'Tableau de bord introuvable.'));
        }
      },
    });
  }

  newDashboard(): void {
    this.confirmIfDirty(() => this.openLayout(null));
  }

  /** Nouveau dashboard pré-rempli ; les widgets que l'utilisateur n'a pas le droit de voir en sont retirés. */
  newFromTemplate(template: DashboardTemplate): void {
    this.confirmIfDirty(() => {
      const items = instantiateTemplate(template, this.store.allowed());
      this.openLayout(null);
      this.context.apply(template.context);
      this.store.open(null, items);
      this.store.markDirty();
      this.store.setEditMode(true);
      if (items.length < template.items.length) {
        this.notification.info(`${template.items.length - items.length} widget(s) du modèle ne vous sont pas autorisés et ont été retirés.`);
      }
    });
  }

  openDialog(): void {
    this.confirmIfDirty(() => {
      const ref = this.modal.open(OpenDashboardModalComponent, { size: 'lg', centered: true, scrollable: true });
      const dialog = ref.componentInstance as OpenDashboardModalComponent;
      dialog.currentLogin = this.account()?.login ?? '';
      dialog.openedId = this.store.layout()?.id ?? null;
      ref.result.then(
        (layout: IDashboardLayout) => this.openLayout(layout),
        (): void => undefined,
      );
    });
  }

  toggleEditMode(): void {
    this.store.setEditMode(!this.store.editMode());
  }

  addWidget(definition: WidgetDefinition): void {
    this.store.addWidget(definition);
    this.store.setEditMode(true);
  }

  duplicateItem(id: string): void {
    this.store.duplicateItem(id);
  }

  /** Fenêtre de réglages d'une tuile ; « Annuler » ne change rien. */
  openSettings(id: string): void {
    const item = this.store.items().find(i => i.id === id);
    const definition = item && findWidgetDefinition(item.widgetKey);
    if (!item || !definition) {
      return;
    }
    const ref = this.modal.open(WidgetSettingsModalComponent, { centered: true });
    (ref.componentInstance as WidgetSettingsModalComponent).init(item, definition);
    ref.result.then(
      (result: WidgetSettingsResult) => this.store.updateSettings(id, result.title, result.params),
      (): void => undefined,
    );
  }

  /** Met à jour le dashboard s'il est à l'utilisateur ; sinon en crée un (dashboard vierge ou partagé par un autre). */
  save(): void {
    const layout = this.store.layout();
    if (!layout || !this.canOverwrite()) {
      this.saveAs();
      return;
    }
    this.persist(this.layoutApi.update(layout.id!, this.toDto(layout)), 'Tableau de bord enregistré.');
  }

  saveAs(): void {
    const ref = this.modal.open(SaveDashboardModalComponent, { centered: true });
    const dialog = ref.componentInstance as SaveDashboardModalComponent;
    const current = this.store.layout();
    if (current?.name) {
      dialog.name.set(this.canOverwrite() ? `${current.name} (copie)` : current.name);
      dialog.description.set(current.description ?? '');
    }
    ref.result.then(
      (result: SaveDashboardResult) =>
        this.persist(
          this.layoutApi.create(this.toDto({ name: result.name, description: result.description, scope: result.scope, isDefault: false })),
          'Tableau de bord créé.',
        ),
      (): void => undefined,
    );
  }

  /** Affiche ce dashboard à l'accueil de l'utilisateur. */
  setAsHome(): void {
    const layout = this.store.layout();
    if (!layout?.id || !this.canOverwrite()) {
      this.notification.warning("Enregistrez d'abord ce tableau de bord à votre nom.");
      return;
    }
    if (this.store.dirty()) {
      this.notification.warning("Enregistrez d'abord vos modifications.");
      return;
    }
    this.layoutApi.setAsDefault(layout.id).subscribe({
      next: res => {
        this.store.markSaved(res.body ?? { ...layout, isDefault: true });
        this.resolver.reload().subscribe();
        this.notification.success("Ce tableau de bord s'affichera désormais à l'accueil.");
      },
      error: (err: HttpErrorResponse) => this.notification.error(errorMessage(err)),
    });
  }

  /** Fait de ce dashboard l'accueil d'un rôle. Il doit être enregistré, sans modification en cours. */
  assignToRole(): void {
    const layout = this.store.layout();
    if (!layout?.id || this.store.dirty()) {
      this.notification.warning("Enregistrez d'abord ce tableau de bord.");
      return;
    }
    const ref = this.modal.open(AssignRoleModalComponent, { centered: true });
    (ref.componentInstance as AssignRoleModalComponent).dashboardName = layout.name ?? '';
    ref.result.then(
      (role: string) =>
        this.layoutApi.setAsDefaultForRole(layout.id!, role).subscribe({
          next: res => {
            if (res.body) {
              this.store.markSaved({ ...res.body, isDefault: layout.isDefault });
            }
            this.resolver.reload().subscribe();
            this.notification.success(`« ${layout.name} » est désormais l'accueil du rôle ${role}.`);
          },
          error: (err: HttpErrorResponse) => this.notification.error(errorMessage(err)),
        }),
      (): void => undefined,
    );
  }

  /** Pour la garde de sortie : `true` si l'utilisateur peut quitter l'écran. */
  confirmLeave(): Promise<boolean> {
    if (!this.store.dirty()) {
      return Promise.resolve(true);
    }
    return new Promise(resolve =>
      this.confirmDialog.onConfirm(
        () => resolve(true),
        'Modifications non enregistrées',
        'Quitter le tableau de bord sans enregistrer vos modifications ?',
        undefined,
        () => resolve(false),
      ),
    );
  }

  private openLayout(layout: IDashboardLayout | null): void {
    const { config, legacyItemsDropped } = parseLayoutConfig(layout?.layoutConfig);
    this.context.apply(config.context ?? DEFAULT_CONTEXT);
    this.store.open(layout, config.items);
    this.store.setLoading(false);
    this.location.replaceState(layout?.id != null ? `/dashboard/${layout.id}` : '/dashboard');
    if (legacyItemsDropped > 0) {
      this.notification.info(`${legacyItemsDropped} tuile(s) de l'ancien format, vides, ont été retirées.`);
    }
  }

  private persist(request$: Observable<{ body: IDashboardLayout | null }>, message: string): void {
    this.store.setSaving(true);
    request$.subscribe({
      next: res => {
        this.store.setSaving(false);
        if (res.body) {
          this.store.markSaved(res.body);
          this.location.replaceState(`/dashboard/${res.body.id}`);
          if (res.body.isDefault) {
            this.resolver.reload().subscribe();
          }
        }
        this.notification.success(message);
      },
      error: (err: HttpErrorResponse) => {
        this.store.setSaving(false);
        this.notification.error(errorMessage(err));
      },
    });
  }

  private toDto(base: Pick<IDashboardLayout, 'name' | 'description' | 'scope' | 'isDefault'> & { id?: number }): IDashboardLayout {
    return {
      id: base.id,
      name: base.name,
      description: base.description,
      scope: base.scope ?? DashboardScope.PRIVATE,
      // à conserver : le serveur recopie isDefault, l'omettre retirerait le dashboard de l'accueil
      isDefault: base.isDefault ?? false,
      isRoute: false,
      componentKey: CUSTOM_COMPONENT_KEY,
      layoutConfig: serializeLayoutConfig(this.store.items(), this.context.settings()),
    };
  }

  private confirmIfDirty(action: () => void): void {
    if (!this.store.dirty()) {
      action();
      return;
    }
    this.confirmDialog.onConfirm(action, 'Modifications non enregistrées', 'Abandonner les modifications en cours ?');
  }
}
