# Proposition — Refonte master/detail de l'écran tiers payant (sur le patron `produit-home`)

> Statut : **proposition d'architecture** — aucune ligne de code écrite.
> Date : octobre 2026.
> Fait suite à une question directe sur l'opportunité de reprendre le patron master/detail déjà
> utilisé pour les produits
> (`features/products/feature/produit-home`, `features/products/ui/produit-detail-panel`,
> `shared/ui/detail-section/*`) pour l'écran de gestion des tiers payants.
> Documents amont (constats détaillés, ne pas dupliquer) :
> [PLAN-ANALYSE-COMPARATIVE-FORMULAIRE-TIERS-PAYANT.md](PLAN-ANALYSE-COMPARATIVE-FORMULAIRE-TIERS-PAYANT.md) —
> la modale de création/édition.
> [PLAN-ANALYSE-COMPARATIVE-ECRAN-GESTION-TIERS-PAYANT.md](PLAN-ANALYSE-COMPARATIVE-ECRAN-GESTION-TIERS-PAYANT.md) —
> l'écran de liste actuel.

---

## 1. Mon avis : oui, sans réserve

C'est la bonne direction, et pour une raison plus forte qu'une préférence esthétique : **ce patron
existe déjà dans cette même application, mature, et résout de lui-même plusieurs écarts déjà
identifiés** sur l'écran tiers payant sans qu'il faille les corriger un par un :

| Écart déjà identifié | Résolu automatiquement par le patron `produit-home` |
|---|---|
| Statut forcé à ACTIF, pas de réactivation (écran-gestion §3) | `produit-home` a déjà un filtre « État du catalogue » + actions groupées `onBulkEnable()`/`onBulkDisable()` — patron à copier tel quel |
| `massUpdateFactureConfig` prêt côté service mais inutilisable (écran-gestion §4) | `produit-home` a déjà une « barre d'actions groupées » (`bulk-action-bar`) qui apparaît sur sélection multiple — même mécanique, autre payload |
| « Voir détails » mort, dossier éclaté entre modules (écran-gestion §5) | Le clic sur une ligne ouvre un panneau détail **inline**, pas une route séparée à construire — le panneau porte lui-même des onglets (Synthèse, Stock, Mouvements, Ventes, Achats, Fournisseurs, Rayons…) : c'est exactement le « dossier consolidé » que je recommandais, avec un patron déjà prouvé |
| Montants sans unité affichée, dans la modale et dans la liste (formulaire §7.1, écran-gestion §6.3) | `app-detail-field format="montant"` formate déjà la devise correctement (`formatCurrencyWithUnit`) — aucun développement ad hoc à refaire |
| Formulaire dense à deux usages (comptoir express vs back-office), (formulaire §3) | Le panneau détail sépare naturellement les sections en onglets/cartes repliables (`app-detail-section`, état replié mémorisé par `storageKey`) — la modale complète reste réservée à la création, le panneau détail devient l'écran de consultation/édition courant |

