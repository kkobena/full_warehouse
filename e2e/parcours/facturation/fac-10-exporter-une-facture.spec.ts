import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La facture n'existe vraiment que sortie de l'application : c'est le PDF qui part chez
 * l'assureur, avec les bons qui la composent, et c'est lui qui sera opposé en cas de litige.
 *
 * Le document reprend la facture telle qu'elle a été figée à l'édition — numéro, période,
 * payeur, lignes — et non l'état du moment : une facture réglée depuis reste identique à
 * celle qui a été envoyée.
 *
 * Parcours en LECTURE : il montre l'action sans produire le fichier.
 */
scenario('FAC-10', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /^Factures/);
    await expect(lignes.first()).toBeVisible();
  });

  await etape(2, async () => {
    // L'export est offert sur la ligne, sans avoir à ouvrir la facture.
    await expect(lignes.first().getByRole('button', { name: 'Exporter la facture en PDF' }))
      .toBeVisible();
  });
});
