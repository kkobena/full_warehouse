import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { ouvrirLaSessionCaissier } from './_caissier';

/**
 * Le caissier est souvent celui qui reçoit le livreur. Savoir avant midi que trois grossistes
 * doivent passer, et combien de références chacun apporte, change l'organisation de la journée —
 * et évite de découvrir au comptoir un carton dont personne n'attendait l'arrivée.
 */
scenario('HOME-16', async ({ etape, page }) => {
  await etape(1, async () => {
    await ouvrirLaSessionCaissier(page);
  });

  await etape(2, async () => {
    const bloc = page.locator('.card.data-card', { has: page.getByText('Livraisons du Jour') });
    await expect(bloc).toBeVisible();
    await expect(bloc).toContainText(/réf\.|Aucune livraison prévue/);
  });
});
