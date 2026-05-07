# Plan d'amélioration — Module Comptes Fournisseurs AP

> Analyse du 07/05/2026
> Composant : `app-comptes-fournisseurs`
> Chemin : `src/main/webapp/app/features/finances/feature/comptes-fournisseurs/`

---

## Score global actuel : 6/10

La base est solide (signals Angular 20, master/detail, KPI, filtres statut, pagination serveur)
mais 4 problèmes critiques empêchent une utilisation en production sereine.

---

## 1. Ce qui est bien fait ✅

| Élément | Détail |
|---|---|
| Architecture Angular 20 signals | `signal()`, `computed()`, `takeUntilDestroyed` — propre |
| Layout master/detail split | UX cohérente avec le reste de l'app |
| 4 KPI cards | Total dû · Échéances dépassées · J+7 · Fournisseurs actifs |
| Filtres statut | EN_ATTENTE · PARTIEL · EN_RETARD · RÉGLÉ avec toggle actif |
| Pagination serveur sur les lignes | Scalable — `X-Total-Count` header |
| Validation montant ≤ solde | `computed(() => montantSaisi() <= solde())` |
| Historique règlements par BL | Onglet "Règlements — N° Bon" |
| Hint premier usage | Dismissable, persiste via `localStorage` |
| Coloration des lignes | Rouge = CRITIQUE, orange = EN_RETARD |

---

## 2. Problèmes critiques 🔴 — bloquants production

### P-CRIT-1 : Zéro feedback utilisateur sur le règlement

**Fichier :** `comptes-fournisseurs.component.ts` lignes 230–255

```typescript
// ACTUEL — success et erreur silencieux
next: () => {
  this.isSaving.set(false);   // ← l'utilisateur ne sait pas si ça a fonctionné
  this.activeTab.set('commandes');
  ...
},
error: () => this.isSaving.set(false)   // ← erreur silencieuse
```

**Impact :** Le pharmacien peut croire que le règlement n'a pas été enregistré et re-soumettre.
Double règlement possible.

