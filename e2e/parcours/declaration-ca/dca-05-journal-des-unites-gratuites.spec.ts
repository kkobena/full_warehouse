import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Des trois règles, celle des unités gratuites est la seule à retirer une PORTION de ligne.
 * Ce journal existe pour le montrer : il compte les lignes concernées, les unités réellement
 * offertes, et ce que leur valeur a retiré du chiffre d'affaires déclaré.
 *
 * Le reste de chaque ligne demeure dans l'assiette — l'écran le rappelle en toutes lettres,
 * parce que c'est exactement ce qu'un contrôleur voudra vérifier.
 */
scenario('DCA-05', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/declaration-ca');
    await ouvrirOnglet(page, 'Unités gratuites vendues');
    await expect(contenu).toContainText('Unités gratuites vendues');
  });

  await etape(2, async () => {
    await expect(contenu).toContainText(/lignes concernées/i);
    await expect(contenu).toContainText(/unités gratuites/i);
    // Le rappel qui distingue cette règle des deux autres.
    await expect(contenu).toContainText(/Le reste de la ligne y demeure/);
  });
});
