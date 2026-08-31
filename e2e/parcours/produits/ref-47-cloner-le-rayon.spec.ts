import { expect } from '@playwright/test';
import { chercherAuCatalogue } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Ouvrir un dépôt ou une seconde officine, c'est devoir replacer tout un catalogue dans un
 * nouveau bâtiment. Le clonage évite de recommencer ce travail : on part du placement déjà
 * décidé dans l'officine et on l'applique au stockage cible, produit par produit.
 *
 * L'écran ne propose que les stockages où le produit N'EST PAS encore rangé — assigner deux
 * fois le même emplacement n'aurait pas de sens, et l'erreur est ainsi rendue impossible
 * plutôt que refusée après coup.
 *
 * Parcours en LECTURE : il ouvre la fenêtre et montre son choix, sans créer une affectation
 * de dépôt dont les parcours de répartition ne s'attendent pas à hériter.
 */
scenario('REF-47', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first()
      .getByRole('button', { name: 'Actions' }).click();
    // Le libellé de l'action change avec l'état du produit : « Cloner vers un autre
    // stockage » quand il a déjà un emplacement, « Assigner un emplacement » sinon.
    await page.getByRole('button', { name: /Cloner vers un autre stockage|Assigner un emplacement/ }).click();
    await expect(modale).toBeVisible();
  });

  await etape(2, async () => {
    // La fenêtre nomme le produit concerné et demande le stockage cible.
    await expect(modale).toContainText(produit);
    await expect(modale).toContainText(/[Ss]tockage/);
  });

  etape.horsPortee(
    3,
    'valider créerait une affectation de dépôt permanente pour ce produit ; la fenêtre et son ' +
      'choix sont illustrés, la validation ne l’est pas.',
  );
});
