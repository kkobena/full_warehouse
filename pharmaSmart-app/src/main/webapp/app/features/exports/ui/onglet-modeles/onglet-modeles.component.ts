import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { ButtonComponent, DataTableComponent } from 'app/shared/ui';
import { ExportsApiService } from '../../data-access/exports-api.service';
import { ExportModele, PERIODES_RELATIVES } from '../../models/exports.model';
import { PanneauModeleComponent } from '../panneau-modele/panneau-modele.component';

const JOURS = ['', 'lundi', 'mardi', 'mercredi', 'jeudi', 'vendredi', 'samedi', 'dimanche'];

/** Modèles d'export : les siens et ceux de l'équipe ; rejouer, modifier, programmer, supprimer. */
@Component({
  selector: 'app-onglet-modeles',
  imports: [DatePipe, DataTableComponent, ButtonComponent, PanneauModeleComponent],
  templateUrl: './onglet-modeles.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OngletModelesComponent {
  /** Modèle préparé depuis le catalogue, à compléter puis enregistrer. */
  readonly aPreparer = input<ExportModele | null>(null);
  /** Un modèle vient d'être lancé : la page bascule sur l'historique. */
  readonly execution = output<void>();

  private readonly api = inject(ExportsApiService);

  protected readonly modeles = rxResource({ stream: () => this.api.listerModeles() });
  protected readonly catalogue = rxResource({ stream: () => this.api.lireCatalogue() });
  private readonly libellesExports = computed(() => new Map((this.catalogue.value()?.exports ?? []).map(export_ => [export_.code, export_.libelle])));

  protected readonly edite = signal<ExportModele | null>(null);
  protected readonly panneauVisible = signal(false);

  constructor() {
    effect(() => {
      const modele = this.aPreparer();
      if (modele) {
        this.ouvrir(modele);
      }
    });
  }

  protected libelleExport(modele: ExportModele): string {
    return this.libellesExports().get(modele.export) ?? modele.export;
  }

  protected libellePeriode(modele: ExportModele): string {
    return PERIODES_RELATIVES.find(periode => periode.valeur === modele.periode)?.libelle ?? 'État du jour';
  }

  protected libelleProgrammation(modele: ExportModele): string {
    const heure = modele.heure?.slice(0, 5) ?? '';
    switch (modele.frequence) {
      case 'DAILY':
        return `Chaque jour à ${heure}`;
      case 'WEEKLY':
        return `Chaque ${JOURS[modele.jour ?? 1]} à ${heure}`;
      case 'MONTHLY':
        return `Le ${modele.jour} de chaque mois à ${heure}`;
      default:
        return '—';
    }
  }

  protected ouvrir(modele: ExportModele | null): void {
    this.edite.set(modele);
    this.panneauVisible.set(true);
  }

  protected enregistrer(modele: ExportModele): void {
    this.api.enregistrerModele(modele).subscribe(() => {
      this.panneauVisible.set(false);
      this.modeles.reload();
    });
  }

  protected executer(modele: ExportModele): void {
    this.api.executerModele(modele.id!).subscribe(() => this.execution.emit());
  }

  protected supprimer(modele: ExportModele): void {
    this.api.supprimerModele(modele.id!).subscribe(() => this.modeles.reload());
  }
}
