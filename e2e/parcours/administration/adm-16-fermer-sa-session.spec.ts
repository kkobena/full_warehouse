import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Scénario AJOUTÉ au cahier de recette, avec ADM-15. Fermer sa session n'est pas une
 * politesse : au comptoir, toute vente est rattachée au compte connecté. Une session laissée
 * ouverte fait signer les ventes du suivant par le précédent.
 */
scenario('ADM-16', async ({ etape, page }) => {
  await etape(1, async () => {
    await page.goto('/');
    // Le menu de compte porte le nom de l'utilisateur connecté — c'est aussi ce qui rend
    // visible, d'un coup d'œil, QUI travaille sur le poste.
    await page.getByRole('button', { name: /Administrator/i }).click();
    // Les entrées du volet de compte sont des `role="menuitem"`, pas des liens : le volet
    // est un menu au sens ARIA, ce qui le rend navigable au clavier.
    await expect(page.getByRole('menuitem', { name: /Se déconnecter/i })).toBeVisible();
  });

  await etape(2, async () => {
    await page.getByRole('menuitem', { name: /Se déconnecter/i }).click();
    // Retour à l'écran de connexion : c'est l'unique état où aucun écran métier n'est
    // atteignable.
    await expect(page).toHaveURL(/\/login/);
    await expect(page.getByRole('heading', { name: 'Bienvenue' })).toBeVisible();
  });
});
