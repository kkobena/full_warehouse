# Analyse comparative — Interface de vente (comptoir) vs logiciels d'officine du marché

> Statut : **analyse** — aucune ligne de code écrite.
> Date : octobre 2026.
> Portée analysée : `pharmaSmart-app/src/main/webapp/app/features/sales/feature/sales-home` et ses
> composants enfants (`sale-creation`, `sale-assurance`, `sale-carnet`, `sale-devis`, `ui/*`).
> Plans liés (ne pas dupliquer) :
> - [PLAN-EXTRACTION-ORDONNANCE-OCR.md](PLAN-EXTRACTION-ORDONNANCE-OCR.md) — lecture d'ordonnance,
>   interactions médicamenteuses, contre-indications (le contrôle de sécurité clinique est traité
>   **là-bas**, pas ici).
> - [PLAN-UX-PANIER-PRODUITS-COMPTOIR.md](PLAN-UX-PANIER-PRODUITS-COMPTOIR.md) — ergonomie détaillée
>   de la grille du panier (`ui/product-list`) : raccourcis clavier, colonne actions, affichage des
>   prix négociés par tiers payant.
> - [PLAN-UX-RECHERCHE-PRODUIT-COMPTOIR.md](PLAN-UX-RECHERCHE-PRODUIT-COMPTOIR.md) — ergonomie
>   détaillée du champ de recherche produit et de la saisie quantité (`ui/product-search`,
>   `ui/product-search-section`). **Ne traite pas** la grille de produits favoris — voir §3 ici.

---

## 1. Méthode

Comparatif fait à partir de l'usage quotidien **préparateur/caissier au comptoir** (pas pharmacien
titulaire, pas back-office) face à des éditeurs de référence du marché officinal francophone :
**Winpharma** (Cegedim/Pharmagest), **LGPI** (Pharmagest), **Smart Rx**, **Alliance Healthcare
(Leo/Pharmaland)**, **Covalia/Pharmagest**, et des solutions ouest-africaines de terrain (ex.
**PharmaSoft CI**, **Atlantic Pharma**) utilisées en Côte d'Ivoire (devise FCFA, pas de carte
Vitale, CMU/assurances privées locales).

Le périmètre existant a été vérifié fichier par fichier (`sales-home`, `sale-actions`,
`payment-mode`, `product-list`, `product-search`, `pending-sales-list`, `fiche-client-panel`,
`customer-overlay-panel`, `force-stock-choice-modal`, `retour-client`, `authorization.service.ts`,
`sales.facade.ts`, `keyboard-shortcuts.mixin.ts`). Résumé de l'existant : voir §2. Les écarts
identifiés : §3 à §8.

---

## 2. Ce qui existe déjà (ne pas refaire)

| Fonction | État | Fichier |
|---|---|---|
| Raccourcis F1-F11 (produit, quantité, client, remise, finaliser, attente, impression) | ✅ Complet | `shared/mixins/keyboard-shortcuts.mixin.ts` |
| Scanner HID/CDC auto, file anti-rebond, bips succès/erreur/ambigu | ✅ Solide | `sales-home.component.ts`, `SalesScannerService` |
| Multi-paiement (espèces, CB, Orange/Wave/Moov/MTN Money, virement, chèque) + rendu monnaie | ✅ Complet | `ui/payment-mode` |
| Mise en attente / reprise de vente, compteur + drawer | ✅ Fonctionnel mais perfectible (§6) | `ui/pending-sales-list` |
| Fiche client en panneau latéral (alertes santé, grossesse, crédit, avoirs, renouvellement chronique) | ✅ Riche | `ui/fiche-client-panel` |
| Remise ligne/globale, taux VO/VNO différencié | ✅ | `ui/product-list` |
| Forçage stock : rupture→avoir ou écart→régularisation | ✅ (voir plan dédié) | `force-stock-choice-modal` |
| Retour client (5 motifs, 3 modes de règlement) | ✅ (voir plan dédié) | `feature/retour-client` |
| Transformation COMPTANT ↔ ASSURANCE ↔ CARNET en cours de vente | ✅ Unique sur le marché, peu d'éditeurs le permettent à chaud | `sales-home.component.ts` |
| Plafond de vente avec indicateur header | ✅ | `salesFacade.plafondIsReached` |
| Impression ticket/facture (PDF web ou ESC/POS Tauri desktop) | ✅ | `data-access/services/print.service.ts` |
| Contrôle de privilèges fins (suppression ligne, remise, prix, forçage stock) avec déverrouillage superviseur | ✅ Supérieur à plusieurs concurrents qui ne gèrent qu'un mot de passe global | `authorization.service.ts` |
| Création client express (« Nouveau client » dans la table de recherche client, utilisée par le panneau de sélection comptoir et la modale des ventes différées/assurance/carnet/avoir) | ✅ Déjà présent — bouton toujours visible, ouvre `UninsuredCustomerFormComponent`, client créé auto-sélectionné dans la vente | `ui/customer-search-table/customer-search-table.component.ts` (`addNewCustomer()`), réutilisé par `ui/customer-overlay-panel` et `ui/customer-selection-modal` |

