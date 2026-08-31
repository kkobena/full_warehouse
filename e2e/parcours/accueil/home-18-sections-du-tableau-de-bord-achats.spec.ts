import { expect } from '@playwright/test';
import { IDENTIFIANTS } from '../../src/config';
import { scenario } from '../../src/scenario';

/**
 * Le module Achats réunit cinq métiers qui ne se confondent pas : piloter, commander, répartir
 * entre dépôts, renvoyer au fournisseur, saisir un bon d'entrée divers. Chaque section
 * n'apparaît qu'aux rôles qui y ont droit.
 *
 * Le point important n'est pas le menu mais ce qu'il y a derrière : un droit refusé ne se
 * contourne pas en tapant l'adresse. Le parcours le vérifie sur deux profils — l'administrateur,
 * qui dispose des cinq sections, puis MARIAM DIALLO, vendeuse, à qui l'adresse elle-même est
 * refusée.
 *
 * C'est la différence entre masquer et interdire.
 */
scenario('HOME-18', async ({ etape, page }) => {
  const sections = page.locator('.pharma-nav-vertical-link');

  await etape(1, async () => {
    await page.goto('/commande');
    await expect(sections.first()).toBeVisible({ timeout: 20000 });
    await expect(sections).toHaveCount(5);
  });

  await etape(2, async () => {
    await page.evaluate(() => localStorage.clear());
    await page.goto('/login');
    await page.locator('[data-cy="username"]').fill('mdiallo');
    await page.locator('[data-cy="password"] input').fill(IDENTIFIANTS.motDePasse);
    await page.locator('[data-cy="submit"]').click();
    await expect(page).not.toHaveURL(/\/login/);

    // L'adresse est saisie directement, sans passer par aucun menu : c'est le contournement
    // qu'un masquage d'affichage laisserait réussir.
    await page.goto('/commande');
    await expect(page.locator('#main-content')).toContainText('403', { timeout: 20000 });
    await expect(sections).toHaveCount(0);
  });
});
