import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Des compteurs présents sur tous les écrans posent une question de coût : les recalculer à
 * chaque navigation ferait payer à la base de données le prix d'une information qui bouge
 * lentement.
 *
 * Le compromis retenu tient en trois temps. Le service partagé charge les huit compteurs une
 * fois, puis les rafraîchit toutes les cinq minutes ; le tableau de bord Pharmacien resserre à
 * deux minutes tant qu'il est affiché ; le serveur, lui, garde son résultat agrégé cinq
 * minutes en cache.
 *
 * Et quand le serveur ne répond pas : les DERNIÈRES valeurs connues restent affichées. Des
 * zéros seraient pire qu'une information un peu ancienne — ils affirmeraient qu'il n'y a plus
 * de péremption à traiter.
 */
scenario('HOME-25', async ({ etape, page }) => {
  const badges = page.locator('.navbar-badge');

  await etape(1, async () => {
    await page.goto('/');
    await expect(badges.first()).toBeVisible({ timeout: 20000 });
  });

  await etape(2, async () => {
    // Les compteurs suivent la navigation : chargés une fois, ils restent affichés d'un écran
    // à l'autre sans nouvel appel.
    const avant = await badges.allInnerTexts();
    await page.goto('/gestion-peremption');
    await expect(badges.first()).toBeVisible();
    expect(await badges.allInnerTexts()).toEqual(avant);
  });

  await etape(3, async () => {
    const avant = await badges.allInnerTexts();
    // Le serveur devient indisponible ; le prochain cycle de rafraîchissement échouera.
    //
    // La navigation reste INTERNE : recharger la page reviendrait à repartir de zéro et ne
    // dirait rien de ce que l'application conserve. C'est bien la session ouverte, avec ses
    // valeurs déjà connues, que l'on soumet à la panne.
    await page.route('**/api/dashboard/alert-counts', route => route.abort());
    await page.getByText('Gestion Stock', { exact: true }).click();
    await page.locator('app-nav-flyout a, app-nav-flyout button').first().click();
    await expect(badges.first()).toBeVisible({ timeout: 20000 });
    // Rien n'est retombé à zéro : l'échec n'efface pas ce que l'on savait.
    expect(await badges.allInnerTexts()).toEqual(avant);
  });
});
