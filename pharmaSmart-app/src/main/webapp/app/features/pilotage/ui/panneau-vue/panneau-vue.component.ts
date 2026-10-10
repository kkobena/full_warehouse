import { ChangeDetectionStrategy, Component, computed, effect, input, model, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonComponent, CheckboxComponent, FormFieldComponent, InputComponent, OffcanvasComponent } from 'app/shared/ui';
import { AffichageAnalyse, ReglageAnalyse, TriAnalyse, VuePilotage } from '../../models/pilotage.model';

const AFFICHAGES: Record<AffichageAnalyse, string> = { BARRES: 'Barres', COURBE: 'Courbe', TABLEAU: 'Tableau', CROISE: 'Croisé' };
const TRIS: Record<TriAnalyse, string> = { VALEUR: 'Valeur', ECART_HAUSSE: 'Plus fortes hausses', ECART_BAISSE: 'Plus fortes baisses' };

/**
 * Enregistre le réglage courant en vue (la période reste celle de la barre). Ouverte sur une vue de l'utilisateur, la modifie ;
 * sinon, en crée une. Le panneau résume ce qui sera gardé.
 */
@Component({
  selector: 'app-panneau-vue',
  imports: [FormsModule, OffcanvasComponent, FormFieldComponent, InputComponent, CheckboxComponent, ButtonComponent],
  template: `
    <app-offcanvas (visibleChange)="visible.set($event)" [visible]="visible()" width="440px">
      <div appOffcanvasHeader class="panneau-vue-entete">
        <i aria-hidden="true" class="pi pi-bookmark"></i>
        <div>
          <h2 class="panneau-vue-titre">{{ vueModifiee() ? 'Modifier la vue' : 'Enregistrer la vue' }}</h2>
          <p class="panneau-vue-sous-titre">Retrouvez ce réglage dans « Vues enregistrées ».</p>
        </div>
      </div>

      <form (ngSubmit)="enregistrer()" class="panneau-vue-formulaire">
        <section aria-labelledby="panneau-vue-resume" class="panneau-vue-resume">
          <h3 class="panneau-vue-resume-titre" id="panneau-vue-resume">Ce qui sera gardé</h3>
          <dl>
            <dt>Indicateurs</dt>
            <dd>{{ libellesIndicateurs().join(', ') }}</dd>
            <dt>Ventilation</dt>
            <dd>{{ libellesAxes().join(' puis ') }}</dd>
            <dt>Éléments</dt>
            <dd>{{ reglage().top ? 'Top ' + reglage().top : 'Tous' }}, triés par {{ tri().toLowerCase() }}</dd>
            <dt>Affichage</dt>
            <dd>{{ affichage() }}</dd>
          </dl>
          <p class="panneau-vue-note">
            <i aria-hidden="true" class="pi pi-info-circle"></i>
            La période et la comparaison ne sont pas gardées : la vue s'applique à celles de la barre du haut.
          </p>
        </section>

        <app-form-field [required]="true" label="Nom de la vue">
          <app-input (ngModelChange)="libelle.set($event)" [maxlength]="100" [ngModel]="libelle()" [required]="true" ariaLabel="Nom de la vue" name="libelle"
                     placeholder="Ex. : CA par famille, plus fortes baisses" />
        </app-form-field>
        <app-checkbox (ngModelChange)="partagee.set($event)" [ngModel]="partagee()" label="Partager avec l'équipe" name="partagee" />
        <p class="panneau-vue-note panneau-vue-note--champ">Une vue partagée apparaît chez vos collègues dans « Vues de l'équipe » ; eux ne peuvent pas la modifier.</p>

        <div class="panneau-vue-actions">
          <app-button (clicked)="visible.set(false)" [outlined]="true" label="Annuler" severity="secondary" />
          <app-button [disabled]="!libelle().trim()" icon="pi pi-check" [label]="vueModifiee() ? 'Mettre à jour' : 'Enregistrer'" type="submit" />
        </div>
      </form>
    </app-offcanvas>
  `,
  styles: `
    .panneau-vue-entete {
      display: flex;
      align-items: flex-start;
      gap: 0.6rem;

      > .pi {
        margin-top: 0.3rem;
        color: var(--pharma-chrome-tab);
        font-size: 1.1rem;
      }
    }

    .panneau-vue-titre {
      margin: 0;
      font-size: 1.15rem;
      font-weight: 700;
    }

    .panneau-vue-sous-titre {
      margin: 0;
      font-size: 0.8rem;
      color: var(--pharma-text-muted);
    }

    .panneau-vue-formulaire {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .panneau-vue-resume {
      padding: 0.75rem 0.85rem;
      border: 1px solid var(--pharma-surface-border);
      border-radius: var(--pharma-surface-radius);
      background: var(--pharma-surface-alt);

      dl {
        display: grid;
        grid-template-columns: max-content 1fr;
        gap: 0.25rem 0.75rem;
        margin: 0 0 0.5rem;
        font-size: 0.85rem;
      }

      dt {
        font-weight: 600;
        color: var(--pharma-text-muted);
      }

      dd {
        margin: 0;
        color: var(--pharma-text);
      }
    }

    .panneau-vue-resume-titre {
      margin: 0 0 0.5rem;
      font-size: 0.8rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.03em;
      color: var(--pharma-chrome-tab);
    }

    .panneau-vue-note {
      line-height: 1.4;
      display: flex;
      gap: 0.35rem;
      margin: 0;
      font-size: 0.8rem;
      color: var(--pharma-text-muted);

      .pi {
        margin-top: 0.15rem;
      }

      &--champ {
        margin-top: -0.5rem;
        padding-left: 1.6rem;
      }
    }

    .panneau-vue-actions {
      display: flex;
      justify-content: flex-end;
      gap: 0.5rem;
      padding-top: 0.75rem;
      border-top: 1px solid var(--pharma-border);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PanneauVueComponent {
  readonly visible = model.required<boolean>();
  readonly reglage = input.required<ReglageAnalyse>();
  /** Vue de l'utilisateur en cours de modification ; `null` : nouvelle vue. */
  readonly vueModifiee = input<VuePilotage | null>(null);
  readonly libellesIndicateurs = input<readonly string[]>([]);
  readonly libellesAxes = input<readonly string[]>([]);

  readonly enregistrement = output<VuePilotage>();

  protected readonly libelle = signal('');
  protected readonly partagee = signal(false);
  protected readonly affichage = computed(() => AFFICHAGES[this.reglage().affichage] ?? this.reglage().affichage);
  protected readonly tri = computed(() => TRIS[this.reglage().tri] ?? this.reglage().tri);

  constructor() {
    effect(() => {
      const vue = this.vueModifiee();
      this.libelle.set(vue?.libelle ?? '');
      this.partagee.set(vue?.partagee ?? false);
    });
  }

  protected enregistrer(): void {
    const reglage = this.reglage();
    this.enregistrement.emit({
      id: this.vueModifiee()?.id ?? null,
      libelle: this.libelle().trim(),
      indicateurs: reglage.indicateurs,
      axe: reglage.axe,
      axe2: reglage.axe2,
      top: reglage.top,
      tri: reglage.tri,
      affichage: reglage.affichage,
      livree: false,
      partagee: this.partagee(),
      modifiable: true,
    });
  }
}
