import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Deux questions se posaient sur deux écrans : « où en sont mes commandes ? » d'un côté, « que
 * dois-je réapprovisionner ? » de l'autre. Elles n'ont pourtant qu'une réponse — ce qu'il faut
 * commander aujourd'hui.
 *
 * La bande les réunit : commandes en attente et bons de livraison à réceptionner viennent du
 * suivi des commandes ; ruptures et produits sous le seuil VMM viennent du moteur SEMOIS.
 * Chaque indicateur est cliquable et ouvre l'écran où l'on traite ce qu'il annonce, ce qui
 * évite le trajet retour par le menu.
 */
scenario('HOME-19', async ({ etape, page }) => {
  const bande = page.locator('app-kpi-strip').first();

  await etape(1, async () => {
    await page.goto('/commande');
    await expect(bande).toBeVisible({ timeout: 20000 });
  });

  await etape(2, async () => {
    // Les deux origines dans la même bande : le suivi des commandes et le calcul SEMOIS.
    await expect(bande).toContainText(/Commandes/i);
    await expect(bande).toContainText(/Bons livraison/i);
    await expect(bande).toContainText(/Stock critique|À commander/i);
  });

  await etape(3, async () => {
    // Un indicateur n'est pas qu'un nombre : il mène à l'écran de traitement.
    await bande.locator('app-kpi-item').first().click();
    await expect(page.locator('#main-content')).toBeVisible();
  });
});
