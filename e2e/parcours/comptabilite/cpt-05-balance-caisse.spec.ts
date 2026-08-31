import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

scenario('CPT-05', async ({ etape, page }) => {
  // Période calculée et non écrite en dur : le jeu de démonstration est daté par rapport au
  // jour du chargement, une date figée cesserait de ramener des mouvements.
  const fin = new Date();
  const debut = new Date(fin);
  debut.setDate(debut.getDate() - 60);

  await etape(1, async () => {
    await page.goto('/comptabilite');
    await ouvrirOnglet(page, /Balance/);
    await expect(page.getByText('Balance de caisse')).toBeVisible();
  });

  await etape(2, async () => {
    await saisirDate(page, 'dateDebut', debut);
    await saisirDate(page, 'dateFin', fin);
    await rechercher(page);

    // Sans période, l'écran affiche des totaux à zéro — état parfaitement valide, mais qui
    // n'illustrerait pas « consulter la balance ». L'assertion exige donc un total NON nul :
    // c'est ce qui distingue l'écran renseigné de l'écran vide, et elle attend au passage la
    // fin du calcul avant que la capture ne soit prise.
    await expect(page.getByText(/Total TTC/)).toBeVisible();
    await expect(page.locator('#main-content')).not.toContainText('Total TTC 0 FCFA');
  });
});
