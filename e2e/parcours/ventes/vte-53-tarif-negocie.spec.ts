import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un assureur peut imposer SON prix : la prise en charge se calcule alors sur un tarif
 * négocié, inférieur au prix catalogue. Le client paie le prix affiché, l'organisme
 * rembourse sur une base plus basse — et la différence tombe sur le patient, sans que le
 * taux affiché (70 %) ait changé. C'est le calcul le moins intuitif de l'écran de vente.
 *
 * Le parcours n'encaisse pas : la démonstration tient dans la répartition, et ne rien
 * finaliser le rend rejouable.
 */
scenario('VTE-53', async ({ etape, page }) => {
  // ZYRTEC 250MG B/12 : 23 110 au catalogue, 19 644 en tarif négocié CNAM
  // (04b_remises_plafonds_tarifs.sql pose le prix de référence à 85 % du catalogue).
  const produit = 'ZYRTEC 250MG';
  const matricule = 'CNAM01-000050';
  // Le numéro de bon doit être UNIQUE par client : l'application refuse un bon déjà
  // employé (« numBonAlreadyUse »). Un numéro figé rendrait le parcours jouable une
  // seule fois, jusqu'à la prochaine restauration de l'instantané.
  const numeroBon = 'BON' + Date.now().toString().slice(-9);
  const modale = page.locator('.modal-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await assurerPanierVide(page);
  await page.goto('/sales-home');
    await expect(page.locator('#produitbox')).toBeVisible();
    await page.getByRole('tab', { name: /Assurance/ }).click();
    const recherche = page.getByPlaceholder('Rechercher un client assuré');
    await recherche.fill('TRAORE');
    await recherche.press('Enter');
    await expect(modale).toContainText('CLIENTS ASSURÉS');
    await modale.locator('tbody tr').filter({ hasText: matricule }).first().dblclick();
    await expect(modale).toBeHidden();

    const bon = page.getByPlaceholder('Numéro de bon');
    await bon.click();
    await bon.pressSequentially(numeroBon, { delay: 25 });
    await bon.press('Enter');
    await expect(page.locator('#produitbox')).toBeFocused();
    await chercherProduit(page, produit);
    await ajouterAuPanier(page, '1');
    await expect(lignes.first()).toContainText(produit);
  });

  await etape(2, async () => {
    const contenu = page.locator('#main-content');
    // Le client est facturé au prix catalogue : 23 110.
    await expect(contenu).toContainText('23 110');
    // Mais l'organisme rembourse 70 % du TARIF NÉGOCIÉ (19 645), soit 13 752 — et non
    // 16 177, qui serait 70 % du catalogue. C'est ce décalage qui fait tout le scénario :
    // le taux affiché n'a pas bougé, la base si.
    await expect(contenu).toContainText('13 752');
    await expect(contenu).not.toContainText('16 177');
  });

  // ── Remise en état : la vente est abandonnée. ────────────────────────────────────────
  await page.getByRole('button', { name: 'Annuler' }).click();
  await expect(modale).toBeVisible();
  await modale.getByRole('button', { name: 'Oui' }).click();
});
