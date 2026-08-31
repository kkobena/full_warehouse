import { defineConfig } from '@playwright/test';
import { BASE_URL, ECHELLE, FENETRE, FICHIER_SESSION, PROJET_CAPTURES, RACINE } from './src/config';
import { join } from 'node:path';

/**
 * Configuration de la campagne de captures (phase 1) et, plus tard, de la suite de
 * non-régression (phase 2). Voir docs/PLAN-PLAYWRIGHT-E2E-ET-CAPTURES.md.
 *
 * Trois projets, volontairement séparés :
 *   - « liage »           : contrôles sur le modèle du cahier de recette. Aucun navigateur,
 *                           aucune base, aucun serveur — exécutable à tout moment.
 *   - « authentification »: se connecte une fois et enregistre la session.
 *   - « parcours »        : les parcours, rejouant cette session, sans prise d'images.
 *   - « captures »        : les mêmes fichiers, avec prise d'images.
 *
 * « parcours » et « captures » partagent leurs fichiers : c'est le même code qui vérifie et qui
 * illustre. Les séparer par projet plutôt que par variable d'environnement rend `npm run
 * captures` portable — sous Windows, npm passe par cmd.exe, où `CAPTURE=1 …` n'a aucun effet.
 */
export default defineConfig({
  testDir: join(RACINE, 'e2e'),
  outputDir: join(RACINE, 'target', 'playwright-artifacts'),

  // Un parcours de capture peut enchaîner une dizaine d'écrans : le défaut de 30 s est court.
  timeout: 90_000,
  expect: { timeout: 10_000 },

  // Une seule base de démonstration, et des parcours qui la modifient (ventes, commandes,
  // inventaires). En parallèle, deux parcours se marcheraient dessus et produiraient des
  // captures incohérentes — le genre de défaut qu'on impute à tort à Playwright.
  fullyParallel: false,
  workers: Number(process.env.E2E_WORKERS ?? 1),

  // Pas de reprise : une capture prise à la seconde tentative, sur un écran déjà modifié par
  // la première, ne montrerait pas ce que sa légende annonce.
  retries: 0,
  forbidOnly: !!process.env.CI,

  reporter: [
    ['list'],
    ['./src/captures-reporter.ts'],
    ['html', { outputFolder: join(RACINE, 'target', 'playwright-report'), open: 'never' }],
  ],

  use: {
    baseURL: BASE_URL,

    // Taille de l'écran que l'application croit avoir. Réglable par E2E_VIEWPORT_WIDTH /
    // E2E_VIEWPORT_HEIGHT ; défaut 1920 x 1080, la résolution d'un poste d'officine.
    viewport: FENETRE,

    // Densité de pixels, réglable par E2E_SCALE. À 2, mêmes écrans en plus net, images
    // ~3 fois plus lourdes. Voir src/config.ts pour la distinction avec la taille de fenêtre.
    deviceScaleFactor: ECHELLE,

    locale: process.env.E2E_LOCALE ?? 'fr-FR',
    // Fixé, quelle que soit la valeur : sans fuseau imposé, les colonnes de date changent
    // d'une machine à l'autre et le manuel se contredit d'une édition à l'autre.
    timezoneId: process.env.E2E_TIMEZONE ?? 'Europe/Paris',

    // Neutralise les transitions ng-bootstrap et Angular : une modale saisie à mi-course
    // donne une image inutilisable. `reducedMotion` est une option de contexte navigateur,
    // pas une option de test : elle passe par `contextOptions`.
    contextOptions: { reducedMotion: 'reduce' },

    trace: 'retain-on-failure',
    screenshot: 'off',
    video: 'off',
  },

  projects: [
    {
      name: 'liage',
      testDir: join(RACINE, 'e2e', 'verifications'),
    },
    {
      name: 'authentification',
      testDir: join(RACINE, 'e2e', 'setup'),
      testMatch: /auth\.setup\.ts/,
    },
    {
      name: 'parcours',
      testDir: join(RACINE, 'e2e', 'parcours'),
      dependencies: ['authentification'],
      use: { storageState: FICHIER_SESSION },
    },
    {
      name: PROJET_CAPTURES,
      testDir: join(RACINE, 'e2e', 'parcours'),
      dependencies: ['authentification'],
      use: { storageState: FICHIER_SESSION },
    },
  ],
});
