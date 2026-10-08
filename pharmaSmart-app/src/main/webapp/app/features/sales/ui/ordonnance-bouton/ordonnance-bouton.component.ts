import {ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked} from '@angular/core';
import {NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {BadgeComponent, ButtonComponent} from 'app/shared/ui';
import {OrdonnanceApiService} from '../../data-access/services/ordonnance-api.service';
import {OrdonnancePanelService} from '../../data-access/services/ordonnance-panel.service';

/**
 * Bouton « Ordonnances du client » avec, dès que le client est choisi, le nombre d'ordonnances en cours :
 * le préparateur sait qu'il y a une reprise ou un reste à délivrer sans ouvrir le panneau, et rien ne s'impose à lui.
 */
@Component({
  selector: 'app-ordonnance-bouton',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonComponent, BadgeComponent, NgbTooltip],
  template: `
    <span class="position-relative d-inline-block">
      <app-button (clicked)="ouvrir()" [ariaLabel]="libelle()" [ngbTooltip]="libelle()" icon="pi pi-file-edit" severity="primary"
                  size="small" [rounded]="true" [text]="true" />
      @if (nombre() > 0) {
        <app-badge [label]="'' + nombre()" severity="success" [rounded]="true" class="position-absolute top-0 end-0" style="transform: scale(0.8); transform-origin: top right; pointer-events: none" />
      }
    </span>
  `,
})
export class OrdonnanceBoutonComponent {
  readonly customerId = input<number | null | undefined>(null);

  protected readonly nombre = signal(0);
  protected readonly libelle = computed(() =>
    this.nombre() > 0 ? `Ordonnances du client (${this.nombre()} en cours)` : 'Ordonnances du client',
  );

  private readonly api = inject(OrdonnanceApiService);
  private readonly panneau = inject(OrdonnancePanelService);

  constructor() {
    // Recompté au choix du client, et après chaque saisie, rattachement ou clôture faits dans le panneau.
    effect(() => {
      const id = this.customerId();
      this.panneau.changements();
      untracked(() => {
        if (!id) {
          this.nombre.set(0);
          return;
        }
        // Un comptage indisponible ne doit rien afficher : le bouton reste utilisable.
        this.api.duClient(id, 'EN_COURS').subscribe({
          next: ordonnances => this.nombre.set(ordonnances.length),
          error: () => this.nombre.set(0),
        });
      });
    });
  }

  protected ouvrir(): void {
    this.panneau.ouvrir(this.customerId());
  }
}
