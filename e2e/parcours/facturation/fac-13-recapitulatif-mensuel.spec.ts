import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le récapitulatif mensuel est l'état qu'on présente en réunion : par organisme et par mois,
 * ce qui a été FACTURÉ, ce qui a été RÉGLÉ, ce qui RESTE — et le solde cumulé, qui reporte
 * l'ardoise du mois précédent.
 *
 * Ce report est ce qui le distingue d'un simple total : une créance de janvier non réglée
 * pèse encore en mars, et c'est le solde cumulé qui le montre. Le solde N-1 sert de point de
 * départ vérifiable — sans lui, un écart d'ouverture se propagerait sans qu'on sache d'où il
 * vient.
 *
 * Parcours en LECTURE.
 */
scenario('FAC-13', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Récapitulatif/);
    // Les quatre totaux de la période retenue.
    await expect(contenu).toContainText('Total facturé');
    await expect(contenu).toContainText('Total réglé');
    await expect(contenu).toContainText('Restant dû');
    await expect(contenu).toContainText('Solde cumulé');
  });

  await etape(2, async () => {
    // Le détail par organisme et par mois : c'est là que se lit le report d'un mois sur
    // l'autre.
    await rechercher(page);
    await expect(contenu).toContainText('Organisme');
    await expect(contenu).toContainText('Facturé');
    await expect(contenu).toContainText('Réglé');
    await expect(contenu).toContainText('Restant');
  });
});
