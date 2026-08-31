import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une pré-vente n'engage rien : ni le stock, ni la caisse. Elle reste donc librement
 * modifiable — le client ajoute un produit au téléphone, retire celui qu'il a trouvé
 * ailleurs — et supprimable si la commande tombe à l'eau.
 *
 * Les deux gestes se prennent sur la ligne, dans l'onglet « Pré-ventes » de l'espace de
 * gestion : éditer rouvre le panier dans l'écran de pré-vente ; supprimer demande
 * confirmation.
 *
 * Parcours ÉCRIVANT dans la base : il crée sa propre pré-vente, la modifie, puis la supprime
 * — il ne laisse donc rien derrière lui.
 */
scenario('VTE-08', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const ajout = 'PARACETAMOL 1G';
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  await assurerCaisseOuverte(page);

  // ── Mise en scène : une pré-vente à modifier. Elle n'est pas photographiée : le scénario
  //    commence à la liste. ────────────────────────────────────────────────────────────────
  await page.goto('/sales-home/prevente');
  await chercherProduit(page, produit);
  await ajouterAuPanier(page, '2');
  await page.getByRole('button', { name: 'Enregistrer' }).click();
  await expect(page.locator('#main-content')).toContainText(/Panier vide|Ajoutez des produits/i);

  await etape(1, async () => {
    await page.goto('/sales-home/gestion');
    await page.getByRole('tab', { name: /Pré-ventes/ }).click();
    await expect(lignes.first()).toBeVisible();
    // Éditer rouvre la pré-vente là où elle a été constituée : l'écran de pré-vente, avec
    // ses lignes.
    await lignes.first().getByRole('button', { name: 'Éditer la pré-vente' }).click();
    await expect(page).toHaveURL(/prevente/);
    await expect(lignes.first()).toContainText(produit);
  });

  await etape(2, async () => {
    // Une ligne de plus, et on réenregistre : le stock n'a toujours pas bougé.
    await chercherProduit(page, ajout);
    await ajouterAuPanier(page, '1');
    await expect(page.locator('#main-content')).toContainText(ajout);
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(page.locator('#main-content')).toContainText(/Panier vide|Ajoutez des produits/i);

    // Et la suppression, sur la même ligne : une confirmation, puis la pré-vente disparaît.
    await page.goto('/sales-home/gestion');
    await page.getByRole('tab', { name: /Pré-ventes/ }).click();
    await expect(lignes.first()).toBeVisible();
    await lignes.first().getByRole('button', { name: 'Supprimer la pré-vente' }).click();
    await expect(modale).toBeVisible();
    await modale.getByRole('button', { name: 'Oui' }).click();
    await expect(modale).toBeHidden();
  });
});
