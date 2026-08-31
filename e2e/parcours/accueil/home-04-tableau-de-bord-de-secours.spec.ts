import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Que se passe-t-il quand la déduction ne donne rien — un utilisateur dont ni le compte ni le
 * rôle n'a de tableau de bord, ou un serveur momentanément muet ?
 *
 * Le choix retenu est de ne JAMAIS faire échouer la connexion sur ce motif : l'accueil
 * générique s'affiche à la place, avec le menu de navigation intact. Un tableau de bord est un
 * confort ; ne pas pouvoir entrer dans l'application ne l'est pas.
 *
 * Le parcours provoque le second cas — la résolution qui n'aboutit pas — parce qu'il produit
 * exactement l'écran du premier, et sans toucher aux comptes de la démonstration.
 */
scenario('HOME-04', async ({ etape, page }) => {
  await etape(1, async () => {
    // La résolution ne répond pas : l'application doit s'en accommoder, pas s'y arrêter.
    await page.route('**/api/dashboard-layouts/resolved', route => route.abort());
    await page.goto('/');

    const contenu = page.locator('#main-content');
    await expect(contenu).toContainText('Bienvenue');
    await expect(contenu).toContainText(/Aucun tableau de bord n'est configuré/);
    // Le menu reste là : c'est ce qui distingue un accueil de secours d'un écran en panne.
    await expect(page.locator('app-navbar, app-sidebar').first()).toBeVisible();
  });
});
