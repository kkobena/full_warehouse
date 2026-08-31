import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La pré-vente est un panier qui attend : le client fait préparer sa commande, repasse plus
 * tard, et rien n'a bougé entre-temps — ni le stock, ni la caisse. C'est ce qui la distingue
 * d'une vente en attente, laquelle appartient à la journée en cours du caissier.
 *
 * L'écran est celui de la vente, à deux différences près : pas d'encaissement, et un bouton
 * « Enregistrer » à la place. La pré-vente rejoint alors l'onglet « Pré-ventes » de l'espace
 * de gestion, où on la retrouvera.
 *
 * Parcours ÉCRIVANT dans la base : il laisse une pré-vente, que VTE-09 transformera.
 */
scenario('VTE-07', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await assurerCaisseOuverte(page);

  await etape(1, async () => {
    await page.goto('/sales-home/prevente');
    await expect(page.locator('#main-content')).toContainText('Gestion des pré-ventes');
    await chercherProduit(page, produit);
    await ajouterAuPanier(page, '2');
    await expect(lignes.first()).toContainText(produit);
    // Aucun pavé d'encaissement : la pré-vente ne touche pas la caisse. Le total est là pour
    // renseigner le client, pas pour être encaissé.
    await expect(page.locator('#main-content')).toContainText('38 620');
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    // L'écran se vide, prêt pour la préparation suivante.
    await expect(page.locator('#main-content')).toContainText(/Panier vide|Ajoutez des produits/i);

    // Et la pré-vente est bien rangée là où on ira la chercher.
    await page.goto('/sales-home/gestion');
    await page.getByRole('tab', { name: /Pré-ventes/ }).click();
    await expect(page.locator('#main-content')).toContainText('Pré-ventes');
    await expect(lignes.first()).toContainText('38 620');
  });
});
