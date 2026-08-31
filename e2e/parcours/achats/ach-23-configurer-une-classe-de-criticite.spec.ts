import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Toutes les références ne méritent pas la même prudence. Un antipaludéen de garde ne doit
 * jamais manquer ; une crème de confort peut attendre la prochaine commande. C'est ce que
 * disent les classes de criticité — A+, A, B, C, D — et chacune porte sa propre politique.
 *
 * Deux réglages font l'essentiel du calcul :
 *
 *   • le COEFFICIENT DE SÉCURITÉ multiplie la vente moyenne mensuelle pour fixer le matelas.
 *     Plus la classe est critique, plus il est élevé — on préfère immobiliser du stock que
 *     manquer ;
 *   • la PROFONDEUR D'HISTORIQUE dit sur combien de mois lire la tendance. Court, on suit la
 *     saison ; long, on lisse les à-coups.
 *
 * La limite de péremption plafonne le stock objectif à trois fois la VMM : sur un produit qui
 * périme, se couvrir trop revient à jeter.
 *
 * Parcours en LECTURE : il montre les réglages sans les enregistrer, une politique modifiée
 * changeant toutes les suggestions du manuel.
 */
scenario('ACH-23', async ({ etape, page }) => {
  const contenu = page.locator('.modal-content:visible');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    // L'écran est une MODALE, ouverte depuis les suggestions : la politique se règle là où
    // l'on constate ses effets, pas dans un coin du paramétrage.
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: 'Classes produits' }).click();
    await expect(page.locator('.modal-content:visible')).toContainText(
      "Paramètres d'approvisionnement par classe",
    );
  });

  await etape(2, async () => {
    // Les cinq classes, chacune avec sa politique.
    await expect(contenu).toContainText('Classe');
    await expect(lignes.first()).toBeVisible();
  });

  await etape(3, async () => {
    // Les deux réglages qui gouvernent le calcul, et le garde-fou des périmables.
    await expect(contenu).toContainText('Coeff. sécurité');
    await expect(contenu).toContainText("Mois d'historique");
    await expect(contenu).toContainText('Limite péremption');
  });

  etape.horsPortee(
    4,
    'enregistrer une politique modifiée recalculerait toutes les suggestions, et avec elles ' +
      'les captures des écrans de réapprovisionnement.',
  );
});
