import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, rattacherUnClient } from '../../src/actions';
import { rejouer } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Deux produits qui ne se combinent pas : à l'ajout du second, l'application le compare à ce qui
 * est déjà au panier du client et ouvre le contrôle d'ordonnance. On renonce à l'ajout, ou on le
 * maintient — le motif est alors obligatoire, et la décision tracée.
 *
 * Les interactions n'étant pas chargées dans le jeu de démonstration, le parcours pose une version
 * FICTIVE « ESSAI-E2E » (scripts/essai-ordonnance/preparer.sql) et la retire à la fin.
 *
 * Parcours ÉCRIVANT dans la base : la vente en cours est abandonnée en fin de parcours.
 */
scenario('VTE-73', async ({ etape, page }) => {
  const premier = 'ADVIL 400 mg';
  const contreIndique = 'AMOXICILLINE TEVA 1 g';
  const modale = page.locator('.modal-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  rejouer('scripts/essai-ordonnance/nettoyer.sql');
  rejouer('scripts/essai-ordonnance/preparer.sql');
  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  await chercherProduit(page, premier);
  await ajouterAuPanier(page, '1');
  await expect(lignes.first()).toContainText('ADVIL');
  await rattacherUnClient(page);

  await etape(1, async () => {
    await chercherProduit(page, contreIndique);
    await ajouterAuPanier(page, '1');
    await expect(modale).toContainText("Contrôle de l'ordonnance");
    await expect(modale).toContainText('Contre-indication');
    await expect(modale).toContainText('ESSAI-E2E');
  });

  await etape(2, async () => {
    await modale.getByRole('button', { name: 'Ne pas ajouter' }).click();
    await expect(modale).toBeHidden();
    await expect(lignes).toHaveCount(1);
  });

  await etape(3, async () => {
    await chercherProduit(page, contreIndique);
    await ajouterAuPanier(page, '1');
    await expect(modale).toContainText('Contre-indication');
    // Sans motif, le maintien est refusé.
    await modale.getByRole('button', { name: /Ajouter malgré/ }).click();
    await expect(modale).toContainText('Le motif est obligatoire');
    await modale.locator('#motifControle').fill('Prescription maintenue après avis du prescripteur');
    await modale.getByRole('button', { name: /Ajouter malgré/ }).click();
    await expect(modale).toBeHidden();
    await expect(lignes).toHaveCount(2);
  });

  await assurerPanierVide(page);
  rejouer('scripts/essai-ordonnance/nettoyer.sql');
});
