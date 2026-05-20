# Analyse Comparative — Module Rapports Officine

## 1. État des lieux : ce qui est déjà implémenté dans Pharma-Smart

### 1.1 Module `entities/reports` (17 rapports)

| Rapport | Catégorie | Données clés | Export |
|---|---|---|---|
| **Dashboard CA** | Ventes | CA temps réel (jour/semaine/mois/année), évolution %, marge, répartition paiements | PDF, Excel, CSV |
| **Sales Summary** | Ventes | CA journalier par type (VO/VNO/Dépôt), panier moyen | — |
| **Comparative Analysis** | Ventes | Comparaison CA période N vs N-1 (mensuel/trimestriel/annuel) | PDF |
| **Sales Forecast** | Ventes | Prévisions CA sur 3/6/12 mois (régression linéaire, MA, saisonnalité) | — |
| **Top Products** | Ventes | Top N produits par CA et par quantité vendue | — |
| **Market Basket** | Ventes | Associations produits (support, confiance, lift) | — |
| **Recap Produit Vendu** | Stock/Ventes | Produits vendus vs invendus, marges, seuils de stock | PDF, Excel, CSV |
| **ABC Pareto** | Stock | Classification A+/A/B/C/D par contribution CA (règle 80/20) | PDF |
| **Stock Alerts** | Stock | Ruptures, alertes, produits proches péremption | PDF |
| **Stock Valuation** | Stock | Valorisation stock par produit/famille/rayon | PDF |
| **Stock Rotation** | Stock | Taux de rotation ABC (Z-score statistique) | PDF |
| **Profitability Analysis** | Marges | Marge brute % par produit, identification produits à faible marge | PDF |
| **Customer Segmentation (RFM)** | Clients | Segmentation Champions/Fidèles/À risque/Inactifs | — |
| **Market Basket** | Clients/Produits | Cross-selling et ventes croisées | — |
| **Supplier Performance** | Fournisseurs | Score performance, délai livraison, taux de service | PDF |
| **Partners Reports** | Navigation | Conteneur (Segmentation + Performance fournisseurs) | — |
| **Sales Reports** | Navigation | Conteneur (7 rapports ventes) | — |

### 1.2 Module `entities/mvt-caisse` (gestion de caisse)

Le module mvt-caisse couvre de façon très complète la gestion et le reporting de la caisse physique :

| Composant | Ce qu'il couvre | Export |
|---|---|---|
| **Visualisation mouvements** | Journal complet des transactions (type, référence, date, montant, mode paiement, opérateur). Filtres : plage date/heure, types, modes, opérateur. Totaux par type et par mode. Impression reçu ESC/POS ou web. | PDF |
| **Balance Caisse** | Totaux TTC, HT, TVA, remises, net, achats, marge, ratio V/A par type de vente. Ventilation par mode de paiement. Mouvements entrées/sorties. Filtre caisses clôturées uniquement. | PDF |
| **Taxe Report (TVA)** | TVA par code ou par jour : montant HT, TVA, TTC. Vue tableau ou graphique donut. | PDF |
| **Tableau Pharmacien** | Ventes (espèces, crédit, remises, net, clients) + achats par groupe fournisseur. Ratios V/A et A/V. Vue journalière ou mensuelle. Graphiques barres. | PDF, Excel |
| **Gestion Caisse** | Sessions de caisse par opérateur : fonds de caisse, recettes, total théorique, mobile, écart (coloré rouge/vert). Statuts : OPEN / CLOSED / VALIDATED / PENDING. | *(bouton print non implémenté)* |
| **Form Transaction** | Saisie manuelle d'une entrée/sortie de caisse avec type, mode, montant, commentaire, date. Impression reçu immédiate. | Reçu |

**Complément — `entities/ticketZ/recapitulatif-caisse` :**
- Rapport EOD (clôture journalière) imprimable avec menu d'export — **implémenté**
- Billetage (comptage physique par dénomination) — **implémenté**

**Conclusion : le périmètre caisse est entièrement couvert.** Aucun gap résiduel identifié.

### 1.3 Module `features/facturation` (couverture tiers payant)

Le module facturation implémente un ensemble complet de fonctionnalités TP qui couvrent une grande partie du suivi financier tiers payant :

