import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent, InputComponent, InputNumberComponent, SelectComponent } from 'app/shared/ui';
import { DashboardItem, KpiAlertDirection, VIZ_LABELS, VizType, WidgetDefinition } from '../../models/dashboard.model';
import { PERIOD_OPTIONS } from '../../models/period';

export interface WidgetSettingsResult {
  title: string | undefined;
  params: Record<string, string>;
}

const OWN_PERIOD = [{ value: '', label: 'Période du tableau de bord' }, ...PERIOD_OPTIONS];
const ALERT_DIRECTIONS: { value: KpiAlertDirection; label: string }[] = [
  { value: 'DESSOUS', label: 'Alerter sous le seuil' },
  { value: 'DESSUS', label: 'Alerter au-dessus du seuil' },
];

/** Réglages d'une tuile : titre, visualisation, période propre, paramètres du widget, seuil d'alerte. */
@Component({
  selector: 'app-widget-settings-modal',
  imports: [FormsModule, ButtonComponent, InputComponent, InputNumberComponent, SelectComponent],
  template: `
    <div class="modal-header">
      <h2 class="modal-title fs-5"><i class="pi pi-cog me-2" aria-hidden="true"></i>Réglages — {{ definition().label }}</h2>
      <button type="button" class="btn-close" aria-label="Fermer" (click)="activeModal.dismiss()"></button>
    </div>

    <div class="modal-body d-flex flex-column gap-3">
      <div>
        <div class="form-label">Titre</div>
        <app-input ariaLabel="Titre de la tuile" [placeholder]="definition().label" [ngModel]="title()" (ngModelChange)="title.set($event ?? '')" />
      </div>

      @if (vizOptions().length > 1) {
        <div>
          <div class="form-label">Visualisation</div>
          <app-select
            ariaLabel="Visualisation"
            [items]="vizOptions()"
            bindLabel="label"
            bindValue="value"
            [searchable]="false"
            [ngModel]="viz()"
            (ngModelChange)="$event && viz.set($event)"
          />
        </div>
      }

      @if (spec()?.usesPeriod) {
        <div>
          <div class="form-label">Période</div>
          <app-select
            ariaLabel="Période de la tuile"
            [items]="periodOptions"
            bindLabel="label"
            bindValue="value"
            [searchable]="false"
            [ngModel]="periode()"
            (ngModelChange)="periode.set($event ?? '')"
          />
          <div class="form-text">Une période propre remplace celle du tableau de bord pour cette seule tuile.</div>
        </div>
      }

      @for (param of spec()?.params ?? []; track param.name) {
        <div>
          <div class="form-label">{{ param.label }}</div>
          @if (param.type === 'number') {
            <app-input-number
              [ariaLabel]="param.label"
              [min]="param.min"
              [max]="param.max"
              [showButtons]="true"
              [ngModel]="numberValue(param.name, param.defaultValue)"
              (ngModelChange)="setValue(param.name, $event)"
            />
          } @else {
            <app-select
              [ariaLabel]="param.label"
              [items]="param.options ?? []"
              bindLabel="label"
              bindValue="value"
              [searchable]="false"
              [ngModel]="values()[param.name] ?? param.defaultValue"
              (ngModelChange)="setValue(param.name, $event)"
            />
          }
        </div>
      }

      @if (viz() === 'KPI') {
        <div>
          <div class="form-label">Seuil d'alerte</div>
          <div class="d-flex gap-2">
            <app-input-number
              class="flex-fill"
              ariaLabel="Seuil d'alerte"
              placeholder="Aucun"
              [ngModel]="seuil()"
              (ngModelChange)="seuil.set($event)"
            />
            <app-select
              class="flex-fill"
              ariaLabel="Sens de l'alerte"
              [items]="alertDirections"
              bindLabel="label"
              bindValue="value"
              [searchable]="false"
              [ngModel]="alerte()"
              (ngModelChange)="$event && alerte.set($event)"
            />
          </div>
          <div class="form-text">Laissez vide pour ne pas alerter. Une valeur en alerte est signalée par une icône et un libellé.</div>
        </div>
      }
    </div>

    <div class="modal-footer">
      <app-button label="Annuler" severity="secondary" [text]="true" (clicked)="activeModal.dismiss()" />
      <app-button label="Appliquer" icon="pi pi-check" (clicked)="apply()" />
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WidgetSettingsModalComponent {
  readonly activeModal = inject(NgbActiveModal);

  protected readonly definition = signal<WidgetDefinition>({} as WidgetDefinition);
  protected readonly spec = computed(() => this.definition().data);
  protected readonly title = signal('');
  protected readonly viz = signal<VizType | undefined>(undefined);
  protected readonly periode = signal('');
  protected readonly seuil = signal<number | null>(null);
  protected readonly alerte = signal<KpiAlertDirection>('DESSOUS');
  /** Paramètres du widget (limite, tri…) et paramètres inconnus, conservés tels quels. */
  protected readonly values = signal<Record<string, string>>({});

  protected readonly periodOptions = OWN_PERIOD;
  protected readonly alertDirections = ALERT_DIRECTIONS;
  protected readonly vizOptions = computed(() => (this.spec()?.visualizations ?? []).map(value => ({ value, label: VIZ_LABELS[value] })));

  /** Appelé par l'ouvreur avant l'affichage. */
  init(item: DashboardItem, definition: WidgetDefinition): void {
    const params = { ...(item.params ?? {}) };
    this.definition.set(definition);
    this.title.set(item.title ?? '');
    const viz = params['viz'] as VizType | undefined;
    this.viz.set(viz && definition.data?.visualizations.includes(viz) ? viz : definition.data?.defaultViz);
    this.periode.set(params['periode'] ?? '');
    this.seuil.set(params['seuil'] != null && params['seuil'] !== '' ? Number(params['seuil']) : null);
    this.alerte.set(params['alerte'] === 'DESSUS' ? 'DESSUS' : 'DESSOUS');
    delete params['viz'];
    delete params['periode'];
    delete params['seuil'];
    delete params['alerte'];
    this.values.set(params);
  }

  protected numberValue(name: string, defaultValue: string): number {
    return Number(this.values()[name] ?? defaultValue);
  }

  protected setValue(name: string, value: string | number | null): void {
    this.values.update(v => ({ ...v, [name]: value == null ? '' : String(value) }));
  }

  protected apply(): void {
    const params: Record<string, string> = {};
    for (const [name, value] of Object.entries(this.values())) {
      if (value !== '') {
        params[name] = value;
      }
    }
    const viz = this.viz();
    if (viz && viz !== this.spec()?.defaultViz) {
      params['viz'] = viz;
    }
    if (this.periode()) {
      params['periode'] = this.periode();
    }
    if (viz === 'KPI' && this.seuil() != null) {
      params['seuil'] = String(this.seuil());
      params['alerte'] = this.alerte();
    }
    this.activeModal.close({ title: this.title(), params } satisfies WidgetSettingsResult);
  }
}
