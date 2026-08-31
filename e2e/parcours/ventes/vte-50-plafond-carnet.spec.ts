import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Scénario CORRIGÉ dans le modèle : il annonçait une vente BLOQUÉE. L'application fait mieux,
 * et c'est heureux — bloquer laisserait le client au comptoir sans solution. Elle PLAFONNE :
 * le carnet prend ce qui reste de crédit, le porteur règle la différence sur-le-champ.
 *
 * Le mécanisme est celui de l'assurance (VTE-52), appliqué au crédit d'entreprise : plafond
 * mensuel du porteur, moins ce qu'il a déjà consommé, égale ce que le carnet peut encore
 * couvrir. Un avertissement nomme l'organisme et le montant retenu.
 *
 * Parcours en LECTURE : la vente n'est pas finalisée, la consommation du porteur reste
 * intacte — donc le parcours rejouable.
 */
scenario('VTE-50', async ({ etape, page }) => {
  // FATOUMATA GNAGNE : carnet plafonné à 50 000 par mois, 45 000 déjà consommés. Il lui
  // reste 5 000 — chiffres figés par `13b_plafonds.sql`.
  const matricule = 'CAR01-000021';
  const produit = 'DOLIPRANE 500MG';
  const bandeau = page.locator('app-insurance-data-bar');
  const totaux = page.locator('app-sale-summary');
  const modale = page.locator('.modal-content');

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    await page.getByRole('tab', { name: /Carnet/ }).click();
    const recherche = page.getByPlaceholder('Rechercher un client assuré');
    // Recherche par matricule : un résultat unique est retenu sans passer par la liste.
    await recherche.fill(matricule);
    await recherche.press('Enter');
    await expect(bandeau).toContainText(matricule);
    // Le taux affiché reste 100 % : rien, à ce stade, n'annonce que le crédit est presque
    // épuisé. C'est la vente qui le révélera.
    await expect(bandeau).toContainText('100%');
  });

  await etape(2, async () => {
    await chercherProduit(page, produit);
    await ajouterAuPanier(page, '2');
    await expect(totaux).toContainText('38 620');
  });

  await etape(3, async () => {
    // 38 620 dus, mais seulement 5 000 de crédit restant : le carnet s'arrête là, et les
    // 33 620 restants passent à la charge du porteur, à encaisser tout de suite.
    await expect(totaux).toContainText('5 000');
    await expect(totaux).toContainText('33 620');
    // L'application ne se contente pas de calculer : elle dit qu'elle a plafonné, et de
    // combien.
    await expect(page.locator('[role="alert"], .toast').filter({ hasText: /plafonné/i }).first()).toBeVisible();
  });

  // ── Remise en état : la vente est abandonnée, la consommation du porteur inchangée. ─────
  await page.locator('app-sale-actions').getByRole('button', { name: 'Annuler' }).click();
  await expect(modale).toBeVisible();
  await modale.getByRole('button', { name: 'Oui' }).click();
});
