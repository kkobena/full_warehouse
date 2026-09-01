# Playwright : manuel utilisateur illustré, puis tests de bout en bout

Analyse de faisabilité et plan d'intégration. Rédigé le 27/08/2026.

**Verdict : faisable, et le projet est mieux placé que la moyenne pour ça** — mais pas pour la
raison qu'on attend. L'atout n'est pas technique, c'est que le cahier de recette contient déjà
la spécification que les tests auraient sinon fallu écrire. Le coût réel n'est pas Playwright,
ce sont les sélecteurs et le jeu de données.

> **Séquencement retenu : le manuel d'abord, les tests de bout en bout ensuite.**
>
> | | Phase 1 — Manuel | Phase 2 — Non-régression |
> | --- | --- | --- |
> | Objectif | un PDF illustré | une suite qui protège des régressions |
> | Ce qu'on écrit | des **parcours de capture** | on **enrichit les mêmes fichiers** d'assertions |
> | Assertions | le strict minimum (§3.1) | complètes |
> | Intégration continue | non | oui |
>
> Le point qui rend ce séquencement rentable : **rien n'est jeté entre les deux phases.** Un
> parcours de capture est déjà une spec Playwright ; la phase 2 lui ajoute des `expect`, elle ne
> le réécrit pas. C'est ce que garantit le liage par identifiant de scénario (§3).
>
> La base `pharma_smart_demo` est créée en amont — voir §5.2.

---

## 0. État d'avancement

| Lot | État | Vérification |
| --- | --- | --- |
| 0 — socle données | **fait** | base `pharma_smart_demo` chargée (`run_all.sql` de bout en bout, 240/242 contrôles) et instantané de référence figé par `dump_reference.ps1`, qui rend les captures reproductibles |
| 1 — socle Playwright | **fait** | `e2e/`, Playwright 1.62.1, Chromium installé |
| 2 — liage cahier-recette | **fait** | `npm run e2e:liage` — 5 contrôles verts sur les 476 scénarios |
| 3 — captures et index | **fait** | chaîne prouvée hors application : images JPEG + `captures.json` légendé depuis le modèle |
| 4 — injection dans le guide | **fait** | chaîne complète prouvée : campagne → `captures.json` → JSON du guide → actifs Angular. Backend compilé |
| 5 — volume | **objectif dépassé** | 402 parcours illustrés sur 11 modules — accueil 23, achats 76, administration 22, clients 20, comptabilité 13, retraitement du CA 11, facturation 46, produits 57, rapports 33, stock 43, ventes 58 — soit tous les scénarios visibles hors module mobile, laissé de côté |

> **Le premier parcours réel a immédiatement confirmé §3.1.** Écrit avec une assertion faible
> (`getByText(/KOUASSI/).first()`), il passait au vert en photographiant une liste **non
> filtrée** : la capture, prise juste après le clic, précédait le rafraîchissement, et
> l'assertion se contentait d'une ligne quelconque plus bas dans la page. Le manuel aurait
> illustré « rechercher un client » avec un écran qui ne montre aucune recherche.
>
> Correction en un seul geste : ancrer l'assertion sur la **première ligne** du tableau. Elle
> devient impossible à satisfaire sur une liste non filtrée, et comme `expect` réessaie, elle
> attend le rafraîchissement avant que la capture ne soit prise.
>
> D'où la règle à retenir pour le lot 5 : **l'assertion se place dans l'étape, avant la
> capture, et porte sur ce que la légende annonce.** Une assertion placée après la dernière
> étape ne protège plus aucune image.

