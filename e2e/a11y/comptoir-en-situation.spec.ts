/**
 * Contraste (axe) des styles du comptoir EN SITUATION (docs/PLAN-STYLES-COMPTOIR.md, §4) : ce que `comptoir.spec.ts`
 * ne voit pas parce qu'il ouvre des écrans vides — le panier avec ses lignes, « À ENCAISSER » et les boutons,
 * l'encart client avec les données de l'assuré, l'écran dépôt, le panneau des ventes en attente, et chaque
 * pastille de mode de règlement.
 *
 * ÉCRIT dans la base : chaque scénario ouvre une vente puis l'abandonne dans le `finally`. À jouer sur la base de
 * démonstration. Même règle d'acceptation que `comptoir.spec.ts` : aucun défaut. Les résultats sont écrits dans `target/a11y-comptoir/axe-en-situation.json`.
 *
 * Limite d'axe, qui vaut ici comme ailleurs : il ne sait pas évaluer un texte posé sur un dégradé. Pour le bandeau
 * (`.pharma-sales-header`), `mesurerBandeau` prend le relais : il échantillonne les pixels du fond derrière chaque
 * texte, icône et bordure, et contrôle la taille des cibles. Les autres zones sur dégradé restent à la charge du
 * calcul (`comptoir-contraste.spec.ts`).
 */
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../src/actions';
import { RACINE } from '../src/config';

const DOSSIER = join(RACINE, 'target', 'a11y-comptoir');
// Le style Billetage est le seul depuis le 2026-10-03 : les trois autres ont été supprimés.
const STYLES = ['billetage'] as const;

type Racine = 'app-sales-home' | 'app-vente-depot';

interface ElementBandeau {
  genre: 'texte' | 'icône' | 'bordure';
  libelle: string;
  couleur: string;
  opacite: number;
  taille: number;
  gras: boolean;
  x: number;
  y: number;
  l: number;
  h: number;
}

/**
 * Contraste réel du bandeau, là où axe ne voit rien (fond dégradé) : le texte est rendu transparent, la page est
 * photographiée, et on cherche le pixel de fond le moins contrasté derrière chaque élément. Seuils WCAG 2.2 :
 * 4,5:1 le texte (3:1 s'il est large), 3:1 les icônes et les bordures de contrôle. Contrôle aussi la taille des
 * cibles (44 px). Rend la liste des défauts ; ne fait rien si une modale ou un panneau voile le bandeau.
 */
