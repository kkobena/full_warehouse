import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * L'état des créances clients sort de l'officine : il part à la comptabilité, ou sur le bureau
 * du titulaire qui décide qui relancer.
 *
 * L'export ne produit pas un état standard mais l'ÉCRAN TEL QU'IL EST FILTRÉ — même période,
 * même client, même état. C'est ce qui permet de transmettre « les créances en cours au
 * 31 août » sans retoucher le document ensuite.
 *
 * Deux écrans l'offrent, pour deux questions différentes : la liste des différés dit ce qui
 * reste dû, l'historique des règlements dit ce qui est rentré.
 */
scenario('CLI-18', async ({ etape, page }) => {
  await etape(1, async () => {
    await page.goto('/differes');
    // On filtre d'abord : c'est le périmètre affiché que le PDF reprendra.
    await choisirDansSelect(page, 'dhStatut', 'En cours');
    await rechercher(page);
    await expect(page.locator('tbody tr').filter({ visible: true }).first()).toBeVisible();
    await expect(page.getByRole('button', { name: 'PDF' })).toBeVisible();
  });

  await etape(2, async () => {
    // L'autre export, sur l'écran voisin : les encaissements plutôt que les créances.
    await ouvrirOnglet(page, /Historique/);
    await expect(page.getByRole('button', { name: 'Exporter PDF' })).toBeVisible();
  });
});
