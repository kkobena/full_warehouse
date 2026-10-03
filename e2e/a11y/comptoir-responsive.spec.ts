/**
 * Tenue de l'écran de vente aux tailles d'écran des officines (docs/PLAN-STYLES-COMPTOIR.md, §6) : portables 1366,
 * iPad Pro 13, Surface Pro 10, Nest Hub Max, 24 pouces à 100 et 125 %. Pour chaque taille, un panier de cinq lignes :
 *
 *  - la page ne défile pas d'elle-même (la zone de vente occupe la place sous la barre de navigation, pas `100vh`) ;
 *  - le règlement est entièrement à l'écran sans défiler (il reste collé au bas de la zone qui défile) ;
 *  - les types de vente sont en rangée à 1440 px et moins, en colonne au-delà, avec des onglets de 44 px.
 *
 * Le débordement horizontal de la page est relevé dans `target/a11y-comptoir/responsive.json` mais n'est pas une
 * règle d'acceptation : il vient du menu principal de l'application (`navbar-collapse`), commun à tous les écrans.
 *
 * ÉCRIT dans la base : chaque taille ouvre une vente puis l'abandonne. À jouer sur la base de démonstration.
 */
import { expect, test, type Page } from '@playwright/test';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../src/actions';
import { RACINE } from '../src/config';

const DOSSIER = join(RACINE, 'target', 'a11y-comptoir');
const SEUIL_NAV_HORIZONTALE = 1440;

const TAILLES = [
  { nom: 'iPad Pro 13 paysage', largeur: 1376, hauteur: 920 },
  { nom: 'iPad Pro 13 portrait', largeur: 1032, hauteur: 1260 },
  { nom: 'Surface Pro 10 paysage', largeur: 1440, hauteur: 830 },
  { nom: 'Surface Pro 10 portrait', largeur: 960, hauteur: 1250 },
  { nom: 'Nest Hub Max', largeur: 1280, hauteur: 720 },
  { nom: 'portable 1366', largeur: 1366, hauteur: 650 },
  { nom: '24 pouces à 125 %', largeur: 1536, hauteur: 740 },
  { nom: '24 pouces à 100 %', largeur: 1920, hauteur: 960 },
] as const;

const PRODUITS = ['DOLIPRANE 1G', 'PARACETAMOL 1G', 'DOLIPRANE 500MG', 'AMOXI', 'IBUPRO'];

async function abandonnerVente(page: Page): Promise<void> {
  const annuler = page.locator('button:has(i.pi-times)').last();
  if (await annuler.isVisible().catch(() => false)) {
    await annuler.click();
    const confirmation = page.locator('.modal-content');
    await confirmation.waitFor({ state: 'visible', timeout: 5000 }).catch(() => undefined);
    if (await confirmation.isVisible().catch(() => false)) {
      await confirmation.getByRole('button', { name: 'Oui' }).click();
      await expect(confirmation).toBeHidden();
    }
  }
}

test.describe.configure({ mode: 'serial' });
test.setTimeout(180_000);

const releves: Record<string, unknown> = {};

for (const taille of TAILLES) {
  test(`${taille.nom} (${taille.largeur} × ${taille.hauteur})`, async ({ page }) => {
    await page.setViewportSize({ width: taille.largeur, height: taille.hauteur });
    try {
      await assurerCaisseOuverte(page);
      await assurerPanierVide(page);
      await page.goto('/sales-home');
      await expect(page.locator('#produitbox')).toBeVisible();
      for (const produit of PRODUITS) {
        await chercherProduit(page, produit);
        await ajouterAuPanier(page, '1');
      }
      await expect(page.locator('tbody tr[data-line-id]')).toHaveCount(PRODUITS.length);
      await page.mouse.move(2, taille.hauteur - 2);
      await page.waitForTimeout(500);

      const mesure = await page.evaluate(() => {
        const doc = document.documentElement;
        const reglement = document.querySelector('app-payment-mode')!.getBoundingClientRect();
        const onglets = [...document.querySelectorAll('.sales-sidebar .nav-link')].map(a => a.getBoundingClientRect());
        return {
          defilementPage: doc.scrollHeight - innerHeight,
          reglementBas: Math.round(reglement.bottom),
          fenetre: innerHeight,
          navHorizontale: new Set(onglets.map(r => Math.round(r.top))).size === 1,
          hauteursOnglets: onglets.map(r => Math.round(r.height)),
          debordementHorizontal: Math.max(0, doc.scrollWidth - innerWidth),
        };
      });
      releves[taille.nom] = mesure;

      expect(mesure.defilementPage, 'la page ne doit pas défiler d’elle-même').toBeLessThanOrEqual(0);
      expect(mesure.reglementBas, 'le règlement doit être entièrement à l’écran').toBeLessThanOrEqual(mesure.fenetre);
      expect(mesure.navHorizontale, 'types de vente en rangée à 1440 px et moins, en colonne au-delà').toBe(
        taille.largeur <= SEUIL_NAV_HORIZONTALE,
      );
      if (mesure.navHorizontale) {
        for (const h of mesure.hauteursOnglets) {
          expect(h, 'onglets de 44 px').toBeGreaterThanOrEqual(44);
        }
      }
    } finally {
      await abandonnerVente(page);
      mkdirSync(DOSSIER, { recursive: true });
      writeFileSync(join(DOSSIER, 'responsive.json'), JSON.stringify(releves, null, 2));
    }
  });
}
