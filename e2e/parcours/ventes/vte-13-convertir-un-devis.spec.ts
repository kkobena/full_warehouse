import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, payerEnEspeces } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le devis est accepté : il devient une vente. La conversion reprend les lignes telles
 * quelles et bascule sur l'écran de vente — c'est là, et seulement là, que le stock se
 * décompte et que la caisse encaisse.
 *
 * Le geste est le même que pour une pré-vente, et pour la même raison : un document qui
 * n'engageait rien devient une opération qui engage tout.
 *
 * Parcours ÉCRIVANT dans la base : il crée son devis, le convertit et l'encaisse.
 */
scenario('VTE-13', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  // ── Mise en scène : le devis accepté. ───────────────────────────────────────────────────
  await page.goto('/sales-home/devis');
  const recherche = page.getByPlaceholder('Rechercher un client...');
  await expect(recherche).toBeVisible();
  await recherche.fill('CLI000145');
  await recherche.press('Enter');
  await expect(contenu).toContainText('AKISSI KOUASSI');
  await chercherProduit(page, produit);
  await ajouterAuPanier(page, '2');
  await page.getByRole('button', { name: 'Enregistrer' }).click();
  await expect(contenu).toContainText(/Panier vide|Sélectionnez un client/i);

  await etape(1, async () => {
    await page.goto('/sales-home/gestion');
    await page.getByRole('tab', { name: /Proformas/ }).click();
    await expect(lignes.first()).toBeVisible();
    await expect(lignes.first()).toContainText('38 620');
  });

  await etape(2, async () => {
    await lignes.first().getByRole('button', { name: 'Transformer en vente' }).click();
    await expect(modale).toBeVisible();
    await modale.getByRole('button', { name: 'Oui' }).click();

    // L'écran de vente reprend la main avec les lignes du devis, prêtes à encaisser.
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
