import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, lireStockProduit } from '../../src/actions';
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
  const produit = 'PARACETAMOL TEVA 500 mg';
  const lignes = page.locator('tbody tr').filter({ visible: true });

  // Mise en place hors étapes : une caisse ouverte (VTE-33) et un panier servi (VTE-01).
  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  // Le stock d'avant, relevé là où le pharmacien le lit : c'est lui qui fera foi à la fin.
  const stockAvant = await lireStockProduit(page, produit);
  await page.goto('/sales-home');
  await chercherProduit(page, produit);
  await ajouterAuPanier(page, '1');
  await expect(lignes.first()).toContainText(produit);

  await etape(1, async () => {
    // Le passage au règlement se fait au clavier : Entrée sur la recherche produit VIDE
    // déplace le curseur au premier mode de paiement. C'est le geste du comptoir — la souris
    // ne sert qu'à ceux qui préfèrent cliquer directement dans le champ.
    const recherche = page.locator('#produitbox');
    await recherche.click();
    await recherche.press('Enter');
    await expect(page.locator('#CASH')).toBeVisible();
  });

  await etape(2, async () => {
    // Le montant remis en espèces, saisi dans le mode de règlement correspondant. Deux modes
    // au maximum peuvent être combinés — un seul suffit ici.
    const remis = 5000;
    await page.locator('#CASH').fill(String(remis));
    await expect(page.locator('#main-content')).toContainText(/MONNAIE/i);

    // La monnaie à rendre est calculée sans qu'on la demande. On la confronte aux deux autres
    // nombres de l'écran plutôt qu'à une valeur écrite en dur : une remise, un changement de
    // prix ou un arrondi de caisse déplacerait le « 915 » attendu sans rien casser d'autre,
    // et le parcours deviendrait faux sans qu'on sache pourquoi.
    const nombre = async (selecteur: string) =>
      Number((await page.locator(selecteur).innerText()).replace(/[^\d-]/g, ''));
    const aEncaisser = await nombre('.amount-to-pay .amount-value');
    await expect.poll(() => nombre('.amount-change .amount-value'), { timeout: 5000 })
      .toBe(remis - aEncaisser);
  });

  await etape(3, async () => {
    // Trois gestes équivalents valident l'encaissement : Entrée depuis le champ du montant,
    // F9, ou ce bouton. On illustre le bouton, seul geste visible sur une image.
    await page.getByRole('button', { name: 'Finaliser' }).click();
    // La vente enregistrée, l'écran revient au panier vide, prêt pour le client suivant :
    // c'est le seul retour visible d'un encaissement réussi au comptoir.
    await expect(lignes.filter({ hasText: produit })).toHaveCount(0);
    await expect(page.locator('#main-content')).toContainText(/Panier vide|Ajoutez des produits/i);
  });

  // La boucle se ferme ici, et elle seule prouve ce que la documentation de ce parcours
  // annonce depuis toujours : « c'est l'encaissement qui décrémente le stock ». Un panier
  // revenu à vide reste vrai même si rien n'est sorti du stock.
  expect(await lireStockProduit(page, produit)).toBe(stockAvant - 1);
});
