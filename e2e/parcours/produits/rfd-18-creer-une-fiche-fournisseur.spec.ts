import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * La fiche fournisseur ne sert pas qu'à passer commande : c'est elle qui, ensuite, permet de
 * savoir si le grossiste tient ses délais, ce qu'on lui doit et à quelle échéance, et si le
 * palier de remise de fin d'année est en vue.
 *
 * Trois blocs, trois usages distincts :
 *
 *   • le DÉLAI de livraison et la fréquence de commande alimentent les suggestions de
 *     réapprovisionnement — commander tous les trois jours chez un grossiste qui livre en
 *     vingt-quatre heures n'appelle pas les mêmes quantités ;
 *   • les JOURS DE CRÉDIT et le seuil critique datent les échéances et déclenchent l'alerte
 *     avant qu'un compte ne dérape ;
 *   • le PALIER et le taux de RFA nourrissent la jauge de fin d'année (FAC-43).
 *
 * Parcours en LECTURE : il remplit sans enregistrer, un fournisseur de test s'invitant
 * ensuite dans toutes les commandes et tous les états de compte.
 */
scenario('RFD-18', async ({ etape, page }) => {
  const modal = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/fournisseur');
    await page.getByRole('button', { name: 'Nouveau fournisseur' }).click();
    await expect(modal).toBeVisible();
    await modal.locator('#field_libelle').fill('GROSSISTE TEST');
    await modal.locator('#field_code').fill('GRTEST');
  });

  await etape(2, async () => {
    await modal.locator('#field_phone').fill('2720304050');
    await modal.locator('#field_delaiLivraisonJours').first().fill('2');
    await modal.locator('#field_frequenceCommandeJours').fill('7');
    await modal.locator('#field_joursCredit').fill('30');
    await modal.locator('#field_joursCritique').fill('45');
    // Le palier et le taux : ce que le fournisseur a promis pour un volume annuel donné.
    await modal.locator('#field_palierRfa').fill('50000000');
    await modal.locator('#field_tauxRfa').fill('2');
  });

  await etape(3, async () => {
    await expect(modal.getByRole('button', { name: 'Enregistrer' })).toBeEnabled();
  });
});
