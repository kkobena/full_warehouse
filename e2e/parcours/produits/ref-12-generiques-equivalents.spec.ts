import { expect } from '@playwright/test';
import { chercherAuCatalogue, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le princeps est en rupture et le client attend : la question n'est pas « qu'ai-je en
 * stock ? » mais « qu'ai-je qui contienne la MÊME MOLÉCULE ? ». L'équivalence se fonde sur la
 * DCI, pas sur le nom commercial — DOLIPRANE, EFFERALGAN et PARACETAMOL GE partagent la même
 * substance et se substituent l'un à l'autre.
 *
 * Le modèle annonçait un onglet « Génériques » ; la liste vivait en réalité dans une fenêtre
 * que RIEN n'ouvrait — elle était écrite, son gestionnaire aussi, mais aucune entrée de menu
 * ne la déclenchait. Elle est devenue l'onglet que le modèle décrivait, à côté des rayons.
 *
 * Parcours en LECTURE.
 */
scenario('REF-12', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const onglet = page.locator('app-produit-generiques-tab');

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await expect(page.locator('tbody tr').filter({ visible: true }).first()).toContainText(produit);
  });

  await etape(2, async () => {
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    await ouvrirOnglet(page, 'Génériques');
    await expect(onglet).toBeVisible();
    // La liste ne se contente pas de nommer : elle porte de quoi décider — le prix de
    // l'équivalent, son stock disponible, et la nature de l'équivalence (générique ou
    // substitut thérapeutique).
    await expect(onglet).toContainText(/PARACETAMOL|DOLIPRANE|EFFERALGAN/i);
    await expect(onglet).toContainText('Prix de vente');
    await expect(onglet).toContainText('Stock');
  });
});
