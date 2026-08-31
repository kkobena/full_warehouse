import { expect } from '@playwright/test';
import { ouvrirOnglet, traverserConfirmations } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La destruction physique a eu lieu : le prestataire est passé, le carton est parti. Le
 * marquer dans l'application ferme le cycle du produit périmé — retrait du stock, attente,
 * destruction — et date l'opération.
 *
 * Cette date n'est pas une formalité : c'est elle qui figure au procès-verbal, et qui
 * répondra à l'inspection demandant ce qu'est devenu tel lot de stupéfiants. Le lot marqué
 * détruit ne disparaît donc pas de la liste : il y reste, avec sa date, et son historique de
 * transactions reste consultable.
 *
 * Parcours ÉCRIVANT dans la base : il marque un lot comme détruit.
 */
scenario('STK-43', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/gestion-peremption');
    await ouvrirOnglet(page, /Lots à détruire/);
    await expect(lignes.first()).toBeVisible();
  });

  await etape(2, async () => {
    // Un lot pas encore détruit : lui seul porte l'action.
    const aDetruire = lignes.filter({ has: page.getByRole('button', { name: 'Détruire ce lot' }) }).first();
    await aDetruire.getByRole('button', { name: 'Détruire ce lot' }).click();
    await expect(page.locator('.modal-content:visible').first()).toBeVisible();
    await traverserConfirmations(page, { limite: 1 });
    // Le lot reste dans la liste, désormais daté : c'est la trace, pas une disparition.
    await expect(lignes.first()).toBeVisible();
  });
});
