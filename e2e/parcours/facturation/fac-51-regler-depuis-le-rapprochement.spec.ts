import { expect } from '@playwright/test';
import { ouvrirFactureARegler, ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le rapprochement est l'écran où l'on découvre l'écart ; c'est donc là qu'on veut le solder,
 * sans repasser par la liste des factures pour retrouver celle qu'on vient de voir.
 *
 * Chaque facture non réglée y porte donc son bouton « Régler », qui ouvre la même saisie que
 * depuis la facture (FAC-11) — montant, mode, date — et le rapprochement se recalcule
 * derrière : l'écart diminue, le taux de recouvrement monte, la ligne change de statut.
 *
 * Parcours en LECTURE : il ouvre la saisie sans imputer de règlement fictif.
 */
scenario('FAC-51', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  let aRegler: import('@playwright/test').Locator;

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Rapprochement/);
    await rechercher(page);
    // Un organisme dont au moins une facture reste à régler : ceux qui viennent en tête
    // peuvent n'avoir que des factures soldées.
    aRegler = await ouvrirFactureARegler(page);
    await expect(contenu).toContainText('N° Facture');
  });

  await etape(2, async () => {
    // Le bouton n'existe que sur une facture qui n'est pas soldée.
    await expect(aRegler).toBeVisible();
    await aRegler.click();
  });

  await etape(3, async () => {
    // La même saisie que depuis la facture : montant, mode, date.
    const modale = page.locator('.modal-content');
    await expect(modale).toBeVisible();
    await expect(modale).toContainText(/Montant/);
  });

  await etape(4, async () => {
    // Une fois validé, le rapprochement se recalcule : écart, taux et statut suivent.
    await expect(page.locator('.modal-content')).toContainText(/Régler|Valider/);
  });
});
