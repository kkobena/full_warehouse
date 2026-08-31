import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le tableau du pharmacien est le document que le cabinet comptable réclame : une ligne par
 * période, les ventes, les achats par grossiste, les ratios.
 *
 * Deux formats, deux usages. Le PDF est ce qu'on remet — figé, daté, présentable. L'Excel est
 * ce qu'on retravaille : le comptable y ajoute ses colonnes, ses calculs, son propre
 * découpage.
 *
 * L'export reprend la période et le regroupement affichés : c'est ce qu'on a vérifié à
 * l'écran qui part, pas un état recalculé autrement.
 */
scenario('CPT-08', async ({ etape, page }) => {
  const fin = new Date();
  const debut = new Date(fin.getFullYear(), fin.getMonth() - 2, 1);

  await etape(1, async () => {
    await page.goto('/comptabilite');
    await ouvrirOnglet(page, /Tableau pharmacien/);
    await saisirDate(page, 'dateDebut', debut);
    await saisirDate(page, 'dateFin', fin);
    await rechercher(page);
  });

  await etape(2, async () => {
    // Le chevron ouvre le choix du format ; l'action principale sort le PDF.
    await page.getByRole('button', { name: 'Autres actions' }).first().click();
    const menu = page.locator('.dropdown-menu.show');
    await expect(menu.getByRole('button', { name: 'Excel' })).toBeVisible();
    await expect(menu.getByRole('button', { name: 'PDF' })).toBeVisible();
  });
});
