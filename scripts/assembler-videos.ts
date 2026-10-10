/**
 * Assemble les vidéos des parcours (e2e/captures/<ID>/parcours.webm, produites par `npm run videos`)
 * en une vidéo PAR FONCTIONNALITÉ, puis une vidéo PAR MODULE — pour les distribuer en dehors du jar.
 *
 * Sortie, par défaut `target/videos-guide/` (VIDEOS_OUT pour en changer) :
 *
 *     index.html                       page autonome : un lecteur par module et par fonctionnalité, chapitres cliquables
 *     guide-videos.json                la même structure, pour un autre support
 *     modules/<module>.mp4 + .vtt
 *     fonctionnalites/<module>--<fonctionnalité>.mp4 + .vtt
 *
 * Les fichiers `.vtt` sont des pistes de chapitres WebVTT : chaque parcours d'une fonctionnalité, chaque
 * fonctionnalité d'un module. Les chapitres portent les titres du guide, jamais les références techniques
 * (VTE-01…) — l'utilisateur ne les connaît pas.
 *
 * Étapes :
 *   1. chaque parcours est normalisé en MP4/H.264 (même définition, même cadence, ralenti : VIDEOS_RALENTI) dans `<sortie>-cache-x<facteur>/` — les
 *      WebM de Playwright n'ont pas de durée lisible, et l'assemblage sans cela saute ;
 *   2. les parcours d'une fonctionnalité sont mis bout à bout sans ré-encodage ;
 *   3. les fonctionnalités d'un module, de même.
 * Un fichier déjà à jour (plus récent que ses sources) n'est pas refait : relancer après une campagne
 * partielle ne coûte que ce qui a changé. `--force` refait tout.
 *
 * ffmpeg : celui du paquet `ffmpeg-static`, ou à défaut `FFMPEG` / le PATH.
 *
 * Lancé via `npm run videos:assembler`. Ne fait pas partie du build Maven : ces vidéos ne sont pas dans le jar.
 */
import { spawnSync } from 'child_process';
import { existsSync, mkdirSync, readFileSync, rmSync, statSync, writeFileSync } from 'fs';
import { createRequire } from 'module';
import { basename, dirname, join, resolve } from 'path';
import { fileURLToPath } from 'url';
import {
  CAHIER_RECETTE,
  type FonctionnaliteRecette,
  type ModuleRecette,
} from '../pharmaSmart-app/src/main/webapp/app/features/cahier-recette/cahier-recette.model';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, '..');
const CAPTURES = process.env.E2E_CAPTURES_DIR ? resolve(process.env.E2E_CAPTURES_DIR) : resolve(ROOT, 'e2e/captures');
const INDEX_VIDEOS = join(CAPTURES, 'videos.json');
const SORTIE = process.env.VIDEOS_OUT ? resolve(process.env.VIDEOS_OUT) : resolve(ROOT, 'target/videos-guide');
const FORCER = process.argv.includes('--force');

/** Définition et cadence communes à tous les parcours normalisés. */
const LARGEUR = Number(process.env.E2E_VIDEO_WIDTH ?? 1280);
const HAUTEUR = Number(process.env.E2E_VIDEO_HEIGHT ?? 720);
const CADENCE = 25;

/**
 * Facteur de ralentissement : 2 = deux fois plus lent. Un test automatique va bien plus vite qu'un
 * utilisateur ; ralentir l'enregistrement allonge d'autant les pauses (le temps de lire la légende)
 * et les gestes. 1 laisse la vidéo telle que filmée (VIDEOS_RALENTI=1).
 */
const RALENTI = Number(process.env.VIDEOS_RALENTI ?? 2);
if (!(RALENTI >= 1 && RALENTI <= 8)) {
  throw new Error(`VIDEOS_RALENTI="${process.env.VIDEOS_RALENTI}" invalide : un nombre entre 1 et 8.`);
}

interface Chapitre {
  titre: string;
  /** Secondes depuis le début de la vidéo. */
  debut: number;
}

interface VideoAssemblee {
  titre: string;
  fichier: string;
  chapitres: Chapitre[];
  duree: number;
}

