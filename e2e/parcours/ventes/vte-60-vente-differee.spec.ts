import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, payerEnEspeces } from '../../src/actions';
import { lire } from '../../src/base-de-donnees';
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
  const client = 'KOUASSI';
  const modale = page.locator('.modal-content');
  const contenu = page.locator('#main-content');

  // Le produit est choisi EN BASE. Figé, il s'épuise : la démo a laissé DOLIPRANE 500 mg à une
  // seule unité alors que le parcours en demande deux, et il s'arrêtait sur « la quantité saisie
  // dépasse le stock » — loin de ce qu'il documente. On exige donc du stock, un prix multiple de
  // cinq (l'arrondi des espèces ne doit pas déplacer les montants annoncés) et un libellé sans
  // virgule ni tiret, que la recherche du comptoir ne tolère pas.
  const sujet = lire(`
    SELECT p.libelle || '|' || p.regular_unit_price
      FROM stock_produit sp
      JOIN produit p ON p.id = sp.produit_id
      JOIN storage s ON s.id = sp.storage_id AND s.storage_type = 'PRINCIPAL'
      JOIN magasin m ON m.id = s.magasin_id AND m.type_magasin = 'OFFICINE'
     WHERE p.status = 'ENABLE'
       AND sp.qty_stock >= 8
       AND p.regular_unit_price >= 2000
       AND MOD(p.regular_unit_price, 5) = 0
       AND p.libelle NOT LIKE '%,%' AND p.libelle NOT LIKE '%-%'
     ORDER BY sp.qty_stock DESC, p.id
     LIMIT 1`);
  expect(sujet, 'la démo doit porter un article en stock au prix rond').toContain('|');
  const [produit, prixBrut] = sujet.split('|');
  const prix = Number(prixBrut);
  const total = prix * 2;
  // Ce que le client peut régler aujourd'hui : la moitié arrondie au millier, donc strictement
  // comprise entre zéro et le dû — c'est l'écart, et lui seul, qui déclenche le différé.
  const verse = Math.floor(total / 2 / 1000) * 1000;
  const reste = total - verse;
  expect(verse, 'le versé doit être strictement inférieur au dû').toBeGreaterThan(0);
  expect(verse).toBeLessThan(total);

  /** Le montant tel que l'écran l'écrit, quel que soit l'espace insécable de la locale. */
  const montant = (valeur: number): RegExp =>
    new RegExp(String(valeur).replace(/\B(?=(\d{3})+(?!\d))/g, '~').split('~').join('[ \u00a0\u202f]'));

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    await chercherProduit(page, produit);
    await ajouterAuPanier(page, '2');
    await expect(contenu).toContainText(montant(total));

    // Saisie touche à touche — le champ est formaté, et `fill()` sur un champ déjà renseigné
    // concatène.
    await payerEnEspeces(page, String(verse));
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Finaliser' }).click();
    // L'écran ne se contente pas de demander confirmation : il rappelle les trois montants,
    // et c'est le reste à payer que le client doit entendre avant de partir.
    await expect(modale).toContainText('Vente différée');
    await expect(modale).toContainText(montant(total));
    await expect(modale).toContainText(montant(verse));
    await expect(modale).toContainText(montant(reste));
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
