import { expect } from '@playwright/test';
import { chercherAuCatalogue } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Trois lectures que rien d'autre ne rapproche : ce que le produit RAPPORTE (marge absolue et
 * taux), le temps qu'il TIENDRA (couverture en jours, rotation), et ce que la loi en dit
 * (statut légal). Le pharmacien les regarde ensemble — une forte marge sur un produit qui
 * tourne une fois l'an ne vaut pas une marge faible sur un produit qui tourne tous les mois.
 *
 * Le statut légal n'est pas une bascule : il se lit ici, et se change dans le formulaire
 * complet — c'est une donnée réglementaire, pas un réglage d'exploitation.
 *
 * Parcours en LECTURE.
 */
scenario('REF-41', async ({ etape, page }) => {
  const produit = 'DOLIPRANE';
  const synthese = page.locator('app-produit-synthese-tab');

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    await expect(page.getByRole('tab', { name: 'Synthèse' })).toBeVisible();
  });

  await etape(2, async () => {
    // La marge sous ses deux formes : en francs (ce qu'on gagne sur une boîte) et en taux
    // (ce qu'on gagne rapporté au prix de vente). Les deux figurent, dans deux cartes.
    await expect(synthese).toContainText('Marge brute');
    await expect(synthese).toContainText('Marge absolue');
    // La couverture : combien de jours le stock tient au rythme de consommation constaté.
    await expect(synthese).toContainText('Jours de stock');
    // Et le statut légal, en pastille — ce qui décide de l'ordonnance et du droit au retour.
    const reglementation = synthese.locator('.synthese-section').filter({ hasText: 'RÉGLEMENTATION' }).first();
    await expect(reglementation).toContainText('Statut légal');
    await expect(reglementation).toContainText(/Sans liste|Liste I|Liste II|Stupéfiants|PSO/i);
  });
});