> **Le lot 5 a déjà servi à autre chose qu'à illustrer.** Écrire un parcours, c'est ouvrir un
> écran avec des données réelles et regarder ce qu'il montre — c'est-à-dire faire, une fois,
> ce que personne ne fait jamais deux fois. Huit défauts applicatifs en sont sortis, tous
> corrigés à la source et listés dans
> [e2e/parcours/README.md](../e2e/parcours/README.md). Trois familles :
>
> - une **erreur 500** sur `/api/dashboard-ca/summary-finances` — un `+` manquant dans du SQL
>   natif (`DashboardCARepository.NB_ECHEANCES_RETARD_SQL`). Le front n'ayant pas de branche
>   d'erreur, le bloc « dettes fournisseurs / créances tiers-payants » du tableau de bord CA
>   ne s'affichait tout simplement pas, sans que rien ne le signale. Le garde-fou « écran sain »
>   a refusé d'illustrer l'écran : c'est exactement ce pour quoi il existe ;
> - des **écrans sans prise** : ng-select n'expose ni rôle ni libellé accessible tant qu'on ne
>   lui donne pas d'`inputId`. Six ont été posés, jamais un `data-testid` ;
> - des **trous du jeu de démonstration** (SEMOIS non initialisé, aucune répartition de stock,
>   aucun retour groupé) qui condamnent une poignée de scénarios à des captures vides. C'est
>   `scripts/demo-data/` qu'il faut compléter, pas les parcours — détail dans
>   [e2e/parcours/README.md](../e2e/parcours/README.md).

**Le jalon de décision est franchi.** La chaîne modèle → parcours → capture → guide fonctionne
de bout en bout ; il ne reste plus qu'à écrire des parcours.

Les trois garde-fous du dispositif ont été vérifiés en les faisant échouer volontairement :
identifiant inconnu (arrêt au chargement), étape du modèle non parcourue, écran non sain
(exception de page). Un parcours en échec n'indexe aucune capture.

### Décisions prises en implémentant le lot 4

- **Les captures ne sont pas écrites dans le modèle.** `cahier-recette.model.ts` gagne un champ
  `captures?`, mais il est **rempli à la génération** depuis `e2e/captures/captures.json`. Sans
  cette séparation, un fichier édité à la main recevrait un diff machine à chaque campagne.
- **Destination `webapp/content/captures/`.** `content/` est déjà déclaré dans les assets
  d'angular.json : les images sont donc servies à l'identique par le serveur de développement
  et par le build de production, et se retrouvent dans le classpath du backend sous `static/`,
  d'où le PDF les lit. Le dossier est un **miroir** — vidé à chaque génération, jamais versionné.
- **Le piège Flying Saucer est levé.** `setDocumentFromString(html)` est appelé sans URL de base :
  une `<img src="content/captures/…">` n'aurait été résolue nulle part, et le PDF serait sorti
  sans images **et sans erreur**. Une URL de base `file:` ne convenait pas non plus, les
  ressources n'étant pas des fichiers une fois l'application packagée en jar. La solution retenue
  est un `ITextUserAgent` dont `openStream` résout depuis le classpath — correct en développement
  comme en jar.

### Comment les images sont disposées dans le manuel

- **Une image sous l'étape qu'elle illustre**, et non une galerie en fin de scénario. Le lecteur
  voit l'écran au moment où le geste est décrit ; il n'a pas à rapprocher lui-même une liste de
  consignes et une liste d'images. Côté PDF, l'étape et son écran forment un bloc insécable
  (`.etape { page-break-inside: avoid }`) : une consigne n'est jamais séparée de son image.
- **Toutes les étapes restent visibles**, illustrées ou non. Une étape sans capture — geste hors
  application, écran non photographié — s'affiche comme du texte : la marche à suivre reste
  complète et sa numérotation continue.
- **Deux étapes consécutives ne sont fusionnées que si l'écran est rigoureusement identique.**
  La comparaison porte sur l'empreinte SHA-256 de l'image, calculée à la génération. Certains
  gestes ne changent rien à l'écran ; répéter l'image laisse croire au lecteur qu'il y a une
  différence à trouver. La fusion est volontairement stricte et locale : une image qui réapparaît
  trois étapes plus loin est de nouveau affichée, parce que le lecteur, lui, a tourné la page.
  Sur la campagne courante, 48 images sur 1 032 sont ainsi écartées.
- **Ni codes techniques, ni discours commercial.** Les identifiants de scénario (`VTE-01`) ne
  servent qu'au liage et ne sont jamais rendus ; les rubriques « bénéfices métier » ont été
  retirées du modèle, du DTO et des deux rendus. Les intitulés sont ceux d'un mode d'emploi :
  *Objectif*, *À savoir*, *Avant de commencer*, *Marche à suivre*, *Vous avez terminé lorsque*.
