import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un inventaire général tient sur deux cents pages : l'imprimer entier pour vérifier un rayon
 * est un gâchis, et le relire un supplice. L'export se cadre donc avant d'être lancé.
 *
 * Deux réglages, qui ne servent pas la même chose : le REGROUPEMENT organise le document —
 * par rayon pour aller recompter sur place, par famille pour discuter avec le comptable, sans
 * regroupement pour un tableur — et le FILTRE DE LIGNES choisit ce qu'on emporte : tout, ou
 * seulement les lignes en écart, qui sont les seules à traiter.
 *
 * Un filtre d'emplacement complète l'ensemble quand l'inventaire couvre rayon et réserve.
 *
 * Parcours en LECTURE.
 */
scenario('STK-27', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/inventaire');
    await ouvrirOnglet(page, /Clôturés/);
    await expect(lignes.first()).toContainText(/clôturé/i);
    await lignes.first().getByRole('button', { name: 'Exporter PDF' }).click();
    await expect(modale).toBeVisible();
  });

  await etape(2, async () => {
    // Le regroupement : la structure du document produit.
    await modale.locator('ng-select').first().click();
    await expect(page.locator('.ng-option').filter({ hasText: 'Grouper par rayon' })).toBeVisible();
    await page.locator('.ng-option').filter({ hasText: 'Grouper par famille' }).first().click();
    await expect(modale).toContainText('Grouper par famille');
  });

  await etape(3, async () => {
    // Le filtre de lignes : ce qu'on emporte. Les lignes en écart suffisent le plus souvent.
    await modale.locator('ng-select').nth(1).click();
    await expect(page.locator('.ng-option').first()).toBeVisible();
    await page.locator('.ng-option').first().click();
    await expect(modale.getByRole('button', { name: 'Exporter PDF' })).toBeVisible();
  });
});
