import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le récapitulatif de caisse part souvent ailleurs : au comptable, au siège d'un groupement.
 * L'imprimer pour le scanner ensuite n'a pas de sens.
 *
 * L'envoi utilise l'adresse configurée sur la FICHE DE L'OFFICINE, et non une adresse saisie
 * au moment de l'envoi : c'est ce qui évite qu'un chiffre de caisse parte à la mauvaise
 * personne un soir de fatigue. Sans adresse valide, l'envoi est refusé avec un message qui
 * invite à compléter la fiche — plutôt qu'un échec silencieux.
 *
 * Parcours en LECTURE : il montre le point d'envoi sans expédier de courriel.
 */
scenario('CPT-15', async ({ etape, page }) => {
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
    // L'envoi est offert à côté de l'export : même document, deux chemins de sortie.
    await expect(page.getByRole('button', { name: 'Message' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Exporter' })).toBeVisible();
  });
});
