import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * « Il manque 42 000 sur cette facture » : la phrase ne suffit pas à décrocher un règlement.
 * Il faut pouvoir dire ce qui a été reçu — quand, par quel moyen, pour quel montant — et donc
 * descendre au détail des versements de la facture en écart.
 *
 * Le dépliage de la ligne les donne, dans l'ordre où ils sont arrivés. C'est là qu'on
 * découvre qu'un virement de mars n'a jamais été imputé, ou qu'un acompte a été porté sur la
 * facture voisine.
 *
 * Parcours en LECTURE.
 */
scenario('FAC-17', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Rapprochement/);
    await rechercher(page);
    await lignes.first().locator('button:has(.pi-eye)').first().click();
    await expect(contenu).toContainText('N° Facture');
  });

  await etape(2, async () => {
    // Seules les factures ayant reçu au moins un versement portent le chevron : les autres
    // n'ont rien à déplier, et l'écart y vaut le montant facturé.
    const avecReglements = lignes.filter({ has: page.locator('button:has(.pi-chevron-right)') }).first();
    await expect(avecReglements).toBeVisible();
    await avecReglements.locator('button:has(.pi-chevron-right)').first().click();
    await expect(contenu).toContainText(/Règlement|Montant|Date/);
  });
});
