import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Une officine peut être rentable et manquer d'argent. C'est même le cas le plus fréquent : le
 * stock est payé comptant, le tiers payant règle à quatre-vingt-dix jours, et l'écart entre
 * les deux se finance sur la trésorerie.
 *
 * Le rapport le pose en trois délais :
 *
 *   • DIO — le nombre de jours de stock détenu ;
 *   • DSO — le nombre de jours pour se faire payer ;
 *   • DPO — le nombre de jours obtenus des fournisseurs.
 *
 * Le BFR est ce que l'officine doit avancer (stock + créances − dettes), et le CYCLE DE
 * TRÉSORERIE — DIO + DSO − DPO — le nombre de jours pendant lesquels elle l'avance. Négocier
 * un délai fournisseur, c'est agir sur le troisième terme sans toucher aux ventes.
 */
scenario('RPT-30', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await ouvrirRapport(page, 'finance', 'BFR');
    await expect(contenu).toContainText(/BFR & Cycle d'exploitation/, { timeout: 20000 });
  });

  await etape(2, async () => {
    await expect(contenu).toContainText(/STOCK \(DIO\)/i);
    await expect(contenu).toContainText(/CRÉANCES TP \(DSO\)/i);
    await expect(contenu).toContainText(/DETTES FOURNISSEURS \(DPO\)/i);
    // La formule est affichée sous chaque indicateur : le lecteur peut refaire le calcul.
    await expect(contenu).toContainText(/Stock \+ Créances − Dettes/);
    await expect(contenu).toContainText(/DIO \+ DSO − DPO/);
  });

  await etape(3, async () => {
    // Les flux mensuels : ce que les organismes doivent, face à ce que les fournisseurs
    // réclament, mois par mois. Deux courbes qui ne se croisent pas au même moment.
    await expect(
      page.getByRole('heading', { name: /Créances TP vs Achats fournisseurs/ }),
    ).toBeVisible();
  });
});
