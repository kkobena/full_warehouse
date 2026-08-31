import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, payerEnEspeces } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le client revient chercher sa commande : la pré-vente devient une vente. La transformation
 * reprend les lignes telles quelles et bascule sur l'écran de vente, où tout ce qui n'avait
 * pas eu lieu se produit enfin — décompte du stock et encaissement.
 *
 * C'est le moment exact où une pré-vente cesse d'être gratuite : jusque-là elle ne coûtait
 * rien, ni en stock ni en caisse.
 *
 * Parcours ÉCRIVANT dans la base : il crée sa pré-vente, la transforme et l'encaisse.
 */
scenario('VTE-09', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');
  const contenu = page.locator('#main-content');

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  // ── Mise en scène : la pré-vente à transformer. ─────────────────────────────────────────
  await page.goto('/sales-home/prevente');
  await chercherProduit(page, produit);
  await ajouterAuPanier(page, '2');
  await page.getByRole('button', { name: 'Enregistrer' }).click();
  await expect(contenu).toContainText(/Panier vide|Ajoutez des produits/i);

  await etape(1, async () => {
    await page.goto('/sales-home/gestion');
    await page.getByRole('tab', { name: /Pré-ventes/ }).click();
    await expect(lignes.first()).toBeVisible();
    await expect(lignes.first()).toContainText('38 620');
  });

  await etape(2, async () => {
    await lignes.first().getByRole('button', { name: 'Transformer en vente' }).click();
    // La confirmation dit ce qui va changer : la pré-vente sera FINALISÉE, donc décomptée.
    await expect(modale).toContainText('La pré-vente va être finalisée');
    await modale.getByRole('button', { name: 'Oui' }).click();

    // L'écran de vente reprend la main, panier constitué, prêt à encaisser.
    await expect(page).toHaveURL(/sales-home/);
    await expect(lignes.first()).toContainText(produit);
    await expect(contenu).toContainText('38 620');
  });

  await etape(3, async () => {
    await payerEnEspeces(page, '40000');
    await page.getByRole('button', { name: 'Finaliser' }).click();
    await expect(contenu).toContainText(/Panier vide|Ajoutez des produits/i);
  });
});
