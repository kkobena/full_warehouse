import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inputBinding,
  input,
  output,
  outputBinding,
  untracked,
  viewChild,
  ViewContainerRef,
} from '@angular/core';
import { AppSplitButtonItem, ButtonComponent, SplitButtonComponent } from 'app/shared/ui';
import { DashboardItem, WidgetAvailability } from '../../models/dashboard.model';
import { PERIOD_OPTIONS } from '../../models/period';
import { findWidgetDefinition } from '../../widgets/widget-registry';
import { widgetLabel } from '../../data-access/store/dashboard.store';

/**
 * Cadre d'une tuile : titre, bouton de retrait en édition, et le composant du widget chargé à la
 * demande. Un widget refusé (droits, licence, clé inconnue) affiche un message à la place de ses
 * données, sans rien demander au serveur.
 */
@Component({
  selector: 'app-widget-tile',
  imports: [ButtonComponent, SplitButtonComponent],
  template: `
    <div class="widget-tile" [class.card]="!frameless()" [class.data-card]="!frameless()" [class.frameless]="frameless()">
      @if ((!frameless() || editMode()) && !inModal()) {
        <div class="widget-tile-header card-header" [class.is-draggable]="editMode()">
          @if (editMode()) {
            <i class="pi pi-arrows-alt widget-tile-drag" aria-hidden="true"></i>
          }
          <h5 class="widget-tile-title card-title">{{ title() }}</h5>
          @if (ownPeriod(); as period) {
            <span class="widget-tile-period" title="Période propre à cette tuile">{{ period }}</span>
          }
          @if (editMode()) {
            <app-split-button
              icon="pi pi-cog"
              size="small"
              severity="secondary"
              [outlined]="true"
              menuAriaLabel="Actions de la tuile"
              [items]="menuItems()"
              (clicked)="openSettings.emit()"
            />
          } @else if (availability() === 'OK' && !frameless()) {
            <app-button
              icon="pi pi-window-maximize"
              [iconOnly]="true"
              [text]="true"
              size="small"
              severity="secondary"
              ariaLabel="Agrandir"
              (clicked)="expand.emit(false)"
            />
          }
        </div>
      }
      <div class="widget-tile-body">
        @switch (availability()) {
          @case ('OK') {
            <ng-container #host />
          }
          @case ('NON_AUTORISE') {
            <div class="widget-tile-state">
              <i class="pi pi-lock" aria-hidden="true"></i>
              <span>Tuile non autorisée pour votre profil</span>
            </div>
          }
          @case ('NON_SOUSCRIT') {
            <div class="widget-tile-state">
              <i class="pi pi-lock" aria-hidden="true"></i>
              <span>Non inclus dans votre abonnement</span>
            </div>
          }
          @default {
            <div class="widget-tile-state">
              <i class="pi pi-question-circle" aria-hidden="true"></i>
              <span>Tuile indisponible{{ editMode() ? ' — retirez-la du tableau de bord' : '' }}</span>
            </div>
          }
        }
      </div>
    </div>
  `,
  styleUrl: './widget-tile.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WidgetTileComponent {
  readonly item = input.required<DashboardItem>();
  readonly editMode = input(false);
  readonly availability = input.required<WidgetAvailability>();
  /** Affichée dans la fenêtre « Agrandir », qui porte déjà le titre : ni en-tête, ni cadre. */
  readonly inModal = input(false);

  readonly remove = output<void>();
  readonly openSettings = output<void>();
  readonly duplicate = output<void>();
  /** `true` : ouvrir en tableau (« Voir les données »), sinon dans la visualisation de la tuile. */
  readonly expand = output<boolean>();
  readonly paramsChange = output<Record<string, string>>();

  protected readonly definition = computed(() => findWidgetDefinition(this.item().widgetKey));
  protected readonly title = computed(() => widgetLabel(this.item()));
  protected readonly frameless = computed(() => this.definition()?.frameless === true && this.availability() === 'OK');
  protected readonly ownPeriod = computed(() => {
    const own = this.item().params?.['periode'];
    return own ? PERIOD_OPTIONS.find(o => o.value === own)?.label : undefined;
  });

  protected readonly menuItems = computed<AppSplitButtonItem[]>(() => {
    const data = this.availability() === 'OK' && this.definition()?.data;
    return [
      { label: 'Dupliquer', icon: 'pi pi-copy', command: () => this.duplicate.emit() },
      ...(data ? [{ label: 'Agrandir', icon: 'pi pi-window-maximize', command: () => this.expand.emit(false) }] : []),
      ...(data && data.visualizations.includes('TABLE')
        ? [{ label: 'Voir les données', icon: 'pi pi-table', command: () => this.expand.emit(true) }]
        : []),
      { label: 'Retirer', icon: 'pi pi-trash', separatorBefore: true, command: () => this.remove.emit() },
    ];
  });

  private readonly host = viewChild('host', { read: ViewContainerRef });

  constructor() {
    // Recrée le widget quand son emplacement apparaît ; item et editMode lui parviennent par liaison
    effect(() => {
      const host = this.host();
      const definition = this.definition();
      if (!host || !definition) {
        return;
      }
      untracked(() => {
        definition.loadComponent().then(type => {
          if (this.host() !== host) {
            return;
          }
          host.clear();
          host.createComponent(type, {
            bindings: [
              inputBinding('item', this.item),
              inputBinding('editMode', this.editMode),
              outputBinding<Record<string, string>>('paramsChange', params => this.paramsChange.emit(params)),
            ],
          });
        });
      });
    });
  }
}
