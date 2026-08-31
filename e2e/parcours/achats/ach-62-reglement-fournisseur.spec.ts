import { expect } from '@playwright/test';
import { assurerCaisseOuverte, choisirDansSelect, ouvrirOnglet, saisirMontant } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Régler un grossiste, c'est imputer un montant sur un compte qui court : le solde recule
 * d'autant, et le règlement reste attaché au fournisseur avec sa date, son mode et sa
 * référence. C'est cette trace qui permettra, trois mois plus tard, de retrouver à quel
 * virement correspond une ligne du relevé bancaire.
 *
 * Le montant est borné par le solde : on ne règle pas plus qu'on ne doit, et le bouton reste
 * inerte tant que la saisie sort de ces limites — comme il reste inerte sans référence pour un
 * règlement non espèces (ACH-68).
 *
 * Parcours ÉCRIVANT dans la base : il enregistre un règlement, que la restauration de
 * l'instantané efface avant la campagne suivante.
 */
scenario('ACH-62', async ({ etape, page }) => {
  const ecran = page.locator('app-comptes-fournisseurs');

  // Un règlement fournisseur SORT de la caisse : il lui faut une caisse ouverte, comme une
  // vente. C'est le serveur qui le dit — « Votre caisse est fermée » — et c'est la première
  // chose à vérifier quand l'enregistrement est refusé.
  await assurerCaisseOuverte(page);

  /** Le montant déjà réglé sur le compte ouvert, lu dans l'en-tête du détail. */
  const montantRegle = async (): Promise<number> => {
    const texte = await ecran.innerText();
    const trouve = texte.match(/Réglé\s*:\s*([\d\s  ]+)/);
    return trouve ? Number(trouve[1].replace(/[^\d]/g, '')) : NaN;
  };
  let regleAvant = 0;

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Comptes fournisseurs/);
    await expect(ecran.locator('tbody tr').first()).toBeVisible();
    await ecran.locator('tbody tr').first().click();
    await expect(ecran).toContainText('Solde');
    regleAvant = await montantRegle();
  });

  await etape(2, async () => {
    await ecran.getByRole('tab', { name: 'Régler' }).click();
    await saisirMontant(page, '#montant', '5000');
    await choisirDansSelect(page, 'modeReglement', 'Virement');
    await page.locator('#reference').fill('VIR-2026-0731');
  });

  await etape(3, async () => {
    await ecran.getByRole('button', { name: 'Enregistrer' }).click();
    // Un règlement se confirme : la fenêtre rappelle le montant et le mode avant d'imputer
    // quoi que ce soit sur le compte du grossiste.
    const confirmation = page.locator('.modal-content');
    await expect(confirmation).toBeVisible();
    await confirmation.getByRole('button', { name: /Oui|Confirmer/ }).click();
    await expect(confirmation).toBeHidden();
    // L'enregistrement referme le détail et rend la main à la liste des comptes : on rouvre
    // le fournisseur pour constater le résultat, comme le ferait celui qui vient de payer.
    await expect(ecran.locator('tbody tr').first()).toBeVisible();
    await ecran.locator('tbody tr').first().click();
    // Le montant réglé a AUGMENTÉ de ce qu'on vient de payer, et le solde a reculé d'autant.
    // On compare à l'état d'avant plutôt qu'à une valeur fixe : le compte de démonstration
    // porte déjà des règlements, et il en portera un de plus à chaque exécution.
    await expect(async () => {
      expect(await montantRegle()).toBe(regleAvant + 5000);
    }).toPass({ timeout: 10000 });
  });
});
