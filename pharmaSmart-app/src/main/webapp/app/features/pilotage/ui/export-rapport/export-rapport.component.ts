import { ChangeDetectionStrategy, Component, computed, inject, input, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { AbilityService } from 'app/core/auth/ability.service';
import { BlobDownloadService } from 'app/shared/services/blob-download.service';
import { ButtonComponent, CheckboxComponent, ModalComponent } from 'app/shared/ui';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { RequetePilotage } from '../../models/pilotage.model';
import { lireReglage } from '../../feature/onglet-analyser/reglage-analyse';
import { lireReglageAnnees } from '../../feature/onglet-comparer-annees/reglage-annees';
import { ONGLETS_PILOTAGE, ONGLETS_RAPPORT_PDF } from '../../pilotage-onglets';

const STORAGE_KEY = 'pilotage-export-selection';

function lireDerniereSelection(): ReadonlySet<string> {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      const ids = JSON.parse(saved) as unknown;
      return new Set(Array.isArray(ids) ? (ids as string[]) : []);
    }
  } catch {}
  return new Set<string>();
}

function sauvegarderSelection(selection: ReadonlySet<string>): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify([...selection]));
  } catch {}
}

/** Bouton « Exporter en PDF » du pilotage : choix des onglets, puis un seul rapport. Mémorise la dernière sélection. */
@Component({
  selector: 'app-export-rapport',
  imports: [FormsModule, ButtonComponent, CheckboxComponent, ModalComponent],
  template: `
    @if (ongletsExportables().length) {
      <app-button (clicked)="ouvrir()" [outlined]="true" icon="pi pi-file-pdf" label="Exporter en PDF" size="small" />
    }
    <app-modal #fenetre [hasFooter]="true" header="Exporter le pilotage en PDF" icon="pi pi-file-pdf" size="lg">
      <div class="d-flex align-items-center justify-content-between border-bottom pb-2 mb-3">
        <span class="text-muted small">Onglets à inclure sur la période de la barre :</span>
        <app-checkbox (ngModelChange)="basculerTout($event)" [ngModel]="toutCoche()" label="Tout sélectionner" />
      </div>
      <div class="onglets-liste">
        @for (onglet of ongletsExportables(); track onglet.id) {
          <app-checkbox (ngModelChange)="basculer(onglet.id, $event)" [label]="onglet.libelle" [ngModel]="selection().has(onglet.id)" />
        }
      </div>
      <ng-container ngProjectAs="[footer]">
        <app-button (clicked)="fenetre.dismiss()" [text]="true" label="Annuler" severity="secondary" />
        <app-button (clicked)="imprimer()" [disabled]="!selection().size" [loading]="enCours()" icon="pi pi-download" label="Exporter" />
      </ng-container>
    </app-modal>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExportRapportComponent {
  readonly requete = input.required<RequetePilotage>();
  /** Onglet affiché : coché par défaut si aucune sélection mémorisée. */
  readonly ongletActif = input<string | undefined>();

  private readonly api = inject(PilotageApiService);
  private readonly ability = inject(AbilityService);
  private readonly route = inject(ActivatedRoute);
  private readonly telechargement = inject(BlobDownloadService);
  private readonly fenetre = viewChild.required<ModalComponent>('fenetre');

  protected readonly ongletsExportables = computed(() =>
    ONGLETS_PILOTAGE.filter(onglet => ONGLETS_RAPPORT_PDF[onglet.id] && this.ability.can('export', onglet.code)),
  );
  protected readonly selection = signal<ReadonlySet<string>>(new Set());
  protected readonly enCours = signal(false);

  protected readonly toutCoche = computed(() => {
    const exp = this.ongletsExportables();
    return exp.length > 0 && exp.every(o => this.selection().has(o.id));
  });

  protected ouvrir(): void {
    const exportables = this.ongletsExportables().map(o => o.id);
    const derniere = [...lireDerniereSelection()].filter(id => exportables.includes(id));
    if (derniere.length > 0) {
      this.selection.set(new Set(derniere));
    } else {
      const actif = this.ongletActif();
      this.selection.set(new Set(actif && exportables.includes(actif) ? [actif] : exportables));
    }
    this.fenetre().open().catch((): void => undefined);
  }

  protected basculer(id: string, coche: boolean): void {
    const s = new Set(this.selection());
    if (coche) {
      s.add(id);
    } else {
      s.delete(id);
    }
    this.selection.set(s);
  }

  protected basculerTout(coche: boolean): void {
    this.selection.set(coche ? new Set(this.ongletsExportables().map(o => o.id)) : new Set());
  }

  /** Dans l'ordre des onglets de la page, quel que soit l'ordre des clics. */
  protected imprimer(): void {
    const requete = this.requete();
    const parametres = this.route.snapshot.queryParamMap;
    const onglets = this.ongletsExportables()
      .filter(onglet => this.selection().has(onglet.id))
      .map(onglet => ONGLETS_RAPPORT_PDF[onglet.id]);
    sauvegarderSelection(this.selection());
    this.telechargement.downloadFromObservable(
      this.api.imprimerRapport(requete, onglets, lireReglageAnnees(parametres), lireReglage(parametres)),
      `pilotage-${requete.du}-${requete.au}`,
      'pdf',
      () => this.enCours.set(true),
      () => {
        this.enCours.set(false);
        this.fenetre().close();
      },
    );
  }
}
