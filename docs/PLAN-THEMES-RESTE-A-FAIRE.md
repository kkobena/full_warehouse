# Thèmes de l'application — reste à faire

Document de suivi, tiré de [PLAN-THEMES-APPLICATION.md](PLAN-THEMES-APPLICATION.md) (journal détaillé, §10) et de
[PLAN-ORGANISATION-BOUTONS-ACTION.md](PLAN-ORGANISATION-BOUTONS-ACTION.md). Mis à jour le 2026-10-09.

Ce qui est fait n'est pas repris ici : thèmes dérivés et garde-fou de contraste (659 tests), comptoir / pré-vente / dépôt /
proforma sur le thème de l'application, couleurs en dur réécrites (34 feuilles hors vente + 18 de la vente, puis 17 de plus le
2026-10-09 : `_grid-caption`, `_expanded-row`, cartes d'indicateurs et rapports — jetons `--pharma-chrome-brand` / `-brand-light` /
`-brand-ink`, valeurs d'avant sous « Bleu acier »), page de connexion, focus clavier des deux menus latéraux, aide du comptoir
(F1 et bouton), thème par défaut provisoire **Indigo**.

Fait le 2026-10-09, sur vos décisions : navigation aux flèches dans `app-toolbar`, ligne sélectionnée sans assombrissement,
sélecteur de couleur du proforma retiré, reçu proposé après un mouvement de caisse au comptoir, Entrée en pré-vente, ancien code
d'aide supprimé, teintes de `table-common-global.scss` sur le thème, doublon d'icône et titre « Types de Pré-vente » du comptoir,
surbrillance de `ng-select`, sélection de texte et halo de focus. Détail et points à voir à l'écran au §5.

## 1. Décisions qui vous reviennent

- [ ] **Plan des boutons, §8** — cinq décisions restent à valider (« Rechercher » en `info` est acté) :
  - position du bouton principal (bord droit, dernier au clavier, ou à gauche et premier) ;
  - sort de `info`, `help` et `contrast` sur les boutons d'action (la décision n° 3 est caduque pour `info`, à réécrire) ;
  - conventions PDF / Excel (menu « Exporter ▾ » neutre ?) ;
  - taille des cibles tactiles (24 px partout, 44 px pour la vente et les écrans au doigt ?).
- [ ] **« Régler tout » (comptes fournisseurs) et « Régler » dans Différés** — les passer en `primary` ?
- [ ] **Écrans avec deux boutons `primary` côte à côte** — faut-il en établir la liste ?
- [ ] **Navigation aux flèches : tabulation itinérante ?** Les flèches sont en place, mais Tab passe encore par chaque bouton de la
  barre. Le patron complet de l'APG n'y laisse qu'un arrêt ; à décider à l'usage (cela change les habitudes clavier et les parcours e2e).
- [ ] **Ligne « Entrée — Ouvrir le type de vente choisi »** de l'aide : la garder ou la supprimer.
- [ ] **Migration pour ROLE_CAISSIER** (`ventes.favoris.gerer`) : le droit existe déjà (accès, modification, suppression) ; créer une `V2.1.28` explicite
  seulement si vous le souhaitez, en précisant les droits à ajouter.

## 2. Chantiers de migration

- [ ] **`app-card`** : écrans sans conteneur `.pharma-smart-content` (rapports, `commande-*`, mouvements de caisse), formulaires et éditeurs.
- [ ] **`<table class="table …">`** restants à remplacer par `app-data-table`.
- [ ] **`ngbNav`** restants à passer en `app-nav-tabs` — à dénombrer (`top-products` est fait).
- [ ] **Bandeaux d'indicateurs écrits à la main** : chercher d'autres cas que `home-base` (fait) pour les passer à `app-kpi-strip`.
- [ ] **Couleurs laissées en dur** :
  - l'assistant de première installation (`setup-wizard`) : fond en dégradé `#008cba → #5bc0de`, texte et titre blancs (titre en
    dégradé découpé). Les jetons de la connexion (`--pharma-chrome-login-*`) conviendraient, mais le titre blanc disparaîtrait sur
    les fonds clairs des thèmes dérivés : à reprendre avec un essai à l'écran.
