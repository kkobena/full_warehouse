import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * « On fait mieux que l'an dernier ? » Répondre demandait deux extractions et une soustraction.
 * Le rapport pose les deux périodes côte à côte et calcule l'écart.
 *
 * Trois vues, parce qu'un même écart n'a pas la même cause selon l'angle : la vue GLOBALE dit
 * s'il y a progression, la vue PAR FAMILLE dit quel rayon la porte, la vue PAR FOURNISSEUR dit
 * chez qui l'achat a suivi. Les deux dernières se trient sur chaque colonne — un tableau de
 * cinquante familles ne se lit pas dans l'ordre alphabétique.
 */
scenario('RPT-12', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await ouvrirRapport(page, 'sales', 'Tableaux Comparatifs');
    await expect(page.getByRole('heading', { name: 'Tableaux Comparatifs CA' })).toBeVisible();
  });

  await etape(2, async () => {
    // La vue par famille : celle qui désigne le rayon responsable de l'écart.
    await page.getByRole('button', { name: 'Par famille' }).click();
    await expect(contenu.locator('tbody tr').first()).toBeVisible();
  });

  await etape(3, async () => {
    await choisirDansSelect(page, 'compType', 'Mensuel');
    await expect(contenu).toContainText(/CA/);
  });

  await etape(4, async () => {
    // Le tri porte sur l'évolution : le classement des familles qui progressent le plus, ou
    // le moins — la seule lecture qui appelle une décision.
    await contenu.locator('th.sortable-col').last().click();
    await expect(contenu.locator('tbody tr').first()).toBeVisible();
  });
});
