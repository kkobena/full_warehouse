import { expect } from '@playwright/test';
import {
  ajouterAuPanier,
  assurerCaisseOuverte,
  assurerPanierVide,
  chercherDansSelect,
  chercherProduit,
  rattacherUnClient,
} from '../../src/actions';
import { rejouer } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * L'ordonnance se suit pendant la vente : on la saisit une fois (prescripteur, renouvellements,
 * produits et quantités), on en reprend les produits dans la vente, puis on rattache la vente à
 * l'ordonnance. Le reste à délivrer et les renouvellements restants sont tenus à jour, et le
 * bouton du client annonce combien d'ordonnances sont en cours.
 *
 * Parcours ÉCRIVANT dans la base : l'ordonnance, le prescripteur et le rattachement d'essai sont
 * retirés à la fin (scripts/essai-ordonnance/nettoyer.sql).
 */
scenario('VTE-71', async ({ etape, page }) => {
  const prescrit = 'AMOXICILLINE TEVA 1 g';
  const panneau = page.locator('.offcanvas.show');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  rejouer('scripts/essai-ordonnance/nettoyer.sql');
  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  await chercherProduit(page, 'DOLIPRANE 500');
  await ajouterAuPanier(page, '1');
  await rattacherUnClient(page);

  const ouvrirPanneau = async () => {
    await page
      .locator('app-customer-overlay-panel')
      .filter({ visible: true })
      .first()
      .getByRole('button', { name: 'Ordonnances du client' })
      .click();
    await expect(panneau).toContainText('Ordonnances');
  };

  await etape(1, async () => {
    await ouvrirPanneau();
    await panneau.getByRole('button', { name: 'Nouvelle ordonnance' }).click();
    await expect(panneau).toContainText('Nouvelle ordonnance');
  });

  await etape(2, async () => {
    // Le prescripteur est créé à la volée, puis une ligne de deux boîtes et un renouvellement.
    await panneau.locator('input[placeholder*="nouveau prescripteur"]').fill('ESSAI E2E Docteur');
    await panneau.getByRole('button', { name: 'Créer' }).click();
    await expect(panneau.locator('ng-select').first()).toContainText('ESSAI E2E Docteur');
    await panneau.locator('#ordoRenouvellements').fill('1');
    await chercherDansSelect(page, 'ordoProduit', prescrit, `${prescrit}, comprimé dispersible`);
    await panneau.locator('input[placeholder="Qté"]').fill('2');
    await panneau.getByRole('button', { name: 'Ajouter la ligne' }).click();
    await expect(panneau.locator('tbody tr')).toHaveCount(1);
    await panneau.getByRole('button', { name: 'Enregistrer' }).click();
    // En cours, un renouvellement : deux boîtes par passage, soit quatre à délivrer au total.
    await expect(panneau).toContainText('En cours');
    await expect(panneau).toContainText('1 renouvellement(s) restant(s)');
    await expect(panneau.locator('tbody tr').filter({ hasText: 'AMOXICILLINE' }).locator('td').nth(3)).toHaveText('4');
  });

  await etape(3, async () => {
    // « Ajouter » présélectionne le produit prescrit par le circuit ordinaire de la vente.
    await panneau.locator('tbody tr').filter({ hasText: 'AMOXICILLINE' }).getByRole('button', { name: /Ajouter/ }).click();
    await expect(panneau).toBeHidden();
    await expect(page.locator('#main-content')).toContainText(/Prix\s*:\s*[\d\s]*\d/);
    await ajouterAuPanier(page, '1');
    await expect(lignes.filter({ hasText: 'AMOXICILLINE' }).first()).toBeVisible();
  });

  await etape(4, async () => {
    await ouvrirPanneau();
    await panneau.getByRole('button', { name: 'Rattacher à la vente en cours' }).click();
    await expect(panneau).toContainText('1 vente(s) liée(s)');
    // La vente n'est pas clôturée : rien n'est encore consommé, il reste quatre à délivrer.
    await expect(panneau.locator('tbody tr').filter({ hasText: 'AMOXICILLINE' }).locator('td').nth(3)).toHaveText('4');
    await panneau.getByRole('button', { name: 'Fermer' }).click();
    // Le bouton du client annonce son ordonnance en cours.
    await expect(page.locator('app-ordonnance-bouton').filter({ visible: true }).first().getByRole('button', { name: /1 en cours/ })).toBeVisible();
  });

  await assurerPanierVide(page);
  rejouer('scripts/essai-ordonnance/nettoyer.sql');
});
