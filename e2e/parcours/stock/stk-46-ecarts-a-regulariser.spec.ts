import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un stock négatif n'est pas une anomalie tant qu'il correspond aux avoirs ouverts : c'est la
 * marchandise due aux clients. La liste « Écarts à régulariser » ne montre que ce qui dépasse,
 * c'est-à-dire des boîtes sorties sans que la machine le sache — la liste de travail avant un
 * ajustement ou un inventaire.
 *
 * Parcours en LECTURE : le contenu dépend du jeu de démonstration, on vérifie la structure.
 */
scenario('STK-46', async ({ etape, page }) => {
  await etape(1, async () => {
    await page.goto('/features-ajustement');
    await expect(page.getByRole('button', { name: 'Écarts à régulariser' })).toBeVisible();
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Écarts à régulariser' }).click();
    await expect(page).toHaveURL(/features-ajustement\/ecarts/);
    const tableau = page.locator('app-data-table');
    for (const colonne of ['Stock machine', 'Dû aux avoirs', 'Écart']) {
      await expect(tableau).toContainText(colonne);
    }
  });

  await etape(3, async () => {
    // Vide ou non, la liste mène au geste correctif.
    await expect(page.locator('app-data-table')).toContainText(/Aucun écart|\d/);
    await page.getByRole('button', { name: 'Nouvel ajustement' }).click();
    await expect(page.locator('app-ajustement-form')).toBeVisible();
  });
});
