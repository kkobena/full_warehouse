import { expect } from '@playwright/test';
import { ouvrirBonDeReception, ouvrirOnglet } from '../../src/actions';
import { executer, lire } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Même situation qu'ACH-85 — une boîte dont le code n'est pas celui du catalogue — mais pas le
 * même geste, parce que l'opérateur n'est pas dans la même posture. En saisie ligne par ligne
 * il a le carton dans les mains et la ligne DÉJÀ à l'écran : lui ouvrir un sélecteur, c'est
 * lui faire redésigner l'article qu'il regarde. Un bandeau s'ouvre donc sur la ligne courante,
 * et trois touches suffisent — Entrée associe, A ouvre le sélecteur pour le cas où la boîte
 * n'est pas celle-là, Échap ignore.
 *
 * Ce que le bandeau cache et qu'il faut savoir : l'association n'écrit pas la même chose selon
 * la ligne. Une ligne dont le grossiste n'a jamais donné le code — un code PROVISOIRE — reçoit
 * le code scanné comme code catalogue. Une ligne déjà codée garde son code, et le code scanné
 * devient un code alternatif. Le bouton le dit, c'est la seule chose qui change à l'écran.
 *
 * Le parcours se place sur une ligne DÉJÀ CODÉE, où l'association crée un code alternatif :
 * c'est le cas réparable, qui se défait en retirant le code. Sur une ligne provisoire, le CIP
 * de la fiche fournisseur serait réécrit — ce que ce parcours n'a pas à faire à la base de
 * démonstration.
 *
 * Parcours ÉCRIVANT : le code mémorisé est retiré à la fin, par son code, qui n'appartient
 * qu'à ce parcours.
 */
const TRAME = '0106181111111114{GS}10LOT77{GS}17280630';
const CODE_MEMORISE = '6181111111114';

scenario('ACH-86', async ({ etape, page }) => {
  const ecran = page.locator('#main-content');
  const sequentiel = page.locator('app-reception-sequential');
  const bandeau = page.locator('.rh-seq__code-banner');

  // ── Décor : le poste repart en saisie séquentielle, mode nominal mémorisé sur le poste
  //    (cf. ACH-84) — une campagne précédente a pu laisser la vue tableau.
  await page.goto('/commande');
  await ouvrirOnglet(page, /Commandes & Réceptions/);
  await page.getByRole('tab', { name: /^Réceptions/ }).click();
  await page.evaluate(() => localStorage.removeItem('reception-view-mode'));
  await ouvrirBonDeReception(page, 'aucun');
  await expect(sequentiel).toBeVisible();

  // Se placer sur une ligne DÉJÀ CODÉE : le bandeau « Code CIP provisoire » signale les
  // autres, et F9 passe à la suivante. Sans cette précaution, l'ordre des lignes du bon
  // déciderait de ce que l'association écrit.
  for (let saut = 0; saut < 20; saut++) {
    if (!(await bandeau.isVisible().catch(() => false))) {
      break;
    }
    await page.keyboard.press('F9');
    await expect(sequentiel).toBeVisible();
  }
  await expect(bandeau, 'toutes les lignes du bon portent un code provisoire').toHaveCount(0);

  await etape(1, async () => {
    const champ = ecran.getByPlaceholder(/Scanner CIP/);
    await champ.fill(TRAME);
    await champ.press('Enter');
    // Pas de fenêtre : le bandeau s'ouvre SUR la ligne, sans rien recouvrir.
    await expect(bandeau).toContainText('Code inconnu');
    await expect(page.locator('.modal-content')).toHaveCount(0);
  });

  await etape(2, async () => {
    // Le code lu, ce que la boîte portait, et les trois touches rappelées à l'écran : c'est
    // tout ce dont l'opérateur dispose, et c'est suffisant pour décider sans lâcher le carton.
    await expect(bandeau).toContainText(CODE_MEMORISE);
    await expect(bandeau).toContainText('Lot LOT77');
    await expect(bandeau).toContainText('30/06/2028');
    await expect(bandeau).toContainText('(lus sur la boîte)');
    await expect(bandeau).toContainText('Entrée · A · Échap');
    // La ligne a un vrai code : son CIP ne sera pas touché, et le bouton l'annonce.
    await expect(bandeau.getByRole('button', { name: 'Associer à cet article' })).toBeVisible();
  });

  await etape(3, async () => {
    // Entrée fait la même chose, mais une touche ne se photographie pas : c'est le bouton
    // qu'il faut montrer.
    await bandeau.getByRole('button', { name: 'Associer à cet article' }).click();
    await expect(page.locator('[role="alert"], .toast, ngb-toast').filter({ hasText: /Code reconnu/i }).first()).toBeVisible();
    // Le bandeau se referme et le scan est rejoué : la ligne suit le chemin ordinaire.
    await expect(bandeau).toHaveCount(0);
  });

  // ── Remise en état : le code mémorisé n'appartient qu'à ce parcours.
  const memorises = lire(`SELECT count(*) FROM produit_code_alias WHERE code = '${CODE_MEMORISE}'`);
  expect(Number(memorises), 'le code scanné n’a pas été mémorisé').toBeGreaterThan(0);
  executer(`DELETE FROM produit_code_alias WHERE code = '${CODE_MEMORISE}'`);
});
