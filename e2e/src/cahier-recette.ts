/**
 * Accès au cahier de recette depuis les parcours Playwright.
 *
 * Le modèle TypeScript `CAHIER_RECETTE` est la source de vérité unique : il alimente déjà le
 * composant Angular et, via generate-cahier-recette-json.ts, le PDF du guide. Les parcours de
 * capture s'y rattachent plutôt que de redécrire les étapes — c'est ce qui interdit à la
 * documentation et aux captures de diverger.
 */
import {
  CAHIER_RECETTE,
  type FonctionnaliteRecette,
  type ModuleRecette,
  type ScenarioRecette,
} from '../../pharmaSmart-app/src/main/webapp/app/features/cahier-recette/cahier-recette.model';

export interface ScenarioLocalise {
  scenario: ScenarioRecette;
  module: ModuleRecette;
  fonctionnalite: FonctionnaliteRecette;
}

/**
 * Index construit à l'import. Une collision d'identifiant fait échouer immédiatement toute la
 * campagne : deux scénarios portant `VTE-01` rendraient les captures inattribuables, et le
 * défaut passerait autrement inaperçu jusqu'au PDF.
 */
const INDEX = new Map<string, ScenarioLocalise>();

for (const module of CAHIER_RECETTE) {
  for (const fonctionnalite of module.fonctionnalites) {
    for (const scenario of fonctionnalite.scenarios) {
      const existant = INDEX.get(scenario.id);
      if (existant) {
        throw new Error(
          `cahier-recette.model.ts : identifiant « ${scenario.id} » utilisé deux fois ` +
            `(${existant.module.nom} / ${existant.fonctionnalite.nom} et ` +
            `${module.nom} / ${fonctionnalite.nom}).`,
        );
      }
      INDEX.set(scenario.id, { scenario, module, fonctionnalite });
    }
  }
}

/**
 * Résout un identifiant de scénario, ou échoue avec un message exploitable.
 *
 * Appelée au chargement du fichier de parcours (pas à l'exécution du test) : une faute de frappe
 * dans l'identifiant arrête la campagne avant d'avoir lancé le moindre navigateur.
 */
export function resoudreScenario(id: string): ScenarioLocalise {
  const trouve = INDEX.get(id);
  if (!trouve) {
    throw new Error(
      `Scénario « ${id} » introuvable dans cahier-recette.model.ts. ` +
        `Vérifier l'identifiant, ou ajouter le scénario au modèle avant d'écrire son parcours.`,
    );
  }
  return trouve;
}

/** Tous les scénarios du modèle, dans l'ordre de lecture du guide. */
export function tousLesScenarios(): ScenarioLocalise[] {
  return [...INDEX.values()];
}

/** Identifiants des scénarios visibles dans le guide (hors `hidden`). */
export function scenariosVisibles(): ScenarioLocalise[] {
  return tousLesScenarios().filter(s => !s.scenario.hidden && !s.fonctionnalite.hidden);
}
