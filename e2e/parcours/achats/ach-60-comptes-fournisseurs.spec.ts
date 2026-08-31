import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Ce que l'officine DOIT à ses grossistes, fournisseur par fournisseur : le solde dû, le
 * statut du compte et la prochaine échéance. C'est l'écran qu'on ouvre avant de décider quoi
 * régler cette semaine — et celui qu'on présente quand un commercial conteste un montant.
 *
 * Le solde n'est pas un total figé : il se construit des bons reçus moins les règlements
 * enregistrés, ce que le détail d'un fournisseur montre ligne à ligne (ACH-61).
 *
 * Parcours en LECTURE.
 */
scenario('ACH-60', async ({ etape, page }) => {
  const ecran = page.locator('app-comptes-fournisseurs');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Comptes fournisseurs/);
    await expect(ecran).toBeVisible();
    await expect(ecran.locator('tbody tr').first()).toBeVisible();
  });

  await etape(2, async () => {
    // Les colonnes de la décision : combien on doit, où en est le compte, et quand tombe la
    // prochaine échéance.
    await expect(ecran).toContainText('Solde dû');
    await expect(ecran).toContainText('Statut');
    await expect(ecran).toContainText('Prochaine échéance');
    // La recherche par fournisseur, et la période : un compte se lit toujours sur un
    // intervalle, jamais dans l'absolu.
    await expect(ecran.getByPlaceholder('Rechercher un fournisseur...')).toBeVisible();
  });
});