| Composant | Ce qu'il couvre | Export |
|---|---|---|
| **Rapprochement** | Par organisme : totalFacture, totalRegle, écartTotal, tauxRecouvrement, lignes en retard. Par facture : statut, écart, règlements expandables. KPI bannière globale. Saisie de règlement inline. | PDF, Excel |
| **Récapitulatif mensuel** | Par organisme × mois : soldePrecedent, totalFacture, totalRegle, soldeActuel, soldeCumule, nombreFactures, nombreImpayées. Détail factures par TP. | PDF, Excel |
| **Historique règlements** | Par organisme : codeFacture, modeRèglement, montantAttendu, montantVersé, créé par, date. Expandable vers sous-règlements. Impression reçu (ESC/POS ou web). | PDF, Excel |
| **Avoirs** | Cycle complet : création → émission → imputation → annulation. Modes global ou ligne. Validation montants. KPIs par statut. | PDF (unitaire + liste), Excel |
| **Planification** | Automatisation de l'édition de factures (périodicité hebdo/mensuelle/quinzaine), gestion FNE, historique d'exécutions, déclenchement manuel. | — |
| **Édition factures** | Génération batch par mode (tous, type, groupe, TP, sélection bons). Sélection paginated, export PDF batch automatique. | PDF batch |

**Ce que le module facturation NE couvre PAS** (et qui reste un gap pour les rapports) :
- Vieillissement des créances par tranche d'âge (0-30j / 30-60j / 60-90j / +90j)
- DSO (Days Sales Outstanding) calculé et comparé par organisme
- Concentration du risque payers (% du CA par payer, top 3)
- Vue analytique TP croisée avec les types de ventes (VO/VNO)

### 1.4 Points forts du périmètre existant

- **Analyse avancée** : RFM, ABC Pareto, Market Basket, Sales Forecast — niveau analytique supérieur à la moyenne des logiciels africains
- **TP complet** : Cycle facturation → règlement → rapprochement → avoir entièrement opérationnel
- **Multi-export** : PDF, Excel, CSV sur tous les modules clés
- **Temps réel** : Dashboard CA + KPI bannières factures en live
- **Analyse prédictive** : Prévisions avec 3 méthodes statistiques

---

## 2. Analyse comparative — Logiciels du marché

### 2.1 Logiciels français (Pharmagest, Winpharma, LGPI, Caducée)

**Pharmagest id. analytics** propose :
- Tableau de bord de gestion (CA, marge, charges, EBE)
- Rapport de substitution générique/biosimilaire par molécule
- Gestion tiers payant : tableau de rejet avec codes motifs + vieillissement créances
- Registre stupéfiants électronique
- Comptabilité analytique par segment (VO/VNO/Para/Services)
- Pilotage BFR (besoin en fonds de roulement)

**Winpharma** ajoute :
- Tableau de bord de pilotage mensuel (12 mois glissants)
- Gestion des remises arrières fournisseurs
- Suivi entretiens pharmaceutiques et actes de prévention
- Comparaison aux benchmarks sectoriels

**LGPI Global Care / Caducée** :
- Suivi lot/rappel de lot (alertes autorités sanitaires nationales)
- Rapport de traçabilité sérialisation (DataMatrix / codes 2D)
- Journal d'override des alertes interactions médicamenteuses

### 2.2 Logiciels africains (Smart Rx, AS Pharm, Pharma 4000)

**Smart Rx Perf (Afrique francophone)** :
- Suivi CNAM/INAM/AMU par payer avec taux de rejet
- Rapport de vieillissement créances tiers payant (DSO par assureur)
- Rapport encaissement par caisse/opérateur
- Analyse concentration payers (risque dépendance institutionnelle)

**AS Pharm** :
- Dashboard payer (AMO vs AMC, entreprises vs mutuelles communautaires)
- Rapport adhérence patient maladies chroniques (diabète, HTA, paludisme)
- Rapport saisonnalité (antipaludéens, moustiquaires — pic saison des pluies)

### 2.3 Logiciels internationaux (McKesson, EnlivenHealth)

**McKesson Enterprise** :
- Rapport productivité opérateur (CA/ETP, transactions/heure, panier moyen par vendeur)
- Rapport shrinkage (pertes inventaire connu vs inconnu)
- Cash flow et trésorerie à 13 semaines glissantes
- Rapport primo-prescription vs. renouvellement (primo vs. refill)

