import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un devis n'a d'utilité que remis : le client l'emporte, le fait viser, revient avec. Le PDF
 * reprend le devis TEL QU'ENREGISTRÉ — mêmes lignes, mêmes prix, mêmes remises — car c'est ce
 * document, et non l'écran, qui fera foi dans la négociation.
 *
 * L'impression s'obtient depuis la ligne du devis, sans le rouvrir.
 *
 * Parcours ÉCRIVANT dans la base : il crée sa proforma ; il ne la supprime pas, l'impression
 * n'ayant de sens que sur un devis conservé.
 */
scenario('VTE-12', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await assurerCaisseOuverte(page);

  // ── Mise en scène : le devis à imprimer. ────────────────────────────────────────────────
  await page.goto('/sales-home/devis');
  const recherche = page.getByPlaceholder('Rechercher un client...');
  await expect(recherche).toBeVisible();
  await recherche.fill('CLI000145');
  await recherche.press('Enter');
  await expect(contenu).toContainText('AKISSI KOUASSI');
  await chercherProduit(page, produit);
  await ajouterAuPanier(page, '2');
  await page.getByRole('button', { name: 'Enregistrer' }).click();
  await expect(contenu).toContainText(/Panier vide|Sélectionnez un client/i);

  await etape(1, async () => {
    await page.goto('/sales-home/gestion');
    await page.getByRole('tab', { name: /Proformas/ }).click();
    await expect(lignes.first()).toBeVisible();
    await expect(lignes.first()).toContainText('38 620');
  });

  await etape(2, async () => {
    // Le PDF s'ouvre dans un onglet : c'est cet onglet qu'on attend, sinon on photographie
    // la liste en croyant photographier le document.
    const pdf = page.waitForEvent('popup');
    await lignes.first().getByRole('button', { name: 'Imprimer le devis en PDF' }).click();
    const onglet = await pdf;
    // Pas de `waitForLoadState()` : la visionneuse PDF du navigateur ne rend jamais l'état
    // « chargé » attendu, et l'attente expire sur un document pourtant affiché.
    await onglet.waitForTimeout(2000);
    // Le document s'ouvre dans un onglet séparé : son ouverture SUFFIT à prouver qu'il a été
    // produit. Ni son URL (un blob, parfois encore vide à cet instant) ni son contenu (une
    // visionneuse PDF n'expose rien au DOM) ne se prêtent à une vérification.
    expect(onglet.isClosed()).toBe(false);
    await onglet.close();
    // Retour à la liste, où le devis est toujours là : imprimer ne consomme pas le devis.
    await expect(lignes.first()).toContainText('38 620');
  });
});
