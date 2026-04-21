# Analyse comparative — Module comptabilité dans les logiciels de gestion d'officine

> Contexte : évaluation pour l'intégration d'un module comptable dans **Pharma-Smart**
> (Spring Boot + Angular, pharmacies Afrique de l'Ouest)

---

## 1. Panorama des solutions de référence

### 1.1 LGPI / Pharmagest Interactive (France)

| Critère | Détail |
|---|---|
| Part de marché | ~9 000 pharmacies équipées (leader France) |
| Module comptable | **Non intégré** — tableaux de bord financiers uniquement |
| Intégration externe | Export vers SAGE, Cegid (connexion manuelle ou semi-auto) |
| Fonctions financières natives | Suivi du CA, calcul des marges, archivage fiscal cloud (HDS) |
| Lien stock → comptabilité | Indirect (rapports de gestion, pas d'écritures automatiques) |

**Résumé :** Pharmagest se concentre sur l'opérationnel métier. La comptabilité est déléguée à un expert-comptable via exports périodiques. Le logiciel génère des **tableaux de bord décisionnels** mais ne produit pas de plan comptable ni de bilan.

---

### 1.2 Winpharma / Datascan (France — fort déploiement Afrique de l'Ouest)

| Critère | Détail |
|---|---|
| Déploiement Afrique | 77,4 % des pharmacies de Bamako (Mali) — source : étude universitaire |
| Module comptable | **Partiel** — suivi des encaissements, comptes clients/fournisseurs |
| Intégration externe | Paramétrable selon l'installation locale |
| Fonctions financières natives | Rapprochement caisse, suivi des dettes fournisseurs, facturation |
| Lien stock → comptabilité | Valorisation stock disponible, pas d'écritures grand-livre automatiques |

**Résumé :** Winpharma intègre les briques financières de base (trésorerie, comptes tiers) sans offrir un vrai module de comptabilité générale (grand-livre, bilan, journaux officiels).

---

### 1.3 SAGE (France / International)

| Produit | Usage pharmacy | Approche |
|---|---|---|
| **SAGE 50** | PME / petites officines | Comptabilité générale complète (GL, AR/AP, paie) — **sans lien métier pharmacy** |
| **SAGE X3** | Industrie pharmaceutique | ERP complet avec comptabilité analytique, lot, coût de revient — orienté fabrication |
| **SAGE Intacct** | Biotech / Life Sciences | Cloud, multi-entité — pas spécifique officine |

**Résumé :** SAGE est le **système de destination** (système comptable de référence), pas un logiciel de pharmacie. Les officines sous SAGE maintiennent deux systèmes distincts avec des exports/réconciliations périodiques.

---

### 1.4 Cegid (France)

- ERP mid-market pour retail et professions de santé
- Intégration possible avec logiciels de caisse pharmacy via API
- Gestion complète : grand-livre, TVA, liasse fiscale, analytique
- Utilisé par les groupements de pharmacies et chaînes (pas les indépendants)

---

### 1.5 PioneerRx / QS1 (USA — référence internationale)

| Critère | Détail |
|---|---|
| Approche | **Module comptable intégré** dans le LGO |
| Fonctions | GL, AR/AP, réconciliation caisse, suivi DIR fees, conformité 340B |
| Lien stock → comptabilité | Automatique : dispensation → COGS → journal en temps réel |
| Lien ventes → comptabilité | Encaissement → écriture GL instantanée |
| Gestion tiers payant | Créances assurances → vieillissement AR automatique |

**Résumé :** Référence du marché américain. Démontre qu'un LGO intégré avec comptabilité native est viable et préféré par les pharmacies indépendantes.

---

### 1.6 AS Pharm (Afrique — SaaS panafricain)

| Critère | Détail |
|---|---|
| Zones couvertes | Sénégal, Côte d'Ivoire, Mali, expansion régionale |
| Module comptable | **Intégré** dans la plateforme SaaS |
| Fonctions | Ventes, facturation, stock, comptabilité, analytique — système unique |
| Modèle | Cloud / SaaS, abonnement mensuel |

**Résumé :** Solution africaine la plus aboutie. L'intégration comptable native est un **argument commercial différenciant** sur le marché africain.

---

## 2. Patterns d'architecture identifiés

### Pattern A — Comptabilité intégrée (≈ 75 % du marché officinal)

```
LGO (ventes + stock + caisse)
        │
        ▼ écritures automatiques
Module comptable intégré
(grand-livre, journaux, bilan)
```

**Avantages :**
- Données en temps réel, zéro resaisie
- Cohérence stock ↔ valorisation ↔ comptabilité
- Piste d'audit complète dans un seul système
- Coût total inférieur (une seule licence)

**Adopté par :** PioneerRx, AS Pharm, Liberty Software, solutions africaines

---

### Pattern B — Intégration avec système externe (≈ 25 % — grandes structures)

```
LGO (opérationnel)
        │
        ▼ export API / EDI / batch
Logiciel comptable spécialisé
(SAGE, Cegid, Oracle, QuickBooks)
```

**Avantages :**
- Comptabilité de niveau enterprise (multi-sociétés, multi-devises)
- Flexibilité de choix de l'expert-comptable

**Inconvénients :**
- Deux systèmes à maintenir et à synchroniser
- Risque de désynchronisation stock / comptabilité
- Coût élevé (deux abonnements, intégration technique)

**Adopté par :** Chaînes de pharmacies, groupements, structures multi-sites

---

## 3. Fonctions comptables couvertes selon les solutions

| Fonction | LGPI | Winpharma | PioneerRx | AS Pharm |
|---|:---:|:---:|:---:|:---:|
| Grand-livre (GL) | ✗ | ✗ | ✓ | ✓ |
| Journaux (achats, ventes, caisse) | ✗ | Partiel | ✓ | ✓ |
| Comptes fournisseurs (AP) | ✗ | ✓ | ✓ | ✓ |
| Comptes clients / tiers payant (AR) | ✗ | ✓ | ✓ | ✓ |
| Réconciliation caisse | ✓ | ✓ | ✓ | ✓ |
| Valorisation stock (COGS) | Partiel | Partiel | ✓ | ✓ |
| TVA / déclarations fiscales | ✗ | ✗ | ✓ | ✓ |
| Bilan / compte de résultat | ✗ | ✗ | ✓ | ✓ |
| Analytique (centre de coût) | ✗ | ✗ | ✓ | Partiel |
| Liasse fiscale | ✗ | ✗ | ✗ | ✗ |

---

## 4. Lien métier pharmacy → comptabilité

Les logiciels intégrés génèrent automatiquement des écritures comptables à partir des événements métier :

| Événement métier | Écriture comptable générée |
|---|---|
| Vente au comptant | Débit caisse / Crédit ventes |
| Vente tiers payant | Débit créances AM/mutuelles / Crédit ventes |
| Réception commande fournisseur | Débit stock / Crédit fournisseur |
| Retour fournisseur (avoir) | Débit fournisseur / Crédit stock |
| Bon d'entrée diverse (BED) | Débit stock / Crédit compte de régularisation |
| Ajustement stock | Débit/Crédit variation de stock |
| Règlement fournisseur | Débit fournisseur / Crédit banque |
| Encaissement tiers payant | Débit banque / Crédit créances |

---

## 5. Recommandation pour Pharma-Smart

### Approche recommandée : **Module intégré, architecture en couches**

```
┌─────────────────────────────────────────────────────┐
│                   PHARMA-SMART                      │
│                                                     │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────┐  │
│  │  Opérationnel│  │  Comptabilité│  │  Reporting│  │
│  │  (existant) │  │  (à créer)   │  │  (à créer)│  │
│  │             │  │              │  │           │  │
│  │ Ventes      │→ │ Grand-livre  │→ │ Bilan     │  │
│  │ Commandes   │→ │ Journaux     │→ │ Résultat  │  │
│  │ Stock       │→ │ AR / AP      │→ │ TVA       │  │
│  │ Caisse      │→ │ Rapprochement│→ │ Analytique│  │
│  └─────────────┘  └──────────────┘  └───────────┘  │
└─────────────────────────────────────────────────────┘
```

### Périmètre minimal viable (MVP comptabilité)

1. **Plan comptable SYSCOHADA** (norme Afrique de l'Ouest UEMOA/CEDEAO)
2. **Journaux automatiques** : achats, ventes, caisse, banque, opérations diverses
3. **Comptes tiers** : fournisseurs (AP) + tiers payants (AR)
4. **Lettrage** : rapprochement paiements ↔ factures
5. **Balance et grand-livre** : consultation et export
6. **Déclaration TVA** : états de synthèse mensuels/trimestriels
7. **Export expert-comptable** : format FEC (France) ou équivalent SYSCOHADA

### Ce qui peut être délégué (hors MVP)

- Liasse fiscale complète → export vers cabinet comptable
- Paie / charges sociales → logiciel dédié
- Immobilisations / amortissements → module optionnel phase 2
- Consolidation multi-sites → phase 3

---

## 6. Conclusion

Les solutions africaines les plus pertinentes (AS Pharm) et les leaders du marché américain (PioneerRx) convergent vers **l'intégration native de la comptabilité** dans le LGO. C'est le choix qui correspond le mieux au profil des utilisateurs cibles de Pharma-Smart : des pharmacies indépendantes ou en groupement qui ne peuvent pas se permettre de maintenir deux systèmes distincts.

L'implémentation sur la base **SYSCOHADA** (système comptable de référence UEMOA) est indispensable pour la conformité réglementaire en Afrique de l'Ouest.

---

*Document généré le 21/04/2026 — à réviser lors de l'initialisation du module comptabilité*
