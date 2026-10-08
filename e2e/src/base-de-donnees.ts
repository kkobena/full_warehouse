import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';

const CONTENEUR = process.env.E2E_DB_CONTAINER ?? 'pharma_smart_postgres';
const BASE = process.env.E2E_DB_NAME ?? 'pharma_smart';
const SCHEMA = process.env.PHARMA_DB_SCHEMA ?? 'pharma_smart';

function psql(args: string[], entree?: string): string {
  return execFileSync('docker', ['exec', '-i', CONTENEUR, 'psql', '-U', 'postgres', '-d', BASE, '-v', 'ON_ERROR_STOP=1', ...args], {
    input: entree,
    encoding: 'utf8',
    env: { ...process.env, MSYS_NO_PATHCONV: '1' },
  }).trim();
}

/** Valeur unique d'une requête de lecture, pour les vérifications de fin de parcours. */
export function lire(requete: string): string {
  return psql(['-At', '-c', `SET search_path = ${SCHEMA}, public; ${requete}`]).split('\n').pop() ?? '';
}

/** Rejoue un script du dépôt (chemin depuis la racine) dans la base de l'application. */
export function rejouer(script: string): void {
  psql([], readFileSync(join(__dirname, '..', '..', script), 'utf8'));
}

/** Joue une instruction d'écriture (jeu d'essai de fin de préparation). */
export function executer(instruction: string): void {
  psql([], `SET search_path = ${SCHEMA}, public; ${instruction}`);
}