- **L'écran et le PDF montrent la même chose.** Le composant Angular lit `CAHIER_RECETTE` en
  mémoire, où les captures n'existent pas : sans index, le guide consulté dans l'application
  serait resté sans images. La génération écrit donc, à côté des images,
  `content/captures/index.json` — rang de l'étape et chemin de l'image, fusion comprise — que le
  composant charge en une requête. Son absence (aucune campagne) est un cas normal : le guide
  reste alors purement textuel.

Détail d'utilisation : [e2e/README.md](../e2e/README.md).

---

## 1. Constat de départ

Relevé sur le dépôt, pas sur des suppositions :

| Fait | Valeur | Conséquence |
| --- | --- | --- |
| Outillage e2e existant | **aucun** (ni Playwright, ni Cypress, ni Protractor) | terrain vierge, aucune migration |
| Scénarios dans `cahier-recette.model.ts` | **476**, avec `id` stable (`VTE-01`…), `etapes[]`, `resultatAttendu` | la spec de test existe déjà |
| Modules | 12 | découpage naturel des specs |
| Gabarits HTML | 361 | surface applicative |
| Gabarits portant un `data-testid` | **0** | ⚠ principal poste de coût |
| Gabarits portant un `data-cy` | **7 sur 361** (17 occurrences) | uniquement des écrans générés — connexion, compte, barre de navigation. Suffisant pour `auth.setup.ts`, rien pour les écrans métier |
| `id=` distincts dans les gabarits | 260 | quelques ancrages exploitables, non systématiques |
| Jeton JWT | `localStorage` (`jwt-token.service.ts:27`) | `storageState` Playwright suffit à s'authentifier une fois |
| Jeu de données déterministe | `scripts/demo-data/` (17 scripts, 238 contrôles) | la fixture existe, jamais exécutée à ce jour |
| Intégration continue | **aucun `.github/workflows`** | rien où brancher la suite aujourd'hui |
| Chaîne du guide | `cahier-recette.model.ts` → `generate-cahier-recette-json.ts` → `cahier-recette.json` → `CahierRecettePdfService` (PDF) + composant Angular | deux rendus à alimenter en images |

Node 24 / npm 11 : Playwright s'installe sans contrainte de version.

---

## 2. Pourquoi Playwright, et pas autre chose

Le manuel étant la phase 1, on pourrait croire qu'un simple script Puppeteer suffirait. Les
raisons qui font pencher pour Playwright valent **dès la phase 1** :

- **Attente automatique.** C'est l'argument décisif pour des captures, pas seulement pour des
  tests. L'application est bourrée d'`httpResource`, d'AG Grid et de modales ng-bootstrap
  animées : une capture prise trop tôt montre un tableau vide ou une modale à mi-course. Le
  remède habituel — semer des `sleep()` — donne un manuel truffé d'écrans à moitié chargés.
  Playwright attend l'état stable de lui-même.
- **`page.screenshot()` natif**, avec masquage d'éléments (`mask`), capture pleine page ou d'un
  seul élément, et sortie JPEG. Pas de greffon à entretenir.
- **`storageState`.** Le JWT étant en `localStorage`, une connexion unique en `globalSetup` sert
  tous les parcours. Sans ça, 60 parcours = 60 passages par l'écran de connexion.
- **Il ne sera pas à remplacer en phase 2.** Choisir un outil de capture pur obligerait à tout
  réécrire le jour où l'on veut des tests. Ici la phase 2 est un enrichissement, pas une
  migration.
- **Trace viewer**, utile plus tard : un parcours rouge est rejouable pas à pas avec le DOM et
  les appels réseau.

Cypress conviendrait aussi ; il est moins à l'aise sur le multi-onglet et l'impression, et son
modèle de capture est moins direct. Aucune raison de préférer Selenium.

---

## 3. L'idée directrice : lier la spec, le test et la capture par l'identifiant de scénario

C'est le cœur de la proposition, et ce qui la distingue d'une suite Playwright ordinaire.

`cahier-recette.model.ts` décrit déjà, pour `VTE-01` :

```ts
{
  id: 'VTE-01',
  titre: 'Rechercher et ajouter un produit à la vente en cours',
  etapes: ['Rechercher le produit par nom ou scanner son code-barres',
           'Choisir la quantité', 'Ajouter à la vente en cours'],
  resultatAttendu: 'La ligne apparaît dans le panier avec son prix net et sa quantité.',
}
```

