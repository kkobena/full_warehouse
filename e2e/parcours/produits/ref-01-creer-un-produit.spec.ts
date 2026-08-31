import { expect } from '@playwright/test';
import { chercherDansSelect, choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Créer un produit, ce n'est pas remplir un nom : c'est le rendre VENDABLE et COMMANDABLE. La
 * fiche exige donc huit renseignements sans lesquels ni le prix, ni la TVA, ni la commande ne
 * pourraient se calculer — code CIP, fournisseur principal, dénomination, prix d'achat, prix
 * de vente, TVA, famille thérapeutique et rayon.
 *
 * Le reste de la fiche — statut légal, gestion des lots, seuils, laboratoire — est facultatif
 * et se complète plus tard : c'est ce qui permet de créer un produit au comptoir sans faire
 * attendre le client.
 *
 * Parcours ÉCRIVANT dans la base : il supprime le produit qu'il a créé.
 */
scenario('REF-01', async ({ etape, page }) => {
  // Un CIP qui ne heurtera aucun code existant, et un libellé qui se reconnaît d'un coup
  // d'œil dans le catalogue s'il venait à survivre à un échec.
  const suffixe = Date.now().toString().slice(-6);
  const codeCip = `77${suffixe}`;
  const libelle = `PARCOURS TEST ${suffixe}`;
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/produits');
    await page.getByRole('button', { name: 'Nouveau' }).click();
    await expect(page.getByRole('heading', { name: /Nouveau Produit/i })).toBeVisible();

    await page.locator('#f_codeCip').fill(codeCip);
    await chercherDansSelect(page, 'f_fournisseur', 'LABOREX', 'LABOREX');
    await page.locator('#f_libelle').fill(libelle);
    await page.locator('#f_costAmount').fill('1500');
    await page.locator('#f_regularUnitPrice').fill('2000');
    await choisirDansSelect(page, 'f_tva', '18');
    // Famille et rayon ne sont pas la même chose et n'ont pas la même liste : la première
    // classe le produit (générique, homéopathie, diététique…), le second dit OÙ il se range
    // dans l'officine. Le formulaire exige les deux.
    await chercherDansSelect(page, 'f_famille', 'HOMEO', 'HOMEOPATHIE');
    await chercherDansSelect(page, 'f_rayon', 'HOMEO', 'HOMEOPATHIE');
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    // L'enregistrement ramène au catalogue : le produit y est, cherchable par son CIP comme
    // par son libellé, et immédiatement proposé à la vente et à la commande.
    await expect(page.getByRole('heading', { name: 'Catalogue produits' })).toBeVisible();
    const champ = page.getByPlaceholder(/Rechercher \(CIP/);
    await champ.fill(codeCip);
    await page.keyboard.press('Enter');
    await expect(lignes.first()).toContainText(libelle);
  });

  // ── Remise en état : le produit créé est supprimé. Il n'a ni stock ni mouvement, la
  //    suppression est donc acceptée — c'est précisément ce que montre REF-04. ─────────────
  await lignes.first().getByRole('button', { name: 'Actions' }).click();
  await page.getByRole('button', { name: 'Supprimer' }).click();
  const confirmation = page.locator('.modal-content');
  await expect(confirmation).toBeVisible();
  await confirmation.getByRole('button', { name: 'Oui' }).click();
  await expect(confirmation).toBeHidden();
});
