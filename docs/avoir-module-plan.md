# Plan de refonte — Module Avoir Tiers-Payant

> Pharma-Smart · Analyse réalisée le 2026-04-21

---

## 1. Bugs critiques (P0 — Bloquant)

| # | Fichier | Problème | Impact |
|---|---------|----------|--------|
| 🔴 | `AvoirServiceImpl.java:65` | Service écrase le statut par `IMPUTE` malgré le défaut `DRAFT` correct du domaine (`AvoirTiersPayant.statut = AvoirStatut.DRAFT`) | Workflow entier court-circuité |
| 🔴 | `AvoirServiceImpl.exportPdf()` | `return new byte[0]` — stub non implémenté | Aucune impression possible |
| 🔴 | `AvoirResource.findAll()` | `tiersPayantId` obligatoire (`@RequestParam`) | Impossible de lister sans filtre TP |

---

## 2. Lacunes fonctionnelles vs besoin métier

| Fonctionnalité | État | Commentaire |
|---|---|---|
| Saisie ligne par ligne (`AvoirLine`) | ❌ UI absente | Table et DTO existent, form ne l'expose pas |
| Recherche par `numAvoir` | ❌ Absent | Impossible de retrouver un avoir par son numéro |
| Vérification `montantTva + montantHt ≤ montantAvoir` | ❌ Absent | Incohérence comptable possible |
| Historique avoirs sur fiche facture | ❌ Absent | Pas de lien facture → avoirs associés |
| Notification à l'émission | ❌ Absent | Le TP n'est pas notifié au passage `EMIS` |
| Avoir sur ventes directes | ❌ TODO commenté | "création d'avoir sur les ventes n'est pas encore implémentée" |
| Facture cible filtrée par TP lors imputation | ⚠️ Partiel | Pas de validation que la facture cible est du même TP |
| Motif d'annulation | ⚠️ Absent | Aucune raison d'annulation enregistrée |
| Export PDF bordereau avoir | 🔴 Stub | Retourne vide |
| Dashboard / KPI avoirs | ❌ Absent | Pas de vue synthétique |

---

## 3. Analyse comparative — Logiciels de référence

### Sage 100 / LGPI Officine
- Avoir = **contrepassation automatique** (écritures comptables symétriques)
- Workflow : `Brouillon → Validé → Comptabilisé` avec verrouillage après comptabilisation
- Lignes obligatoires : référence les lignes de la facture origine avec quantités retournées
- PDF immédiat (Jasper Reports côté serveur)

### Cegid Pharma
- Avoir lié aux **dossiers rejetés** par l'organisme (AMO/AMC)
- Workflow : `Saisie → Envoi organisme → Retour → Imputation`
- **Motif de rejet par ligne** obligatoire (code rejet)
- Réconciliation automatique avec le retour de virement

### Odoo Accounting
- Note de crédit = **facture inversée** (montants négatifs), même structure que la facture
- Bouton "Ajouter note de crédit" depuis la facture, pré-remplit toutes les lignes
- Statuts : `Brouillon → Confirmé → Lettré` (lettrage automatique si même montant)
- Timeline visible sur la facture d'origine

### EBP Pharmacie
- Avoir = correction de facturation sur les montants uniquement
- Saisie rapide depuis la liste des factures avec pré-remplissage complet
- Validation en deux étapes : saisie opérateur → validation responsable
- Export au format NOEMIE pour les organismes

### Ce qui ne répond pas au besoin métier Pharma-Smart

```
Architecture domain (correct) :
  AvoirTiersPayant → factureTiersPayant (FactureTiersPayant)
                           └→ tiersPayant (TiersPayant)
  Le tiers-payant est dérivé automatiquement de la facture.
  La colonne tiers_payant_id a été supprimée (V1.5.4 — redondante).
  tiersPayantId dans IAvoirCommand frontend est donc inutile.

1. ABSENCE DE GESTION DES REJETS PAR LIGNE
   En pharmacie, un organisme rejette des dossiers précis dans une facture,
   pas la facture entière. Le form actuel ne gère qu'un montant global.
   Les tables avoir_line et AvoirLine existent mais sont inutilisées côté UI.

2. WORKFLOW TROP SIMPLE
   La réalité : organisme → retourne un virement réduit → pharmacie saisit
   l'écart → l'avoir correspond à ce qui n'est pas récupéré.
   Manque : date de retour organisme, montant virement reçu, réconciliation.

3. PAS DE TRAÇABILITÉ DE L'IMPUTATION
   On sait qu'un avoir est "IMPUTE" mais pas sur quelle facture cible il a
   été déduit, ni la date de l'opération.

4. BUG STATUT INITIAL dans AvoirServiceImpl
   Le domaine initialise correctement statut = DRAFT.
   Mais AvoirServiceImpl.creerAvoir() l'écrase à IMPUTE.
   Tout le workflow est inutilisable en production.

5. PDF NON FONCTIONNEL
   L'avoir ne peut pas être transmis à l'organisme.
```

---

## 4. Plan UX — Refonte proposée

### Écran 1 — Liste des avoirs

```
Header :  [ Période | Tiers-payant | Statut ]   [ + Nouvel avoir ]

KPI bar :
┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  Brouillons  │ │    Émis      │ │   Imputés    │ │   Annulés    │
│      3       │ │      7       │ │     42       │ │      1       │
│  45 000 F    │ │  120 000 F   │ │ 1 250 000 F  │ │   8 000 F    │
└──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘

Tableau :
| N° Avoir    | Facture origine | Tiers-payant | Montant | Date  | Statut  | Actions         |
| AV-2025_001 | FAC-2025-042    | CPAM PN      | 12 450  | 15/01 | [ÉMIS]  | Imputer PDF Ann.|
| AV-2025_002 | FAC-2025-031    | MGPTT        | 8 200   | 10/01 | [DRAFT] | Émettre PDF Ann.|

- Badge : DRAFT=gris · ÉMIS=bleu · IMPUTÉ=vert · ANNULÉ=rouge
- Actions contextuelles selon statut uniquement
- Clic ligne → drawer détail latéral
```

