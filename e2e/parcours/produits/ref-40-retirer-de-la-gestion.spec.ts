import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un produit qui ne sera plus jamais réapprovisionné encombre : il remonte dans les
 * recherches, il fausse les propositions de commande, et le supprimer emporterait avec lui
 * l'historique des ventes qui lui sont attachées. L'application n'offre pas d'archivage
 * distinct — c'est la MISE EN VEILLE qui joue ce rôle, et le modèle le dit désormais.
 *
 * La différence avec REF-39 n'est pas dans le geste, elle est dans l'intention : là il
 * s'agissait d'une suspension temporaire, ici d'un retrait durable. D'où l'importance du
 * troisième temps : savoir RETROUVER ce qu'on a retiré, sans quoi le retrait serait une
 * disparition.
 *
 * Parcours ÉCRIVANT dans la base : il réactive le produit hors étapes, pour laisser le
 * catalogue de démonstration tel qu'il l'a trouvé.
 */
scenario('REF-40', async ({ etape, page }) => {
  const recherche = 'BELLADONNA';
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const ligne = () => lignes.first();

  const chercherAuCatalogue = async (etat: 'Produits actifs' | 'Produits désactivés'): Promise<void> => {
    await choisirDansSelect(page, 'produitFiltreEtat', etat);
    const champ = page.getByPlaceholder(/Rechercher \(CIP/);
    await champ.fill('');
    await champ.fill(recherche);
    await page.keyboard.press('Enter');
  };

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue('Produits actifs');
    await expect(ligne()).toContainText(recherche);
    await expect(ligne()).toContainText('Actif');
  });

  await etape(2, async () => {
    await ligne().getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Mettre en veille' }).click();
    const confirmation = page.locator('.modal-content');
    await expect(confirmation).toBeVisible();
    await confirmation.getByRole('button', { name: /Oui|Confirmer/ }).click();
    await expect(confirmation).toBeHidden();
  });

  await etape(3, async () => {
    // Le produit n'a pas disparu : il a changé de liste. Le filtre d'état est ce qui rend le
    // retrait réversible — sans lui, on ne saurait plus ce qu'on a retiré.
    await chercherAuCatalogue('Produits désactivés');
    await expect(ligne()).toContainText(recherche);
    await expect(ligne()).toContainText('Veille');
  });

  // ── Remise en état : le produit reprend sa place au catalogue. ──────────────────────────
  await ligne().getByRole('button', { name: 'Actions' }).click();
  await page.getByRole('button', { name: 'Réactiver' }).click();
  const confirmation = page.locator('.modal-content');
  await expect(confirmation).toBeVisible();
  await confirmation.getByRole('button', { name: /Oui|Confirmer/ }).click();
  await expect(confirmation).toBeHidden();
});
