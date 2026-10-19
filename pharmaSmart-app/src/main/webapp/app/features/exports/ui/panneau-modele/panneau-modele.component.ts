import { ChangeDetectionStrategy, Component, computed, effect, input, model, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonComponent, CheckboxComponent, InputComponent, OffcanvasComponent, PillSelectorComponent, SelectComponent } from 'app/shared/ui';
import {
  CodeExport,
  ExportCatalogue,
  ExportModele,
  FiltreExport,
  FiltresRetenus,
  FORMAT_PAR_DEFAUT,
  FormatExport,
  Frequence,
  listerFormats,
  PERIODES_RELATIVES,
  PeriodeRelative,
  retenirFiltres,
} from '../../models/exports.model';
import { ReglagesExportComponent } from '../reglages-export/reglages-export.component';

const HEURES = Array.from({ length: 17 }, (_, rang) => rang + 6).map(heure => ({ libelle: `${heure} h`, valeur: `${String(heure).padStart(2, '0')}:00:00` }));
const JOURS_SEMAINE = ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi', 'Dimanche'].map((libelle, rang) => ({ libelle, valeur: rang + 1 }));
const JOURS_MOIS = Array.from({ length: 28 }, (_, rang) => ({ libelle: `Le ${rang + 1}`, valeur: rang + 1 }));

/**
 * Crée ou modifie un modèle : l'export, son format, sa période relative (recalculée à chaque exécution), ses réglages, le
 * partage et la programmation. Les postes étant éteints la nuit, un export dû pendant l'arrêt part au démarrage suivant.
 *
 * <p>Une édition s'enregistre pour être rejouée d'un clic, mais ne se programme pas : son service la bâtit avec les droits de
 * qui la demande, et une exécution de nuit n'aurait personne derrière elle.
 */
@Component({
  selector: 'app-panneau-modele',
  imports: [FormsModule, OffcanvasComponent, InputComponent, SelectComponent, ReglagesExportComponent, PillSelectorComponent, CheckboxComponent, ButtonComponent],
  templateUrl: './panneau-modele.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PanneauModeleComponent {
  readonly visible = model.required<boolean>();
  readonly modele = input<ExportModele | null>(null);
  readonly exports = input.required<readonly ExportCatalogue[]>();
  readonly enregistrement = output<ExportModele>();

  protected readonly periodes = PERIODES_RELATIVES;
  protected readonly heures = HEURES;
  protected readonly frequences: { libelle: string; valeur: Frequence | null }[] = [
    { libelle: 'Non programmé', valeur: null },
    { libelle: 'Chaque jour', valeur: 'DAILY' },
    { libelle: 'Chaque semaine', valeur: 'WEEKLY' },
    { libelle: 'Chaque mois', valeur: 'MONTHLY' },
  ];

  protected readonly libelle = signal('');
  protected readonly export = signal<CodeExport | null>(null);
  protected readonly format = signal<FormatExport>(FORMAT_PAR_DEFAUT);
  protected readonly periode = signal<PeriodeRelative | null>('MOIS_PRECEDENT');
  protected readonly partage = signal(false);
  protected readonly frequence = signal<Frequence | null>(null);
  protected readonly heure = signal<string>('07:00:00');
  protected readonly jour = signal<number>(1);
  protected readonly filtres = signal<FiltresRetenus>({});

  private readonly choisi = computed(() => this.exports().find(entree => entree.code === this.export()));
  protected readonly periodique = computed(() => this.choisi()?.periodique ?? false);
  protected readonly programmable = computed(() => this.choisi()?.programmable ?? false);
  protected readonly filtresDuModele = computed(() => this.choisi()?.filtres ?? []);
  protected readonly formats = computed(() => listerFormats(this.choisi()?.formats ?? []));
  protected readonly jours = computed(() => (this.frequence() === 'WEEKLY' ? JOURS_SEMAINE : JOURS_MOIS));
  protected readonly valide = computed(() => !!this.libelle().trim() && !!this.export() && (!this.periodique() || !!this.periode()));

  constructor() {
    effect(() => {
      const modele = this.modele();
      this.libelle.set(modele?.libelle ?? '');
      this.export.set(modele?.export ?? null);
      this.format.set(modele?.format ?? FORMAT_PAR_DEFAUT);
      this.periode.set(modele?.periode ?? 'MOIS_PRECEDENT');
      this.partage.set(modele?.partage ?? false);
      this.frequence.set(modele?.frequence ?? null);
      this.heure.set(modele?.heure ?? '07:00:00');
      this.jour.set(modele?.jour ?? 1);
      this.filtres.set(modele?.filtres ?? {});
    });
  }

  protected choisir<T>(cible: (valeur: T) => void, selection: unknown): void {
    cible(selection as T);
  }

  /** Changer d'export change les réglages proposés et les formats offerts : les anciens n'auraient plus de sens. */
  protected choisirExport(selection: unknown): void {
    this.export.set(selection as CodeExport);
    this.filtres.set({});
    const offerts = this.choisi()?.formats ?? [];
    if (offerts.length && !offerts.includes(this.format())) {
      // Le premier dans l'ordre du sélecteur, pas celui que le serveur a listé en tête.
      this.format.set(this.formats()[0]?.value ?? FORMAT_PAR_DEFAUT);
    }
    if (!this.programmable()) {
      this.frequence.set(null);
    }
  }

  protected regler(reglage: { code: FiltreExport; valeurs: string[] }): void {
    this.filtres.update(filtres => ({ ...filtres, [reglage.code]: reglage.valeurs }));
  }

  protected enregistrer(): void {
    const frequence = this.programmable() ? this.frequence() : null;
    this.enregistrement.emit({
      id: this.modele()?.id ?? null,
      libelle: this.libelle().trim(),
      export: this.export()!,
      format: this.format(),
      periode: this.periodique() ? this.periode() : null,
      partage: this.partage(),
      frequence,
      heure: frequence ? this.heure() : null,
      jour: frequence === 'WEEKLY' || frequence === 'MONTHLY' ? this.jour() : null,
      prochaineExecution: null,
      proprietaire: null,
      modifiable: true,
      filtres: retenirFiltres(this.filtres()),
    });
  }
}