async function mesurerBandeau(page: Page): Promise<string[]> {
  const voile = await page.locator('.modal-content:visible, .offcanvas.show').count();
  if (voile > 0 || (await page.locator('.pharma-sales-header').count()) === 0) {
    return [];
  }
  // La page défile de quelques pixels après certains clics (la zone de vente est haute comme la fenêtre, sous la barre
  // de navigation) : le bandeau passerait sous cette barre et la mesure porterait sur elle.
  await page.evaluate(() => window.scrollTo(0, 0));
  const { elements, petites } = await page.evaluate(() => {
    const entete = document.querySelector('.pharma-sales-header') as HTMLElement;
    const opaciteCumulee = (e: Element): number => {
      let o = 1;
      for (let n: Element | null = e; n && n !== document.body; n = n.parentElement) {
        o *= Number(getComputedStyle(n).opacity);
      }
      return o;
    };
    const liste: ElementBandeau[] = [];
    const ajouter = (e: Element, r: DOMRect, libelle: string, genre: ElementBandeau['genre'], couleur?: string): void => {
      const cs = getComputedStyle(e);
      if (cs.visibility === 'hidden' || cs.display === 'none' || r.width < 2 || r.height < 2) {
        return;
      }
      liste.push({
        genre,
        libelle,
        couleur: couleur ?? cs.color,
        opacite: opaciteCumulee(e),
        taille: parseFloat(cs.fontSize),
        gras: Number(cs.fontWeight) >= 700,
        x: r.x,
        y: r.y,
        l: r.width,
        h: r.height,
      });
    };
    const parcours = document.createTreeWalker(entete, NodeFilter.SHOW_TEXT);
    while (parcours.nextNode()) {
      const noeud = parcours.currentNode;
      if (!noeud.textContent?.trim() || !noeud.parentElement) {
        continue;
      }
      const plage = document.createRange();
      plage.selectNodeContents(noeud);
      ajouter(noeud.parentElement, plage.getBoundingClientRect(), noeud.textContent.trim().slice(0, 28), 'texte');
    }
    entete
      .querySelectorAll('i.pi')
      .forEach(i => ajouter(i, i.getBoundingClientRect(), (i.className.match(/pi-[\w-]+/g) ?? []).pop() ?? 'icône', 'icône'));
    entete.querySelectorAll('button, .ng-select-container, input').forEach(e => {
      const cs = getComputedStyle(e);
      // Une bordure transparente (bouton « texte ») n'est pas une bordure.
      if (parseFloat(cs.borderTopWidth) > 0 && cs.borderTopStyle !== 'none' && !/, 0\)$/.test(cs.borderTopColor)) {
        const nom = (e.getAttribute('aria-label') ?? e.textContent ?? '').trim().replace(/\s+/g, ' ').slice(0, 28);
        ajouter(e, e.getBoundingClientRect(), nom, 'bordure', cs.borderTopColor);
      }
    });
    const cibles = [...entete.querySelectorAll('button, [role=combobox], input')]
      .map(e => ({ libelle: (e.getAttribute('aria-label') ?? e.textContent ?? '').trim().slice(0, 28), r: e.getBoundingClientRect() }))
      // Le champ de saisie interne d'un ng-select (largeur nulle) n'est pas une cible.
      .filter(c => c.r.width >= 2 && c.r.height > 0 && (c.r.width < 44 || c.r.height < 44))
      .map(c => `${c.libelle || 'sans nom'} ${Math.round(c.r.width)} × ${Math.round(c.r.height)}`);
    return { elements: liste, petites: cibles };
  });

  const style = await page.addStyleTag({
    content:
      '.pharma-sales-header *{color:transparent!important;text-shadow:none!important;-webkit-text-fill-color:transparent!important;border-color:transparent!important} .pharma-sales-header i{opacity:0!important}',
  });
  await page.waitForTimeout(300);
  const photo = `data:image/png;base64,${(await page.screenshot()).toString('base64')}`;
  await style.evaluate((e: Element): void => e.remove());

  const insuffisants = await page.evaluate(
    async ([source, liste]) => {
      const image = new Image();
      image.src = source as string;
      await image.decode();
      const toile = document.createElement('canvas');
      toile.width = image.width;
      toile.height = image.height;
      const g = toile.getContext('2d') as CanvasRenderingContext2D;
      g.drawImage(image, 0, 0);
      const lum = (c: number[]): number => {
        const f = (v: number): number => (v / 255 <= 0.03928 ? v / 255 / 12.92 : Math.pow((v / 255 + 0.055) / 1.055, 2.4));
        return 0.2126 * f(c[0]) + 0.7152 * f(c[1]) + 0.0722 * f(c[2]);
      };
      const rapport = (a: number[], b: number[]): number => {
        const x = lum(a);
        const y = lum(b);
        return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
      };
      const k = image.width / innerWidth;
      const defauts: string[] = [];
      for (const e of liste as ElementBandeau[]) {
        const c = (e.couleur.match(/[\d.]+/g) ?? []).map(Number);
        const alpha = (c.length > 3 ? c[3] : 1) * e.opacite;
        // Bordure : fond pris juste à l'extérieur du contrôle ; autrement, la surface de l'élément.
        const zones =
          e.genre === 'bordure'
            ? [
                [e.x - 2, e.y + e.h / 2, 1, 1],
                [e.x + e.l / 2, e.y - 2, 1, 1],
                [e.x + e.l / 2, e.y + e.h + 1, 1, 1],
              ]
            : [[e.x, e.y, e.l, e.h]];
        let pire = 99;
        for (const [x, y, l, h] of zones) {
          const x0 = Math.max(0, Math.floor(x * k));
          const y0 = Math.max(0, Math.floor(y * k));
          const largeur = Math.max(1, Math.min(image.width - x0, Math.ceil(l * k)));
          const hauteur = Math.max(1, Math.min(image.height - y0, Math.ceil(h * k)));
          const donnees = g.getImageData(x0, y0, largeur, hauteur).data;
          for (let i = 0; i < donnees.length; i += 8) {
            const fond = [donnees[i], donnees[i + 1], donnees[i + 2]];
            const avant = c.slice(0, 3).map((v, j) => v * alpha + fond[j] * (1 - alpha));
            pire = Math.min(pire, rapport(avant, fond));
          }
        }
        const seuil = e.genre === 'texte' ? (e.taille >= 24 || (e.taille >= 18.66 && e.gras) ? 3 : 4.5) : 3;
        if (pire < seuil) {
          defauts.push(`${e.genre} « ${e.libelle} » : ${pire.toFixed(2)}:1 (seuil ${seuil})`);
        }
      }
      return defauts;
    },
    [photo, elements] as [string, ElementBandeau[]],
  );
  return [...insuffisants, ...petites.map(p => `cible trop petite : ${p}`)];
}

