import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un assureur règle par virement global : un montant, une date, et la charge de retrouver
 * quelles factures il couvre. L'historique des règlements répond à cela — c'est le pendant du
 * portefeuille de factures, vu depuis l'argent qui rentre.
 *
 * Chaque ligne rapproche trois montants : ce qui était ATTENDU sur la facture, ce qui a été
 * RÉGLÉ, et ce qui RESTE. C'est cet écart qui alimente la relance, et l'opérateur qui a saisi
 * l'encaissement y figure — un règlement mal imputé se retrouve par là.
 *
 * Les filtres suivent la question qu'on se pose : une période, un tiers payant, un groupe.
 *
 * Parcours en LECTURE.
 */
scenario('FAC-40', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });
  // Les lignes de RÈGLEMENT, par opposition aux lignes de total d'organisme : elles seules
  // portent les actions.
  const reglements = lignes.filter({ has: page.locator('button') });

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Historique règlements/);
    await expect(contenu).toContainText(/Historique des règlements/);
    await expect(contenu).toContainText(/Montant attendu/);
    await expect(contenu).toContainText(/Restant/);
  });

  await etape(2, async () => {
    // Les filtres : une période, une recherche libre, et — sous le sélecteur « Groupés » —
    // le groupe et le tiers payant. C'est ainsi qu'on isole « ce que la CNAM a versé ce
    // trimestre ».
    await expect(contenu).toContainText(/Recherche libre/);
    await expect(contenu).toContainText(/Du/);
    await rechercher(page);
    // Le tableau est GROUPÉ par organisme : la première ligne est un total, pas un règlement.
    await expect(lignes.first()).toBeVisible();
    await expect(reglements.first()).toBeVisible();
  });

  await etape(3, async () => {
    // Le détail d'un règlement : les factures qu'il couvre, et dans quelle proportion.
    await reglements.first().locator('button:has(.pi-eye)').first().click();
    await expect(page.locator('.modal-content')).toBeVisible();
  });
});
