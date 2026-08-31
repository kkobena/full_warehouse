import { expect } from '@playwright/test';
import { chercherAuCatalogue, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Tout ce qui entre en officine ne passe pas par une commande : un dépannage confrère, un
 * reliquat retrouvé, un stock repris à l'ouverture. La saisie manuelle de lot RATTACHE ces
 * unités à un numéro de lot et à une péremption, pour qu'elles suivent la règle FEFO comme
 * les autres.
 *
 * Le garde-fou est celui qu'il faut connaître : la quantité saisie NE PEUT PAS dépasser le
 * stock disponible du produit. Cet écran ne crée pas de stock — il documente celui qui existe
 * déjà, et que personne n'avait rattaché à un lot.
 *
 * Le modèle annonçait un onglet « Lots / péremption » : c'est une fenêtre, ouverte depuis le
 * menu d'actions de la ligne. Corrigé dans cahier-recette.model.ts en écrivant ce parcours.
 *
 * Parcours ÉCRIVANT dans la base : il ajoute un lot, que la restauration de l'instantané
 * efface avant la campagne suivante.
 */
scenario('REF-44', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const numeroLot = `LDEMO${Date.now().toString().slice(-5)}`;
  const modale = page.locator('.modal-content');
  const peremption = new Date();
  peremption.setFullYear(peremption.getFullYear() + 2);

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first()
      .getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Saisir un lot' }).click();
    // L'en-tête rappelle le stock disponible et la règle : la saisie s'y compare.
    await expect(modale).toContainText('Saisir un lot');
    await expect(modale).toContainText('Stock disponible');
  });

  await etape(2, async () => {
    await modale.locator('#numLot').fill(numeroLot);
    await saisirDate(page, 'expiryDate', peremption);
    await modale.locator('#quantityReceived').fill('2');
    await expect(modale.locator('#numLot')).toHaveValue(numeroLot);
  });

  await etape(3, async () => {
    await modale.getByRole('button', { name: 'Enregistrer le lot' }).click();
    await expect(modale).toBeHidden();
    // Le lot rejoint la liste FEFO du produit, à sa place dans l'ordre des péremptions. La
    // liste du catalogue porte les produits tels qu'ils ont été chargés : on la relit, sans
    // quoi on vérifierait l'état d'AVANT la saisie.
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    await page.getByRole('tab', { name: 'Stock' }).click();
    await expect(page.locator('app-produit-stock-tab')).toContainText(numeroLot);
  });
});