---

## 3. Gaps identifiés — Ce qui manque dans Pharma-Smart

### 3.1 Lacunes CRITIQUES (impact réglementaire ou financier direct)

#### GAP-001 — Registre Stupéfiants / Psychotropes
- **Ce qui manque** : Registre électronique chronologique par molécule (entrées/sorties/solde courant), PV de destruction, export inspection conforme aux autorités sanitaires nationales (DPM, AIRP, DPML selon le pays)
- **Impact** : Obligation légale — sanction pénale possible en cas de défaillance. Registre papier = risque d'erreur et perte de temps
- **Référence marché** : Pharmagest, LGPI, Winpharma ont tous un module dédié

#### GAP-002 — Vieillissement Créances TP & DSO par Organisme
- **Contexte** : Le module rapprochement couvre la réconciliation ligne à ligne et le récapitulatif couvre le bilan mensuel. Ce qui manque est la vue d'**analyse du risque créances** :
  - Tranches d'âge des impayés : 0-30j / 30-60j / 60-90j / +90j par organisme
  - DSO (délai moyen encaissement) calculé et historisé par organisme
  - Score de fiabilité payer (tendance retard chronique vs. occasionnel)
- **Impact** : Identifier les organismes à risque avant que les impayés deviennent irrecouvrables
- **Différence avec l'existant** : Le rapprochement montre les lignes individuelles en retard ; ce rapport agrège en analyse de risque financier par payer

#### GAP-003 — Marge Brute par Segment / Famille de Produit
- **Ce qui manque** : CA, coût d'achat et marge brute (€ et %) selon deux axes complémentaires :
  - **Par segment de vente** : VO (remboursables), VNO (OTC), parapharmacologie, dispositifs médicaux, services
  - **Par famille de produit** : agrégation de la marge par famille (antibiotiques, antalgiques, antipaludéens, dermatologie…) avec tri par marge %, lien vers Profitability Analysis
- **Périmètre réaliste** : L'application ne gérant pas les charges fixes, le rapport s'arrête à la **marge brute**. L'EBE n'est pas calculable automatiquement.
- **Distinction** : Le rapport Profitability Analysis existant est **produit par produit** ; ce rapport agrège à deux niveaux supérieurs (segment et famille) pour la vue de pilotage
- **Valeur** : Identifier quels segments et quelles familles tirent la marge, et orienter les décisions d'assortiment

#### ~~GAP-004~~ — Réconciliation Caisse → **COUVERT**
- Le module `mvt-caisse` implémente : balance caisse, journal mouvements, rapport TVA, tableau pharmacien, billetage (comptage physique par dénomination), gestion des sessions avec écart coloré.
- Le rapport EOD (clôture journalière imprimable) est implémenté dans `entities/ticketZ/recapitulatif-caisse/recapitulatif-caisse.component` avec export menu.
- **Aucun gap résiduel identifié sur ce périmètre.**

### 3.2 Lacunes HAUTES (pilotage financier)

#### GAP-005 — Cash Flow & BFR (Besoin en Fonds de Roulement)
- **Ce qui manque** : Calcul mensuel BFR = stock + créances TP (soldeCumule récapitulatif) - dettes fournisseurs. Cash conversion cycle (DIO + DSO - DPO). Projection trésorerie
- **Note** : Les données existent dans le système (stock valorisé + solde cumulé TP + dettes fournisseurs) — il faut les agréger dans une vue financière unifiée
- **Impact** : Une officine peut avoir un CA fort et une trésorerie fragile — vue essentielle pour le banquier

#### GAP-006 — Budget vs. Réalisé
- **Ce qui manque** : Saisie d'un budget annuel mensuel (CA cible, marge cible, charges), comparaison mensuelle réalisé vs. budget, alerte sur écarts matériels
- **Distinction** : Le Comparative Analysis compare N vs N-1 ; ce rapport compare réalisé vs. **objectif défini par le titulaire**

