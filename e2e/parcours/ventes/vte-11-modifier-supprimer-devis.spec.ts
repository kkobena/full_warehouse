import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un devis se négocie : le client demande une remise, ajoute une ligne, renonce à une autre.
 * Tant qu'il n'est pas converti en vente, il reste donc modifiable — et supprimable si
 * l'affaire ne se fait pas. Rien de tout cela ne touche le stock : le devis n'a jamais rien
 * réservé.
 *
 * Parcours ÉCRIVANT dans la base : il crée sa propre proforma, la modifie, puis la supprime.
 */
scenario('VTE-11', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const ajout = 'PARACETAMOL 1G';
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  await assurerCaisseOuverte(page);

  // ── Mise en scène : la proforma à retoucher. ────────────────────────────────────────────
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
    await lignes.first().getByRole('button', { name: 'Éditer le devis' }).click();
    // Le devis se rouvre là où il a été écrit, avec son client et ses lignes.
    await expect(contenu).toContainText('AKISSI KOUASSI');
    await expect(lignes.first()).toContainText(produit);
  });

  await etape(2, async () => {
    // Une ligne de plus, réenregistrée : le devis a changé, le stock n'a pas bougé.
    await chercherProduit(page, ajout);
    await ajouterAuPanier(page, '1');
    await expect(contenu).toContainText(ajout);
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(contenu).toContainText(/Panier vide|Sélectionnez un client/i);

    // Puis la suppression, sur la même ligne de la liste.
    await page.goto('/sales-home/gestion');
    await page.getByRole('tab', { name: /Proformas/ }).click();
    await expect(lignes.first()).toBeVisible();
    await lignes.first().getByRole('button', { name: 'Supprimer le devis' }).click();
    await expect(modale).toBeVisible();
    await modale.getByRole('button', { name: 'Oui' }).click();
    await expect(modale).toBeHidden();
  });
});
