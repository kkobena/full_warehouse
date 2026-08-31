import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Compter un rayon ne se décide pas à l'avance : on constate un écart devant le meuble, et
 * l'on compte tout de suite. Passer par l'écran général d'inventaire — choisir la catégorie,
 * le stockage, puis retrouver le rayon dans une liste — casse cet élan.
 *
 * Le raccourci pré-remplit ce que le contexte donne déjà : la catégorie « rayon », le
 * stockage, et le rayon lui-même. Il ne reste qu'à confirmer, et l'inventaire s'ouvre en
 * saisie.
 *
 * C'est le même inventaire que celui de l'écran général, pas une variante : il se clôture et
 * se valorise de la même façon (STK-05 et suivants).
 *
 * Parcours en LECTURE : il s'arrête avant la création, un inventaire ouvert verrouillant les
 * mouvements du rayon pour les parcours de stock.
 */
scenario('RFD-16', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modal = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/rayon');
    await expect(lignes.first()).toBeVisible();
    await lignes.first().click();
    await expect(page.getByRole('button', { name: 'Inventaire' })).toBeVisible();
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Inventaire' }).click();
    await expect(modal).toBeVisible();
  });

  await etape(3, async () => {
    // Le rayon et son stockage sont déjà posés : c'est tout l'intérêt du raccourci.
    await expect(modal).toContainText(/[Rr]ayon/);
    await expect(modal.getByRole('button', { name: /Créer|Valider|Enregistrer/ }).first()).toBeVisible();
  });
});
