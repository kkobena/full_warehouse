import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Range des `app-detail-section` en rangées qui se partagent la largeur.
 *
 * <p>Flex et non grid : une grille fige ses colonnes pour toutes les rangées, et une dernière
 * rangée de deux cartes gardait une colonne vide pendant que les valeurs voisines tronquaient.
 */
@Component({
  selector: 'app-detail-grid',
  template: `<ng-content />`,
  styles: `
    :host {
      display: flex;
      flex-wrap: wrap;
      // Sans quoi chaque carte s'étire à la hauteur de la plus haute de sa rangée.
      align-items: flex-start;
      // Assez d'air pour que l'ombre des cartes se voie.
      gap: 0.9rem;
      padding: 0.1rem  0.5rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DetailGridComponent {}
