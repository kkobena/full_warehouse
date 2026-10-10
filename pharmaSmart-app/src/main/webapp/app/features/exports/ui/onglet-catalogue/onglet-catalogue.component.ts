import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, output, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';

import { PharmaDatePickerComponent } from 'app/shared/date-picker/pharma-date-picker.component';
import { ButtonComponent, CardComponent, PillSelectorComponent } from 'app/shared/ui';
import { NGB_DATE_TO_ISO, TODAY_NGB_DATE } from 'app/shared/util/warehouse-util';
import { ExportsApiService } from '../../data-access/exports-api.service';
import { ExportCatalogue, ExportModele, FORMATS, FormatExport, LienExport } from '../../models/exports.model';

/**
 * Catalogue : les exports de données ouverts à l'utilisateur, par rubrique, puis les exports existants rattachés (comptabilité,
 * déclarations, pilotage), qui restent dans leur écran.
 */
@Component({
  selector: 'app-onglet-catalogue',
  imports: [FormsModule, CardComponent, ButtonComponent, PillSelectorComponent, PharmaDatePickerComponent],
  templateUrl: './onglet-catalogue.component.html',
  styleUrl: './onglet-catalogue.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OngletCatalogueComponent {
  /** Un export vient d'être demandé : la page bascule sur l'historique, qui suit sa génération. */
  readonly demande = output<void>();
  readonly modele = output<ExportModele>();

  private readonly api = inject(ExportsApiService);
  private readonly router = inject(Router);

  protected readonly catalogue = rxResource({ stream: () => this.api.lireCatalogue() });
  protected readonly formats = FORMATS;

  protected readonly choisi = signal<ExportCatalogue | null>(null);
  protected readonly du = signal<NgbDateStruct | null>({ ...TODAY_NGB_DATE(), day: 1 });
  protected readonly au = signal<NgbDateStruct | null>(TODAY_NGB_DATE());
  protected readonly format = signal<FormatExport>('XLSX');
  protected readonly envoi = signal(false);
  protected readonly erreur = signal<string | null>(null);

  /** Exports de données, par rubrique, dans l'ordre du catalogue. */
  protected readonly rubriques = computed(() => regrouper(this.catalogue.value()?.exports ?? [], export_ => export_.libelleRubrique));
  protected readonly liens = computed(() => regrouper(this.catalogue.value()?.liens ?? [], lien => lien.rubrique));

  protected choisir(export_: ExportCatalogue): void {
    this.erreur.set(null);
    this.choisi.set(export_);
  }

  protected choisirFormat(selection: unknown): void {
    this.format.set(selection as FormatExport);
  }

  protected exporter(export_: ExportCatalogue): void {
    this.envoi.set(true);
    this.erreur.set(null);
    this.api
      .demander({
        export: export_.code,
        format: this.format(),
        du: export_.periodique ? NGB_DATE_TO_ISO(this.du()) : null,
        au: export_.periodique ? NGB_DATE_TO_ISO(this.au()) : null,
      })
      .subscribe({
        next: () => {
          this.envoi.set(false);
          this.demande.emit();
        },
        error: (erreur: unknown) => {
          this.envoi.set(false);
          this.erreur.set((erreur instanceof HttpErrorResponse ? (erreur.error?.detail ?? erreur.error?.message) : null) ?? "L'export n'a pas pu être lancé.");
        },
      });
  }

  protected enregistrerCommeModele(export_: ExportCatalogue): void {
    this.modele.emit({
      id: null,
      libelle: export_.libelle,
      export: export_.code,
      format: this.format(),
      periode: export_.periodique ? 'MOIS_PRECEDENT' : null,
      partage: false,
      frequence: null,
      heure: null,
      jour: null,
      prochaineExecution: null,
      proprietaire: null,
      modifiable: true,
    });
  }

  protected ouvrir(lien: LienExport): void {
    void this.router.navigate([lien.route], { queryParams: lien.onglet ? { onglet: lien.onglet } : {} });
  }
}

function regrouper<T>(elements: readonly T[], cle: (element: T) => string): { libelle: string; elements: T[] }[] {
  const groupes = new Map<string, T[]>();
  for (const element of elements) {
    const groupe = groupes.get(cle(element)) ?? [];
    groupe.push(element);
    groupes.set(cle(element), groupe);
  }
  return Array.from(groupes, ([libelle, membres]) => ({ libelle, elements: membres }));
}