interface ModuleAssemble {
  nom: string;
  description: string;
  video: VideoAssemblee;
  fonctionnalites: Array<VideoAssemblee & { description?: string }>;
}

function trouverFfmpeg(): string {
  if (process.env.FFMPEG) {
    return process.env.FFMPEG;
  }
  try {
    const chemin = createRequire(import.meta.url)('ffmpeg-static') as string | null;
    if (chemin && existsSync(chemin)) {
      return chemin;
    }
  } catch {
    // paquet absent : on essaie le PATH
  }
  return 'ffmpeg';
}

const FFMPEG = trouverFfmpeg();

function ffmpeg(args: string[]): { sortie: string; ok: boolean } {
  const r = spawnSync(FFMPEG, ['-hide_banner', '-loglevel', 'error', '-y', ...args], { encoding: 'utf8' });
  if (r.error) {
    throw new Error(
      `ffmpeg introuvable (${FFMPEG}). Installer le paquet "ffmpeg-static" (npm install), ou renseigner FFMPEG avec le chemin de l'exécutable.`,
    );
  }
  return { sortie: r.stderr, ok: r.status === 0 };
}

/** Durée en secondes, lue sur la bannière de `ffmpeg -i` (pas de ffprobe dans le paquet). */
function dureeDe(fichier: string): number {
  const r = spawnSync(FFMPEG, ['-hide_banner', '-i', fichier], { encoding: 'utf8' });
  const m = /Duration:\s*(\d+):(\d+):(\d+(?:\.\d+)?)/.exec(r.stderr ?? '');
  if (!m) {
    throw new Error(`Durée illisible : ${fichier}`);
  }
  return Number(m[1]) * 3600 + Number(m[2]) * 60 + Number(m[3]);
}

function aJour(cible: string, sources: string[]): boolean {
  if (FORCER || !existsSync(cible)) {
    return false;
  }
  const t = statSync(cible).mtimeMs;
  return sources.every(s => statSync(s).mtimeMs <= t);
}

function slug(texte: string): string {
  return texte
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 60);
}

/** Un parcours en MP4 normalisé (définition, cadence, pixels), avec une vraie durée. */
function normaliser(source: string, cible: string): void {
  if (aJour(cible, [source])) {
    return;
  }
  mkdirSync(dirname(cible), { recursive: true });
  const filtre =
    `scale=${LARGEUR}:${HAUTEUR}:force_original_aspect_ratio=decrease,` +
    `pad=${LARGEUR}:${HAUTEUR}:(ow-iw)/2:(oh-ih)/2,setsar=1,setpts=${RALENTI}*PTS,fps=${CADENCE}`;
  const r = ffmpeg([
    '-i', source, '-vf', filtre, '-an',
    '-c:v', 'libx264', '-preset', 'veryfast', '-crf', '27', '-pix_fmt', 'yuv420p',
    '-movflags', '+faststart', cible,
  ]);
  if (!r.ok) {
    throw new Error(`Normalisation impossible : ${source}\n${r.sortie}`);
  }
}

/** Met des MP4 de même encodage bout à bout, sans les ré-encoder. */
function concatener(entrees: string[], cible: string): void {
  if (aJour(cible, entrees)) {
    return;
  }
  mkdirSync(dirname(cible), { recursive: true });
  const liste = `${cible}.liste.txt`;
  writeFileSync(liste, entrees.map(f => `file '${f.replace(/\\/g, '/').replace(/'/g, "'\\''")}'`).join('\n') + '\n', 'utf8');
  const r = ffmpeg(['-f', 'concat', '-safe', '0', '-i', liste, '-c', 'copy', '-movflags', '+faststart', cible]);
  rmSync(liste, { force: true });
  if (!r.ok) {
    throw new Error(`Assemblage impossible : ${cible}\n${r.sortie}`);
  }
}

