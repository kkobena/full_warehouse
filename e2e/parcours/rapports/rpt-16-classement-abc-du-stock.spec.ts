import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Une officine référence plusieurs milliers de produits et ne peut pas les suivre avec la même
 * attention. Le classement ABC dit lesquels méritent qu'on s'en occupe.
 *
 * PharmaSmart en retient cinq classes plutôt que trois : A+ pour les 60 premiers pourcents du
 * chiffre d'affaires, A jusqu'à 80 %, B jusqu'à 95 %, C jusqu'à 99 %, et D pour le reste — dont
 * les produits SANS AUCUNE VENTE. Cette dernière classe est celle qu'un ABC classique noie
 * dans le C, alors qu'elle désigne exactement ce qui immobilise de l'argent sans rien
 * rapporter.
 */
scenario('RPT-16', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await ouvrirRapport(page, 'stock', 'Analyse ABC');
    // Le titre de l'écran est un `<h1>` porté par `app-toolbar` ; la carte du tableau
    // porte un `<h5>` de même libellé. On vise le titre d'écran.
    await expect(page.getByRole('heading', { level: 1, name: /Analyse ABC Pareto/ })).toBeVisible();
  });

  await etape(2, async () => {
    // Les cinq classes, avec pour chacune le nombre de références et la part de CA : c'est
    // le rapport entre les deux qui fait la lecture.
    await expect(contenu).toContainText(/A\+/);
    await expect(contenu).toContainText(/80–95% CA/);
    await expect(contenu).toContainText(/SANS VENTES/i);
    await expect(contenu).toContainText(/TOTAL PRODUITS/i);
  });
});
