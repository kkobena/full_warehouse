# Analyse comparative — Formulaire tiers payant (`FormTiersPayantComponent`) vs logiciels d'officine du marché

> Statut : **analyse** — aucune ligne de code écrite.
> Date : octobre 2026.
> Portée analysée : `pharmaSmart-app/src/main/webapp/app/entities/tiers-payant/form-tiers-payant/`
> + points d'entrée : `tiers-payant.component.ts` (gestion référentielle : boutons « ASSURANCE »,
> « CARNET », « DEPOT », édition en liste) et création à la volée depuis
> `assure-step.component.ts`/`complementaire-step.component.ts` (cf. analyse du formulaire client
> assuré) + chaîne back : `TiersPayantResource`, `TiersPayantServiceImpl`, `TiersPayantDto`.
> Méthode et référentiel marché identiques aux analyses précédentes :
> [PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md](PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md),
> [PLAN-ANALYSE-COMPARATIVE-FORMULAIRE-CLIENT-ASSURE.md](PLAN-ANALYSE-COMPARATIVE-FORMULAIRE-CLIENT-ASSURE.md) —
> comparatif fait du point de vue **comptoir et back-office officine**, face à Winpharma, LGPI,
> Smart Rx, Alliance Healthcare, Covalia, et aux solutions ouest-africaines de terrain (PharmaSoft
> CI, Atlantic Pharma).

---

## 1. Ce que fait le formulaire aujourd'hui

Un seul formulaire dense (6 cartes réparties en une grille 2 colonnes puis 3 colonnes), utilisé à
**deux contextes très différents** sans aucune adaptation :

1. **Création à la volée** pendant la saisie d'un client assuré (« Ajouter un nouveau
   tiers-payant » depuis la recherche d'organisme) — un caissier/pharmacien doit aller vite.
2. **Gestion référentielle complète** depuis l'écran « Tiers payants » (boutons dédiés par
   catégorie ASSURANCE/CARNET/DEPOT, ou édition d'une fiche existante) — tâche d'administration,
   généralement faite par un profil back-office.

Le formulaire comprend : identité (nom abrégé, nom long, téléphone, email — tous obligatoires),
informations générales (NCC, groupe tiers payant, code organisme), paramètres de facturation
(délai de règlement, nombre de bordereaux, montant maxi par facture, modèle de facture, périodicité
facture définitive/provisoire avec inclusion auto), plafonds et remises (remise forfaitaire,
plafond consommation, plafond absolu), et plafonds clients (uniquement catégorie ASSURANCE).

---

## 2. Ce qui existe déjà et va au-delà de ce que proposent certains concurrents

| Fonction | État | Détail |
|---|---|---|
| Détection de doublon serveur à la création **et** à la modification | ✅ | `TiersPayantServiceImpl.createFromDto`/`updateFromDto` vérifient nom, nom long **et** code organisme — contrairement au formulaire client assuré qui n'a aucun contrôle équivalent |
| Paramétrage fin de la facturation par organisme (périodicité définitive/provisoire distinctes, inclusion/exclusion de la génération automatique, modèle et ordre de tri de facture) | ✅ Plus riche que la plupart des logiciels locaux qui n'ont souvent qu'un taux de remboursement global | `periodicitesOptions`, `inclureFacturationAutoDefinitive/Provisoire` |
| Double niveau de plafonnement (plafond de l'organisme **et** plafond propre à chaque client de cet organisme) | ✅ Distinction peu courante chez la concurrence low-cost | Section « Plafonds et Remises » + « Plafonds Clients » |
| Affichage conditionnel selon la catégorie (ex. section « Plafonds Clients » et périodicités masquées pour DEPOT) | ✅ Évite d'afficher des champs non pertinents pour un dépôt-vente | `@if (categorie === 'ASSURANCE')` / `@if (categorie !== 'DEPOT')` |
| Phrase explicative dynamique sous le commutateur d'inclusion automatique (« Inclus — génération MENSUEL » / « Exclu de la génération automatique ») | ✅ Bon réflexe d'ergonomie, rare même chez les éditeurs établis | Lignes 202-255 du template |

**Constat général :** côté paramétrage métier de la facturation tiers payant, ce formulaire est
**plus complet** que la moyenne du marché local. L'essentiel des écarts porte sur l'usage
**quotidien au comptoir** (le même écran sert à deux publics très différents sans s'adapter) et
sur des contrôles de cohérence/qualité de données qui existent ailleurs dans l'app mais pas ici.

---

## 3. Écart n°1 — Un seul formulaire dense pour deux usages radicalement différents

### Le constat
Que l'on arrive via « Ajouter un nouveau tiers-payant » en pleine vente assurance (contexte
comptoir, urgent) ou via le bouton « ASSURANCE » de l'écran de gestion référentielle (contexte
back-office, posé), c'est **exactement le même formulaire à 6 cartes et ~20 champs** qui s'ouvre.
Rien ne distingue les deux usages : pas de mode « rapide » limité à l'identité de l'organisme, pas
de repli automatique des sections de paramétrage facturation (délai de règlement, périodicité,
modèle de facture, plafonds) qui n'ont aucune utilité immédiate pour un caissier pressé de finir sa
vente.

