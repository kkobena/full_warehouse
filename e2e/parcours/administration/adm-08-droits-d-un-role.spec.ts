import { expect } from '@playwright/test';
import { chercherDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le contrôle d'accès de PharmaSmart est un refus par défaut : un rôle ne voit et ne peut que
 * ce qu'on lui a EXPLICITEMENT accordé. La grille des autorisations est donc la réponse à la
 * question « qu'est-ce que ce rôle a le droit de faire ? » — et elle se lit d'un coup d'œil.
 *
 * Chaque ligne est une entrée de navigation ou une ACTION (forcer le stock, annuler une vente,
 * appliquer une remise…) ; chaque colonne un verbe : afficher, accéder, créer, modifier,
 * supprimer, exporter, exécuter.
 *
 * Parcours en LECTURE : il ne coche rien.
 */
scenario('ADM-08', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/admin/access-management');
    await page.getByRole('tab', { name: /Autorisations/ }).click();
    await expect(contenu).toContainText('Permissions par rôle');
    // Tant qu'aucun rôle n'est choisi, la grille reste vide : les droits n'existent pas dans
    // l'absolu, ils appartiennent à un rôle.
    await expect(contenu).toContainText('Sélectionnez un rôle');
  });

  await etape(2, async () => {
    await chercherDansSelect(page, 'navRole', 'Caiss', 'Caissier');
    // La grille se remplit : entrées de menu ET actions, avec leurs sept colonnes de verbes.
    await expect(contenu).toContainText('Entrée de menu');
    await expect(contenu).toContainText('Afficher');
    await expect(contenu).toContainText('Exécuter');
    // « Action » et « Onglet » : la grille dit de quelle NATURE est chaque ligne — un écran,
    // un onglet, ou un geste précis soumis à privilège.
    await expect(contenu).toContainText('Action');
    await expect(contenu).toContainText('Onglet');
    // Les privilèges sensibles du comptoir figurent ici, nommément — ce sont eux que VTE-46
    // et VTE-47 mettent en scène côté caissier.
    await expect(contenu).toContainText('Supprimer une ligne vente');
    await expect(contenu).toContainText('Appliquer une remise à la vente');
  });
});
