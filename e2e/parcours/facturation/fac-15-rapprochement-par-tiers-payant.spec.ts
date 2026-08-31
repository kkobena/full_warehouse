import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le rapprochement confronte, payeur par payeur, ce qui a été FACTURÉ à ce qui a été RÉGLÉ, et
 * nomme la différence : l'ÉCART. C'est l'état qu'on ouvre avant d'appeler un assureur, parce
 * qu'il donne le chiffre à discuter et le taux de recouvrement qui le met en perspective.
 *
 * Un écart n'est pas nécessairement un impayé : il peut venir d'un règlement non imputé, d'un
 * avoir non déduit, d'une facture rejetée. Le distinguer demande de descendre au détail — et
 * c'est ce que permet le dépliage par période.
 *
 * Parcours en LECTURE.
 */
scenario('FAC-15', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Rapprochement/);
    // Les quatre repères : facturé, réglé, écart, et ce qui a dépassé son échéance.
    await expect(contenu).toContainText('Total facturé');
    await expect(contenu).toContainText('Total réglé');
    await expect(contenu).toContainText('Écart global');
    await expect(contenu).toContainText('En retard');
  });

  await etape(2, async () => {
    // Le détail par organisme et par période, avec le taux de recouvrement.
    await rechercher(page);
    await expect(contenu).toContainText('Organisme');
    await expect(contenu).toContainText('Écart');
    await expect(contenu).toContainText('Taux');
  });
});
