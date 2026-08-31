import { expect } from '@playwright/test';
import { chercherDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le vocabulaire d'une officine n'est pas celui d'une autre : ce que l'application appelle
 * « Ventes en cours », telle équipe l'appelle « Paniers ouverts ». Renommer un menu ne
 * demande donc pas de développement — cela se fait sur place, dans la grille des
 * autorisations, et l'enregistrement est immédiat.
 *
 * Deux libellés cohabitent, et la distinction est utile :
 *   * le LIBELLÉ, celui qu'on lit dans le menu ;
 *   * le TITRE LONG, celui qu'affichera la barre d'outils de l'écran ouvert — réservé aux
 *     ONGLETS, seuls à ouvrir un écran, et qui reprend le libellé tant qu'on ne l'a pas
 *     défini.
 *
 * Parcours ÉCRIVANT dans la base : il rend au menu son libellé et son titre d'origine.
 */
scenario('ADM-17', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const libelleOrigine = 'Ventes en cours';
  const libelleParcours = 'Paniers ouverts';
  const titreLong = 'Paniers ouverts — reprise et abandon';

  const ligne = (texte: string) => page.locator('tbody tr').filter({ hasText: texte }).first();

  await etape(1, async () => {
    await page.goto('/admin/access-management');
    await page.getByRole('tab', { name: /Autorisations/ }).click();
    await chercherDansSelect(page, 'navRole', 'Caiss', 'Caissier');
    // Chaque ligne porte son crayon : c'est là que le nom se retouche, sans quitter la
    // grille des droits.
    await expect(ligne(libelleOrigine)).toBeVisible();
    await expect(ligne(libelleOrigine).getByRole('button', { name: 'Modifier le libellé du menu' })).toBeVisible();
  });

  await etape(2, async () => {
    await ligne(libelleOrigine).getByRole('button', { name: 'Modifier le libellé du menu' }).click();
    const champ = page.locator('input.libelle-input');
    await champ.fill(libelleParcours);
    // `Entrée` valide — le champ enregistre aussi à la perte du focus.
    await champ.press('Enter');
    await expect(ligne(libelleParcours)).toBeVisible();
  });

  await etape(3, async () => {
    // Le second bouton n'apparaît que sur les ONGLETS : eux seuls ouvrent un écran doté
    // d'une barre d'outils. Son infobulle rappelle la règle — à défaut de titre long, la
    // barre reprend le libellé du menu.
    const bouton = ligne(libelleParcours).getByRole('button', { name: "Définir le titre de la barre d'outils" });
    await expect(bouton).toBeVisible();
    await bouton.click();
    const champ = page.locator('input.libelle-input');
    await champ.fill(titreLong);
    await champ.press('Enter');
    await expect(contenu).toContainText(titreLong);
  });

  // ── Remise en état : le menu retrouve son nom, et sa barre son titre par défaut. ────────
  await ligne(titreLong).getByRole('button', { name: "Définir le titre de la barre d'outils" }).click();
  const champTitre = page.locator('input.libelle-input');
  await champTitre.fill(libelleParcours);
  await champTitre.press('Enter');
  await ligne(libelleParcours).getByRole('button', { name: 'Modifier le libellé du menu' }).click();
  const champLibelle = page.locator('input.libelle-input');
  await champLibelle.fill(libelleOrigine);
  await champLibelle.press('Enter');
  await expect(ligne(libelleOrigine)).toBeVisible();
});
