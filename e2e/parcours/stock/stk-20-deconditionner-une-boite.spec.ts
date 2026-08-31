import { expect } from '@playwright/test';
import { chercherAuCatalogue } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le client demande six comprimés, la boîte en contient vingt. Le déconditionnement crée
 * l'unité de vente à partir de la boîte : une boîte quitte le stock du produit conditionné,
 * et vingt unités entrent dans celui du produit « détail ».
 *
 * Les deux produits sont distincts — c'est ce qui permet de leur donner un prix propre — mais
 * liés : le détail n'existe que rattaché à sa boîte, et son stock ne se remplit que par ce
 * geste. C'est pourquoi l'action n'est offerte que sur un produit déclaré déconditionnable ET
 * dont le détail est configuré.
 *
 * Parcours ÉCRIVANT dans la base : il déconditionne une boîte.
 */
scenario('STK-20', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');
  const produit = 'AMOXICILLINE SIROP';

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    const ligne = lignes.filter({ hasText: produit }).first();
    await expect(ligne).toBeVisible();
    await ligne.getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Déconditionner' }).click();
    await expect(modale).toContainText('Déconditionner le produit');
  });

  await etape(2, async () => {
    // Le stock de boîtes disponible, et la quantité à ouvrir : on ne déconditionne pas
    // au-delà de ce qu'on a.
    await expect(modale).toContainText('Stock actuel');
    const champ = modale.locator('#qtyMvt');
    await champ.click();
    await champ.fill('');
    await champ.pressSequentially('1', { delay: 40 });
    await expect(champ).toHaveValue('1');
  });

  await etape(3, async () => {
    await modale.getByRole('button', { name: 'Enregistrer' }).click();
    // La boîte est sortie, les unités sont entrées : le catalogue le montre sur les deux
    // produits, celui de la boîte et celui du détail.
    await expect(modale).toBeHidden({ timeout: 20000 });
  });
});
