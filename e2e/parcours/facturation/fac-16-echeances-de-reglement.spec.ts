import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une créance n'est en retard que par rapport à une DATE. Chaque tiers payant a son délai de
 * règlement négocié — trente jours pour les mutuelles publiques, quarante-cinq pour certaines
 * assurances privées — et c'est ce délai qui donne l'échéance de chaque facture.
 *
 * Le rapprochement met donc l'échéance en regard de la facture, et signale en rouge les
 * lignes dépassées. Un compteur « en retard » les résume par organisme : c'est lui qui décide
 * de l'ordre des relances, bien avant le montant.
 *
 * Parcours en LECTURE.
 */
scenario('FAC-16', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Rapprochement/);
    await rechercher(page);
    await expect(lignes.first()).toBeVisible();
  });

  await etape(2, async () => {
    // Le détail d'un organisme : facture par facture, avec son échéance.
    await lignes.first().locator('button:has(.pi-eye)').first().click();
    await expect(contenu).toContainText('Échéance');
    await expect(contenu).toContainText('N° Facture');
  });
});
