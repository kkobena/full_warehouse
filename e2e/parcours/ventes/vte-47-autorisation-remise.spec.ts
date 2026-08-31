import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, seConnecterEnTantQue } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Accorder une remise, c'est décider de ce que l'officine renonce à encaisser. Le geste est
 * donc soumis au même contrôle que la suppression d'une ligne (VTE-46) : un caissier qui ne
 * détient pas le privilège doit faire valider par quelqu'un qui l'a.
 *
 * La remise proposée ici est la GRILLE PRODUIT — celle que VTE-03 applique automatiquement
 * ligne par ligne. C'est la seule en service : la remise attachée au client subsiste dans le
 * modèle mais plus rien ne la lit au calcul d'une vente.
 *
 * Parcours ÉCRIVANT dans la base : il enregistre une utilisation de clé de sécurité, et
 * abandonne la vente ensuite.
 */
scenario('VTE-47', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const modale = page.locator('.modal-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  // Comme VTE-46 : un compte DÉPOURVU du privilège, sans quoi il n'y aurait rien à autoriser.
  await seConnecterEnTantQue(page, 'kkone', 'admin');
  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    await chercherProduit(page, produit);
    await ajouterAuPanier(page, '2');
    await expect(lignes.first()).toContainText(produit);

    // Le bouton « Remise » ouvre la liste des grilles en vigueur.
    await page.getByRole('button', { name: 'Remise' }).click();
    await expect(page.locator('.remise-popover-content')).toBeVisible();
    await page.locator('.remise-popover-item').first().click();

    // La remise n'est pas appliquée : l'autorisation est demandée d'abord, et elle nomme le
    // privilège en jeu.
    await expect(modale).toContainText('Autorisation requise');
    await expect(modale).toContainText('Application de remise');
  });

  await etape(2, async () => {
    await modale.locator('#securityKey input').fill('1234');
    await modale.locator('#comment').fill('Remise accordée à un client fidèle');
    await modale.getByRole('button', { name: 'Autoriser' }).click();
    await expect(modale).toBeHidden();
    // La remise s'inscrit alors sur la vente, et le total en tient compte.
    await expect(page.locator('#main-content')).toContainText(/Remise/i);
  });

  // ── Remise en état : la vente est abandonnée. ───────────────────────────────────────────
  await page.locator('app-sale-actions').getByRole('button', { name: 'Annuler' }).click();
  await expect(modale).toBeVisible();
  await modale.getByRole('button', { name: 'Oui' }).click();
});
