import { expect } from '@playwright/test';
import { traverserConfirmations } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Tous les périmés ne se renvoient pas au grossiste : au-delà d'un certain délai, ou pour des
 * produits qu'il ne reprend pas, le lot ne repart nulle part — il sort du stock et attend la
 * destruction.
 *
 * Le retrait est donc l'issue la plus fréquente, et la plus simple : le lot quitte le stock
 * vendable, sans avoir de fournisseur, de bon ni de motif à choisir. Il rejoint les lots à
 * détruire (STK-42), où il attendra le procès-verbal.
 *
 * Ce que le retrait n'est pas : une correction d'inventaire. Le stock retiré est daté, tracé,
 * et sa valeur alimente la démarque connue — celle qu'on explique.
 *
 * Parcours ÉCRIVANT dans la base : il retire un lot périmé du stock.
 */
scenario('STK-36', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/gestion-peremption');
    await expect(page.getByRole('tab', { name: /Produits périmés/ })).toBeVisible();
    await expect(lignes.first()).toBeVisible();
  });

  await etape(2, async () => {
    // Un lot présent dans UN SEUL emplacement : le retrait y est ciblé d'office. Les lots
    // multi-emplacements demandent d'abord de choisir lequel (STK-34).
    const ligne = lignes.filter({ has: page.locator('.pharma-badge-info .pi-map-marker') }).first();
    await expect(ligne).toBeVisible();
    await ligne.getByRole('button', { name: 'Retirer du stock' }).click();
    const question = page.locator('.modal-content:visible').first();
    await expect(question).toBeVisible();
  });

  await etape(3, async () => {
    await traverserConfirmations(page, { limite: 1 });
    await expect(page.locator('.modal-content:visible')).toHaveCount(0);
  });
});
