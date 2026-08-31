import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le stock informatique et le stock physique divergent : une réception mal saisie, un retour
 * non enregistré, et l'écran annonce zéro là où le rayon a encore des boîtes. Refuser la vente
 * serait absurde — le client a le produit en main.
 *
 * L'application refuse d'abord (« Stock insuffisant »), puis PROPOSE de forcer. Le forçage
 * n'est offert qu'aux utilisateurs qui en ont le privilège, et il laisse une trace : le stock
 * passera en négatif, ce qui rendra l'écart visible à l'inventaire au lieu de le dissimuler.
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
    await ajouterAuPanier(page, '500');
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
