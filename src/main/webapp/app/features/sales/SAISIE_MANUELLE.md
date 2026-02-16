

# Amélioration de la saisie manuelle produit

## Objectif
Améliorer le comportement de la **recherche lors de la saisie manuelle au clavier**.

## Constat actuel
- Après la saisie de **3 caractères**, si un seul résultat correspond, le produit est **sélectionné automatiquement**.
- Ce comportement empêche l’utilisateur de **continuer à taper** pour préciser ou corriger sa recherche.
- Risque d’erreurs ou de frustration pour l’utilisateur.

## Attendu
- Analyser spécifiquement le cas de la **saisie manuelle** (hors scan).
- Identifier les faiblesses UX liées à la sélection automatique après 3 caractères.
- Proposer une solution **fiable, robuste et user-friendly** qui :
  - Laisse le **contrôle à l’utilisateur**,
  - Évite les **sélections involontaires**,
  - Reste **rapide et efficace**,
  - Différencie le comportement entre **saisie clavier** et **scan** si nécessaire.

## Contraintes
- Ergonomie cohérente et prévisible pour l’utilisateur.
- Adaptée à un contexte applicatif métier.
- Compatible avec des saisies partielles ou complètes avant validation.

