import { ChangeDetectionStrategy, Component, computed, effect, input, model, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonComponent, CheckboxComponent, InputComponent, OffcanvasComponent, PillSelectorComponent, SelectComponent } from 'app/shared/ui';
import { ExportCatalogue, ExportDonnees, ExportModele, FORMATS, FormatExport, Frequence, PERIODES_RELATIVES, PeriodeRelative } from '../../models/exports.model';

const HEURES = Array.from({ length: 17 }, (_, rang) => rang + 6).map(heure => ({ libelle: `${heure} h`, valeur: `${String(heure).padStart(2, '0')}:00:00` }));
const JOURS_SEMAINE = ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi', 'Dimanche'].map((libelle, rang) => ({ libelle, valeur: rang + 1 }));
const JOURS_MOIS = Array.from({ length: 28 }, (_, rang) => ({ libelle: `Le ${rang + 1}`, valeur: rang + 1 }));

/**
 * Crée ou modifie un modèle : l'export, son format, sa période relative (recalculée à chaque exécution), le partage et la
 * programmation. Les postes étant éteints la nuit, un export dû pendant l'arrêt part au démarrage suivant.
 */
@Component({
  selector: 'app-panneau-modele',
  imports: [FormsModule, OffcanvasComponent, InputComponent, SelectComponent, PillSelectorComponent, CheckboxComponent, ButtonComponent],
  templateUrl: './panneau-modele.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PanneauModeleComponent {
  readonly visible = model.required<boolean>();
  readonly modele = input<ExportModele | null>(null);
  readonly exports = input.required<readonly ExportCatalogue[]>();
  readonly enregistrement = output<ExportModele>();

  protected readonly formats = FORMATS;
  protected readonly periodes = PERIODES_RELATIVES;
  protected readonly heures = HEURES;
  protected readonly frequences: { libelle: string; valeur: Frequence | null }[] = [
    { libelle: 'Non programmé', valeur: null },
    { libelle: 'Chaque jour', valeur: 'DAILY' },
    { libelle: 'Chaque semaine', valeur: 'WEEKLY' },
    { libelle: 'Chaque mois', valeur: 'MONTHLY' },
  ];

  protected readonly libelle = signal('');
  protected readonly export = signal<ExportDonnees | null>(null);
  protected readonly format = signal<FormatExport>('XLSX');
  protected readonly periode = signal<PeriodeRelative | null>('MOIS_PRECEDENT');
  protected readonly partage = signal(false);
  protected readonly frequence = signal<Frequence | null>(null);
  protected readonly heure = signal<string>('07:00:00');
  protected readonly jour = signal<number>(1);

  protected readonly periodique = computed(() => this.exports().find(export_ => export_.code === this.export())?.periodique ?? false);
  protected readonly jours = computed(() => (this.frequence() === 'WEEKLY' ? JOURS_SEMAINE : JOURS_MOIS));
  protected readonly valide = computed(() => !!this.libelle().trim() && !!this.export() && (!this.periodique() || !!this.periode()));

  constructor() {
    effect(() => {
      const modele = this.modele();
      this.libelle.set(modele?.libelle ?? '');
      this.export.set(modele?.export ?? null);
      this.format.set(modele?.format ?? 'XLSX');
      this.periode.set(modele?.periode ?? 'MOIS_PRECEDENT');
      this.partage.set(modele?.partage ?? false);
      this.frequence.set(modele?.frequence ?? null);
      this.heure.set(modele?.heure ?? '07:00:00');
      this.jour.set(modele?.jour ?? 1);
    });
  }

  protected choisir<T>(cible: (valeur: T) => void, selection: unknown): void {
    cible(selection as T);
  }

  protected enregistrer(): void {
    const frequence = this.frequence();
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
    });
  }
}
