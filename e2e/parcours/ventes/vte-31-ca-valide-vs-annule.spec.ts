import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le chiffre d'affaires d'une journée n'est pas ce qui a été tapé au comptoir : c'est ce qui
 * y est resté. Une vente annulée après encaissement — erreur de ligne, client qui se ravise —
 * disparaît du CA, et confondre les deux, c'est se croire plus performant qu'on ne l'est.
 *
 * PharmaSmart tient donc les deux comptes sur la même période : le JOURNAL des ventes donne
 * le chiffre d'affaires des ventes clôturées, l'écran des ANNULATIONS donne le montant annulé
 * et le nombre d'annulations. Les ventes importées d'un autre système sont exclues des deux :
 * elles n'ont pas été faites ici.
 *
 * Parcours en LECTURE.
 */
scenario('VTE-31', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/sales-home/gestion');
    await ouvrirOnglet(page, /Journal des ventes/);
    await expect(contenu).toContainText(/Total CA/);
  });

  await etape(2, async () => {
    // La période se cadre par les mêmes bornes des deux côtés : c'est ce qui rend la
    // comparaison honnête.
    await expect(contenu).toContainText(/Du/);
    await expect(contenu).toContainText(/Nombre de ventes/);
  });

  await etape(3, async () => {
    // Le pendant : ce que la même période a perdu en annulations.
    await ouvrirOnglet(page, /Annulations/);
    await expect(contenu).toContainText(/Nombre total annulations/);
    await expect(contenu).toContainText(/Montant annulé/);
  });
});
