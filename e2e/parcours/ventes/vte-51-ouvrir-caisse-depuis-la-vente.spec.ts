import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseFermee, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le cas du matin : le comptoir sert un client avant que quiconque ait ouvert sa caisse.
 * Plutôt que de refuser l'encaissement et de faire perdre le panier, l'application propose
 * l'ouverture sur place, puis finalise la vente d'elle-même. C'est un détail d'ergonomie
 * qu'aucune capture d'écran ne montre spontanément — il faut être dans la situation.
 *
 * Parcours ÉCRIVANT dans la base : il ouvre la caisse de l'utilisateur et enregistre une
 * vente. Comme toute campagne part de l'instantané de référence, cet état ne s'accumule pas.
 */
scenario('VTE-51', async ({ etape, page }) => {
  const produit = 'PARACETAMOL 1G';
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  // Le scénario n'a de sens que caisse FERMÉE : c'est l'absence de caisse qui déclenche la
  // proposition d'ouverture. D'autres parcours l'ouvrent — on repose donc l'état voulu.
  await assurerCaisseFermee(page);

  await etape(1, async () => {
    await assurerPanierVide(page);
  await page.goto('/sales-home');
    await chercherProduit(page, produit);
    await ajouterAuPanier(page, '1');
    await expect(lignes.first()).toContainText(produit);
  });

  await etape(2, async () => {
    await page.locator('#CASH').fill('2000');
    await page.getByRole('button', { name: 'Finaliser' }).click();
    // L'encaissement ne s'ouvre pas : la caisse manque. L'application le dit et propose de
    // la remédier ici même — c'est ce que la capture doit montrer.
    await expect(modale).toContainText('Ouverture de caisse');
    await expect(modale).toContainText(/fonds de caisse/i);
  });

  await etape(3, async () => {
    const montant = modale.locator('#cashFundAmount');
    // Attendre que le champ ait PRIS LE FOCUS avant de saisir : la modale le fait au bout de
    // 100 ms, en profitant pour y écrire le fonds proposé et le sélectionner. Une saisie
    // antérieure serait purement et simplement remplacée.
    await expect(montant).toBeFocused();
    await montant.fill('50000');
    await modale.getByRole('button', { name: 'Enregistrer' }).click();
    // Le résultat attendu, et le seul qui compte : le panier n'a pas été perdu, la vente
    // s'est finalisée. Un panier vidé ET une caisse ouverte serait un échec silencieux.
    await expect(modale).toBeHidden();
    await expect(lignes.filter({ hasText: produit })).toHaveCount(0);
    await expect(page.locator('#main-content')).toContainText(/Panier vide|Ajoutez des produits/i);
  });
});
