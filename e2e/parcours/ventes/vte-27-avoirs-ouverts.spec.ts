import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un avoir est une dette de l'officine envers un client : il a été remboursé en droit d'achat
 * plutôt qu'en espèces. L'onglet « Avoirs clients » recense ceux qui restent DUS — statut
 * ouvert, date d'expiration non atteinte.
 *
 * Chaque ligne porte ce qui compte pour le comptoir : le produit d'origine, le montant, le
 * SOLDE restant — un avoir peut avoir été consommé en partie — et surtout la date
 * d'expiration, calculée à la création selon le délai de validité paramétré.
 *
 * Parcours en LECTURE.
 */
scenario('VTE-27', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/sales-home/gestion');
    await page.getByRole('tab', { name: /Avoirs clients/ }).click();
    await expect(contenu).toContainText('Avoirs clients');
    // Le total en tête dit d'un coup d'œil ce que l'officine doit encore honorer.
    await expect(contenu).toContainText('Nombre total');
    await expect(contenu).toContainText('Montant total');
    await expect(lignes.first()).toBeVisible();
  });

  await etape(2, async () => {
    // Les colonnes disent tout : créé le, EXPIRE LE, produit, montant, solde, client, statut.
    // C'est la date d'expiration qui fait la différence entre un droit et un souvenir.
    await expect(contenu).toContainText('Expire le');
    await expect(contenu).toContainText('Solde');
    await expect(contenu).toContainText('Ouvert');
  });
});
