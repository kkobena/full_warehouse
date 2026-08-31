import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Le bandeau porte quatre indicateurs, dont trois cliquables : ils servent de raccourcis de
 * filtrage. « Prochaines péremptions 30j » et « Retours fournisseur » n'affichaient AUCUNE
 * valeur avant d'être branchés côté backend — le modèle Angular déclarait deux champs que
 * l'API n'a jamais envoyés.
 */
scenario('STK-33', async ({ etape, page }) => {
  await page.goto('/gestion-peremption');
  await expect(page.getByRole('tab', { name: /Produits périmés/ })).toBeVisible();

  await etape(1, async () => {
    const raccourci = page.locator('app-kpi-item.clickable').filter({ hasText: /Prochaines péremptions/ });
    // L'indicateur doit PORTER UN NOMBRE avant d'être cliqué : c'est ce qui distingue un
    // raccourci utilisable d'une carte muette, et c'est précisément ce qui manquait.
    await expect(raccourci).toContainText(/\d/);
    await raccourci.click();
    await expect(page.locator('tbody tr').first()).toBeVisible();
  });
});
