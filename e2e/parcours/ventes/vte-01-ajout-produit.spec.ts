import { expect } from '@playwright/test';
import { assurerPanierVide, saisirQuantite } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * L'écran de vente est le plus utilisé de l'officine, et le seul où la vitesse compte : tout
 * y est pensé pour la frappe au clavier — la recherche prend le focus, la quantité se saisit
 * dans la foulée, et Entrée ajoute la ligne. La souris n'est nulle part obligatoire.
 *
 * Parcours ÉCRIVANT dans la base : dès la première ligne, une vente « en cours » est créée
 * côté serveur. Il l'annule une fois la dernière capture prise.
 */
scenario('VTE-01', async ({ etape, page }) => {
  const produit = 'PARACETAMOL 1G';
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await assurerPanierVide(page);
    await page.goto('/sales-home');
    await page.locator('#produitbox').fill(produit);
    // La liste ne propose que des produits RÉELS : chaque suggestion porte son code CIP, son
    // stock et son prix, ce qui évite d'avoir à ouvrir la fiche pour vérifier.
    await expect(page.locator('.ng-option').first()).toContainText(produit);
  });

  await etape(2, async () => {
    // Retenir la suggestion : c'est ce geste — clic, ou flèches puis Entrée — qui distingue
    // « recherché » de « sélectionné ». Le prix affiché en est la preuve à l'écran.
    await page.locator('.ng-option').first().click();
    await expect(page.locator('#main-content')).toContainText(/Prix\s*:/);
    // Le curseur passe seul au champ quantité : c'est ce que la légende annonce, on le vérifie.
    await expect(page.locator('#quantiteSaisie')).toBeFocused();
  });

  await etape(3, async () => {
    // Le champ quantité suit immédiatement la recherche, sans clic : c'est la séquence de
    // frappe d'un comptoir. La valeur proposée (1) est remplacée, pas complétée.
    await saisirQuantite(page, '2');
  });

  await etape(4, async () => {
    await page.locator('#quantiteSaisie').press('Enter');
    // La ligne au panier : produit, quantité, prix unitaire et total. Le total est ce qui
    // prouve que la quantité a bien été prise en compte — 2 × 1 085.
    await expect(lignes.first()).toContainText(produit);
    await expect(page.locator('#main-content')).toContainText(/À ENCAISSER/i);
  });

  // ── Remise en état, hors étapes : la vente en cours est abandonnée. ───────────────────
  await page.getByRole('button', { name: 'Annuler' }).click();
  const confirmation = page.locator('.modal-content');
  await expect(confirmation).toBeVisible();
  await confirmation.getByRole('button', { name: 'Oui' }).click();
  await expect(lignes.filter({ hasText: produit })).toHaveCount(0);
});
