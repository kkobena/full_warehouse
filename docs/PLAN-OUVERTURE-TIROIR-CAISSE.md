# Plan — Ouverture du tiroir-caisse

> Statut : **plan** — aucune ligne de code écrite. Rédigé le 2026-10-03.
> Origine : écart n°3 de [PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md](PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md) §5,
> sorti dans un plan dédié parce que le matériel, la traçabilité et les droits en font un chantier à part entière.

## 1. Le besoin

Rendre la monnaie hors vente (appoint, dépannage), vérifier le fond de caisse en début de service, remettre un bon d'achat : des gestes
quotidiens qui demandent d'ouvrir le tiroir **sans vente**. Sans bouton dédié, le caissier ouvre une fausse vente à zéro pour déclencher le
tiroir : un contournement qui salit la caisse et qu'on ne peut pas contrôler.

Deux besoins distincts, à ne pas confondre :

| | Ouverture **manuelle** | Ouverture **automatique** |
|---|---|---|
| Déclencheur | un bouton, par choix du caissier | un encaissement en espèces, à l'impression du ticket |
| Risque | fraude (tiroir ouvert sans trace) | tiroir qui s'ouvre sans raison (carte, mobile money) |
| Traçabilité | indispensable (qui, quand, pourquoi) | portée par la vente elle-même |

## 2. Ce que le code fait aujourd'hui (relevé le 2026-10-03)

- **Aucune commande d'ouverture de tiroir nulle part** : ni côté Java, ni côté Rust, ni côté Angular. `CashRegisterService` ne gère que la
  **session** de caisse (ouverture et fermeture du jour comptable), pas le tiroir physique.
- **Chaîne d'impression ESC/POS** : le serveur fabrique les octets du ticket (`AbstractJava2DReceiptPrinterService`, `generateEscPosReceipt`
  dans `AbstractSaleReceiptService` et ses dérivés : comptant, assurance, dépôt, différé, mouvement de caisse, ticket Z…), l'écran les reçoit
  (`SalesDataResource`, `getEscPosReceiptForTauri`), puis `TauriPrinterService.printEscPos` les passe à la commande Tauri `print_escpos`
  (`src-tauri/src/printer.rs`), qui les écrit **en mode RAW** dans la file d'impression Windows (`WritePrinter`).
- **Windows seulement** : `print_escpos` répond « not implemented for this platform » ailleurs.
- **Hors Tauri (navigateur)** : l'écran ne sait produire qu'un PDF ; aucun accès aux octets de l'imprimante. L'ouverture du tiroir n'y est donc
  **pas réalisable depuis le poste**.
- **Droits** : un privilège est une ligne `nav_item` de niveau 3 (`pr-…`) avec ses rôles dans `nav_item_role`, posée par une migration Flyway ;
  l'écran le lit par `AuthorizationService`. Modèle : `pr-forcer-alerte-sante` (V2.1.13). Tout endpoint `/api/**` doit porter
  `@RequiresNavAccess` ou `@NavAccessExempt`.

## 3. Conception

