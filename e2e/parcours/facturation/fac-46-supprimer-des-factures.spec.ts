import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une facture éditée par erreur — mauvaise période, mauvais payeur, double génération —
 * bloque ses lignes de vente : elles sont consommées et ne repartiront sur aucune autre
 * facture tant qu'elle existe. La supprimer les libère.
 *
 * La suppression n'est donc possible que sur des factures IMPAYÉES : une facture déjà réglée
 * a une contrepartie comptable, et l'effacer ferait disparaître l'encaissement avec elle.
 * L'opération est groupée, parce qu'une erreur d'édition en produit rarement une seule.
 *
 * Parcours en LECTURE : il montre le contrôle sans supprimer — le jeu de démonstration a
 * besoin de ses factures.
 */
scenario('FAC-46', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /^Factures/);
    // Les impayées : les seules que la suppression accepte.
    await choisirDansSelect(page, 'fhStatut', 'Non réglées');
    await page.keyboard.press('Escape');
    await rechercher(page);
    await expect(lignes.first()).toBeVisible();
  });

  await etape(2, async () => {
    // La sélection : l'erreur d'édition se corrige en lot.
    await lignes.first().locator('input[type="checkbox"]').first().check();
    await expect(contenu).toContainText(/Supprimer la sélection/);
  });

  await etape(3, async () => {
    // Le parcours s'arrête au bouton : supprimer libérerait des lignes de vente que les
    // autres parcours de facturation attendent.
    await expect(page.getByRole('button', { name: 'Supprimer la sélection' })).toBeVisible();
  });
});
