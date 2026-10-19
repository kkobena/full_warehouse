import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MultiSelectComponent, SelectComponent } from 'app/shared/ui';
import { FiltreCatalogue, FiltreExport, FiltresRetenus } from '../../models/exports.model';

/**
 * « Aucune valeur retenue », en une seule instance partagée, et ce n'est pas une micro-optimisation.
 *
 * `[ngModel]` compare le modèle reçu par identité : une liste vide fraîche à chaque appel lui paraît
 * être une nouvelle valeur, il réécrit donc le champ, dont le signal interne notifie un nouveau
 * cycle de détection, qui rappelle la méthode, qui rend une autre liste vide… L'onglet se figeait,
 * et avec lui l'application entière.
 */
const AUCUNE_VALEUR: string[] = [];

/**
 * Les réglages qu'une entrée du catalogue annonce — filtres des écrans de données, découpage et indicateurs des éditions.
 * Chacun se présente comme à l'écran d'origine : une liste à choix multiple, ou un choix unique quand le service n'en accepte
 * qu'un. Sans choix, l'export porte sur tout.
 */
@Component({
  selector: 'app-reglages-export',
  imports: [FormsModule, MultiSelectComponent, SelectComponent],
  templateUrl: './reglages-export.component.html',
  styleUrl: './reglages-export.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReglagesExportComponent {
  readonly filtres = input.required<readonly FiltreCatalogue[]>();
  readonly valeurs = input.required<FiltresRetenus>();
  /** Identifiant de rattachement des libellés, pour que deux panneaux ouverts ne se disputent pas les mêmes. */
  readonly prefixe = input<string>('reglage');
  readonly reglage = output<{ code: FiltreExport; valeurs: string[] }>();

  protected retenues(filtre: FiltreCatalogue): string[] {
    return this.valeurs()[filtre.code] ?? AUCUNE_VALEUR;
  }

  protected unique(filtre: FiltreCatalogue): string | null {
    return this.retenues(filtre)[0] ?? null;
  }

  protected choisirPlusieurs(filtre: FiltreCatalogue, selection: unknown): void {
    this.reglage.emit({ code: filtre.code, valeurs: (selection as string[] | null) ?? [] });
  }

  protected choisirUne(filtre: FiltreCatalogue, selection: unknown): void {
    const valeur = selection as string | null;
    this.reglage.emit({ code: filtre.code, valeurs: valeur ? [valeur] : [] });
  }
}
