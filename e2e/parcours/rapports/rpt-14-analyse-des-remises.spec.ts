import { expect } from '@playwright/test';
import { rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Une remise ne se voit pas dans le chiffre d'affaires : elle en est absente. C'est ce qui la
 * rend difficile à surveiller — on ne compte pas ce qu'on n'a pas encaissé.
 *
 * Le rapport la remet au bilan : le MONTANT TOTAL REMISÉ sur la période, le taux moyen qu'il
 * représente, la part des ventes concernées, et le classement des produits sur lesquels la
 * remise se concentre. Deux points de remise moyenne sur un rayon à forte rotation pèsent plus
 * qu'un geste commercial exceptionnel dont tout le monde se souvient.
 */
scenario('RPT-14', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await ouvrirRapport(page, 'sales', 'Analyse des Remises');
    await expect(page.getByRole('heading', { name: 'Analyse des Remises' })).toBeVisible();
  });

  await etape(2, async () => {
    const fin = new Date();
    const debut = new Date(fin.getFullYear(), fin.getMonth() - 5, 1);
    await saisirDate(page, 'ra-from', debut);
    await saisirDate(page, 'ra-to', fin);
    await rechercher(page);

    await expect(contenu).toContainText(/MONTANT TOTAL REMISÉ/i);
    // Le CA après remises à côté du montant remisé : la remise se lit toujours par rapport
    // à ce qu'elle a laissé.
    await expect(contenu).toContainText(/CA APRÈS REMISES/i);
    await expect(contenu).toContainText(/VENTES AVEC REMISE/i);
    await expect(page.getByRole('heading', { name: /Top 10 produits par montant remisé/ })).toBeVisible();
  });
});
