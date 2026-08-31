import { expect } from '@playwright/test';
import { assurerCaisseOuverte } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un caissier, une caisse. La règle paraît évidente ; sans elle, deux sessions ouvertes en
 * parallèle se partageraient les encaissements de la même journée, et aucun comptage du soir
 * ne pourrait plus être rapproché de quoi que ce soit.
 *
 * L'application ne l'énonce pas par un message d'erreur : elle rend le geste IMPOSSIBLE.
 * Caisse ouverte, l'écran ne montre plus l'ouverture — il montre la session en cours, son
 * fonds et ses encaissements. Il n'y a rien à cliquer deux fois.
 *
 * Parcours en LECTURE : il constate l'état, sans rien modifier.
 */
scenario('VTE-34', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  // La caisse doit être OUVERTE pour que le scénario ait un objet.
  await assurerCaisseOuverte(page);

  await etape(1, async () => {
    await page.goto('/my-cash-register');
    // La session en cours occupe l'écran : impossible d'en ouvrir une seconde puisque le
    // formulaire d'ouverture n'existe plus.
    await expect(page.getByText(/Ma caisse en cours/i)).toBeVisible();
    await expect(contenu).toContainText(/En cours/i);
    await expect(page.getByRole('button', { name: 'Ouvrir la caisse' })).toHaveCount(0);
    // Ce qui reste offert, c'est la suite logique : fermer celle-ci.
    await expect(page.getByRole('button', { name: /Fermer la caisse/i })).toBeVisible();
  });
});
