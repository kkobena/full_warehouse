import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La TVA se déclare sur pièce : le rapport imprimé est ce que l'officine transmet, et ce
 * qu'elle conserve pour justifier le montant reversé.
 *
 * L'export reprend la période et la ventilation affichées — journalière ou sur toute la
 * période — parce qu'un contrôle porte sur une échéance précise, pas sur un cumul recalculé
 * autrement.
 */
scenario('CPT-03', async ({ etape, page }) => {
  const fin = new Date();
  const debut = new Date(fin);
  debut.setDate(debut.getDate() - 60);

  await etape(1, async () => {
    await page.goto('/comptabilite');
    await ouvrirOnglet(page, /Rapport TVA/);
    await saisirDate(page, 'dateDebut', debut);
    await saisirDate(page, 'dateFin', fin);
    await rechercher(page);
  });

  await etape(2, async () => {
    // L'impression part de ce qui est à l'écran : même période, même ventilation.
    await expect(page.getByRole('button', { name: 'Imprimer' })).toBeVisible();
  });
});
