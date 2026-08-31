import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Tous les produits ne méritent pas la même attention. Une poignée de références fait
 * l'essentiel du chiffre d'affaires — les classes A de l'analyse de Pareto — et ce sont
 * celles-là qu'un écart de stock coûte cher. Les compter quatre fois par an quand les classes
 * C le sont une fois vaut mieux que de tout compter également.
 *
 * Le critère « Classification ABC » fait exactement cela : à chaque échéance, le planning
 * inventorie la classe qui vient dans la rotation, et l'index avance tout seul. Personne n'a
 * à se souvenir de ce qui a été compté la fois précédente.
 *
 * Parcours ÉCRIVANT dans la base : il crée un planning par classification.
 */
scenario('STK-28', async ({ etape, page }) => {
  const modale = page.locator('.modal-content');
  const libelle = `Parcours STK-28 — ABC ${Date.now().toString().slice(-5)}`;

  await etape(1, async () => {
    await page.goto('/inventaire');
    await ouvrirOnglet(page, /Tournant/);
    await page.getByRole('button', { name: 'Nouveau planning' }).click();
    await modale.locator('#libelle').fill(libelle);
    await choisirDansSelect(page, 'frequence', 'Mensuel');
    // Le critère qui fait tourner la rotation sur les classes de Pareto.
    await choisirDansSelect(page, 'critere', 'Par classification ABC (Pareto)');
  });

  await etape(2, async () => {
    await modale.getByRole('button', { name: 'Créer' }).click();
    await expect(page.locator('tbody tr').filter({ hasText: libelle }).first()).toBeVisible();
  });

  await etape(3, async () => {
    // Le compteur d'exécutions et la prochaine échéance : c'est là que se lit l'avancée de
    // la rotation, échéance après échéance.
    const ligne = page.locator('tbody tr').filter({ hasText: libelle }).first();
    await expect(ligne).toContainText(/Actif/);
    await expect(page.locator('#main-content')).toContainText(/Prochain tournant|Taux de couverture/);
  });
});