### 3.1 La commande
La commande standard est **ESC p** : `1B 70 m t1 t2`. `m` choisit la broche du connecteur (0 ou 1), `t1` et `t2` règlent l'impulsion en
multiples de 2 ms. Valeur courante : `1B 70 00 19 FA` (50 ms d'impulsion, 500 ms de repos). Certains modèles préfèrent `10 14 01 00 01`
(DLE DC4). **Le choix est donc un réglage**, pas une constante : clé de configuration (octets en hexadécimal) avec `1B 70 00 19 FA` par défaut.

### 3.2 Ouverture manuelle
- Un bouton « Ouvrir le tiroir » (icône `pi pi-inbox`) à l'écran de vente, **à côté du bandeau**, sans rien ajouter au bandeau lui-même
  (contrainte : ni marge, ni padding, ni bordure en plus).
- Il **demande un motif** : appoint, vérification du fond de caisse, bon d'achat, autre (commentaire libre). Sans motif, pas d'ouverture :
  c'est ce qui rend le geste contrôlable.
- Tauri : l'écran envoie les octets de la commande à `print_escpos`. L'imprimante cible est celle déjà choisie pour les tickets du poste.
- Navigateur : le bouton est **grisé avec la raison** (« disponible dans l'application bureau »), plutôt que caché ou cassé.

### 3.3 Ouverture automatique
- À l'impression du ticket d'une vente **réglée au moins en partie en espèces**, le serveur ajoute la commande d'ouverture **après la découpe**
  du ticket (les octets sont déjà fabriqués côté serveur : un seul endroit à modifier, `AbstractJava2DReceiptPrinterService`).
- Réglage `APP_OUVRIR_TIROIR_ESPECES` (oui / non), car certains postes ont un tiroir câblé directement à l'imprimante **avec ouverture au
  pilote** : l'ouvrir deux fois serait un défaut.
- Un règlement CB, mobile money, virement ou chèque n'ouvre rien. Un règlement mixte avec espèces ouvre.

### 3.4 Traçabilité
Nouvelle table `cash_drawer_open_log` (nouvelle migration Flyway, jamais une migration existante modifiée) :
`id`, `magasin_id`, `cash_register_id` (la session en cours), `user_id`, `ouvert_le`, `source` (`MANUEL` / `AUTO_ESPECES`), `motif`
(manuel seulement), `commentaire`, et le lien vers la vente pour l'ouverture automatique (identifiant composé `id` + `sale_date`, la table
`sales` étant partitionnée : pas de clé étrangère simple, à trancher comme pour les autres tables qui la référencent).
- Le journal est **en écriture seule** pour l'application ; aucune suppression.
- Le **ticket Z / clôture de caisse** affiche le nombre d'ouvertures manuelles et leurs motifs : c'est ce qu'un responsable regarde pour
  repérer un usage anormal.

### 3.5 Droits
- Nouveau privilège `pr-ouvrir-tiroir-caisse` (niveau 3), rôles de départ : `ROLE_ADMIN`, `ROLE_PHARMACIEN`. **Pour le caissier : à décider**
  (§6) — l'ouvrir sans droit défait le contrôle, mais le lui refuser le renvoie à la fausse vente à zéro.
- Endpoint `POST /api/cash-register/tiroir/ouverture` (journalise ; l'envoi des octets à l'imprimante reste côté poste) avec
  `@RequiresNavAccess` sur le code du privilège.
- Une ouverture exige une **caisse ouverte** pour l'utilisateur : sans session, rien à rattacher.

## 4. Matériel et pièges

| Point | Pourquoi |
|---|---|
| Le tiroir se branche sur **l'imprimante** (prise RJ11/RJ12), pas sur le PC | l'ouverture passe par l'imprimante ticket ; si elle est éteinte ou hors ligne, rien ne s'ouvre |
| Le **pilote Windows** peut avaler ou dupliquer l'impulsion | beaucoup de pilotes ont une option « ouvrir le tiroir avant / après impression » : à désactiver si on envoie ESC p soi-même |
| Broche 2 ou broche 5 | selon le câblage du tiroir ; d'où le réglage de `m` |
| Impulsion trop courte | un tiroir ancien ne s'ouvre pas ; trop longue, l'électroaimant chauffe |
| Postes multiples | chaque poste a son imprimante et son tiroir ; le réglage est **par poste** (côté application bureau), pas par magasin |
| Délai de la file d'impression | l'ouverture suit le travail d'impression : quelques centaines de millisecondes de retard sont normales |

Un **ticket vierge** n'est pas nécessaire : la commande seule, envoyée en RAW, déclenche l'impulsion sans rien imprimer.

## 5. Phases

| Phase | Contenu | Critère de sortie | Effort |
|---|---|---|---|
| **0** | Valider les questions du §6 ; relever, sur un poste réel, le modèle d'imprimante, le câblage du tiroir et le réglage du pilote | réponses notées ici ; commande validée sur le matériel | 0,5 j (+ passage sur place) |
| **1** | Commande Tauri dédiée (`ouvrir_tiroir`) réutilisant `send_raw_to_printer`, réglage des octets, test Rust | une ouverture depuis l'application bureau, sur le matériel réel | 1 j |
| **2** | Table `cash_drawer_open_log`, privilège, endpoint, service, test d'intégration (droits, caisse fermée, journal) | l'ouverture est refusée sans droit ni caisse, journalisée sinon | 1,5 j |
| **3** | Bouton, fenêtre de motif, état « indisponible » hors Tauri, tests Jest | geste complet à l'écran ; bouton grisé avec sa raison hors Tauri | 1 j |
| **4** | Ouverture automatique à l'impression d'un ticket avec espèces, réglage, test sur les tickets comptant et dépôt | un règlement en espèces ouvre une fois, un règlement par carte n'ouvre pas | 1 j |
| **5** | Ticket Z : nombre et motifs des ouvertures manuelles | visible à la clôture | 0,5 j |

Total : **environ 5,5 jours**, dont une demi-journée sur un poste réel qu'aucun test automatique ne remplace.

## 6. Questions à trancher
1. **Matériel** : quels modèles d'imprimante ticket et de tiroir sur les postes ? Tous sous Windows ? (Le code actuel n'imprime en ESC/POS que sous Windows.)
2. **Automatique** : veut-on l'ouverture à chaque règlement en espèces ? Les pilotes en font-ils déjà une partie ?
3. **Droit** : qui peut ouvrir à la main — le caissier aussi, ou seulement le pharmacien et l'administrateur ?
4. **Motifs** : la liste du §3.2 convient-elle, ou en faut-il d'autres (échange de monnaie avec un autre commerce, par exemple) ?
5. **Navigateur** : les postes en navigateur existent-ils ? Si oui, l'ouverture y est impossible tant que le tiroir est branché au poste ; la seule voie
   serait une imprimante pilotée par le serveur, hors périmètre ici.

## 7. Hors périmètre
- Le contrôle du **contenu** du tiroir (comptage, écarts de caisse) : il relève de la clôture de caisse existante.
- Les tiroirs **sans imprimante** (branchés en USB ou en série directement sur le poste).
- L'ouverture à distance depuis le serveur.
