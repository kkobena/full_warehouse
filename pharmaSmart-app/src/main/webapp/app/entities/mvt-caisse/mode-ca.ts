import { computed, inject, Injectable } from '@angular/core';

import { LicenseService } from 'app/core/license/license.service';

/** Lequel des deux chiffres d'affaires un écran de comptabilité affiche. */
export type ModeCa = 'REEL' | 'DECLARE';

/**
 * Les modules qui séparent le chiffre réel du chiffre déclaré.
 *
 * <p>Énumérés plutôt que déduits du drapeau « optionnel » du catalogue : toute option vendue plus
 * tard deviendrait sinon un motif de basculer la comptabilité en mode déclaré. Doit rester
 * identique à `ModeChiffreAffaireResolver.MODULES_RETRAITEMENT` côté serveur.
 */
export const MODULES_RETRAITEMENT_CA = ['EXCLUSION_RAYON', 'EXCLUSION_TP', 'EXCLUSION_UG', 'CALLEBASSE'] as const;

/**
 * Décide du mode d'affichage des écrans de comptabilité.
 *
 * <p>Défaut **`REEL`** : l'état neutre, la donnée non retraitée. Il ne bascule vers `DECLARE` que
 * sur souscription avérée d'au moins un module de retraitement.
 */
@Injectable({ providedIn: 'root' })
export class ModeCaService {
  private readonly licenseService = inject(LicenseService);

  /** Recalculé si la licence change en cours de session : une activation ne doit pas attendre un redémarrage. */
  readonly modeComptabilite = computed<ModeCa>(() =>
    MODULES_RETRAITEMENT_CA.some(module => this.licenseService.hasFeature(module)) ? 'DECLARE' : 'REEL',
  );
}
