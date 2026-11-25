import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le circuit d'avoir client était complet dans un sens et muet dans l'autre.
 *
 * Quand un produit manque au comptoir, la promesse faite au client est enregistrée, et la
 * réception d'une livraison rapproche automatiquement l'avoir du produit reçu, puis prévient le
 * client. Mais rien ne signalait un produit promis que personne n'avait commandé : le mécanisme
 * exploitait la réception sans jamais déclencher la commande. Le client attendait un appel qui
 * ne pouvait pas venir.
 *
 * Parcours en LECTURE SEULE : il n'écrit rien.
 */
scenario('ACH-90', async ({ etape, page }) => {
  await page.goto('/commande');
  await ouvrirOnglet(page, /Commandes & Réceptions/);

  const ongletDus = page.getByRole('tab', { name: /Dus aux clients/ });

  await etape(1, async () => {
    // La pastille doit se lire AVANT d'ouvrir l'onglet : une promesse non tenue qu'il faut
    // aller chercher ne vaut guère mieux qu'une promesse invisible.
    await expect(ongletDus).toBeVisible();
  });

  await etape(2, async () => {
    await ongletDus.click();
    await expect(page.locator('app-produits-dus')).toBeVisible();
  });

  await etape(3, async () => {
    const tableau = page.locator('app-produits-dus table');
    await expect(tableau).toBeVisible();

    // Le jeu de démonstration garantit au moins un produit dû non commandé : un avoir ouvert
    // sur trois reste délibérément non rattaché, et un garde-fou le vérifie au chargement.
    const premiereLigne = tableau.locator('tbody tr').first();
    await expect(premiereLigne).toBeVisible();

    // Les trois chiffres qui font décider : qui attend, combien, et depuis quand.
    await expect(premiereLigne.locator('td').nth(1)).toHaveText(/\d+/);
    await expect(premiereLigne.locator('td').nth(2)).toHaveText(/\d+/);
    await expect(premiereLigne.getByText(/\d+ j/)).toBeVisible();

    // Le résumé compte les mêmes produits que la liste. Il vit dans le bandeau `grid-caption`
    // des écrans voisins, dont les libellés sont capitalisés (« Unités dues ») : la casse n'est
    // pas ce que l'on vérifie ici.
    await expect(page.locator('app-produits-dus .grid-caption')).toContainText(/unités dues/i);
  });
});
