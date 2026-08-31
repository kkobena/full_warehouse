import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La vente assurance est le cœur du métier ivoirien : le client ne paie que sa part, et
 * l'officine porte le reste en créance sur l'organisme jusqu'à la facturation. Trois éléments
 * doivent donc figurer sur la même image — le total, la part prise en charge et le reste à
 * encaisser — faute de quoi le lecteur ne comprend pas ce qu'il regarde.
 *
 * Parcours ÉCRIVANT dans la base : il enregistre une vente et constitue une créance tiers
 * payant réelle.
 */
scenario('VTE-04', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 1G';
  // Un assuré à UN seul organisme, sans plafond mensuel : la répartition est alors la règle
  // nue — 70 % / 30 %. Le plafonnement fait l'objet de VTE-52, la couverture à deux
  // organismes de VTE-44.
  const matricule = 'CIE01-000118';
  // Le numéro de bon doit être UNIQUE par client : l'application refuse un bon déjà
  // employé (« numBonAlreadyUse »). Un numéro figé rendrait le parcours jouable une
  // seule fois, jusqu'à la prochaine restauration de l'instantané.
  const numeroBon = 'BON' + Date.now().toString().slice(-9);
  const modale = page.locator('.modal-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await assurerCaisseOuverte(page);

  await etape(1, async () => {
    await assurerPanierVide(page);
  await page.goto('/sales-home');
    await expect(page.locator('#produitbox')).toBeVisible();
    await page.getByRole('tab', { name: /Assurance/ }).click();

    // La recherche d'un assuré ouvre une liste : elle montre le matricule ET le taux de
    // chaque organisme, ce qui permet de vérifier la couverture avant de servir.
    const recherche = page.getByPlaceholder('Rechercher un client assuré');
    await recherche.fill('ASSI');
    await recherche.press('Enter');
    await expect(modale).toContainText('CLIENTS ASSURÉS');
    // Sélection par MATRICULE : plusieurs assurés partagent le même nom, et le matricule est
    // justement ce qui les distingue au comptoir. Le double-clic vaut sélection — l'infobulle
    // du bouton d'action le dit aussi.
    await modale.locator('tbody tr').filter({ hasText: matricule }).first().dblclick();
    await expect(modale).toBeHidden();
    // Le bandeau porte désormais l'assuré, son matricule et le taux de son organisme.
    await expect(page.locator('#main-content')).toContainText(matricule);
    await expect(page.locator('#main-content')).toContainText(/Taux\s*:\s*70/);
  });

  await etape(2, async () => {
    // Le numéro de bon est la pièce qui rattache la vente au dossier de l'organisme : sans
    // lui, la facturation ne saurait pas quoi joindre.
    const bon = page.getByPlaceholder('Numéro de bon');
    await bon.click();
    // Frappe caractère par caractère : le champ porte `appKeyFilter="alphanum"`, qui filtre
    // les touches — une valeur posée d'un bloc ne l'atteint pas. Le filtre explique aussi
    // l'absence de tirets dans le numéro : ils seraient refusés à la saisie.
    await bon.pressSequentially(numeroBon, { delay: 30 });
    await expect(bon).toHaveValue(numeroBon);
    await bon.press('Enter');
  });

  await etape(3, async () => {
    // `Entrée` sur le dernier bon donne le focus à la recherche produit : c'est la
    // continuité de frappe voulue au comptoir. Attendre ce focus évite d'écrire dans un
    // champ que l'application s'apprête à reprendre.
    await expect(page.locator('#produitbox')).toBeFocused();
    await chercherProduit(page, produit);
    await ajouterAuPanier(page, '2');

    // 2 × 12 975 = 25 950, dont 70 % pour l'organisme : 18 165 d'assurance et 7 785 à la
    // charge du patient. Les trois montants doivent apparaître ENSEMBLE — c'est leur
    // rapprochement qui fait la démonstration.
    await expect(lignes.first()).toContainText(produit);
    await expect(page.locator('#main-content')).toContainText(/TOTAL ASSURANCE/i);
    await expect(page.locator('#main-content')).toContainText('18 165');
    await expect(page.locator('#main-content')).toContainText('7 785');
  });

  await etape(4, async () => {
    // Seule la part patient s'encaisse : le reste devient une créance sur l'organisme.
    await page.locator('#CASH').fill('10000');
    await page.getByRole('button', { name: 'Finaliser' }).click();
    await expect(page.locator('#main-content')).toContainText(/Panier vide|Sélectionnez un client assuré/i);
  });
});
