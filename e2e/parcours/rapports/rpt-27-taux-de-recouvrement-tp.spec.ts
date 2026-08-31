import { expect } from '@playwright/test';
import { rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Facturer n'est pas encaisser. Le taux de recouvrement rapporte ce qui a été RÉGLÉ à ce qui a
 * été facturé sur la période : c'est la seule mesure qui distingue un bon mois d'un mois où
 * l'on a beaucoup travaillé sans être payé.
 *
 * Le rapport l'accompagne de son commentaire — « à surveiller », par exemple — et du décompte
 * des factures en retard, parce qu'un taux moyen peut masquer un seul organisme qui ne règle
 * plus rien.
 */
scenario('RPT-27', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await ouvrirRapport(page, 'finance', 'Taux Recouvrement');
    await expect(page.getByRole('heading', { name: /Taux de Recouvrement Tiers-Payants/ })).toBeVisible();
  });

  await etape(2, async () => {
    const fin = new Date();
    const debut = new Date(fin.getFullYear(), fin.getMonth() - 5, 1);
    await saisirDate(page, 'trt-from', debut);
    await saisirDate(page, 'trt-to', fin);
    await rechercher(page);

    await expect(contenu).toContainText(/TAUX DE RECOUVREMENT/i);
    // Les deux montants dont le taux est le rapport, et les impayés qui restent.
    await expect(contenu).toContainText(/MONTANT RÉGLÉ/i);
    await expect(contenu).toContainText(/MONTANT RESTANT/i);
    await expect(contenu).toContainText(/EN RETARD/i);
  });
});
