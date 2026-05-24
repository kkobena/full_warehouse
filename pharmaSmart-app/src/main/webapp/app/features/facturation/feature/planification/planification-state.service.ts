import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs/operators';

import { NotificationService } from '../../../../shared/services/notification.service';
import { IGroupeTiersPayant } from '../../../../shared/model/groupe-tierspayant.model';
import { ITiersPayant } from '../../../../shared/model';
import {
  IHistoriqueCertificationFne,
  IHistoriquePlanification,
  IPlanification,
  IPlanificationFne,
} from '../../data-access/models';
import { PlanificationApiService } from '../../data-access/services/planification-api.service';
import { NgbConfirmDialogService } from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { GroupeTiersPayantService } from "../../../../entities/groupe-tiers-payant/groupe-tierspayant.service";
import { TiersPayantService } from "../../../../entities/tiers-payant/tierspayant.service";

@Injectable()
export class PlanificationStateService {
  // ── Options ───────────────────────────────────────────────────────────
  readonly periodicitesOptions = [
    { label: 'Tout', value: null },
    { label: 'Mensuel', value: 'MENSUEL' },
    { label: 'Quinzainière', value: 'QUINZAINE' },
    { label: 'Bimensuel', value: 'BIMENSUEL' },
  ];
  readonly categorieOptions = [
    { label: 'Tout', value: null },
    { label: 'Assurance', value: 'ASSURANCE' },
    { label: 'Carnet', value: 'CARNET' },
    { label: 'Dépôt', value: 'DEPOT' },
  ];

  // ── Global ────────────────────────────────────────────────────────────
  readonly planifications = signal<IPlanification[]>([]);
  readonly loading = signal(false);
  readonly guideVisible = signal(false);

  // ── Computed lists ────────────────────────────────────────────────────
  readonly planificationsDefinitives = computed(() => this.planifications().filter(p => !p.factureProvisoire));
  readonly planificationsProvisoires = computed(() => this.planifications().filter(p => !!p.factureProvisoire));

  // ── Selected plans ────────────────────────────────────────────────────
  readonly selectedPlanDef = signal<IPlanification | null>(null);
  readonly selectedPlanProv = signal<IPlanification | null>(null);

  // ── Sub-tabs active ids ───────────────────────────────────────────────
  readonly activeSubTabDef = signal('exec');
  readonly activeSubTabProv = signal('exec');

  // ── Historique ────────────────────────────────────────────────────────
  readonly historiques = signal<Record<number, IHistoriquePlanification[]>>({});
  private readonly _loadingHistorique = signal<Record<number, boolean>>({});

  // ── Groupes ───────────────────────────────────────────────────────────
  readonly groupesDef = signal<IGroupeTiersPayant[]>([]);
  readonly groupesProv = signal<IGroupeTiersPayant[]>([]);
  readonly loadingGroupesDef = signal(false);
  readonly loadingGroupesProv = signal(false);
  selectedGroupesDef: IGroupeTiersPayant[] = [];
  selectedGroupesProv: IGroupeTiersPayant[] = [];
  massPeriodiciteGroupesDef: string | null = null;
  massPeriodiciteGroupesProv: string | null = null;
  filtrePeriodiciteGroupesDef: string | null = null;
  filtrePeriodiciteGroupesProv: string | null = null;

  // ── Tiers payants ─────────────────────────────────────────────────────
  readonly tpsDef = signal<ITiersPayant[]>([]);
  readonly tpsProv = signal<ITiersPayant[]>([]);
  readonly loadingTpsDef = signal(false);
  readonly loadingTpsProv = signal(false);
  selectedTpsDef: ITiersPayant[] = [];
  selectedTpsProv: ITiersPayant[] = [];
  massPeriodiciteTPsDef: string | null = null;
  massPeriodiciteTPsProv: string | null = null;
  filtrePeriodiciteTPsDef: string | null = null;
  filtrePeriodiciteTPsProv: string | null = null;
  filtreCategorieTpsDef: string | null = null;
  filtreCategorieTpsProv: string | null = null;

  // ── FNE ───────────────────────────────────────────────────────────────
  readonly planificationFne = signal<IPlanificationFne | null>(null);
  readonly loadingFne = signal(false);
  readonly historiquesFne = signal<IHistoriqueCertificationFne[]>([]);
  readonly loadingHistoriqueFne = signal(false);

