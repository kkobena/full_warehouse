import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, payerEnEspeces } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le client demande six boîtes, le rayon n'en a que quatre : il paie les six et repart avec un
 * avoir pour les deux manquantes. C'est le cas le plus fréquent d'avoir en officine, et il ne
 * se déclenche par aucun bouton dédié — seulement par l'écart entre la quantité DEMANDÉE et la
 * quantité SERVIE, que le panier affiche « servie / demandée » dès qu'elles diffèrent.
 *
 * L'écart naît du dialogue « Stock insuffisant » : « Le client sera livré plus tard » garde la
 * quantité demandée et ne sert que ce que le stock couvre. Rien, à l'écran, n'annonce qu'un
 * avoir se prépare avant la fenêtre de confirmation.
 *
 * Parcours ÉCRIVANT dans la base : il enregistre un avoir réel.
 */
scenario('VTE-49', async ({ etape, page }) => {
  // Quatre boîtes en rayon dans la démonstration, et aucune réserve : le client en demande six.
  const produit = 'COTON HYDROPHILE 100G';
  const ligne = page.locator('tbody tr').filter({ visible: true }).first();
  const modale = page.locator('.modal-content');

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    await chercherProduit(page, produit);
    await ajouterAuPanier(page, '6');
    // Le stock ne couvre pas la demande : l'écran demande ce qui se passe au comptoir.
    await expect(modale).toContainText('Stock insuffisant');
    await modale.getByRole('button', { name: /livré plus tard/ }).click();
    await expect(modale).toBeHidden();
    // Six demandées, quatre servies : l'écart est posé, et le total reste celui des six —
    // le client paie ce qu'il a commandé.
    await expect(ligne).toContainText(produit);
    await expect(ligne).toContainText(/4\s*\/\s*6/);
  });

  await etape(2, async () => {
    // `payerEnEspeces` plutôt qu'un `fill` direct : le champ se reformate à chaque frappe, et
    // un `fill` sur un champ déjà renseigné y CONCATÈNE la nouvelle valeur au lieu de la
    // remplacer. Le helper vide, retape et vérifie que la somme est bien posée.
    await payerEnEspeces(page, '7000');
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
