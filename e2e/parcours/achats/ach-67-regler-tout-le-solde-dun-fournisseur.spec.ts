import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * On ne règle pas un grossiste bon de livraison par bon de livraison : on lui vire une somme,
 * et elle s'impute sur ce qu'on lui doit.
 *
 * « Régler tout » fait exactement cela — il répartit le montant saisi sur les commandes non
 * soldées, EN COMMENÇANT PAR LES PLUS ANCIENNES, jusqu'à épuisement du montant ou du solde.
 * L'ordre n'est pas un détail : c'est lui qui fait tomber les créances les plus âgées, celles
 * qui déclenchent les relances.
 *
 * Un versement inférieur au solde laisse donc les commandes récentes ouvertes, ce qui est le
 * comportement attendu d'un acompte.
 *
 * Parcours en LECTURE : il ouvre la saisie sans valider, un règlement soldant pour de bon des
 * commandes dont vivent les autres parcours de compte.
 */
scenario('ACH-67', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Comptes fournisseurs/);
    await expect(lignes.first()).toBeVisible();
    await lignes.first().click();
    await expect(contenu).toContainText(/Solde/);
  });

  await etape(2, async () => {
    // L'onglet « Régler » du panneau : le solde dû y est rappelé avant toute saisie.
    await page.getByRole('tab', { name: /Régler/ }).first().click();
    await expect(contenu).toContainText(/Solde à régler/);
  });

  await etape(3, async () => {
    // Le geste qui répartit : montant, mode et référence, puis imputation par ancienneté.
    await expect(page.getByRole('button', { name: 'Régler tout' })).toBeVisible();
  });
});