- [ ] **À regarder à l'écran sous un thème dérivé** (réécrites le 2026-10-09, non vues) : en-tête du récapitulatif de
  `declaration-tva` (passé à l'en-tête clair des tableaux), cartes « primaire » du journal des ventes, du récapitulatif de caisse,
  de la balance et de la synthèse d'activité ; `.text-primary` global (`dashboard-common-global.scss`), qui imposait `#008cba` en
  `!important` par-dessus l'encre des thèmes dérivés et suit désormais `--pharma-chrome-brand-ink` — à repasser à axe.

## 3. Vérifications d'accessibilité

- [ ] **Non faites** : taille du texte (1.4.4), reflow (1.4.10), espacement du texte (1.4.12), contenu au survol (1.4.13), daltonisme sur les thèmes.
- [ ] **À surveiller** : ressemblance entre `primary` et `success` dans le thème Vert.
- [ ] **Noms accessibles** : les menus latéraux (`app-nav-sidebar`) des écrans n'ont pas de nom, à poser écran par écran.
- [ ] **Icône du lien actif** de `app-nav-sidebar` à 2,6:1 (ambre, lime) : acceptable car elle accompagne un libellé, à confirmer.

## 4. Tests

- [ ] Des suites Jest de `shared/` et d'`entities` échouent pour des raisons sans rapport avec les thèmes : les comparer à une base propre.
- [ ] Les e2e sans thème explicite (et les captures de référence) passent en Indigo avec le nouveau défaut : vérifier qu'aucun ne dépend de « Bleu acier ».

## 5. À vérifier à l'écran (modifié le 2026-10-09, compilé et testé, non vu)

- [ ] **`app-toolbar`** : zone d'actions en `role="toolbar"` nommée « Actions : <titre> » ; ← → bouclent, Début / Fin, boutons
  désactivés ou masqués sautés, flèches laissées aux champs de saisie.
- [ ] **Ligne sélectionnée** : Bootstrap assombrissait chaque cellule par une ombre interne (rayure, survol) dont seule la première,
  porteuse du liseré, était exemptée. Variables `--bs-table-bg-*` neutralisées sur `tr.ligne-selected`.
- [ ] **Comptoir** : icône du titre = mode (panier, signet, proforma), celle du type de vente n'est plus que dans la pastille ; titre de
  colonne sur une ligne (124 px pour 134 disponibles), points de suspension au-delà.
- [ ] **Pré-vente, Entrée** dans le champ produit vide, panier rempli : confirmation « Finaliser la pré-vente », puis **Enregistrer**
  (pas « Passer en vente ») — comptant, assurance, carnet ; ligne ajoutée à l'aide F1.
- [ ] **Mouvement de caisse au comptoir** : confirmation « Voulez-vous imprimer le reçu ? » (Tauri ou serveur, comme l'écran des
  mouvements) ; le toast de succès est retiré, la confirmation en tient lieu.
- [ ] **`table-common-global.scss`** : survol de ligne, fond de `.pharma-code`, badge `primary` sur jetons (`--pharma-row-hover-bg`,
  `--pharma-code-bg`, `--pharma-badge-primary-*`) ; en-têtes des sous-tableaux (cyan, et `.bg-primary`) à l'en-tête clair du thème,
  `.bg-info` / `.bg-success` inchangés. « Bleu acier » garde ses valeurs.
- [ ] **`ng-select`** : option active au clavier en teinte de l'accent avec liseré (le gris d'avant faisait 1,1:1 sur le panneau) ; la
  règle grise de `global.scss` est supprimée.
- [ ] **Sélection de texte** : `::selection` global, teinte `--p-primary-200` et encre foncée imposée (lisible sur les barres sombres).
- [ ] **Halo de focus** des champs : 25 % au lieu de 15 % sous les fonds de page (déjà 25 % sous les thèmes de couleur).

## 6. Phase 5 — mise en service

- [ ] Essais par les utilisateurs, puis choix **définitif** du thème par défaut (`DEFAULT_CHROME`, aujourd'hui Indigo, provisoire).
- [ ] Notes de version : écarts de « Bleu acier » (anciennement « Actuel ») par rapport à l'ancien rendu — en-tête de tableau, en-tête de barre
  d'outils, anneau de focus, couleurs des sous-onglets ; accent unique du comptoir (plus de couleur par type de vente) ; F1 ouvre un panneau
  à gauche et non plus une modale ; Échap ferme tous les panneaux latéraux.
