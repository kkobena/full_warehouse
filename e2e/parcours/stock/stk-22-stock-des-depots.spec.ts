import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un dépôt n'est pas un client : c'est un prolongement de l'officine, qui détient de la
 * marchandise sans l'avoir payée. Le stock qu'il porte reste donc suivi — c'est ce que montre
 * cet écran, dépôt par dépôt.
 *
 * Ce stock ne vient pas d'une commande fournisseur mais d'un TRANSFERT depuis l'officine : la
 * « vente dépôt » (VTE-29) dessaisit l'officine et crédite le dépôt, sans encaissement ni
 * chiffre d'affaires. Le retour dépôt fait le chemin inverse.
 *
 * D'où l'importance de cet écran : c'est la seule vue qui dit ce qui dort chez le dépositaire,
 * et donc ce qu'on peut lui reprendre plutôt que de le racheter au grossiste.
 *
 * Parcours en LECTURE.
 */
scenario('STK-22', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/depot');
    await ouvrirOnglet(page, /Produits|Stock/);
    // Le dépôt se choisit : chacun porte son propre stock.
    await expect(contenu).toContainText(/Sélectionner un dépôt|dépôt/i);
  });

  await etape(2, async () => {
    const selecteur = page.locator('#depots');
    await selecteur.focus();
    await selecteur.press('ArrowDown');
    await page.locator('.ng-option').first().click();
    // Le stock détenu par ce dépôt, produit par produit, avec sa valorisation d'achat.
    await expect(contenu).toContainText('Cip');
    await expect(contenu).toContainText(/Prix achat/);
  });

  await etape(3, async () => {
    // Les deux sens du transfert sont à portée : alimenter le dépôt, ou reprendre.
    await expect(page.getByRole('button', { name: /Retour dépôt/ })).toBeVisible();
  });
});
