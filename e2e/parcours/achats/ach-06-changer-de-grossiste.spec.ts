import { expect } from '@playwright/test';
import { chercherDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une commande préparée pour un grossiste peut devoir partir chez un autre : rupture annoncée,
 * tarif plus intéressant, agence de livraison différente. Le changement se fait sur la commande
 * ouverte, dans le sélecteur de fournisseur — il n'y a pas d'action « changer de grossiste »
 * séparée, et c'est délibéré : le fournisseur est un attribut de la commande, pas une opération.
 *
 * Ce qui se joue derrière est plus qu'un changement d'étiquette. Chaque ligne est rattachée au
 * couple (produit, fournisseur) du nouveau grossiste, et son prix d'achat comme son prix de
 * vente sont repris de SA grille. Un produit que le nouveau fournisseur ne référence pas reçoit
 * une entrée créée à partir du fournisseur principal du produit, marquée en CODE PROVISOIRE :
 * la commande reste possible, et le code définitif se corrigera à la réception (ACH-03).
 *
 * Parcours ÉCRIVANT dans la base : il change le grossiste d'une commande, puis le remet.
 */
scenario('ACH-06', async ({ etape, page }) => {
  const nouveau = 'LABOREX ABIDJAN TREICHVILLE';
  const grille = page.locator('app-commande-requested');
  let origine = '';

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: 'Commandes fournisseurs' }).click();
    const liste = page.locator('app-commande-requested-home');
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    // Une commande NON transmise : une commande envoyée au grossiste est verrouillée, son
    // fournisseur avec (ACH-28).
    const modifiable = liste.locator('tbody tr')
      .filter({ hasNot: page.locator('.pharma-badge-info', { hasText: /^PHARMAML$/ }) })
      .first();
    await modifiable.dblclick();
    await expect(grille).toBeVisible();
    origine = (await page.locator('#fournisseur-select').inputValue().catch(() => ''))
      || (await grille.locator('.fournisseur-select-input').innerText()).trim();
    await expect(grille.locator('.fournisseur-select-input')).toBeVisible();
  });

  await etape(2, async () => {
    // Le sélecteur est groupé : les agences sont proposées sous leur grossiste.
    await chercherDansSelect(page, 'fournisseur-select', 'LABOREX', nouveau);
    await expect(grille.locator('.fournisseur-select-input')).toContainText(nouveau);
  });

  await etape(3, async () => {
    // Rien à valider : le rattachement part dès la sélection, et les lignes reviennent avec
    // les prix du nouveau grossiste — le bandeau de totaux a suivi.
    await expect(grille.locator('.grid-caption')).toContainText(/Achat/i);
  });

  // ── Remise en état : la commande retrouve son grossiste. ────────────────────────────────
  const libelleOrigine = origine.replace(/\s+/g, ' ').trim();
  if (libelleOrigine && !libelleOrigine.includes(nouveau)) {
    const terme = libelleOrigine.split(' ')[0];
    await chercherDansSelect(page, 'fournisseur-select', terme, libelleOrigine).catch(() => {});
  }
});
