import { expect, test } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../src/actions';

/**
 * Régression de docs/PLAN-BUG-VENTES-ASSURANCE-SANS-CLIENT.md §6.
 *
 * Changer une vente comptant en vente assurance crée côté serveur une NOUVELLE vente, sans assuré. L'écran la relit aussitôt par
 * son nouvel identifiant : cette lecture plantait (NullPointerException sur l'assuré absent, HTTP 500), l'écran gardait l'ancien
 * identifiant, supprimé par la transformation, et le choix de l'assuré comme l'ajout d'un produit échouaient ensuite.
 *
 * Parcours ÉCRIVANT dans la base : il crée une vente et la remet à zéro à la fin.
 */
test('comptant → assurance : la vente se relit, l\'assuré se choisit, un second produit s\'ajoute', async ({ page }) => {
  test.setTimeout(150_000);
  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  await page.goto('/sales-home');
  await chercherProduit(page, 'DOLIPRANE 500');
  await ajouterAuPanier(page, '1');
  const lignes = page.locator('tbody tr').filter({ visible: true });
  await expect(lignes.first()).toContainText('DOLIPRANE');

  // Transformation, puis relecture de la nouvelle vente : c'est elle qui rendait 500.
  const relecture = page.waitForResponse(r => r.request().method() === 'GET' && /\/api\/sales\/\d+\/\d{4}-\d{2}-\d{2}$/.test(r.url()));
  await page.getByRole('tab', { name: /Assurance/ }).click();
  const modale = page.locator('.modal-content');
  await expect(modale).toContainText(/Changement de type de vente/i);
  await modale.getByRole('button', { name: 'Oui' }).click();
  expect((await relecture).status(), 'la vente transformée, sans assuré, doit pouvoir être relue').toBe(200);
  await expect(lignes.first()).toContainText('DOLIPRANE');

  // Choix de l'assuré : les montants se calculent sur la nouvelle vente.
  const recherche = page.getByPlaceholder('Rechercher un client assuré');
  await recherche.fill('ASSI');
  const changement = page.waitForResponse(r => r.request().method() === 'PUT' && r.url().includes('/assurance/change/customer'));
  await recherche.press('Enter');
  await expect(modale).toContainText('CLIENTS ASSURÉS');
  await modale.locator('tbody tr').first().dblclick();
  expect((await changement).ok(), 'le client se rattache à la vente transformée').toBe(true);
  await expect(modale).toBeHidden();
  await expect(page.locator('#main-content')).toContainText(/total assurance\s*[1-9]/i);

  // Un second produit s'ajoute à la vente assurance.
  await chercherProduit(page, 'DOLIPRANE 250');
  await ajouterAuPanier(page, '1');
  await expect(lignes).toHaveCount(2);

  await assurerPanierVide(page);
});

/**
 * Même parcours, mais la relecture de la vente transformée échoue (500 simulé). La transformation a supprimé la vente
 * comptant : l'écran doit déjà tenir le NOUVEL identifiant, renvoyé par la transformation, et non l'ancien.
 */
test('comptant → assurance : si la relecture échoue, l\'écran garde le nouvel identifiant', async ({ page }) => {
  test.setTimeout(150_000);
  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  await page.goto('/sales-home');
  await chercherProduit(page, 'DOLIPRANE 500');
  await ajouterAuPanier(page, '1');

  const motifRelecture = /\/api\/sales\/\d+\/\d{4}-\d{2}-\d{2}$/;
  let relectureEchouee = false;
  await page.route(motifRelecture, route => {
    if (route.request().method() === 'GET' && !relectureEchouee) {
      relectureEchouee = true;
      return route.fulfill({ status: 500, contentType: 'application/json', body: '{"title":"Erreur simulée"}' });
    }
    return route.continue();
  });

  const transformation = page.waitForResponse(r => r.url().includes('/sales/assurance/transform?'));
  await page.getByRole('tab', { name: /Assurance/ }).click();
  const modale = page.locator('.modal-content');
  await modale.getByRole('button', { name: 'Oui' }).click();
  const nouvelId = ((await (await transformation).json()) as { id: number }).id;
  await expect.poll(() => relectureEchouee).toBe(true);

  // Choix de l'assuré : la requête doit viser la nouvelle vente.
  const recherche = page.getByPlaceholder('Rechercher un client assuré');
  await recherche.fill('ASSI');
  const changement = page.waitForRequest(r => r.method() === 'PUT' && r.url().includes('/assurance/change/customer'));
  await recherche.press('Enter');
  await expect(modale).toContainText('CLIENTS ASSURÉS');
  await modale.locator('tbody tr').first().dblclick();
  expect(JSON.stringify((await changement).postDataJSON())).toContain(`"id":${nouvelId}`);

  await page.unroute(motifRelecture);
  await assurerPanierVide(page);
});
