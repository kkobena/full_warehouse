import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Les mêmes états que la comptabilité courante, mais sur ce qui a réellement été ENCAISSÉ — et
 * après les exclusions du module.
 *
 * La distinction n'est pas théorique : une vente facturée à un tiers payant est du chiffre
 * d'affaires bien avant d'être de l'argent, et une déclaration assise sur le facturé demande
 * de payer l'impôt sur ce qui n'est pas encore rentré.
 *
 * Trois états partagent cette base — balance de caisse, rapport de TVA, tableau pharmacien —
 * et tous appliquent les mêmes règles d'exclusion que les journaux. C'est ce qui garantit
 * qu'un état et un journal ne peuvent pas se contredire.
 */
scenario('DCA-11', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/declaration-ca');
    await ouvrirOnglet(page, 'Balance caisse (CA encaissé)');
    await expect(contenu).toContainText(/Balance caisse/i);
  });

  await etape(2, async () => {
    await expect(contenu).toContainText(/Du/);
    await expect(contenu).toContainText(/Au/);
  });

  await etape(3, async () => {
    // Le rapport de TVA lit la même base : deux états, une seule assiette.
    await ouvrirOnglet(page, 'Rapport TVA (CA encaissé)');
    await expect(contenu).toContainText(/Rapport TVA/i);
  });
});
