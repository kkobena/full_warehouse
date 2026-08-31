import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le client demande cinq boîtes, l'officine n'en sert que trois : il paie les cinq et repart
 * avec un avoir pour les deux manquantes. C'est le cas le plus fréquent d'avoir en officine,
 * et il ne se déclenche par aucun bouton dédié — seulement par l'écart entre la quantité
 * DEMANDÉE et la quantité SERVIE, deux colonnes voisines du panier.
 *
 * D'où l'intérêt de l'illustrer : rien, à l'écran, n'annonce qu'un avoir se prépare avant la
 * fenêtre de confirmation.
 *
 * Parcours ÉCRIVANT dans la base : il enregistre un avoir réel.
 */
scenario('VTE-49', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 250MG';
  const ligne = page.locator('tbody tr').filter({ visible: true }).first();

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  // Mise en place hors étapes : le panier servi, ce que couvre VTE-01.
  await chercherProduit(page, produit);
  await ajouterAuPanier(page, '5');
  await expect(ligne).toContainText(produit);

  await etape(1, async () => {
    // QTÉ.S est la SECONDE cellule éditable de la ligne — la première étant QTÉ.D. Les deux
    // se ressemblent, et c'est précisément ce que l'image doit lever.
    await ligne.locator('app-editable-cell').nth(1).click();
    const champ = ligne.locator('input[type="number"]').first();
    await champ.fill('3');
    await champ.press('Enter');
    // Cinq demandées, trois servies : l'écart est posé, et le total reste celui des cinq —
    // le client paie ce qu'il a commandé.
    await expect(ligne).toContainText('5');
    await expect(ligne).toContainText('3');
  });

  await etape(2, async () => {
    await page.locator('#CASH').fill('130000');
    await page.getByRole('button', { name: 'Finaliser' }).click();
    // L'application ne bascule pas en avoir en silence : elle nomme la cause et demande
    // confirmation. C'est le seul avertissement de tout le parcours.
    const confirmation = page.locator('.modal-content');
    await expect(confirmation).toContainText('Avoir détecté');
    await expect(confirmation).toContainText(/quantité demandée ≠ quantité servie/i);
    await confirmation.getByRole('button', { name: 'Oui' }).click();

    // Un avoir se réclame plus tard : il DOIT porter un nom. L'application enchaîne donc
    // sur la sélection du client — et propose d'en créer un s'il n'existe pas encore. C'est
    // le seul moment de la vente comptant où le client cesse d'être facultatif.
    await expect(confirmation).toContainText('SÉLECTION CLIENT');
    await expect(confirmation).toContainText(/livraison partielle/i);
    const recherche = confirmation.getByPlaceholder('Rechercher un client');
    await recherche.fill('KOUASSI');
    await recherche.press('Enter');
    await expect(confirmation.locator('tbody tr').first()).toContainText(/KOUASSI/i);
    await confirmation.locator('tbody tr').first().dblclick();

    await expect(confirmation).toBeHidden();
    await expect(page.locator('#main-content')).toContainText(/Panier vide|Ajoutez des produits/i);
  });
});
