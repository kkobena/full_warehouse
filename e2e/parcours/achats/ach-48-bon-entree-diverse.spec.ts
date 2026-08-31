import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Tout ce qui entre en stock ne vient pas d'une commande : un dépannage confrère, un don, une
 * régularisation d'inventaire, un retour de dépôt. Le BON D'ENTRÉE DIVERSE trace ces entrées
 * hors commande, avec leur MOTIF — sans lui, le stock augmenterait sans qu'on sache pourquoi,
 * et l'écart d'inventaire deviendrait inexplicable.
 *
 * Parcours en LECTURE : il ouvre la saisie sans enregistrer une entrée que les parcours de
 * stock ne s'attendent pas à trouver.
 */
scenario('ACH-48', async ({ etape, page }) => {
  const ecran = page.locator('app-bed-home');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Bons d'Entrée Diverses/);
    await expect(ecran).toBeVisible();
    // La liste porte le motif de chaque entrée : c'est la colonne qui justifie le mouvement.
    await expect(ecran).toContainText('Motif');
    await expect(ecran).toContainText('Référence');
  });

  await etape(2, async () => {
    // Deux boutons « Nouveau » : celui de la barre d'outils et celui de l'état vide.
    await ecran.getByRole('button', { name: 'Nouveau' }).first().click();
    // La saisie commence par les PRODUITS : on liste ce qui entre, et l'on dira pourquoi au
    // moment de valider — c'est là que le motif est demandé, et il est obligatoire.
    await expect(page.locator('#main-content')).toContainText(/Produit/);
    await expect(page.locator('#main-content')).toContainText(/Aucune ligne/i);
  });

  etape.horsPortee(
    3,
    'valider ferait entrer des quantités en stock hors commande ; la saisie et le choix du ' +
      'motif sont illustrés, la validation ne l’est pas.',
  );
});
