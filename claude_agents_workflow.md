# Workflow complet : Architect → Code Writer → Code Reviewer

Ce fichier présente un **workflow complet pour un projet Java/Spring Boot** en utilisant les agents Claude : Architect, Code Writer et Code Reviewer.

---

## Contexte
Projet : Traitement de fichiers `.data` et `.queue` via Spring Batch, stockage des résultats en base, avec API REST pour consultation.

---

## 1. Architect (Software Architect)

### Objectif
Définir l’architecture technique et les composants clés.

### Commande / Prompt
```
/architect Conçois une architecture pour une application Spring Boot qui :
- Lit des fichiers .data et .queue,
- Transforme les données via Spring Batch,
- Stocke les résultats dans une base SQL,
- Expose une API REST pour consulter les données,
- Prévoit un logging et une gestion des erreurs centralisée.
```

### Résultat attendu
- Liste des modules / microservices :
  - **Batch Processor** : lecture + transformation
  - **Database Layer** : DAO / Repository
  - **REST API** : endpoints pour consultation
  - **Logging & Monitoring**n- Diagramme logique (C4 ou simple)
- Technologies recommandées (Spring Boot, Spring Batch, PostgreSQL, Lombok, etc.)

---

## 2. Code Writer

### Objectif
Générer le code fonctionnel pour chaque module.

### Commande / Prompt (Batch Processor)
```
/code_writer Écris un service Spring Boot pour lire des fichiers .data et .queue depuis un dossier d'entrée,
les transformer (extraction et validation des champs),
et les stocker dans une table SQL "processed_data".
Utilise Spring Batch et Lombok pour les modèles.
Inclue un Job, Step, et un ItemProcessor.
```

### Commande / Prompt (REST API)
```
/code_writer Génère un contrôleur REST Spring Boot avec endpoints :
- GET /data → retourne toutes les données traitées
- GET /data/{id} → retourne une donnée par ID
- POST /data → ajoute une donnée manuellement
Inclue validation et gestion d’erreurs.
```

### Résultat attendu
- Fichiers Java complets (JobConfig, Processors, Entities, Controllers)
- Tests unitaires simples
- Structure Maven ou Gradle prête à compiler

---

## 3. Code Reviewer

### Objectif
Analyser et améliorer le code généré par Code Writer.

### Commande / Prompt (Batch Processor)
```
/code_reviewer Analyse le code du service Batch Processor généré pour Spring Boot.
- Signale les bugs ou erreurs potentielles.
- Propose des optimisations de performance.
- Vérifie les bonnes pratiques Java et Spring (SOLID, Clean Code).
```

### Commande / Prompt (REST API)
```
/code_reviewer Revois le code du contrôleur REST généré.
- Vérifie la validation des entrées.
- Propose des améliorations pour la sécurité et la gestion d’erreurs.
- Optimise le code si nécessaire.
```

### Résultat attendu
- Liste de corrections ou améliorations
- Suggestions de refactoring
- Conseils pour tester ou documenter le code

---

## 4. Itération et intégration

1. Appliquer les suggestions du **Code Reviewer**
2. Repasser éventuellement le code corrigé dans **Code Reviewer** pour vérification
3. Intégrer les modules dans le projet global
4. Tester le workflow complet (Spring Batch + API REST + DB)
5. Déployer sur serveur / container si nécessaire

---

## Conseils pratiques
- Toujours **donner le contexte complet** à Claude (type de projet, technologies, contraintes)
- Si besoin, **scinder le code en plusieurs prompts** pour éviter les réponses tronquées
- Tu peux **enregistrer les prompts** pour automatiser ce workflow sur d’autres projets
- Pour projets complexes, **répéter Architect → Code Writer → Code Reviewer par module** plutôt qu’en une seule fois

