import { expect, test, type Page } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, rattacherUnClient } from '../src/actions';
import { executer, lire, rejouer } from '../src/base-de-donnees';

/**
 * Produit sur ordonnance : la clôture exige une ordonnance ou un prescripteur
 * (docs/PLAN-EXTRACTION-ORDONNANCE-OCR.md §33). AMOXICILLINE est sur ordonnance dans le jeu de démo ; PARACETAMOL, lui, est en libre accès.
 *
 * Parcours ÉCRIVANT dans la base : il enregistre DEUX ventes réelles (stock et caisse bougent, comme VTE-05) ; les
 * prescripteurs, ordonnances et rattachements d'essai sont retirés à la fin.
 */
test.describe.configure({ mode: 'serial' });

const PRODUIT = 'AMOXICILLINE TEVA 1 g';

test.beforeAll(() => {
  rejouer('scripts/essai-ordonnance/nettoyer.sql');
  // Garde : le parcours n'a de sens que sur un produit sur ordonnance.
  expect(lire(`SELECT statut_legal <> 'SANS_LISTE' FROM produit WHERE libelle LIKE '${PRODUIT}%' ORDER BY id LIMIT 1`), `${PRODUIT} doit être sur ordonnance`).toBe('t');
});
test.afterAll(() => rejouer('scripts/essai-ordonnance/nettoyer.sql'));

/** Panier d'un produit sur ordonnance, puis le geste du comptoir jusqu'au bouton « Finaliser ». */
async function preparerLaFinalisation(page: Page, avecClient = false): Promise<void> {
  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  await page.goto('/sales-home');
  await chercherProduit(page, PRODUIT);
  await ajouterAuPanier(page, '1');
  if (avecClient) {
    await rattacherUnClient(page);
  }
  const recherche = page.locator('#produitbox');
  await recherche.click();
  await recherche.press('Enter');
  await expect(page.locator('#CASH')).toBeVisible();
  await page.locator('#CASH').fill('100000');
}

test('refus sans ordonnance ni prescripteur, puis prescripteur seul : la vente se clôt', async ({ page }) => {
  test.setTimeout(240_000);
  const modale = page.locator('.modal-content');
  await preparerLaFinalisation(page);

  // Refus : la vente reste ouverte, la fenêtre nomme le produit.
  await page.getByRole('button', { name: 'Finaliser' }).click();
  await expect(modale).toContainText('Ordonnance requise');
  await expect(modale).toContainText(PRODUIT);
  await modale.getByRole('button', { name: 'Revenir à la vente' }).click();
  await expect(modale).toBeHidden();
  await expect(page.locator('tbody tr').filter({ visible: true }).first()).toContainText(PRODUIT);

  // Prescripteur seul (aucun client, donc aucune ordonnance à proposer) : créé à la volée, puis la vente se clôt.
  await page.getByRole('button', { name: 'Finaliser' }).click();
  await expect(modale).toContainText('Ordonnance requise');
  await modale.locator('input[placeholder*="nouveau prescripteur"]').fill('ESSAI E2E Requis');
  await modale.getByRole('button', { name: 'Créer' }).click();
  await modale.getByRole('button', { name: 'Valider' }).click();
  await expect(modale).toBeHidden();
  await expect(page.locator('#main-content')).toContainText(/Panier vide|Ajoutez des produits/i);
  expect(
    Number(lire("SELECT count(*) FROM vente_prescripteur v JOIN prescripteur p ON p.id = v.prescripteur_id WHERE p.nom = 'ESSAI E2E Requis'")),
  ).toBe(1);
});

test("avec une ordonnance en cours du client : on l'associe", async ({ page }) => {
  test.setTimeout(240_000);
  const modale = page.locator('.modal-content');
  await preparerLaFinalisation(page, true);
  const client = lire("SELECT customer_id FROM sales WHERE statut = 'ACTIVE' AND customer_id IS NOT NULL ORDER BY created_at DESC LIMIT 1");
  executer(`WITH p AS (INSERT INTO prescripteur (nom) VALUES ('ESSAI E2E Ordonnance') RETURNING id),
    o AS (INSERT INTO ordonnance (customer_id, prescripteur_id, date_prescription, renouvellements) SELECT ${Number(client)}, p.id, current_date, 0 FROM p RETURNING id)
    INSERT INTO ordonnance_ligne (ordonnance_id, rang, produit_id, quantite_prescrite)
    SELECT o.id, 1, (SELECT id FROM produit WHERE libelle LIKE 'AMOXICILLINE TEVA 1 g%' ORDER BY id LIMIT 1), 2 FROM o;`);

  await page.getByRole('button', { name: 'Finaliser' }).click();
  await expect(modale).toContainText('Ordonnance requise');
  await expect(modale).toContainText('Ordonnances en cours du client');
  await expect(modale).toContainText('ESSAI E2E Ordonnance');
  await modale.getByRole('button', { name: 'Valider' }).click();
  await expect(modale).toBeHidden();
  await expect(page.locator('#main-content')).toContainText(/Panier vide|Ajoutez des produits/i);
  expect(Number(lire('SELECT count(*) FROM ordonnance_vente'))).toBe(1);
  // Le produit vendu est le produit prescrit : la ligne de vente est liée, mais la vente étant clôturée, 1 sur 2 est délivré.
  expect(Number(lire('SELECT count(*) FROM ordonnance_delivrance'))).toBe(1);
});
