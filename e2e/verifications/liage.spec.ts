/**
 * Contrôles sur le liage lui-même. Aucun navigateur, aucune base, aucun serveur : ce projet
 * est exécutable à tout moment, y compris avant que la base de démonstration existe.
 *
 *     npx playwright test -c e2e --project=liage
 *
 * Il vérifie ce dont dépend toute la chaîne du manuel : que le modèle est exploitable, que
 * l'index des identifiants est sain, et que se tromper d'identifiant échoue franchement.
 */
import { expect, test } from '@playwright/test';
import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { RACINE } from '../src/config';
import { resoudreScenario, scenariosVisibles, tousLesScenarios } from '../src/cahier-recette';

test.describe('Modèle du cahier de recette', () => {
  test('le modèle se charge et son index est sans collision', () => {
    // L'index est construit à l'import de cahier-recette.ts : si deux scénarios partageaient
    // un identifiant, l'import aurait déjà levé et ce fichier ne serait pas exécuté.
    const scenarios = tousLesScenarios();
    expect(scenarios.length).toBeGreaterThan(400);
    expect(new Set(scenarios.map(s => s.scenario.id)).size).toBe(scenarios.length);
  });

  test('un identifiant connu se résout vers son module et sa fonctionnalité', () => {
    const premier = scenariosVisibles()[0];
    const resolu = resoudreScenario(premier.scenario.id);

    expect(resolu.scenario.titre).toBe(premier.scenario.titre);
    expect(resolu.module.nom).toBeTruthy();
    expect(resolu.fonctionnalite.nom).toBeTruthy();
  });

  test('un identifiant inconnu échoue avec un message exploitable', () => {
    // Le comportement qui protège la campagne : une faute de frappe dans un parcours doit
    // arrêter l'exécution au chargement, pas produire une capture orpheline.
    expect(() => resoudreScenario('XXX-99')).toThrow(/XXX-99.*introuvable/s);
  });

  test('tout scénario visible est exploitable comme parcours', () => {
    // Un scénario sans étape ne peut ni être parcouru ni être illustré : il apparaîtrait dans
    // le guide comme un titre suivi de rien. Autant le savoir avant d'écrire les parcours.
    const defectueux = scenariosVisibles()
      .filter(s => s.scenario.etapes.length === 0 || !s.scenario.resultatAttendu?.trim())
      .map(s => `${s.scenario.id} (${s.module.nom})`);

    expect(defectueux, `Scénarios sans étape ou sans résultat attendu : ${defectueux.join(', ')}`).toEqual([]);
  });
});

test.describe('Couverture du manuel', () => {
  test('état de la couverture par les parcours', () => {
    const couverts = identifiantsCouverts();
    const visibles = scenariosVisibles();
    const total = visibles.length;
    const pourcentage = total === 0 ? 0 : Math.round((couverts.size / total) * 100);

    // Informatif, non bloquant : viser 437 parcours n'a pas de sens (§7 du plan). Ce compte
    // sert à suivre l'avancement du lot 5, pas à faire échouer la campagne.
    test.info().annotations.push({
      type: 'couverture',
      description: `${couverts.size}/${total} scénario(s) visibles couverts (${pourcentage} %)`,
    });

    // Seul vrai défaut possible ici : un parcours rattaché à un identifiant que le modèle ne
    // connaît pas. `resoudreScenario` l'attraperait à l'exécution ; on le signale plus tôt.
    const orphelins = [...couverts].filter(id => !visibles.some(s => s.scenario.id === id));
    expect(orphelins, `Parcours rattachés à des scénarios inconnus ou masqués : ${orphelins.join(', ')}`).toEqual([]);
  });
});

/** Identifiants rattachés par un `scenario('…')` dans les fichiers de parcours. */
function identifiantsCouverts(): Set<string> {
  const racine = join(RACINE, 'e2e', 'parcours');
  const identifiants = new Set<string>();

  for (const fichier of fichiersTypeScript(racine)) {
    const contenu = readFileSync(fichier, 'utf8');
    for (const trouve of contenu.matchAll(/\bscenario\(\s*['"]([A-Z]{2,4}-\d+)['"]/g)) {
      identifiants.add(trouve[1]);
    }
  }
  return identifiants;
}

function fichiersTypeScript(dossier: string): string[] {
  let entrees: string[];
  try {
    entrees = readdirSync(dossier);
  } catch {
    // Dossier absent : aucun parcours écrit à ce stade, ce n'est pas une anomalie.
    return [];
  }

  return entrees.flatMap(entree => {
    const chemin = join(dossier, entree);
    if (statSync(chemin).isDirectory()) {
      return fichiersTypeScript(chemin);
    }
    return chemin.endsWith('.ts') ? [chemin] : [];
  });
}
