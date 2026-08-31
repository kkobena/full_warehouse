import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * « Démarque inconnue » est un terme comptable : ce que l'officine a perdu sans savoir
 * comment. Tout l'objet de l'analyse des écarts est de le réduire — chaque ligne qualifiée
 * en casse, vol ou erreur de saisie sort de l'inconnu. Le résumé montre ce partage.
 */
scenario('STK-14', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/inventaire');
    await ouvrirOnglet(page, /Clôturés/);
    await expect(lignes.first()).toContainText(/clôturé/i);
    // Le chevron de la ligne, nommé en écrivant STK-16 : les actions de la ligne étaient
    // des boutons sans nom accessible.
    await page.getByRole('button', { name: 'Déplier le détail' }).first().click();
    await expect(page.getByText(/Analyse des écarts/i).first()).toBeVisible();
  });

  await etape(2, async () => {
    // La répartition annoncée : des causes nommées, chacune avec son nombre de produits,
    // ses unités et sa part. Un total de démarque sans détail ne serait qu'un chiffre.
    const resume = page.locator('app-gap-summary');
    await expect(resume).toContainText('Vol');
    await expect(resume).toContainText('Casse');
    await expect(resume).toContainText(/Total démarque/i);
    await expect(resume).toContainText(/%/);
  });
});
