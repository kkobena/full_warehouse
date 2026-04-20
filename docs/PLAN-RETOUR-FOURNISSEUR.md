# Plan d'implémentation — Retours fournisseur (Pharma-Smart)

## Contexte

Analyse fonctionnelle comparative du module retour fournisseur (branche `bon_bed`)
par rapport aux logiciels de référence officine (Winpharma, Pharmagest iGest, SmartRx, Caducée).

---

## État actuel ✅

- Création d'un retour lié à une commande de réception (BL)
- Sélection multi-lots par ligne avec répartition FEFO automatique
- Motif de retour obligatoire par ligne
- Décrément immédiat du stock à la création (lot + StockProduit), réinsertion en cas de suppression/modification
- Statuts : `VALIDATED → PROCESSING → CLOSED`
- Réponse fournisseur avec quantité acceptée par ligne, clôture automatique si tout accepté
- Chemin "hors commande" pour les lots sans entrée tracée
- Batch depuis lots périmés + algorithme de résolution lot
- Export PDF, Excel, CSV
- EDI PharmaML (`sendEdi`)

---

## Anomalies et lacunes à corriger

### P0 — Bug bloquant

#### BUG-1 : Réponse fournisseur impossible sur statut PROCESSING

**Fichier :** `RetourBonServiceImpl.java:200`

`createSupplierResponse` rejette tout statut ≠ `VALIDATED` avec _"Ce retour est déjà traité"_.
Or le template Angular affiche le bouton "Saisir réponse" pour `PROCESSING` également
(`retour-fournisseur.component.html:256-265`).

**Effet :** dès qu'un retour est marqué "En cours", il est bloqué — l'utilisateur ne peut
plus saisir la réponse fournisseur.

**Correction :**

```java
// RetourBonServiceImpl.java — createSupplierResponse
if (retourBon.getStatut() != RetourStatut.VALIDATED
    && retourBon.getStatut() != RetourStatut.PROCESSING) {
    throw new GenericError("Ce retour est déjà traité");
}
```

---

### P1 — Correctifs métier critiques

#### P1-1 : Contrôle double retour (quantité déjà retournée)

**Fichier :** `RetourBonServiceImpl.java:520` (`createRetourBonItem`)

Le backend vérifie `qtyMvt > initStock` (stock global), pas la somme des retours déjà créés
pour le même lot/orderLine. Il est possible de retourner la même unité sur deux bons distincts.

**Logique attendue :**
```
quantitéRetournable = qté reçue − Σ RetourBonItem.qtyMvt où lotId = lotId (statuts actifs)
```

**Tâches :**
- [ ] Ajouter `RetourBonItemRepository.sumQtyMvtByLotId(lotId, excludeRetourBonId)` (JPQL)
- [ ] Contrôler dans `createRetourBonItem` avant décrément
- [ ] Retourner un message d'erreur explicite avec la quantité déjà retournée

---

#### P1-2 : Statut PARTIALLY_ACCEPTED + clôture manuelle

**Fichier :** `RetourBonServiceImpl.java:220`, `RetourStatut.java`

Si le fournisseur accepte partiellement, `allItemsAccepted = false`, le bon reste ouvert
indéfiniment. Aucun statut intermédiaire ni bouton de clôture manuelle n'existe.

**Tâches :**
- [ ] Ajouter `PARTIALLY_ACCEPTED` dans l'enum `RetourStatut`
- [ ] Dans `createSupplierResponse` : si `!allItemsAccepted` → statut `PARTIALLY_ACCEPTED`
- [ ] Ajouter `closeManually(Integer id)` dans `RetourBonService` (statut → `CLOSED`)
- [ ] Ajouter le bouton "Clôturer manuellement" dans le template sur `PARTIALLY_ACCEPTED`
- [ ] Mettre à jour le filtre onglet "En attente" pour inclure `PARTIALLY_ACCEPTED`

---

### P2 — Améliorations fonctionnelles importantes

#### P2-1 : Valeur financière du retour

Le retour n'expose pas sa valeur (`Σ prixAchat × qtyMvt`). C'est la première information
consultée par un acheteur ou un comptable.

**Tâches :**
- [ ] Ajouter `montantTotal` calculé dans `RetourBonDTO` (somme des items)
- [ ] Ajouter `montantAccepte` calculé dans `RetourBonDTO`
- [ ] Afficher les deux colonnes dans la liste (liste + ligne expandée)
- [ ] Intégrer les montants dans le PDF

---

#### P2-2 : Référence structurée du bon de retour

