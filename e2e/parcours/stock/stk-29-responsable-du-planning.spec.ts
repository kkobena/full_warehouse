import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un inventaire qui n'est à personne n'est fait par personne. Désigner un responsable sur le
 * planning répond à cela : les inventaires qu'il générera lui seront assignés, et l'on saura
 * à qui demander où en est le comptage du rayon dermato.
 *
 * Le champ est facultatif — un planning sans responsable produit des inventaires ouverts à
 * tous —, mais dans une officine à plusieurs préparateurs, il fait la différence entre un
 * tournant qui tourne et un tournant qui s'oublie.
 *
 * Parcours ÉCRIVANT dans la base : il crée un planning avec responsable.
 */
scenario('STK-29', async ({ etape, page }) => {
  const modale = page.locator('.modal-content');
  const libelle = `Parcours STK-29 — responsable ${Date.now().toString().slice(-5)}`;

  await etape(1, async () => {
    await page.goto('/inventaire');
    await ouvrirOnglet(page, /Tournant/);
    await page.getByRole('button', { name: 'Nouveau planning' }).click();
    await modale.locator('#libelle').fill(libelle);
    await choisirDansSelect(page, 'frequence', 'Mensuel');
    await choisirDansSelect(page, 'critere', 'Par famille de produits');
  });

  await etape(2, async () => {
    // Le responsable : celui à qui les inventaires générés seront assignés.
    await modale.locator('#userId').focus();
    await modale.locator('#userId').press('ArrowDown');
    const option = page.locator('.ng-option').first();
    // Le libellé de l'option empile nom, prénom et login : on ne retient que le premier mot
    // pour le retrouver dans le champ, qui n'en affiche qu'une partie.
    const employe = (await option.innerText()).trim().split(/\s+/)[0];
    await option.click();
    await expect(page.locator('ng-select', { has: page.locator('#userId') })).toContainText(employe);
  });

  await etape(3, async () => {
    await modale.getByRole('button', { name: 'Créer' }).click();
    const ligne = page.locator('tbody tr').filter({ hasText: libelle }).first();
    await expect(ligne).toBeVisible();
  });
});
