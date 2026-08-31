import { expect } from '@playwright/test';
import { carte } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le classement fournisseurs de l'accueil est le résumé du rapport « Performance des
 * fournisseurs » (RPT-33) : même score, mêmes délais, mais réduit au Top 5 et doublé d'un
 * choix de période. Sur trente jours, une officine qui n'a rien commandé voit des montants à
 * zéro — c'est la vue douze mois qui montre l'écran utile.
 */
scenario('HOME-09', async ({ etape, page }) => {
  const bloc = carte(page, 'Achats par fournisseur');

  await etape(1, async () => {
    await page.goto('/');
    await expect(bloc).toBeVisible();
  });

  await etape(2, async () => {
    const douzeMois = bloc.getByRole('button', { name: '12 mois', exact: true });
    await douzeMois.click();
    await expect(douzeMois).toHaveClass(/active/);
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
  });
});
