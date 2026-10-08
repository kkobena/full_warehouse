import { expect, test } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherDansSelect, chercherProduit, rattacherUnClient } from '../src/actions';
import { executer, lire, rejouer } from '../src/base-de-donnees';

/**
 * Contrôle d'ordonnance et suivi d'ordonnance (docs/PLAN-EXTRACTION-ORDONNANCE-OCR.md §18 et §21).
 *
 * Les interactions n'étant pas chargées en base, le parcours pose une version FICTIVE « ESSAI-E2E »
 * (scripts/essai-ordonnance/preparer.sql) et la retire à la fin. Parcours ÉCRIVANT dans la base :
 * il laisse sa vente en cours abandonnée et supprime ses ordonnances et prescripteurs d'essai.
 */
test.describe.configure({ mode: 'serial' });

const PRODUIT_A = 'ADVIL 400 mg';
const PRODUIT_B = 'AMOXICILLINE TEVA 1 g';
const NEUTRE = 'DOLIPRANE 500';

test.beforeAll(() => {
  rejouer('scripts/essai-ordonnance/nettoyer.sql');
  rejouer('scripts/essai-ordonnance/preparer.sql');
});

test.afterAll(() => {
  rejouer('scripts/essai-ordonnance/nettoyer.sql');
});

test('contrôle : une contre-indication du panier se refuse, ou se prend en compte avec un motif tracé', async ({ page }) => {
  test.setTimeout(180_000);
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  await page.goto('/sales-home');
  await chercherProduit(page, PRODUIT_A);
  await ajouterAuPanier(page, '1');
  await expect(lignes.first()).toContainText('ADVIL');
  await rattacherUnClient(page);

  // Second produit : AMOXICILLINE x IBUPROFÈNE est une CI de la version d'essai.
  await chercherProduit(page, PRODUIT_B);
  await ajouterAuPanier(page, '1');
  await expect(modale).toContainText("Contrôle de l'ordonnance");
  await expect(modale).toContainText('Contre-indication');
  await expect(modale).toContainText('ESSAI-E2E');

  // Refus : la ligne n'est pas ajoutée.
  await modale.getByRole('button', { name: 'Ne pas ajouter' }).click();
  await expect(modale).toBeHidden();
  await expect(lignes).toHaveCount(1);

  // Prise en compte : le motif est obligatoire, puis la ligne s'ajoute et la décision est tracée.
  await chercherProduit(page, PRODUIT_B);
  await ajouterAuPanier(page, '1');
  await expect(modale).toContainText('Contre-indication');
  await modale.getByRole('button', { name: /Ajouter malgré/ }).click();
  await expect(modale).toContainText('Le motif est obligatoire');
  await modale.locator('#motifControle').fill('Essai e2e : prescription maintenue');
  await modale.getByRole('button', { name: /Ajouter malgré/ }).click();
  await expect(modale).toBeHidden();
  await expect(lignes).toHaveCount(2);

  expect(Number(lire("SELECT count(*) FROM alerte_sante_derogation WHERE alertes LIKE '%ESSAI-E2E%'"))).toBeGreaterThan(0);

  await assurerPanierVide(page);
});

