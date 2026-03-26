# Analyse UX/Fonctionnelle — Dashboards & Suggestions Commande
## Pharma-Smart vs Logiciels de référence (Winpharma · Périscopie · Pharmagest)
## Date : Mars 2026

---

## 1. Deux dashboards distincts — Pertinence ?

### Ce qu'on a aujourd'hui

| Dashboard | Périmètre | Données affichées |
|---|---|---|
| **commande-dashboard** | Flux opérationnel | Commandes en cours, à réceptionner, PharmaML pending |
| **semois-dashboard** | Santé du stock analytique | Ruptures, sous seuil, OK, surstock, répartition par classe |

### Verdict : Utiles mais mal organisés

Les deux dashboards répondent à deux questions **légitimes et complémentaires** :
- "Où en sont mes commandes du moment ?" → opérationnel, court terme
- "Mon stock est-il en bonne santé ?" → stratégique, moyen terme

Problèmes identifiés :
1. Fragmentation de la décision : le pharmacien ne voit jamais les deux contextes en même temps.
2. Aucun lien actionnable entre eux : une rupture SEMOIS n'est pas cliquable pour déclencher une commande.
3. Sidebar surchargée : 10 entrées dont 2 dashboards + 2 modules suggestion.
4. Le commande-dashboard est en lecture seule : aucune quick action.
5. Le SEMOIS dashboard n'est jamais la vue d'entrée : découverte difficile.

### Ce que font les logiciels de référence

**Winpharma** : Un seul écran "Approvisionnement" avec zones côte à côte (météo stock + commandes en cours).
Les indicateurs sont directement cliquables vers la liste filtrée.

**Périscopie** : KPI central unique (taux de rupture %) + valeur stock à risque + mini-graphe tendance 7 jours.

**Pharmagest iSoft** : Dashboard unifié "Pilotage réappro" avec bandeau de santé toujours visible.

### Recommandation
Fusionner les deux dashboards en une seule vue "Tableau de bord approvisionnement" avec :
1. Bandeau santé stock (compact, toujours visible)
2. Alertes urgentes (top 5 ruptures cliquables)
3. Flux commandes (en cours, à réceptionner, PharmaML)

---

## 2. Deux modules suggestion — Séparation justifiée ?

### Ce qu'on a aujourd'hui

| Module | Mécanisme | Capacités | Lacune |
|---|---|---|---|
| **suggestion-home** | Split-panel fournisseur/produits + AG Grid | Commander, Sélection, Export PDF/CSV, PharmaML dispo, Comparaison prix, Valider, Rejeter, Fusionner | — |
| **semois-suggestions** | Table paginée simple | Affichage + filtres + aide drawer | **Impossible de commander** |

### La lacune critique 🔴

**Les suggestions SEMOIS ne peuvent pas être commandées.**
Le pharmacien voit "URGENT — Doliprane 1000mg — Qté à commander: 50" mais ne peut rien faire.
Il doit retourner dans suggestion-home, chercher le fournisseur, ajouter manuellement le produit : 5 étapes inutiles.

### Ce que font les logiciels de référence

**Winpharma** : Bouton "Créer bon de commande SEMOIS" — génère les bons groupés par fournisseur en 1 clic.

**Périscopie** : Checkbox multi-sélection + "Ajouter au panier" → montant estimé en temps réel.

**Pharmagest iSoft** : Workflow en 3 étapes dans le même écran (sélectionner → ajuster → commander/PharmaML).

### Les deux menus doivent-ils rester séparés ?

Non. La séparation est artificielle et uniquement technique. Du point de vue pharmacien, c'est une seule activité.
Recommandation : un seul onglet "Suggestions" avec filtre Source (Toutes | SEMOIS | Manuel).

---

## 3. Ce qui est inutile / à supprimer

### Dans semois-suggestions
- Le bouton "Guide" et le drawer d'aide (400 lignes, non lus) → remplacer par tooltips inline
- La colonne "Fournisseur" en permanence → afficher uniquement si filtre "tous fournisseurs"
- La colonne "Stock Obj." en unités brutes → condenser avec couverture cible en mois (Axe 7)

### Dans la sidebar
- 2 onglets dashboard → à fusionner
- 2 onglets suggestion → à fusionner avec filtre source
- Résultat cible : 6 entrées max au lieu de 10

---

## 4. Quick actions manquantes sur le dashboard

| Action | Winpharma | Périscopie | Pharmagest | Pharma-Smart actuel |
|---|---|---|---|---|
| Commander les urgents | ✅ 1 clic | ✅ | ✅ | ❌ absent |
| Nouvelle commande manuelle | ✅ | ✅ | ✅ | ❌ absent |
| Recalculer SEMOIS | — | — | ✅ | ✅ (caché dans suggestion-home) |
| Vérifier dispo PharmaML | ✅ | ❌ | ✅ | ✅ (caché dans suggestion-home) |
| Budget consommé / alerte | ✅ | ✅ | ✅ | ✅ (suggestion-home seulement) |
| Indicateur fraîcheur SEMOIS | — | — | ✅ | ✅ (suggestion-home seulement) |

### Recommandations pour le dashboard unifié
1. [Commander les urgents] → génère suggestions pour ruptures A+/A
2. [Nouvelle commande] → shortcut vers commande vide
3. [Recalculer SEMOIS] → avec indicateur fraîcheur visible
4. [Vérifier dispo multi-grossistes] → PharmaML multi
5. Barre budget → progression visuelle

---

## 5. Priorisation

| Priorité | Fonctionnalité | Impact | Effort |
|---|---|---|---|
| 🔴 P0 | Rendre commandables les suggestions SEMOIS | Majeur — flux bloquant | Moyen |
| 🔴 P0 | Quick action "Commander les urgents" sur dashboard | Majeur | Faible |
| 🟠 P1 | Fusion des deux dashboards | Fort | Moyen |
| 🟠 P1 | Indicateur fraîcheur SEMOIS visible partout | Fort | Faible |
| 🟡 P2 | Fusion suggestion-home + semois-suggestions | Moyen | Fort |
| 🟡 P2 | Suppression drawer d'aide → tooltips inline | Confort | Faible |
| 🟢 P3 | Simplification sidebar (6 entrées max) | Confort | Faible |

---

## 6. Synthèse

L'architecture actuelle souffre d'un problème fondamental de cohérence workflow :
les outils d'analyse (SEMOIS) et les outils d'action (suggestion-home, commandes) sont cloisonnés.

Point le plus urgent : la **liste SEMOIS non commandable** — un écran qui montre un problème
sans donner les moyens de le résoudre, contre-productif et source de méfiance envers l'outil.

Les logiciels de référence ont tous résolu ce problème en réunissant analyse et action
dans le même écran, réduisant à 1-2 clics le chemin entre "je vois un problème" et "j'ai commandé".

