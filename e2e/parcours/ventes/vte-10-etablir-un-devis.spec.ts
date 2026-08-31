import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le devis — la proforma — répond à une demande qui n'est pas une vente : un devis pour une
 * mutuelle, une entreprise, une administration qui veut un prix avant d'engager sa dépense.
 * Il calcule comme une vente, mais ne touche ni le stock ni la caisse.
 *
 * Une différence tient tout l'écran : le CLIENT est obligatoire. Un devis est un document
 * nominatif — sans destinataire, il n'a pas d'objet — et tant qu'il manque, la quantité reste
 * grisée.
 *
 * Parcours ÉCRIVANT dans la base : il laisse une proforma, que VTE-11 à VTE-13 reprendront.
 */
scenario('VTE-10', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const client = 'AKISSI KOUASSI';
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await assurerCaisseOuverte(page);

  await etape(1, async () => {
    await page.goto('/sales-home/devis');
    const recherche = page.getByPlaceholder('Rechercher un client...');
    await expect(recherche).toBeVisible();
    // Recherche par CODE client : un résultat unique est retenu sans passer par la liste.
    await recherche.fill('CLI000145');
    await recherche.press('Enter');
    await expect(contenu).toContainText(client);

    await chercherProduit(page, produit);
    await ajouterAuPanier(page, '2');
    await expect(lignes.first()).toContainText(produit);
    await expect(contenu).toContainText('38 620');
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(contenu).toContainText(/Panier vide|Sélectionnez un client/i);

    // La proforma rejoint son onglet, où on la retrouvera pour l'imprimer ou la convertir.
    await page.goto('/sales-home/gestion');
    await page.getByRole('tab', { name: /Proformas/ }).click();
    await expect(contenu).toContainText('Liste des Proformas');
    await expect(lignes.first()).toContainText('38 620');
  });
});
