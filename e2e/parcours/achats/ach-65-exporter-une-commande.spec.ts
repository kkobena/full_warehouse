import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une commande s'exporte pour deux usages qui n'ont rien à voir : le PDF part chez le
 * grossiste ou au classeur — il se lit tel quel ; le CSV part dans un tableur, pour comparer
 * des prix ou préparer un import. D'où deux formats et non un seul.
 *
 * Parcours en LECTURE : il produit un fichier, il ne modifie rien.
 */
scenario('ACH-65', async ({ etape, page }) => {
  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    const liste = page.locator('app-list-bons');
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    await liste.locator('tbody tr').filter({ hasNotText: 'Clôturé' }).first().dblclick();
    await expect(page.getByRole('button', { name: 'Exporter' })).toBeVisible();
  });

  await etape(2, async () => {
    const actions = page.locator('app-split-button').filter({ hasText: 'Exporter' });
    await actions.getByRole('button').last().click();
    await expect(page.getByRole('button', { name: 'PDF' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'CSV' })).toBeVisible();
    const telechargement = page.waitForEvent('download');
    await page.getByRole('button', { name: 'PDF' }).click();
    const fichier = await telechargement;
    expect(fichier.suggestedFilename()).toMatch(/\.(pdf|csv)$/);
  });
});
