import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le rapport d'activité est le document de synthèse qu'on emporte en réunion ou qu'on
 * transmet au comptable : chiffre d'affaires, achats, recettes, mouvements de caisse et
 * ventilations, sur une seule pièce.
 *
 * L'export reprend la période affichée, sans recalcul : ce qu'on a vérifié à l'écran est ce
 * qui part.
 */
scenario('CPT-14', async ({ etape, page }) => {
  const fin = new Date();
  const debut = new Date(fin.getFullYear(), fin.getMonth() - 2, 1);

  await etape(1, async () => {
    await page.goto('/comptabilite');
    await ouvrirOnglet(page, /Rapport d'activité/);
    await saisirDate(page, 'du', debut);
    await saisirDate(page, 'au', fin);
    await rechercher(page);
  });

  await etape(2, async () => {
    await expect(page.getByRole('button', { name: 'Imprimer' })).toBeVisible();
  });
});
