import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le procès-verbal de destruction n'est pas un confort : c'est la pièce qu'une inspection
 * réclame, et la seule preuve que des stupéfiants périmés ont été détruits plutôt que
 * détournés. Il se produit depuis la liste des lots à détruire, dans les trois formats
 * habituels — PDF pour signer, Excel et CSV pour l'archivage et le contrôle.
 *
 * Comme pour les péremptions (STK-35), l'export reprend le PÉRIMÈTRE FILTRÉ : un procès-verbal
 * se fait pour une période et un magasin donnés, pas pour l'historique entier.
 *
 * Parcours en LECTURE : il ouvre le choix du format sans produire le document.
 */
scenario('STK-45', async ({ etape, page }) => {
  await etape(1, async () => {
    await page.goto('/gestion-peremption');
    await ouvrirOnglet(page, /Lots à détruire/);
    await expect(page.locator('tbody tr').first()).toBeVisible();
    // Le périmètre se cadre avant, sous « Filtres avancés » : dates, magasin, emplacement,
    // rayon. Un procès-verbal porte sur une période, pas sur l'historique entier.
    await expect(page.getByRole('button', { name: /Filtres avancés/ })).toBeVisible();
  });

  await etape(2, async () => {
    const exporter = page.locator('app-split-button').filter({ hasText: /Export/i }).first();
    await exporter.getByRole('button').last().click();
    await expect(page.getByRole('button', { name: 'PDF' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Excel' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Csv' })).toBeVisible();
  });
});
