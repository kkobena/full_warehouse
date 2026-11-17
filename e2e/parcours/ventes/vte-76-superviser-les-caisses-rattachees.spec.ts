import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { lire } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Le titulaire veut savoir ce qu'a fait la caisse de sa parapharmacie sans y aller et sans
 * demander les chiffres à son vendeur. La gestion de caisse lui montre donc les sessions de son
 * officine ET de ce qui lui est rattaché.
 *
 * La colonne « Magasin » et son filtre n'apparaissent QUE s'il y a plus d'un magasin à lire :
 * une officine seule n'en voit ni l'un ni l'autre, et c'est voulu — une colonne qui répète
 * trois cents fois le même nom n'informe personne. Ce parcours ne peut donc se jouer que si le
 * jeu de démonstration porte un magasin rattaché ; il le vérifie en base avant d'ouvrir
 * l'écran, pour qu'une colonne manquante ne passe pas pour une régression.
 *
 * Le droit de supervision n'élargit que la LECTURE : une caisse ne s'ouvre et ne se clôture que
 * dans son propre magasin. Ce parcours lit, et rien d'autre.
 */
scenario('VTE-76', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  const unite = lire(`SELECT name FROM magasin WHERE type_magasin = 'UNITE_GESTION' ORDER BY id LIMIT 1`);
  expect(unite, 'la démo doit porter une unité de gestion rattachée à l’officine').not.toBe('');
  const caissesDeLUnite = Number(
    lire(`SELECT count(*) FROM cash_register cr JOIN magasin m ON m.id = cr.magasin_id
           WHERE m.type_magasin = 'UNITE_GESTION'`),
  );
  expect(caissesDeLUnite, 'l’unité doit avoir tenu caisse pour qu’il y ait quelque chose à superviser').toBeGreaterThan(0);

  await etape(1, async () => {
    await page.goto('/mvt-caisse');
    await ouvrirOnglet(page, /Gestion de caisse/);
    await expect(lignes.first()).toBeVisible();
  });

  await etape(2, async () => {
    // La colonne est la trace visible du cloisonnement : elle dit de quel magasin vient chaque
    // session. Son absence signifierait que la portée de lecture s'est refermée sur l'officine.
    await expect(
      page.getByRole('columnheader', { name: 'Magasin', exact: true }),
      'la colonne « Magasin » doit apparaître dès qu’il y a plus d’un magasin à lire',
    ).toBeVisible();
  });

  await etape(3, async () => {
    // Le filtre n'a pas d'identifiant : on le désigne par son invite, seul repère stable.
    const filtreMagasin = page
      .locator('ng-select')
      .filter({ has: page.locator('.ng-placeholder', { hasText: 'Magasin' }) })
      .first();
    await expect(filtreMagasin).toBeVisible();
    await filtreMagasin.click();
    await page.locator('.ng-option', { hasText: unite }).first().click();
    await expect(filtreMagasin).toContainText(unite);

    // La liste s'est rechargée : toutes ses lignes viennent de l'unité, et elle n'est pas vide.
    await expect(lignes.first()).toContainText(unite);
    const affichees = await lignes.count();
    expect(affichees, 'le filtre ne doit pas vider la liste').toBeGreaterThan(0);
    for (let rang = 0; rang < affichees; rang++) {
      await expect(lignes.nth(rang), 'aucune caisse d’un autre magasin ne doit rester').toContainText(unite);
    }
    // Le fond de tiroir de l'unité : la supervision donne les chiffres, pas seulement les noms.
    // L'espace des milliers dépend du séparateur de la locale — on l'accepte sous ses formes.
    await expect(lignes.first()).toContainText(/30[\s  ]?000/);
  });
});
