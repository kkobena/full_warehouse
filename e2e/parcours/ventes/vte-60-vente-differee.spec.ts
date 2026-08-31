import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, payerEnEspeces } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le différé ne se CHOISIT pas — il se propose. Il n'existe ni bouton ni mode de règlement
 * « différé » : c'est l'écart entre le dû et le versé qui déclenche la proposition, au moment
 * de valider. Un montant versé nul suit exactement le même chemin.
 *
 * Trois fenêtres s'enchaînent, et chacune ferme une porte de sortie :
 *   1. le rappel du reste à payer, qui demande confirmation ;
 *   2. la désignation du CLIENT — sans lui, personne ne porte la créance ;
 *   3. le motif, obligatoire, qui restera attaché à la vente.
 *
 * Parcours ÉCRIVANT dans la base : il enregistre une vente et ouvre une créance client.
 */
scenario('VTE-60', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const client = 'KOUASSI';
  const modale = page.locator('.modal-content');
  const contenu = page.locator('#main-content');

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    await chercherProduit(page, produit);
    await ajouterAuPanier(page, '2');
    await expect(contenu).toContainText('38 620');

    // 10 000 sur 38 620 : c'est ce que le client peut régler aujourd'hui. Saisie touche à
    // touche — le champ est formaté, et `fill()` sur un champ déjà renseigné concatène.
await payerEnEspeces(page, '10000');
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Finaliser' }).click();
    // L'écran ne se contente pas de demander confirmation : il rappelle les trois montants,
    // et c'est le reste à payer que le client doit entendre avant de partir.
    await expect(modale).toContainText('Vente différée');
    await expect(modale).toContainText('38 620');
    await expect(modale).toContainText('10 000');
    await expect(modale).toContainText('28 620');
  });

  await etape(3, async () => {
    await modale.getByRole('button', { name: 'Oui' }).click();
    // Une créance sans débiteur n'existe pas : l'application impose de désigner le client,
    // et propose de le créer sur place s'il n'est pas encore connu.
    await expect(modale).toContainText('SÉLECTION CLIENT - Vente différée');
    const recherche = modale.getByPlaceholder(/Rechercher/i).first();
    await recherche.fill(client);
    await recherche.press('Enter');
    await modale.locator('tbody tr').filter({ hasText: client }).first().dblclick();
    await expect(modale).toBeHidden();
  });

  await etape(4, async () => {
    // Le commentaire est OBLIGATOIRE : c'est lui qui expliquera la créance au moment du
    // recouvrement, quand plus personne ne se souviendra de la scène du comptoir.
    const commentaire = page.getByPlaceholder('Saisir un commentaire');
    await expect(commentaire).toBeVisible();
    await commentaire.fill('Client habituel — solde réglé à la prochaine visite');

    await page.getByRole('button', { name: 'Finaliser' }).click();
    await expect(contenu).toContainText(/Panier vide|Aucun produit dans la vente/i);
  });
});
