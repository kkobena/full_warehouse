import { expect } from '@playwright/test';
import { carte } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Deux blocs côte à côte qui répondent à deux questions différentes : par quel moyen l'argent
 * est entré (espèces, mobile money, carte), et pour le compte de qui il reste à entrer (part
 * des tiers payants). Les confondre est l'erreur la plus courante à la lecture d'un tableau
 * de bord d'officine — d'où l'image.
 */
scenario('HOME-08', async ({ etape, page }) => {
  // « Auj. » est la période d'ouverture : selon l'heure et le jour, elle peut ne porter aucune
  // vente, et les deux blocs s'affichent alors vides à juste titre. Le mois en cours est la
  // période qui montre l'écran vivant — même choix qu'en HOME-07.
  const passerAuMois = async () => {
    const mois = page.getByRole('button', { name: /Mois$/ });
    await mois.click();
    await expect(mois).toHaveClass(/active/);
  };

  await etape(1, async () => {
    await page.goto('/');
    await expect(page.locator('.kpi-strip-item').filter({ hasText: 'CA Net' })).toBeVisible();
    await passerAuMois();
  });

  await etape(2, async () => {
    const reglements = carte(page, 'Total par mode de règlement');
    const tiersPayants = carte(page, 'Ventes par tiers payant');

    // Les deux cartes affichent « Aucune donnée sur la période » quand elles sont vides :
    // l'ancrage porte donc sur un montant, pas sur le titre de la carte.
    await expect(reglements).not.toContainText('Aucune donnée');
    await expect(tiersPayants).not.toContainText('Aucune donnée');
    await expect(reglements).toContainText(/\d/);
    await expect(tiersPayants).toContainText(/#1/);

    // Les deux blocs sont sous la bande d'indicateurs : sans ce défilement, la capture
    // montrerait le haut de l'accueil et non ce que la légende annonce.
    await reglements.scrollIntoViewIfNeeded();
  });
});
