import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * L'écran des différés n'avait rien à montrer avant que 09_ventes.sql ne produise des ventes
 * à crédit et 14b_reglements.sql leurs règlements. C'est le premier parcours qui illustre
 * cette partie du jeu de données.
 */
scenario('CLI-11', async ({ etape, page }) => {
  // Colonne « Restant » de la liste : c'est elle qui distingue un compte différé d'un
  // client ordinaire, donc l'ancrage qui prouve qu'on est sur le bon écran.
  const lignes = page.locator('tbody tr');

  await etape(1, async () => {
    await page.goto('/differes');
    await expect(page.getByRole('columnheader', { name: 'Restant' })).toBeVisible();
  });

  await etape(2, async () => {
    // Au moins un compte : le jeu de démonstration laisse délibérément des créances
    // vivantes, sans quoi cet écran — et les relances — n'auraient rien à afficher.
    await expect(lignes.first()).toBeVisible();
    await expect(await lignes.count()).toBeGreaterThan(0);
  });
});
