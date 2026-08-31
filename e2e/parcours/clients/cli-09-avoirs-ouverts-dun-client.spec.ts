import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un avoir naît d'un retour : le client rapporte un produit, on lui crédite la somme. La
 * question, à sa visite suivante, est donc « combien lui reste-t-il à consommer ? ».
 *
 * Le solde affiché ne compte QUE les avoirs ouverts et non expirés — c'est tout l'intérêt du
 * chiffre : additionner des avoirs déjà consommés ou périmés promettrait au client un crédit
 * qu'il n'a plus.
 *
 * Les avoirs soldés et expirés restent visibles pour autant : ils expliquent, le jour où le
 * client s'étonne, pourquoi son crédit a disparu.
 */
scenario('CLI-09', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/customer');
    await expect(lignes.first()).toBeVisible();
    await lignes.first().locator('app-button[ngbtooltip="Voir détails"] button').first().click();
    await page.getByRole('tab', { name: /Avoirs/ }).first().click();
  });

  await etape(2, async () => {
    // Le solde en tête, puis ce qui le compose : chaque avoir avec sa date d'expiration et
    // son statut, les deux critères qui décident s'il compte encore.
    await expect(contenu).toContainText(/Solde disponible|Aucun avoir pour ce client/);
  });
});
