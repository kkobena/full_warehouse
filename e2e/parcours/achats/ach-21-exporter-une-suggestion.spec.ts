import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une proposition d'achat s'emporte hors de l'application pour deux raisons : la faire viser
 * par le titulaire, ou la comparer aux tarifs d'un autre grossiste dans un tableur. D'où les
 * deux formats, offerts directement sur la ligne du fournisseur — sans avoir à ouvrir la
 * proposition.
 *
 * Parcours en LECTURE : il produit un fichier, il ne modifie rien.
 */
scenario('ACH-21', async ({ etape, page }) => {
  const liste = page.locator('app-suggestion-fournisseur-list');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Propositions d'achat/ }).click();
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    // Les actions tiennent dans le menu de la ligne : éditer, valider, commander, exporter
    // dans les deux formats, supprimer.
    await liste.locator('tbody tr').first().getByRole('button', { name: 'Actions' }).click();
    await expect(page.getByRole('button', { name: 'Export PDF' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Export CSV' })).toBeVisible();
  });

  await etape(2, async () => {
    const telechargement = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Export PDF' }).click();
    const fichier = await telechargement;
    expect(fichier.suggestedFilename()).toMatch(/\.(pdf|csv)$/);
  });
});
