import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le modèle faisait de l'ouverture du panneau une étape distincte : il s'ouvre en réalité dès
 * la sélection de la facture. L'étape 2 a été reformulée sur ce que le panneau apporte
 * vraiment — ses trois onglets.
 */
scenario('FAC-09', async ({ etape, page }) => {
  await page.goto('/facturation');
  await ouvrirOnglet(page, /^Factures/);
  await expect(page.getByRole('columnheader', { name: 'N° Facture' })).toBeVisible();

  await etape(1, async () => {
    await page.locator('tbody tr').first().click();
    // Le panneau maître-détail liste les bons facturés : sa colonne « N° Bon » prouve
    // qu'on regarde bien le contenu de la facture, et non la liste restée en place.
    await expect(page.getByRole('columnheader', { name: 'N° Bon' })).toBeVisible();
  });

  await etape(2, async () => {
    // « Versements » : les encaissements adossés à la facture par 14b_reglements.sql.
    // Sans eux, cet onglet serait vide alors que la facture s'affiche « Réglé ».
    await ouvrirOnglet(page, 'Versements');
    await expect(page.locator('.detail-column')).toBeVisible();
  });
});
