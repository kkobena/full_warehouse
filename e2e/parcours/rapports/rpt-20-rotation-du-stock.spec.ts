import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Un produit qui rapporte beaucoup et un produit qui tourne vite ne sont pas le même produit.
 * Le Pareto répond à la première question, la rotation à la seconde : combien de fois le stock
 * détenu s'est-il écoulé sur la période.
 *
 * C'est l'angle de la TRÉSORERIE. Une référence à forte marge qui tourne une fois l'an
 * immobilise de l'argent douze mois durant, et finira probablement en péremption ; une
 * référence à faible marge qui tourne chaque semaine finance le reste du stock.
 */
scenario('RPT-20', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await ouvrirRapport(page, 'stock', 'Analyse ABC');
    // Le titre de l'écran est un `<h1>` porté par `app-toolbar` ; la carte du tableau
    // porte un `<h5>` de même libellé. On vise le titre d'écran.
    await expect(page.getByRole('heading', { level: 1, name: /Analyse ABC Pareto/ })).toBeVisible();
  });

  await etape(2, async () => {
    await page.getByRole('tab', { name: 'Par rotation' }).click();
    await expect(contenu.locator('tbody tr').first()).toBeVisible();
  });

  await etape(3, async () => {
    // Les produits à faible rotation : ceux dont on veut la liste, parce que ce sont eux
    // qu'on cessera de commander.
    await choisirDansSelect(page, 'rotClassification', 'C - Faible rotation');
    await expect(contenu.locator('tbody tr').first()).toBeVisible();
  });

  await etape(4, async () => {
    // L'export est déclenché pour de bon : son gabarit Thymeleaf manquait, et le serveur
    // répondait « Error resolving template [reports/stock-rotation/main] ». Un parcours qui
    // se contente de voir le bouton n'aurait rien remarqué.
    const reponse = page.waitForResponse(
      r => r.url().includes('/api/stock/rotation/export') && r.request().method() === 'GET',
    );
    await page.getByRole('button', { name: 'Exporter PDF' }).click();
    const pdf = await reponse;
    expect(pdf.status(), "L'export PDF de la rotation a échoué.").toBe(200);
    expect((await pdf.body()).subarray(0, 4).toString('latin1')).toBe('%PDF');
  });
});
