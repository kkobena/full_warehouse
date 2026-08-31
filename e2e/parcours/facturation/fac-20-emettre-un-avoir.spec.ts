import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Émettre, c'est arrêter d'hésiter : l'avoir cesse d'être modifiable et devient un document
 * qu'on peut opposer au tiers payant. Il ne réduit encore aucun solde — ce sera l'imputation
 * (FAC-21) — mais son montant et son motif ne bougeront plus.
 *
 * L'écran le dit en n'offrant « Émettre » que sur les brouillons : sur un avoir déjà émis, le
 * bouton n'existe pas. Le serveur refuse de son côté tout avoir qui n'est pas en brouillon,
 * si bien que la règle tient même si l'on contourne l'écran.
 */
scenario('FAC-20', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Avoirs/);
    await rechercher(page);
    // Le brouillon se reconnaît à ce qu'il est le seul à porter le bouton « Émettre ».
    const brouillon = lignes.filter({ has: page.getByRole('button', { name: 'Émettre' }) }).first();
    await expect(brouillon).toBeVisible();
    await expect(brouillon).toContainText(/AV-/);
  });

  await etape(2, async () => {
    const brouillon = lignes.filter({ has: page.getByRole('button', { name: 'Émettre' }) }).first();
    await brouillon.getByRole('button', { name: 'Émettre' }).click();
    await expect(contenu).toContainText(/Émis/);
  });
});
