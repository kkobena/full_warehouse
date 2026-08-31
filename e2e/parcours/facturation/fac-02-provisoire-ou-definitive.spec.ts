import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une facture définitive CONSOMME ses lignes de vente : elles ne pourront plus jamais partir
 * sur une autre facture. C'est ce qui garantit qu'un bon n'est pas facturé deux fois — et ce
 * qui rend l'opération irréversible.
 *
 * La facture provisoire est l'inverse : un aperçu, qu'on envoie au correspondant pour accord
 * ou qu'on relit soi-même, et qui laisse les lignes disponibles. On la produit autant de fois
 * qu'on veut, et la définitive vient ensuite.
 *
 * Le choix se fait à la génération, par un interrupteur qui dit son état en toutes lettres —
 * « Oui » ou « Non » — plutôt que par une case à cocher qu'on lit de travers.
 *
 * Parcours en LECTURE.
 */
scenario('FAC-02', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Édition de factures/);
    // Par défaut, l'édition est DÉFINITIVE : c'est le geste courant, et le provisoire est
    // l'exception qu'on demande explicitement.
    await expect(contenu).toContainText(/Factures provisoires/);
    const interrupteur = contenu.locator('.toggle-wrap').first();
    await expect(interrupteur).toContainText('Non');
  });

  await etape(2, async () => {
    // Basculer en provisoire : la facture produite laissera ses lignes réutilisables.
    const interrupteur = contenu.locator('.toggle-wrap').first();
    await interrupteur.locator('app-switch, input, .form-check-input').first().click();
    await expect(interrupteur).toContainText('Oui');
  });
});