C'est un cas de test. Il ne manque que l'exécution. La spec Playwright ne le recopie pas, elle
s'y **rattache** :

```ts
// e2e/specs/ventes/vte-01-ajout-produit.spec.ts
scenario('VTE-01', ({ etape, page, vente }) => {
  etape(1, async () => { await vente.rechercher('PARACETAMOL'); });
  etape(2, async () => { await vente.saisirQuantite(2); });
  etape(3, async () => { await vente.ajouter(); });

  await expect(vente.lignePanier('PARACETAMOL')).toBeVisible();
});
```

Ce que la fixture `scenario()` apporte, et qui justifie l'indirection :

1. **Elle échoue si `VTE-01` n'existe pas dans le modèle**, ou si le nombre d'`etape()` ne
   correspond pas à `etapes.length`. Le jour où quelqu'un ajoute une étape à la
   documentation sans toucher au test, la suite le dit.
2. **En mode capture** (`CAPTURE=1`), chaque `etape(n)` prend une capture après son bloc et
   l'enregistre sous `VTE-01/etape-1.jpg`. En mode ordinaire, elle ne fait rien : les tests de
   non-régression restent rapides.
3. **La légende de l'image est le texte de l'étape**, tiré du modèle. Le manuel ne peut pas
   diverger de ce qui a été exécuté.

En phase 1 le fichier ci-dessus est un **parcours de capture** ; en phase 2 il devient un test,
par ajout d'`expect` — sans changer d'outil, de structure ni de nom de fichier.

### 3.1 Pourquoi la phase 1 ne peut pas être « sans assertion du tout »

Le réflexe, quand seul le manuel compte, est de retirer toute vérification : on navigue, on
capture, on n'affirme rien. C'est un piège, et il faut le nommer maintenant.

**Un parcours sans assertion photographie sans broncher une page d'erreur, un tableau vide ou une
session expirée.** Le manuel est alors faux, et personne ne s'en aperçoit avant le lecteur. Un
manuel qui montre le mauvais écran est pire que pas de manuel : il est cru.

La contrepartie minimale — et suffisante — est **une assertion par scénario**, celle qui traduit
le `resultatAttendu` déjà écrit dans le modèle. Une ligne, celle du `expect` de l'exemple. Elle
ne fait pas de ce parcours un test de non-régression ; elle garantit seulement que la capture
montre ce que sa légende prétend.

C'est la limite entre les deux phases : **phase 1, on affirme que l'écran est le bon ; phase 2,
on affirme que le comportement est le bon.** La première est une exigence de qualité du manuel,
pas une anticipation du travail de test.

Le manuel utilisateur devient alors un sous-produit vérifié : une capture n'existe que si le
parcours qui l'a produite est passé au vert.

---

## 4. Comment les images rejoignent le guide

Point à trancher avec soin : le modèle est **édité à la main**, la capture est **produite par
la machine**. Les mélanger dans le même fichier condamne `cahier-recette.model.ts` à des diffs
générés à chaque exécution.

Proposition : garder les deux séparés et ne les réunir qu'à la génération.

```
cahier-recette.model.ts          (main)     ─┐
                                             ├─→ generate-cahier-recette-json.ts ─→ cahier-recette.json
e2e/captures/captures.json       (machine)  ─┘        (fusion par id de scénario)
```

`captures.json` est écrit par le reporter Playwright :

```json
{ "VTE-01": [{ "fichier": "VTE-01/etape-1.jpg", "legende": "Rechercher le produit…" }] }
```

Le générateur, qui fait aujourd'hui un `JSON.stringify` direct (`generate-cahier-recette-json.ts:19`),
fusionne le fichier s'il est présent et se comporte comme aujourd'hui s'il est absent. Le build
Maven (`pharmaSmart-app/pom.xml:169`, phase `generate-resources`) n'a pas à changer.

Il reste à ajouter `captures?: CaptureEcran[]` sur `ScenarioRecette` et sur son miroir Java
`ScenarioRecetteDTO` (record, champ à ajouter), puis à rendre les images aux deux endroits :

- **Composant Angular** — `cahier-recette.component.html:118`, une balise `<img>` sous chaque
  `<li>` d'étape. Sans difficulté.
