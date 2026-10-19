import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, output, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';


import { PharmaDatePickerComponent } from 'app/shared/date-picker/pharma-date-picker.component';
import { BlobDownloadService } from 'app/shared/services/blob-download.service';
import { ButtonComponent, CardComponent, PillSelectorComponent } from 'app/shared/ui';
import { NGB_DATE_TO_ISO, TODAY_NGB_DATE } from 'app/shared/util/warehouse-util';
import { ExportsApiService } from '../../data-access/exports-api.service';
import {
  ExportCatalogue,
  ExportModele,
  FiltreExport,
  FiltresRetenus,
  FORMAT_PAR_DEFAUT,
  FormatExport,
  listerFormats,
  TELECHARGEMENT,
  retenirFiltres,
} from '../../models/exports.model';
import { ReglagesExportComponent } from '../reglages-export/reglages-export.component';

const ECHEC = "L'export n'a pas pu être produit.";

/**
 * Catalogue : les exports de données et les éditions ouverts à l'utilisateur, par rubrique. Chaque entrée se règle sur place —
 * période, filtres de l'écran d'où les données viennent, format — puis part en tâche de fond ; il n'y a pas à ouvrir cet écran.
 * Les boutons d'export des écrans d'origine restent : ceci est un chemin de plus.
 */
@Component({
  selector: 'app-onglet-catalogue',
  imports: [FormsModule, CardComponent, ButtonComponent, PillSelectorComponent, ReglagesExportComponent, PharmaDatePickerComponent],
  templateUrl: './onglet-catalogue.component.html',
  styleUrl: './onglet-catalogue.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OngletCatalogueComponent {
  readonly modele = output<ExportModele>();

  private readonly api = inject(ExportsApiService);
  private readonly telechargement = inject(BlobDownloadService);

  protected readonly catalogue = rxResource({ stream: () => this.api.lireCatalogue() });

  protected readonly choisi = signal<ExportCatalogue | null>(null);
  protected readonly du = signal<NgbDateStruct | null>({ ...TODAY_NGB_DATE(), day: 1 });
  protected readonly au = signal<NgbDateStruct | null>(TODAY_NGB_DATE());
  protected readonly format = signal<FormatExport>(FORMAT_PAR_DEFAUT);
  protected readonly filtres = signal<FiltresRetenus>({});
  protected readonly envoi = signal(false);
  protected readonly erreur = signal<string | null>(null);

  /** Toutes les entrées ouvertes à l'utilisateur, dans l'ordre reçu : une seule grille, sans rubriques. */
  protected readonly entrees = computed(() => this.catalogue.value()?.exports ?? []);

  /** Les formats de l'entrée en cours de réglage. */
  protected readonly formats = computed(() => listerFormats(this.choisi()?.formats ?? []));

  /** Nombre de réglages posés, pour le dire sans déplier le détail. */
  protected readonly nombreDeFiltres = computed(() => Object.keys(retenirFiltres(this.filtres())).length);

  protected choisir(entree: ExportCatalogue): void {
    this.erreur.set(null);
    this.filtres.set({});
    this.choisi.set(entree);
    if (!entree.formats.includes(this.format())) {
      // Le premier dans l'ordre du sélecteur, pas celui que le serveur a listé en tête.
      this.format.set(this.formats()[0]?.value ?? FORMAT_PAR_DEFAUT);
    }
  }

  protected choisirFormat(selection: unknown): void {
    this.format.set(selection as FormatExport);
  }

  protected regler(reglage: { code: FiltreExport; valeurs: string[] }): void {
    this.filtres.update(filtres => ({ ...filtres, [reglage.code]: reglage.valeurs }));
  }

  protected viderFiltres(): void {
    this.filtres.set({});
  }

  /** Le fichier part tout de suite : plus d'attente, plus d'historique où aller le rechercher. */
  protected exporter(entree: ExportCatalogue): void {
    this.envoi.set(true);
    this.erreur.set(null);
    this.api
      .exporter({
        export: entree.code,
        format: this.format(),
        du: entree.periodique ? NGB_DATE_TO_ISO(this.du()) : null,
        au: entree.periodique ? NGB_DATE_TO_ISO(this.au()) : null,
        filtres: retenirFiltres(this.filtres()),
      })
      .subscribe({
        next: fichier => {
          this.envoi.set(false);
          this.telechargement.download(fichier, nommer(entree, this.du(), this.au()), TELECHARGEMENT[this.format()]);
        },
        error: (erreur: unknown) => {
          this.envoi.set(false);
          void lireMessage(erreur).then(message => this.erreur.set(message));
        },
      });
  }

  protected enregistrerCommeModele(entree: ExportCatalogue): void {
    this.modele.emit({
      id: null,
      libelle: entree.libelle,
      export: entree.code,
      format: this.format(),
      periode: entree.periodique ? 'MOIS_PRECEDENT' : null,
      partage: false,
      frequence: null,
      heure: null,
      jour: null,
      prochaineExecution: null,
      proprietaire: null,
      modifiable: true,
      filtres: retenirFiltres(this.filtres()),
    });
  }

}

/** « lignes-vente_2026-10-01_2026-10-31 » ; le service du téléchargement ajoute l'extension. */
function nommer(entree: ExportCatalogue, du: NgbDateStruct | null, au: NgbDateStruct | null): string {
  const base = entree.code.toLowerCase().replace(/_/g, '-');
  if (!entree.periodique) {
    return base;
  }
  return `${base}_${NGB_DATE_TO_ISO(du)}_${NGB_DATE_TO_ISO(au)}`;
}

/**
 * Le fichier arrivant en `blob`, le corps d'une erreur en est un aussi : il faut le lire pour
 * retrouver le message du serveur — « ne se produit pas en PDF », « choisissez une période valide ».
 */
async function lireMessage(erreur: unknown): Promise<string> {
  if (erreur instanceof HttpErrorResponse && erreur.error instanceof Blob) {
    try {
      const corps = JSON.parse(await erreur.error.text()) as { detail?: string; message?: string };
      return corps.detail ?? corps.message ?? ECHEC;
    } catch {
      return ECHEC;
    }
  }
  return ECHEC;
}
