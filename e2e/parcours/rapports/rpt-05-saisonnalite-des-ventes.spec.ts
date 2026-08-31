import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Une officine ne vend pas la même chose en janvier et en août, et son mois de mars n'a de
 * sens que comparé à un autre mois de mars. Le rapport met donc chaque mois de la période
 * FACE au même mois de l'année précédente, avec l'écart entre les deux.
 *
 * C'est la lecture qui distingue une bonne saison d'une bonne année : un mois en hausse de
 * 4 % dans un métier dont le mois est structurellement fort n'est pas une performance.
 */
scenario('RPT-05', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await ouvrirRapport(page, 'sales', 'Saisonnalité CA');
    await expect(page.getByRole('heading', { name: /Saisonnalité du Chiffre d'Affaires/ })).toBeVisible();
  });

  await etape(2, async () => {
    // La période et la période précédente côte à côte, mois par mois.
    await expect(contenu).toContainText(/CA PÉRIODE/i);
    await expect(contenu).toContainText(/ÉVOLUTION/i);
    await expect(page.getByRole('heading', { name: /Évolution mensuelle du CA/ })).toBeVisible();
    await expect(contenu.locator('tbody tr')).not.toHaveCount(0);
  });
});