#### ~~GAP-007~~ — Remises Arrières Fournisseurs (RFA) → **COUVERT**
- Le module `features/finances/remises-rfa` implémente : suivi des paliers RFA par fournisseur (CA commandé N vs. palier cible, % atteint avec barre de progression colorée, RFA estimée vs. RFA reçue), suivi des avoirs fournisseurs reçus (statut RECU/EN_ATTENTE).
- **Enhancements souhaitables** (non bloquants) : sélection de période (actuellement figé sur l'année calendaire), export Excel/PDF, comparaison N vs. N-1, filtrage par fournisseur.

### 3.3 Lacunes MOYENNES (clinique et opérationnel)

#### GAP-008 — Adhérence Patient Maladies Chroniques
- **Ce qui manque** : Patients avec dernière date de délivrance vs. date de prochain renouvellement attendu, taux PDC (Proportion Days Covered), liste patients en retard de refill exportable pour relance
- **Valeur métier africain** : Suivi diabète, HTA, paludisme chronique — fidélisation patient + qualité des soins

#### GAP-009 — Indicateurs Qualité de Dispensation
- **Ce qui manque** : Tableau de bord interne des indicateurs qualité : taux de substitution générique par molécule, taux de refus patient, ordonnances refusées (contre-indication, interaction), entretiens pharmaceutiques réalisés vs. cible
- **Applicable** : Indicateurs définis localement par le titulaire ou imposés par l'autorité sanitaire nationale — aucune dépendance à un système externe

#### GAP-010 — Substitution Générique (détail opérationnel)
- **Ce qui manque** : Taux de substitution par molécule, par prescripteur, par patient ; taux de refus patient ; prescriptions "non-substituable" par prescripteur
- **Relation avec GAP-009** : Le rapport qualité (GAP-009) donne le taux global ; ce rapport donne le détail actionnable par molécule et par prescripteur pour cibler les efforts

#### GAP-011 — Concentration Payers / Risque Institutionnel
- **Ce qui manque** : % du CA total par payer (espèces + chaque organisme TP), ratio de concentration top 3 payers, historique fiabilité payer (DSO, taux rejet tendance)
- **Contexte africain** : Certaines pharmacies dépendent à 40-60% d'un seul payer institutionnel (CNAM, grande mutuelle d'entreprise, programme ONG) — risque de trésorerie critique si ce payer tarde ou est perdu
- **Distinction avec GAP-002** : GAP-002 analyse les retards ligne à ligne ; GAP-011 analyse la concentration du risque au niveau stratégique

#### GAP-012 — Rappels de Lot / Pharmacovigilance
- **Ce qui manque** : Log des alertes rappel de lot reçues des autorités (DPM, AIRP), stock affecté identifié et mis en quarantaine, patients ayant reçu les lots concernés, statut retour/destruction
- **Valeur** : Traçabilité réglementaire et protection des patients

#### GAP-013 — Shrinkage & Pertes Inventaire
- **Ce qui manque** : Écart entre stock théorique (achats - ventes) et stock physique post-inventaire, classification des pertes (péremption réalisée, vol suspecté, erreur dispensation), trend en % du CA
- **Complément** : Le rapport Stock Alerts signale les produits proches péremption ; ce rapport documente les pertes **constatées** après inventaire

### 3.4 Lacunes BASSES (stratégique)

#### GAP-014 — Recrutement & Churn Patients
- **Ce qui manque** : Nouveaux patients du mois (primo-visite), taux de churn (actifs il y a 6 mois, absents depuis 3 mois), taux de fidélisation (visite unique → récurrent)
- **Distinction** : Le RFM existant segmente les patients actifs ; ce rapport analyse le **flux net** entrée/sortie de patientèle

#### GAP-015 — Calendrier Saisonnier & Plan d'Approvisionnement
- **Ce qui manque** : Heatmap demande par famille × mois sur 2-3 ans, indice saisonnier par produit, recommandation de dates de constitution de stock avant-saison
- **Contexte africain** : Antipaludéens, moustiquaires, réhydratation — pics de demande très marqués selon saison des pluies
- **Valeur** : Transforme les prévisions existantes (Sales Forecast) en planning d'achat actionnable

#### GAP-016 — Productivité du Personnel par Opérateur
- **Ce qui manque** : CA généré par opérateur, panier moyen par vendeur, transactions/heure, détection d'écarts de performance entre caisses
- **Dépendance** : Nécessite que les ventes soient liées à un opérateur identifié dans le système

---

## 4. Benchmark synthétique — Couverture fonctionnelle

