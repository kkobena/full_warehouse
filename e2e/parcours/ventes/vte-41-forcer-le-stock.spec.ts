import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le rayon est vide et le client veut le produit : plutôt que de le renvoyer, l'officine
 * encaisse aujourd'hui et livrera après le prochain réapprovisionnement.
 *
 * L'application refuse d'abord (« Stock insuffisant »), puis PROPOSE de forcer. Le forçage
 * n'est offert qu'aux utilisateurs qui en ont le privilège, et il ne crée AUCUN stock : la
 * quantité servie reste bornée au disponible, l'écart devient un avoir client — d'où le
 * client obligatoire à l'encaissement. Le stock ne passe pas en négatif : rien ne sort de
 * ce qui n'existe pas.
 *
 * Parcours en LECTURE : il décline le forçage, et le stock n'est pas touché.
 */
scenario('VTE-41', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const modale = page.locator('.modal-content');

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    await chercherProduit(page, produit);
    // Le panneau du produit affiche le stock du rayon : la quantité demandée le dépasse
    // largement, et c'est le serveur qui tranche — pas l'écran.
    await expect(page.locator('#main-content')).toContainText(/Rayon\s*:/);
    // Une quantite volontairement absurde : les produits vedettes du jeu de demonstration
    // sont approvisionnes pour la campagne, et 500 boites ne les depassaient plus. Le
    // parcours ne doit pas dependre du niveau de stock du jour.
    await ajouterAuPanier(page, '9999');
  });

  await etape(2, async () => {
    // Le refus n'est pas un blocage : c'est une question. « La quantité saisie est supérieure
    // à la quantité stock du produit. Voulez-vous continuer ? »
    await expect(modale).toContainText('Forcer le stock');
    await expect(modale).toContainText('supérieure à la quantité stock');
    await expect(modale.getByRole('button', { name: 'Oui' })).toBeVisible();
    await expect(modale.getByRole('button', { name: 'Non' })).toBeVisible();
  });

  // ── Remise en état : on décline. Le panier reste vide, le stock intact. ─────────────────
  await modale.getByRole('button', { name: 'Non' }).click();
  await expect(modale).toBeHidden();
});
