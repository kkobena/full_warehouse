import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un fournisseur conteste un solde : il faut lui opposer un relevé, bon de livraison par bon
 * de livraison, avec ce qui a été réglé et ce qui reste dû.
 *
 * Deux portées, et l'écran les distingue nettement. Depuis la barre, l'export couvre TOUS les
 * comptes — c'est le document qu'on remet à la comptabilité en fin de mois. Depuis le panneau
 * de détail, il ne couvre QUE le fournisseur ouvert — c'est celui qu'on envoie au grossiste
 * qui réclame.
 *
 * Dans les deux cas, le PDF reprend ce qui est à l'écran, filtres compris : on transmet ce
 * qu'on a vérifié, pas un état recalculé autrement.
 *
 * Parcours en LECTURE : il montre les deux boutons sans déclencher de téléchargement.
 */
scenario('ACH-63', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Comptes fournisseurs/);
    await expect(lignes.first()).toBeVisible();
    // L'export global : tous les comptes, tels que filtrés.
    await expect(page.getByRole('button', { name: 'Exporter en PDF' }).first()).toBeVisible();
  });

  await etape(2, async () => {
    // Le détail d'un fournisseur porte son propre export, limité à lui.
    await lignes.first().click();
    await expect(page.getByRole('button', { name: 'Exporter en PDF' }).nth(1)).toBeVisible();
  });
});
