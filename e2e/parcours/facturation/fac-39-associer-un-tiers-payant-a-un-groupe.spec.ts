import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * L'association décide de deux choses à la fois : de quelles règles l'organisme hérite, et
 * s'il entre dans une facture groupée ou reste facturé seul.
 *
 * C'est la seconde qui compte au quotidien. Un organisme rattaché à un groupe disparaît des
 * factures individuelles pour se fondre dans celle du groupe — et l'en retirer le fait
 * réapparaître. Une erreur ici se voit un mois plus tard, sur une facture qui manque.
 *
 * Le rattachement se fait depuis la fiche de l'organisme, pas depuis le groupe : c'est
 * l'organisme qui appartient à un groupe, jamais l'inverse.
 *
 * Parcours en LECTURE : il montre le choix sans le modifier.
 */
scenario('FAC-39', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/tiers-payant');
    await expect(lignes.first()).toBeVisible();
    await lignes.first().locator('button:has(.pi-pencil)').first().click();
    await expect(page.locator('#field_libelle')).not.toHaveValue('');
  });

  await etape(2, async () => {
    // Le champ « groupe » est un choix parmi les groupes existants — et le vider suffit à
    // ramener l'organisme en facturation individuelle.
    const champGroupe = page.locator('ng-select', { has: page.locator('#groupeTiersPayantId') });
    await expect(champGroupe).toBeVisible();
    await champGroupe.click();
    await expect(page.locator('.ng-option').first()).toBeVisible({ timeout: 15000 });
    await page.keyboard.press('Escape');
  });
});
