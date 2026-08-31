import { expect } from '@playwright/test';
import { ouvrirBonDeReception, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * « Le grossiste m'a facturé 1 880 alors que j'avais commandé à 1 760. » La question suivante
 * n'est pas de savoir si l'écart existe — l'écran le signale déjà (ACH-43) — mais s'il est
 * VRAISEMBLABLE. Un produit dont le tarif a monté deux fois en six mois n'appelle pas la même
 * réaction qu'un prix stable depuis deux ans.
 *
 * L'historique répond à cela : pour un couple produit/fournisseur donné, les changements
 * successifs de prix d'achat et de prix de vente, du plus récent au plus ancien, avec le bon
 * de livraison qui les a constatés. Il est alimenté par la réception elle-même, à chaque fois
 * qu'un prix diffère de celui de la fiche.
 *
 * Parcours en LECTURE.
 */
scenario('ACH-44', async ({ etape, page }) => {
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    await ouvrirBonDeReception(page, 'aucun');
    // La grille donne accès, ligne par ligne, à l'historique du couple produit/fournisseur.
    await page.getByRole('button', { name: 'Grille' }).click();
    await expect(page.locator('.ag-row').first()).toBeVisible();
  });

  await etape(2, async () => {
    const ligne = page.locator('.ag-row').first();
    await ligne.getByRole('button', { name: 'Historique des prix' }).click();
    await expect(modale).toContainText(/Historique/i);
    // Ancien et nouveau prix, d'achat comme de vente, et le bon qui les a constatés.
    await expect(modale).toContainText(/Ancien PA/);
    await expect(modale).toContainText(/Bon de livraison/);
  });
});
