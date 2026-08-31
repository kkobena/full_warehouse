import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet, saisirMontant } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un règlement fournisseur non espèces laisse forcément une trace ailleurs : un numéro de
 * chèque, une référence de virement, un ticket de carte. L'exiger ici, c'est garantir qu'on
 * pourra rapprocher le paiement du relevé bancaire — et répondre à un grossiste qui affirme
 * n'avoir rien reçu.
 *
 * La règle est CONDITIONNELLE, et elle ne l'était pas : le champ était obligatoire même pour
 * un paiement en espèces, ce qui obligeait à inventer une référence pour enregistrer du
 * liquide. Corrigé en écrivant ce parcours.
 *
 * Parcours en LECTURE : il montre le refus, il ne valide aucun règlement.
 */
scenario('ACH-68', async ({ etape, page }) => {
  const ecran = page.locator('app-comptes-fournisseurs');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Comptes fournisseurs/);
    await expect(ecran.locator('tbody tr').first()).toBeVisible();
    await ecran.locator('tbody tr').first().click();
    // L'onglet du DÉTAIL, et non celui de la barre latérale : les deux portent des noms
    // proches, et cliquer le second quitte l'écran.
    await ecran.getByRole('tab', { name: 'Régler' }).click();
    await choisirDansSelect(page, 'modeReglement', 'Chèque');
  });

  await etape(2, async () => {
    // Le champ arrive PRÉ-REMPLI du solde dû : on le remplace, on ne complète pas.
    await saisirMontant(page, '#montant', '1000');
    await page.locator('#reference').fill('');
    // Le refus n'attend pas le clic : le bouton reste INERTE tant que la référence manque.
    // C'est la forme la plus claire du contrôle — on ne peut pas se tromper puis découvrir
    // l'erreur, on ne peut simplement pas valider.
    const enregistrer = ecran.getByRole('button', { name: 'Enregistrer' });
    await expect(enregistrer).toBeDisabled();
    // La référence renseignée, l'enregistrement redevient possible.
    await page.locator('#reference').fill('CH-2026-0148');
    await expect(enregistrer).toBeEnabled();
  });
});
