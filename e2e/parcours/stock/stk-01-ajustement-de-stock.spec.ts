import { expect } from '@playwright/test';
import { chercherDansSelect, choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * L'ajustement est l'aveu écrit d'un écart : le stock informatique dit 12, l'étagère en
 * montre 9. Trois boîtes ont disparu, et la question n'est pas seulement de rétablir le
 * chiffre — c'est de dire POURQUOI. Casse, vol, erreur de saisie, don : le motif est
 * obligatoire, et c'est lui qui rendra l'écart compréhensible six mois plus tard.
 *
 * L'ajustement se prépare ligne à ligne, se corrige librement, et n'engage rien tant qu'il
 * n'est pas CLÔTURÉ. La clôture est le point de bascule : elle applique les mouvements au
 * stock et fige le document.
 *
 * La quantité se saisit signée : positive pour une entrée, négative pour une sortie. Un même
 * ajustement peut donc porter les deux — ce qu'on a retrouvé et ce qu'on a perdu.
 *
 * Parcours ÉCRIVANT dans la base : il prépare un ajustement sans le clôturer.
 */
scenario('STK-01', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const ecran = page.locator('app-ajustement-form');

  await etape(1, async () => {
    await page.goto('/features-ajustement');
    await page.getByRole('button', { name: /Nouvel? ajustement|Nouveau/ }).first().click();
    await expect(ecran).toBeVisible();
  });

  await etape(2, async () => {
    // Le motif d'abord : sans lui, la ligne ne peut pas être ajoutée.
    await choisirDansSelect(page, 'ajustement-motif', 'Casse');
    await chercherDansSelect(page, 'produitbox', produit, produit);
    await expect(ecran).toContainText(/Stock/);
  });

  await etape(3, async () => {
    // Une sortie : la quantité est NÉGATIVE. L'écran annonce le stock qui en résultera,
    // avant d'ajouter quoi que ce soit.
    const qte = ecran.locator('input[placeholder="Qté (±)"]');
    await qte.click();
    await qte.fill('');
    await qte.pressSequentially('-3', { delay: 40 });
    await expect(qte).toHaveValue('-3');
  });

  await etape(4, async () => {
    await ecran.getByRole('button', { name: /Ajouter|^$/ }).last().click();
    await expect(ecran.locator('tbody tr').first()).toContainText(produit);
  });

  await etape(5, async () => {
    // Tant que l'ajustement n'est pas clôturé, le stock n'a pas bougé : le document se
    // complète, se corrige, et attend. Le parcours s'arrête là — clôturer appliquerait
    // les mouvements.
    await expect(ecran.getByRole('button', { name: 'Clôturer' })).toBeVisible();
  });
});