/** Contrôle axe d'une racine ; renvoie le nombre de défauts de contraste et d'éléments « incomplets ». */
async function mesurer(page: Page, racine: Racine, scenario: string, style: string, detail = ''): Promise<number> {
  // Les lignes du résumé ont `transition: all 0.2s` : mesurées à mi-course, leur couleur est intermédiaire et
  // fait apparaître un défaut qui n'existe pas au repos (vu une fois sur cinq passages).
  await page.addStyleTag({ content: '*, *::before, *::after { transition: none !important; animation: none !important; }' });
  const resultat = await new AxeBuilder({ page }).include(racine).withRules(['color-contrast']).analyze();
  const noeuds = resultat.violations.flatMap(v => v.nodes);
  const bandeau = await mesurerBandeau(page);
  mkdirSync(DOSSIER, { recursive: true });
  writeFileSync(
    join(DOSSIER, `axe-situation-${scenario}${detail}-${style}.json`),
    JSON.stringify(
      {
        defauts: noeuds.map(n => ({ cible: n.target, resume: n.failureSummary })),
        bandeau,
        incomplets: resultat.incomplete.flatMap(v => v.nodes).length,
      },
      null,
      2,
    ),
  );
  return noeuds.length + bandeau.length;
}

/** Abandonne la vente en cours : bouton « Annuler » (icône ✗ sur petit écran), puis confirmation. */
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

async function remplirPanierComptant(page: Page): Promise<void> {
  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  await page.goto('/sales-home');
  await expect(page.locator('#produitbox')).toBeVisible();
  await chercherProduit(page, 'DOLIPRANE ADULTES 1000');
  await ajouterAuPanier(page, '2');
  await chercherProduit(page, 'PARACETAMOL TEVA 500 mg');
  await ajouterAuPanier(page, '1');
  // Sélectionne la première ligne : c'est l'état « ligne sélectionnée » qu'il faut contrôler.
  await page.locator('tbody tr[data-line-id]').first().click();
  await page.mouse.move(2, 2);
}

test.describe.configure({ mode: 'serial' });
test.setTimeout(180_000);

const defauts: Record<string, Record<string, number>> = {};
const noter = (scenario: string, style: string, n: number) => ((defauts[scenario] ??= {})[style] = n);