Le gain n'est donc pas que cosmétique : adopter ce patron **supprime le besoin de réinventer** une
bonne partie des correctifs proposés séparément dans les deux documents précédents, en réutilisant
des composants déjà écrits, testés et éprouvés sur le catalogue produit (le plus gros écran de
gestion de l'application).

---

## 2. Proposition de structure

### 2.1 Écran maître (remplace `TiersPayantComponent` actuel)

Reprendre exactement la charpente de `produit-home.component.html` :

```
<app-toolbar> recherche + filtre catégorie + filtre statut (ACTIF/DESACTIVE/TOUS, nouveau) </app-toolbar>
<app-hint storageKey="tiers-payant-list"> Cliquez sur une ligne pour voir le dossier complet </app-hint>

@if (hasSelection()) {
  <bulk-action-bar>
    - Réactiver / Désactiver (sélection multiple)
    - Modifier la facturation (→ massUpdateFactureConfig, enfin exposé)
  </bulk-action-bar>
}

<div class="split-container" [class.panel-open]="panelOpen()">
  <div class="list-column">
    <app-tiers-payant-list … (selectionMode multiple, (tiersPayantSelected), (selectionChanged)) />
  </div>
  @if (panelOpen()) {
    <div class="detail-column">
      <app-tiers-payant-detail-panel [tiersPayant]="selectedTiersPayant()!" (closePanel)="…" (editRequested)="…" />
    </div>
  }
</div>
```

- Le filtre de statut comble directement l'écart §3 de l'analyse de l'écran de gestion.
- La barre d'actions groupées comble directement l'écart §4 (mise à jour groupée de facturation) ET
  ajoute la réactivation groupée, qui n'existait nulle part.
- Le bouton « Nouveau tiers payant » (split-button ASSURANCE/CARNET/DEPOT) reste identique — il
  ouvre toujours la modale complète `FormTiersPayantComponent`, qui garde son rôle de **création**.

### 2.2 Panneau détail (nouveau `TiersPayantDetailPanelComponent`)

Sur le patron de `ProduitDetailPanelComponent` : en-tête avec nom/code/badges (catégorie, statut),
bouton Modifier (ouvre la modale complète en édition, inchangée), puis onglets `ngbNav` :

| Onglet | Contenu (en `app-detail-grid` / `app-detail-section` / `app-detail-field`) | Comble l'écart |
|---|---|---|
| **Identité** | Nom, nom long, téléphone, email, NCC, code organisme, groupe — en lecture, édition via le bouton Modifier | — |
| **Facturation** | Délai règlement, périodicités, modèles de facture, inclusions auto — `app-detail-field format="montant"` pour les montants | formulaire §3 (sections reléguées hors du flux de création express) |
| **Plafonds & Remises** | Plafond conso/journalier organisme et clients, remise forfaitaire, taux de couverture par défaut (cf. proposition §9 du plan formulaire) | formulaire §7.1 |
| **Clients** | Liste des clients rattachés à **ce** tiers payant précis (filtre par `tiersPayantId`, pas seulement par catégorie large) | écran-gestion §5 — vrai manque de contenu comblé ici |
| **Factures** | Table filtrée du portefeuille `/facturation`, réutilisant `facture-api.service` avec `tiersPayantIds: [id]` déjà préremplis — **pas de redéveloppement**, un simple appel avec filtre fixé | écran-gestion §5 |
| **Règlements** | Idem, `reglement-api.service` filtré sur ce tiers payant | écran-gestion §5 |
| **Tarifs produits négociés** | Reprend le contenu de `ListPrixReferenceComponent`, actuellement ouvert en modale (`voirTarifsProduits()`) — devient un onglet du dossier plutôt qu'une fenêtre à part | cohérence de navigation |

### 2.3 Ce qui ne change pas
- `FormTiersPayantComponent` (la modale) reste l'écran de **création** et d'**édition complète** —
  ce document ne remet pas en cause son contenu, déjà traité dans le plan dédié.
- La création à la volée depuis le formulaire client assuré (« Ajouter un nouveau tiers-payant »)
  continue d'ouvrir cette même modale, inchangée.

---

## 3. Pourquoi ne pas tout fusionner dans le panneau détail (y compris la création)

Le panneau détail est fait pour **consulter/naviguer** un enregistrement déjà sélectionné : il
affiche des données déjà chargées (factures, règlements, clients), ce qui suppose un `id` existant.
La création reste un cas à part (formulaire vide, validations avant tout enregistrement) et garde
sa place naturelle en modale — exactement la même séparation que `produit-home` applique déjà
(`onNewProduit()` ouvre une modale de création, le panneau détail ne sert qu'à la consultation d'un
produit existant).

---

## 4. Risques et points à vérifier avant chiffrage

- **Onglets Factures/Règlements** : à confirmer que `facture-api.service`/`reglement-api.service`
  acceptent un filtre `tiersPayantIds` à un seul élément sans redévelopper leur logique de pagination
  (a priori oui, `recapitulatif.component.ts` l'utilise déjà avec `tiersPayantIds: this.selectedTiersPayants.map(t => t.id)`).
- **Onglet Clients par tiers payant précis** : nécessite un nouvel endpoint ou paramètre côté
  `CustomerDataService`/`AssuredCustomerResource` (aujourd'hui filtré par catégorie large
  uniquement, jamais par un `tiersPayantId`) — c'est le seul morceau qui demande un vrai
  développement back, le reste n'étant que de la réutilisation.
- **Droits d'accès** : vérifier que les onglets Factures/Règlements respectent les mêmes
  `RequiresNavAccess` que le module `/facturation` d'origine (un utilisateur qui voit la liste des
  tiers payants ne devrait pas forcément voir leurs règlements si ce n'est pas son rôle).

---

## 5. Effort estimé (vue d'ensemble, à affiner)

| Bloc | Effort |
|---|---|
| Écran maître : split-container, filtre statut, bulk-action-bar (réactivation + facturation groupée) | Moyen — essentiellement de la composition, patron déjà écrit |
| Panneau détail : coquille + onglets Identité/Facturation/Plafonds (données déjà disponibles) | Faible à moyen |
| Onglets Factures/Règlements (réutilisation filtrée) | Faible à moyen |
| Onglet Clients par tiers payant précis | Moyen (nouveau filtre back) |
| Onglet Tarifs produits (déplacement depuis la modale existante) | Faible |

---

## 6. Ce que ce document remplace dans les analyses précédentes

Les écarts suivants, déjà identifiés individuellement, sont **absorbés** par cette refonte plutôt
que corrigés isolément — les documents sources restent valides comme constat, mais leur
recommandation ponctuelle est remplacée par celle-ci :
- Écran-gestion §3 (réactivation), §4 (mise à jour groupée), §5 (dossier éclaté).
- Formulaire §7.1 (montants sans unité) pour la partie **consultation** — la modale de saisie
  elle-même garde son propre besoin de `suffix` sur `app-input-number`, non couvert ici.