- **PDF Flying Saucer** — `main.html:286`. ⚠ **Piège identifié** :
  `CahierRecettePdfService` appelle `renderer.setDocumentFromString(html)` **sans URL de base**
  (lignes 82 et 98). Une `<img src="captures/…">` relative ne se résoudra pas. Deux issues :
  passer la surcharge `setDocumentFromString(html, baseUrl)`, ou encoder les images en `data:`
  dans le contexte Thymeleaf. À vérifier sur le jar au moment de l'implémentation, comme l'a
  déjà été `findPagePositionsByID`.

---

## 5. Les cinq obstacles réels

Aucun n'est bloquant. Ils sont classés par coût.

### 5.1 Zéro `data-testid` sur 361 gabarits — *le poste principal*

Presque rien à quoi s'accrocher. Sept gabarits portent des `data-cy`, héritage du générateur —
connexion, écrans de compte, barre de navigation. C'est une chance ponctuelle : `auth.setup.ts`
s'en sert et n'a donc besoin d'aucun ancrage nouveau. **Aucun écran métier n'en a**, et c'est là
que se fera tout le travail.

La parade n'est pas d'annoter les 361 gabarits :

1. **Sélecteurs par rôle et libellé d'abord** (`getByRole('button', { name: 'Valider' })`,
   `getByLabel('Libellé')`). Ils ne coûtent rien et testent au passage l'accessibilité. Le
   Design System maison rend du Bootstrap natif : les rôles sont corrects.
2. **`data-testid` uniquement là où le libellé est ambigu ou instable** — grilles AG Grid,
   lignes de tableau, boutons d'action répétés. À vue de nez : **60 à 90 attributs** sur les
   écrans effectivement couverts, pas 361.

Ne pas annoter à l'avance « pour plus tard » : on paie l'entretien d'ancrages jamais utilisés.

### 5.2 Le jeu de données

Un manuel dont les copies d'écran montrent des tableaux vides ne vaut rien, et une suite e2e sans
données déterministes est rouge un jour sur deux. Les deux besoins se confondent :
`scripts/demo-data/` y répond.

La base est créée en amont par `scripts/demo-data/create_database.sql`, désormais complété pour
donner **tous les droits** à `pharma_smart` (propriété de la base et du schéma, privilèges sur
les objets existants, `ALTER DEFAULT PRIVILEGES` pour ceux que Flyway créera, et l'attribut
`CREATEDB` qui permet au rôle de refaire lui-même sa base sans repasser par un superutilisateur).
Le script est idempotent et se termine par un contrôle explicite des droits obtenus.

Reste un point à traiter, propre aux captures : les dates des scripts de démo sont **relatives au
jour d'exécution**. Une capture prise le 27/08 et une prise le 28/08 diffèrent sur toutes les
colonnes de date → différence d'image permanente, et un manuel qui se contredit d'une édition à
l'autre.

Deux parades, à combiner :

- restaurer la base depuis un `pg_dump` **figé** avant chaque campagne de captures — rapide, et
  surtout identique d'une fois sur l'autre ;
- rendre la date de référence des scripts paramétrable au lieu de `CURRENT_DATE`, pour pouvoir
  regénérer ce dump à l'identique.

**C'est le vrai prérequis du manuel**, davantage que Playwright lui-même.

### 5.3 Poids et versionnement des images

60 scénarios × 3 étapes ≈ 180 images. En PNG pleine page 1280×800, ~35 Mo ; en JPEG qualité 80,
~8 Mo. Recommandation : **JPEG**, et **ne pas versionner les captures** — les régénérer à la
demande dans `pharmaSmart-app/target/`, et ne figer un lot dans `docs/captures/` que lorsqu'on
publie une version du manuel. Sinon chaque exécution pollue l'historique Git.

### 5.4 Tauri n'est pas testable par Playwright

L'application de bureau tourne dans WebView2 ; Playwright ne le pilote pas. Il faudrait
`tauri-driver` + WebdriverIO, une seconde chaîne à entretenir.

**Recommandation : ne pas le faire.** L'Angular servi par Tauri est le même code ; le tester via
le navigateur couvre la fonctionnalité. Restent hors de portée les commandes Rust —
`src-tauri/src/printer.rs`, l'impression thermique, l'accès disque. Ces parties se testent par
tests unitaires Rust, pas en bout en bout.

