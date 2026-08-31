import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Un encours tiers payant de quinze millions ne dit rien tant qu'on ignore son ÂGE. Vingt
 * factures de janvier et vingt factures de la semaine dernière font le même total et n'ont pas
 * le même sens : les premières sont un problème de recouvrement, les secondes un délai normal.
 *
 * Le rapport découpe donc l'encours en quatre tranches d'ancienneté, puis le ventile par
 * organisme — avec, pour chacun, son délai contractuel et un jugement en clair sur son
 * comportement de paiement. Un organisme à 30 jours contractuels dont l'âge moyen est de 98
 * jours ne « paie pas en retard » : il ne respecte pas la convention.
 */
scenario('RPT-25', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await ouvrirRapport(page, 'finance', 'Créances TP');
    await expect(page.getByRole('tab', { name: 'Vieillissement créances TP' })).toBeVisible({ timeout: 20000 });
  });

  await etape(2, async () => {
    await expect(contenu).toContainText(/TOTAL EN COURS/i);
    // Les quatre tranches, avec leur part : c'est la répartition, pas le total, qui alerte.
    await expect(contenu).toContainText(/0 – 30 JOURS/i);
    await expect(contenu).toContainText(/> 90 JOURS/i);
    // Et le détail par organisme, seul niveau où une relance se décide.
    await expect(contenu).toContainText(/délai contractuel/i);
    await expect(contenu).toContainText(/COMPORTEMENT DE PAIEMENT/i);
  });
});