### Pourquoi c'est un écart avec les usages courants
Les logiciels de référence qui permettent une création de tiers payant « à chaud » pendant une
vente limitent systématiquement la saisie à l'identité minimale (nom, contact) et renvoient le
paramétrage de facturation à un écran d'administration distinct, accessible plus tard sans bloquer
la vente en cours. Ici, le caissier doit soit tout remplir (y compris des champs de facturation
qu'il ne maîtrise pas forcément — délai de règlement contractuel, modèle de facture...), soit
laisser des valeurs par défaut dont il ne connaît pas la portée, dans un contexte où il n'a ni le
temps ni la légitimité métier de les décider.

### Ce qu'il faudrait
Faire porter un mode « création express » (nom, nom long, téléphone, email, catégorie — rien de
plus) quand le formulaire est ouvert depuis le contexte vente/client, les autres sections restant
accessibles uniquement depuis la fiche complète en gestion référentielle.

---

## 4. Écart n°2 — Email obligatoire pour un organisme tiers payant, hypothèse pas toujours vraie

### Le constat
`email` est à la fois `Validators.required` et `Validators.email`. Aucune organisation ne peut être
enregistrée sans adresse email valide.

### Pourquoi c'est un écart avec les usages courants
Beaucoup de petites mutuelles locales, caisses d'entreprise ou antennes régionales de la CNPS
fonctionnent encore principalement par téléphone ou courrier, sans adresse email dédiée et
consultée. Forcer ce champ pousse l'utilisateur à saisir une adresse de complaisance
(`contact@xxx.ci`, parfois fictive) uniquement pour pouvoir valider — ce qui dégrade la qualité du
fichier tiers payant exactement de la même façon que le téléphone obligatoire dégrade le fichier
client comptant (cf. analyse du formulaire comptant, écart similaire).

### Ce qu'il faudrait
Rendre `email` facultatif (garder `Validators.email` pour valider le format si renseigné), le
téléphone suffisant comme contact minimal obligatoire.

---

## 5. Écart n°3 — Pas de recherche préalable à la création depuis l'écran de gestion référentielle

### Le constat
Les boutons « ASSURANCE »/« CARNET »/« DEPOT » de `tiers-payant.component.ts` ouvrent directement un
formulaire **vide**. Le contrôle de doublon n'intervient qu'au moment de l'enregistrement, après
que l'utilisateur ait rempli l'intégralité des champs obligatoires. À l'inverse, le même formulaire
ouvert **depuis la fiche client assuré** passe d'abord par une recherche (`app-select-search` sur le
nom de l'organisme) et ne propose la création que si rien n'est trouvé.

### Pourquoi c'est un écart avec les usages courants
C'est une incohérence entre les deux points d'entrée du même formulaire : le chemin le plus
« administratif » (gestion référentielle, où l'utilisateur a justement le temps de chercher
d'abord) saute l'étape de recherche que le chemin le plus pressé (vente en cours) respecte déjà.
Les logiciels de référence appliquent la règle inverse : chercher avant de créer est précisément le
réflexe que l'on attend d'un profil back-office qui structure un référentiel, pour éviter les
doublons de saisie (« Mutuelle Fraternité » vs « MUTUELLE FRATERNITE » vs « Fraternité Mutuelle »)
qu'un simple contrôle exact sur le nom ne rattrape pas toujours.

### Ce qu'il faudrait
Faire précéder les 3 boutons de création d'une recherche identique à celle déjà implémentée côté
client assuré, réutilisable telle quelle.

---

## 6. Écart n°4 — Identifiant fiscal (NCC) ni unique, ni validé en format

### Le constat
`ncc` (numéro de compte contribuable, identifiant fiscal ivoirien) est un simple champ texte libre,
sans contrôle d'unicité côté serveur (contrairement au nom/nom long/code organisme, qui eux sont
vérifiés) et sans validation de format alors que `codeOrganisme` a lui une contrainte `@Pattern`
alphanumérique.

### Pourquoi c'est un écart avec les usages courants
Le NCC est par nature un identifiant **unique et officiel** par entité — c'est en pratique la clé la
plus fiable pour détecter qu'un même organisme est ressaisi sous un nom légèrement différent (ce que
le contrôle actuel sur nom/nom long ne détecte pas, cf. §5). Les logiciels de gestion qui
interagissent avec la facturation/fiscalité (even indirectement, via les factures tiers payant déjà
gérées ici — modèle de facture, NCC présent précisément pour ça) valident généralement ce type de
numéro officiel, au moins en format, et souvent en unicité.

