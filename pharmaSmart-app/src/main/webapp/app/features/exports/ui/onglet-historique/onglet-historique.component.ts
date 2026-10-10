import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

import { AppBadgeSeverity, BadgeComponent, ButtonComponent, DataTableComponent } from 'app/shared/ui';
import { BlobDownloadService } from 'app/shared/services/blob-download.service';
import { formatNumber } from 'app/shared/utils/format-utils';
import { ExportsApiService } from '../../data-access/exports-api.service';
import { ExportFichier, StatutExport } from '../../models/exports.model';

const TAILLE_PAGE = 20;
/** Tant qu'un fichier se prépare, l'historique se relit à ce rythme. */
const RELECTURE_MS = 3_000;

const STATUTS: Record<StatutExport, { libelle: string; severite: AppBadgeSeverity }> = {
  EN_ATTENTE: { libelle: 'En attente', severite: 'secondary' },
  EN_COURS: { libelle: 'En préparation', severite: 'info' },
  TERMINE: { libelle: 'Prêt', severite: 'success' },
  ECHEC: { libelle: 'Échec', severite: 'danger' },
  EXPIRE: { libelle: 'Expiré', severite: 'secondary' },
};

/** Historique des exports : qui, quand, quoi, combien de lignes ; téléchargement tant que le fichier n'a pas expiré. */
@Component({
  selector: 'app-onglet-historique',
  imports: [DatePipe, DataTableComponent, BadgeComponent, ButtonComponent],
  templateUrl: './onglet-historique.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OngletHistoriqueComponent {
  private readonly api = inject(ExportsApiService);
  private readonly telechargement = inject(BlobDownloadService);

  protected readonly page = signal(0);
  protected readonly historique = rxResource({
    params: () => this.page(),
    stream: ({ params }) =>
      this.api
        .listerHistorique(params, TAILLE_PAGE)
        .pipe(map(reponse => ({ fichiers: reponse.body ?? [], total: Number(reponse.headers.get('X-Total-Count') ?? 0) }))),
  });

  protected readonly fichiers = computed(() => this.historique.value()?.fichiers ?? []);
  protected readonly pages = computed(() => Math.max(1, Math.ceil((this.historique.value()?.total ?? 0) / TAILLE_PAGE)));
  protected readonly enPreparation = computed(() => this.fichiers().some(fichier => fichier.statut === 'EN_ATTENTE' || fichier.statut === 'EN_COURS'));

  constructor() {
    effect(onCleanup => {
      if (this.enPreparation() && !this.historique.isLoading()) {
        const minuterie = setTimeout(() => this.historique.reload(), RELECTURE_MS);
        onCleanup(() => clearTimeout(minuterie));
      }
    });
  }

  protected libelleStatut(statut: StatutExport): string {
    return STATUTS[statut].libelle;
  }

  protected severite(statut: StatutExport): AppBadgeSeverity {
    return STATUTS[statut].severite;
  }

  protected formaterTaille(octets: number | null): string {
    if (octets === null) {
      return '—';
    }
    return octets < 1_048_576 ? `${formatNumber(Math.ceil(octets / 1024))} Ko` : `${(octets / 1_048_576).toFixed(1).replace('.', ',')} Mo`;
  }

  protected telecharger(fichier: ExportFichier): void {
    this.api.telecharger(fichier.id).subscribe(blob => {
      const base = fichier.export.toLowerCase().replaceAll('_', '-') + (fichier.du ? `_${fichier.du}_${fichier.au}` : '');
      this.telechargement.download(blob, base, fichier.format === 'XLSX' ? 'excel' : 'csv');
    });
  }
}
