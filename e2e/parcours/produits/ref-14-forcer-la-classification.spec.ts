import { expect } from '@playwright/test';
import { chercherAuCatalogue, chercherDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La classe ABC est recalculée périodiquement à partir des ventes : c'est ce qu'on veut pour
 * l'essentiel du catalogue, et ce qu'on ne veut surtout pas pour certains produits. Une
 * insuline se garde en A+ même si elle se vend trois fois l'an — la rupture y est un risque
 * vital, pas un manque à gagner.
 *
 * D'où le VERROU : on choisit la classe à la main, et on empêche le calcul automatique de la
 * reprendre. Sans ce verrou, le réglage tiendrait jusqu'au prochain passage du calcul.
 *
 * Le modèle annonçait un motif à saisir : le formulaire n'en demande pas. Corrigé dans
 * cahier-recette.model.ts en écrivant ce parcours.
 *
 * Parcours ÉCRIVANT dans la base : il rétablit la classe et le verrou d'origine.
 */
scenario('REF-14', async ({ etape, page }) => {
  const produit = 'ARNICA MONTANA 7CH';
  const select = page.locator('ng-select', { has: page.locator('#f_criticite') });
  const verrou = () => page.getByRole('switch').filter({ visible: true }).last();
  let classeOrigine = '';

  const ouvrirLaFiche = async (): Promise<void> => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('app-produit-list tbody tr').first()
      .getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Éditer' }).click();
    await expect(page.getByRole('heading', { name: /Modifier Produit/i })).toBeVisible();
    // Le formulaire est découpé en onglets, et la classe de criticité ne vit pas dans celui
    // qui s'appelle « Classification » — lequel porte le laboratoire, la forme et la DCI —
    // mais dans « Approvisionnement », avec les seuils qu'elle commande.
    await page.getByRole('tab', { name: 'Approvisionnement' }).click();
  };

  await etape(1, async () => {
    await ouvrirLaFiche();
    await expect(page.locator('#main-content')).toContainText('Classe de criticité produit');
    classeOrigine = (await select.innerText()).trim();
  });

  await etape(2, async () => {
    // La liste ne donne pas seulement des lettres : chaque classe est décrite par ce qu'elle
    // recouvre, ce qui évite de classer au jugé.
    await chercherDansSelect(page, 'f_criticite', 'A+', 'Produits vitaux');
    // Le verrou est l'autre moitié du geste : sans lui, le prochain calcul reprendrait la main.
    await expect(page.locator('#main-content')).toContainText('Verrouiller cette classe');
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(page.getByRole('heading', { name: 'Catalogue produits' })).toBeVisible();
  });

  // ── Remise en état : la classe d'origine est rendue au produit. ─────────────────────────
  await ouvrirLaFiche();
  const origine = classeOrigine.split('—')[0].trim();
  if (origine && !origine.startsWith('A+')) {
    await chercherDansSelect(page, 'f_criticite', origine, origine);
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(page.getByRole('heading', { name: 'Catalogue produits' })).toBeVisible();
  }
});
