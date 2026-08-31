import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le rayon est la carte de l'officine : c'est lui qui dit où trouver une boîte, ce qu'on
 * compte lors d'un inventaire tournant, et ce qu'on range après réception.
 *
 * Deux qualifications comptent plus que les autres. Le STOCKAGE de rattachement, parce qu'un
 * même libellé — « Réserve » — n'est pas le même endroit selon qu'il relève du rayon ou de
 * l'arrière-boutique. Et le TYPE DE ZONE — ambiant, froid, toxique, ordonnance — parce qu'il
 * gouverne des règles réelles : on ne range pas un vaccin ailleurs qu'au froid.
 *
 * La position est libre, et c'est voulu : chaque officine a son vocabulaire de rangement.
 *
 * Parcours en LECTURE : il remplit sans enregistrer, un rayon de test se retrouvant ensuite
 * dans tous les filtres et tous les inventaires.
 */
scenario('RFD-11', async ({ etape, page }) => {
  const modal = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/rayon');
    await page.getByRole('button', { name: 'Nouveau' }).click();
    await expect(modal).toBeVisible();

    await modal.locator('#rf-libelle').fill('VITRINE SAISONNIERE');
    await modal.locator('#rf-code').fill('VITSAI');
    await choisirDansSelect(page, 'rf-typeZone', 'Ambiant');
    await modal.locator('#rf-position').fill('Façade gauche, allée 2');
  });

  await etape(2, async () => {
    await expect(modal.getByRole('button', { name: 'Enregistrer' })).toBeEnabled();
  });
});