### 5.5 Pas d'intégration continue — reporté en phase 2, avec sa conséquence

Il n'y a aucun `.github/workflows`. La campagne de captures tournera en local, ce qui convient
parfaitement au manuel : on la lance quand on veut une édition.

La conséquence à assumer est ailleurs : **pendant toute la phase 1, les parcours se dégraderont
sans que personne ne le voie.** Une refonte d'écran casse un parcours ; on ne le découvre qu'à la
campagne suivante, quand il faut livrer. Ce n'est pas grave si l'on s'y attend, coûteux si l'on
compte dessus au dernier moment.

Deux garde-fous à coût quasi nul, à retenir dès le lot 1 : lancer `npm run captures` avant chaque
livraison, et garder les parcours **courts et indépendants**, pour qu'un écran remanié n'en casse
qu'un seul.

---

## 6. Découpage proposé

### Phase 1 — Le manuel

| Lot | Contenu | Livrable vérifiable |
| --- | --- | --- |
| **0. Socle données** | base créée via `create_database.sql` (fait), scripts de démo exécutés, `pg_dump` de référence figé, date de référence paramétrable | la base se restaure et `99_verification.sql` passe ses 238 contrôles |
| **1. Socle Playwright** | `e2e/`, `playwright.config.ts`, `globalSetup` (connexion + `storageState`), 1 parcours de fumée | `npm run captures` vert sur « se connecter et atteindre le tableau de bord » |
| **2. Liage cahier-recette** | fixture `scenario()`, contrôle d'existence de l'id, contrôle du nombre d'étapes | un parcours rattaché à `VTE-01` ; un id inventé fait échouer l'exécution |
| **3. Captures** | mode `CAPTURE=1`, reporter écrivant `captures.json`, JPEG qualité 80 | `captures.json` produit, images sur disque |
| **4. Injection dans le guide** | champ `captures` (TS + DTO Java), fusion dans le générateur, rendu Angular, rendu PDF avec URL de base | **le PDF téléchargé contient l'image de `VTE-01`** |
| **5. Volume** | 50 à 70 scénarios illustrés, une assertion chacun (§3.1), `data-testid` posés au fil de l'eau | manuel PDF illustré sur les 12 modules |

Le **lot 4 est le jalon de décision** : il prouve la chaîne de bout en bout — modèle → parcours →
capture → PDF — sur **un seul** scénario. C'est l'ordre qui échoue le plus tôt et le moins cher.
Tant qu'il n'est pas franchi, écrire des parcours en masse serait un pari.

### Phase 2 — Les tests de bout en bout

Déclenchée **après** la livraison du manuel. Elle ne recommence rien : elle reprend les fichiers
de la phase 1.

| Lot | Contenu | Livrable vérifiable |
| --- | --- | --- |
| **6. Assertions** | enrichissement des parcours existants : états intermédiaires, effets sur le stock, la caisse, la comptabilité | les parcours échouent sur une régression injectée volontairement |
| **7. Intégration continue** | workflow, base restaurée depuis le dump figé, publication du rapport et des traces | la suite tourne sur chaque *pull request* |
| **8. Cas d'échec** | ce qu'un manuel n'illustre pas : saisies invalides, droits insuffisants, ruptures de stock | couverture des chemins d'erreur |

Deux avertissements sur l'intervalle entre les phases.

**Les parcours se dégraderont pendant la phase 1.** Sans intégration continue, une refonte d'écran
casse des parcours sans que personne ne le sache — on ne le découvre qu'à la campagne de captures
suivante. C'est le prix assumé du séquencement, pas un oubli. Le limiter coûte peu : relancer
`npm run captures` avant chaque livraison suffit à voir ce qui a cassé.

**Le lot 6 sera d'autant moins cher que le lot 5 aura été honnête.** Un parcours de capture écrit
avec de bons sélecteurs et son assertion de §3.1 se transforme en test en quelques lignes. Un
parcours écrit avec des `sleep()` et des sélecteurs CSS fragiles sera à refaire — et la phase 1
n'aura alors rien capitalisé.

---

## 7. Ce que ça ne fera pas

À dire maintenant plutôt qu'après :

