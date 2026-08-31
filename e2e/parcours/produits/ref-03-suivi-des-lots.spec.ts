import { expect } from '@playwright/test';
import { chercherAuCatalogue } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le modèle annonçait une « mise à jour rapide (détail) », un formulaire allégé qui n'existe
 * pas : ce que l'application offre, ce sont des interrupteurs qui s'enregistrent SUR-LE-CHAMP
 * dans le panneau de détail. Corrigé dans cahier-recette.model.ts en écrivant ce parcours.
 *
 * Le suivi des lots est celui qui change le plus de choses en aval : activé, la réception de
 * ce produit exigera un numéro de lot et une date de péremption ; coupé, elle n'en demandera
 * pas. C'est un réglage qu'on corrige souvent depuis le comptoir de réception, sans vouloir
 * rouvrir toute la fiche.
 *
 * Parcours ÉCRIVANT dans la base : il rétablit le réglage d'origine.
 */
scenario('REF-03', async ({ etape, page }) => {
  const produit = 'ARNICA';
  const interrupteur = () => page.getByRole('switch', { name: 'Suivi des lots' });

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    // Le panneau s'ouvre sur la synthèse : c'est là que vivent les réglages immédiats.
    await expect(page.getByRole('tab', { name: 'Synthèse' })).toBeVisible();
    await expect(interrupteur()).toBeVisible();
  });

  await etape(2, async () => {
    // Aucun bouton « Enregistrer » : la bascule EST l'enregistrement. C'est ce qui fait
    // gagner du temps, et ce qui demande de savoir que le geste est immédiat.
    const avant = await interrupteur().isChecked();
    await interrupteur().click();
    await expect(interrupteur()).toBeChecked({ checked: !avant });
  });

  await etape(3, async () => {
    // On rétablit le réglage : le parcours illustre le geste, il ne change pas durablement
    // les règles de réception d'un produit du catalogue de démonstration.
    const apres = await interrupteur().isChecked();
    await interrupteur().click();
    await expect(interrupteur()).toBeChecked({ checked: !apres });
  });
});
