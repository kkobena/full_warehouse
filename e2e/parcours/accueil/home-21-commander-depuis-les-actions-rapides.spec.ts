import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Deux façons de commander, et deux boutons pour les distinguer.
 *
 * « Commander les urgents » part du calcul : ruptures et produits sous le seuil de sécurité,
 * déjà triés, prêts à devenir une commande. « Nouvelle commande » part de l'intention du
 * pharmacien — une promotion, une commande de saison, un besoin que le stock ne connaît pas
 * encore.
 *
 * Aucun des deux ne demande de passer par le menu : c'est le sens d'une action rapide.
 */
scenario('HOME-21', async ({ etape, page }) => {
  await etape(1, async () => {
    await page.goto('/commande');
    await expect(page.locator('app-kpi-strip').first()).toBeVisible({ timeout: 20000 });
    await expect(page.getByRole('button', { name: 'Commander les urgents' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Nouvelle commande' })).toBeVisible();
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Commander les urgents' }).click();
    // On arrive directement sur les suggestions, sans étape de sélection intermédiaire.
    await expect(page.locator('#main-content')).toContainText(/Suggestion|SEMOIS|Urgent/i);
  });
});
