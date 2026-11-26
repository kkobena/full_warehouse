import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le retour dépôt est la réciproque exacte de la vente dépôt (VTE-29) : la marchandise confiée
 * revient à l'officine, sans qu'aucun argent ne change de main — le dépôt ne l'avait jamais
 * payée. C'est ce qui permet de reprendre ce qui dort chez le dépositaire plutôt que de le
 * racheter au grossiste.
 *
 * Deux choses le distinguent d'un retour client ou fournisseur :
 *
 *   * on ne choisit pas dans le catalogue mais dans CE QUE LE DÉPÔT DÉTIENT. La liste produit
 *     est son stock, chaque ligne portant la quantité dont il dispose ; tant qu'aucun dépôt
 *     n'est choisi, la recherche reste fermée ;
 *   * un seul enregistrement déplace le stock DES DEUX CÔTÉS et laisse DEUX mouvements — la
 *     sortie du dépôt et l'entrée à l'officine. Longtemps seule l'entrée était tracée : le
 *     stock du dépôt baissait sans que son historique le dise.
 *
 * Parcours ÉCRIVANT dans la base : il reprend réellement de la marchandise au dépôt.
 */
scenario('STK-47', async ({ etape, page }) => {
  const produit = 'EFFERALGAN';
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/depot');
    await ouvrirOnglet(page, /Produits|Stock/);
    // Le retour part de l'écran qui montre ce que le dépôt détient : on regarde avant de
    // décider ce qu'on reprend.
    await page.getByRole('button', { name: /Retour dépôt/ }).click();
    await expect(page).toHaveURL(/new-return/);
    await expect(contenu).toContainText('Retours Dépôt');
  });

  await etape(2, async () => {
    // Sans dépôt choisi, il n'y a rien à reprendre : c'est le détenteur qui ouvre la saisie.
    await expect(page.locator('#product-search')).toBeDisabled();
    await choisirDansSelect(page, 'depot-select', 'DEPOT');
    await expect(page.locator('#product-search')).toBeEnabled();
  });

  await etape(3, async () => {
    const recherche = page.locator('#product-search');
    await recherche.click();
    await recherche.fill(produit);
    // La suggestion affiche le stock détenu par le dépôt, pas celui de l'officine : c'est lui
    // qui borne ce qu'on peut reprendre.
    const suggestion = page.locator('.ng-option').first();
    await expect(suggestion).toContainText(produit);
    await expect(suggestion).toContainText(/Stock/i);
    await suggestion.click();

    // Le choix du produit arme le champ quantité et lui donne le focus, avec 1 par défaut.
    const quantite = page.locator('#quantiteSaisie');
    await expect(quantite).toBeEnabled();
    await quantite.click();
    await quantite.press('Control+a');
    await quantite.pressSequentially('2', { delay: 40 });
    await page.getByRole('button', { name: 'Ajouter au panier' }).click();
    await expect(lignes.first()).toContainText(produit);
  });

  await etape(4, async () => {
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    // Le stock a changé de main des deux côtés d'un coup.
    await expect(
      page
        .locator('[role="alert"], .toast, ngb-toast')
        .filter({ hasText: /succès/i })
        .first(),
    ).toBeVisible();
  });

  // ── Hors capture : l'écran revient de lui-même à la liste des dépôts après le message. On
  //    l'attend pour ne pas laisser la navigation courir dans le parcours suivant. ──
  await expect(page).toHaveURL(/\/depot$/, { timeout: 10_000 });
});
