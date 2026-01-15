import { Injectable } from '@angular/core';
import { BaseScannerService, ScanEvent } from './scanner/base-scanner.service';

// Re-export ScanEvent pour la compatibilité avec le code existant
export { ScanEvent } from './scanner/base-scanner.service';

/**
 * Service de détection de scan pour les composants individuels.
 * Utilisé par ProduitSearchAutocompleteScannerComponent pour détecter
 * les codes-barres scannés dans le champ de recherche.
 *
 * Ce service étend BaseScannerService et maintient la compatibilité
 * avec l'API existante (méthode keyPressed retournant string | null).
 *
 * @example
 * // Écouter les événements de scan :
 * scanDetectorService.onScanEvent$.subscribe(event => {
 *   if (event.type === 'complete') {
 *     console.log('Scanned:', event.code);
 *   }
 * });
 *
 * // Traiter les touches :
 * document.addEventListener('keydown', e => scanDetectorService.keyPressed(e.key));
 */
@Injectable({
  providedIn: 'root',
})
export class ScanDetectorService extends BaseScannerService {
  /**
   * Traite une touche pressée.
   * Maintient la compatibilité avec l'ancienne API.
   *
   * @param key - La touche pressée (event.key)
   * @returns Le code scanné si un scan vient de se terminer avec Enter, null sinon
   * @deprecated Préférer utiliser processKey() qui retourne plus d'informations
   */
  keyPressed(key: string): string | null {
    const result = this.processKey(key);
    return result.completedCode;
  }
}
