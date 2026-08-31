import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le virement de l'assureur est arrivé : il faut l'imputer. Le règlement se saisit depuis la
 * facture elle-même, dans un onglet « Régler » qui n'apparaît que sur une facture NON RÉGLÉE
 * ou PARTIELLEMENT réglée — une facture soldée n'a rien à recevoir, et l'écran ne propose pas
 * le geste.
 *
 * Trois informations suffisent : le montant, le mode de paiement, la date. Un interrupteur
 * « règlement partiel » dit d'emblée si l'on solde ou si l'on encaisse un acompte — c'est lui
 * qui décide du statut de la facture après enregistrement (FAC-12).
 *
 * Le mode de paiement commande la suite : un chèque demande la banque, son numéro et le
 * bénéficiaire, qu'on retrouvera au rapprochement bancaire.
 *
 * Parcours en LECTURE : il ouvre la saisie et montre ses champs, sans imputer — un règlement
 * fictif fausserait le recouvrement du jeu de démonstration.
 */
scenario('FAC-11', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /^Factures/);
    // Une facture non réglée : les seules à porter l'onglet « Régler ».
    await choisirDansSelect(page, 'fhStatut', 'Non réglées');
    await page.keyboard.press('Escape');
    await rechercher(page);
    await expect(lignes.first()).toBeVisible();
    await lignes.first().click();
    // Le panneau maître-détail s'ouvre sur le contenu de la facture.
    await expect(page.getByRole('columnheader', { name: 'N° Bon' })).toBeVisible();
  });

  await etape(2, async () => {
    // L'onglet n'existe que sur une facture qui attend de l'argent.
    await ouvrirOnglet(page, /Régler/);
    await expect(contenu).toContainText(/Montant/);
    await expect(contenu).toContainText(/Mode de paiement/);
  });

  await etape(3, async () => {
    // Solder ou encaisser un acompte : l'interrupteur le dit avant la saisie, et c'est lui
    // qui décidera du statut de la facture.
    await expect(page.locator('#partialPayment')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Valider' })).toBeVisible();
  });
});
