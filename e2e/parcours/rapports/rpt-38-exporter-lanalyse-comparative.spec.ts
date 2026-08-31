import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * L'export ne rejoue pas le rapport : il prend la vue AFFICHÉE — celle que l'on vient de
 * configurer et de vérifier — et l'imprime telle quelle. Configurer d'abord, exporter ensuite :
 * ce qui part est ce que l'on a lu.
 */
scenario('RPT-38', async ({ etape, page }) => {
  await etape(1, async () => {
    await ouvrirRapport(page, 'sales', 'Tableaux Comparatifs');
    await expect(page.getByRole('heading', { name: 'Tableaux Comparatifs CA' })).toBeVisible();
    await page.getByRole('button', { name: 'Par famille' }).click();
    await expect(page.locator('#main-content').locator('tbody tr').first()).toBeVisible();
  });

  await etape(2, async () => {
    await expect(page.getByRole('button', { name: 'Exporter PDF' })).toBeVisible();
  });
});