**Constat général :** le moteur de vente (paiement, remise, autorisations, stock, impression) est
mature, parfois plus rigoureux que la concurrence. Les écarts se situent presque tous dans
**l'ergonomie comptoir du quotidien** : ce qui fait gagner des secondes sur chaque client, pas ce
qui sécurise la transaction.

---

## 3. Écart n°1 — Aucune grille de produits/actions rapides (touches comptoir)

### Le constat

Tous les éditeurs cités proposent, à côté de la recherche produit, une **grille de boutons
tactiles/raccourcis** pour les produits sans ordonnance à très forte rotation : masques, gants,
préservatifs, paracétamol 1g boîte de 8, doliprane sirop, test de grossesse, eau physiologique,
etc. Le caissier clique une tuile au lieu de taper 4 caractères + valider dans l'autocomplete.

Ici, `product-search.component.ts` n'offre qu'un champ de recherche texte + scanner. Pour un
produit fréquent sans code-barres à portée (ex. un conseil comptoir, pas de boîte scannée), c'est
une recherche complète à chaque fois — plus lent, et plus propice à sélectionner la mauvaise
référence dans une liste de résultats proches.

### Pourquoi c'est un manque quotidien

Le préparateur en conseil comptoir gère souvent **plusieurs petits articles** à la suite
(parapharmacie, hygiène, DM) sans ordonnance ni code-barres sorti — c'est le flux le plus rapide
possible chez les concurrents, et le plus lent ici.

### Piste d'implémentation

1. Nouveau composant `ui/quick-products-grid` : grille configurable de tuiles (image, libellé
   court, prix), alimentée par une liste **« produits favoris du poste »** stockée par magasin
   (nouvelle entité `ProduitFavori { magasinId, produitId, ordre, couleur? }` côté backend, CRUD
   simple) ou — version minimale sans backend — top N produits par fréquence de vente sur
   30 jours, calculé côté `ProduitService` (`GET /api/produits/favoris?magasinId=`).
2. Affichage dans `sale-creation`/`sale-devis` (onglet comptant) : panneau latéral ou zone sous le
   champ recherche, repliable (`sidebarCollapsed`-like), activable/désactivable par préférence
   utilisateur.
3. Un clic = équivalent de `onProductScanned()` déjà existant (réutiliser le pipeline de dispatch
   de `sales-home.component.ts`, pas de nouveau code de gestion de ligne).
4. Admin : écran simple (titulaire/responsable) pour épingler/dépingler un produit depuis la fiche
   produit (bouton « Ajouter aux favoris comptoir »).

**Effort estimé : moyen.** Gain ergonomique élevé, risque faible (n'ajoute qu'un raccourci vers un
pipeline existant).

---

## 4. Écart n°2 — Pas de substitution générique proposée en rupture de stock

### Le constat

La table `substitut` existe déjà côté domaine (mentionnée dans `PLAN-PRODUIT-DCI-N-N.md` comme
« table indépendante… alimentée par PharmaML et la fusion de produits ») mais **n'est appelée nulle
part dans le module `sales`** (confirmé : aucune occurrence de `substitut` dans
`features/sales/**`).

