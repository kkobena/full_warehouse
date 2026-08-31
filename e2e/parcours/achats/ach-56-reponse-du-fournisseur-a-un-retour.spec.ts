import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un retour envoyé n'est pas un retour accepté. Le grossiste arbitre : il reprend tout, une
 * partie, ou refuse — parce que le lot a dépassé le délai contractuel, parce que l'emballage
 * était ouvert, parce que la référence n'est pas la sienne.
 *
 * C'est cet arbitrage qu'on saisit ici, ligne à ligne : la quantité ACCEPTÉE en regard de la
 * quantité renvoyée. Tant que rien n'est saisi, la colonne reste vide — et c'est précisément
 * ce qui distingue, dans la liste, un retour en attente d'un retour tranché.
 *
 * De cette saisie découle l'avoir (ACH-57) : on ne réclame que ce que le fournisseur a
 * reconnu.
 *
 * Parcours en LECTURE : il ouvre la saisie sans enregistrer de réponse.
 */
scenario('ACH-56', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Retours fournisseurs/);
    await expect(lignes.first()).toBeVisible();
    // La saisie n'est offerte que sur un retour dont le fournisseur a été avisé : répondre
    // à sa place sur un bon jamais envoyé n'aurait pas de sens.
    const repondable = lignes
      .filter({ has: page.locator('app-button[ngbtooltip="Saisir la réponse fournisseur"]') })
      .first();
    await expect(repondable).toBeVisible();
    await repondable
      .locator('app-button[ngbtooltip="Saisir la réponse fournisseur"] button')
      .first()
      .click();
    await expect(modale).toBeVisible();
  });

  await etape(2, async () => {
    // Les deux colonnes qui portent tout l'arbitrage, côte à côte.
    await expect(modale).toContainText('Qté retournée');
    await expect(modale).toContainText('Qté acceptée');
    // Et l'aboutissement : l'avoir se constitue de ce que le fournisseur a reconnu.
    await expect(modale.getByRole('button', { name: "Créer l'avoir" })).toBeVisible();
  });
});
