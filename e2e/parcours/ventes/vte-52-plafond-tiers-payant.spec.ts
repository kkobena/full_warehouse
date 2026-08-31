import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le plafond est ce qui distingue une prise en charge annoncée d'une prise en charge réelle.
 * L'organisme rembourse 70 % — jusqu'à un montant mensuel par assuré. Au-delà, la part
 * patient enfle sans que personne ne l'ait décidé au comptoir : c'est exactement le genre de
 * surprise qu'un manuel doit désamorcer.
 *
 * Le parcours n'encaisse PAS : le plafonnement se lit avant la validation, et ne pas
 * finaliser laisse la consommation de l'assuré intacte — donc le parcours rejouable.
 */
scenario('VTE-52', async ({ etape, page }) => {
  // MOUSSA TRAORE (CNAM01-000098) a déjà consommé une bonne part de son plafond mensuel.
  // Le matricule, et non le nom : cinq assurés se prénomment MOUSSA.
  const matricule = 'CNAM01-000098';
  const produit = 'DOLIPRANE 1G';
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
    await expect(page.locator('#main-content')).toContainText(matricule);

    // Le numéro de bon, puis la vente : `Entrée` sur le dernier bon enchaîne sur la
    // recherche produit.
    const bon = page.getByPlaceholder('Numéro de bon');
    await bon.click();
    await bon.pressSequentially(numeroBon, { delay: 25 });
    await bon.press('Enter');
    await expect(page.locator('#produitbox')).toBeFocused();
    await chercherProduit(page, produit);
    await ajouterAuPanier(page, '2');
    await expect(lignes.first()).toContainText(produit);
  });

  await etape(2, async () => {
    const contenu = page.locator('#main-content');
    // Sans plafond, l'organisme prendrait 70 % de 25 950, soit 18 165. Mais CNAM plafonne
    // chaque assuré à 50 000 par mois et celui-ci en a déjà consommé 35 000 : il ne lui
    // reste que 15 000 de droit, et c'est ce montant, et lui seul, qui est pris en charge.
    // Ces chiffres sont FIGÉS par 13b_plafonds.sql pour rester citables dans le manuel.
    await expect(contenu).toContainText('25 950');
    await expect(contenu).toContainText('15 000');
    await expect(contenu).not.toContainText('18 165');
    // Et la différence retombe sur le patient : 25 950 − 15 000 = 10 950.
    await expect(contenu).toContainText('10 950');
    // L'application ne se contente pas de calculer : elle DIT qu'elle a plafonné, et de
    // combien. Sans ce message, le caissier croirait à une erreur de taux.
    await expect(contenu).toContainText(/plafonné à\s*15\s?000/i);
  });

  // ── Remise en état : la vente est abandonnée, la consommation de l'assuré inchangée. ──
  await page.getByRole('button', { name: 'Annuler' }).click();
  await expect(modale).toBeVisible();
  await modale.getByRole('button', { name: 'Oui' }).click();
});
