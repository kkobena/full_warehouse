import { expect } from '@playwright/test';
import { ouvrirMenuActions, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La facture n'existe vraiment que sortie de l'application : c'est le PDF qui part chez
 * l'assureur, avec les bons qui la composent, et c'est lui qui sera opposé en cas de litige.
 *
 * Le document reprend la facture telle qu'elle a été figée à l'édition — numéro, période,
 * payeur, lignes — et non l'état du moment : une facture réglée depuis reste identique à
 * celle qui a été envoyée.
 *
 * Parcours en LECTURE des données, mais il produit la facture : c'est le document qui part
 * chez le tiers payant, et un export en échec ne se voit qu'à l'ouverture du fichier.
 */
scenario('FAC-10', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /^Factures/);
    await expect(lignes.first()).toBeVisible();
  });

  await etape(2, async () => {
    // L'export est offert dans le menu d'actions de la ligne, sans avoir à ouvrir la facture.
    const menu = await ouvrirMenuActions(page, lignes.first());
    const exporter = menu.getByRole('button', { name: 'Exporter en PDF' });
    await expect(exporter).toBeVisible();

    const telechargement = page.waitForEvent('download');
    await exporter.click();
    expect((await telechargement).suggestedFilename()).toMatch(/\.(pdf|xlsx|xls|csv)$/i);
  });
});
