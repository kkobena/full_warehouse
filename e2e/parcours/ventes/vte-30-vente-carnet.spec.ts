import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le carnet est traité comme un organisme payeur, à ceci près qu'il couvre la TOTALITÉ de
 * l'achat — c'est la pratique courante : l'employeur ou la société partenaire règle tout,
 * et le porteur rembourse ensuite. L'écran le montre sans détour : TOTAL ASSURANCE égale le
 * total de la vente, et « À ENCAISSER » reste à zéro. Rien ne passe donc par la caisse.
 *
 * Ce zéro commande le geste de finalisation. Le champ de recherche produit, vide une fois le
 * panier constitué, répond à la touche Entrée (`onProductSearchEnter` dans
 * `sale-carnet.component.ts`) : reste-t-il quelque chose à encaisser, la touche envoie le
 * focus dans le champ du règlement, où Entrée validera le paiement ; ne reste-t-il rien —
 * le cas du carnet —, elle demande directement la finalisation. Le caissier finit donc la
 * vente sans quitter le clavier, et le pavé de règlement n'est jamais sollicité.
 *
 * Parcours ÉCRIVANT dans la base : il enregistre une vente portée au compte du carnet.
 */
scenario('VTE-30', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const matricule = 'CAR01-000045';
  const modale = page.locator('.modal-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    await page.getByRole('tab', { name: /Carnet/ }).click();
    const recherche = page.getByPlaceholder('Rechercher un client assuré');
    await recherche.fill('SANGARE');
    await recherche.press('Enter');
    // La liste s'intitule « CLIENTS CARNET » : ce ne sont pas les mêmes clients que la vente
    // assurance, et c'est le carnet qui les qualifie.
    await expect(modale).toContainText('CLIENTS CARNET');
    await modale.locator('tbody tr').filter({ hasText: matricule }).first().dblclick();
    await expect(modale).toBeHidden();
    await expect(page.locator('#main-content')).toContainText(matricule);
    await expect(page.locator('#main-content')).toContainText(/CARNET/);
  });

  await etape(2, async () => {
    await chercherProduit(page, produit);
    await ajouterAuPanier(page, '2');
    await expect(lignes.first()).toContainText(produit);
    await expect(page.locator('#main-content')).toContainText('38 620');
  });

  await etape(3, async () => {
    // Le carnet prend les 38 620 en entier ; il ne reste rien à encaisser. C'est ce que le
    // caissier doit vérifier avant de finaliser — un montant à encaisser non nul sur un
    // carnet signalerait un taux mal renseigné sur le contrat du porteur.
    const contenu = page.locator('#main-content');
    // « Total assurance » est mis en capitales par la feuille de style : c'est le texte du
    // DOM qu'il faut viser, pas ce que l'écran donne à lire.
    //
    // Les deux MONTANTS, et pas seulement les libellés : « contient 100% » était vrai sur
    // n'importe quel écran portant le taux du contrat, et laissait donc passer une vente où
    // le carnet n'aurait couvert qu'une part. Ce que le scénario promet, c'est que le carnet
    // prend le total et qu'il ne reste rien au comptoir.
    const texte = (await contenu.innerText()).replace(/\s+/g, ' ');
    expect(texte).toMatch(/Total assurance 38\s?620/i);
    expect(texte).toMatch(/À ENCAISSER 0(?!\d)/i);

    // Le geste réel du comptoir : le champ de recherche produit est vide, Entrée y demande
    // la finalisation. `focus()` plutôt qu'un clic — cliquer déplierait la liste du
    // ng-select, et Entrée y vaudrait choix d'une suggestion.
    const recherche = page.locator('#produitbox');
    await recherche.focus();
    await recherche.press('Enter');
    await expect(modale).toContainText('Voulez-vous vraiment finaliser la vente');
  });

  // ── Hors capture : la réponse à la question. L'image de l'étape 3 doit montrer le carnet à
  //    100 %, l'encaissement à zéro et la confirmation appelée par Entrée — répondre avant la
  //    prise donnerait un écran déjà vidé. ──────────────────────────────────────────────────
  await modale.getByRole('button', { name: 'Oui' }).click();
  await expect(page.locator('#main-content')).toContainText(/Panier vide|Sélectionnez un client/i);
});
