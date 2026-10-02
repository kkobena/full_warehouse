# Analyse comparative — Formulaire client assuré (`AssureFormStepComponent`) vs logiciels d'officine du marché

> Statut : **analyse** — aucune ligne de code écrite.
> Date : octobre 2026.
> Portée analysée : `pharmaSmart-app/src/main/webapp/app/entities/customer/assure-form-step/`
> (`assure-form-step` orchestrateur à onglets, `assure-step` infos assuré principal,
> `ayant-droit-step` bénéficiaire, `complementaire-step` mutuelles RC1-RC3, services d'état partagé)
> + chaîne back : `AssuredCustomerResource`, `AssuredCustomerServiceImpl`, `AssuredCustomerDTO`,
> `ClientTiersPayantDTO`.
> Méthode et référentiel marché identiques à
> [PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md](PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md) :
> comparatif fait du point de vue **préparateur/caissier au comptoir**, face à **Winpharma**
> (Cegedim/Pharmagest), **LGPI** (Pharmagest), **Smart Rx**, **Alliance Healthcare
> (Leo/Pharmaland)**, **Covalia/Pharmagest**, et des solutions ouest-africaines de terrain
> (**PharmaSoft CI**, **Atlantic Pharma**) — devise FCFA, pas de carte Vitale, CMU/mutuelles privées
> locales.
> Document complémentaire (analyse interne détaillée, hors comparaison marché) :
> [PLAN-ANALYSE-FORMULAIRE-CLIENT-COMPTOIR.md](PLAN-ANALYSE-FORMULAIRE-CLIENT-COMPTOIR.md) —
> formulaire client comptant équivalent, mêmes grilles d'analyse.

---

## 1. Ce que fait le formulaire aujourd'hui (rappel)

Assistant à 2 onglets (« Infos Assuré » / « Infos Ayant-droit », ce dernier masqué en édition) :
nom, prénom, tiers payant (recherche + création inline), numéro de carte, taux, date de validité,
puis téléphone/email/date de naissance/sexe en complément, et jusqu'à 3 mutuelles complémentaires
en bas de page. Le bénéficiaire (ayant droit) se limite à **une seule personne** par client assuré
(formulaire simple, pas de liste répétable, malgré un modèle de données qui supporte un tableau).

---

## 2. Ce qui existe déjà (ne pas refaire)

| Fonction | État | Fichier |
|---|---|---|
| Recherche de tiers payant avec typeahead serveur | ✅ | `assure-step.component.ts` (`loadTiersPayants`) |
| Création de tiers payant à la volée sans quitter la modale (« Ajouter un nouveau tiers-payant ») | ✅ Équivalent à la création express de client côté comptant | `addTiersPayantAssurance()` |
| Mutuelles complémentaires illimitées (jusqu'à 3) en grille répétable avec ajout/suppression | ✅ | `complementaire-step.component.ts` |
| Validation de date de naissance cohérente front (bornes 1930 → aujourd'hui) et back (`@PastOrPresent`) | ✅ | `DateNaissDirective` / `AssuredCustomerDTO` |
| Disposition 2 colonnes (nom/prénom/tiers payant/num/taux à gauche, contacts à droite) | ✅ Plus compact que le formulaire comptant équivalent | `card-grid-2col` |
| Import JSON en masse de clients/tiers payants (migration, legacy) | ✅ Existe mais en tâche d'administration, pas au comptoir | `customer.component.ts` / `tiers-payant.component.ts` (`uploadJsonData`) |

**Constat général :** la brique « recherche + création à la volée » du tiers payant est déjà au
niveau des logiciels du marché pour cette tâche précise. Les écarts se situent dans les usages
**propres à l'assurance santé** que la concurrence couvre et que ce formulaire ignore encore, et
dans l'ergonomie d'un formulaire multi-écrans plus long que son équivalent comptant.

---

## 3. Écart n°1 — Un seul ayant droit saisissable, alors que la famille est la norme

### Le constat
`AyantDroitStepComponent` porte un unique `FormGroup` (pas de `FormArray`), et
`saveFormState()` écrit systématiquement `currentAssure.ayantDroits = [ayantDroits]` — un tableau
figé à une seule entrée. Pourtant le modèle (`ICustomer.ayantDroits: ICustomer[]`) et l'écran
« Assurances complémentaires » juste au-dessus (grille répétable, jusqu'à 3 lignes) prouvent que le
patron technique pour une **liste** existe déjà dans ce même dossier.

### Pourquoi c'est un manque quotidien
Un contrat CNPS/CMU/mutuelle d'entreprise couvre quasi systématiquement le conjoint **et** plusieurs
enfants. Tous les logiciels de référence (Winpharma, LGPI, Smart Rx...) permettent de saisir la
famille complète de l'assuré en une seule fois à la création — généralement via une grille
« + Ajouter un ayant droit » répétée autant de fois que nécessaire. Ici, pour un ménage de 4
personnes couvertes, le caissier doit créer l'assuré avec un seul enfant, puis chercher un **second
chemin** (non présent dans ce dossier) pour ajouter les autres — ou pire, créer plusieurs « clients »
distincts pour contourner la limite, ce qui fausse ensuite les statistiques famille et la
facturation au tiers payant.

### Ce qu'il faudrait
Transformer `ayant-droit-step` en `FormArray` répétable, sur le même patron que
`complementaire-step` (bouton « + Ajouter un ayant droit », carte par bénéficiaire, suppression
individuelle). C'est l'écart le plus structurant du dossier : il touche un usage réellement
quotidien, pas un cas limite.

---

## 4. Écart n°2 — Pas de lecture de la carte d'assuré (scan/OCR), tout est retapé à la main

### Le constat
Le numéro de carte (`num`), souvent long (10 à 15 caractères alphanumériques pour une CNPS/mutuelle)
est un champ texte simple avec juste un filtre `alphanum` — aucune saisie assistée par scan.

### Pourquoi c'est un manque quotidien
Une bonne partie des logiciels de référence (et presque tous les éditeurs équipant des réseaux avec
beaucoup d'assurés) proposent soit la lecture d'un code-barres/QR présent sur la carte d'assuré (le
même scanner HID déjà utilisé pour les produits, cf. `SalesScannerService` existant dans le module
vente, pourrait être réutilisé ici), soit a minima une capture photo de la carte jointe au dossier
pour preuve. Ici, chaque chiffre du numéro est retapé manuellement — source d'erreur classique (un
chiffre inversé). La base impose bien une contrainte d'unicité `(tiers_payant_id, num)` sur
`ClientTiersPayant` (correction : une vérification existe donc au niveau base de données, contre ce
qu'indiquait une version précédente de cette analyse), mais rien ne garantit qu'elle remonte comme
message clair à l'écran plutôt que comme une erreur technique brute — à vérifier côté
`AssuredCustomerServiceImpl` avant de considérer le point clos (cf. §6).

### Ce qu'il faudrait
Réutiliser le scanner déjà intégré côté vente pour peupler `num` par scan de code-barres quand la
carte en porte un (CNPS en a généralement), avec un champ de secours en saisie manuelle pour les
cartes sans code-barres (mutuelles artisanales locales).

---

## 5. Écart n°3 — Formulaire en assistant séquentiel, plus lent qu'un écran unique pour un geste répété

### Le constat
Contrairement au formulaire comptant (un seul écran scrollable), celui-ci impose un système
d'onglets avec un bouton « Suivant » conditionné à la validité complète de l'onglet 1
(`disabled]="!assureStepComponent()?.editForm?.valid"`). Le caissier ne peut pas commencer par
l'ayant droit si c'est la carte présentée en premier, ni voir en un coup d'œil l'ensemble du dossier
avant de valider.

### Pourquoi c'est un manque quotidien
Les éditeurs de référence pour la vente assurance en Afrique de l'Ouest (PharmaSoft CI, Atlantic
Pharma) privilégient des formulaires à sections dépliables sur un seul écran plutôt que des
assistants pas-à-pas, précisément parce que le comptoir n'a pas le temps d'un parcours linéaire
strict : le client peut annoncer ses informations dans le désordre, ou le caissier peut vouloir
revenir en arrière sans perdre sa saisie de l'onglet suivant. Un assistant séquentiel, bien que plus
guidé pour un nouvel employé, ralentit l'utilisateur expérimenté qui fait ce geste des dizaines de
fois par jour.

### Ce qu'il faudrait (nuancé)
Ne pas forcément supprimer les onglets (ils structurent utilement une saisie complexe pour un
débutant), mais au minimum : autoriser la navigation entre onglets sans validation complète
préalable de l'onglet précédent (juste bloquer l'enregistrement final, pas la consultation), et
afficher un indicateur visuel de progression (✓ sur l'onglet 1 une fois rempli) plutôt qu'un simple
blocage silencieux du bouton.

---

## 6. Écart n°4 — Doublon de dossier non détecté, message d'erreur à vérifier sur le numéro de carte

### Le constat
Aucune vérification d'existence du client (nom/prénom) n'est faite à la création d'un assuré — à
la différence du client comptant, qui bloque sur un doublon exact. Sur le numéro de carte seul, une
contrainte d'unicité `(tiers_payant_id, num)` existe en base (`ClientTiersPayant`), donc une
tentative de réutiliser un numéro déjà attribué pour le même organisme échouera — mais rien dans
`AssuredCustomerServiceImpl` ne semble intercepter spécifiquement cette violation pour la traduire
en message compréhensible (« Ce numéro de carte est déjà utilisé par [Nom du client] ») : à
confirmer, car une violation de contrainte SQL brute remontée telle quelle serait un écart
ergonomique sérieux (erreur technique illisible pour un caissier).

### Pourquoi c'est un manque quotidien
Les logiciels du marché qui gèrent des tiers payants mutualisés (CNPS, CMU, mutuelles d'entreprise)
vérifient systématiquement, dès la saisie du numéro de carte — **avant** la tentative
d'enregistrement — qu'il n'est pas déjà attribué, et surtout qu'ils présentent le nom du dossier
déjà existant pour que le caissier tranche immédiatement (carte présentée deux fois par erreur, ou
réelle tentative de fraude). Ici, même si la contrainte technique protège la donnée, l'absence de
contrôle **anticipé** (avant de remplir tout le reste du formulaire) et l'absence de nom affiché
pour le dossier en conflit restent un écart avec les usages attendus. Le doublon nom+prénom sans
rapport avec un numéro de carte (deux fiches distinctes pour la même personne, chacune avec un
numéro différent ou vide) n'est lui protégé par rien.

### Ce qu'il faudrait
Un contrôle live (ou a minima au submit, comme pour le client comptant) sur le numéro de carte par
tiers payant, avec un message explicite pointant vers le dossier existant plutôt qu'un rejet muet
ou une erreur technique ; et un contrôle de doublon nom+prénom équivalent à celui du client
comptant pour le cas où le numéro de carte n'est pas encore renseigné.

---

## 7. Ergonomie du formulaire — facilité d'utilisation au quotidien

### 7.1 Deux formulaires pour la même notion de carte, deux expériences différentes
Le client comptant n'a « rien de comparable » à remplir alors que le client assuré doit saisir 5
champs rien que pour le tiers payant principal (organisme, numéro, taux, validité) avant même
d'arriver aux coordonnées. Cet écart de charge de saisie entre les deux parcours est normal (la
donnée assurance est intrinsèquement plus riche), mais rien dans l'interface ne **réduit la charge
perçue** : pas de pré-remplissage du taux par défaut de l'organisme sélectionné (un tiers payant a
presque toujours un taux standard connu — ex. CNPS 80 %), alors que la sélection de l'organisme est
faite juste avant dans le même écran et pourrait suggérer automatiquement sa valeur par défaut,
modifiable seulement en cas d'exception.
→ Proposition détaillée (champ `tauxCouvertureDefaut` sur le tiers payant + pré-remplissage) :
voir [PLAN-ANALYSE-COMPARATIVE-FORMULAIRE-TIERS-PAYANT.md](PLAN-ANALYSE-COMPARATIVE-FORMULAIRE-TIERS-PAYANT.md)
§9.

### 7.2 Pas de retour visuel pendant la recherche de tiers payant
`app-select-search` déclenche une requête serveur à partir de 2 caractères, mais rien dans le
template n'indique un état de chargement pendant l'appel réseau (pas de `loading` visible sur le
composant) — contrairement à `customer-search-table` côté vente qui affiche un `loading()` signal
dédié. Pour une recherche réseau potentiellement plus lente qu'une recherche locale, l'absence de
feedback peut laisser croire à une saisie bloquée.

### 7.3 Taux et numéro de sécurité filtrés en « int »/« alphanum » sans validation de format métier
Le filtre clavier limite les caractères saisissables, mais ne vérifie jamais la **longueur attendue**
réelle d'un numéro de carte CNPS/CMU (généralement un format fixe par organisme). Les logiciels du
marché qui gèrent plusieurs tiers payants appliquent souvent un masque de saisie différent par
organisme sélectionné (ex. CNPS = 10 chiffres, telle mutuelle = 8 caractères alphanumériques) — ici,
le même champ générique s'applique à tous, sans retour immédiat si la longueur saisie est
incohérente avec l'organisme choisi.

### 7.4 Pas de récapitulatif avant validation finale
Après avoir rempli potentiellement 2 onglets + jusqu'à 3 mutuelles, le bouton « Enregistrer »
soumet directement sans écran de relecture (« vous allez créer Jean Kouassi, CNPS n°12345, ayant
droit Awa Kouassi — confirmer ? »). Pour un formulaire aussi dense, un récapitulatif avant
soumission finale est un standard d'ergonomie répandu chez la concurrence sur ce type de saisie
à enjeu (erreur plus coûteuse à corriger qu'un simple nom mal orthographié).

### 7.5 Annulation sans confirmation malgré le volume de saisie
Comme noté en interne, `cancel()` ferme la modale immédiatement sans avertissement — un clic
accidentel ou une touche `Échap` non intentionnelle peut faire perdre la saisie de 2 onglets et
3 mutuelles. Les logiciels concurrents demandent presque systématiquement une confirmation
(« Annuler sans enregistrer ? ») au-delà d'un certain nombre de champs remplis.

### 7.6 Pas de raccourci clavier pour passer d'un onglet à l'autre
Comme pour le formulaire comptant, l'enchaînement onglet 1 → onglet 2 → enregistrer dépend
entièrement de la souris (clic sur « Suivant », clic sur « Enregistrer »). Aucun raccourci (`Tab`
en fin de dernier champ, `Entrée` pour avancer) n'accélère ce parcours pourtant plus long que celui
du client comptant, et donc plus pénalisé par l'absence de clavier pur.

---

## 8. Synthèse priorisée

| # | Constat | Impact usage quotidien | Effort estimé |
|---|---|---|---|
| §3 | Un seul ayant droit saisissable (pas de `FormArray`) | Élevé — majorité des contrats familiaux mal couverts dès la création | Moyen (réutiliser le patron `complementaire-step`) |
| §6 | Pas de détection de doublon / numéro de carte déjà utilisé | Élevé — rejets de facturation, fraude non détectée | Moyen |
| §4 | Pas de scan/OCR du numéro de carte | Moyen — ressaisie manuelle source d'erreurs | Moyen à élevé (dépend du matériel déjà déployé) |
| §7.1 | Pas de taux par défaut suggéré selon l'organisme | Moyen — charge de saisie perçue | Faible |
| §5 | Assistant séquentiel bloquant, pas de navigation libre entre onglets | Moyen — ralentit l'utilisateur expérimenté | Faible à moyen |
| §7.4 | Pas de récapitulatif avant validation | Faible à moyen — erreurs découvertes tard | Faible |
| §7.5 | Pas de confirmation à l'annulation | Faible à moyen — perte de saisie accidentelle | Faible |
| §7.2 | Pas d'indicateur de chargement sur la recherche tiers payant | Faible | Faible |
| §7.3 | Pas de masque de saisie par organisme | Faible à moyen | Moyen |

---

## 9. Hors périmètre volontaire

- Les manques techniques internes (bornes serveur absentes sur `taux`, absence de `@NotBlank`,
  bug `Validators.min(1)` dupliqué, accessibilité `aria-*`) restent documentés dans l'analyse
  interne détaillée (document de référence en tête de fichier) et ne sont pas repris ici pour
  rester centré sur la comparaison marché et l'usage quotidien.
- La vérification en ligne des droits ouverts auprès d'un tiers payant (type télétransmission) n'est
  pas évoquée comme un manque : même les éditeurs de référence cités ne la proposent pas
  systématiquement en Côte d'Ivoire, faute d'API nationale équivalente à la carte Vitale — ce n'est
  donc pas un écart significatif face au marché actuel.


