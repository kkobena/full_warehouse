import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une officine à plusieurs postes clôture plusieurs caisses. Additionner les tickets Z à la
 * main, poste par poste et jour par jour, est long et se trompe.
 *
 * Le récapitulatif consolide tous les tickets Z de la période, quelle que soit la caisse, et
 * les ventile PAR MODE DE PAIEMENT. C'est cette ventilation qui sert : l'espèce se compte dans
 * le tiroir, le mobile et la carte se rapprochent des relevés.
 *
 * La consolidation doit être exacte dans les deux sens — ne rien omettre, ne rien compter
 * deux fois — puisque c'est elle qui justifie le dépôt en banque.
 */
scenario('CPT-09', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const fin = new Date();
  const debut = new Date(fin);
  debut.setDate(debut.getDate() - 60);

  await etape(1, async () => {
    await page.goto('/comptabilite');
    await ouvrirOnglet(page, /Récapitulatif de caisse/);
    await saisirDate(page, 'fromDate', debut);
    await saisirDate(page, 'toDate', fin);
    await rechercher(page);
  });

  await etape(2, async () => {
    // Le total consolidé, puis sa ventilation par mode : c'est elle qu'on rapproche.
    await expect(contenu).toContainText(/RÉCAPITULATIF GÉNÉRAL/i);
    await expect(contenu).toContainText(/VUE D'ENSEMBLE DES PAIEMENTS/i);
    await expect(contenu).toContainText(/ESPECE/i);
  });
});
