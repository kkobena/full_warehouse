import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { ouvrirLaSessionCaissier } from './_caissier';

/**
 * Une créance client se recouvre au comptoir, quand le client repasse — pas depuis un bureau.
 * Le bloc met donc la relance là où elle peut se faire : le nom, le montant dû, le téléphone
 * s'il est connu, et un niveau d'urgence.
 *
 * Il ne retient que les échéances DU JOUR. C'est délibéré : la liste de tous les impayés serait
 * ingérable au comptoir, et une liste ingérable finit ignorée.
 */
scenario('HOME-15', async ({ etape, page }) => {
  await etape(1, async () => {
    await ouvrirLaSessionCaissier(page);
  });

  await etape(2, async () => {
    const bloc = page.locator('.card.data-card', { has: page.getByText('Différés à Relancer') });
    await expect(bloc).toBeVisible();
    await expect(bloc).toContainText(/échéance\(s\) aujourd'hui|Aucun différé à relancer/);
  });
});
