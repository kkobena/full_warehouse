import { expect } from '@playwright/test';
import { saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le journal affiche un total en tête — nombre de ventes et chiffre d'affaires de la période
 * filtrée. C'est ce chiffre que le pharmacien reporte, et qu'il doit pouvoir CONTRÔLER : le
 * total annoncé doit être celui des lignes, pas celui de la page affichée.
 *
 * La nuance est tout l'objet du scénario : la liste est paginée — quinze ventes à la fois —
 * tandis que le total, lui, porte sur la période entière. Confondre les deux, c'est déclarer
 * un CA faux.
 *
 * Parcours en LECTURE.
 */
scenario('VTE-18', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });
  // Une journée pleine du jeu de démonstration, close depuis longtemps : ses ventes ne
  // bougeront plus, et le total non plus.
  //
  // L'officine FERME LE LUNDI, et une semaine en arrière retombe sur le même jour de la
  // semaine : un parcours lancé un lundi visait donc une journée sans la moindre vente, et le
  // journal s'ouvrait vide — à juste titre. On recule d'un jour de plus dans ce cas.
  const jour = new Date();
  jour.setDate(jour.getDate() - 7);
  if (jour.getDay() === 1) {
    jour.setDate(jour.getDate() - 1);
  }

  await etape(1, async () => {
    await page.goto('/sales-home/gestion');
    await saisirDate(page, 'fromDate', jour);
    await saisirDate(page, 'toDate', jour);
    await page.getByRole('button', { name: 'Rechercher' }).click();
    await expect(lignes.first()).toBeVisible();
    // Les deux chiffres de contrôle, côte à côte : combien de ventes, et pour quel montant.
    await expect(contenu).toContainText('Nombre de ventes');
    await expect(contenu).toContainText('Total CA');
  });

  await etape(2, async () => {
    // Le pied de liste dit ce qui est AFFICHÉ, l'en-tête ce qui est COMPTÉ : « 1 à 15 sur
    // N ». Le total du haut couvre les N ventes de la période, pas les quinze de la page —
    // c'est la confusion que ce scénario sert à écarter.
    await expect(contenu).toContainText(/Affichage de \d+ à \d+ sur \d+ ventes/);
    const entete = (await contenu.innerText()).match(/Nombre de ventes\s*:\s*(\d+)/);
    const pied = (await contenu.innerText()).match(/sur (\d+) ventes/);
    expect(entete?.[1], 'compteur de l\'en-tête').toBe(pied?.[1]);
  });
});