- **Pas 437 scénarios.** Viser 50 à 70. La priorité « manuel » change le critère de sélection :
  on illustre en premier ce qu'un utilisateur **n'arrive pas à faire sans image** — écrans denses,
  parcours à plusieurs étapes, formulaires à règles implicites — et non ce dont la régression
  coûterait le plus cher. Le reste du guide restera textuel, ce qui est acceptable : un manuel
  n'a pas besoin d'une capture par paragraphe.
- **Pas de test de l'impression thermique**, ni des commandes Rust de Tauri.
- **Pas de remplacement des tests Jest**, ni en phase 2. Le e2e est lent et coûteux à entretenir ;
  il valide des parcours, pas des règles de calcul. La remise, la ventilation tiers payant ou la
  consommation FEFO se testent bien moins cher en unitaire, côté Java.
- **Pas de manuel automatique de bout en bout.** Les captures illustrent un texte qui reste
  rédigé à la main. C'est d'ailleurs souhaitable : une capture sans explication n'est pas un
  manuel.
- **Pas de filet de non-régression avant la phase 2.** À la fin de la phase 1 il existera des
  parcours automatisés — ce ne sont pas encore des tests. Les faire passer pour tels donnerait
  une fausse assurance : ils vérifient qu'un écran s'affiche, pas qu'un calcul est juste.

---

## 8. Décisions à trancher avant d'écrire du code

1. **Captures versionnées ou non ?** Recommandation : non versionnées par défaut, un lot figé
   dans `docs/captures/` au moment de publier une édition du manuel. Sinon chaque exécution
   pollue l'historique Git de plusieurs mégaoctets d'images.
2. **Date de référence des scripts de démo** — la rendre paramétrable maintenant (lot 0) ou
   accepter que les captures ne soient pas reproductibles ? La première option coûte peu et évite
   un manuel qui se contredit d'une édition à l'autre.
3. **Périmètre du lot 5** — quels modules illustrer en premier ? Proposition alignée sur la
   priorité manuel : Ventes (l'écran le plus dense), puis Commandes/Réception (parcours le plus
   long), puis Facturation/tiers payant (règles les moins devinables).
4. **Discipline d'écriture en phase 1.** Point le plus facile à négliger et le plus coûteux à
   rattraper : accepte-t-on d'écrire les parcours de capture avec la rigueur d'un test —
   sélecteurs par rôle, aucune temporisation fixe, une assertion par scénario — alors que rien
   ne l'impose encore ? C'est ce qui décide si la phase 2 est un enrichissement ou une réécriture.

*Le point qui bloquait — l'accès superutilisateur pour créer `pharma_smart_demo` — est levé :
la base est créée en amont, et `create_database.sql` accorde désormais l'ensemble des droits.*

---

## 9. Estimation

Ordre de grandeur, à affiner.

**Phase 1 — le manuel**

| Lot | Charge |
| --- | --- |
| 0 — socle données (base déjà créée ; reste l'exécution, le dump figé, la date paramétrable) | 1 à 1,5 j |
| 1 — socle Playwright | 1 j |
| 2 — liage cahier-recette | 1 j |
| 3 — captures | 1 j |
| 4 — injection guide (TS + Java + PDF) | 1,5 j, dont le piège de l'URL de base Flying Saucer |
| **Jalon : chaîne complète prouvée sur un scénario** | **≈ 5,5 j** |
| 5 — 50 à 70 scénarios illustrés, `data-testid` compris | 8 à 12 j |
| **Total phase 1** | **≈ 14 à 18 j** |

**Phase 2 — les tests de bout en bout**

| Lot | Charge |
| --- | --- |
| 6 — assertions sur les parcours existants | 4 à 6 j *(si le lot 5 a été écrit proprement ; le double sinon)* |
| 7 — intégration continue | 1 à 2 j |
| 8 — cas d'échec | 3 à 5 j |
| **Total phase 2** | **≈ 8 à 13 j** |

Le lot 5 domine la phase 1, et c'est normal : c'est le seul qui produise du volume. Les lots 0 à 4
suffisent à obtenir un PDF illustré — sur un seul scénario — et donc à décider de la suite en
connaissance de cause plutôt que sur estimation.

La fourchette du lot 6 est la seule qui dépende d'un choix déjà fait aujourd'hui, pas d'un aléa :
elle double si la phase 1 est écrite « puisque ce ne sont que des captures ».
