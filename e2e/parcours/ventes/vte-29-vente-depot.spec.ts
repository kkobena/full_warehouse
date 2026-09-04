import { expect } from '@playwright/test';
import { chercherProduit, choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une vente dépôt n'est pas une vente : c'est un TRANSFERT vers un point de vente rattaché.
 * L'officine se dessaisit de la marchandise mais n'encaisse rien — le dépôt réglera plus tard,
 * sur relevé. D'où trois différences avec le comptoir, visibles à l'écran :
 *
 *   * on choisit un DÉPÔT au lieu d'un client ;
 *   * il n'y a pas de pavé de règlement ;
 *   * le montant reste dû, et n'entre pas dans le chiffre d'affaires déclaré.
 *
 * La validation ne demande pas la souris. Le champ de recherche produit garde le focus d'un
 * article au suivant ; une fois le panier constitué, il est VIDE, et c'est cet état qui donne
 * son second rôle à la touche Entrée : elle y demande la finalisation du transfert
 * (`onSaveKeyDown` dans `vente-depot.component.ts`), qu'une confirmation vient encadrer. Le
 * caissier ne quitte donc jamais le clavier entre le premier produit et l'enregistrement.
 *
 * Parcours ÉCRIVANT dans la base : il transfère réellement du stock vers le dépôt.
 */
scenario('VTE-29', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/sales-home/gestion');
    await page.getByRole('tab', { name: /Ventes dépôt/ }).click();
    await expect(contenu).toContainText('Ventes dépôt');
    await page.getByRole('button', { name: 'Nouvelle vente dépôt' }).click();
    await expect(page).toHaveURL(/vente-depot/);
    // Sans dépôt choisi, l'écran ne propose rien : c'est le destinataire qui ouvre la saisie.
    await expect(contenu).toContainText('Sélectionnez un dépôt');
  });

  await etape(2, async () => {
    await choisirDansSelect(page, 'depotSelect', 'DEPOT');
    await chercherProduit(page, produit);
    // L'écran dépôt ne donne pas le focus au champ quantité, à la différence du comptoir :
    // on l'y met soi-même, puis on efface la valeur par défaut avant de frapper la sienne.
    const quantite = page.locator('#quantiteSaisie');
    await expect(quantite).toBeEnabled();
    await quantite.click();
    await quantite.press('Control+a');
    await quantite.pressSequentially('3', { delay: 40 });
    await page.getByRole('button', { name: 'Ajouter au panier' }).click();
    await expect(lignes.first()).toContainText(produit);
  });

  const confirmation = page.locator('.modal-content:visible');

  await etape(3, async () => {
    // `focus()` plutôt qu'un clic : cliquer le champ DÉPLIERAIT la liste du ng-select, et
    // Entrée y vaudrait alors choix d'une suggestion au lieu de demander la finalisation.
    const recherche = page.locator('#produitbox');
    await recherche.focus();
    await recherche.press('Enter');

    // Ce que la touche déclenche : la question, pas l'enregistrement. Un transfert de stock
    // ne part jamais sur une frappe seule.
    await expect(confirmation).toContainText('Voulez-vous vraiment finaliser la vente');
  });

  // ── Hors capture : on répond à la question, et le transfert est enregistré. L'image de
  //    l'étape 3 doit montrer la CONFIRMATION, qui est ce que la touche Entrée provoque ;
  //    répondre avant la prise donnerait un écran déjà vidé, sans rapport avec sa légende. ──
  await confirmation.getByRole('button', { name: 'Oui' }).click();
  // La vente validée, l'écran repart à zéro : plus de panier, prêt pour le dépôt suivant.
  await expect(contenu).toContainText(/Recherchez un produit pour commencer|Sélectionnez un dépôt/i);
});