test('suivi : saisir une ordonnance au comptoir, la reprendre et la rattacher à la vente', async ({ page }) => {
  test.setTimeout(240_000);
  const panneau = page.locator('.offcanvas.show');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  await page.goto('/sales-home');
  await chercherProduit(page, NEUTRE);
  await ajouterAuPanier(page, '1');
  await rattacherUnClient(page);

  const ouvrirPanneau = async () => {
    await page
      .locator('app-customer-overlay-panel')
      .filter({ visible: true })
      .first()
      .getByRole('button', { name: 'Ordonnances du client' })
      .click();
    await expect(panneau).toContainText('Ordonnances');
  };

  // Un traitement chronique actif du client, dont l'ordonnance date de 200 jours : la nouvelle ordonnance le renouvelle.
  const client = lire("SELECT customer_id FROM sales WHERE statut = 'ACTIVE' AND customer_id IS NOT NULL ORDER BY created_at DESC LIMIT 1");
  executer(`INSERT INTO customer_traitement_chronique (customer_id, produit_id, duree_jours, date_ordonnance, actif, note)
            SELECT ${Number(client)}, (SELECT id FROM produit WHERE libelle LIKE 'AMOXICILLINE TEVA 1 g%' ORDER BY id LIMIT 1), 30, current_date - 200, TRUE, 'ESSAI E2E';`);

  // Saisie : prescripteur créé à la volée, une ligne de 2 boîtes, 1 renouvellement.
  await ouvrirPanneau();
  await panneau.getByRole('button', { name: 'Nouvelle ordonnance' }).click();
  await expect(panneau).toContainText('Nouvelle ordonnance');
  await panneau.locator('input[placeholder*="nouveau prescripteur"]').fill('ESSAI E2E Docteur');
  await panneau.getByRole('button', { name: 'Créer' }).click();
  await expect(panneau.locator('ng-select').first()).toContainText('ESSAI E2E Docteur');
  await panneau.locator('#ordoRenouvellements').fill('1');
  await chercherDansSelect(page, 'ordoProduit', PRODUIT_B, 'AMOXICILLINE TEVA 1 g, comprimé dispersible');
  await panneau.locator('input[placeholder="Qté"]').fill('2');
  await panneau.getByRole('button', { name: 'Ajouter la ligne' }).click();
  await expect(panneau.locator('tbody tr')).toHaveCount(1);
  await panneau.getByRole('button', { name: 'Enregistrer' }).click();

  expect(lire("SELECT date_ordonnance = current_date FROM customer_traitement_chronique WHERE note = 'ESSAI E2E'"), 'le traitement chronique couvert est renouvelé').toBe('t');

  // Liste : en cours, 1 renouvellement, capacité 2 x (1 + 1) = 4 à délivrer.
  await expect(panneau).toContainText('En cours');
  await expect(panneau).toContainText('1 renouvellement(s) restant(s)');
  const ligneOrdonnance = panneau.locator('tbody tr').filter({ hasText: 'AMOXICILLINE' });
  await expect(ligneOrdonnance).toContainText('2');
  await expect(ligneOrdonnance.locator('td').nth(3)).toHaveText('4');
  expect(Number(lire("SELECT count(*) FROM ordonnance o JOIN prescripteur p ON p.id = o.prescripteur_id WHERE p.nom LIKE 'ESSAI E2E%'"))).toBe(1);

  // Reprise : « Ajouter » présélectionne le produit par le circuit ordinaire, puis il se vend.
  await ligneOrdonnance.getByRole('button', { name: /Ajouter/ }).click();
  await expect(panneau).toBeHidden();
  await expect(page.locator('#main-content')).toContainText(/Prix\s*:\s*[\d\s]*\d/);
  await ajouterAuPanier(page, '1');
  await expect(lignes.filter({ hasText: 'AMOXICILLINE' }).first()).toBeVisible();

  // Rattachement : la vente en cours et sa ligne AMOXICILLINE se lient à l'ordonnance.
  await ouvrirPanneau();
  await panneau.getByRole('button', { name: 'Rattacher à la vente en cours' }).click();
  await expect(page.getByText(/Vente rattachée/).first()).toBeVisible();
  expect(Number(lire('SELECT count(*) FROM ordonnance_vente'))).toBe(1);
  expect(lire("SELECT type_prescription FROM sales WHERE statut = 'ACTIVE' AND customer_id IS NOT NULL ORDER BY created_at DESC LIMIT 1"), 'la vente rattachée est une vente sur ordonnance').toBe('PRESCRIPTION');
  expect(Number(lire('SELECT count(*) FROM ordonnance_delivrance'))).toBe(1);

  // La vente n'est pas clôturée : elle ne consomme rien, il reste 4 à délivrer.
  await expect(panneau.locator('tbody tr').filter({ hasText: 'AMOXICILLINE' }).locator('td').nth(3)).toHaveText('4');

  await panneau.getByRole('button', { name: 'Fermer' }).click();

  // Le client a une ordonnance en cours : la pastille du bouton le dit, sans rien imposer.
  const bouton = page.locator('app-ordonnance-bouton').filter({ visible: true }).first();
  await expect(bouton).toContainText('1');
  await expect(bouton.getByRole('button', { name: /1 en cours/ })).toBeVisible();

  await assurerPanierVide(page);
});
