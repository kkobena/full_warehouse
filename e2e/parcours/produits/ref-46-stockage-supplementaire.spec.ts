import { expect } from '@playwright/test';
import { chercherAuCatalogue, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une officine qui tient un dépôt ou une seconde implantation gère le MÊME produit à deux
 * endroits, avec deux stocks et deux emplacements distincts. « Ajouter à un autre stockage »
 * est ce qui rend le produit visible et gérable là-bas : sans cela, il n'existe que dans
 * l'officine principale, et le dépôt ne peut ni le recevoir ni le vendre.
 *
 * Parcours en LECTURE : il ouvre la fenêtre d'affectation et montre ce qu'elle demande, sans
 * créer un second emplacement que les parcours de dépôt ne s'attendent pas à trouver.
 */
scenario('REF-46', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const onglet = page.locator('app-produit-rayons-tab');
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    await ouvrirOnglet(page, 'Rayons');
    await expect(onglet).toBeVisible();
  });

  await etape(2, async () => {
    await onglet.getByRole('button', { name: 'Ajouter à un autre stockage' }).click();
    await expect(modale).toBeVisible();
  });

  await etape(3, async () => {
    // Les deux choix vont ensemble : le stockage, puis le rayon qu'on y occupera. Un produit
    // affecté à un stockage sans emplacement y serait introuvable.
    await expect(modale).toContainText(/Stockage|Emplacement|Rayon/i);
  });
});
