import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le cas extrême de la couverture multiple : trois organismes dont les taux cumulent
 * exactement 100 %. Rien ne reste à la charge du patient, et pourtant la vente n'est pas
 * gratuite — l'officine porte trois créances qu'elle facturera séparément.
 *
 * C'est la contrepartie de VTE-44, qui montre le même mécanisme avec une part patient
 * résiduelle. Les deux images se lisent ensemble : c'est le CUMUL des taux qui décide de ce
 * qui reste à encaisser, et le modèle interdit qu'il dépasse 100 %.
 *
 * Parcours ÉCRIVANT dans la base : il enregistre une vente et constitue trois créances.
 */
scenario('VTE-59', async ({ etape, page }) => {
  const produit = 'DOLIPRANE ADULTES 1000';
  // OLIVIER KONE : CNPS 70 % (RO), COLINA 20 % (RC1), CIE SANTE 10 % (RC2).
  const matricule = 'CNPS01-000012';
  const modale = page.locator('.modal-content');
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    await page.getByRole('tab', { name: /Assurance/ }).click();
    const recherche = page.getByPlaceholder('Rechercher un client assuré');
    await recherche.fill('KONE');
    await recherche.press('Enter');
    await expect(modale).toContainText('CLIENTS ASSURÉS');
    await modale.locator('tbody tr').filter({ hasText: matricule }).first().dblclick();
    await expect(modale).toBeHidden();

    await expect(contenu).toContainText(matricule);
    await expect(contenu).toContainText('CNPS');
    await expect(contenu).toContainText('COLINA');
    await expect(contenu).toContainText('CIE SANTE');
  });

  await etape(2, async () => {
    // Trois organismes, trois bons : chacun ouvre son propre dossier de remboursement.
    const bons = page.getByPlaceholder('Numéro de bon');
    await expect(bons).toHaveCount(3);
    const base = Date.now().toString().slice(-8);
    for (let rang = 0; rang < 3; rang++) {
      const bon = bons.nth(rang);
      await bon.click();
      await bon.pressSequentially('BON' + base + rang, { delay: 30 });
      await expect(bon).toHaveValue('BON' + base + rang);
    }
    await bons.last().press('Enter');

    await chercherProduit(page, produit);
    await ajouterAuPanier(page, '2');
    await expect(lignes.first()).toContainText(produit);
  });

  await etape(3, async () => {
    // 2 × 8 580 = 17 160, répartis en 12 012 + 3 432 + 1 716. La somme des trois parts fait
    // le total de la vente : « À ENCAISSER » reste à zéro, et le bouton Finaliser n'attend
    // aucun règlement.
    await expect(contenu).toContainText('12 012');
    await expect(contenu).toContainText('3 432');
    await expect(contenu).toContainText('1 716');
    await expect(contenu).toContainText('À ENCAISSER');

    await page.getByRole('button', { name: 'Finaliser' }).click();
    await expect(contenu).toContainText(/Panier vide|Sélectionnez un client assuré/i);
  });
});
