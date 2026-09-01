import { expect } from '@playwright/test';
import {
  ajouterAuPanier,
  assurerCaisseOuverte,
  assurerPanierVide,
  chercherProduit,
  mettreEnAttente,
  ouvrirVentesEnAttente,
  payerEnEspeces,
} from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * L'autre moitié de VTE-14 : ce qui a été mis de côté doit revenir intact. Le point que
 * l'image doit établir, c'est que les lignes sont TOUTES là — une reprise partielle serait
 * pire que pas de mise en attente du tout.
 *
 * Le parcours gare lui-même la vente qu'il reprend, plutôt que de compter sur celle d'un
 * autre : deux parcours ne doivent pas se tenir par la main.
 *
 * Parcours ÉCRIVANT dans la base : il finalise la vente reprise.
 */
scenario('VTE-15', async ({ etape, page }) => {
  const produit = 'PARACETAMOL 1G';
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await assurerCaisseOuverte(page);

  // Mise en place hors étapes : une vente en attente, ce que couvre déjà VTE-14.
  await assurerPanierVide(page);
  await page.goto('/sales-home');
  await chercherProduit(page, produit);
  await ajouterAuPanier(page, '3');
  await expect(lignes.first()).toContainText(produit);
  await mettreEnAttente(page);
  await expect(page.locator('#main-content')).toContainText(/Panier vide|Ajoutez des produits/i);

  await etape(1, async () => {
    await ouvrirVentesEnAttente(page);
    // La liste dit ce qu'elle contient : référence, type, nombre d'articles, montant et
    // vendeur. C'est sur ces colonnes qu'on retrouve la bonne vente.
    await expect(page.locator('app-pending-sales-list')).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Référence' })).toBeVisible();
  });

  await etape(2, async () => {
    // Viser le BOUTON dans la liste, et non la « première ligne » : le tableau range le
    // détail dépliable d'une vente dans une ligne à part, qui ne porte aucune action.
    await page.locator('app-pending-sales-list')
      .getByRole('button', { name: 'Reprendre la vente' }).first().click();
    // Reprise intacte : le produit ET sa quantité. Vérifier la seule présence du produit
    // laisserait passer une reprise qui aurait perdu les quantités.
    await expect(lignes.first()).toContainText(produit);
    await expect(lignes.first()).toContainText('3');
  });

  await etape(3, async () => {
    await payerEnEspeces(page, '5000');
    await page.getByRole('button', { name: 'Finaliser' }).click();
    await expect(page.locator('#main-content')).toContainText(/Panier vide|Ajoutez des produits/i);
  });
});
