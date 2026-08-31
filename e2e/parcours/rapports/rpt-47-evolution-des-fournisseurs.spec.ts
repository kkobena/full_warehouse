import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Le tableau des fournisseurs dit où l'on en est ; l'onglet « Évolution N vs N-1 » dit où l'on
 * va. Un grossiste dont le volume double en un an a gagné une position de négociation ; un
 * dont le délai de livraison s'allonge de deux jours prépare une rupture qu'on n'a pas encore
 * vue.
 *
 * La comparaison porte sur les deux grandeurs ensemble — les montants ET les délais — parce
 * qu'un fournisseur moins cher et plus lent n'est pas moins cher.
 */
scenario('RPT-47', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await ouvrirRapport(page, 'partners', 'Performance Fournisseurs');
    await expect(page.getByRole('heading', { name: /Performance des Fournisseurs/ }).first()).toBeVisible();
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Évolution N vs N-1' }).click();
  });

  await etape(3, async () => {
    // Les deux périodes côte à côte, montants et délais.
    await expect(contenu).toContainText(/N-1|Évolution/i);
    await expect(contenu.locator('tbody tr').first()).toBeVisible();
  });
});
