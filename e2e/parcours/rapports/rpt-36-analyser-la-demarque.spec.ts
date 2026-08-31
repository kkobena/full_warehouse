import { expect } from '@playwright/test';
import { rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * La démarque, c'est le stock qui disparaît sans avoir été vendu : casse, vol, péremption,
 * erreur de comptage. Chaque unité perdue l'est AU COÛT D'ACHAT — c'est de l'argent déjà
 * sorti, pas une vente manquée.
 *
 * Le rapport la ventile par MOTIF, et c'est le motif qui appelle la décision : de la casse
 * répétée sur un rayon désigne un rangement, des péremptions un surstock, un écart d'inventaire
 * récurrent une procédure. Le total, seul, ne dit rien de ce qu'il faut changer.
 */
scenario('RPT-36', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await ouvrirRapport(page, 'stock', 'Démarque');
    await expect(page.getByRole('heading', { name: /Démarque & Ajustements/ })).toBeVisible();
  });

  await etape(2, async () => {
    const fin = new Date();
    const debut = new Date(fin.getFullYear(), fin.getMonth() - 5, 1);
    await saisirDate(page, 'dem-from', debut);
    await saisirDate(page, 'dem-to', fin);
    await rechercher(page);
  });

  await etape(3, async () => {
    // La valeur au coût d'achat en tête, la ventilation par motif au-dessous.
    await expect(contenu).toContainText(/Valeur perdue/i);
    await expect(contenu).toContainText(/Quantité perdue/i);
    await expect(contenu).toContainText(/Motif/i);
  });
});
