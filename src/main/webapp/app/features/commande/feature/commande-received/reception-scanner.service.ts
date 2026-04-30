import { Injectable } from '@angular/core';
import { BaseScannerService } from '../../../../shared/scanner';

/**
 * Service de scan dédié au module de réception de bon de livraison.
 *
 * Intentionnellement NON fourni en racine (`providedIn: 'root'`) pour éviter
 * tout partage de buffer/état avec les autres modules (vente, recherche produit…).
 *
 * Il doit être déclaré dans le tableau `providers` du composant hébergeur :
 *
 * ```typescript
 * @Component({ providers: [ReceptionScannerService] })
 * export class CommandeReceivedComponent { ... }
 * ```
 *
 * Avantages par rapport à l'utilisation directe de `ScanDetectorService` :
 *  - État de scan isolé (buffer propre à la réception)
 *  - Possibilité d'ajouter des comportements spécifiques à la réception
 *    (ex : blocage des inputs actifs pendant un scan) sans régression sur la vente
 *  - Réinitialisation automatique lors de la destruction du composant
 */
@Injectable()
export class ReceptionScannerService extends BaseScannerService {}

