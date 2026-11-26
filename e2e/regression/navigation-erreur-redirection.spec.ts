import { expect, test } from '@playwright/test';

/**
 * Redirection quand la navigation elle-même échoue (`handleNavigationError`, app.config.ts) : une
 * route dont le resolver se voit refuser ses données par l'API doit mener à l'écran d'erreur qui
 * correspond au statut, et non laisser l'utilisateur sur une page blanche.
 *
 * Le gestionnaire retourne une `RedirectCommand` : c'est le routeur qui conduit la redirection,
 * au lieu d'une seconde navigation lancée pendant que la première échoue.
 *
 * Appui : la fiche utilisateur (`/admin/user-management/:login/view`). Son resolver
 * (`UserManagementResolve`) laisse remonter l'erreur HTTP jusqu'au routeur — contrairement aux
 * resolvers des catégories, clients ou produits, qui interceptent eux-mêmes le 404.
 *
 * L'erreur est SIMULÉE par Playwright sur la seule requête du resolver : rien n'est écrit en base,
 * et le backend ne voit jamais la requête.
 */
const FICHE = '/admin/user-management/admin/view';
const REQUETE_DU_RESOLVER = /\/api\/admin\/users\/admin(\?|$)/;

const CAS = [
  { statut: 403, ecran: '/accessdenied', destination: /\/accessdenied$/ },
  { statut: 404, ecran: '/404', destination: /\/404$/ },
  { statut: 500, ecran: '/error', destination: /\/error$/ },
  { statut: 401, ecran: '/login', destination: /\/login/ },
];

for (const { statut, ecran, destination } of CAS) {
  test(`navigation refusée par l'API (${statut}) : redirection vers ${ecran}`, async ({ page }) => {
    await page.route(REQUETE_DU_RESOLVER, route => route.fulfill({ status: statut, json: { status: statut, title: `Essai ${statut}` } }));

    if (statut === 401) {
      // Sur un 401, AuthExpiredInterceptor déconnecte aussi le client. On simule la réponse du
      // serveur : la vraie déconnexion pourrait invalider la session partagée par les autres tests.
      await page.route(/\/api\/logout(\?|$)/, route => route.fulfill({ status: 200, body: '' }));
    }

    // Départ depuis l'accueil, application déjà amorcée : c'est la navigation vers la fiche qui
    // échoue, pas le démarrage.
    await page.goto('/');
    await expect(page.locator('#main-content')).toBeVisible();

    const reponse = page.waitForResponse(REQUETE_DU_RESOLVER);
    await page.evaluate(url => history.pushState(null, '', url), FICHE);
    await page.evaluate(() => window.dispatchEvent(new PopStateEvent('popstate')));
    expect((await reponse).status()).toBe(statut);

    await expect(page).toHaveURL(destination);
    await expect(page).not.toHaveURL(/user-management/);
    if (statut !== 401) {
      await expect(page.locator('.err-title')).toBeVisible();
    }
  });
}
