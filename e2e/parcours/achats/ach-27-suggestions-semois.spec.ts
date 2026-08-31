import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Là où la proposition d'achat dit « commandez ceci », l'analyse SEMOIS dit POURQUOI : elle
 * pose côte à côte la vente mensuelle moyenne, la marge de sécurité, le stock objectif et le
 * stock actuel. La quantité à commander est la différence entre les deux derniers — elle se
 * vérifie, elle ne se subit pas.
 *
 * L'urgence n'est pas un jugement mais un calcul : un produit dont le stock est déjà sous sa
 * marge de sécurité est signalé, quelle que soit sa classe. C'est ce qui permet de traiter
 * d'abord ce qui manquera demain.
 *
 * Parcours en LECTURE.
 */
scenario('ACH-27', async ({ etape, page }) => {
  const ecran = page.locator('app-semois-suggestions');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: 'Tableau de bord stock' }).click();
    await expect(ecran).toBeVisible();
  });

  await etape(2, async () => {
    // Trois filtres, trois angles : le fournisseur (qui livrera), la classe ABC (ce que le
    // produit pèse) et la criticité (ce qu'une rupture coûterait).
    await expect(ecran).toContainText('Tous fournisseurs');
    await expect(ecran).toContainText('Toutes classes');
    await expect(ecran).toContainText('Toutes criticités');
  });

  await etape(3, async () => {
    // Les quatre colonnes du raisonnement, et la quantité qui en découle.
    await expect(ecran).toContainText('VMM');
    await expect(ecran).toContainText('Marge Séc.');
    await expect(ecran).toContainText('Stock Obj.');
    await expect(ecran).toContainText('Stock Act.');
    await expect(ecran).toContainText('Qté à Cmd');
  });
});
