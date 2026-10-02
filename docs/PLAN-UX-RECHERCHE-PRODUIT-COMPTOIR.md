# Plan — Ergonomie de la recherche produit au comptoir

> Fichiers analysés :
> `pharmaSmart-app/src/main/webapp/app/features/sales/ui/product-search-section/` (wrapper d'écran)
> `pharmaSmart-app/src/main/webapp/app/features/sales/ui/product-search/` (combo + scan local)
> `pharmaSmart-app/src/main/webapp/app/shared/quantite-produt-saisie/` (saisie quantité)
> `pharmaSmart-app/src/main/webapp/app/features/sales/shared/mixins/product-handling.mixin.ts` (orchestration)
>
> Plan lié (ne pas dupliquer) :
> - [PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md](PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md)
>   §3 — grille de produits favoris/fréquents du comptoir. **C'est là, et uniquement là, que ce
>   besoin est traité** : un composant permanent et dédié (`ui/quick-products-grid`), visible en
>   continu à côté du champ recherche — pas une liste mélangée au dropdown de l'autocomplete. Voir
>   la note au §2.5 ci-dessous.

## Le problème en une phrase

La recherche produit est le point d'entrée de **chaque** ligne du panier, sur 5 écrans de vente
(COMPTANT, ASSURANCE, CARNET, devis, vente dépôt) — le flux scan→ajout est déjà solide et bien
architecturé, mais la **saisie manuelle** (clavier/souris) traîne encore des frictions évitables :
boutons codés mais jamais branchés, avertissements découverts après coup, et un champ texte qui
reste la seule porte d'entrée pour les produits à forte rotation.


---

## 1. Ce qui fonctionne déjà bien — à ne pas casser

### 1.1 Feedback scan (déjà fait, vérifié — **hors scope de ce plan**)

Le scan est **global** (douchette ou port série, capté au niveau page, pas dans un input), et c'est
déjà correctement traité à ce niveau-là, dans `sales-home.component.ts` :

- `searchAndDispatch()` (ligne ~715) distingue explicitement **4 cas** avec un bip + un toast
  dédiés à chacun :
  - 1 résultat → `scanAudio.beepSuccess()` + ajout direct.
  - 0 résultat → `scanAudio.beepError()` + `notificationService.error('Produit non trouvé : …')`.
  - plusieurs résultats (code ambigu) → `scanAudio.beepWarning()` + toast listant jusqu'à 3 libellés
    + mention explicite "le 1er a été ajouté — vérifiez la ligne".
  - erreur serveur / timeout → `scanAudio.beepError()` + message différencié (`TimeoutError` vs
    erreur générique).
- `armPendingScanTimer()` gère même le cas d'un scan reçu pendant un `loading()` en cours, avec un
  TTL et un abandon propre (bip + toast "Scan abandonné — chargement trop long").
- `ScanAudioFeedbackService` génère 3 tonalités distinctes (succès/erreur/avertissement) via
  `AudioContext`, sans fichier audio.

**Conclusion : ce point, cité dans une itération précédente de l'analyse comme une lacune, est en
réalité déjà couvert — et correctement situé à l'échelle globale, pas dans le composant input
local. Aucune action proposée ici.**

### 1.2 Fallback `salesScanner` null (déjà encadré — **hors scope de ce plan**)

Sur demande explicite, ce point n'est pas retouché dans ce plan. Toute action éventuelle sur le
scanner (local ou global) est volontairement exclue du périmètre ci-dessous.

### 1.3 Autres points forts constatés

- **Détection scan vs saisie manuelle** robuste dans `product-search.component.ts`
  (`ScanDetectorService` + `isManualSearching` + garde-fou anti-boucle de 2 s) : une douchette et
  une frappe rapide ne se marchent pas dessus.
- **Dropdown de résultats informatif** (`product-search.component.html`) : CIP, libellé, badge
  stock coloré (vert/orange/rouge + icône), badge réserve, prix — visible sans ouvrir la fiche
  produit.
- **Enchaînement clavier après sélection manuelle** : sélection → focus auto sur quantité
  (`onProductSelected` du mixin) → `Entrée` ajoute au panier → focus repart sur la recherche
  (`focusProductSearch`). Bon pattern "boucle sans souris".
- **Double usage de `Entrée` sur champ vide** (`onKeyEnter` → `onProductSearchEnter` dans
  `sale-creation.component.ts`) : si le panier a des lignes, ouvre directement le paiement avec
  focus CASH — raccourci de finalisation bien pensé, à ne pas toucher.
- **Garde-fou client requis** (`checkCustomerRequired`) centralisé dans le mixin, réutilisé par les
  5 écrans.
- **Alerte santé** (`AlerteSanteGuardService.verifier`) bloquant l'ajout *avant* tout envoi API en
  cas d'allergie connue du patient/ayant droit.

---

## 2. Lacunes identifiées (hors scan)

### 2.1 Boutons +/- de quantité : codés mais invisibles

`QuantiteProdutSaisieComponent` expose `incrementQuantity()` / `decrementQuantity()` (avec refocus
automatique), mais **aucun bouton dans le template** ne les appelle — seul le champ texte et le
bouton "Ajouter" (coché) existent. Fonctionnalité morte, alors qu'elle serait immédiatement utile
sur écran tactile comptoir.

### 2.2 Pas de feedback avant l'ajout si la quantité dépasse le stock

`product-search-section.component.html` affiche déjà le stock rayon/réserve dans `.product-meta`,
mais rien ne compare visuellement la quantité en cours de saisie à ce stock. Le caissier ne
découvre le problème qu'une fois la ligne ajoutée au panier (alerte `pharma-row-warning`), un cran
plus tard que nécessaire.

### 2.3 "Client requis" découvert tardivement, après la saisie

`checkCustomerRequired()` (dans le mixin) n'intervient qu'au moment de sélectionner un produit ou
de valider la quantité, via un toast d'avertissement *a posteriori*. Rien dans l'UI du champ
recherche ne signale, **avant** la frappe, qu'un client doit d'abord être choisi (vente
ASSURANCE/CARNET). Le caissier peut taper tout un produit avant de se faire recaler.

### 2.4 Pas d'assistance "sélection évidente" en saisie manuelle

Si une recherche manuelle ne retourne qu'un seul résultat, l'utilisateur doit quand même ouvrir le
dropdown et cliquer/`Entrée` dessus — pas d'auto-sélection quand le texte tapé correspond à un code
CIP exact et unique. Pour un comptoir qui tape parfois un CIP complet à la main (scanner HS, codeCIP
illisible), c'est une étape manuelle superflue.

### 2.5 Produits fréquents/récents — **traité dans l'autre plan, pas ici**

Un besoin réel existe (accélérer la vente des produits OTC à forte rotation sans recherche texte),
mais il **ne doit pas** être résolu en mélangeant une liste de suggestions au champ autocomplete —
une liste qui n'apparaît qu'au focus ou après une frappe partielle est moins visible et moins fiable
qu'un composant permanent.

Ce besoin est déjà analysé et cadré dans
[PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md §3](PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md#3--écart-n1--aucune-grille-de-produitsactions-rapides-touches-comptoir)
sous la forme d'une **grille de tuiles permanente** (`ui/quick-products-grid`), affichée en
continu à côté du champ recherche (pas à l'intérieur), alimentée par les favoris du poste ou le
top des ventes à 30 jours, et branchée sur le même pipeline d'ajout que le scan
(`onProductScanned()`). Toute implémentation de ce besoin doit se référer à ce plan-là, pas à
celui-ci.

### 2.6 Dropdown sans DCI/forme/dosage/péremption

Le template `#produitItem` (`product-search.component.html`) n'affiche que CIP, libellé, stock,
réserve et prix — aucune information sur la forme galénique, le dosage, le statut générique, ou une
péremption proche. Le caissier doit déjà "connaître" le produit pour confirmer visuellement qu'il
s'agit du bon.

### 2.7 Erreurs réseau de la recherche manuelle silencieuses

Dans `product-search.component.ts`, `loadProduits()` (recherche texte, hors scan) a un
`catchError(() => of({ body: [] }))` : une panne API et "aucun résultat" produisent exactement le
même dropdown vide, sans toast ni distinction. *(Ce point concerne la recherche manuelle texte,
pas le scan — donc dans le périmètre de ce plan, contrairement à 1.1.)*

---

## 3. Axes d'amélioration proposés (hors scan)

| Priorité | Axe | Détail | Pourquoi |
|---|---|---|---|
| **Haute** | Désactiver visuellement le champ recherche produit | Griser `app-product-search` + message inline ("Sélectionnez un client avant d'ajouter un produit") quand `requiresCustomer() && !hasCustomer()`, au lieu d'un simple toast après coup | Évite une saisie complète pour rien, erreur découverte immédiatement |
| **Haute** | Câbler les boutons +/- déjà codés | Ajouter 2 `app-button` dans `quantite-produt-saisie.component.ts` appelant `incrementQuantity()`/`decrementQuantity()` | Gain UX tactile quasi gratuit, code déjà présent et testé |
| **Moyenne** | Avertissement inline si quantité > stock disponible | Comparer en temps réel la valeur tapée dans `jhi-quantite-produt-saisie` au `selectedProduct()?.totalQuantity`, et styliser l'input en orange/rouge avant l'ajout | Anticipe la rupture au lieu de la découvrir dans le panier |
| **Moyenne** | Distinguer "aucun résultat" vs "erreur réseau" en recherche manuelle | Dans `loadProduits()`, propager un signal d'erreur distinct plutôt que `of({ body: [] })` systématique, et afficher un toast uniquement sur erreur réseau (pas sur "0 résultat", qui est un état normal) | Évite de confondre un vrai problème technique avec une recherche sans résultat |
| **Moyenne** | Auto-sélection sur correspondance CIP exacte unique | Si le texte tapé correspond exactement à un `codeCip` et qu'un seul résultat revient, sélectionner automatiquement sans attendre un clic | Réduit une étape manuelle répétée, utile en secours scanner |
| **Basse** | Enrichir le dropdown (DCI/forme/dosage/péremption) | Étendre `#produitItem` avec ces champs s'ils existent déjà côté `IProduit`/`ProduitSearch` (à vérifier), sinon prévoir leur exposition côté API dans une itération séparée | Accélère la reconnaissance visuelle du bon produit |

> ℹ️ Le besoin "produits fréquents/récents" **n'apparaît volontairement pas** dans ce tableau : il
> est couvert par la grille permanente du plan
> [PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md §3](PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md),
> pas par ce plan-ci.

---

## 4. Plan d'implémentation proposé (par lots, non régressif)

| Lot | Contenu | Effort | Risque |
|---|---|---|---|
| 1 | Désactivation visuelle + message inline "client requis" sur `app-product-search-section` | Faible — un `@if`/classe conditionnelle sur un input existant, pas de nouvelle logique métier | Faible |
| 2 | Boutons +/- sur `jhi-quantite-produt-saisie` | Faible — branchement de méthodes déjà écrites et testées | Faible |
| 3 | Avertissement inline stock insuffisant avant ajout | Moyen — nécessite un calcul réactif (`computed`) comparant quantité saisie et stock du produit sélectionné | Faible, purement additif/visuel |
| 4 | Distinction erreur réseau / 0 résultat en recherche manuelle | Faible/Moyen — ajouter un signal d'erreur + toast ciblé, sans changer le flux scan (hors scope) | Faible |
| 5 | Auto-sélection CIP exact unique | Moyen — logique à ajouter dans `loadProduits()`/`searchFn()`, à bien isoler du flux scan existant | Moyen (bien tester les interactions avec le debounce et `isManualSearching`) |
| 6 | Enrichissement dropdown (DCI/forme/péremption) | Moyen — dépend de la disponibilité des champs dans `ProduitSearch` | Moyen (à vérifier côté modèle avant de chiffrer) |

Recommandation : livrer les lots 1 et 2 en premier (effort minimal, gain immédiat, zéro risque sur
le flux scan), puis le lot 3 (anticipation rupture) qui a le plus d'impact sur la fiabilité du
service au comptoir. Les lots 4 à 6 peuvent suivre sans dépendance bloquante entre eux.

La grille de produits favoris/fréquents (besoin réel, mais hors périmètre de ce plan) suit son
propre séquencement dans
[PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md §9](PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md)
(priorité 1 de ce plan-là).

