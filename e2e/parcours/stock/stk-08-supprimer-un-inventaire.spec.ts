import { expect } from '@playwright/test';
import { traverserConfirmations } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un inventaire ouvert par erreur — mauvais rayon, mauvais périmètre — n'a pas à traîner dans
 * la liste : il fausserait la lecture de ce qui reste à compter, et personne n'oserait le
 * clôturer de peur d'appliquer des écarts inventés.
 *
 * La suppression n'est possible QUE tant qu'il n'est pas clôturé : après, l'inventaire est un
 * document comptable qui a produit des mouvements de stock, et il ne se supprime plus. C'est
 * pourquoi l'action ne figure pas sur l'onglet des inventaires clôturés.
 *
 * Parcours ÉCRIVANT dans la base : il crée un inventaire puis le supprime — il ne détruit
 * donc rien que le jeu de démonstration ait à conserver.
 */
scenario('STK-08', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const description = `Parcours STK-08 — à supprimer ${Date.now().toString().slice(-5)}`;

  await etape(1, async () => {
    // Mise en scène : l'inventaire qu'on supprimera, créé par le parcours.
    await page.goto('/inventaire');
    await page.getByRole('button', { name: 'Nouveau' }).click();
    const modale = page.locator('.modal-content');
    await modale.locator('#inventoryCategory').focus();
    await modale.locator('#inventoryCategory').press('ArrowDown');
    await page.locator('.ng-option').filter({ hasText: 'global' }).first().click();
    await modale.locator('#description').fill(description);
    await modale.getByRole('button', { name: 'Créer' }).click();
    // L'inventaire créé enchaîne sur sa feuille de comptage : on la ferme.
    const feuille = page.locator('.modal-content').last();
    await feuille.getByRole('button', { name: /Annuler|Fermer/ }).first().click();
    await page.goto('/inventaire');
    await expect(lignes.filter({ hasText: description }).first()).toBeVisible();
  });

  await etape(2, async () => {
    // La suppression demande confirmation en nommant l'inventaire : dans une liste où
    // plusieurs comptages se ressemblent, c'est le dernier garde-fou.
    await lignes.filter({ hasText: description }).first()
      .getByRole('button', { name: 'Supprimer' }).click();
    const question = page.locator('.modal-content:visible').first();
    await expect(question).toContainText(/Suppression inventaire/);
    await expect(question).toContainText(description);
  });

  await etape(3, async () => {
    await traverserConfirmations(page, { limite: 1 });
    await expect(lignes.filter({ hasText: description })).toHaveCount(0);
  });
});
