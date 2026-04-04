# Analyse comparative — Module de Facturation Tiers-Payant

> **Périmètre :** `src/main/webapp/app/entities/facturation` + backend `service/facturation/` + `web/rest/facturation/`
> **Référence :** Logiciels de gestion d'officine — contexte Afrique de l'Ouest francophone (LGPI, Winpharma, ISA-Pharma, Axiom Pharma, solutions CAMEG/CAME/CENAME, et standards ONPC/SYLOS)
> **Date :** 2026-04-02

---

## Sommaire

1. [Vue d'ensemble du module](#1-vue-densemble-du-module)
2. [Fonctionnalités implémentées](#2-fonctionnalités-implémentées)
3. [Référentiels comparatifs](#3-référentiels-comparatifs)
4. [Analyse comparative détaillée](#4-analyse-comparative-détaillée)
5. [Forces du module actuel](#5-forces-du-module-actuel)
6. [Lacunes identifiées](#6-lacunes-identifiées)
7. [Recommandations priorisées](#7-recommandations-priorisées)
8. [Synthèse par score](#8-synthèse-par-score)

---

## 1. Vue d'ensemble du module

### Architecture actuelle

Le module de facturation de Pharma-Smart est structuré en deux grandes zones fonctionnelles :

| Composant | Rôle |
|---|---|
| `FacturesComponent` | Liste, recherche, consultation et actions sur factures existantes |
| `EditionComponent` | Création de factures selon 6 modes d'édition |
| `FactureDetailComponent` | Vue détaillée d'une facture simple |
| `GroupeFactureDetailComponent` | Vue détaillée d'une facture groupée |
| `FneCertificateViewerComponent` | Visualisation du certificat FNE |
| `EditionFactureResource` (backend) | 15 endpoints REST couvrant CRUD + PDF + règlement |
| `FacturationServiceRegistry` (backend) | Dispatch vers les 6 services d'édition spécialisés |
| `CertificationFactureResource` (backend) | Certification numérique FNE |

### Modes d'édition supportés

```
ALL              → Toutes les ordonnances éligibles
SELECTION_BON    → Bons sélectionnés manuellement
TIERS_PAYANT     → Tiers-payants spécifiques
TYPE             → Filtre par catégorie de tiers-payant
GROUP            → Groupement par groupe de tiers-payant
SELECTED         → Sélection manuelle par groupement
```

---

## 2. Fonctionnalités implémentées

### 2.1 Gestion des factures

- [x] Création multi-mode (6 modes d'édition)
- [x] Listing paginé avec lazy loading
- [x] Filtrage multicritère (dates, tiers-payants, statut, numéro)
- [x] Vue détaillée par bon (décomposition jusqu'à la ligne produit)
- [x] Export PDF individuel et groupé
- [x] Suppression (factures non réglées uniquement)
- [x] Facturation provisoire (flag `factureProvisoire`)
- [x] Gestion des groupes de tiers-payants

### 2.2 Règlement

- [x] Route vers le module Règlement depuis la liste des factures
- [x] Endpoint `reglement/{id}` pour données de solde
- [x] Suivi du montant réglé / montant restant / statut (PAID / NOT_PAID / PARTIALLY_PAID)
- [x] Support de la remise forfaitaire (`remiseForfetaire`)

### 2.3 Certification numérique (FNE)

- [x] Certification d'une facture simple via `/api/certification-factures/certifier/{id}`
- [x] Certification groupée
- [x] Affichage du certificat FNE (iframe web / navigateur externe Tauri)
- [x] Stockage de la référence FNE sur la facture

### 2.4 Données métier

- [x] Montant brut / net / client / remise / taux de prise en charge
- [x] Ayants-droit sur les bons
- [x] Période de facturation (début/fin)
- [x] Code de génération pour lier les factures d'un même lot
- [x] Numéro de bon, numéro de facture, numéro de vente

---

## 3. Référentiels comparatifs

### 3.1 LGPI / Pharmagest (France)

Leader européen en gestion d'officine. Couvre la facturation CPAM/mutuelles, SESAM-Vitale, FSE (Feuille de Soins Électronique), Noémie, et PUI (Pharmacie à Usage Intérieur).

### 3.2 Winpharma (France/Afrique francophone)

Présent en Afrique de l'Ouest. Gère les tiers-payants CNAM, CNSS, mutuelles privées, avec module de relance et bordereaux PDF.

### 3.3 ISA-Pharma / SIGIPh (Sénégal, Côte d'Ivoire)

Logiciels locaux adaptés aux contextes USSD/CNAM/IPM. Gèrent les bons de prise en charge papier et la réconciliation manuelle.

### 3.4 Axiom Pharma / CG Pharma (Afrique subsaharienne)

Orientés gestion de stock + facturation tiers-payants CNSS/CNAMGS (Gabon), CNSS (Burkina), RAMU (Bénin). Accent sur la traçabilité des bons.

### 3.5 SYLOS / ONPC (Officines publiques et ONG)

Utilisés dans les centrales d'achat et pharmacies hospitalières. Facturation en lots, réconciliation CAMEG/CAME, export CSV/Excel réglementaire.

---

## 4. Analyse comparative détaillée

### 4.1 Création et édition de factures

| Critère | Pharma-Smart | LGPI/Pharmagest | Winpharma | ISA-Pharma/SIGIPh |
|---|:---:|:---:|:---:|:---:|
| Modes d'édition multiples | ✅ 6 modes | ✅ 4+ modes | ✅ 3 modes | ⚠️ 2 modes |
| Regroupement par groupe tiers-payant | ✅ | ✅ | ✅ | ❌ |
| Facturation par sélection de bons | ✅ | ✅ | ⚠️ partiel | ✅ |
| Facturation provisoire | ✅ | ✅ | ✅ | ❌ |
| Facturation périodique automatisée | ❌ | ✅ | ✅ | ⚠️ |
| Gestion des ordonnances renouvelables | ❌ | ✅ | ✅ | ⚠️ |
| Facturation multi-devises | ❌ | ⚠️ Europe | ❌ | ❌ |
| Avoir / note de crédit | ❌ | ✅ | ✅ | ❌ |

**Observation :** La diversité des modes d'édition de Pharma-Smart est un atout réel. L'absence d'avoir/note de crédit est la lacune la plus critique face aux logiciels de référence.

---

### 4.2 Gestion des tiers-payants

| Critère | Pharma-Smart | LGPI/Pharmagest | Winpharma | ISA-Pharma/SIGIPh |
|---|:---:|:---:|:---:|:---:|
| Tiers-payant simple / groupé | ✅ | ✅ | ✅ | ⚠️ |
| Catégories de tiers-payants | ✅ | ✅ | ✅ | ⚠️ |
| Taux de prise en charge configurables | ✅ (via PrixReference) | ✅ | ✅ | ✅ |
| Plafonds de remboursement | ❌ | ✅ | ✅ | ⚠️ |
| Ayants-droit (famille assurée) | ✅ | ✅ | ✅ | ✅ |
| Numéro d'assurance sur le bon | ⚠️ (non visible UI) | ✅ | ✅ | ✅ |
| Gestion des mutuelles complémentaires | ❌ | ✅ | ✅ | ❌ |
| Vérification d'éligibilité temps réel | ❌ | ✅ (Vitale) | ⚠️ | ❌ |
| Délai de règlement configurable | ❌ | ✅ | ✅ | ❌ |

**Observation :** La gestion des mutuelles complémentaires (double prise en charge) est absente. Dans le contexte CNAM/CNSS en Afrique de l'Ouest, ceci est fréquent (ex. : IPM + IPRES au Sénégal).

---

### 4.3 Suivi des paiements et règlements

| Critère | Pharma-Smart | LGPI/Pharmagest | Winpharma | Axiom/CG Pharma |
|---|:---:|:---:|:---:|:---:|
| Suivi statut facture (PAID/PARTIAL/UNPAID) | ✅ | ✅ | ✅ | ✅ |
| Règlement partiel | ✅ | ✅ | ✅ | ✅ |
| Échéancier de paiement | ❌ | ✅ | ✅ | ⚠️ |
| Relance automatique des impayés | ❌ | ✅ | ✅ | ❌ |
| Historique des versements | ⚠️ (module séparé) | ✅ intégré | ✅ intégré | ✅ |
| Lettrage des règlements | ❌ | ✅ | ✅ | ❌ |
| Réconciliation bancaire | ❌ | ✅ | ⚠️ | ❌ |
| Virement SEPA/local | ❌ | ✅ | ⚠️ | ❌ |
| Alerte créance ancienne | ❌ | ✅ | ✅ | ❌ |

**Observation :** La séparation du règlement dans un module externe (`/reglement`) brise le flux naturel de la facturation. Les logiciels de référence intègrent le suivi du règlement dans l'interface de facturation.

---

### 4.4 Documents et exports

| Critère | Pharma-Smart | LGPI/Pharmagest | Winpharma | SYLOS/ONPC |
|---|:---:|:---:|:---:|:---:|
| PDF facture simple | ✅ | ✅ | ✅ | ✅ |
| PDF facture groupée / bordereau | ✅ | ✅ | ✅ | ✅ |
| Export Excel / CSV | ❌ (bouton caché) | ✅ | ✅ | ✅ |
| Récapitulatif mensuel par tiers-payant | ❌ | ✅ | ✅ | ✅ |
| Lettre de rappel impayé | ❌ | ✅ | ✅ | ❌ |
| État de rapprochement | ❌ | ✅ | ⚠️ | ⚠️ |
| Bordereau de transmission | ⚠️ (PDF partiel) | ✅ | ✅ | ✅ |
| Format EDI/XML (échanges normalisés) | ❌ | ✅ (Noemie/B2) | ⚠️ | ❌ |
| Personnalisation des modèles PDF | ❌ | ✅ | ⚠️ | ❌ |

**Observation :** L'export Excel est présent dans le code (`hidden button`) mais non exposé. C'est une lacune critique car les tiers-payants exigent souvent un fichier tableur pour leur réconciliation.

---

### 4.5 Certification et conformité réglementaire

| Critère | Pharma-Smart | LGPI/Pharmagest | Winpharma | ISA-Pharma/SIGIPh |
|---|:---:|:---:|:---:|:---:|
| Certification numérique FNE | ✅ | ✅ (CPS/Vitale) | ⚠️ | ❌ |
| Signature électronique des factures | ❌ | ✅ | ⚠️ | ❌ |
| Archivage légal (durée réglementaire) | ❌ | ✅ | ⚠️ | ❌ |
| Numérotation séquentielle garantie | ⚠️ (partiel) | ✅ | ✅ | ⚠️ |
| Piste d'audit (qui a créé/modifié) | ❌ | ✅ | ✅ | ❌ |
| Conformité TVA / exonération médicaments | ⚠️ | ✅ | ✅ | ⚠️ |
| Déclaration fiscale automatisée | ❌ | ✅ | ⚠️ | ❌ |
| Connexion CNAM en ligne | ❌ | ✅ | ⚠️ | ⚠️ |

**Observation :** La certification FNE est un point différenciant fort pour le contexte local. L'absence de piste d'audit est une faiblesse de conformité pour les audits institutionnels.

---

### 4.6 Ergonomie et expérience utilisateur

| Critère | Pharma-Smart | LGPI/Pharmagest | Winpharma | ISA-Pharma/SIGIPh |
|---|:---:|:---:|:---:|:---:|
| Interface moderne (Web/SPA) | ✅ Angular 20 | ⚠️ Client lourd | ⚠️ Client lourd | ❌ Legacy |
| Responsive / tablette | ✅ | ❌ | ❌ | ❌ |
| Application desktop native (Tauri) | ✅ | ✅ (installateur) | ✅ | ❌ |
| Recherche rapide par numéro de bon | ✅ | ✅ | ✅ | ⚠️ |
| Raccourcis clavier | ⚠️ limités | ✅ | ✅ | ❌ |
| Actions en masse (sélection multiple) | ❌ | ✅ | ✅ | ❌ |
| Tableau de bord facturation | ❌ | ✅ | ✅ | ❌ |
| Notifications / alertes dans l'interface | ❌ | ✅ | ✅ | ❌ |
| Aperçu avant impression | ❌ | ✅ | ✅ | ❌ |

**Observation :** L'interface Angular 20 de Pharma-Smart surpasse les clients lourds legacy en modernité et responsive design. Cependant, l'absence d'actions en masse et de tableau de bord facturation pénalise la productivité pour les officines à fort volume.

---

### 4.7 Intégration avec les autres modules

| Critère | Pharma-Smart | LGPI/Pharmagest | Winpharma | Axiom/CG Pharma |
|---|:---:|:---:|:---:|:---:|
| Liaison ventes → facturation automatique | ✅ | ✅ | ✅ | ✅ |
| Liaison facturation → comptabilité | ❌ | ✅ | ✅ | ⚠️ |
| Liaison facturation → règlement | ⚠️ (lien URL) | ✅ intégré | ✅ intégré | ✅ |
| Liaison facturation → stock | ✅ (via bons) | ✅ | ✅ | ✅ |
| API ouverte pour intégration tierce | ⚠️ (REST existant) | ⚠️ | ❌ | ❌ |
| Module relance intégré | ❌ | ✅ | ✅ | ❌ |
| Connexion caisse / encaissement direct | ⚠️ | ✅ | ✅ | ⚠️ |

---

## 5. Forces du module actuel

### ✅ Points distinctifs forts

1. **6 modes d'édition** — Une granularité rare : la facturation par sélection de bons (`SELECTION_BON`) et par groupe de tiers-payants (`GROUP`) n'est pas proposée dans toutes les solutions du marché africain.

2. **Certification FNE intégrée** — L'intégration du FNE (Fichier National des Établissements) avec affichage du certificat directement dans l'interface est un avantage réglementaire concret.

3. **Architecture moderne** — Angular 20 standalone, signaux réactifs, lazy loading, API REST documentée. La base technique est solide et maintenable.

4. **Déploiement hybride** — Le module fonctionne identiquement en web et en application Tauri desktop, avec gestion spécifique de l'ouverture du certificat FNE selon le contexte.

5. **Facturation provisoire** — Le flag `factureProvisoire` permet un cycle de validation avant finalisation, absent dans plusieurs solutions locales.

6. **Détail jusqu'à la ligne produit** — L'arborescence Facture → Item → Ligne de vente → Produit (CIP, quantité, prix unitaire) est plus détaillée que ce que proposent ISA-Pharma ou CG Pharma.

7. **Taux de prise en charge configurables par produit** — Via le module `PrixReference`, chaque tiers-payant peut avoir un prix de référence spécifique par molécule, ce qui dépasse les simples taux globaux des solutions concurrentes.

---

## 6. Lacunes identifiées

### 🔴 Lacunes critiques (bloquantes en production avancée)

#### L6.1 — Avoir / Note de crédit
**Impact :** Toute correction de facture payée partiellement ou erronée exige un workflow d'avoir. Sans cela, les remboursements trop-perçus ou les erreurs de facturation ne peuvent être régularisés qu'en supprimant la facture (uniquement si `NOT_PAID`).

> *LGPI, Winpharma, Axiom : avoir automatique lié à la facture d'origine, avec numérotation séquentielle.*

#### L6.2 — Export Excel/CSV désactivé
**Impact :** Les services des mutuelles et CNSS exigent un fichier tableur pour leur réconciliation. Le bouton est présent dans le code mais masqué (`hidden`). Sa non-disponibilité force les pharmaciens à une ressaisie manuelle.

#### L6.3 — Actions en masse sur la liste des factures
**Impact :** Impossible de sélectionner plusieurs factures pour les certifier, exporter ou archiver en une seule opération. Sur des officines traitant 50+ tiers-payants, la productivité est fortement impactée.

#### L6.4 — Relance des impayés
**Impact :** Aucune alerte, aucun email/SMS automatique pour les créances en retard. Les factures `NOT_PAID` vieillissantes ne déclenchent aucune action proactive.

---

### 🟠 Lacunes importantes (limitantes à moyen terme)

#### L6.5 — Piste d'audit
Aucun enregistrement de l'opérateur ayant créé, modifié ou supprimé une facture. Requis pour les audits de la CNSS et des mutuelles.

#### L6.6 — Intégration comptabilité
Pas de génération d'écritures comptables (débit créances / crédit ventes). Les logiciels de référence produisent des exports comptables (SAGE, EBP) ou une API vers un module comptable.

#### L6.7 — Délai de règlement et échéancier
Aucune date d'échéance configurable par tiers-payant. Impossible de calculer les jours de retard ou d'appliquer des pénalités conventionnelles.

#### L6.8 — Numéro d'assurance non visible sur le bon
Le modèle `FactureItem` / `DossierFactureDto` ne remonte pas visiblement le numéro de police ou d'adhérent de l'assuré. Ce numéro est exigé sur les bordereaux transmis aux mutuelles.

#### L6.9 — Tableau de bord facturation
Aucun écran de synthèse avec : total facturé du mois, total réglé, taux de recouvrement, top 5 des tiers-payants impayés. Les logiciels de référence proposent ces KPIs en page d'accueil du module.

---

### 🟡 Lacunes mineures (améliorations souhaitables)

#### L6.10 — Aperçu avant impression du PDF
Aucun aperçu dans le navigateur avant le téléchargement. Une `<iframe>` Blob ou un `object` HTML suffirait.

#### L6.11 — Personnalisation du modèle PDF
L'en-tête de la facture (logo, coordonnées officine, pied de page légal) n'est pas configurable par l'interface. Nécessite un changement de code côté backend.

#### L6.12 — Gestion des mutuelles complémentaires
Un patient peut avoir une assurance principale (CNSS) + une complémentaire (IPM). La décomposition du reste-à-charge entre les deux tiers-payants n'est pas gérée.

#### L6.13 — Règlement intégré dans la vue facture
L'accès au règlement se fait via un lien routé (`/reglement`). L'intégrer en onglet ou en panneau latéral dans `FactureDetailComponent` réduirait les allers-retours.

#### L6.14 — Filtrage par montant
Impossible de filtrer les factures par tranche de montant ou par balance restante > X. Utile pour identifier les gros impayés.

---

## 7. Recommandations priorisées

### Phase 1 — Consolidation (court terme, 1-2 sprints)

| # | Action | Fichiers concernés | Effort |
|---|---|---|---|
| R1 | **Activer l'export Excel** — débloquer le bouton caché, implémenter `exportExcel()` côté service | `factures.component.ts`, `facture.service.ts` | Faible |
| R2 | **Piste d'audit** — ajouter `createdBy` / `updatedBy` / `deletedBy` sur `Facture` (Spring Data Auditing) | `Facture.java`, `FactureDto.java`, `facture.model.ts` | Moyen |
| R3 | **Afficher le numéro d'assuré** — remonter `numeroAssurance` / `matricule` depuis `DossierFactureDto` | `FactureItemDto.java`, `facture-detail.component.html` | Faible |
| R4 | **Aperçu PDF inline** — ouvrir le PDF dans un modal `<iframe>` avant téléchargement | `factures.component.ts` | Faible |

### Phase 2 — Complétude métier (moyen terme, 2-4 sprints)

| # | Action | Fichiers concernés | Effort |
|---|---|---|---|
| R5 | **Avoir / Note de crédit** — ajouter `ModeEditionEnum.AVOIR`, service `EditionAvoirService`, lien à la facture d'origine | `ModeEditionEnum.java`, nouveau service, nouvelles routes Angular | Élevé |
| R6 | **Tableau de bord facturation** — composant KPI : total facturé, recouvré, taux, top impayés | Nouveau composant `FacturationDashboardComponent` | Moyen |
| R7 | **Délai de règlement par tiers-payant** — ajouter `delaiReglement` sur `TiersPayant`, calcul d'échéance | `TiersPayant.java`, `FactureDto`, vue `factures.component` | Moyen |
| R8 | **Actions en masse** — checkboxes multi-sélection + boutons groupe (certifier, exporter, archiver) | `factures.component.ts/html` | Moyen |

### Phase 3 — Valeur ajoutée (long terme, 4+ sprints)

| # | Action | Description | Effort |
|---|---|---|---|
| R9 | **Relance automatique** | Job Spring Batch, email/SMS aux tiers-payants pour factures en retard > N jours | Élevé |
| R10 | **Export comptable** | Génération d'un fichier FEC ou export SAGE/EBP des écritures facturation | Élevé |
| R11 | **Double tiers-payant** | Gestion assuré principal + mutuelle complémentaire sur un même bon | Très élevé |
| R12 | **Personnalisation PDF** | Interface de configuration des modèles Thymeleaf (logo, pied de page) | Moyen |
| R13 | **Connexion CNAM/API externe** | Vérification éligibilité en ligne via l'API CNAM locale si disponible | Très élevé |

---

## 8. Synthèse par score

> Notation : 0 = absent, 1 = partiel, 2 = satisfaisant, 3 = excellent

| Domaine | Pharma-Smart | LGPI / Pharmagest | Winpharma | ISA-Pharma / SIGIPh |
|---|:---:|:---:|:---:|:---:|
| Création & modes d'édition | **2.5** | 3 | 2.5 | 1.5 |
| Gestion tiers-payants | **2** | 3 | 2.5 | 1.5 |
| Suivi règlement | **1.5** | 3 | 2.5 | 1.5 |
| Documents & exports | **1.5** | 3 | 2.5 | 1.5 |
| Certification / conformité | **2** | 2.5 | 1.5 | 0.5 |
| Ergonomie / UX | **2.5** | 1.5 | 1.5 | 0.5 |
| Intégration modules | **1.5** | 3 | 2.5 | 1.5 |
| **TOTAL /21** | **13.5** | **19** | **15.5** | **8.5** |

---

### Conclusion

Le module de facturation de Pharma-Smart est **fonctionnellement solide pour les cas d'usage courants** d'une officine africaine francophone : édition multi-mode, suivi des statuts, certification FNE, gestion des groupes de tiers-payants. Sa base technique Angular 20 le place nettement au-dessus des solutions legacy locales (ISA-Pharma, SIGIPh).

Les trois chantiers les plus impactants pour combler l'écart avec Winpharma sont, par ordre de retour sur investissement :

1. **Activer l'export Excel** (effort faible, valeur immédiate)
2. **Implémenter l'avoir / note de crédit** (effort élevé, mais fonctionnalité bloquante pour la conformité comptable)
3. **Intégrer le tableau de bord facturation avec KPIs** (effort moyen, valeur commerciale forte)
