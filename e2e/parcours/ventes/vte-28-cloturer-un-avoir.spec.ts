import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Scénario CORRIGÉ dans le modèle : il décrivait un avoir « imputé sur le total » au moment
 * d'encaisser une nouvelle vente. L'écran de vente n'offre rien de tel, et pour une bonne
 * raison — un avoir porte sur un PRODUIT, pas sur un montant. Il se solde en servant le
 * client : on le clôture depuis l'onglet « Avoirs clients ».
 *
 * L'application vérifie d'abord le stock du produit dû : clôturer un avoir qu'on ne peut pas
 * honorer reviendrait à effacer une dette sans la payer.
 *
 * Parcours ÉCRIVANT dans la base : il solde un avoir réel.
 */
scenario('VTE-28', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const modale = page.locator('.modal-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/sales-home/gestion');
    await page.getByRole('tab', { name: /Avoirs clients/ }).click();
    await expect(contenu).toContainText('Avoirs clients');
    // Les avoirs OUVERTS : ceux que l'officine doit encore honorer.
    await expect(lignes.first()).toContainText('Ouvert');
  });

  await etape(2, async () => {
    await lignes.first().getByRole('button', { name: 'Clôturer' }).click();
    // La fenêtre rappelle ce qui est dû : le produit, sa quantité, le montant initial et le
    // solde. C'est ce récapitulatif qui permet de servir le bon client au bon produit.
    await expect(modale).toContainText("Clôturer l'avoir");
    await expect(modale).toContainText('Détail de l');
    await expect(modale).toContainText('Montant initial');
  });

  await etape(3, async () => {
    // Le mode de clôture dit COMMENT la dette est éteinte : produit remis, remboursement,
    // geste commercial. Le commentaire garde la trace de l'échange au comptoir.
    const mode = modale.locator('app-select').first();
    await mode.click();
    await page.locator('.ng-option').first().click();
    await modale.locator('textarea').fill('Produit remis au client au comptoir');

    await modale.getByRole('button', { name: /Confirmer la clôture/ }).click();
    await expect(modale).toBeHidden();
  });
});