### Ce qu'il faudrait
Ajouter l'unicité du NCC au contrôle de doublon existant (aux côtés de nom/nom long/code organisme),
et un format de validation de base (le NCC ivoirien suit un schéma connu).

---

## 7. Ergonomie — facilité d'utilisation au quotidien

### 7.1 Montants et plafonds sans borne minimale ni unité affichée
`nbreBordereaux`, `montantMaxParFcture`, `remiseForfaitaire`, `plafondConso`,
`plafondJournalierClient`, `plafondConsoClient` sont tous des `app-input-number` **sans `[min]`**
(seul `delaiReglement` a `[min]="0"`) — rien n'empêche de saisir un plafond ou un montant négatif,
ce qui n'a aucun sens métier. Par ailleurs, `app-input-number` supporte un `suffix` d'affichage
(documenté dans son propre composant, ex. `suffix=" F CFA"`), mais aucun des champs monétaires ici
ne l'utilise : l'utilisateur doit deviner si « Montant maxi par facture » est en FCFA, en milliers,
etc. Les logiciels de référence affichent systématiquement l'unité à côté d'un montant.

### 7.2 Pas d'aide contextuelle sur des champs techniques peu familiers
« Code organisme » et « Identifiant contribuable » (NCC) sont deux champs voisins sans infobulle ni
texte d'aide expliquant leur rôle respectif ni leur format attendu. Pour un utilisateur comptoir qui
n'a pas une familiarité quotidienne avec l'administration fiscale/les codes de télétransmission,
cette ambiguïté ralentit la saisie ou produit des valeurs incorrectes silencieusement acceptées
(aucune validation de format autre que le `@Pattern` alphanumérique générique du code organisme).

### 7.3 Catégorie du tiers payant invisible dans le formulaire
`categorie` pilote plusieurs sections conditionnelles (plafonds clients, périodicités de
facturation) mais n'apparaît **nulle part visuellement** dans le formulaire : elle est injectée en
amont par le composant appelant (`categorie: "ASSURANCE"`, etc.) et reste une propriété interne
muette. Un utilisateur qui ouvre la fiche d'un tiers payant existant et constate l'absence de la
section « Plafonds Clients » n'a aucun indice expliquant pourquoi (parce que c'est un DEPOT) — la
règle métier est invisible, ce qui nuit à la compréhension du formulaire pour un nouvel utilisateur.

