import { expect } from '@playwright/test';
import { chercherAuCatalogue, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le rayon se vide plus vite que la réserve, et l'inverse arrive aussi : le transfert manuel
 * est le geste qui rééquilibre les deux SANS toucher au stock total de l'officine. Rien
 * n'entre, rien ne sort — la marchandise change d'emplacement, et les lots avec elle.
 *
 * La fenêtre annonce la quantité TRANSFÉRABLE, qui n'est pas la quantité disponible : on ne
 * vide jamais complètement un emplacement, et les lots proches de péremption partent en
 * premier — c'est le sens du rappel « FEFO ! » sur le bouton quand un lot devient urgent.
 *
 * Parcours ÉCRIVANT dans la base : il transfère une unité vers la réserve, puis la ramène au
 * rayon. Le stock total de l'officine est le même avant et après — c'est la propriété même du
 * transfert, et c'est ce qui rend le parcours rejouable.
 */
scenario('REF-49', async ({ etape, page }) => {
  const produit = 'ARNICA MONTANA 9CH';
  const onglet = page.locator('app-produit-stock-tab');
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    await ouvrirOnglet(page, 'Stock');
    // Les deux cartes et la flèche entre elles : le transfert se lit avant de se faire.
    await expect(onglet).toContainText('Rayon');
    await expect(onglet).toContainText('Réserve');
  });

  await etape(2, async () => {
    await onglet.getByRole('button', { name: /Vers réserve/ }).click();
    await expect(modale).toBeVisible();
    // Le garde-fou est chiffré : disponible d'un côté, transférable de l'autre.
    await expect(modale).toContainText('Quantité disponible');
    await expect(modale).toContainText('Quantité transférable max');
    await modale.locator('#field_quantity').fill('1');
  });

  await etape(3, async () => {
    await modale.getByRole('button', { name: 'Transférer' }).click();
    await expect(modale).toBeHidden();
    // Les deux cartes se recomptent : ce que le rayon perd, la réserve le gagne.
    await expect(onglet).toContainText('Réserve');
  });

  // ── Remise en état : l'unité repart au rayon, le stock retrouve sa répartition. ─────────
  await onglet.getByRole('button', { name: 'Réapprovisionner rayon' }).click();
  await expect(modale).toBeVisible();
  await modale.locator('#field_quantity').fill('1');
  await modale.getByRole('button', { name: 'Transférer' }).click();
  await expect(modale).toBeHidden();
});
