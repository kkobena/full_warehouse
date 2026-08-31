import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un référentiel se corrige souvent : une faute de frappe dans un libellé, un code aligné sur
 * celui du grossiste, une famille rattachée à la mauvaise catégorie.
 *
 * La modification se répercute PARTOUT d'un coup — sur les fiches produit, dans les filtres,
 * dans les rapports — puisque c'est le même objet qui est référencé. C'est ce qui rend le
 * geste utile, et c'est aussi ce qui demande d'y regarder à deux fois : renommer une famille
 * réécrit l'historique des rapports qui la citent.
 *
 * Parcours en LECTURE : il ouvre la fiche et montre qu'elle est modifiable, sans enregistrer.
 */
scenario('RFD-02', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modal = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/famille-produit');
    await expect(lignes.first()).toBeVisible();
    await lignes.first().locator('app-button[ngbtooltip="Modifier"] button').first().click();
    await expect(modal).toBeVisible();
  });

  await etape(2, async () => {
    // La fiche s'ouvre pré-remplie : on corrige, on ne ressaisit pas.
    await expect(modal.locator('#libelle')).not.toHaveValue('');
    await modal.locator('#libelle').fill('ANTALGIQUES ET ANTIPYRETIQUES');
  });

  await etape(3, async () => {
    await expect(modal.getByRole('button', { name: 'Enregistrer' })).toBeEnabled();
  });
});
