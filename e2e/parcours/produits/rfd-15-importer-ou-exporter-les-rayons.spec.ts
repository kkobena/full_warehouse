import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Une implantation se prépare rarement devant le logiciel : elle se dessine sur un tableur,
 * à plusieurs, parfois avec le prestataire qui pose les meubles. L'import évite de la
 * ressaisir, et l'export permet de la relire ou de la transmettre.
 *
 * Deux imports distincts, à ne pas confondre — l'écran les nomme d'ailleurs séparément :
 *
 *   • « Importer rayons » charge la STRUCTURE, les rayons eux-mêmes ;
 *   • « Importer affectations » charge le CONTENU, sous la forme `code_cip ; code_rayon`.
 *
 * Le second suppose le premier : on ne range pas dans un rayon qui n'existe pas.
 *
 * Parcours en LECTURE : il montre les trois chemins sans envoyer de fichier.
 */
scenario('RFD-15', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/rayon');
    await expect(lignes.first()).toBeVisible();
  });

  await etape(2, async () => {
    // L'export part de ce qui est à l'écran : le stockage retenu au filtre.
    await expect(page.getByRole('button', { name: 'Exporter' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Importer rayons' })).toBeVisible();
  });

  await etape(3, async () => {
    // Le format attendu figure sur le bouton lui-même : `code_cip ; code_rayon`. C'est ce
    // qui évite le fichier refusé après vingt minutes de mise en forme.
    const affectations = page.getByRole('button', { name: 'Importer affectations' });
    await expect(affectations).toBeVisible();
    await affectations.hover();
    await expect(page.locator('body')).toContainText(/code_cip/);
  });
});
