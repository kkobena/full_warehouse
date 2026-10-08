import { expect, test } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, rattacherUnClient } from '../src/actions';
import { executer, lire, rejouer } from '../src/base-de-donnees';

/**
 * Gestion des prescripteurs depuis le panneau Ordonnance (docs/PLAN-EXTRACTION-ORDONNANCE-OCR.md §13.3) :
 * corriger une fiche, fusionner un doublon, désactiver. Données d'essai « ESSAI E2E » posées en SQL, retirées à la fin.
 * Parcours ÉCRIVANT dans la base : sa vente en cours est abandonnée.
 */
test.afterAll(() => {
  rejouer('scripts/essai-ordonnance/nettoyer.sql');
});

test('prescripteurs : corriger, fusionner un doublon, désactiver', async ({ page }) => {
  test.setTimeout(180_000);
  rejouer('scripts/essai-ordonnance/nettoyer.sql');
  executer(`INSERT INTO prescripteur (nom, prenom) VALUES ('ESSAI E2E Dupont', 'A.'), ('ESSAI E2E Dupond', 'Alain'), ('ESSAI E2E Martin', NULL);`);

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  await page.goto('/sales-home');
  await chercherProduit(page, 'DOLIPRANE 500');
  await ajouterAuPanier(page, '1');
  await rattacherUnClient(page);
  await page.locator('app-customer-overlay-panel').filter({ visible: true }).first().getByRole('button', { name: /Ordonnances du client/ }).click();
  await page.locator('.offcanvas.show').getByRole('button', { name: 'Prescripteurs' }).click();

  const modale = page.locator('.modal-content');
  await expect(modale).toContainText('Prescripteurs');
  await modale.locator('input[placeholder^="Rechercher par nom"]').fill('ESSAI E2E');
  const lignes = modale.locator('tbody tr');
  await expect(lignes).toHaveCount(3);

  // Correction : la spécialité se retrouve en base.
  await lignes.filter({ hasText: 'Martin' }).getByRole('button', { name: /Modifier/ }).click();
  await modale.locator('app-form-field', { hasText: 'Spécialité' }).locator('input').fill('Cardiologue');
  await modale.getByRole('button', { name: 'Enregistrer' }).click();
  await expect(lignes.filter({ hasText: 'Martin' })).toContainText('Cardiologue');
  expect(lire("SELECT specialite FROM prescripteur WHERE nom = 'ESSAI E2E Martin'")).toBe('Cardiologue');

  // Fusion du doublon « Dupont » dans « Dupond » : la fiche source est désactivée.
  await lignes.filter({ hasText: 'Dupont' }).getByRole('button', { name: /Fusionner/ }).click();
  await modale.locator('ng-select').click();
  await page.locator('.ng-option', { hasText: 'Dupond' }).click();
  await modale.getByRole('button', { name: 'Fusionner' }).click();
  await expect(lignes).toHaveCount(2);
  expect(lire("SELECT actif FROM prescripteur WHERE nom = 'ESSAI E2E Dupont'")).toBe('f');

  // Désactivation.
  await lignes.filter({ hasText: 'Martin' }).getByRole('button', { name: /Désactiver/ }).click();
  await expect(lignes).toHaveCount(1);
  expect(lire("SELECT actif FROM prescripteur WHERE nom = 'ESSAI E2E Martin'")).toBe('f');

  await modale.getByRole('button', { name: 'Fermer' }).last().click();
  await page.locator('.offcanvas.show').getByRole('button', { name: 'Fermer' }).click();
  await assurerPanierVide(page);
});
