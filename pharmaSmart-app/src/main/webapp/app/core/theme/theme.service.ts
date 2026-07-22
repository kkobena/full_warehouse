import { Injectable } from '@angular/core';

/**
 * Historique : ce service pilotait un sélecteur multi-thèmes PrimeNG
 * (Aura/Lara/Material/Nora). Depuis le retrait de `primeng`/`@primeuix/themes`,
 * l'app n'a plus qu'un seul habillage visuel (Bootswatch "yeti" + tokens figés
 * dans `_pharma-tokens.scss`, cf. docs/PLAN-MIGRATION-PRIMENG-VERS-NGBOOTSTRAP.md §8.8).
 * Conservé en stub le temps qu'un futur thème custom PharmaSmart (§8.8.2) ne le
 * remplace, pour ne pas casser les injections existantes.
 */
@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  loadCurrentTheme(): void {
    // Aucun thème alternatif à charger — voir le commentaire de classe.
  }
}