### 7.4 Liste des groupes tiers payant chargée une fois, sans recherche
`groupeTiersPayants` est peuplée par une seule requête (`queryPromise({search: ''})`) à
l'initialisation, puis affichée dans un `app-select` simple — pas de typeahead serveur comme pour le
tiers payant lui-même. Si le nombre de groupes croît (regroupements par branche, par réseau
d'agences...), la liste déroulante deviendra longue à parcourir sans moyen de filtrer en tapant.

### 7.5 Pas de sauvegarde/avertissement de perte de saisie
Pour un formulaire aussi dense (6 cartes, souvent rempli en plusieurs minutes par un profil
back-office qui configure soigneusement la facturation d'un organisme), une fermeture accidentelle
de la modale (clic sur le fond, touche `Échap`) ne déclenche aucune confirmation — la perte de
saisie potentielle est plus coûteuse ici que pour n'importe quel autre formulaire client déjà
analysé, puisque c'est le plus long des trois.

### 7.6 Message d'erreur de doublon : faute de frappe visible par l'utilisateur
Le message serveur renvoyé en cas de doublon contient une coquille : « *…le même nom ou le code
orgasisme* » (« orgasisme » pour « organisme »). Détail mineur mais visible à chaque doublon détecté
— à corriger pour l'image de sérieux du produit.

### 7.7 Dette d'accessibilité commune aux autres formulaires du dossier client
Comme pour les formulaires client comptant et client assuré déjà analysés : pas de composant
`app-form-field` réutilisé, pas d'`aria-describedby`/`role="alert"` sur les messages d'erreur. Même
dette transverse, un seul correctif sur le composant partagé bénéficierait aux trois formulaires.

---

## 8. Synthèse priorisée

| # | Constat | Impact usage quotidien | Effort estimé |
|---|---|---|---|
| §3 | Même formulaire dense pour création express (comptoir) et gestion référentielle (back-office) | Élevé — ralentit le geste le plus fréquent (vente en cours) | Moyen (mode « express » conditionnel) |
| §5 | Pas de recherche avant création depuis l'écran de gestion référentielle | Moyen à élevé — source de doublons d'organisme non détectés par le contrôle exact actuel | Faible (réutiliser le composant de recherche déjà existant côté client assuré) |
| §4 | Email obligatoire alors que pas toujours disponible pour un petit organisme | Moyen — données de complaisance, qualité du fichier tiers payant | Faible |
| §6 | NCC ni unique ni validé en format | Moyen — doublons d'organisme non détectés, qualité fiscale | Faible à moyen |
| §7.1 | Montants/plafonds sans borne ni unité affichée | Moyen — erreurs de saisie sur des paramètres de facturation sensibles | Faible |
| §7.3 | Catégorie invisible alors qu'elle pilote l'affichage de sections entières | Faible à moyen — formulaire difficile à comprendre pour un nouvel utilisateur | Faible (affichage lecture seule suffirait) |
| §7.5 | Pas de confirmation à la fermeture accidentelle | Faible à moyen — formulaire le plus long des trois analysés, perte la plus coûteuse | Faible |
| §7.2 | Pas d'aide contextuelle sur code organisme / NCC | Faible | Faible |
| §7.4 | Pas de recherche sur la liste des groupes tiers payant | Faible aujourd'hui, croît avec le volume | Faible |
| §7.6 | Coquille dans le message d'erreur serveur | Faible (image) | Trivial |
| §9 | Pas de taux de couverture par défaut sur le tiers payant pour pré-remplir le formulaire assuré | Moyen — ressaisie répétée d'une valeur presque toujours identique, risque de faute de frappe sur le remboursement | Faible à moyen |

---

## 9. Proposition — Taux de couverture par défaut (pré-remplissage du formulaire assuré)

### Rappel du manque identifié
L'analyse du formulaire client assuré notait en **§7.1**
([PLAN-ANALYSE-COMPARATIVE-FORMULAIRE-CLIENT-ASSURE.md](PLAN-ANALYSE-COMPARATIVE-FORMULAIRE-CLIENT-ASSURE.md)) :
*« pas de pré-remplissage du taux par défaut de l'organisme sélectionné (un tiers payant a presque
toujours un taux standard connu — ex. CNPS 80 %) »*. Cette section détaille la solution côté
formulaire tiers payant, puisque c'est lui qui doit porter la donnée source.

### Le constat technique
Le taux de prise en charge (`taux`) n'existe aujourd'hui **qu'au niveau de la relation**
`ClientTiersPayant` (un taux par couple client/organisme, saisi à chaque création d'assuré dans
`assure-step.component.ts` et `complementaire-step.component.ts`). L'entité `TiersPayant` elle-même
ne porte aucune notion de taux standard — alors qu'en pratique, l'écrasante majorité des assurés
d'un même organisme (CNPS, telle mutuelle d'entreprise) sont couverts au même taux contractuel, les
exceptions individuelles étant rares (ALD, convention particulière). Résultat : ce même taux
standard est ressaisi manuellement à chaque création de client assuré, sans aide, avec un risque de
faute de frappe directement répercuté sur le calcul de remboursement.

