import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un bon de retour circule : il accompagne le carton chez le grossiste, il sert de preuve si
 * l'avoir tarde, il alimente la comptabilité.
 *
 * D'où deux sorties de nature différente, et l'écran les sépare. Le PDF, imprimé DEPUIS UNE
 * LIGNE, est le bon lui-même — celui qu'on glisse dans le colis. L'export Excel ou CSV, lancé
 * depuis la barre, reprend la SÉLECTION affichée : période, statut, fournisseur, pour
 * rapprocher ce qui est parti de ce qui a été remboursé.
 *
 * Parcours en LECTURE : il montre les deux chemins sans déclencher de téléchargement.
 */
scenario('ACH-59', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Retours fournisseurs/);
    await expect(lignes.first()).toBeVisible();
    // Le bon lui-même, ligne par ligne : c'est le papier qui part avec le carton.
    await expect(
      lignes.first().locator('app-button[ngbtooltip="Imprimer le bon de retour"] button'),
    ).toBeVisible();
  });

  await etape(2, async () => {
    // L'export de la sélection : le chevron ouvre le choix du format.
    await page.getByRole('button', { name: 'Autres actions' }).first().click();
    const menu = page.locator('.dropdown-menu.show');
    await expect(menu.getByRole('button', { name: 'Excel' })).toBeVisible();
    await expect(menu.getByRole('button', { name: 'CSV' })).toBeVisible();
  });
});
