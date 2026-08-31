import { expect } from '@playwright/test';
import { chercherAuCatalogue, creerProduitJetable } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La suppression d'un produit n'est offerte que lorsqu'elle est SANS CONSÉQUENCE. Dès qu'un
 * produit a du stock, l'action est proposée mais inerte ; et même sans stock, le serveur
 * refuse la suppression d'un produit référencé par une vente, une commande ou un lot — car
 * l'effacer emporterait avec lui l'historique qui le cite.
 *
 * D'où la règle que le pharmacien doit retenir : on ne supprime que ce qui vient d'être créé
 * par erreur. Tout le reste se met en veille (REF-39, REF-40).
 *
 * Le produit supprimé ici est créé par le parcours lui-même, hors étapes : supprimer un
 * produit du catalogue de démonstration priverait les autres parcours de leurs données.
 */
scenario('REF-04', async ({ etape, page }) => {
  const suffixe = Date.now().toString().slice(-6);
  const libelle = `SIROP DEMONSTRATION ${suffixe}`;
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await creerProduitJetable(page, libelle, `78${suffixe}`);

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, libelle);
    await expect(lignes.first()).toContainText(libelle);
    // Le produit vient d'être créé : aucun stock, aucun mouvement. C'est le seul cas où la
    // suppression est réellement praticable.
    await expect(lignes.first()).toContainText('0');
  });

  await etape(2, async () => {
    await lignes.first().getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Supprimer' }).click();
    const confirmation = page.locator('.modal-content');
    await expect(confirmation).toBeVisible();
    await confirmation.getByRole('button', { name: 'Oui' }).click();
    await expect(confirmation).toBeHidden();
    // La recherche qui le trouvait à l'instant ne ramène plus rien : la fiche n'existe plus.
    await page.getByPlaceholder(/Rechercher \(CIP/).fill(libelle);
    await page.keyboard.press('Enter');
    await expect(page.locator('#main-content')).toContainText('Aucun produit trouvé');
  });
});
