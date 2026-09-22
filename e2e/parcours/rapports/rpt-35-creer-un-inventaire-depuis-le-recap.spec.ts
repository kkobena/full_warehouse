import { expect } from '@playwright/test';
import { saisirDate, traverserConfirmations } from '../../src/actions';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Un rapport qui ne débouche sur rien se lit une fois. Celui-ci débouche.
 *
 * Le périmètre qu'on vient de construire à coups de filtres — les produits qui se vendent sous
 * leur prix d'achat, ceux dont le stock est sous le seuil, ceux qui n'ont rien vendu depuis
 * trois mois — est exactement la liste qu'on voudrait recompter ou recommander. Le menu
 * « Actions » l'envoie telle quelle vers un nouvel inventaire ou vers une suggestion de
 * réapprovisionnement.
 *
 * Sans cela, il faudrait ressaisir la liste ailleurs, et l'on ne le ferait pas.
 *
 * Parcours ÉCRIVANT dans la base : il crée l'inventaire puis le supprime. Il se contentait
 * auparavant d'ouvrir le menu et de vérifier que l'entrée existait, en renvoyant sur les
 * parcours du module Stock. Or ceux-ci créent leurs inventaires par l'écran de création, qui
 * emprunte un tout autre chemin côté serveur : la création depuis le récapitulatif échouait sur
 * une contrainte de base, et aucun parcours ne s'en apercevait. L'étape 2 du cahier de recette
 * dit « Lancer », pas « Voir » — elle lance.
 */
scenario('RPT-35', async ({ etape, page }) => {
  const prefixeInventaire = 'Inventaire des produits';

  await etape(1, async () => {
    await ouvrirRapport(page, 'stock', 'Récap Produits Vendus/Invendus');
    await expect(page.getByRole('heading', { name: /Récapitulatif Produits/ })).toBeVisible();

    const fin = new Date();
    const debut = new Date(fin.getFullYear(), fin.getMonth() - 2, 1);
    await saisirDate(page, 'startDate', debut);
    await saisirDate(page, 'endDate', fin);
    await page.getByRole('button', { name: 'Rechercher' }).click();
    await expect(page.locator('#main-content').locator('tbody tr').first()).toBeVisible();
  });

  await etape(2, async () => {
    // Le chevron ouvre les trois destinations possibles du périmètre filtré.
    await page.getByRole('button', { name: 'Autres actions' }).first().click();
    const menu = page.locator('.dropdown-menu.show').first();
    await expect(menu).toContainText('Créer Inventaire');
    await expect(menu).toContainText(/Créer suggestion/);

    await menu.getByText('Créer Inventaire').click();

    // Le compte rendu nomme le nombre de produits repris : c'est lui qui distingue un
    // inventaire réellement créé d'un appel parti en erreur.
    await expect(page.locator('ngb-toast')).toContainText(/produit\(s\).*pris en compte pour l'inventaire/);
    await expect(page.locator('ngb-toast')).not.toContainText(/Erreur lors de la création/);
  });

  // Ménage hors étapes : le jeu de démonstration n'a pas à conserver cet inventaire.
  await page.goto('/inventaire');
  const cree = page
    .locator('tbody tr')
    .filter({ visible: true })
    .filter({ hasText: prefixeInventaire })
    .first();
  await expect(cree).toBeVisible();
  await cree.getByRole('button', { name: 'Supprimer' }).click();
  await traverserConfirmations(page, { limite: 1 });
});
