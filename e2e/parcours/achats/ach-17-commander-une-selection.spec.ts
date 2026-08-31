import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * On ne commande pas toujours tout ce qui est proposé : le budget du mois est serré, ou seuls
 * les urgents pressent. La sélection de lignes permet de n'emporter qu'une partie de la
 * proposition — le reste y demeure, et pourra partir la semaine suivante.
 *
 * Les filtres du dessus (Tous, Urgents, Normaux, Couverts) servent précisément à cela :
 * filtrer, tout sélectionner, commander — trois gestes au lieu de vingt cases à cocher.
 *
 * Parcours en LECTURE : commander consommerait des lignes du jeu de démonstration, dont
 * dépendent ACH-11 à ACH-21.
 */
scenario('ACH-17', async ({ etape, page }) => {
  const panneau = page.locator('app-suggestion-produit-panel');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Propositions d'achat/ }).click();
    const liste = page.locator('app-suggestion-fournisseur-list');
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    await liste.locator('tbody tr').first().click();
    await expect(panneau.locator('.ag-row').first()).toBeVisible();
  });

  await etape(2, async () => {
    // Cocher deux lignes : la barre d'actions de sélection apparaît alors, et son libellé
    // dit sur quoi elle portera.
    await panneau.locator('.ag-row').nth(0).locator('.ag-selection-checkbox, input[type="checkbox"]').first().click();
    await panneau.locator('.ag-row').nth(1).locator('.ag-selection-checkbox, input[type="checkbox"]').first().click();
    await expect(panneau.getByRole('button', { name: 'Commander la sélection' })).toBeVisible();
    // Et les filtres qui rendent la sélection rapide, urgents en tête.
    await expect(panneau).toContainText('Urgents');
  });

  etape.horsPortee(
    3,
    'valider créerait une commande avec les lignes retenues et les retirerait de la ' +
      'proposition ; la sélection est illustrée, la commande ne l’est pas.',
  );
});
