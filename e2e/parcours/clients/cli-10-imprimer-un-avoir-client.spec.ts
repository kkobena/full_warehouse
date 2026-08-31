import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Le client repart avec son bon d'avoir : c'est sa preuve, et le seul document qu'il pourra
 * présenter à sa prochaine visite si le solde ne s'affiche pas comme il l'attend.
 *
 * Le PDF reprend les trois informations qui font foi — le montant, le motif du retour, et la
 * DATE D'EXPIRATION. Cette dernière est la plus importante à écrire : un avoir périmé ne se
 * conteste pas si le client savait quand l'utiliser.
 *
 * L'impression part de la ligne de l'avoir, dans la fiche du client : c'est là qu'on le
 * retrouve des semaines plus tard.
 */
scenario('CLI-10', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/customer');
    await expect(lignes.first()).toBeVisible();
    await lignes.first().locator('app-button[ngbtooltip="Voir détails"] button').first().click();
    await page.getByRole('tab', { name: /Avoirs/ }).first().click();
    await expect(contenu).toContainText(/Solde disponible|Aucun avoir pour ce client/);

  });

  await etape(2, async () => {
    // Le bon PDF vit sur la ligne de l'avoir ; sans avoir, il n'y a rien à imprimer.
    const bouton = page.getByRole('button', { name: "Bon d'avoir PDF" });
    if ((await bouton.count()) > 0) {
      await expect(bouton.first()).toBeVisible();
    } else {
      await expect(contenu).toContainText('Aucun avoir pour ce client');
    }
  });
});
