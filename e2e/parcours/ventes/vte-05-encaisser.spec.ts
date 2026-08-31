import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * L'encaissement clôt la vente : c'est lui qui décrémente le stock, alimente la caisse et
 * rend le ticket imprimable. Tant qu'il n'a pas eu lieu, rien n'est acquis — le panier peut
 * encore être modifié ou abandonné.
 *
 * Parcours ÉCRIVANT dans la base : il enregistre une vente réelle. Comme toute campagne part
 * de l'instantané de référence, ces ventes ne s'accumulent pas d'une édition à l'autre.
 */
scenario('VTE-05', async ({ etape, page }) => {
  const produit = 'PARACETAMOL 1G';
  const lignes = page.locator('tbody tr').filter({ visible: true });

  // Mise en place hors étapes : une caisse ouverte (VTE-33) et un panier servi (VTE-01).
  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  await page.goto('/sales-home');
  await chercherProduit(page, produit);
  await ajouterAuPanier(page, '1');
  await expect(lignes.first()).toContainText(produit);

  await etape(1, async () => {
    // Le montant remis en espèces, saisi dans le mode de règlement correspondant. Deux modes
    // au maximum peuvent être combinés — un seul suffit ici.
    await page.locator('#CASH').fill('2000');
    // La monnaie à rendre est calculée sans qu'on la demande : 2 000 − 1 085.
    await expect(page.locator('#main-content')).toContainText(/MONNAIE/i);
    await expect(page.locator('#main-content')).toContainText(/915/);
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Finaliser' }).click();
    // La vente enregistrée, l'écran revient au panier vide, prêt pour le client suivant :
    // c'est le seul retour visible d'un encaissement réussi au comptoir.
    await expect(lignes.filter({ hasText: produit })).toHaveCount(0);
    await expect(page.locator('#main-content')).toContainText(/Panier vide|Ajoutez des produits/i);
  });
});
