import { expect } from '@playwright/test';
import { chercherAuCatalogue } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Trois contraintes qui ne se devinent pas d'un libellé : la chaîne du froid, l'inscription
 * sur la liste des médicaments essentiels, la disponibilité pendant les gardes. On les pose
 * ici, et le reste de l'application en tient compte — l'alerte thermosensible à la réception,
 * la liste de garde, les états de stock.
 *
 * Comme le suivi des lots (REF-03), ces bascules s'enregistrent SUR-LE-CHAMP : il n'y a pas de
 * bouton de validation, et c'est ce qu'il faut savoir avant de les toucher.
 *
 * Parcours ÉCRIVANT dans la base : il rétablit l'état d'origine de l'attribut basculé.
 */
scenario('REF-42', async ({ etape, page }) => {
  const produit = 'DOLIPRANE';
  const synthese = page.locator('app-produit-synthese-tab');
  const thermosensible = () => page.getByRole('switch', { name: 'Thermosensible' });

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    const reglementation = synthese.locator('.synthese-section').filter({ hasText: 'RÉGLEMENTATION' }).first();
    await expect(reglementation).toBeVisible();
    // Les quatre attributs qui se basculent, plus le statut légal qui, lui, se lit seulement.
    await expect(reglementation).toContainText('Thermosensible');
    await expect(reglementation).toContainText('Médicament essentiel');
    await expect(reglementation).toContainText('Produit de garde');
    await expect(reglementation).toContainText('Classification personnalisée');
  });

  await etape(2, async () => {
    const avant = await thermosensible().isChecked();
    await thermosensible().click();
    await expect(thermosensible()).toBeChecked({ checked: !avant });
    // Remise en état immédiate : marquer durablement un produit du catalogue de démonstration
    // changerait les alertes de chaîne du froid des autres parcours.
    await thermosensible().click();
    await expect(thermosensible()).toBeChecked({ checked: avant });
  });
});
