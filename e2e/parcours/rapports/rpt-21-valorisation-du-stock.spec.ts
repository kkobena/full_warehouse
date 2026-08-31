import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Le stock d'une officine est son premier poste d'immobilisation : plusieurs mois de
 * trésorerie dorment sur les étagères. Le rapport en donne les deux faces — ce qu'il a COÛTÉ
 * et ce qu'il RAPPORTERAIT s'il se vendait — et l'écart entre les deux, la marge potentielle.
 *
 * « Potentielle » est le mot juste : cette marge n'est acquise que si le produit se vend avant
 * de périmer. Lue avec la rotation (RPT-20), elle dit où l'argent est réellement placé.
 */
scenario('RPT-21', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await ouvrirRapport(page, 'stock', 'Valorisation du Stock');
    await expect(page.getByRole('heading', { name: 'Valorisation du Stock' }).first()).toBeVisible();
  });

  await etape(2, async () => {
    // Les deux filtres du rapport : la famille de produit et le rayon.
    await expect(contenu).toContainText(/Famille de Produit/i);
    await expect(contenu).toContainText(/Rayons/i);
  });

  await etape(3, async () => {
    await expect(contenu).toContainText(/VALEUR D'ACHAT/i);
    await expect(contenu).toContainText(/VALEUR DE VENTE/i);
    await expect(contenu).toContainText(/MARGE POTENTIELLE/i);
    // Le taux de marge est cohérent avec les deux valorisations : il a longtemps affiché
    // 0,00 % à côté d'une marge de plusieurs dizaines de millions (division entière dans la
    // vue matérialisée, corrigée par V2.0.6).
    const margeMoyenne = contenu.locator('app-kpi-item').filter({ hasText: 'Marge moyenne' });
    await expect(margeMoyenne).not.toContainText('0,00 %');
  });

  await etape(4, async () => {
    // L'export est réellement déclenché, et non seulement constaté : le gabarit Thymeleaf du
    // rapport manquait purement et simplement (« Error resolving template
    // [reports/stock-valuation/main] »), et un parcours qui se contente de voir le bouton
    // n'aurait rien remarqué.
    const reponse = page.waitForResponse(
      r => r.url().includes('/api/stock/valuation/export') && r.request().method() === 'GET',
    );
    await page.getByRole('button', { name: 'Exporter PDF' }).click();
    const pdf = await reponse;
    expect(pdf.status(), "L'export PDF de la valorisation a échoué.").toBe(200);
    expect((await pdf.body()).subarray(0, 4).toString('latin1')).toBe('%PDF');
  });
});
