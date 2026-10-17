import { expect } from '@playwright/test';
import {
  ajouterAuPanier,
  assurerPanierVide,
  chercherDansSelect,
  chercherProduit,
  choisirDansSelect,
  rattacherUnClient,
} from '../../src/actions';
import { executer } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * L'étiquette que le patient relit chez lui. Elle ne porte ni code-barres ni prix : le
 * patient, le prescripteur, l'instruction de prise et la durée — c'est-à-dire ce qu'une boîte
 * seule ne dit pas.
 *
 * Le point important est qu'on ne la SAISIT PAS : elle est tirée de l'ordonnance déjà
 * enregistrée au comptoir (VTE-71). Rien à ressaisir, donc rien qui puisse différer de ce qui
 * a été délivré — c'est la raison pour laquelle l'impression part de l'ordonnance et non d'un
 * formulaire libre. Une étiquette par ligne prescrite, ou une seule quand on n'étiquette
 * qu'une boîte.
 *
 * Parcours ÉCRIVANT : l'ordonnance et le prescripteur d'essai sont créés par le décor, puis
 * retirés à la fin. Aucune vente n'est encaissée.
 */
const PRESCRIPTEUR = 'ESSAI E2E POSOLOGIE';

/** Retire l'ordonnance et le prescripteur d'essai. Les lignes suivent l'ordonnance (cascade). */
function nettoyer(): void {
  executer(
    `DELETE FROM ordonnance WHERE prescripteur_id IN (SELECT id FROM prescripteur WHERE nom LIKE '${PRESCRIPTEUR}%')`,
  );
  executer(`DELETE FROM prescripteur WHERE nom LIKE '${PRESCRIPTEUR}%'`);
}

scenario('VTE-75', async ({ etape, page }) => {
  const prescrit = 'AMOXICILLINE TEVA 1 g';
  const panneau = page.locator('.offcanvas.show');
  const modale = page.locator('.modal-content');

  const ouvrirPanneau = async (): Promise<void> => {
    await page
      .locator('app-customer-overlay-panel')
      .filter({ visible: true })
      .first()
      .getByRole('button', { name: 'Ordonnances du client' })
      .click();
    await expect(panneau).toContainText('Ordonnances');
  };

  // ── Décor : une ordonnance en cours, avec une posologie sur sa ligne. Sans posologie,
  //    l'étiquette n'aurait rien à dire — c'est ce champ-là qu'elle existe pour porter.
  nettoyer();
  await assurerPanierVide(page);
  await chercherProduit(page, 'DOLIPRANE 500');
  await ajouterAuPanier(page, '1');
  await rattacherUnClient(page);
  await ouvrirPanneau();
  await panneau.getByRole('button', { name: 'Nouvelle ordonnance' }).click();
  await panneau.locator('input[placeholder*="nouveau prescripteur"]').fill(PRESCRIPTEUR);
  await panneau.getByRole('button', { name: 'Créer' }).click();
  await expect(panneau.locator('ng-select').first()).toContainText(PRESCRIPTEUR);
  await chercherDansSelect(page, 'ordoProduit', prescrit, `${prescrit}, comprimé dispersible`);
  await panneau.locator('input[placeholder^="Posologie"]').fill('1 cp x 3 /j');
  await panneau.locator('input[placeholder^="Durée"]').fill('7');
  await panneau.locator('input[placeholder="Qté"]').fill('2');
  await panneau.getByRole('button', { name: 'Ajouter la ligne' }).click();
  await expect(panneau.locator('tbody tr')).toHaveCount(1);
  await panneau.getByRole('button', { name: 'Enregistrer' }).click();
  await expect(panneau).toContainText('En cours');
  await panneau.getByRole('button', { name: 'Fermer' }).click();
  await expect(panneau).toBeHidden();

  await etape(1, async () => {
    await ouvrirPanneau();
    // La posologie saisie se lit sous le produit prescrit : c'est elle que l'étiquette
    // reprendra, et c'est pourquoi on l'imprime d'ici plutôt que d'un formulaire à part.
    await expect(panneau.locator('tbody tr').filter({ hasText: 'AMOXICILLINE' })).toContainText('1 cp x 3 /j');
  });

  await etape(2, async () => {
    // L'action est rattachée à l'ORDONNANCE, pas à la vente : on peut réimprimer l'étiquette
    // d'une délivrance passée sans rouvrir le ticket.
    await panneau.getByRole('button', { name: 'Étiquettes' }).first().click();
    await expect(modale).toContainText(/Étiquettes de posologie/i);
    // Sans choix de ligne, une étiquette par ligne prescrite : le défaut est le cas courant.
    await expect(modale).toContainText(/Sans choix, une étiquette par ligne prescrite/i);
  });

  await etape(3, async () => {
    // On n'étiquette qu'une boîte : la ligne se désigne, et seule celle-là sort.
    await choisirDansSelect(page, 'posoLigne', 'AMOXICILLINE');
    const telechargement = page.waitForEvent('download');
    await modale.getByRole('button', { name: 'Imprimer' }).click();
    const fichier = await telechargement;
    expect(fichier.suggestedFilename()).toMatch(/^posologie-\d+\.pdf$/);
  });

  // ── Remise en état : l'ordonnance d'essai et son prescripteur, puis le panier.
  nettoyer();
  await assurerPanierVide(page);
});