for (const style of STYLES) {
  test.describe(style, () => {
    test(`panier comptant, montants et boutons — ${style}`, async ({ page }) => {
      try {
        await remplirPanierComptant(page);
        await expect(page.locator('#main-content')).toContainText(/À ENCAISSER/i);
        noter('panier-comptant', style, await mesurer(page, 'app-sales-home', 'panier-comptant', style));
      } finally {
        await abandonnerVente(page);
      }
    });

    test(`assurance, encart client avec les données de l’assuré — ${style}`, async ({ page }) => {
      const matricule = 'CIE01-000118';
      const numeroBon = 'BON' + Date.now().toString().slice(-9);
      const modale = page.locator('.modal-content');
      try {
        await assurerCaisseOuverte(page);
        await assurerPanierVide(page);
        await page.goto('/sales-home');
        await expect(page.locator('#produitbox')).toBeVisible();
        await page.getByRole('tab', { name: /Assurance/ }).click();

        const recherche = page.getByPlaceholder('Rechercher un client assuré');
        await recherche.fill('ASSI');
        await recherche.press('Enter');
        await expect(modale).toContainText('CLIENTS ASSURÉS');
        // Fenêtre de sélection de l'assuré, ouverte : un premier contrôle.
        noter('assurance-selection-assure', style, await mesurer(page, 'app-sales-home', 'assurance-selection-assure', style));
        await modale.locator('tbody tr').filter({ hasText: matricule }).first().dblclick();
        await expect(modale).toBeHidden();

        const bon = page.getByPlaceholder('Numéro de bon');
        await bon.click();
        await bon.pressSequentially(numeroBon, { delay: 30 });
        await bon.press('Enter');
        await expect(page.locator('#produitbox')).toBeFocused();
        await chercherProduit(page, 'DOLIPRANE ADULTES 1000');
        await ajouterAuPanier(page, '2');
        await expect(page.locator('#main-content')).toContainText(/TOTAL ASSURANCE/i);
        await expect(page.locator('#main-content')).toContainText(matricule);
        noter('assurance-encart-assure', style, await mesurer(page, 'app-sales-home', 'assurance-encart-assure', style));
      } finally {
        await abandonnerVente(page);
      }
    });

    test(`vente carnet, client et données du porteur — ${style}`, async ({ page }) => {
      const matricule = 'CAR01-000045';
      const modale = page.locator('.modal-content');
      try {
        await assurerCaisseOuverte(page);
        await assurerPanierVide(page);
        await page.goto('/sales-home');
        await expect(page.locator('#produitbox')).toBeVisible();
        await page.getByRole('tab', { name: /Carnet/ }).click();

        const recherche = page.getByPlaceholder('Rechercher un client assuré');
        await recherche.fill('SANGARE');
        await recherche.press('Enter');
        await expect(modale).toContainText('CLIENTS CARNET');
        noter('carnet-selection-client', style, await mesurer(page, 'app-sales-home', 'carnet-selection-client', style));
        await modale.locator('tbody tr').filter({ hasText: matricule }).first().dblclick();
        await expect(modale).toBeHidden();
        await expect(page.locator('#main-content')).toContainText(matricule);

        await chercherProduit(page, 'DOLIPRANE 500 mg');
        await ajouterAuPanier(page, '2');
        await expect(page.locator('#main-content')).toContainText(/Total assurance/i);
        noter('carnet-encart-client', style, await mesurer(page, 'app-sales-home', 'carnet-encart-client', style));
      } finally {
        await abandonnerVente(page);
      }
    });

    test(`fiche client en panneau, avec les données de l’assuré — ${style}`, async ({ page }) => {
      const matricule = 'CIE01-000118';
      const modale = page.locator('.modal-content');
      try {
        await assurerCaisseOuverte(page);
        await assurerPanierVide(page);
        await page.goto('/sales-home');
        await expect(page.locator('#produitbox')).toBeVisible();
        await page.getByRole('tab', { name: /Assurance/ }).click();

        const recherche = page.getByPlaceholder('Rechercher un client assuré');
        await recherche.fill('ASSI');
        await recherche.press('Enter');
        await expect(modale).toContainText('CLIENTS ASSURÉS');
        await modale.locator('tbody tr').filter({ hasText: matricule }).first().dblclick();
        await expect(modale).toBeHidden();
        await expect(page.locator('#main-content')).toContainText(matricule);

        await page.getByRole('button', { name: 'Voir la fiche client' }).click();
        const panneau = page.locator('app-fiche-client-panel .offcanvas');
        await expect(panneau).toBeVisible();
        // Le panneau charge la fiche : on attend la fin du chargement, pas le premier rendu.
        await expect(panneau.locator('.spinner-border')).toBeHidden();
        await expect(panneau).toContainText(/Couverture|Aucune alerte/i);
        await page.waitForTimeout(600);
        noter('fiche-client', style, await mesurer(page, 'app-sales-home', 'fiche-client', style));
        await panneau.locator('.btn-close').click();
        await expect(panneau).toBeHidden();
      } finally {
        await abandonnerVente(page);
      }
    });

    test(`prévente avec lignes, palette des documents — ${style}`, async ({ page }) => {
      // 7 × 19 310 = 135 170 : un montant que les autres ventes de démonstration n'ont pas, pour retrouver cette
      // prévente-là dans la liste et ne supprimer qu'elle.
      // L'écran sépare les milliers par une espace insécable : `\s` la couvre, une espace simple non.
      const montant = /135\s?170/;
      const lignes = page.locator('tbody tr').filter({ visible: true });
      try {
        await assurerCaisseOuverte(page);
        await page.goto('/sales-home/prevente');
        await expect(page.locator('#produitbox')).toBeVisible();
        await chercherProduit(page, 'DOLIPRANE 500 mg');
        await ajouterAuPanier(page, '7');
        await expect(page.locator('#main-content')).toContainText(montant);
        await page.locator('tbody tr[data-line-id]').first().click();
        await page.mouse.move(2, 2);
        noter('prevente-lignes', style, await mesurer(page, 'app-sales-home', 'prevente-lignes', style));
      } finally {
        // La prévente n'a pas de bouton « Annuler » : on l'enregistre, puis on la supprime de la liste.
        const enregistrer = page.getByRole('button', { name: 'Enregistrer' });
        if (await enregistrer.isVisible().catch(() => false)) {
          await enregistrer.click();
          await expect(page.locator('#main-content')).toContainText(/Panier vide|Ajoutez des produits/i);
        }
        await page.goto('/sales-home/gestion');
        await page.getByRole('tab', { name: /Pré-ventes/ }).click();
        // Toutes celles de ce montant : une exécution interrompue n'en laisse pas derrière elle.
        const mienne = lignes.filter({ hasText: montant });
        await expect(mienne.first()).toBeVisible();
        for (let restantes = await mienne.count(); restantes > 0; restantes--) {
          await mienne.first().getByRole('button', { name: 'Supprimer la pré-vente' }).click();
          const confirmation = page.locator('.modal-content');
          await expect(confirmation).toBeVisible();
          await confirmation.getByRole('button', { name: 'Oui' }).click();
          await expect(confirmation).toBeHidden();
          // La liste se recharge : cliquer avant la fin viserait une ligne qui va disparaître.
          await expect(mienne).toHaveCount(restantes - 1);
        }
      }
    });

    test(`écran dépôt avec un dépôt et une ligne — ${style}`, async ({ page }) => {
      try {
        await page.goto('/sales-home/vente-depot');
        await expect(page.locator('app-vente-depot')).toBeVisible();
        await page.locator('.depot-select .ng-select-container').click();
        await page.locator('.ng-option').first().click();
        await expect(page.locator('#produitbox')).toBeEnabled();
        await chercherProduit(page, 'DOLIPRANE ADULTES 1000');
        await ajouterAuPanier(page, '1');
        await expect(page.locator('tbody tr[data-line-id]').first()).toBeVisible();
        noter('depot', style, await mesurer(page, 'app-vente-depot', 'depot', style));
      } finally {
        await abandonnerVente(page);
      }
    });

    test(`panneau des ventes en attente — ${style}`, async ({ page }) => {
      await assurerCaisseOuverte(page);
      await assurerPanierVide(page);
      await page.goto('/sales-home');
      await expect(page.locator('#produitbox')).toBeVisible();
      await page.getByRole('button', { name: /ventes en attente/i }).click();
      await expect(page.locator('.pending-sales-container')).toBeVisible();
      await page.waitForTimeout(800);
      noter('ventes-en-attente', style, await mesurer(page, 'app-sales-home', 'ventes-en-attente', style));
    });

    test(`chaque pastille de mode de règlement — ${style}`, async ({ page }) => {
      try {
        await remplirPanierComptant(page);
        let total = 0;
        const bascule = page.getByRole('button', { name: 'Changer de mode de règlement' });
        await bascule.click();
        const nombre = await page.locator('.payment-mode-menu button').count();
        await page.keyboard.press('Escape');
        for (let i = 0; i < nombre; i++) {
          await bascule.click();
          await page.locator('.payment-mode-menu button').nth(i).click();
          await page.waitForTimeout(500);
          total += await mesurer(page, 'app-sales-home', 'pastilles', style, `-${i}`);
        }
        expect(nombre, 'au moins un autre mode de règlement à contrôler').toBeGreaterThan(0);
        noter('pastilles', style, total);
      } finally {
        await abandonnerVente(page);
      }
    });
  });
}

test('aucun défaut de contraste en situation', () => {
  mkdirSync(DOSSIER, { recursive: true });
  writeFileSync(join(DOSSIER, 'axe-en-situation.json'), JSON.stringify(defauts, null, 2));

  for (const [scenario, parStyle] of Object.entries(defauts)) {
    expect(parStyle['billetage'] ?? 0, `${scenario} : défauts de contraste`).toBe(0);
  }
});
