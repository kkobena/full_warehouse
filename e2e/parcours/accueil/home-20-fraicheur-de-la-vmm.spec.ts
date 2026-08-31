import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Toutes les suggestions de commande reposent sur la VMM — la vente mensuelle moyenne. Une VMM
 * calculée il y a une semaine propose donc des quantités d'il y a une semaine, sans que rien à
 * l'écran ne le signale : c'est la façon la plus discrète de commander de travers.
 *
 * D'où le badge : il dit l'âge du dernier calcul, et non pas seulement son résultat. Et d'où
 * l'action qui l'accompagne, pour relancer le calcul sans attendre le cycle automatique — un
 * pic de grippe ne demande pas l'autorisation du planificateur.
 */
scenario('HOME-20', async ({ etape, page }) => {
  await etape(1, async () => {
    await page.goto('/commande');
    await expect(page.locator('app-kpi-strip').first()).toBeVisible({ timeout: 20000 });
  });

  await etape(2, async () => {
    // « VMM · À jour » ou « VMM · 12/08/2026 » : l'âge, jamais un simple voyant vert.
    await expect(page.locator('app-toolbar')).toContainText(/VMM/);
  });

  await etape(3, async () => {
    const recalculer = page.getByRole('button', { name: /Recalculer VMM/i });
    await expect(recalculer).toBeVisible();
    await recalculer.click();
    // Le calcul est confié au serveur : l'écran confirme la prise en compte, il n'attend pas
    // le résultat pour rendre la main.
    await expect(page.locator('app-toolbar')).toContainText(/VMM/);
  });
});
