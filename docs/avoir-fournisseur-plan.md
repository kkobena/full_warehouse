# Plan d'amélioration — Avoir Fournisseur & Retour Bon

> Analyse du code existant + comparaison logiciels de référence (Pharmagest, LGPI, Winpharma)
> Date : 2026-04-22

---

## Décisions architecturales (notes du product owner)

> Ces décisions priment sur les recommandations initiales de l'analyse.

1. **Supprimer `ReponseRetourBon` et `ReponseRetourBonItem`** — `AvoirFournisseur` suffit. La saisie de la réponse fournisseur devient directement la création de l'avoir avec ses lignes.
2. **Garder `montant` en `long`** — La devise cible (FCFA) est entière, pas de centimes. BigDecimal est inutile ici.
3. **Pattern UX : remplacer la liste par le workspace** — Comme `list-bons.component.html` remplace la liste par `app-reception-concordance` / détail inline, le retour par ligne utilisera le même pattern (la liste des lignes BL est remplacée par le workspace de saisie du retour, sans navigation ni modal).
4. **Évaluer `app-reception-concordance`** — Voir si ce composant peut afficher aussi les données de retour fournisseur associées au bon (retours déjà effectués, avoirs existants).

---

## 1. État actuel — Analyse critique

### 1.1 Chaîne d'entités (supprimée)

**Avant :**
```
RetourBon → ReponseRetourBon → ReponseRetourBonItem → AvoirFournisseur
```

**Après (cible) :**
```
RetourBon → AvoirFournisseur → AvoirFournisseurLine
```

La suppression de `ReponseRetourBon` / `ReponseRetourBonItem` simplifie le flux :
- L'avoir est créé directement depuis le retour bon, avec les lignes acceptées par le fournisseur
- Plus d'entité intermédiaire — la réponse fournisseur EST l'avoir
- Un `RetourBon` peut avoir plusieurs avoirs (`@OneToMany`) — cas des réponses partielles successives

### 1.2 Problèmes identifiés (après décisions PO)

| # | Fichier | Problème | Criticité |
|---|---------|----------|-----------|
| P0 | `AvoirFournisseurStatut` | Pas de statut `ANNULE` — impossible d'annuler un avoir | Haute |
| P0 | `AvoirFournisseur` | `@OneToOne reponseRetourBon` à remplacer par `@ManyToOne retourBon` après suppression ReponseRetourBon | Haute |
| P0 | `ReponseRetourBon` / `ReponseRetourBonItem` | À supprimer — remplacés par `AvoirFournisseurLine` | Haute |
| P1 | `AvoirFournisseur` | `reference` générée après save (double requête) — pattern fragile | Basse |
| P1 | UI | Pas de retour **par ligne** depuis la fiche BL | Haute |
| P1 | UI | Onglet AVOIRS dans `retour-fournisseur` basique — pas d'imputation depuis la liste | Moyenne |
| P2 | `AvoirFournisseur` | Pas de lien explicite avec la commande sur laquelle l'avoir est imputé | Basse |
| P2 | Exports | Pas de PDF / Excel pour les avoirs fournisseur | Basse |

---

## 2. Comparaison avec logiciels de référence

### 2.1 Pharmagest (leader marché)

| Fonctionnalité | Pharmagest | Pharma-Smart actuel | Cible |
|----------------|-----------|---------------------|-------|
| Retour par ligne depuis BL | ✅ Sélection lignes + qtés sur le BL reçu | ❌ Retour global uniquement | ✅ Workspace inline |
| Génération avoir | ✅ Immédiate sur saisie réponse | ✅ Via ReponseRetourBon | ✅ Directement sur RetourBon |
| Traçabilité ligne BL → avoir | ✅ | ⚠️ Via 3 entités | ✅ Via AvoirFournisseurLine |
| Annulation avoir | ✅ | ❌ | ✅ Statut ANNULE |
| PDF avoir fournisseur | ✅ | ❌ | P2 |
| Dashboard encours par fournisseur | ✅ | ⚠️ Basique | ✅ KPI amélioré |

### 2.2 LGPI

