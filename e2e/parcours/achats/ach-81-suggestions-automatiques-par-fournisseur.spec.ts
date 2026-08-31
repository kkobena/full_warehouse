import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Les propositions de commande se recalculent seules, à heure fixe : agrégation des ventes,
 * rafraîchissement des données, recalcul SEMOIS, puis regroupement par fournisseur. Une
 * officine qui éteint ses postes le soir n'y perd rien — un rattrapage part au démarrage
 * suivant, ce qui évite le trou du lundi matin.
 *
 * La quantité proposée comble l'écart entre le STOCK OBJECTIF et le stock virtuel, puis
 * s'aligne sur le colisage du fournisseur : commander sept boîtes quand elles viennent par
 * douze ne sert à rien.
 *
 * Le point qui compte pour la confiance : une ligne ajustée à la main n'est JAMAIS écrasée
 * par le traitement automatique. Sans cette garantie, personne ne corrigerait une suggestion.
 *
 * Parcours en LECTURE : l'écran annonce lui-même « consultation uniquement », la validation
 * se faisant depuis le réapprovisionnement.
 */
scenario('ACH-81', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  etape.horsPortee(
    1,
    'la fréquence d’exécution est un réglage de planificateur, sans écran : elle se règle ' +
      'dans la configuration du serveur, pas dans l’application.',
  );

  await etape(2, async () => {
    await page.goto('/semois/suggestions');
    // Le recalcul manuel existe à côté du planifié : même traitement, déclenché à la main.
    await expect(page.getByRole('button', { name: 'Recalculer VMM' })).toBeVisible({ timeout: 20000 });
  });

  await etape(3, async () => {
    // Le regroupement par fournisseur est le filtre premier : on commande chez l'un, pas
    // « en général ».
    await expect(contenu).toContainText('Tous fournisseurs');
    await expect(contenu).toContainText(/VMM/);
    await expect(contenu).toContainText(/STOCK OBJ/i);
  });

  await etape(4, async () => {
    // La quantité proposée et la couverture qui en résulte : c'est ce qu'on relit avant de
    // valider, et ce qu'une correction manuelle vient parfois remplacer.
    await expect(contenu).toContainText(/QTÉ À CMD/i);
    await expect(contenu).toContainText(/COUV\. ACT \/ CIBLE/i);
  });
});
