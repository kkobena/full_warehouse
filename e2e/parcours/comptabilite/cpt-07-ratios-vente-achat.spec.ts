import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Deux ratios, et ils disent la même chose à l'envers l'un de l'autre : ce que l'officine vend
 * rapporté à ce qu'elle achète, et l'inverse.
 *
 * Leur utilité tient à leur lecture croisée sur la durée. Un ratio vente/achat qui tombe mois
 * après mois signale qu'on achète plus vite qu'on ne vend — le stock gonfle, la trésorerie se
 * tend, et cela se voit ici bien avant de se voir en banque.
 *
 * Ils figurent dans le même tableau que le chiffre d'affaires et les achats qui les
 * composent : le ratio n'est pas un chiffre à part, c'est leur rapport, et il se vérifie
 * d'un coup d'œil sur la même ligne.
 */
scenario('CPT-07', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
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
    // Les ratios sont une colonne du tableau, à côté de ce qui les compose.
    await expect(contenu).toContainText('Ratios');
    await expect(contenu).toContainText("Chiffre d'affaires");
    await expect(contenu).toContainText('Achats');
  });
});