### Proposition

**1. Back — nouveau champ sur le tiers payant**
- `TiersPayant` (entité) : `tauxCouvertureDefaut` (`Integer`, nullable — un organisme peut ne pas
  avoir de taux standard, ex. DEPOT), avec `@Min(0)`/`@Max(100)`.
- `TiersPayantDto` : champ miroir, mêmes bornes (comble au passage l'absence de garde-fou sur les
  taux déjà relevée plus largement dans l'analyse du formulaire assuré, §3.3).
- Migration : colonne `taux_couverture_defaut` sur `tiers_payant`, nullable, pas de valeur par
  défaut imposée (ne pas supposer 100 % silencieusement).

**2. Front — formulaire tiers payant (`FormTiersPayantComponent`)**
- Nouveau champ dans la carte « Plafonds et Remises » (à côté de `remiseForfaitaire`) :
  `app-input-number` avec `[min]="0"`, `[max]="100"`, `suffix=" %"`, label « Taux de couverture par
  défaut ». Non obligatoire (`categorie === 'DEPOT'` n'a pas de sens pour ce champ : à masquer dans
  ce cas, même condition que les sections déjà masquées pour DEPOT).
- `ITiersPayant`/`TiersPayant` (modèle front) : ajout de `tauxCouvertureDefaut?: number`.

**3. Front — pré-remplissage dans le formulaire client assuré**
- `AssureStepComponent.onSelectTiersPayant(tiersPayant)` : après affectation de `this.tiersPayant`,
  patcher `taux` du formulaire avec `tiersPayant.tauxCouvertureDefaut` — **seulement si le champ
  `taux` est encore vide** (ne jamais écraser une valeur déjà modifiée par l'utilisateur, y compris
  en réouvrant un dossier existant en édition).
- `ComplementaireStepComponent.onSelectTiersPayant(tiersPayant, index)` : même logique, appliquée à
  la ligne du `FormArray` concernée (`taux` de la mutuelle complémentaire sélectionnée).
- Affichage d'un indice sous le champ taux quand une valeur a été suggérée automatiquement (« Taux
  standard de l'organisme : 80 % — modifiable ») pour que l'utilisateur sache que c'est une
  suggestion et non une valeur figée, et garde la main pour un cas exceptionnel (cf. écart déjà
  noté sur l'absence de masque de saisie par organisme, §7.3 du présent document).

### Pourquoi c'est la bonne brique pour le combler
- Le patron de pré-remplissage « sélection d'une référence → valeurs par défaut suggérées, non
  imposées » existe déjà ailleurs dans l'app (ex. les switches d'inclusion automatique de
  facturation affichent une phrase explicative dynamique, §2 du présent document) — cette
  proposition suit la même logique plutôt que d'en introduire une nouvelle.
- Elle réduit la charge de saisie perçue identifiée dans l'analyse du formulaire assuré sans
  retirer la possibilité de corriger un cas particulier, donc sans risque de masquer une exception
  contractuelle réelle.

### Effort estimé
Faible à moyen : un champ, une migration, deux points de pré-remplissage (`assure-step`,
`complementaire-step`). Pas de changement de modèle de données structurant (le taux par client
reste la donnée de référence pour le calcul de remboursement ; le nouveau champ n'est qu'une
valeur suggérée au moment de la saisie).

---

## 10. Hors périmètre volontaire

- Les manques d'accessibilité transverses (`app-form-field`, `aria-*`) sont déjà documentés dans les
  deux analyses précédentes (client comptant, client assuré) et ne sont pas redétaillés ici.
- La cohérence des libellés de montants au format FCFA à l'échelle de toute l'application (pas
  seulement ce formulaire) dépasse le périmètre de cette analyse ciblée.

