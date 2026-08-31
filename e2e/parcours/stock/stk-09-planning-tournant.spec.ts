import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * L'inventaire annuel ferme l'officine et fatigue tout le monde pour un résultat qu'on
 * n'exploite qu'une fois. L'inventaire TOURNANT prend le problème autrement : un morceau du
 * stock à la fois, à intervalle régulier, sans jamais fermer.
 *
 * Le planning porte cette régularité : un libellé, une fréquence — de quotidien à trimestriel
 * — et un critère de rotation qui décide de ce qu'on compte à chaque échéance. Par rayon,
 * par famille, ou par classification ABC : les produits qui font le chiffre d'affaires se
 * comptent plus souvent que les autres (STK-28).
 *
 * Le planning est ce qui se répète ; les inventaires, eux, sont générés à chaque échéance
 * (STK-10) et se retrouvent dans l'onglet « En cours ».
 *
 * Parcours ÉCRIVANT dans la base : il crée un planning.
 */
scenario('STK-09', async ({ etape, page }) => {
  const modale = page.locator('.modal-content');
  const libelle = `Parcours STK-09 — hebdo rayon ${Date.now().toString().slice(-5)}`;

  await etape(1, async () => {
    await page.goto('/inventaire');
    await ouvrirOnglet(page, /Tournant/);
    await page.getByRole('button', { name: 'Nouveau planning' }).click();
    await expect(modale).toBeVisible();
  });

  await etape(2, async () => {
    await modale.locator('#libelle').fill(libelle);
    // La fréquence dit QUAND, le critère dit QUOI.
    await choisirDansSelect(page, 'frequence', 'Hebdomadaire');
    await choisirDansSelect(page, 'critere', 'Par rayon');
  });

  await etape(3, async () => {
    await modale.getByRole('button', { name: 'Créer' }).click();
    // Le planning rejoint la liste des tournants : il produira ses inventaires tout seul.
    await expect(page.locator('tbody tr').filter({ hasText: libelle }).first()).toBeVisible();
  });
});
