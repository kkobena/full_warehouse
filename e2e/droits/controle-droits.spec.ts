/**
 * Contrôle des droits des endpoints (docs/PLAN-SECURISATION-ENDPOINTS.md), rôle par rôle.
 *
 * Pour chaque compte : ouvre chaque écran de son menu, clique chacun de ses onglets (imbriqués
 * compris) et relève toute réponse 403 de l'API. Un écran que le menu accorde ne doit jamais
 * se voir refuser les données qu'il charge : un 403 ici est un code `nav_item` oublié dans la
 * cartographie des endpoints, ou un droit manquant dans les migrations.
 *
 * La campagne des parcours tourne en `admin`, qui passe toujours : elle ne peut pas révéler ces
 * oublis. D'où ce contrôle, joué avec des comptes non-admin et le backend en mode ENFORCE.
 *
 * Lecture seule : aucun formulaire n'est validé, seuls des onglets sont cliqués.
 */
import { expect, Page, test } from '@playwright/test';
import { COMPTES_DROITS } from '../src/config';

const ONGLET = '#main-content .nav-link[role="tab"]:visible:not(.disabled)';
const MAX_ONGLETS_PAR_ECRAN = 80;

interface Refus {
  ecran: string;
  requete: string;
  detail: string;
}

interface NoeudNav {
  routerLink?: string;
  children?: NoeudNav[];
}

function routesDuMenu(noeuds: NoeudNav[], acc = new Set<string>()): Set<string> {
  for (const n of noeuds ?? []) {
    if (n.routerLink) acc.add(n.routerLink);
    routesDuMenu(n.children ?? [], acc);
  }
  return acc;
}

async function attendreLeCalme(page: Page): Promise<void> {
  await page.waitForLoadState('networkidle', { timeout: 15_000 }).catch(() => undefined);
  await page.waitForTimeout(600);
}

// Pas de session partagée : chaque compte se connecte lui-même.
test.use({ storageState: { cookies: [], origins: [] } });

for (const { login, motDePasse } of COMPTES_DROITS) {
  test(`${login} — aucun écran de son menu n'essuie de refus`, async ({ page }, testInfo) => {
    test.setTimeout(15 * 60_000);
    const refus: Refus[] = [];
    let ecran = 'connexion';

    page.on('response', async reponse => {
      if (reponse.status() !== 403 || !reponse.url().includes('/api/')) return;
      const detail = await reponse
        .json()
        .then(corps => corps?.detail ?? '')
        .catch(() => '');
      refus.push({ ecran, requete: `${reponse.request().method()} ${new URL(reponse.url()).pathname}`, detail });
    });

    await page.goto('/login');
    await page.locator('[data-cy="username"]').fill(login);
    await page.locator('[data-cy="password"] input').fill(motDePasse);
    await page.locator('[data-cy="submit"]').click();
    await expect(page, `Connexion refusée pour « ${login} » : vérifier E2E_COMPTES_DROITS.`).not.toHaveURL(/\/login/);
    ecran = 'accueil';
    await attendreLeCalme(page);

    const arbre = await page.evaluate(async () => {
      const jeton = localStorage.getItem('pharma_smart_access_token');
      const reponse = await fetch('/api/nav/my-items', { headers: { Authorization: `Bearer ${jeton}` } });
      return reponse.json();
    });
    const routes = [...routesDuMenu(arbre)];
    let onglets = 0;

    for (const route of routes) {
      ecran = route;
      await page.goto(route).catch(() => undefined);
      await attendreLeCalme(page);

      // En largeur : un onglet cliqué peut en révéler d'autres, imbriqués.
      const vus = new Set<string>();
      for (let tour = 0; tour < MAX_ONGLETS_PAR_ECRAN; tour++) {
        const libelles = (await page.locator(ONGLET).allInnerTexts()).map(t => t.trim().replace(/\s+/g, ' '));
        const index = libelles.findIndex(l => l && !vus.has(l));
        if (index < 0) break;
        vus.add(libelles[index]);
        ecran = `${route} › ${libelles[index]}`;
        await page.locator(ONGLET).nth(index).click({ timeout: 5_000 });
        onglets++;
        await attendreLeCalme(page);
        // Un onglet-lien peut quitter l'écran : on y revient pour continuer.
        if (!new URL(page.url()).pathname.startsWith(route)) {
          await page.goto(route).catch(() => undefined);
          await attendreLeCalme(page);
        }
      }
    }

    await testInfo.attach('bilan', {
      body: JSON.stringify({ login, ecrans: routes.length, onglets, refus }, null, 2),
      contentType: 'application/json',
    });
    const lisibles = [...new Set(refus.map(r => `${r.ecran} → ${r.requete} (${r.detail})`))];
    expect(lisibles, `${routes.length} écrans, ${onglets} onglets parcourus`).toEqual([]);
  });
}
