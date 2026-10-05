# Thèmes de l'application — reste à faire

Document de suivi, tiré de [PLAN-THEMES-APPLICATION.md](PLAN-THEMES-APPLICATION.md) (journal détaillé, §10) et de
[PLAN-ORGANISATION-BOUTONS-ACTION.md](PLAN-ORGANISATION-BOUTONS-ACTION.md). Mis à jour le 2026-10-04.

Ce qui est fait n'est pas repris ici : thèmes dérivés et garde-fou de contraste (659 tests), comptoir / pré-vente / dépôt /
proforma sur le thème de l'application, couleurs en dur réécrites (34 feuilles hors vente + 18 de la vente), page de connexion,
focus clavier des deux menus latéraux, aide du comptoir (F1 et bouton), thème par défaut provisoire **Indigo**.

## 1. Décisions qui vous reviennent

- [ ] **Plan des boutons, §8** — cinq décisions restent à valider (« Rechercher » en `info` est acté) :
  - position du bouton principal (bord droit, dernier au clavier, ou à gauche et premier) ;
  - sort de `info`, `help` et `contrast` sur les boutons d'action (la décision n° 3 est caduque pour `info`, à réécrire) ;
  - conventions PDF / Excel (menu « Exporter ▾ » neutre ?) ;
  - taille des cibles tactiles (24 px partout, 44 px pour la vente et les écrans au doigt ?) ;
  - `role=toolbar` avec navigation aux flèches.
- [ ] **« Régler tout » (comptes fournisseurs) et « Régler » dans Différés** — les passer en `primary` ?
- [ ] **Écrans avec deux boutons `primary` côte à côte** — faut-il en établir la liste ?
- [ ] **Ligne sélectionnée des tableaux** — corriger l'assombrissement des cellules après la première (défaut ancien, aussi sous « Bleu acier »).
- [ ] **Sélecteur de couleur du proforma** (violet, sarcelle, indigo) — n'a plus d'effet : le retirer ?
- [ ] **Reçu après un mouvement de caisse au comptoir** — l'imprimer, comme sur l'écran des mouvements de caisse ?
- [ ] **Pré-vente, touche Entrée** dans le champ produit vide, panier rempli : comportement attendu à confirmer (aucun règlement affiché ;
  en assurance ou carnet, le code finalise si le montant à payer est 0).
- [ ] **Ligne « Entrée — Ouvrir le type de vente choisi »** de l'aide : la garder ou la supprimer.
- [ ] **Migration pour ROLE_CAISSIER** (`ventes.favoris.gerer`) : le droit existe déjà (accès, modification, suppression) ; créer une `V2.1.28` explicite
  seulement si vous le souhaitez, en précisant les droits à ajouter.
- [ ] **Supprimer l'ancien code d'aide** : `SellingHomeShortcutsService` (plus injecté nulle part) et `ShortcutsHelpDialogComponent`.

## 2. Chantiers de migration

- [ ] **`app-card`** : écrans sans conteneur `.pharma-smart-content` (rapports, `commande-*`, mouvements de caisse), formulaires et éditeurs.
- [ ] **`<table class="table …">`** restants à remplacer par `app-data-table`.
- [ ] **`ngbNav`** restants à passer en `app-nav-tabs` — à dénombrer (`top-products` est fait).
- [ ] **Bandeaux d'indicateurs écrits à la main** : chercher d'autres cas que `home-base` (fait) pour les passer à `app-kpi-strip`.
- [ ] **Couleurs laissées en dur** : `$gc-primary` (`_grid-caption.scss`) et `$primary-blue` (`inventory-home`), qui passent par des fonctions Sass ;
  le dégradé `.bg-primary` de `table-common-global.scss` (utilitaire de gravité, à décider).
- [ ] **Doublon d'icône** dans le bandeau du comptoir (titre et pastille de type) : à arbitrer à l'usage.
- [ ] **Titre « Types de Pré-vente »** sur deux lignes dans la colonne : défaut ancien, non traité.

## 3. Vérifications d'accessibilité

- [ ] **Non faites** : taille du texte (1.4.4), reflow (1.4.10), espacement du texte (1.4.12), contenu au survol (1.4.13), daltonisme sur les thèmes.
- [ ] **Non vérifiés** : bordure des champs de formulaire au focus, surbrillance de `ng-select`.
- [ ] **À surveiller** : ressemblance entre `primary` et `success` dans le thème Vert.
- [ ] **Noms accessibles** : les menus latéraux (`app-nav-sidebar`) des écrans n'ont pas de nom, à poser écran par écran.
- [ ] **Icône du lien actif** de `app-nav-sidebar` à 2,6:1 (ambre, lime) : acceptable car elle accompagne un libellé, à confirmer.



## 4. Tests

- [ ] Des suites Jest de `shared/` et d'`entities` échouent pour des raisons sans rapport avec les thèmes : les comparer à une base propre.
- [ ] Les e2e sans thème explicite (et les captures de référence) passent en Indigo avec le nouveau défaut : vérifier qu'aucun ne dépend de « Bleu acier ».

## 6. Phase 5 — mise en service

- [ ] Essais par les utilisateurs, puis choix **définitif** du thème par défaut (`DEFAULT_CHROME`, aujourd'hui Indigo, provisoire).
- [ ] Notes de version : écarts de « Bleu acier » (anciennement « Actuel ») par rapport à l'ancien rendu — en-tête de tableau, en-tête de barre
  d'outils, anneau de focus, couleurs des sous-onglets ; accent unique du comptoir (plus de couleur par type de vente) ; F1 ouvre un panneau
  à gauche et non plus une modale ; Échap ferme tous les panneaux latéraux.
