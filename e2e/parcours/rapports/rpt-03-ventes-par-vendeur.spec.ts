import { expect } from '@playwright/test';
import { rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Comparer des vendeurs est un exercice délicat : le chiffre d'affaires seul récompense celui
 * qui a tenu la caisse un samedi. Le rapport donne donc quatre grandeurs ensemble — le nombre
 * de ventes, le montant, le TICKET MOYEN et le taux de remise accordé — et c'est leur lecture
 * croisée qui dit quelque chose.
 *
 * Un vendeur à fort ticket moyen et forte remise ne fait pas le même métier qu'un vendeur à
 * petits paniers nombreux ; le total de tous les vendeurs, lui, doit retomber sur le CA de la
 * période.
 */
scenario('RPT-03', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await ouvrirRapport(page, 'sales', 'Performance Vendeurs');
    await expect(page.getByRole('heading', { name: 'Performance des Vendeurs' })).toBeVisible();
  });

  await etape(2, async () => {
    const fin = new Date();
    const debut = new Date(fin.getFullYear(), fin.getMonth() - 2, 1);
    await saisirDate(page, 'sbs-from', debut);
    await saisirDate(page, 'sbs-to', fin);
    await rechercher(page);

    await expect(contenu).toContainText(/VENDEURS ACTIFS/i);
    await expect(contenu).toContainText(/TICKET MOYEN/i);
    // Les vendeurs de la démonstration se relaient sur la période : le classement en compte
    // plusieurs, sans quoi la comparaison n'aurait pas d'objet.
    await expect(contenu.locator('tbody tr')).not.toHaveCount(0);
  });
});
