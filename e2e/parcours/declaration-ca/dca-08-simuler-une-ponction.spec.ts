import { expect } from '@playwright/test';
import { ouvrirOnglet, saisirDate, saisirMontant } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La simulation est la étape qui rend la décision informée : elle montre l'effet exact d'un
 * objectif avant qu'il n'engage quoi que ce soit.
 *
 * Elle répartit le montant visé sur les ventes éligibles — celles qu'aucune exclusion n'a déjà
 * réduites — et rend quatre chiffres : ce qui est réellement ponctionnable, ce qui l'a été, le
 * taux moyen qui en résulte, et le chiffre d'affaires déclaré qui en découle. L'aperçu des
 * premières ventes touchées permet de vérifier que la réduction se répartit au lieu de se
 * concentrer.
 *
 * Rien n'est écrit : c'est ce qui la sépare de la validation (DCA-09). On peut donc la rejouer
 * autant de fois qu'on veut, en changeant l'objectif ou le plafond.
 */
scenario('DCA-08', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const debut = new Date();
  debut.setMonth(debut.getMonth() - 2, 1);
  const fin = new Date(debut.getFullYear(), debut.getMonth() + 1, 0);

  await etape(1, async () => {
    await page.goto('/declaration-ca');
    await ouvrirOnglet(page, 'Ponction');
    await saisirDate(page, 'ponction-du', debut);
    await saisirDate(page, 'ponction-au', fin);
    // Un taux modeste : la ponction se répartit d'autant mieux qu'elle est faible.
    await saisirMontant(page, '#valeur', '2');
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Simuler' }).click();
    await expect(contenu).toContainText(/Ponctionné/i, { timeout: 30000 });
  });

  await etape(3, async () => {
    // Les quatre chiffres qui font la décision : ce qui était visé, ce qui a pu l'être, le
    // taux moyen qui en résulte et le CA déclaré qui en découle.
    await expect(contenu).toContainText(/Objectif/i);
    await expect(contenu).toContainText(/Taux moyen/i);
    await expect(contenu).toContainText(/CA déclaré/i);
    // Le nombre de ventes touchées face au nombre d'éligibles : c'est lui qui dit si la
    // réduction se répartit ou se concentre.
    await expect(contenu).toContainText(/vente\(s\) impactée\(s\) sur/i);
    // Et l'aperçu, ligne à ligne, de ce que chaque vente céderait.
    await expect(contenu).toContainText(/Aperçu des premières ventes/i);
  });
});