- Retour par ligne depuis la réception, avec quantité partielle
- Avoir créé immédiatement (pas d'entité réponse intermédiaire) → **correspond à notre cible**
- Numéro séquentiel de l'avoir (ex: `AV-2026-0001`)

### 2.3 Winpharma / Caduciel

- Avoir avec statut : En attente → Imputé / Remboursé → Annulé
- Suivi du montant restant (si imputation partielle sur plusieurs commandes)
- Export PDF + Excel de la liste des avoirs

### 2.4 Conclusion comparative

**Ce qui manque le plus** :
1. **Retour par ligne depuis la fiche BL** — 100% des logiciels de référence le font
2. **Statut ANNULE** sur l'avoir fournisseur
3. **PDF de l'avoir fournisseur** — document à envoyer au fournisseur

---

## 3. Plan d'implémentation

### P0 — Refactoring structurel (prérequis à tout le reste)

#### P0.1 Supprimer `ReponseRetourBon` et `ReponseRetourBonItem`

**Entités à supprimer :**
- `domain/ReponseRetourBon.java`
- `domain/ReponseRetourBonItem.java`
- `repository/ReponseRetourBonRepository.java`
- `service/dto/ReponseRetourBonDTO.java`
- Références dans `RetourBonService.createSupplierResponse()`
- `SupplierResponseModalComponent` — à supprimer ou réécrire en création d'avoir

**Entité à créer — `AvoirFournisseurLine`** :
```java
@Entity
@Table(name = "avoir_fournisseur_line")
public class AvoirFournisseurLine implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "avoir_fournisseur_id", nullable = false)
    private AvoirFournisseur avoirFournisseur;

    @ManyToOne(optional = false)
    @JoinColumn(name = "retour_bon_item_id", nullable = false)
    private RetourBonItem retourBonItem;

    @Column(name = "qty_mvt", nullable = false)
    private Integer qtyMvt;           // quantité acceptée par le fournisseur

    @Column(name = "prix_achat", nullable = false)
    private long prixAchat;           // prix validé par le fournisseur (long = FCFA entier)

    @Column(name = "commentaire", length = 150)
    private String commentaire;
}
```

**Modifier `AvoirFournisseur`** :
```java
// Remplacer le lien vers ReponseRetourBon par un lien direct vers RetourBon
@ManyToOne(optional = false)
@JoinColumn(name = "retour_bon_id", nullable = false)
private RetourBon retourBon;

// Remplacer montant (long) par calcul sur lignes — garder long (FCFA)
@Column(name = "montant", nullable = false)
private long montant;               // somme de (prixAchat × qtyMvt) des lignes

// Ajouter la collection de lignes
@OneToMany(mappedBy = "avoirFournisseur", cascade = CascadeType.ALL, orphanRemoval = true)
private List<AvoirFournisseurLine> lignes = new ArrayList<>();
```

**Migration Flyway requise :**
```sql
-- Créer la table des lignes d'avoir
CREATE TABLE avoir_fournisseur_line (
    id SERIAL PRIMARY KEY,
    avoir_fournisseur_id INT NOT NULL REFERENCES avoir_fournisseur(id),
    retour_bon_item_id INT NOT NULL REFERENCES retour_bon_item(id),
    qty_mvt INT NOT NULL,
    prix_achat BIGINT NOT NULL,
    commentaire VARCHAR(150)
);

-- Remplacer la FK reponse_retour_bon_id par retour_bon_id sur avoir_fournisseur
ALTER TABLE avoir_fournisseur ADD COLUMN retour_bon_id INT REFERENCES retour_bon(id);
-- (migrer les données existantes via la chaîne reponseRetourBon.retourBon si nécessaire)
ALTER TABLE avoir_fournisseur DROP COLUMN reponse_retour_bon_id;

-- Supprimer les anciennes tables (après migration des données)
DROP TABLE reponse_retour_bon_item;
DROP TABLE reponse_retour_bon;
```

#### P0.2 Ajouter `ANNULE` à `AvoirFournisseurStatut`

```java
public enum AvoirFournisseurStatut {
    EN_ATTENTE, REMBOURSE, IMPUTE, ANNULE
}
```

---

### P1 — Retour par ligne depuis la fiche BL

#### P1.1 Pattern UX — Workspace inline (même pattern que list-bons)

`list-bons.component.html` utilise le pattern : quand un bon est sélectionné, la liste est **remplacée** par une vue détail inline (`app-reception-concordance` + lignes). Ce même pattern s'applique au retour par ligne.

**Flux UX cible :**
1. L'utilisateur est dans la liste des BL reçus (`list-bons.component`)
2. Il clique sur "Retour par ligne" sur un BL → la liste est remplacée par `app-retour-workspace`
3. `app-retour-workspace` affiche les lignes du BL avec sélection native PrimeNG (`selectionMode="multiple"`)
4. Pour chaque ligne sélectionnée : champ quantité à retourner (max = qté reçue) + motif optionnel
5. Bouton "Créer l'avoir" → crée directement un `AvoirFournisseur` avec ses lignes (sans RetourBon intermédiaire dans ce flow, ou avec création automatique d'un RetourBon)
6. Retour "← Liste" restaure la vue liste

**`app-reception-concordance` — évaluation :**
Ce composant affiche actuellement les statistiques de concordance BL/commande. Il pourrait être enrichi pour afficher aussi :
- Le nombre et montant total des avoirs fournisseur rattachés à ce BL
- Un lien rapide vers la liste des avoirs du BL
- Indicateur visuel si des retours sont en cours (statut EN_ATTENTE)

**Composant Angular à créer** :
```
features/commande/ui/retour-workspace/
  retour-workspace.component.ts   ← logique sélection lignes + création avoir
  retour-workspace.component.html ← table PrimeNG selectionMode="multiple"
  retour-workspace.component.scss
```

**Intégration dans `list-bons.component`** :
```typescript
// Même pattern que editingReceived() / selectedClosed()
protected retourWorkspaceBon = signal<ICommande | null>(null);

// Dans le template :
// @if (retourWorkspaceBon()) { <app-retour-workspace [bon]="retourWorkspaceBon()!" /> }
// @else { ... liste normale ... }
```

**Backend — nouvelle méthode `AvoirFournisseurService.createFromBonLines()`** :
```java
AvoirFournisseurDTO createFromBonLines(
    Integer commandeId,
    LocalDate commandeDate,
    List<AvoirLigneCommand> lignes,  // ligneId + qtyAcceptee + prixAchat
    String commentaire
);
```
- Crée un `RetourBon` (statut CLOSED) si pas déjà existant pour ce BL
- Crée les `RetourBonItem` correspondants
- Crée l'`AvoirFournisseur` avec ses `AvoirFournisseurLine`
- Ajuste le stock (sortie)

#### P1.2 UI Avoirs fournisseur améliorée (onglet AVOIRS)

L'onglet `AVOIRS` dans `retour-fournisseur.component` doit afficher :

- Liste paginée des avoirs avec colonnes : Référence | Fournisseur | Date | Montant | Statut | Retour lié
- Filtres : fournisseur, statut, période, référence avoir
- Actions par ligne :
  - **Imputer** (EN_ATTENTE → IMPUTE) avec saisie de la commande cible
  - **Rembourser** (EN_ATTENTE → REMBOURSE)
  - **Annuler** (→ ANNULE, avec motif)
  - **PDF** (télécharger l'avoir)
- KPI bar : total encours | total imputé | total remboursé

---

### P2 — Améliorations complémentaires (backlog)

#### P2.1 PDF Avoir fournisseur

- `AvoirFournisseurPdfService extends CommonReportService`
- Template Thymeleaf `avoir-fournisseur/pdf/main.html`
- Endpoint `GET /api/avoirs-fournisseur/{id}/pdf`
- En-tête pharmacie + coordonnées fournisseur
- Tableau des lignes (produit, qté, prix unitaire, sous-total) — montants en entiers FCFA
- Total général + signature

#### P2.2 Export Excel liste avoirs fournisseur

- Endpoint `GET /api/avoirs-fournisseur/export/excel`
- Colonnes : Référence, Fournisseur, Date, Retour lié, Montant, Statut, Utilisateur

#### P2.3 Imputation avec lien commande

```java
// AvoirFournisseur.java
@ManyToOne
@JoinColumns({
    @JoinColumn(name = "commande_imputee_id", referencedColumnName = "id"),
    @JoinColumn(name = "commande_imputee_date", referencedColumnName = "order_date")
})
private Commande commandeImputee;

@Column(name = "montant_restant")
private long montantRestant;    // long (FCFA entier)
```

---

## 4. Modèle de données cible

```
RetourBon (1) ────────── (*) AvoirFournisseur
                                    │
                            (*) AvoirFournisseurLine
                                    │
                            (1) RetourBonItem
```

**Tables Flyway à créer / modifier :**

| Table | Action | Migration |
|-------|--------|-----------|
| `avoir_fournisseur_line` | Créer | V{N}__avoir_fournisseur_line.sql |
| `avoir_fournisseur` | Ajouter `retour_bon_id`, supprimer `reponse_retour_bon_id` | V{N}__avoir_fournisseur_refacto.sql |
| `avoir_fournisseur` | Ajouter `montant_restant`, `commande_imputee_id/date` | V{N}__avoir_fournisseur_imputation.sql (P2) |
| `reponse_retour_bon` | Supprimer | V{N}__drop_reponse_retour_bon.sql |
| `reponse_retour_bon_item` | Supprimer | Même migration |

---

## 5. Priorisation

| Priorité | Tâche | Effort | Impact |
|----------|-------|--------|--------|
| P0 | Ajouter ANNULE à l'enum | 30 min | Critique |
| P0 | Supprimer ReponseRetourBon/Item + créer AvoirFournisseurLine | 1 jour | Critique |
| P0 | Modifier AvoirFournisseur (lien RetourBon direct) + migration | 2h | Critique |
| P1 | Retour par ligne — workspace inline (pattern list-bons) | 2 jours | Très élevé |
| P1 | Enrichir `app-reception-concordance` avec données retour/avoirs | 3h | Moyen |
| P1 | UI avoirs améliorée (liste + KPI + actions) | 1 jour | Élevé |
| P2 | PDF avoir fournisseur | 1 jour | Moyen |
| P2 | Export Excel avoirs | 3h | Moyen |
| P2 | Imputation avec lien commande + montant restant | 1 jour | Faible |
