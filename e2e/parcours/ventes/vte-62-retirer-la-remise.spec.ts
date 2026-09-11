import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le pendant de VTE-03 : une remise se retire aussi facilement qu'elle se pose, tant que la
 * vente n'est pas encaissée. Le geste tient à une croix que personne ne remarque — d'où
 * l'intérêt de l'illustrer plutôt que de l'écrire.
 *
 * Parcours ÉCRIVANT dans la base : il crée une vente en cours, qu'il abandonne à la fin.
 */
scenario('VTE-62', async ({ etape, page }) => {
  const produit = 'ATORVASTATINE 100MG';
  const ligne = page.locator('tbody tr').filter({ visible: true }).first();
  const contenu = page.locator('#main-content');

  // Mise en place hors étapes : un panier servi (VTE-01) portant une remise (VTE-03). Ce que
  // le scénario montre commence APRÈS, quand il s'agit de la défaire.
  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  await page.goto('/sales-home');
  await chercherProduit(page, produit);
  await ajouterAuPanier(page, '2');
  await expect(ligne).toContainText('1 320');
  await page.getByRole('button', { name: 'Remise' }).click();
  const choix = page.locator('ngb-popover-window').first();
  await expect(choix).toContainText('Choisir une remise');
  await choix.getByText('Grille de remise officine').first().click();
  await expect(contenu).toContainText(/Remise\s*:\s*\d+\s*%/);

  await etape(1, async () => {
    // La pastille est la seule trace visible d'une remise en cours : c'est elle qu'il faut
    // savoir reconnaître avant de pouvoir la retirer.
    await expect(contenu).toContainText(/Remise\s*:\s*\d+\s*%/);
  });

  await etape(2, async () => {
    // Le bouton n'a pas de libellé visible — une croix — mais porte son nom accessible :
    // c'est ce nom que le lecteur retrouvera dans l'infobulle.
    await page.getByRole('button', { name: 'Supprimer la remise' }).click();
    await expect(page.locator('.modal-content')).toContainText(/supprimer la remise/i);
  });

  await etape(3, async () => {
    const confirmation = page.locator('.modal-content');
    await confirmation.getByRole('button', { name: 'Oui' }).click();
    // La preuve que la remise est bien partie : la pastille disparaît et le net à encaisser
    // revient au montant brut de la ligne, à l'arrondi de caisse près.
    await expect(contenu).not.toContainText(/Remise\s*:\s*\d+\s*%/);
    await expect(ligne).toContainText('1 320');
  });

  // ── Remise en état, hors étapes : la vente en cours est abandonnée. ───────────────────
  await page.getByRole('button', { name: 'Annuler' }).click();
  const abandon = page.locator('.modal-content');
  await expect(abandon).toBeVisible();
  await abandon.getByRole('button', { name: 'Oui' }).click();
  await expect(contenu).toContainText(/Panier vide|Ajoutez des produits/i);
});