Chez les concurrents (Winpharma, LGPI), dès qu'un produit est en stock 0 ou insuffisant, une
icône/bouton « Équivalents » ouvre la liste des génériques/substituts disponibles en stock, avec
prix comparé — décision prise en quelques secondes devant le client, sans l'envoyer voir ailleurs.

### Pourquoi c'est un manque quotidien

C'est la situation la plus fréquente et la plus stressante au comptoir : « je n'ai plus ce
produit, qu'est-ce que je propose ? ». Aujourd'hui le préparateur doit quitter l'écran de vente ou
chercher de mémoire.

### Piste d'implémentation

1. Backend : endpoint `GET /api/produits/{id}/substituts?magasinId=` qui interroge la table
   `substitut` existante et filtre par stock > 0 dans le magasin courant (réutiliser le DTO
   `ProduitSearch` déjà utilisé par le scanner).
2. Dans `force-stock-choice-modal` (déjà affiché quand le stock est insuffisant) : ajouter une
   **troisième option** « Proposer un équivalent disponible » à côté de « Rupture → avoir » et
   « Écart d'inventaire ». Sélection → remplace la ligne par le substitut choisi via le même
   pipeline d'ajout de ligne (pas de nouvelle mutation de vente).
3. Un bouton « Équivalents » discret sur chaque ligne de `product-list.component` même hors
   rupture (le client demande parfois un équivalent moins cher).

**Dépendance :** la qualité du résultat dépend du nettoyage DCI n-n du plan
`PLAN-PRODUIT-DCI-N-N.md` (lots 3/4) — mais la table `substitut` actuelle suffit pour une v1.

**Effort estimé : moyen.**

---

## 5. Écart n°3 — Pas d'ouverture manuelle du tiroir-caisse

### Le constat

`CashRegisterService` ne gère que l'ouverture/fermeture de la **session de caisse** (jour
comptable), pas l'ouverture physique du tiroir. Aucun bouton « Ouvrir le tiroir » dans
`sales-home.component.html`.

### Pourquoi c'est un manque quotidien

Rendre la monnaie sur un paiement en espèces hors vente (appoint, dépannage), vérifier le fond de
caisse en début de service, ou ouvrir le tiroir sans vente (ex. remise d'un bon d'achat) sont des
gestes quotidiens. Sans bouton dédié, le caissier doit forcer une fausse vente à 0 pour ouvrir le
tiroir — contournement observé chez plusieurs clients de logiciels concurrents avant l'ajout de ce
bouton.

### Piste d'implémentation

1. Si le tiroir est piloté par l'imprimante ticket (cas standard ESC/POS, signal au même port série
   que l'impression) : ajouter une commande `print.service.ts` → `openCashDrawer()` (trame ESC/POS
   `27 112 0 25 250`), appelée depuis un bouton header `pi pi-inbox` à côté du badge scanner.
2. Journaliser l'ouverture manuelle (qui, quand) dans un nouveau log léger `cash_drawer_open_log`
   pour traçabilité anti-fraude — point sensible en officine.
3. Restreindre par privilège (`PR_OPEN_CASH_DRAWER`) pour éviter l'ouverture libre par tout profil.

**Effort estimé : faible** (si le matériel ESC/POS est déjà piloté par `print.service.ts`, sinon
dépend du pilote imprimante en place).

---

## 6. Écart n°4 — Ventes en attente peu visibles (drawer seul, pas de bandeau)

### Le constat