| Domaine | Pharma-Smart | Pharmagest | Winpharma | Smart Rx |
|---|:---:|:---:|:---:|:---:|
| CA & Ventes dashboard | ✅ | ✅ | ✅ | ✅ |
| Comparatif périodes | ✅ | ✅ | ✅ | ✅ |
| Prévisions ventes | ✅ | 🔶 | 🔶 | ❌ |
| ABC Pareto produits | ✅ | ✅ | ✅ | 🔶 |
| Segmentation RFM clients | ✅ | 🔶 | ❌ | ❌ |
| Market basket | ✅ | ❌ | ❌ | ❌ |
| Performance fournisseurs | ✅ | 🔶 | ✅ | ❌ |
| Réconciliation TP (rapprochement) | ✅ | ✅ | ✅ | ✅ |
| Récapitulatif mensuel TP | ✅ | ✅ | ✅ | ✅ |
| Cycle avoirs / ajustements TP | ✅ | ✅ | ✅ | 🔶 |
| Vieillissement créances / DSO | ❌ | ✅ | ✅ | ✅ |
| Registre stupéfiants | ❌ | ✅ | ✅ | 🔶 |
| P&L analytique par segment | ❌ | ✅ | ✅ | ❌ |
| Cash Flow & BFR | ❌ | 🔶 | ✅ | ❌ |
| Budget vs. réalisé | ❌ | 🔶 | ✅ | ❌ |
| Réconciliation caisse (end-of-day) | ✅ | ✅ | ✅ | ✅ |
| Adhérence patients chroniques | ❌ | 🔶 | 🔶 | ✅ |
| Indicateurs qualité dispensation | ❌ | 🔶 | 🔶 | ❌ |
| Substitution générique détail | ❌ | ✅ | ✅ | 🔶 |
| Concentration payers | ❌ | ❌ | ❌ | ✅ |
| Rappels de lot / pharmacovig. | ❌ | ✅ | 🔶 | ❌ |
| Productivité personnel | ❌ | 🔶 | ✅ | 🔶 |
| Shrinkage / pertes inventaire | ❌ | 🔶 | 🔶 | ❌ |
| Calendrier saisonnier | ❌ | ❌ | ❌ | 🔶 |

✅ Présent et complet | 🔶 Partiel/basique | ❌ Absent

**Conclusion** : Pharma-Smart est **supérieur** à la concurrence sur l'analyse avancée (RFM, Market Basket, Forecast), possède un **module TP complet** (rapprochement, récapitulatif, avoirs) et un **module caisse très avancé** (balance, TVA, tableau pharmacien). Les lacunes principales restantes sont : registre stupéfiants (obligation légale), analyse du risque créances (vieillissement/DSO), et pilotage financier global (P&L segment, BFR, budget).

---

## 5. Proposition de priorisation

### Niveau 1 — Critique (3-6 mois) : Réglementaire + complément TP

1. **Registre Stupéfiants** (GAP-001) — obligation légale, risque pénal
2. **Vieillissement Créances & DSO** (GAP-002) — complément naturel du rapprochement existant

### Niveau 2 — Haute priorité (6-12 mois) : Pilotage financier

4. **P&L Analytique par Segment** (GAP-003) — vue VO/VNO/Para/Services
5. **Cash Flow & BFR** (GAP-005) — agréger stock valorisé + solde TP + dettes fournisseurs
6. **Budget vs. Réalisé** (GAP-006) — objectifs vs. réalisé mensuel
7. **Concentration Payers** (GAP-011) — priorité africaine : risque institutionnel

### Niveau 3 — Moyen terme (12-18 mois) : Clinique + Opérationnel

8. **Adhérence Patients Chroniques** (GAP-008)
9. **Indicateurs Qualité Dispensation** (GAP-009)
10. **Substitution Générique Détail** (GAP-010)
11. **Rappels de Lot / Pharmacovigilance** (GAP-012)
12. **Shrinkage & Pertes Inventaire** (GAP-013)

### Niveau 4 — Long terme (18 mois+) : Stratégique

14. **Recrutement & Churn Patients** (GAP-014)
15. **Calendrier Saisonnier & Approvisionnement** (GAP-015)
16. **Productivité Personnel** (GAP-016) — si les ventes sont liées aux opérateurs
