import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un assuré couvert par deux organismes — une assurance principale et une complémentaire.
 *
 * Deux choses sont à comprendre, et l'écran les montre l'une après l'autre :
 *
 *  1. les organismes viennent de la FICHE du client : les sélectionner les charge tous, avec
 *     leur rang (RO, RC1), leur taux et leur propre champ de bon. Rien à ressaisir ;
 *  2. la vente reste modifiable. Le client ne présente pas sa mutuelle ? On retire le
 *     complémentaire, et la part patient monte aussitôt. Il la retrouve ? On le rajoute
 *     depuis la liste de ses organismes.
 *
 * Les taux ne se cascadent pas : chacun prend son pourcentage du TOTAL — 70 % + 20 % ici,
 * soit 90 % — et le patient paie le reste. Le manuel doit le dire, sinon le pharmacien
 * calcule faux.
 *
 * Parcours ÉCRIVANT dans la base : il crée une vente en cours, abandonnée en fin de parcours.
 */
scenario('VTE-44', async ({ etape, page }) => {
  const produit = 'DOLIPRANE ADULTES 1000';
  // AWA TRAORE : ASACI à 70 % (rang RO) et SUNU à 20 % (rang RC1). Le cumul plafonne à
  // 90 %, il reste donc une part patient — sans elle, le retrait du complémentaire ne se
  // lirait nulle part. Ces contrats sont figés par `04_clients.sql`.
  const matricule = 'ASA01-000003';
  const modale = page.locator('.modal-content');
  const contenu = page.locator('#main-content');
  const bandeau = page.locator('app-insurance-data-bar');
  const totaux = page.locator('app-sale-summary');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    await page.getByRole('tab', { name: /Assurance/ }).click();
    const recherche = page.getByPlaceholder('Rechercher un client assuré');
    await recherche.fill('TRAORE');
    await recherche.press('Enter');
    await expect(modale).toContainText('CLIENTS ASSURÉS');
    await modale.locator('tbody tr').filter({ hasText: matricule }).first().dblclick();
    await expect(modale).toBeHidden();

    // Les DEUX organismes sont là, chacun avec son rang et son taux : c'est l'image qui
    // prouve que la couverture double est prise en compte avant même le premier produit.
    await expect(bandeau).toContainText(matricule);
    await expect(bandeau).toContainText('ASACI');
    await expect(bandeau).toContainText('SUNU');
    await expect(bandeau).toContainText(/Taux\s*:\s*70/);
    await expect(bandeau).toContainText(/Taux\s*:\s*20/);
  });

  await etape(2, async () => {
    await chercherProduit(page, produit);
    await ajouterAuPanier(page, '2');
    await expect(lignes.first()).toContainText(produit);

    // 2 × 8 580 = 17 160. ASACI en prend 70 % (12 012), SUNU 20 % (3 432) : le pavé des
    // totaux nomme chaque organisme et sa part, puis leur somme (15 444), et ne laisse que
    // 1 715 à encaisser.
    await expect(totaux).toContainText('12 012');
    await expect(totaux).toContainText('3 432');
    await expect(totaux).toContainText('15 444');
    await expect(totaux).toContainText('1 715');
  });

  await etape(3, async () => {
    // Le retrait passe par une confirmation nommée : on ne fait pas disparaître une prise en
    // charge d'un clic distrait.
    await page.getByRole('button', { name: 'Supprimer ce tiers payant' }).last().click();
    await expect(modale).toContainText('Êtes-vous sûr de vouloir supprimer le tiers payant SUNU');
    await modale.getByRole('button', { name: 'Oui' }).click();
    await expect(modale).toBeHidden();

    // SUNU quitte le bandeau ET les totaux : l'assurance retombe à 12 012, et les 3 432 que
    // la mutuelle prenait passent au patient — 5 150 à encaisser au lieu de 1 716.
    await expect(bandeau).not.toContainText('SUNU');
    await expect(totaux).toContainText('12 012');
    await expect(totaux).toContainText('5 150');
  });

  await etape(4, async () => {
    await page.getByRole('button', { name: 'Ajouter un tiers payant complémentaire' }).click();
    await expect(modale).toContainText('AJOUTER UN TIERS PAYANT');
    // La liste ne propose que les organismes de la FICHE du client qui ne sont pas déjà sur
    // la vente : on ne peut pas inventer une couverture au comptoir.
    await choisirDansSelect(page, 'tiersPayant', 'SUNU');
    // Le bon est propre à chaque organisme, et doit être unique par client.
    await page.locator('#numBon').fill('BON' + Date.now().toString().slice(-9));
    await modale.getByRole('button', { name: 'Ajouter' }).click();
    await expect(modale).toBeHidden();

    // Tout revient en place : la mutuelle reprend ses 3 432, le patient ses 1 716.
    await expect(bandeau).toContainText('SUNU');
    await expect(totaux).toContainText('3 432');
    await expect(totaux).toContainText('1 715');
  });

  // ── Remise en état : la vente est abandonnée. ────────────────────────────────────────
  await page.locator('app-sale-actions').getByRole('button', { name: 'Annuler' }).click();
  await expect(modale).toBeVisible();
  await modale.getByRole('button', { name: 'Oui' }).click();
  await expect(contenu).toContainText(/Panier vide|Sélectionnez un client assuré/i);
});
