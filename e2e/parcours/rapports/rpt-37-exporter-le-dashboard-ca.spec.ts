import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Trois sorties, trois usages — et ce ne sont pas trois formats du même fichier.
 *
 * Le PDF est le tableau de bord lui-même, graphiques compris : c'est la pièce qu'on transmet
 * au comptable ou qu'on archive. L'Excel et le CSV sortent le RÉSUMÉ QUOTIDIEN du chiffre
 * d'affaires — une ligne par jour, faite pour être recalculée ailleurs.
 *
 * Confondre les deux fait perdre du temps : on n'agrège pas un PDF, et on ne présente pas un
 * CSV.
 */
scenario('RPT-37', async ({ etape, page }) => {
  await etape(1, async () => {
    await ouvrirRapport(page, 'sales', 'Dashboard CA');
    await expect(page.getByRole('heading', { name: /Dashboard Chiffre d'Affaires/ })).toBeVisible();
  });

  await etape(2, async () => {
    await expect(page.getByRole('button', { name: 'PDF', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Excel', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'CSV', exact: true })).toBeVisible();
  });
});
