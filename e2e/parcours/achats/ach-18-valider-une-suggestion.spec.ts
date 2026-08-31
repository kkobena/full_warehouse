import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Valider une proposition d'achat, c'est dire : « ces quantités me conviennent, j'en fais une
 * commande ». Elle quitte alors les propositions pour rejoindre les commandes fournisseurs,
 * où elle sera transmise au grossiste.
 *
 * Le tri par urgence, juste au-dessus, est ce qui rend la décision tenable : on regarde
 * d'abord les urgents — les produits déjà sous leur seuil — puis on valide.
 *
 * Parcours en LECTURE : valider transformerait une proposition en commande, et les compteurs
 * des autres parcours d'achat comptent sur l'état du jeu de démonstration.
 */
scenario('ACH-18', async ({ etape, page }) => {
  const panneau = page.locator('app-suggestion-produit-panel');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Propositions d'achat/ }).click();
    const liste = page.locator('app-suggestion-fournisseur-list');
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    // Un simple clic ouvre la proposition : la ligne est le lien vers son détail.
    await liste.locator('tbody tr').first().click();
    await expect(panneau).toBeVisible();
  });

  await etape(2, async () => {
    // Les trois gestes offerts sur une proposition : la valider, en commander tout ou partie,
    // ou n'en commander que les lignes filtrées — les urgentes, par exemple.
    await expect(panneau.getByRole('button', { name: 'Valider' })).toBeVisible();
    await expect(panneau.getByRole('button', { name: 'Commander tout' })).toBeVisible();
    await expect(panneau).toContainText('Urgents');
  });
});
