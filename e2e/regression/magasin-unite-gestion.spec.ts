import { expect, Page, test } from '@playwright/test';
import { lire } from '../src/base-de-donnees';

/**
 * Création d'une unité de gestion depuis la fiche du magasin (`/magasin`).
 *
 * Une unité de gestion — une parapharmacie, par exemple — tient son stock, sa caisse et ses
 * commandes au même endroit que l'officine, mais facture sous l'identité FNE de celle-ci. Le
 * parcours vérifie en base ce que l'écran ne montre pas : le type et le rattachement, que l'ancien
 * formulaire perdait à chaque modification (l'unité redevenait une officine détachée).
 *
 * Parcours ÉCRIVANT dans la base : l'unité « ESSAI E2E » est supprimée à la fin, par l'API.
 */
const NOM = 'ESSAI E2E PARA';

// `0` pour « aucune » : `lire()` rogne la sortie, et une valeur vide y laisserait la ligne « SET ».
const idDeLUnite = (): string => lire(`SELECT COALESCE(MAX(id), 0) FROM magasin WHERE name = '${NOM}'`);

async function supprimerLUnite(page: Page): Promise<void> {
  const id = idDeLUnite();
  if (id === '0') {
    return;
  }
  const jeton = await page.evaluate(() => localStorage.getItem('pharma_smart_access_token'));
  const reponse = await page.request.delete(`/api/magasins/${id}`, { headers: { Authorization: `Bearer ${jeton}` } });
  expect(reponse.ok(), `suppression de l'unité d'essai ${id} : ${reponse.status()}`).toBeTruthy();
}

test('unité de gestion : créer, retrouver, modifier sans perdre son rattachement', async ({ page }) => {
  test.setTimeout(120_000);
  const officine = lire(`SELECT id FROM magasin WHERE type_magasin = 'OFFICINE' ORDER BY id LIMIT 1`);

  await page.goto('/magasin');
  await supprimerLUnite(page); // reste d'une exécution interrompue

  try {
    // Création : rattachée d'office à l'officine, sans identité FNE à saisir.
    await page.getByRole('button', { name: 'Nouvelle unité de gestion' }).click();
    await expect(page).toHaveURL(/magasin\/unite-gestion\/new$/);
    await expect(page.getByRole('note')).toContainText("sous l'identité FNE de son officine");
    await expect(page.locator('#fnePointOfSale')).toHaveCount(0);
    await expect(page.locator('#field_phone')).not.toHaveValue('');

    await page.locator('#field_fullName').fill('Parapharmacie — essai e2e');
    await page.locator('#field_name').fill(NOM);
    await page.getByRole('button', { name: 'Enregistrer' }).click();

    await expect(page).toHaveURL(/magasin$/);
    const ligne = page.locator('app-unites-gestion-list tbody tr', { hasText: NOM });
    await expect(ligne).toBeVisible();
    expect(lire(`SELECT type_magasin || '/' || parent_id FROM magasin WHERE name = '${NOM}'`)).toBe(`UNITE_GESTION/${officine}`);

    // Modification : l'unité reste une unité, rattachée à la même officine.
    await ligne.getByRole('button', { name: `Éditer l’unité ${NOM}` }).click();
    await expect(page).toHaveURL(/magasin\/\d+\/edit$/);
    await expect(page.locator('#field_name')).toHaveValue(NOM);
    await page.locator('#field_note').fill('Fermée le lundi');
    await page.getByRole('button', { name: 'Enregistrer' }).click();

    await expect(page).toHaveURL(/magasin$/);
    expect(lire(`SELECT type_magasin || '/' || parent_id || '/' || note FROM magasin WHERE name = '${NOM}'`)).toBe(
      `UNITE_GESTION/${officine}/Fermée le lundi`,
    );
  } finally {
    await supprimerLUnite(page);
  }
  expect(idDeLUnite()).toBe('0');
});
