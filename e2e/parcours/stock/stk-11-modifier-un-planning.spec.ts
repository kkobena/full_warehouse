import { expect } from '@playwright/test';
import { ouvrirOnglet, traverserConfirmations } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un planning tournant se règle dans le temps : la fréquence hebdomadaire tenue en janvier
 * devient intenable en décembre, et le rayon qu'on inventoriait tous les mois s'est vidé.
 * Modifier plutôt que supprimer conserve l'historique des exécutions — c'est lui qui prouve
 * que le stock a été contrôlé.
 *
 * La DÉSACTIVATION est le geste intermédiaire : le planning cesse de générer des inventaires
 * sans disparaître, et se rallume le jour où on en a de nouveau besoin. C'est ce qu'on fait
 * pendant les congés, ou le temps d'un déménagement de rayon.
 *
 * Parcours ÉCRIVANT dans la base : il désactive un planning puis le réactive.
 */
scenario('STK-11', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/inventaire');
    await ouvrirOnglet(page, /Tournant/);
    await expect(lignes.first()).toBeVisible();
    // Le planning porte son état : actif, il produira ses inventaires ; inactif, il attend.
    await expect(lignes.first()).toContainText(/Actif|Inactif/);
  });

  await etape(2, async () => {
    // Modifier : le formulaire reprend les réglages en place plutôt que d'en repartir.
    await lignes.first().getByRole('button', { name: 'Modifier le planning' }).click();
    const modale = page.locator('.modal-content');
    await expect(modale.locator('#libelle')).not.toHaveValue('');
    await expect(modale.getByRole('button', { name: 'Modifier' })).toBeVisible();
    await modale.getByRole('button', { name: 'Annuler' }).click();
  });

  await etape(3, async () => {
    // Désactiver : le planning reste dans la liste, avec son historique, mais ne produit plus.
    await lignes.first().getByRole('button', { name: 'Désactiver' }).click();
    await traverserConfirmations(page, { limite: 1 });
    await expect(lignes.first()).toContainText('Inactif');
    // Et se rallume aussi simplement.
    await lignes.first().getByRole('button', { name: 'Activer' }).click();
    await traverserConfirmations(page, { limite: 1 });
    await expect(lignes.first()).toContainText('Actif');
  });
});
