# Plan — Améliorations UX du choix des modes de règlement

> Statut : idées non validées, à discuter avant toute implémentation.
> Contexte : notées lors de la migration de `payment-mode.component.ts`
> (PrimeNG → ng-bootstrap), pas encore mises en œuvre.

## Fonctionnement actuel

- L'écran démarre avec une seule ligne « Espèces » (CASH).
- Un bouton « + » ouvre un popover listant les modes non encore utilisés ;
  en choisir un ajoute une ligne.
- Le bouton « x » d'une ligne :
  - **supprime** la ligne directement s'il en reste plusieurs ;
  - **ouvre un popover de remplacement** si c'est la dernière ligne restante
    (impossible de descendre à zéro mode).
- Pas de réordonnancement. Les montants ne se redistribuent que dans deux cas :
  la suppression d'une ligne reverse son montant sur la première ligne restante,
  et l'ajout d'un mode quand `maxPaymentModes` est atteint réattribue
  automatiquement le reste à l'autre ligne.

## Pistes d'amélioration (non tranchées)

1. **Un même bouton « x » avec deux comportements différents.**
   Il supprime ou remplace selon le nombre de lignes restantes, sans indice
   visuel pour savoir lequel des deux va se produire. Piste : garder « x »
   uniquement pour supprimer (y compris sur la dernière ligne — descendre à
   zéro mode est un état incomplet déjà géré par `canSubmit`), et donner au
   remplacement sa propre affordance (ex. cliquer l'icône du mode lui-même
   pour le changer). Cela sépare deux intentions actuellement confondues
   dans un seul bouton.

2. **Le popover de sélection de mode n'est qu'une grille d'icônes**, sans
   libellé. Ajouter un texte sous chaque icône (ou a minima un tooltip,
   peu coûteux) aiderait un caissier peu familier avec les pictogrammes.

3. **La redistribution automatique des montants est surprenante** : le solde
   est reversé sur « la première ligne », pas nécessairement celle qu'on
   vient de modifier — c'est un effet de bord de l'ordre du tableau plutôt
   qu'un choix UX assumé. À trancher explicitement : redistribuer vers la
   dernière ligne ajoutée ? vers celle qui n'a pas le focus ? etc.

4. **Aucune confirmation ni annulation à la suppression d'une ligne.** Sur un
   poste de vente l'impact est faible (on peut rajouter le mode
   immédiatement), mais ça mérite d'être un choix déliberé plutôt qu'un
   comportement hérité par défaut.

5. **Popover d'icônes vs menu déroulant.** Le reste de l'application migrée
   utilise maintenant le motif `ngbDropdown` pour les menus d'actions par
   ligne (cf. `sales-journal`, `devis-list`). Envisager d'aligner la
   sélection de mode de paiement sur ce même motif pour la cohérence
   visuelle globale, plutôt que de garder un popover d'icônes spécifique.
   NB: voir l'utilisation de menu déroulant avec des icon ou avec les libellé de mode reglement stylisé comme icon image
6. ** Proposer des racourcis clavier pour la gestion de mode reglement **

## Prochaine étape

Choisir avec l'équipe produit lesquelles de ces pistes valent la peine
d'être implémentées, puis détailler un vrai plan d'implémentation
(maquettes, impact sur `PaymentModeEntry`/`PaymentModeManagerService`,
tests) avant de toucher au code.
