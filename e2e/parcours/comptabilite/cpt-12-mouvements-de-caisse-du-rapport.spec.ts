import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Toute la caisse ne vient pas des ventes : il y a les entrées diverses, les sorties, les
 * fonds de caisse, les règlements de fournisseurs payés au comptoir.
 *
 * Le rapport d'activité les rassemble dans leur propre carte, à côté du chiffre d'affaires et
 * des recettes. C'est ce qui explique un tiroir dont le contenu ne correspond pas aux ventes
 * du jour — et sans cette carte, on chercherait longtemps.
 */
scenario('CPT-12', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const fin = new Date();
  const debut = new Date(fin.getFullYear(), fin.getMonth() - 2, 1);

  await etape(1, async () => {
    await page.goto('/comptabilite');
    await ouvrirOnglet(page, /Rapport d'activité/);
    await saisirDate(page, 'du', debut);
    await saisirDate(page, 'au', fin);
    await rechercher(page);
  });

  await etape(2, async () => {
    await expect(contenu).toContainText('Mouvements de caisse');
  });
});
