import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un assureur règle rarement d'un coup : un acompte, puis le solde trois mois plus tard, et
 * parfois un reliquat contesté qui ne vient jamais. La facture doit donc porter en
 * permanence trois chiffres cohérents — ce qui était dû, ce qui a été reçu, ce qui reste — et
 * un STATUT qui s'en déduit plutôt que d'être saisi à la main.
 *
 * C'est ce statut qui pilote le reste : « Partiel » maintient la facture dans les relances et
 * lui garde son onglet « Régler » (FAC-11) ; « Réglé » l'en sort. Aucun de ces états ne se
 * choisit — ils tombent du solde.
 *
 * Les versements successifs restent consultables un par un : c'est ce qui permet de répondre
 * à « quand avez-vous reçu quoi ? ».
 *
 * Parcours en LECTURE.
 */
scenario('FAC-12', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /^Factures/);
    // Une facture partiellement réglée : celle qui montre les trois chiffres à la fois.
    await choisirDansSelect(page, 'fhStatut', 'Partiellement réglées/Non réglées');
    await page.keyboard.press('Escape');
    await rechercher(page);
    const partielle = lignes.filter({ hasText: /Partiel/ }).first();
    await expect(partielle).toBeVisible();
    // Montant facturé, montant réglé, restant : les trois se lisent sur la ligne.
    await expect(partielle).toContainText(/\d/);
    await partielle.click();
  });

  await etape(2, async () => {
    // Le détail des versements : chacun avec sa date et son mode.
    await expect(page.getByRole('columnheader', { name: 'N° Bon' })).toBeVisible();
    await ouvrirOnglet(page, 'Versements');
    await expect(page.locator('.detail-column')).toBeVisible();
  });
});
