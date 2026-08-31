import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * La liste des péremptions sort de l'écran pour trois usages qui n'ont pas le même format :
 * le PDF pour la tournée dans les rayons, papier en main ; l'Excel pour trier et chiffrer
 * avant d'appeler le grossiste ; le CSV pour ce qu'on veut en faire ensuite.
 *
 * L'export reprend la liste TELLE QU'ELLE EST FILTRÉE : c'est ce qui permet de sortir « les
 * périmés du rayon dermato » sans exporter les huit cents lignes de l'officine. Filtrer
 * d'abord, exporter ensuite (STK-32).
 *
 * Parcours en LECTURE : il ouvre le choix du format sans produire le fichier.
 */
scenario('STK-35', async ({ etape, page }) => {
  await etape(1, async () => {
    await page.goto('/gestion-peremption');
    await expect(page.locator('tbody tr').first()).toBeVisible();
  });

  await etape(2, async () => {
    // Le bouton à choix : le format se retient au moment d'exporter, pas avant.
    const exporter = page.locator('app-split-button').filter({ hasText: /Export/i }).first();
    await exporter.getByRole('button').last().click();
    await expect(page.getByRole('button', { name: 'PDF' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Excel' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Csv' })).toBeVisible();
  });
});
