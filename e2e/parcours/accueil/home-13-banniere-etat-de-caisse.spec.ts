import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { ouvrirLaSessionCaissier } from './_caissier';

/**
 * « Combien devrais-je avoir en tiroir ? » est la question du caissier, et il fallait jusqu'ici
 * lancer une clôture pour l'apprendre — c'est-à-dire arrêter la caisse pour la consulter.
 *
 * La bannière y répond en permanence : fond d'ouverture, encaissements en espèces déjà faits,
 * et leur somme, les espèces théoriques. L'écart avec le tiroir se voit donc à tout moment, pas
 * seulement à la fermeture.
 *
 * Caisse fermée, la bannière donne la date de la dernière fermeture : l'état est toujours nommé.
 */
scenario('HOME-13', async ({ etape, page }) => {
  await etape(1, async () => {
    await ouvrirLaSessionCaissier(page);
  });

  await etape(2, async () => {
    const banniere = page.locator('.caisse-banner');
    await expect(banniere).toBeVisible();
    await expect(banniere).toContainText(/Caisse Ouverte|Caisse Fermée/);
  });
});