**Correction :**
- Injecter `ToastAlertComponent` (déjà utilisé dans l'app)
- Afficher toast vert "Règlement enregistré" sur succès
- Afficher toast rouge "Erreur lors de l'enregistrement" sur erreur

---

### P-CRIT-2 : Ambiguïté règlement global vs règlement par BL

**Problème :** Le bouton **"Régler ce BL"** pré-remplit le formulaire avec `ligne.restantDu`
mais soumet à `POST /api/supplier-performance/{id}/ap/reglement` qui est un règlement
**global fournisseur** — le règlement n'est pas imputé sur la commande spécifique.

**Scénario problématique :**
```
Fournisseur LABOREX — Solde 500 000 FCFA
  BL-001 : restant 200 000 FCFA
  BL-002 : restant 300 000 FCFA

Pharmacien clique "Régler ce BL" sur BL-001
→ Formulaire pré-rempli : 200 000 FCFA
→ Soumet → règlement global de 200 000 FCFA
→ BL-001 n'est pas marqué RÉGLÉ côté backend
→ Le statut reste EN_ATTENTE → confusion
```

**Correction (2 options) :**
- **Option A** (simple) : Ajouter `commandeId` optionnel dans `IReglementFournisseurCommand`
  → Le backend impute sur ce BL en priorité, puis le restant en global.
- **Option B** (propre) : Créer une route dédiée `POST /ap/commandes/{commandeId}/reglement`
  → Règlement strictement lié à un BL unique.

---

### P-CRIT-3 : Pas de filtre période

**Problème :** L'API `GET /api/supplier-performance/ap` charge **toutes les commandes
non réglées depuis l'ouverture de la pharmacie**. Pas de paramètre `fromDate/toDate`.

**Impact :** Pour une pharmacie ouverte depuis 3+ ans, la liste peut contenir des centaines
de vieilles commandes dont certaines sont irrécouvrable (fournisseur disparu, litige).

**Correction :**
- Ajouter un filtre date (période) dans la toolbar : `Du [date] au [date]`
- Passer `fromDate` / `toDate` en paramètre `HttpParams` dans `FournisseurApApiService.getComptes()`
- Enrichir l'endpoint backend avec ces paramètres optionnels

---

### P-CRIT-4 : Pas d'export

**Problème :** Aucun bouton PDF/Excel.

**Impact :** Le pharmacien ne peut pas imprimer l'état de ses dettes pour une réunion
avec son représentant fournisseur — usage terrain très fréquent.

**Correction :**
- Ajouter bouton `[Export PDF]` dans la toolbar principale → génère la liste filtrée
- Ajouter bouton `[Export PDF]` dans le panel détail → génère l'état d'un fournisseur
- Backend : `GET /api/supplier-performance/ap/export/pdf?fromDate=&toDate=`
- Backend : `GET /api/supplier-performance/{id}/ap/export/pdf`

---

## 3. Problèmes moyens 🟠 — UX dégradée

### P-MOY-1 : Badge Commandes affiche la taille de page et non le total

**Fichier :** `comptes-fournisseurs.component.html` ligne 189

```html
<!-- ACTUEL — affiche lignes().length = nb de lignes de la page courante -->
<span class="badge bg-danger ms-1">{{ lignes().length }}</span>

<!-- CORRECT -->
<span class="badge bg-danger ms-1">{{ totalLignes() }}</span>
```

---

### P-MOY-2 : `totalCommande` / `totalRegle` présents dans le modèle mais non affichés

**Fichier :** `fournisseur-ap.model.ts` lignes 9–10

Ces champs existent dans `ICompteFournisseurAP` mais ne sont affichés nulle part dans le template.
Le panel détail ne montre pas le récapitulatif financier global du fournisseur.

**Correction — ajouter un mini-récapitulatif dans le panel header :**
```html
<div class="d-flex gap-4 mt-1" style="font-size: 0.82rem;">
  <span>Commandé : <strong>{{ formatCurrency(f.totalCommande) }} FCFA</strong></span>
  <span>Réglé : <strong class="text-success">{{ formatCurrency(f.totalRegle) }} FCFA</strong></span>
  <span>Solde : <strong class="text-danger">{{ formatCurrency(f.solde) }} FCFA</strong></span>
</div>
```

---

### P-MOY-3 : Pas de confirmation avant enregistrement d'un règlement

Un clic accidentel sur "Enregistrer" avec un montant important → règlement mal enregistré.

**Correction :** Ajouter un dialog de confirmation `ngbModal` (déjà utilisé dans l'app) :
```
"Confirmer le règlement de 200 000 FCFA par chèque pour LABOREX ?"
[Confirmer]  [Annuler]
```

---

### P-MOY-4 : Pas de bouton "Régler tout"

Action rapide pour solder un fournisseur en 1 clic.

**Correction :** Ajouter dans le panel header :
```html
<p-button label="Régler tout" icon="pi pi-check"
          severity="success" size="small"
          (onClick)="reglementForm.patchValue({ montant: solde() }); activeTab.set('regler')" />
```

---

### P-MOY-5 : `prochaineEcheance` affiché sans formatage

**Fichier :** `comptes-fournisseurs.component.html` ligne 134

```html
<!-- ACTUEL — string ISO "2026-05-15" affiché brut -->
<td>{{ c.prochaineEcheance ?? '—' }}</td>

<!-- CORRECT -->
<td>{{ c.prochaineEcheance ? (c.prochaineEcheance | date:'dd/MM/yyyy') : '—' }}</td>
```

---

## 4. Améliorations mineures 🟡

| # | Fichier | Problème | Correction |
|---|---|---|---|
| M-1 | `fournisseur-ap-api.service.ts` | `SpringPage<T>` défini dans le service (mauvais endroit) | Déplacer dans `models/` ou `shared/` |
| M-2 | `fournisseur-ap.model.ts` | Pas de champ `mobile` — fournisseurs ont souvent un mobile | Ajouter `mobile?: string` |
| M-3 | `comptes-fournisseurs.component.ts` | `modeReglementOptions` manque "Traite/Effet" — mode courant en Afrique de l'Ouest | Ajouter `{ label: 'Traite', value: 'TRAITE' }` |
| M-4 | `comptes-fournisseurs.component.html` | Contact non affiché dans la liste principale | Afficher téléphone sous le nom fournisseur |
| M-5 | `comptes-fournisseurs.component.html` | Critères CRITIQUE vs EN_RETARD non documentés | Ajouter `pTooltip` d'explication sur les tags statut |
| M-6 | `comptes-fournisseurs.component.ts` | `selectedFournisseur()?.fournisseurId === c.fournisseurId` dans le template | Extraire en méthode `isSelected(c)` |

---

## 5. Analyse de couverture du besoin métier

| Besoin pharmacien | Statut | Notes |
|---|---|---|
| Voir combien je dois à chaque fournisseur | ✅ Couvert | Colonne "Solde dû" + KPI total |
| Voir quelles commandes ne sont pas réglées | ✅ Couvert | Onglet Commandes avec filtres |
| Filtrer par statut (retard, partiel…) | ✅ Couvert | Boutons filtre fonctionnels |
| Connaître les échéances urgentes | ✅ Couvert | KPI J+7 + prochaine échéance |
| Voir l'historique des règlements | ✅ Couvert | Onglet "Règlements — N° Bon" |
| Enregistrer un règlement | ⚠️ Partiel | Présent mais sans feedback ni lien BL fiable |
| Régler un BL spécifique | ❌ Manquant | Ambiguïté global vs BL (P-CRIT-2) |
| Imprimer l'état pour réunion fournisseur | ❌ Manquant | Pas d'export (P-CRIT-4) |
| Filtrer par période | ❌ Manquant | Pas de filtre date (P-CRIT-3) |
| Savoir le total commandé vs réglé | ⚠️ Partiel | Champs en modèle mais non affichés (P-MOY-2) |

---

## 6. Plan de correction priorisé

### Sprint 1 — Corrections critiques (2 jours)

| Tâche | Fichier(s) | Effort |
|---|---|---|
| Toast feedback success/erreur règlement | `comptes-fournisseurs.component.ts` | 0.5j |
| Filtre période (fromDate/toDate) | composant + service + backend | 0.5j |
| Correction badge `totalLignes()` | `comptes-fournisseurs.component.html` | 15 min |
| Afficher `totalCommande` / `totalRegle` | `comptes-fournisseurs.component.html` | 15 min |

### Sprint 2 — Export + règlement par BL (2 jours)

| Tâche | Fichier(s) | Effort |
|---|---|---|
| Export PDF liste globale | backend + service Angular + bouton | 1j |
| Export PDF détail fournisseur | backend + service Angular + bouton | 0.5j |
| Règlement lié à un BL (`commandeId` dans le POST) | backend + service + composant | 0.5j |

### Sprint 3 — UX (1 jour)

| Tâche | Fichier(s) | Effort |
|---|---|---|
| Dialog de confirmation avant règlement | `comptes-fournisseurs.component.ts/html` | 0.5j |
| Bouton "Régler tout" | `comptes-fournisseurs.component.html` | 15 min |
| Formatage `prochaineEcheance` | `comptes-fournisseurs.component.html` | 5 min |
| Ajout mode "Traite/Effet" | `comptes-fournisseurs.component.ts` | 5 min |
| Ajout champ `mobile` | modèle + backend | 15 min |

---

## 7. Résumé

| Priorité | Nb problèmes | Effort estimé |
|---|---|---|
| 🔴 Critiques | 4 | ~2 jours |
| 🟠 Moyens | 5 | ~1.5 jours |
| 🟡 Mineurs | 6 | ~1 jour |
| **Total** | **15** | **~4.5 jours** |

**Score après corrections : 9/10**

---

*Analyse créée le 07/05/2026*

