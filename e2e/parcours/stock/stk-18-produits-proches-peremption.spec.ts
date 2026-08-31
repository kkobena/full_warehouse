import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Constat relevé en écrivant ce parcours, volontairement PAS transformé en assertion : la
 * liste est triée par date de péremption DÉCROISSANTE — les échéances les plus lointaines
 * d'abord — sur un écran dont l'objet est de montrer ce qui approche. Figer ce tri dans une
 * assertion reviendrait à le consacrer ; il est signalé, pas entériné.
 */
scenario('STK-18', async ({ etape, page }) => {
  await etape(1, async () => {
    await page.goto('/gestion-peremption');
    await expect(page.getByRole('tab', { name: /Produits périmés/ })).toBeVisible();
  });

  await etape(2, async () => {
    await expect(page.getByRole('columnheader', { name: 'Date de péremption' })).toBeVisible();
    await expect(page.locator('tbody tr').first()).toContainText(/\d{2}\/\d{2}\/\d{4}/);
  });
});
