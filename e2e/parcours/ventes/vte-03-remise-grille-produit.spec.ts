import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Scénario CORRIGÉ dans le modèle en écrivant ce parcours. Il annonçait deux dispositifs
 * cumulables — remise produit ET remise client. La remise client existe encore dans le modèle
 * de données et son écran d'administration, mais PLUS RIEN NE LA LIT au calcul d'une vente :
 * seule la grille produit s'applique. Le manuel ne peut pas promettre l'autre.
 *
 * Le point que l'image doit faire comprendre : la remise se choisit UNE FOIS sur la vente,
 * puis chaque ligne reçoit le taux de SON code produit — 7 % ici, 10 % ailleurs.
 *
 * Parcours ÉCRIVANT dans la base : il enregistre une vente réelle.
 */
scenario('VTE-03', async ({ etape, page }) => {

  const produit = 'ATORVASTATINE 100MG';
  const ligne = page.locator('tbody tr').filter({ visible: true }).first();

  await assurerCaisseOuverte(page);

  await etape(1, async () => {
    await assurerPanierVide(page);
  await page.goto('/sales-home');
    await chercherProduit(page, produit);
    await ajouterAuPanier(page, '2');
    // Plein tarif à ce stade : 2 × 660. C'est cette valeur que l'étape suivante doit faire
    // bouger — sans elle, on ne saurait pas dire que la remise a fait quelque chose.
    await expect(ligne).toContainText('1 320');
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Remise' }).click();
    const choix = page.locator('ngb-popover-window').first();
    await expect(choix).toContainText('Choisir une remise');
    await choix.getByText('Grille de remise officine').first().click();
    // La grille est attachée à la vente : le bandeau affiche SON TAUX. Attendre le seul mot
    // « Remise : » ne suffit pas — l'étiquette est là avant même qu'une grille soit choisie,
    // et l'étape suivante partait alors sur une vente sans remise.
    await expect(page.locator('#main-content')).toContainText(/Remise\s*:\s*\d+\s*%/);
  });

  await etape(3, async () => {
    // La ligne garde son montant BRUT — quantité × prix unitaire — et la remise apparaît à
    // part, sous le total : c'est cette séparation que le scénario promet, et c'est elle qui
    // rend le ticket lisible pour le client.
    //
    // Ni le taux ni le montant ne sont gravés ici. Le bandeau annonce le taux de la GRILLE,
    // mais chaque produit peut porter le sien : ATORVASTATINE se voit appliquer 10 % là où
    // le bandeau affiche 15. Un montant figé se périmerait au premier ajustement de la
    // grille de démonstration. On vérifie donc ce qui doit rester vrai : une remise non
    // nulle, et un net à encaisser qui en découle exactement.
    await expect(ligne).toContainText('1 320');
    const bandeau = (await page.locator('#main-content').innerText()).replace(/ | /g, ' ');
    // Le libellé est rendu en CAPITALES sous le total (« REMISE »), alors que le bandeau
    // du haut écrit « Remise : » — d'où la recherche insensible à la casse, sans quoi on
    // tombe sur le bandeau, qui ne porte pas de montant.
    const remise = Number(/REMISE\s*[-\u2212\u2013]\s*([\d ]+)/i.exec(bandeau)?.[1]?.replace(/ /g, '') ?? 0);
    const aEncaisser = Number(/ENCAISSER\s*([\d ]+)/i.exec(bandeau)?.[1]?.replace(/ /g, '') ?? 0);
    expect(remise).toBeGreaterThan(0);
    // Le net n'est pas exactement la soustraction : il est arrondi à la dizaine, comme le
    // veut la monnaie en circulation — 1 320 remisés de 199 donnent 1 120 à encaisser, et
    // non 1 121. C'est la caisse qui a raison : on ne rend pas une pièce d'un franc.
    expect(Math.abs(aEncaisser - (1320 - remise))).toBeLessThanOrEqual(10);
  });

  // ── L'encaissement suit, hors étape : sa capture montrerait un panier vide, alors que la
  //    légende parle du montant remisé. ──────────────────────────────────────────────────
  await page.locator('#CASH').fill('2000');
  await page.getByRole('button', { name: 'Finaliser' }).click();
  await expect(page.locator('#main-content')).toContainText(/Panier vide|Ajoutez des produits/i);
});
