import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un retour fournisseur se négocie par carton, pas par boîte : on rassemble tout ce qui
 * repart chez le même grossiste et on lui envoie un seul bon. Le retour groupé fait cela
 * depuis la liste des périmés — les lots cochés, un motif COMMUN, et un avoir estimé qui se
 * met à jour ligne à ligne.
 *
 * Chaque ligne reste ajustable : la quantité se réduit — le grossiste peut n'en reprendre
 * qu'une partie — et une ligne se décoche si elle ne le concerne pas. L'avoir estimé au bas
 * du tableau est ce qu'on peut espérer récupérer ; c'est ce chiffre qui décide si le retour
 * vaut la peine, face à une destruction pure et simple.
 *
 * Parcours en LECTURE : il prépare le retour groupé sans l'émettre.
 */
scenario('STK-38', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/gestion-peremption');
    await expect(lignes.first()).toBeVisible();
    // Deux lots mono-emplacement : le groupé n'a pas à choisir d'emplacement pour eux.
    const candidats = lignes.filter({ has: page.locator('.pharma-badge-info .pi-map-marker') });
    await candidats.nth(0).locator('input[type="checkbox"]').first().check();
    await candidats.nth(1).locator('input[type="checkbox"]').first().check();
    await page.getByRole('button', { name: /Retour fournisseur groupé/ }).click();
    await expect(modale).toBeVisible();
  });

  await etape(2, async () => {
    // Chaque ligne reste ajustable : la quantité se réduit, une ligne se décoche.
    await expect(modale).toContainText(/Qté à retourner/);
    await expect(modale.locator('tbody tr').first()).toBeVisible();
  });

  await etape(3, async () => {
    // Un motif COMMUN : le bon part chez un grossiste pour une même raison.
    await expect(modale).toContainText(/Motif de retour \(commun à tous les lots\)/);
    await modale.locator('ng-select').first().click();
    await page.locator('.ng-option').first().click();
  });

  await etape(4, async () => {
    // L'avoir estimé : ce qu'on récupérerait, et donc ce que la destruction coûterait.
    await expect(modale).toContainText(/Avoir estimé/);
  });

  await etape(5, async () => {
    // Le parcours s'arrête au bouton : émettre les avoirs engage la relation fournisseur.
    await expect(modale.getByRole('button', { name: /Créer les retours/ })).toBeVisible();
  });
});
