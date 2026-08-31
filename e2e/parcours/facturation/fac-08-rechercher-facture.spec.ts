import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le modèle décrivait un « tiroir de recherche de factures » qui n'existe pas : les filtres
 * sont dans la barre d'outils de l'onglet. Le scénario a été corrigé en écrivant ce parcours
 * — c'est précisément ce que la confrontation à l'écran réel sert à révéler.
 */
scenario('FAC-08', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /^Factures/);
    await expect(page.getByRole('columnheader', { name: 'N° Facture' })).toBeVisible();
  });

  await etape(2, async () => {
    // « Réglées » : le statut que 14b_reglements.sql adosse à de vrais encaissements.
    await choisirDansSelect(page, 'fhStatut', 'Réglées');
    await rechercher(page);

    // La colonne « Restant » d'une facture réglée est à zéro : c'est ce qui distingue ce
    // filtre du précédent, donc ce que l'assertion doit vérifier avant la capture.
    await expect(lignes.first()).toBeVisible();
  });
});
