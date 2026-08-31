import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Les tiers payants se rangent en groupes — mutuelles publiques, assurances privées, réseaux
 * de gestion — et ces groupes correspondent souvent à un même interlocuteur, une même adresse
 * d'envoi, un même délai de règlement. Les facturer un par un serait vingt fois le même geste.
 *
 * Le mode « par groupes » applique à tout le groupe la mécanique de la génération unitaire :
 * même regroupement des bons, même numérotation séquentielle, une facture par tiers payant.
 * Ce n'est pas une facture globale — chaque payeur reçoit la sienne — mais une opération
 * unique.
 *
 * Parcours en LECTURE : il prépare la génération sans l'exécuter.
 */
scenario('FAC-04', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Édition de factures/);
    await expect(contenu).toContainText(/Mode d'édition/);
  });

  await etape(2, async () => {
    await choisirDansSelect(page, 'edMode', "Par groupes et compagnies d'assurances");
    await expect(contenu).toContainText(/Groupes tiers-payants/);
  });

  await etape(3, async () => {
    // Le groupe à facturer, puis la période : les deux bornes commandent quels bons entrent.
    await page.locator('#edGroupe').focus();
    await page.locator('#edGroupe').press('ArrowDown');
    await page.locator('.ng-option').first().click();
    await page.keyboard.press('Escape');
    await rechercher(page);
  });

  await etape(4, async () => {
    // L'édition produira une facture PAR TIERS PAYANT du groupe, chacune numérotée dans la
    // même séquence annuelle : ce n'est pas une facture globale, mais une seule opération.
    await expect(page.getByRole('button', { name: 'Éditer' }).first()).toBeVisible();
  });
});