### Écran 2 — Création d'avoir (modal)

```
Mode A — Depuis la fiche facture (contexte pré-rempli)
┌──────────────────────────────────────────────────────────┐
│ Nouvel avoir sur FAC-2025-042                            │
├──────────────────────────────────────────────────────────┤
│ [info-box] FAC-2025-042 | CPAM Paris Nord                │
│            Montant net : 85 000  |  Réglé : 72 550       │
├──────────────────────────────────────────────────────────┤
│ Type d'avoir  ○ Montant global   ○ Par ligne de dossier  │
├──────────────────────────────────────────────────────────┤
│ MODE GLOBAL                                              │
│   Montant avoir *  [____________]  ≤ 72 550              │
│   dont TVA         [____________]                        │
│   dont HT          [____________]                        │
│   Motif *          [________________________________]    │
├──────────────────────────────────────────────────────────┤
│ MODE PAR LIGNE                                           │
│  ┌──────────────────────────────────────────────────┐    │
│  │ N° Dossier | Patient      | Montant | Rejet | Motif│   │
│  │ DOS-001    | MARTIN Paul  | 1 250   | [x]   | [__] │   │
│  │ DOS-002    | DURAND Marie |   890   | [ ]   |      │   │
│  └──────────────────────────────────────────────────┘    │
│  Total lignes rejetées : 1 250 F                         │
├──────────────────────────────────────────────────────────┤
│ [Annuler]                     [Enregistrer brouillon]    │
└──────────────────────────────────────────────────────────┘

Mode B — Création libre (sans facture)
- Autocomplete facture (PAID/PARTIALLY_PAID uniquement)
- Une fois facture choisie → affiche info-box + mêmes champs
```

### Écran 3 — Détail avoir (drawer latéral)

```
┌──────────────────────────────────┐
│ AV-2025_001           [ÉMIS] 🔵  │
├──────────────────────────────────┤
│ Facture      : FAC-2025-042      │
│ Tiers-payant : CPAM Paris Nord   │
│ Créé le      : 15/01/2025 Martin │
├──────────────────────────────────┤
│ Montant avoir :   12 450 F       │
│ dont TVA      :    1 245 F       │
│ dont HT       :   11 205 F       │
├──────────────────────────────────┤
│ Motif :                          │
│ "Dossiers rejetés suite retour   │
│  virement du 14/01/2025"         │
├──────────────────────────────────┤
│ TIMELINE                         │
│ ● 15/01 Créé    (Martin)         │
│ ● 16/01 Émis    (Martin)         │
│ ○ —    Imputation en attente     │
├──────────────────────────────────┤
│ [Imputer] [PDF] [Annuler]        │
└──────────────────────────────────┘
```

### Écran 4 — Dialog d'imputation

```
┌──────────────────────────────────────┐
│ Imputer AV-2025_001 (12 450 F)       │
├──────────────────────────────────────┤
│ Facture cible *                      │
│ [Autocomplete — même TP uniquement]  │
│                                      │
│ FAC-2025-055 | Restant : 45 000 F    │
│ ✅ Montant avoir ≤ restant           │
├──────────────────────────────────────┤
│ [Annuler]              [Imputer]     │
└──────────────────────────────────────┘
```

### Lien retour — Onglet "Avoirs" sur fiche facture

```
Ajouter un onglet "Avoirs associés" sur la fiche facture :
┌──────────────────────────────────────────┐
│ N° Avoir    │ Montant │ Date   │ Statut   │
│ AV-2025_001 │ 12 450  │ 15/01  │ [IMPUTÉ] │
└──────────────────────────────────────────┘
Bouton [+ Créer un avoir] visible si statut PAID ou PARTIALLY_PAID
```

---

## 5. Feuille de route

### P0 — Bloquant (corrections immédiates)

- [ ] Corriger statut initial `IMPUTE` → `DRAFT` dans `AvoirServiceImpl.creerAvoir()`
- [ ] Implémenter `exportPdf()` avec Flying Saucer + template Thymeleaf
- [ ] Rendre `tiersPayantId` optionnel dans `AvoirResource.findAll()`

### P1 — Fonctionnel minimal

- [ ] KPI bar dans `AvoirComponent` (4 tuiles : brouillons / émis / imputés / annulés)
- [ ] Drawer détail avec timeline des transitions
- [ ] Recherche par `numAvoir` dans les filtres
- [ ] Dialog d'imputation avec autocomplete filtré par même TP
- [ ] Validation `montantTva + montantHt ≤ montantAvoir`

### P2 — Valeur métier

- [ ] Saisie par ligne de dossier (activer `AvoirLine` côté UI)
- [ ] Motif obligatoire à l'annulation
- [ ] Onglet "Avoirs associés" sur la fiche facture
- [ ] Traçabilité imputation (facture cible + date enregistrées)

### P3 — Excellence

- [ ] Notification email/in-app à l'émission
- [ ] Export Excel liste des avoirs
- [ ] Réconciliation avec règlements (`AvoirTiersPayant ↔ Reglement`)
- [ ] Export format NOEMIE pour les organismes AMO/AMC
