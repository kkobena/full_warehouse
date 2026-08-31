import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * L'avoir est la contrepartie financière du retour : il constate une créance sur le
 * fournisseur, qui viendra en déduction de la prochaine facture.
 *
 * Il ne se déduit pas du bon de retour tel quel, mais de ce que le grossiste a RECONNU
 * (ACH-56) : chaque ligne porte sa quantité acceptée, et l'avoir se constitue de celles-là.
 * Une ligne refusée — quantité acceptée à zéro — n'y entre pas.
 *
 * Les deux colonnes restent modifiables jusqu'au bout, quantité comme prix d'achat : le
 * grossiste reprend parfois à un tarif qui n'est plus celui de la commande, et l'avoir doit
 * refléter ce qu'il paiera, pas ce qu'on espérait.
 *
 * Parcours en LECTURE : il montre la constitution de l'avoir sans le créer.
 */
scenario('ACH-57', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Retours fournisseurs/);
    await expect(lignes.first()).toBeVisible();
    const repondable = lignes
      .filter({ has: page.locator('app-button[ngbtooltip="Saisir la réponse fournisseur"]') })
      .first();
    await repondable
      .locator('app-button[ngbtooltip="Saisir la réponse fournisseur"] button')
      .first()
      .click();
    await expect(modale).toBeVisible();
  });

  await etape(2, async () => {
    // Ce qui fonde l'avoir, ligne à ligne : la quantité reconnue et le prix retenu.
    await expect(modale).toContainText('Qté acceptée');
    await expect(modale).toContainText('Prix achat');
    // Le sort de chaque ligne se lit d'un coup d'œil — accepté, partiel, refusé.
    await expect(modale.locator('.pi-check-circle, .pi-times-circle, .pi-exclamation-triangle').first()).toBeVisible();
  });

  await etape(3, async () => {
    await expect(modale.getByRole('button', { name: "Créer l'avoir" })).toBeVisible();
  });
});
