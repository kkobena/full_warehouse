import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Certains grossistes reprennent les périmés, sous condition de délai et de motif. Autant en
 * profiter : un lot repris est un avoir, un lot détruit est une perte sèche.
 *
 * Le retour part du lot périmé lui-même, ce qui évite de ressaisir produit, quantité et
 * péremption. Deux informations restent à donner : le FOURNISSEUR — déduit du lot quand il
 * est connu, à choisir sinon — et le MOTIF, obligatoire, qui justifiera l'avoir.
 *
 * La quantité est modifiable : on ne renvoie pas forcément tout le lot, le grossiste pouvant
 * n'en reprendre qu'une partie.
 *
 * Parcours en LECTURE : il prépare le retour et montre les contrôles, sans l'émettre.
 */
scenario('STK-19', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/gestion-peremption');
    await expect(lignes.first()).toBeVisible();
    // Un lot dont la colonne FOURNISSEUR est renseignée : un lot saisi à la main sur un
    // stock déjà présent n'a pas de réception d'origine, donc aucun fournisseur à créditer —
    // l'écran le dit alors et renvoie vers le retour manuel, mais ce n'est pas le chemin
    // qu'on montre ici. Et un lot mono-emplacement, le multi ayant son parcours (STK-34).
    const ligne = lignes
      .filter({ has: page.locator('.pharma-badge-info .pi-map-marker') })
      .filter({ hasText: /LABOREX|COPHARMED|DPCI|TEDIS|UBIPHARM/ })
      .first();
    await expect(ligne).toBeVisible();
    await ligne.getByRole('button', { name: 'Faire un retour fournisseur' }).click();
    await expect(modale).toBeVisible();
  });

  await etape(2, async () => {
    // La modale reprend le lot : numéro, péremption, stock disponible. Rien à ressaisir.
    await expect(modale).toContainText(/N° Lot/);
    await expect(modale).toContainText(/Stock disponible/);
  });

  await etape(3, async () => {
    // Le motif est obligatoire — c'est lui qui justifiera l'avoir auprès du grossiste.
    await expect(modale).toContainText(/Motif de retour/);
    await modale.locator('ng-select').last().click();
    await page.locator('.ng-option').first().click();
    // La quantité est modifiable : le grossiste ne reprend pas forcément tout le lot.
    await expect(modale).toContainText(/Quantité/);
  });

  await etape(4, async () => {
    // Le fournisseur est déduit du lot quand sa réception est connue. Sans réception
    // d'origine, l'écran demande de le désigner — ou renvoie vers le retour manuel.
    await expect(modale).toContainText(/[Ff]ournisseur/);
  });

  await etape(5, async () => {
    // Le parcours s'arrête au bouton : émettre l'avoir engage la relation avec le grossiste.
    await expect(modale.getByRole('button', { name: /Créer le retour/ })).toBeVisible();
  });
});
