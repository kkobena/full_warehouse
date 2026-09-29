import { expect } from '@playwright/test';
import { carte } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le classement fournisseurs de l'accueil est le résumé du rapport « Performance des
 * fournisseurs » (RPT-33) : même score, mêmes délais, mais réduit au Top 5. Il suit la période
 * du tableau de bord — le bloc n'a plus de sélecteur propre. Sur une journée, une officine qui
 * n'a rien commandé voit des montants à zéro : c'est l'année qui montre l'écran utile.
 */
scenario('HOME-09', async ({ etape, page }) => {
  const bloc = carte(page, 'Achats par fournisseur');
  const periode = (libelle: string) =>
    page.locator('.dashboard-periode-selector').getByRole('button', { name: libelle, exact: true });

  await etape(1, async () => {
    await page.goto('/');
    await expect(bloc).toBeVisible();
  });

  await etape(2, async () => {
    await periode('Année').click();
    await expect(periode('Année')).toHaveClass(/active/);
    await bloc.scrollIntoViewIfNeeded();
  });

  await etape(3, async () => {
    // Un classement, donc un premier rang, un score et un délai — les trois composantes que
    // la légende annonce, sur la période qui vient d'être choisie.
    await expect(bloc).toContainText(/#1/);
    await expect(bloc).toContainText(/Score\s*:\s*\d/);
    await expect(bloc).toContainText(/\d+\s*j délai/);

    // Et un classement DÉCROISSANT sur la période affichée. L'API classe par volume douze
    // mois : la liste montrait, sous « 30 j », un premier moins-disant que le deuxième.
    const montants = await bloc.locator('li .fs-6').allInnerTexts();
    const valeurs = montants.map(m => Number(m.replace(/\D/g, '')));
    expect(valeurs.length).toBeGreaterThan(1);
    expect(valeurs).toEqual([...valeurs].sort((a, b) => b - a));

    // Le badge doit désigner le bon fournisseur : « #1 » porte le plus gros montant de la
    // période, sinon le classement se contredit lui-même à la première ligne.
    const premier = bloc.locator('li').filter({ hasText: /#1/ }).first();
    const nom = (await premier.locator('.fw-semibold').first().innerText()).trim();
    const montantDe = async (fournisseur: string) =>
      Number(
        (await bloc.locator('li').filter({ hasText: fournisseur }).first()
          .locator('.fs-6').innerText()).replace(/\D/g, ''),
      );

    const surLAnnee = await montantDe(nom);
    expect(surLAnnee, 'le premier rang porte le plus gros montant').toBe(Math.max(...valeurs));

    // L'année englobe le mois : le cumul du même fournisseur ne peut pas augmenter quand on
    // resserre la période. On le suit par son NOM — il peut aussi sortir du Top, montant nul.
    await periode('Mois').click();
    await expect(periode('Mois')).toHaveClass(/active/);
    const surLeMois = async () =>
      (await bloc.locator('li').filter({ hasText: nom }).count()) === 0 ? 0 : montantDe(nom);
    await expect.poll(surLeMois, { timeout: 5000 }).toBeLessThanOrEqual(surLAnnee);
  });
});
