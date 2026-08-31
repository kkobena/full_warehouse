import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La remise de fin d'année ne se voit nulle part dans le prix d'achat : elle se gagne sur le
 * VOLUME, à la fin de l'exercice, et un fournisseur chez qui on commande un peu moins que
 * prévu fait perdre un palier entier.
 *
 * D'où la jauge : elle compare le chiffre commandé depuis janvier au palier négocié, et dit
 * en un coup d'œil chez qui il reste quelques commandes à passer pour décrocher la remise.
 *
 * Cette jauge affichait 100 % en toute circonstance jusqu'à peu — la barre remplissait son
 * conteneur quelle que soit la valeur — ce qui laissait croire tous les paliers atteints.
 */
scenario('FAC-43', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Remises & RFA/);
    await expect(contenu).toContainText('Paliers RFA');
  });

  await etape(2, async () => {
    // Le palier négocié, ce qui a été commandé, et l'écart entre les deux : les trois
    // colonnes qui font décider d'une commande de fin d'année.
    await expect(contenu).toContainText('Palier RFA');
    await expect(contenu).toContainText('CA commandé');
    await expect(contenu).toContainText('% atteint');
    await expect(page.locator('.progress-bar-rfa').first()).toBeVisible();
  });
});