  private readonly api = inject(PlanificationApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly groupeService = inject(GroupeTiersPayantService);
  private readonly tpService = inject(TiersPayantService);

  // ── Load ──────────────────────────────────────────────────────────────
  load(): void {
    this.loading.set(true);
    this.api
      .getAll()
      .pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.planifications.set(res.body ?? []),
        error: () => this.notificationService.error('Erreur lors du chargement des planifications'),
      });
  }

  // ── Plan selection ────────────────────────────────────────────────────
  onSelectPlanDef(plan: IPlanification | null): void {
    this.selectedPlanDef.set(plan);
    this.activeSubTabDef.set('exec');
    this.groupesDef.set([]);
    this.tpsDef.set([]);
    this.selectedGroupesDef = [];
    this.selectedTpsDef = [];
    if (plan?.id) this.loadHistoriqueForPlan(plan.id);
  }

  onSelectPlanProv(plan: IPlanification | null): void {
    this.selectedPlanProv.set(plan);
    this.activeSubTabProv.set('exec');
    this.groupesProv.set([]);
    this.tpsProv.set([]);
    this.selectedGroupesProv = [];
    this.selectedTpsProv = [];
    if (plan?.id) this.loadHistoriqueForPlan(plan.id);
  }

  // ── Sub-tab changes ───────────────────────────────────────────────────
  onSubTabChangeDef(tab: string): void {
    this.activeSubTabDef.set(tab);
    const plan = this.selectedPlanDef();
    if (!plan) return;
    if (tab === 'groupes' && this.groupesDef().length === 0) this.loadGroupesDef();
    else if (tab === 'tps' && this.tpsDef().length === 0) this.loadTpsDef();
  }

  onSubTabChangeProv(tab: string): void {
    this.activeSubTabProv.set(tab);
    const plan = this.selectedPlanProv();
    if (!plan) return;
    if (tab === 'groupes' && this.groupesProv().length === 0) this.loadGroupesProv();
    else if (tab === 'tps' && this.tpsProv().length === 0) this.loadTpsProv();
  }

  // ── Historique ────────────────────────────────────────────────────────
  loadHistoriqueForPlan(id: number): void {
    if (this.historiques()[id]) return;
    this._loadingHistorique.update(s => ({ ...s, [id]: true }));
    this.api
      .getHistorique(id, { page: 0, size: 30 })
      .pipe(
        finalize(() => {
          this._loadingHistorique.update(s => {
            const next = { ...s };
            delete next[id];
            return next;
          });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: res => this.historiques.update(s => ({ ...s, [id]: res.body ?? [] })),
        error: () => this.notificationService.error('Erreur chargement historique'),
      });
  }

  getHistoriqueForPlan(id?: number): IHistoriquePlanification[] {
    return id ? (this.historiques()[id] ?? []) : [];
  }

  isLoadingHistorique(id?: number): boolean {
    return id ? !!this._loadingHistorique()[id] : false;
  }

  // ── Groupes ───────────────────────────────────────────────────────────
  loadGroupesDef(): void {
    const plan = this.selectedPlanDef();
    if (!plan) return;
    this.loadingGroupesDef.set(true);
    this.selectedGroupesDef = [];
    const params: Record<string, any> = { size: 500 };
    if (this.filtrePeriodiciteGroupesDef) params['periodiciteDefinitive'] = this.filtrePeriodiciteGroupesDef;
    this.groupeService
      .query(params)
      .pipe(finalize(() => this.loadingGroupesDef.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.groupesDef.set(res.body ?? []),
        error: () => this.notificationService.error('Erreur chargement groupes'),
      });
  }

  loadGroupesProv(): void {
    const plan = this.selectedPlanProv();
    if (!plan) return;
    this.loadingGroupesProv.set(true);
    this.selectedGroupesProv = [];
    const params: Record<string, any> = { size: 500 };
    if (this.filtrePeriodiciteGroupesProv) params['periodiciteProvisoire'] = this.filtrePeriodiciteGroupesProv;
    this.groupeService
      .query(params)
      .pipe(finalize(() => this.loadingGroupesProv.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.groupesProv.set(res.body ?? []),
        error: () => this.notificationService.error('Erreur chargement groupes'),
      });
  }

  // ── TPs ───────────────────────────────────────────────────────────────
  loadTpsDef(): void {
    const plan = this.selectedPlanDef();
    if (!plan) return;
    this.loadingTpsDef.set(true);
    this.selectedTpsDef = [];
    const params: Record<string, any> = { size: 1000 };
    if (this.filtrePeriodiciteTPsDef) params['periodiciteDefinitive'] = this.filtrePeriodiciteTPsDef;
    if (this.filtreCategorieTpsDef) params['type'] = this.filtreCategorieTpsDef;
    this.tpService
      .query(params)
      .pipe(finalize(() => this.loadingTpsDef.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.tpsDef.set(res.body ?? []),
        error: () => this.notificationService.error('Erreur chargement tiers payants'),
      });
  }

  loadTpsProv(): void {
    const plan = this.selectedPlanProv();
    if (!plan) return;
    this.loadingTpsProv.set(true);
    this.selectedTpsProv = [];
    const params: Record<string, any> = { size: 1000 };
    if (this.filtrePeriodiciteTPsProv) params['periodiciteProvisoire'] = this.filtrePeriodiciteTPsProv;
    if (this.filtreCategorieTpsProv) params['type'] = this.filtreCategorieTpsProv;
    this.tpService
      .query(params)
      .pipe(finalize(() => this.loadingTpsProv.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.tpsProv.set(res.body ?? []),
        error: () => this.notificationService.error('Erreur chargement tiers payants'),
      });
  }

  // ── Toggle inclure individuel ─────────────────────────────────────────
  onToggleInclureGroupeDef(groupe: IGroupeTiersPayant): void {
    const updated = { ...groupe, inclureFacturationAutoDefinitive: !groupe.inclureFacturationAutoDefinitive };
    this.groupeService
      .update(updated)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.groupesDef.update(list => list.map(g => (g.id === groupe.id ? updated : g))),
        error: () => this.notificationService.error('Erreur mise à jour'),
      });
  }

  onToggleInclureGroupeProv(groupe: IGroupeTiersPayant): void {
    const updated = { ...groupe, inclureFacturationAutoProvisoire: !groupe.inclureFacturationAutoProvisoire };
    this.groupeService
      .update(updated)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.groupesProv.update(list => list.map(g => (g.id === groupe.id ? updated : g))),
        error: () => this.notificationService.error('Erreur mise à jour'),
      });
  }

  onToggleInclureTpDef(tp: ITiersPayant): void {
    const updated = { ...tp, inclureFacturationAutoDefinitive: !tp.inclureFacturationAutoDefinitive };
    this.tpService
      .update(updated)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.tpsDef.update(list => list.map(t => (t.id === tp.id ? updated : t))),
        error: () => this.notificationService.error('Erreur mise à jour'),
      });
  }

  onToggleInclureTpProv(tp: ITiersPayant): void {
    const updated = { ...tp, inclureFacturationAutoProvisoire: !tp.inclureFacturationAutoProvisoire };
    this.tpService
      .update(updated)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.tpsProv.update(list => list.map(t => (t.id === tp.id ? updated : t))),
        error: () => this.notificationService.error('Erreur mise à jour'),
      });
  }

  // ── Mass actions — Groupes Déf ────────────────────────────────────────
  onMassIncludeGroupesDef(): void {
    const n = this.selectedGroupesDef.length;
    if (!n) return;
    this.confirmDialog.onConfirm(
      () => this.massUpdateGroupes(this.selectedGroupesDef, { inclureAutoDefinitif: true }, () => {
        this.groupesDef.update(list => this.applyMassInclureGroupe(list, this.selectedGroupesDef, true, 'def'));
        this.selectedGroupesDef = [];
      }),
      'Inclure en masse',
      `Inclure ${n} groupe(s) dans la facturation automatique définitive ?`,
    );
  }

  onMassExcludeGroupesDef(): void {
    const n = this.selectedGroupesDef.length;
    if (!n) return;
    this.confirmDialog.onConfirm(
      () => this.massUpdateGroupes(this.selectedGroupesDef, { inclureAutoDefinitif: false }, () => {
        this.groupesDef.update(list => this.applyMassInclureGroupe(list, this.selectedGroupesDef, false, 'def'));
        this.selectedGroupesDef = [];
      }),
      'Exclure en masse',
      `Exclure ${n} groupe(s) de la facturation automatique définitive ?`,
    );
  }

  onMassSetPeriodiciteGroupesDef(): void {
    if (!this.massPeriodiciteGroupesDef) return;
    const n = this.selectedGroupesDef.length;
    const p = this.massPeriodiciteGroupesDef;
    this.confirmDialog.onConfirm(
      () => this.massUpdateGroupes(this.selectedGroupesDef, { periodiciteDefinitive: p }, () => {
        this.selectedGroupesDef = [];
        this.massPeriodiciteGroupesDef = null;
        this.loadGroupesDef();
      }),
      'Modifier la périodicité',
      `Appliquer la périodicité « ${p} » à ${n} groupe(s) ?`,
    );
  }

  // ── Mass actions — Groupes Prov ───────────────────────────────────────
  onMassIncludeGroupesProv(): void {
    const n = this.selectedGroupesProv.length;
    if (!n) return;
    this.confirmDialog.onConfirm(
      () => this.massUpdateGroupes(this.selectedGroupesProv, { inclureAutoProvisoire: true }, () => {
        this.groupesProv.update(list => this.applyMassInclureGroupe(list, this.selectedGroupesProv, true, 'prov'));
        this.selectedGroupesProv = [];
      }),
      'Inclure en masse',
      `Inclure ${n} groupe(s) dans la facturation automatique provisoire ?`,
    );
  }

  onMassExcludeGroupesProv(): void {
    const n = this.selectedGroupesProv.length;
    if (!n) return;
    this.confirmDialog.onConfirm(
      () => this.massUpdateGroupes(this.selectedGroupesProv, { inclureAutoProvisoire: false }, () => {
        this.groupesProv.update(list => this.applyMassInclureGroupe(list, this.selectedGroupesProv, false, 'prov'));
        this.selectedGroupesProv = [];
      }),
      'Exclure en masse',
      `Exclure ${n} groupe(s) de la facturation automatique provisoire ?`,
    );
  }

  onMassSetPeriodiciteGroupesProv(): void {
    if (!this.massPeriodiciteGroupesProv) return;
    const n = this.selectedGroupesProv.length;
    const p = this.massPeriodiciteGroupesProv;
    this.confirmDialog.onConfirm(
      () => this.massUpdateGroupes(this.selectedGroupesProv, { periodiciteProvisoire: p }, () => {
        this.selectedGroupesProv = [];
        this.massPeriodiciteGroupesProv = null;
        this.loadGroupesProv();
      }),
      'Modifier la périodicité',
      `Appliquer la périodicité « ${p} » à ${n} groupe(s) ?`,
    );
  }

  // ── Mass actions — TPs Déf ────────────────────────────────────────────
  onMassIncludeTpsDef(): void {
    const n = this.selectedTpsDef.length;
    if (!n) return;
    this.confirmDialog.onConfirm(
      () => this.massUpdateTps(this.selectedTpsDef, { inclureAutoDefinitif: true }, () => {
        this.tpsDef.update(list => this.applyMassInclureTp(list, this.selectedTpsDef, true, 'def'));
        this.selectedTpsDef = [];
      }),
      'Inclure en masse',
      `Inclure ${n} tiers payant(s) dans la facturation automatique définitive ?`,
    );
  }

  onMassExcludeTpsDef(): void {
    const n = this.selectedTpsDef.length;
    if (!n) return;
    this.confirmDialog.onConfirm(
      () => this.massUpdateTps(this.selectedTpsDef, { inclureAutoDefinitif: false }, () => {
        this.tpsDef.update(list => this.applyMassInclureTp(list, this.selectedTpsDef, false, 'def'));
        this.selectedTpsDef = [];
      }),
      'Exclure en masse',
      `Exclure ${n} tiers payant(s) de la facturation automatique définitive ?`,
    );
  }

  onMassSetPeriodiciteTpsDef(): void {
    if (!this.massPeriodiciteTPsDef) return;
    const n = this.selectedTpsDef.length;
    const p = this.massPeriodiciteTPsDef;
    this.confirmDialog.onConfirm(
      () => this.massUpdateTps(this.selectedTpsDef, { periodiciteDefinitive: p }, () => {
        this.selectedTpsDef = [];
        this.massPeriodiciteTPsDef = null;
        this.loadTpsDef();
      }),
      'Modifier la périodicité',
      `Appliquer la périodicité « ${p} » à ${n} tiers payant(s) ?`,
    );
  }

  // ── Mass actions — TPs Prov ───────────────────────────────────────────
  onMassIncludeTpsProv(): void {
    const n = this.selectedTpsProv.length;
    if (!n) return;
    this.confirmDialog.onConfirm(
      () => this.massUpdateTps(this.selectedTpsProv, { inclureAutoProvisoire: true }, () => {
        this.tpsProv.update(list => this.applyMassInclureTp(list, this.selectedTpsProv, true, 'prov'));
        this.selectedTpsProv = [];
      }),
      'Inclure en masse',
      `Inclure ${n} tiers payant(s) dans la facturation automatique provisoire ?`,
    );
  }

  onMassExcludeTpsProv(): void {
    const n = this.selectedTpsProv.length;
    if (!n) return;
    this.confirmDialog.onConfirm(
      () => this.massUpdateTps(this.selectedTpsProv, { inclureAutoProvisoire: false }, () => {
        this.tpsProv.update(list => this.applyMassInclureTp(list, this.selectedTpsProv, false, 'prov'));
        this.selectedTpsProv = [];
      }),
      'Exclure en masse',
      `Exclure ${n} tiers payant(s) de la facturation automatique provisoire ?`,
    );
  }

  onMassSetPeriodiciteTpsProv(): void {
    if (!this.massPeriodiciteTPsProv) return;
    const n = this.selectedTpsProv.length;
    const p = this.massPeriodiciteTPsProv;
    this.confirmDialog.onConfirm(
      () => this.massUpdateTps(this.selectedTpsProv, { periodiciteProvisoire: p }, () => {
        this.selectedTpsProv = [];
        this.massPeriodiciteTPsProv = null;
        this.loadTpsProv();
      }),
      'Modifier la périodicité',
      `Appliquer la périodicité « ${p} » à ${n} tiers payant(s) ?`,
    );
  }

  // ── Toggle actif / execute / delete ──────────────────────────────────
  onToggleActif(planification: IPlanification): void {
    if (!planification.id) return;
    const action = planification.actif ? 'Désactiver' : 'Activer';
    this.confirmDialog.onConfirm(
      () =>
        this.api
          .toggleActif(planification.id!)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () =>
              this.planifications.update(list =>
                list.map(p => (p.id === planification.id ? { ...p, actif: !p.actif } : p)),
              ),
            error: () => this.notificationService.error("Erreur lors du changement d'état"),
          }),
      `${action} la planification`,
      `${action} la planification « ${planification.libelle} » ?`,
    );
  }

  onExecuterMaintenant(planification: IPlanification): void {
    if (!planification.id) return;
    this.confirmDialog.onConfirm(
      () => this.doExecuter(planification.id!),
      'Exécution manuelle',
      `Exécuter la planification "${planification.libelle}" maintenant ?`,
    );
  }

  onDelete(planification: IPlanification): void {
    if (!planification.id) return;
    this.confirmDialog.onConfirm(
      () => this.doDelete(planification.id!),
      'Suppression',
      `Supprimer la planification "${planification.libelle}" ?`,
    );
  }

  // ── FNE ──────────────────────────────────────────────────────────────
  loadFne(): void {
    this.loadingFne.set(true);
    this.api
      .getFnePlanification()
      .pipe(finalize(() => this.loadingFne.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.planificationFne.set(res.body),
        error: () => this.notificationService.error('Erreur chargement planification FNE'),
      });
  }

  onToggleActifFne(): void {
    const plan = this.planificationFne();
    if (!plan?.id) return;
    const action = plan.actif ? 'Désactiver' : 'Activer';
    this.confirmDialog.onConfirm(
      () =>
        this.api
          .toggleActifFne(plan.id!)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => this.planificationFne.update(p => (p ? { ...p, actif: !p.actif } : p)),
            error: () => this.notificationService.error("Erreur lors du changement d'état FNE"),
          }),
      `${action} la certification FNE`,
      `${action} la planification de certification FNE « ${plan.libelle} » ?`,
    );
  }

  onExecuterFne(): void {
    const plan = this.planificationFne();
    if (!plan?.id) return;
    this.confirmDialog.onConfirm(
      () => {
        this.api
          .executerMaintenantFne(plan.id!)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.notificationService.success('Certification FNE lancée');
              this.loadFne();
            },
            error: () => this.notificationService.error('Erreur lors de la certification FNE'),
          });
      },
      'Certification FNE',
      'Lancer la certification FNE des factures en attente ?',
    );
  }

  onMainTabChange(tab: string): void {
    if (tab === 'fne' && !this.planificationFne()) this.loadFne();
  }

  // ── Helpers ───────────────────────────────────────────────────────────
  getStatutBadgeClass(statut?: string): string {
    switch (statut) {
      case 'SUCCESS':
        return 'badge bg-success';
      case 'ECHEC':
        return 'badge bg-danger';
      case 'EN_COURS':
        return 'badge bg-warning text-dark';
      default:
        return 'badge bg-secondary';
    }
  }

  getPeriodiciteLabel(p?: string): string {
    switch (p) {
      case 'HEBDOMADAIRE':
        return 'Hebdomadaire';
      case 'MENSUEL':
        return 'Mensuel';
      case 'BIMENSUEL':
        return 'Bimensuel';
      case 'QUINZAINE':
        return 'Quinzaine';
      default:
        return p ?? '—';
    }
  }

  // ── Private ───────────────────────────────────────────────────────────
  private doExecuter(id: number): void {
    this.api
      .executerMaintenant(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.notificationService.success('Exécution lancée avec succès');
          this.load();
        },
        error: () => this.notificationService.error("Erreur lors de l'exécution"),
      });
  }

  private doDelete(id: number): void {
    this.api
      .delete(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.planifications.update(list => list.filter(p => p.id !== id));
          this.notificationService.success('Planification supprimée');
        },
        error: () => this.notificationService.error('Erreur lors de la suppression'),
      });
  }

  private massUpdateGroupes(
    selection: IGroupeTiersPayant[],
    config: Record<string, any>,
    onSuccess: () => void,
  ): void {
    const ids = selection.map(g => g.id!).filter(Boolean);
    if (!ids.length) return;
    this.groupeService
      .massUpdateFactureConfig(ids, config)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.notificationService.success(`${ids.length} groupe(s) mis à jour`);
          onSuccess();
        },
        error: () => this.notificationService.error('Erreur lors de la mise à jour en masse'),
      });
  }

  private massUpdateTps(selection: ITiersPayant[], config: Record<string, any>, onSuccess: () => void): void {
    const ids = selection.map(t => t.id!).filter(Boolean);
    if (!ids.length) return;
    this.tpService
      .massUpdateFactureConfig(ids, config)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.notificationService.success(`${ids.length} tiers payant(s) mis à jour`);
          onSuccess();
        },
        error: () => this.notificationService.error('Erreur lors de la mise à jour en masse'),
      });
  }

  private applyMassInclureGroupe(
    list: IGroupeTiersPayant[],
    selection: IGroupeTiersPayant[],
    inclure: boolean,
    type: 'def' | 'prov',
  ): IGroupeTiersPayant[] {
    const ids = new Set(selection.map(g => g.id));
    return list.map(g =>
      ids.has(g.id)
        ? { ...g, ...(type === 'def' ? { inclureFacturationAutoDefinitive: inclure } : { inclureFacturationAutoProvisoire: inclure }) }
        : g,
    );
  }

  private applyMassInclureTp(
    list: ITiersPayant[],
    selection: ITiersPayant[],
    inclure: boolean,
    type: 'def' | 'prov',
  ): ITiersPayant[] {
    const ids = new Set(selection.map(t => t.id));
    return list.map(t =>
      ids.has(t.id)
        ? { ...t, ...(type === 'def' ? { inclureFacturationAutoDefinitive: inclure } : { inclureFacturationAutoProvisoire: inclure }) }
        : t,
    );
  }
}

