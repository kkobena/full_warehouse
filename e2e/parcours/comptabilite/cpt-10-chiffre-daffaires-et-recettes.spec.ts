import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le rapport d'activité répond à la question la plus simple et la plus mal posée : « combien
 * a-t-on fait ? ». Car deux chiffres différents s'y cachent.
 *
 * Le CHIFFRE D'AFFAIRES est ce qui a été vendu — TTC, TVA, HT, remises, net, et la marge. Les
 * RECETTES sont ce qui est entré en caisse. Les deux divergent dès qu'il y a du crédit ou du
 * tiers payant : une vente d'aujourd'hui payée dans deux mois est du chiffre d'affaires
 * aujourd'hui, et une recette plus tard.
 *
 * Les afficher côte à côte, sur la même période, est ce qui rend l'écart visible plutôt que
 * subi.
 */
scenario('CPT-10', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
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
    // Le chiffre d'affaires et sa décomposition, jusqu'à la marge.
    await expect(contenu).toContainText("Chiffre d'affaires");
    await expect(contenu).toContainText(/Montant TTC/);
    await expect(contenu).toContainText(/Marge/);
  });

  await etape(3, async () => {
    // Les recettes, qui n'en sont pas la copie : ce qui est réellement entré.
    await expect(contenu).toContainText('Recettes');
  });
});
