/**
 * Reporter qui rassemble les captures d'une campagne dans `captures/captures.json`.
 *
 * Les images sont écrites par `etape()` au fil de l'eau ; ce qui manque, c'est l'index qui les
 * rattache à un scénario et porte leur légende. Il transite par des pièces jointes Playwright
 * plutôt que par une variable partagée : les parcours s'exécutent dans plusieurs processus, un
 * état global en mémoire ne les traverserait pas.
 *
 * Fusion par défaut : une exécution ciblée sur un seul scénario met à jour ses entrées sans
 * effacer celles des autres. `E2E_CAPTURES_RESET=1` repart d'un index vide.
 */
import type { FullResult, Reporter, TestCase, TestResult } from '@playwright/test/reporter';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';
import { DOSSIER_CAPTURES, FICHIER_INDEX, REINITIALISER_INDEX } from './config';
import { PREFIXE_PIECE_JOINTE, type CaptureIndexee } from './scenario';

/** Index du manuel : les captures d'un scénario, dans l'ordre de ses étapes. */
type IndexCaptures = Record<string, CaptureIndexee[]>;

export default class CapturesReporter implements Reporter {
  private readonly collectees = new Map<string, Map<number, CaptureIndexee>>();

  onTestEnd(test: TestCase, result: TestResult): void {
    // Un parcours en échec ne doit rien produire : c'est toute la garantie du dispositif —
    // une capture n'existe que si le parcours qui l'a prise est passé au vert.
    if (result.status !== 'passed') {
      return;
    }

    for (const piece of result.attachments) {
      if (!piece.name.startsWith(PREFIXE_PIECE_JOINTE) || !piece.body) {
        continue;
      }
      const capture = JSON.parse(piece.body.toString('utf8')) as CaptureIndexee;
      const parScenario = this.collectees.get(capture.scenarioId) ?? new Map<number, CaptureIndexee>();
      parScenario.set(capture.ordre, capture);
      this.collectees.set(capture.scenarioId, parScenario);
    }
  }

  onEnd(_result: FullResult): void {
    if (this.collectees.size === 0) {
      return;
    }

    const index: IndexCaptures = REINITIALISER_INDEX ? {} : this.lireIndexExistant();

    for (const [scenarioId, parOrdre] of this.collectees) {
      index[scenarioId] = [...parOrdre.values()].sort((a, b) => a.ordre - b.ordre);
    }

    // Trié par identifiant : sans cela, l'ordre dépendrait de l'ordonnancement des workers et
    // le fichier changerait à chaque exécution sans qu'aucune capture n'ait bougé.
    const trie = Object.fromEntries(Object.entries(index).sort(([a], [b]) => a.localeCompare(b)));

    mkdirSync(dirname(FICHIER_INDEX), { recursive: true });
    writeFileSync(FICHIER_INDEX, JSON.stringify(trie, null, 2) + '\n', 'utf8');

    const nbImages = Object.values(trie).reduce((total, liste) => total + liste.length, 0);
    process.stdout.write(
      `\nCaptures : ${Object.keys(trie).length} scénario(s), ${nbImages} image(s) — ${DOSSIER_CAPTURES}\n`,
    );
  }

  printsToStdio(): boolean {
    return true;
  }

  private lireIndexExistant(): IndexCaptures {
    if (!existsSync(FICHIER_INDEX)) {
      return {};
    }
    try {
      return JSON.parse(readFileSync(FICHIER_INDEX, 'utf8')) as IndexCaptures;
    } catch {
      // Index illisible (exécution interrompue en pleine écriture) : on repart à zéro plutôt
      // que d'échouer, la campagne en cours va de toute façon le réécrire.
      return {};
    }
  }
}