`pending-sales-list` n'est accessible que via un drawer plein écran (F11 ou bouton « En attente »).
Les logiciels concurrents affichent souvent un **bandeau de tickets suspendus** en permanence
(mini-onglets en bas ou à droite de l'écran de vente), permettant de jongler entre 2-3 clients en
simultané (un client cherche son portefeuille pendant qu'on sert le suivant) sans ouvrir un drawer
plein écran à chaque fois.

### Piste d'implémentation

1. Ajouter un **bandeau compact** (max 5 pastilles) sous le header, alimenté par la même donnée que
   `countPendingSales`/`loadPendingSalesCount` (`apiService.countPendingSales` → étendre pour
   retourner aussi les 5 dernières ventes en attente, pas seulement le total).
2. Clic sur une pastille = reprise directe (réutilise `onSaleResumed`/`resumePendingSaleSuccess$`
   déjà câblés), sans ouvrir le drawer.
3. Le drawer complet reste disponible (F11) pour la recherche/filtre vendeur sur un grand nombre de
   ventes en attente.

**Effort estimé : faible-moyen.** Amélioration purement UI, aucune nouvelle logique métier.

---

## 7. Écart n°5 — Pas de mode dégradé en cas de coupure réseau

### Le constat

Le pipeline de scan (`searchAndDispatch`), la recherche produit, le paiement et la finalisation
dépendent tous d'appels HTTP synchrones. Aucune file d'attente locale ni mode offline détecté dans
`sales-home.component.ts` ou `sales.facade.ts`. En contexte ouest-africain (connectivité moins
fiable qu'en métropole), une coupure réseau bloque intégralement le comptoir.

### Piste d'implémentation (ambitieuse, à phaser)

1. **Phase 1 — minimal :** détecter la perte réseau (`navigator.onLine` + ping périodique léger) et
   afficher un bandeau d'alerte explicite au lieu de messages d'erreur HTTP bruts (« Erreur de
   recherche produit ») — déjà un progrès pour le diagnostic du caissier.(A ne pas faire pour )
2. **Phase 2 — cache produit local :** l'app desktop (Tauri) a accès à un stockage local ; mettre en
   cache la table produits/prix/stock du jour (déjà interrogée en lecture) pour permettre une
   recherche + ajout de ligne hors-ligne, avec synchronisation différée de la vente à la reconnexion
   (file de ventes en attente d'envoi, horodatées).
3. Hors périmètre immédiat du front seul : nécessite une réflexion architecture backend
   (idempotence, conflits de stock) — à cadrer dans un plan séparé si retenu.

**Effort estimé : élevé.** À ne traiter qu'après validation terrain du besoin réel (fréquence des
coupures constatées chez les clients).

---

## 8. Écarts mineurs (liste courte, effort faible)

| # | Écart | Piste |
|---|---|---|
| 8.1 | Pas de suggestion de **vente associée** (ex. ajout pansement avec désinfectant) | Table de règles simples `produit_associe(produit_id, produit_suggere_id)`, bandeau discret dans `product-list` après ajout d'une ligne |
| 8.2 | Pas d'affichage de la **date de péremption du lot servi** pendant la vente (FEFO silencieux) | `product-search`/`product-list` : afficher la péremption du lot qui sera sorti (donnée déjà connue côté `SalesLineServiceImpl` FEFO) ; alerte visuelle si < 3 mois |
| 8.3 | Pas d'impression d'**étiquette prix/code-barres** depuis la vente pour un produit déconditionné sans étiquette | Réutiliser `print.service.ts`, nouveau template étiquette format rouleau |
| 8.4 | Pas de **stock réseau** (autre officine du groupe) visible en cas de rupture locale | `showStock` ne montre que le magasin courant ; étendre l'appel stock avec un paramètre multi-magasin si l'organisation en a plusieurs (hors périmètre mono-officine) |
| 8.5 | Pas de **programme de fidélité** (points, remise palier) | Nouvelle entité `CarteFidelite`/`PointFidelite`, calcul à l'encaissement, affichage solde dans `fiche-client-panel` |

---

## 9. Priorisation proposée

| Priorité | Écart | Effort | Impact quotidien comptoir |
|---|---|---|---|
| 1 | §3 Grille produits favoris | Moyen | Très élevé (gain de temps sur chaque vente OTC) |
| 2 | §4 Substitution générique en rupture | Moyen | Très élevé (situation quotidienne stressante) |
| 3 | §5 Ouverture tiroir-caisse | Faible | Élevé (sécurité/traçabilité) |
| 4 | §6 Bandeau ventes en attente | Faible-Moyen | Moyen |
| 5 | §8.2 Péremption lot affichée | Faible | Moyen (conformité) |
| 6 | §8.1 Vente associée | Faible | Moyen (chiffre d'affaires) |
| 7 | §8.3 Étiquette prix | Faible | Faible-Moyen |
| 8 | §7 Mode dégradé réseau | Élevé | Variable (dépend du terrain) |
| 9 | §8.4 Stock réseau, §8.5 Fidélité | Moyen-Élevé | Dépend du modèle commercial de l'officine |

---

