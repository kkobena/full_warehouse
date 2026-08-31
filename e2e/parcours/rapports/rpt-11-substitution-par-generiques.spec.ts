import { expect } from '@playwright/test';
import { rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * La substitution est à la fois un acte pharmaceutique et une décision économique : le
 * générique coûte moins cher au patient, laisse souvent une meilleure marge à l'officine, et
 * le taux de substitution est un indicateur que l'assurance maladie regarde.
 *
 * Le rapport ne compare donc pas les génériques à l'ensemble des ventes — ce qui ne voudrait
 * rien dire — mais aux PRINCEPS SUBSTITUABLES : les seuls cas où la substitution était
 * possible. C'est ce dénominateur qui rend le taux interprétable.
 */
scenario('RPT-11', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await ouvrirRapport(page, 'sales', 'Génériques');
    await expect(page.getByRole('heading', { name: /Génériques & Substitution/ })).toBeVisible();
  });

  await etape(2, async () => {
    const fin = new Date();
    const debut = new Date(fin.getFullYear(), fin.getMonth() - 2, 1);
    await saisirDate(page, 'gs-from', debut);
    await saisirDate(page, 'gs-to', fin);
    await rechercher(page);

    await expect(contenu).toContainText(/PRODUITS GÉNÉRIQUES VENDUS/i);
    // Le dénominateur qui donne son sens au taux.
    await expect(contenu).toContainText(/PRINCEPS SUBSTITUABLES VENDUS/i);
    await expect(contenu).toContainText(/CA GÉNÉRIQUES/i);
  });
});
