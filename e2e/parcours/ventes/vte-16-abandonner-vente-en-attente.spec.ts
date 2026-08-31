import { expect } from '@playwright/test';
import {
  ajouterAuPanier,
  assurerPanierVide,
  chercherProduit,
  mettreEnAttente,
  ouvrirVentesEnAttente,
} from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Scénario CORRIGÉ dans le modèle : il annonçait un bouton « Supprimer » dans la liste des
 * ventes en attente. Il n'y en a pas — et c'est la première chose qu'un utilisateur y
 * cherche. Le chemin réel passe par la reprise, puis l'annulation depuis l'écran de vente.
 * Un manuel qui promettrait la suppression directe enverrait chercher un bouton absent.
 */
scenario('VTE-16', async ({ etape, page }) => {
  const produit = 'PARACETAMOL 1G';
  const lignes = page.locator('tbody tr').filter({ visible: true });

  // Mise en place hors étapes : une vente en attente à abandonner (VTE-14).
  await assurerPanierVide(page);
  await page.goto('/sales-home');
  await chercherProduit(page, produit);
  await ajouterAuPanier(page, '1');
  await expect(lignes.first()).toContainText(produit);
  await mettreEnAttente(page);
  await expect(page.locator('#main-content')).toContainText(/Panier vide|Ajoutez des produits/i);

  const liste = page.locator('app-pending-sales-list');

  await etape(1, async () => {
    await ouvrirVentesEnAttente(page);
    await expect(liste).toBeVisible();
    await expect(liste.locator('tbody tr').first()).toBeVisible();
  });

  await etape(2, async () => {
    // Viser le BOUTON dans la liste, et non la « première ligne » : le tableau range le
    // détail dépliable d'une vente dans une ligne à part, qui ne porte aucune action.
    await liste.getByRole('button', { name: 'Reprendre la vente' }).first().click();
    await expect(lignes.first()).toContainText(produit);
  });

  await etape(3, async () => {
    await page.getByRole('button', { name: 'Annuler' }).click();
    const confirmation = page.locator('.modal-content');
    // La confirmation prévient de ce qui va être perdu : c'est ce qui distingue l'abandon
    // d'une mise en attente.
    await expect(confirmation).toContainText(/Annulation de la vente/i);
    await confirmation.getByRole('button', { name: 'Oui' }).click();
    await expect(page.locator('#main-content')).toContainText(/Panier vide|Ajoutez des produits/i);
  });
});
