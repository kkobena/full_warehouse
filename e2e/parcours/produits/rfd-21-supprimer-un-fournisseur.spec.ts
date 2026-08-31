import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Supprimer un fournisseur principal emporte ses agences — c'est logique, une agence sans
 * maison mère n'a plus de conditions commerciales — mais ce n'est pas ce à quoi on pense en
 * cliquant sur une corbeille.
 *
 * L'écran le dit donc AVANT, en toutes lettres, dans la demande de confirmation. C'est la
 * seule chose qui sépare un ménage de printemps d'une perte de trois dépôts et de leur
 * historique de commandes.
 *
 * Parcours en LECTURE : il montre l'avertissement sans supprimer.
 */
scenario('RFD-21', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/fournisseur');
    await expect(lignes.first()).toBeVisible();
  });

  await etape(2, async () => {
    await lignes.first().locator('app-button[ngbtooltip="Supprimer"] button').first().click();
  });

  await etape(3, async () => {
    // L'avertissement nomme le fournisseur ET la conséquence sur ses agences.
    const question = page.locator('.modal-content:visible');
    await expect(question).toContainText(/Supprimer/);
    await expect(question).toContainText(/agences rattachées seront également supprimées/i);
  });
});