function tempsVtt(secondes: number): string {
  const ms = Math.round(secondes * 1000);
  const h = String(Math.floor(ms / 3_600_000)).padStart(2, '0');
  const m = String(Math.floor((ms % 3_600_000) / 60_000)).padStart(2, '0');
  const s = String(Math.floor((ms % 60_000) / 1000)).padStart(2, '0');
  return `${h}:${m}:${s}.${String(ms % 1000).padStart(3, '0')}`;
}

function ecrireChapitres(cible: string, chapitres: Chapitre[], duree: number): void {
  const blocs = chapitres.map((c, i) => {
    const fin = i + 1 < chapitres.length ? chapitres[i + 1].debut : duree;
    return `${i + 1}\n${tempsVtt(c.debut)} --> ${tempsVtt(fin)}\n${c.titre}\n`;
  });
  writeFileSync(cible.replace(/\.mp4$/, '.vtt'), `WEBVTT\n\n${blocs.join('\n')}`, 'utf8');
}

function echapperHtml(texte: string): string {
  return texte.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function ecrirePage(modules: ModuleAssemble[]): void {
  const minutes = (s: number): string => `${Math.max(1, Math.round(s / 60))} min`;
  const lecteur = (v: VideoAssemblee, id: string): string => `
      <figure class="video">
        <video id="${id}" controls preload="none" src="${echapperHtml(v.fichier)}">
          <track kind="chapters" srclang="fr" label="Chapitres" src="${echapperHtml(v.fichier.replace(/\.mp4$/, '.vtt'))}" default>
        </video>
        <ol class="chapitres">${v.chapitres
          .map(c => `<li><button type="button" data-video="${id}" data-debut="${c.debut}">${echapperHtml(c.titre)}</button></li>`)
          .join('')}</ol>
      </figure>`;

  const corps = modules
    .map((m, i) => {
      const fonctionnalites = m.fonctionnalites
        .map(
          (f, j) => `
        <details class="fonctionnalite">
          <summary>${echapperHtml(f.titre)} <small>${minutes(f.duree)}</small></summary>
          ${f.description ? `<p>${echapperHtml(f.description)}</p>` : ''}${lecteur(f, `f${i}-${j}`)}
        </details>`,
        )
        .join('');
      return `
    <section class="module">
      <h2>${echapperHtml(m.nom)} <small>${minutes(m.video.duree)}</small></h2>
      <p>${echapperHtml(m.description)}</p>
      <details open><summary>Le module en entier</summary>${lecteur(m.video, `m${i}`)}</details>
      ${fonctionnalites}
    </section>`;
    })
    .join('');

  const html = `<!doctype html>
<html lang="fr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Vidéos du guide PharmaSmart</title>
<style>
  :root { --fond:#f8fafc; --texte:#0f172a; --discret:#475569; --trait:#cbd5e1; --accent:#1d4ed8; }
  @media (prefers-color-scheme: dark) { :root { --fond:#0f172a; --texte:#f1f5f9; --discret:#94a3b8; --trait:#334155; --accent:#93c5fd; } }
  body { margin:0; padding:24px 16px 64px; background:var(--fond); color:var(--texte); font:16px/1.5 system-ui,Segoe UI,sans-serif; }
  main { max-width:980px; margin:0 auto; }
  h1 { margin:0 0 4px; } h2 { margin:32px 0 4px; } small { color:var(--discret); font-weight:400; }
  .intro { color:var(--discret); margin:0 0 8px; }
  .module { border-top:1px solid var(--trait); }
  details { margin:8px 0; } summary { cursor:pointer; font-weight:600; padding:4px 0; }
  .video { margin:8px 0 12px; } video { width:100%; max-width:900px; background:#000; border-radius:6px; }
  .chapitres { list-style:none; margin:8px 0 0; padding:0; display:flex; flex-wrap:wrap; gap:6px; }
  .chapitres button { font:inherit; font-size:14px; cursor:pointer; padding:4px 10px; border-radius:999px;
    border:1px solid var(--trait); background:transparent; color:var(--accent); }
  .chapitres button:hover, .chapitres button:focus-visible { border-color:var(--accent); }
</style>
</head>
<body>
<main>
  <h1>Vidéos du guide PharmaSmart</h1>
  <p class="intro">Une vidéo par module, et une par fonctionnalité. Les boutons sous chaque vidéo mènent directement à une étape.</p>${corps}
</main>
<script>
  document.addEventListener('click', function (e) {
    var b = e.target.closest('button[data-video]');
    if (!b) return;
    var v = document.getElementById(b.dataset.video);
    v.currentTime = Number(b.dataset.debut);
    v.play();
  });
</script>
</body>
</html>
`;
  writeFileSync(join(SORTIE, 'index.html'), html, 'utf8');
}

function main(): void {
  if (!existsSync(INDEX_VIDEOS)) {
    throw new Error(`Aucune vidéo à assembler : ${INDEX_VIDEOS} est absent. Lancer d'abord "npm run videos".`);
  }
  const index = JSON.parse(readFileSync(INDEX_VIDEOS, 'utf8')) as Record<string, string>;
  const parcours = (id: string): string | null => {
    const fichier = index[id] ? join(CAPTURES, index[id]) : null;
    return fichier && existsSync(fichier) ? fichier : null;
  };

  const resultat: ModuleAssemble[] = [];
  // Hors du dossier de sortie : les parcours normalisés ne font pas partie de ce qu'on distribue.
  const dossierNormalise = join(dirname(SORTIE), `${basename(SORTIE)}-cache-x${RALENTI}`);
  let nbParcours = 0;

  for (const module of CAHIER_RECETTE as ModuleRecette[]) {
    const fonctionnalites: ModuleAssemble['fonctionnalites'] = [];

    for (const f of module.fonctionnalites as FonctionnaliteRecette[]) {
      const scenarios = f.scenarios.filter(s => !s.hidden && parcours(s.id));
      if (scenarios.length === 0) {
        continue;
      }
      const normalises: string[] = [];
      const chapitres: Chapitre[] = [];
      let cumul = 0;
      for (const s of scenarios) {
        const cible = join(dossierNormalise, `${s.id}.mp4`);
        normaliser(parcours(s.id)!, cible);
        chapitres.push({ titre: s.titre, debut: cumul });
        cumul += dureeDe(cible);
        normalises.push(cible);
        nbParcours++;
      }
      const fichier = `fonctionnalites/${slug(module.id)}--${slug(f.nom)}.mp4`;
      concatener(normalises, join(SORTIE, fichier));
      const duree = dureeDe(join(SORTIE, fichier));
      ecrireChapitres(join(SORTIE, fichier), chapitres, duree);
      fonctionnalites.push({ titre: f.nom, description: f.description, fichier, chapitres, duree });
      process.stdout.write(`  ${module.nom} › ${f.nom} : ${scenarios.length} parcours, ${Math.round(duree)} s\n`);
    }

    if (fonctionnalites.length === 0) {
      continue;
    }
    const fichier = `modules/${slug(module.id)}.mp4`;
    concatener(fonctionnalites.map(f => join(SORTIE, f.fichier)), join(SORTIE, fichier));
    const duree = dureeDe(join(SORTIE, fichier));
    let cumul = 0;
    const chapitres = fonctionnalites.map<Chapitre>(f => {
      const c = { titre: f.titre, debut: cumul };
      cumul += f.duree;
      return c;
    });
    ecrireChapitres(join(SORTIE, fichier), chapitres, duree);
    resultat.push({
      nom: module.nom,
      description: module.description,
      video: { titre: module.nom, fichier, chapitres, duree },
      fonctionnalites,
    });
  }

  if (resultat.length === 0) {
    throw new Error('Aucun parcours du guide ne correspond aux vidéos de videos.json.');
  }
  writeFileSync(join(SORTIE, 'guide-videos.json'), JSON.stringify(resultat, null, 2) + '\n', 'utf8');
  ecrirePage(resultat);
  const nbFonct = resultat.reduce((total, m) => total + m.fonctionnalites.length, 0);
  process.stdout.write(
    `\nVidéos assemblées : ${resultat.length} module(s), ${nbFonct} fonctionnalité(s), ${nbParcours} parcours -> ${SORTIE}\n`,
  );
}

main();
