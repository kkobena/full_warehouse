import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Deux façons de décider quoi commander, et le choix engage toute l'officine.
 *
 * Le modèle CLASSIQUE prend la moyenne des ventes passées. Simple, immédiat, sans réglage :
 * c'est celui d'une officine qui démarre, ou qui n'a pas encore d'historique fiable.
 *
 * Le modèle SEMOIS pondère les mois récents, écarte les mois de rupture — un mois sans stock
 * n'est pas un mois sans demande —, applique la classe de criticité (ACH-23), une marge de
 * sécurité et un facteur saisonnier. Il commande mieux, au prix d'un paramétrage.
 *
 * L'écran présente les deux côte à côte avec leurs conditions d'emploi, plutôt qu'un
 * interrupteur nu : le choix ne se devine pas.
 *
 * Parcours en LECTURE : il montre le choix sans le changer, basculer de modèle recalculant
 * toutes les suggestions.
 */
scenario('ACH-24', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/semois/model-config');
    await expect(contenu).toContainText('Modèle Classique');
  });

  await etape(2, async () => {
    await expect(contenu).toContainText('Modèle SEMOIS');
  });

  await etape(3, async () => {
    // Chaque modèle est présenté avec ce à quoi il convient : c'est ce qui rend le choix
    // possible sans lire la documentation.
    await expect(contenu).toContainText(/temps de configurer|VMM|saisonnier|criticité/i);
  });

  etape.horsPortee(
    4,
    'basculer de modèle recalculerait toutes les suggestions de commande, et avec elles les ' +
      'captures des écrans de réapprovisionnement.',
  );
});
