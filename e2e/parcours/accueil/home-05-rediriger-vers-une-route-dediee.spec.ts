import { expect } from '@playwright/test';
import { IDENTIFIANTS } from '../../src/config';
import { scenario } from '../../src/scenario';

/**
 * Certains profils n'ont que faire d'un tableau de bord : un vendeur ouvre sa session pour
 * servir un patient, pas pour lire des indicateurs. Son « tableau de bord » est donc configuré
 * en mode ROUTE — au lieu d'afficher un bloc sur l'accueil, l'application l'envoie directement
 * à l'écran de prévente.
 *
 * C'est la même mécanique de résolution que HOME-01 : seule la nature de ce qu'elle rend
 * change, une redirection au lieu d'un composant.
 *
 * MARIAM DIALLO n'est que vendeuse ; les comptes de la démonstration partagent le mot de passe
 * de l'administrateur.
 */
scenario('HOME-05', async ({ etape, page }) => {
  await etape(1, async () => {
    await page.goto('/login');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/login');
    await page.locator('[data-cy="username"]').fill('mdiallo');
    await page.locator('[data-cy="password"] input').fill(IDENTIFIANTS.motDePasse);
    await page.locator('[data-cy="submit"]').click();

    // Aucun clic dans le menu : la redirection est le résultat de la résolution.
    await expect(page).toHaveURL(/\/sales-home\/prevente/, { timeout: 20000 });
  });
});
