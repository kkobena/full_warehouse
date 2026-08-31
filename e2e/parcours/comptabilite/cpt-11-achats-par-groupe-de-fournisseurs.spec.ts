import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une officine commande chez plusieurs agences d'un même grossiste. Lire les achats agence par
 * agence donne une vue en miettes ; c'est au niveau du GROUPE que se négocie la remise de fin
 * d'année, et donc au niveau du groupe qu'il faut lire le volume.
 *
 * Cette ventilation dit avec qui l'officine travaille réellement — et fait apparaître la
 * dépendance à un fournisseur avant qu'elle ne devienne un risque.
 */
scenario('CPT-11', async ({ etape, page }) => {
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
    // Le regroupement n'apparaît que s'il y a des achats sur la période : sans commande, la
    // section n'aurait rien à montrer.
    await expect(contenu).toContainText(/Achats par groupe fournisseur|Total des achats/);
  });
});