Le retour utilise son `id` numérique brut. Les logiciels de référence génèrent
`RET-AAAA-NNN` imprimé sur le bordereau joint à la marchandise.

**Tâches :**
- [ ] Ajouter colonne `reference VARCHAR(30)` dans la table `retour_bon` (migration Flyway)
- [ ] Générer `RET-{YYYY}-{id:04d}` à la création dans `RetourBonServiceImpl.create`
- [ ] Afficher la référence dans la liste, le PDF et l'email EDI

---

#### P2-3 : Contrôle délai de retour

Les CGV fournisseurs et l'accord-cadre UNPF imposent des délais (30–90 jours défectueux,
12 mois périmés). Aucune validation entre `dateMtv` du retour et `orderDate` de la commande.

**Tâches :**
- [ ] Ajouter `delaiRetourJours` (configurable, défaut 365) dans les paramètres magasin
- [ ] Avertissement non-bloquant si `dateMtv − orderDate > delaiRetourJours`
- [ ] Afficher le warning dans `SupplierReturnsComponent` avant sauvegarde

---

#### P2-4 : Montant visible dans la liste + avoir fournisseur

**Dépend de P2-1.** Lien vers un futur module comptable :
- [ ] Créer entité `AvoirFournisseur` liée à `ReponseRetourBon` (montant = `Σ acceptedQty × prixAchat`)
- [ ] Dashboard : encours avoirs fournisseurs par fournisseur

---

### P3 — Améliorations UX secondaires

#### P3-1 : `canSave()` en mode édition

En mode édition, `selectedCommande` est hydratée programmatiquement dans `loadRetourForEdit`
mais `canSave()` dépend du signal. Si la signal n'est pas correctement settée, la sauvegarde
est bloquée silencieusement.

**Tâches :**
- [ ] S'assurer que `this.selectedCommande.set(commande)` est appelé avant le check `canSave`
- [ ] Ajouter un log d'avertissement si `canSave()` est false en mode édition au moment de `save()`

---

#### P3-2 : Regroupement multi-retours par fournisseur

Winpharma et Pharmagest permettent d'agréger plusieurs retours vers le même fournisseur
en un seul bordereau d'expédition.

**Tâches :**
- [ ] Vue "Regrouper par fournisseur" dans l'onglet En attente
- [ ] PDF groupé multi-retours

---

## Comparaison logiciels de référence

| Fonctionnalité | Pharma-Smart | Winpharma | Pharmagest iGest | SmartRx / Caducée |
|---|:---:|:---:|:---:|:---:|
| Retour sur BL | ✅ | ✅ | ✅ | ✅ |
| Sélection lot FEFO | ✅ | ✅ | ✅ | ✅ |
| Motif par ligne | ✅ | ✅ | ✅ | ✅ |
| EDI PharmaML | ✅ (stub) | ✅ | ✅ | Partiel |
| Contrôle double retour | ❌ P1-1 | ✅ | ✅ | ✅ |
| Avoir comptable auto | ❌ P2-4 | ✅ | ✅ | ✅ |
| Statut acceptation partielle | ❌ P1-2 | ✅ | ✅ | ✅ |
| Référence structurée RET-NNN | ❌ P2-2 | ✅ | ✅ | ✅ |
| Délai retour contrôlé | ❌ P2-3 | Configurable | ✅ | Partiel |
| Valeur financière visible | ❌ P2-1 | ✅ | ✅ | ✅ |
| Regroupement par fournisseur | ❌ P3-2 | ✅ | ✅ | ❌ |
| Hors commande | ✅ | Partiel | ✅ | Partiel |
| Export PDF bordereau | ✅ | ✅ | ✅ | ✅ |

---

## Ordre d'implémentation recommandé

```
Sprint 1 (bugs + métier critique)
  BUG-1  Réponse fournisseur sur PROCESSING          ~1h
  P1-1   Contrôle double retour                      ~3h
  P1-2   Statut PARTIALLY_ACCEPTED + clôture manuelle ~4h

Sprint 2 (valeur perçue)
  P2-1   Valeur financière dans liste + PDF           ~3h
  P2-2   Référence structurée RET-AAAA-NNN           ~2h
  P2-3   Avertissement délai retour                  ~2h

Sprint 3 (comptabilité + UX avancée)
  P2-4   AvoirFournisseur + dashboard encours        ~1j
  P3-1   Fix canSave() mode édition                  ~1h
  P3-2   Regroupement multi-retours                  ~2j
```

---

*Analyse réalisée le 2026-04-20 — branche `bon_bed`*
