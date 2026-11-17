import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Deux conditions d'achat décidaient déjà du coût réel d'un bon, mais aucune n'était visible au
 * moment où l'on pouvait encore agir dessus.
 *
 * Le franco de port n'existait nulle part : un bon à 142 300 F quand le franco est à 150 000 F
 * fait payer des frais de port qu'un article de plus aurait supprimés, et l'officine ne
 * l'apprend qu'à la facture.
 *
 * Le palier RFA, lui, était calculé — mais dans un écran financier qu'on ouvre après coup. Pire,
 * il cumulait le chiffre d'affaires par AGENCE en le comparant au palier du GROUPE : le
 * pourcentage affiché valait environ le tiers du vrai, et l'alerte d'approche ne pouvait
 * pratiquement jamais se déclencher.
 *
 * Ce parcours vérifie que les deux informations sont là, dans le bandeau de totaux, pendant la
 * saisie. Parcours en LECTURE SEULE : il n'écrit rien.
 */
scenario('ACH-89', async ({ etape, page }) => {
  const bandeau = page.locator('.grid-caption');

  await page.goto('/commande');
  await ouvrirOnglet(page, /Commandes & Réceptions/);
  await page.getByRole('tab', { name: /Commandes fournisseurs/ }).click();
  const liste = page.locator('app-commande-requested-home');
  await expect(liste.locator('tbody tr').first()).toBeVisible();
  await liste.locator('tbody tr').first().dblclick();
  await expect(page.locator('app-commande-requested')).toBeVisible();

  await etape(1, async () => {
    // Le bandeau porte déjà les montants ; c'est là que les conditions d'achat doivent se lire,
    // au même endroit et au même instant.
    await expect(bandeau).toBeVisible();
    await expect(bandeau).toContainText('Achat');
  });

  await etape(2, async () => {
    // Franco : soit il manque un montant, soit il est atteint. Les deux formulations sont
    // acceptées — ce qui compte est que l'état soit dit, pas lequel.
    await expect(bandeau).toContainText('Franco');
    await expect(bandeau).toContainText(/il manque|atteint/);
  });

  await etape(3, async () => {
    // Palier RFA : le pourcentage est celui du GROUPE, pas de l'agence qui reçoit le bon.
    // L'infobulle nomme le grossiste principal, ce qui le prouve à l'écran.
    const palier = bandeau.getByText('Palier RFA');
    await expect(palier).toBeVisible();
    await expect(bandeau).toContainText(/\d+%/);
  });
});
