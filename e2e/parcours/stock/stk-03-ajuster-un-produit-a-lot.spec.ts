import { expect } from '@playwright/test';
import { chercherDansSelect, choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Sur un produit suivi par lot, corriger le stock ne suffit pas : il faut dire QUEL lot. Sans
 * cela, le stock de l'emplacement et la somme des lots divergent, et la traçabilité — celle
 * qu'un rappel de lot rendra soudain indispensable — devient fausse.
 *
 * L'écran ne pose la question que dans le sens où elle se pose : une ENTRÉE doit désigner le
 * lot à créditer, avec son numéro et sa péremption sous les yeux ; une SORTIE, elle, débite
 * d'elle-même les lots les plus proches de la péremption (FEFO), ce qui est le seul ordre
 * défendable — sortir un lot lointain en laissant périmer le proche coûterait deux fois.
 *
 * Parcours ÉCRIVANT dans la base : il prépare un ajustement sans le clôturer.
 */
scenario('STK-03', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const ecran = page.locator('app-ajustement-form');

  await etape(1, async () => {
    await page.goto('/features-ajustement');
    await page.getByRole('button', { name: 'Nouvel ajustement' }).click();
    await expect(ecran).toBeVisible();
    await choisirDansSelect(page, 'ajustement-motif', 'Erreur de saisie');
    await chercherDansSelect(page, 'produitbox', produit, produit);
    await expect(ecran).toContainText(/Rayon\s*:/);
  });

  await etape(2, async () => {
    // Quantité POSITIVE : c'est une entrée, et le choix du lot s'ouvre avec elle.
    const qte = ecran.locator('input[placeholder="Qté (±)"]');
    await qte.click();
    await qte.fill('');
    await qte.pressSequentially('2', { delay: 40 });
    await expect(ecran).toContainText(/Sélectionner un lot/);
  });

  await etape(3, async () => {
    // Le lot se choisit sur son numéro et sa péremption : deux lots du même produit ne sont
    // pas interchangeables.
    await ecran.locator('.meta-item--lot input').first().click();
    const option = page.locator('.ng-option').first();
    await expect(option).toBeVisible();
    await option.click();
  });

  await etape(4, async () => {
    await ecran.getByRole('button', { name: /Ajouter|^$/ }).last().click();
    await expect(ecran.locator('tbody tr').first()).toContainText(produit);
  });

  await etape(5, async () => {
    // Le sens inverse : une quantité NÉGATIVE ne demande aucun lot. L'application débite
    // elle-même, du plus proche de la péremption au plus lointain (FEFO) — c'est le seul
    // ordre qui ne fabrique pas de périmés.
    const qte = ecran.locator('input[placeholder="Qté (±)"]');
    await qte.click();
    await qte.fill('');
    await qte.pressSequentially('-1', { delay: 40 });
    await expect(ecran).not.toContainText(/Sélectionner un lot/);
  });
});
