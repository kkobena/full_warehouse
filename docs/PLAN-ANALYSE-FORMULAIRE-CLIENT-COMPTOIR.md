# Analyse — Formulaire de création client comptoir (`UninsuredCustomerFormComponent`)

> Statut : **analyse** — aucune ligne de code écrite.
> Date : octobre 2026.
> Portée analysée :
> `pharmaSmart-app/src/main/webapp/app/entities/customer/uninsured-customer-form/` (component,
> template, styles) + chaîne complète appelée : `CustomerService` (front),
> `CustomerResource#createUninsuredCustomer/updateUninsuredCustomer` (back),
> `UninsuredCustomerService`, `UninsuredCustomerDTO`, `CustomerDTO`, `UninsuredCustomer` (domaine),
> `Util.isValidPhoneNumber`, `CustomerAlreadyExistException`, `InvalidPhoneNumberException`.
> Plan lié (ne pas dupliquer) : [PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md](PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md)
> §2 note déjà la création express comme acquis ; le présent document détaille ce formulaire
> précisément et liste ce qu'il manque.

---

## 1. Ce que fait le formulaire aujourd'hui

- Modale ng-bootstrap à 4 champs : `firstName` (label **« Nom »**), `lastName` (label
  **« Prénom(s) »**), `phone`, `email`.
- Obligatoires : nom, prénom, téléphone (`Validators.required`). Email facultatif.
- `phone` filtré en saisie par `appKeyFilter="int"` (chiffres uniquement, plus signe `-` autorisé
  par la regex — inutile pour un téléphone).
- Focus auto sur le champ Nom à l'ouverture (`ngAfterViewInit` + `setTimeout`).
- À la soumission : `POST/PUT /api/customers/uninsured`. Le back :
  - vérifie les doublons sur **(prénom, nom, téléphone)** → `CustomerAlreadyExistException`
    (« Ce client existe déjà ») ;
  - valide le téléphone avec `Util.isValidPhoneNumber` (libphonenumber, région forcée **"CI"**)
    → `InvalidPhoneNumberException` (« Le numéro de téléphone saisi n'est pas correct ») ;
  - génère un `code` client aléatoire à 6 chiffres.
  - **Aucune autre contrainte serveur** : `CustomerDTO`/`UninsuredCustomerDTO` n'ont **aucune**
    annotation Bean Validation (`@NotBlank`, `@Email`, `@Size`…) malgré le `@Valid` du contrôleur.
- En cas d'erreur, un toast générique (`NotificationService.error`) affiche le message du back ;
  aucun champ n'est mis en évidence, le formulaire reste rempli.
- Utilisé dans 5 points d'entrée : `customer-search-table` (vente comptoir, carnet, différé,
  assurance, avoir), `sale-devis`, `customer.component` (fichier clients), `customer-detail`,
  `customer-data-table` (liste clients sans assurance).

---

## 2. Cas usuels manquants (métier)

### 2.1 Doublon détecté trop tard, et sans aide à la décision
Le contrôle d'existence (`findOne` sur prénom+nom+téléphone exact) n'a lieu **qu'à la
soumission**, côté serveur. Le caissier tape tout le formulaire, clique « Enregistrer », et
découvre alors que le client existe déjà — sans savoir **lequel** (pas d'ID, pas de lien vers la
fiche existante), et sans option « sélectionner le client existant » pour rattraper la vente en
cours. Dans les logiciels du marché (Winpharma, LGPI...), une recherche par téléphone déclenche
une alerte **avant** la saisie complète, avec proposition directe de sélectionner le client trouvé.
→ Manque : recherche par téléphone en live (debounce) dès que 8-10 chiffres sont saisis, avec
bandeau « Un client existe déjà avec ce numéro : Jean Kouassi — [Sélectionner ce client] ».

### 2.2 Téléphone restreint à la Côte d'Ivoire, aucun fallback
`Util.isValidPhoneNumber` force `region = "CI"`. Un client frontalier (Mali, Burkina, Ghana) ou un
visiteur avec un numéro étranger ne peut pas être enregistré avec son vrai numéro : le formulaire
rejette silencieusement tout numéro non-ivoirien valide, avec un message qui ne l'explique pas
(« numéro saisi n'est pas correct » alors qu'il est correct, juste étranger).
→ Manque : indicatif pays sélectionnable, ou au moins un mode dégradé (« numéro étranger — pas de
validation stricte ») plutôt qu'un rejet pur et simple.

### 2.3 Téléphone non obligatoire en pratique mais bloquant ici
Le téléphone est `Validators.required` dans ce formulaire alors que de nombreux clients comptant
ponctuels (achat unique, pas de suivi) n'en donnent pas. Actuellement l'opérateur est contraint de
saisir un faux numéro (ou de répéter un numéro « bidon » déjà utilisé pour d'autres clients) pour
pouvoir valider — ce qui **pollue le contrôle de doublon** (2.1) et la qualité du fichier client,
et peut même provoquer un faux-positif `CustomerAlreadyExistException` si un numéro générique du
genre `0000000000` est réutilisé par plusieurs clients.
→ Manque : téléphone optionnel (ou case « Client anonyme / pas de téléphone »), le nom/prénom
suffisant à défaut.

### 2.4 Aucune case / shortcut « Client anonyme / passage » 
Beaucoup de ventes comptant (un Doliprane, un pansement) n'ont pas besoin d'identité réelle. Les
concurrents proposent un client générique « Client comptant » en un clic, sans ouvrir de
formulaire du tout. Ici, chaque nouveau client — même pour un achat de 500 F — impose de remplir
nom + prénom + téléphone.
→ Manque : accès direct à un client « passage » par défaut, ce formulaire ne servant qu'aux clients
qu'on veut vraiment identifier (suivi, différé, fidélité).

### 2.5 Pas de contrôle de format sur l'email, nulle part
`email` n'a ni `Validators.email` côté Angular, ni `@Email` côté DTO. Le `type="email"` HTML5 ne
sert à rien car le `<form novalidate>` désactive la validation native du navigateur. Un email
clairement invalide (`"azer"`, `"a@"`) est donc accepté silencieusement, jusqu'à l'échec probable
d'un envoi ultérieur (facture, relevé, consentement SMS/email).
→ Manque : `Validators.email` + message inline.

### 2.6 Pas de capture du consentement RGPD-like à la création
Le modèle `CanalConsentement` / `enregistrerConsentement` existe (`CustomerService`), preuve qu'une
gestion de consentement (SMS, email, marketing) est prévue ailleurs dans l'app. Mais ce formulaire
de création express ne propose **aucune case à cocher** de consentement au moment même où l'email
ou le téléphone sont saisis — le point le plus naturel pour le demander. Le consentement doit donc
être rattrapé plus tard dans une autre fonctionnalité, ce qui, en pratique, ne sera jamais fait au
comptoir (surcharge de travail du caissier).
→ Manque : au minimum une case « Autoriser les SMS/rappels » liée à `enregistrerConsentement`.

### 2.7 Aucun moyen de convertir un client comptant en client assuré
Le formulaire ne permet de créer que des clients `STANDARD` (non assuré), ce qui est son rôle. Mais
si le caissier découvre en cours de saisie que le client a en fait une mutuelle/CMU (cas fréquent :
« Ah en fait j'ai une carte »), il n'y a **aucun raccourci** pour basculer vers le formulaire
assuré (`assure-step`) sans fermer la modale et perdre la saisie déjà faite (nom/prénom/téléphone).
→ Manque : bouton « Ce client est assuré » qui transmet les champs déjà saisis au formulaire
assuré.

### 2.8 Pas de date de naissance / sexe, même optionnels
`ICustomer` porte déjà `datNaiss` et `sexe` (utilisés pour les clients assurés). Pour un client
comptant qui va ensuite bénéficier du dossier santé (`dossierSante`, alertes grossesse/âge,
traitements chroniques — fonctions déjà présentes dans `CustomerService`), l'absence totale de ces
champs à la création oblige une ressaisie complète plus tard dans une autre fiche pour activer ces
contrôles de sécurité. Un client comptant régulier (habitué du quartier) finit souvent par avoir un
dossier santé suivi, mais rien dans ce formulaire ne l'anticipe.
→ Manque : au moins la date de naissance en champ optionnel repliable (« + Infos complémentaires »,
qui existe déjà comme section mais ne contient que l'email).

### 2.9 Pas d'adresse
Aucun champ adresse (même simple : quartier/ville). Nécessaire pour la facturation légale, le
recouvrement des différés (`relancesDifferes`, `reglementsDifferes` existent déjà côté service) et
les campagnes de relance physique. Actuellement, un client en différé non soldé ne peut être
retrouvé que par téléphone.

### 2.10 Message d'erreur doublon non actionnable
`CustomerAlreadyExistException` / `InvalidPhoneNumberException` remontent un message texte via
toast, mais rien n'indique **quel champ** est en cause (le trio prénom+nom+téléphone est vérifié
globalement). L'utilisateur doit deviner s'il faut changer le nom, le prénom ou le téléphone.
→ Manque : le back pourrait renvoyer une clé d'erreur par champ, ou à défaut le texte devrait
l'expliciter plus précisément (ex. « Un client "Jean Kouassi" existe déjà avec ce téléphone »).

### 2.11 Pas de recherche de doublon proche (fautes de frappe)
Le contrôle `findOne` exige une **correspondance exacte** sur prénom+nom+téléphone. Une faute de
frappe triviale (« Kouacy » au lieu de « Kouassi ») crée un doublon silencieux non détecté — à
l'inverse, une incohérence mineure (ex. numéro différent du même client) crée un second dossier.
Le module `doublons()` / `fusionner()` existe déjà côté `CustomerService` pour nettoyer après coup,
preuve que c'est un problème connu de la base — mais rien ne le prévient à la création.

---

## 3. Ergonomie du formulaire

### 3.1 Intitulés « Nom » / « Prénom(s) » inversés par rapport au reste de l'app( A ne pas traiter)
Le champ lié à `formControlName="firstName"` est étiqueté **« Nom »**, et celui lié à
`lastName` est étiqueté **« Prénom(s) »**. C'est cohérent avec `assure-step.component.html` (même
inversion), mais **incohérent avec `customer-edit-modal.component.html`**, qui étiquette
`lastName` → « Nom » et `firstName` → « Prénom » (l'ordre intuitif). Le même concept métier porte
donc un libellé différent selon l'écran, ce qui est une source d'erreur de saisie (notamment pour
un caissier qui bascule entre les deux). C'est un point de cohérence UX à trancher à l'échelle de
l'application, au-delà de ce seul formulaire.

### 3.2 Mise en page verticale, non optimisée pour un formulaire à 4 champs
Chaque champ est en `form-field-full` (pleine largeur), empilé verticalement : Nom, puis Prénom,
puis Téléphone, chacun sur sa ligne. `customer-edit-modal.component.html`, pour les **mêmes
champs**, utilise un layout `form-row` à deux colonnes (Nom + Prénom côte à côte). Résultat ici :
une modale artificiellement haute (3 lignes + section complémentaire + actions) pour 4 champs qui
tiendraient sur 2 lignes, plus de défilement oculaire et de distance de souris pour un geste
répété des dizaines de fois par jour au comptoir.
→ Suggestion : Nom + Prénom sur une ligne, Téléphone + Email sur une seconde — un seul bloc
« Champs obligatoires » au lieu de deux cartes empilées pour une info aussi simple.

### 3.3 Duplication de cartes pour un seul champ optionnel
La section « Informations complémentaires » (carte séparée, avec en-tête et icône) n'existe que
pour l'email. Cela double le nombre de cadres visuels, d'en-têtes et d'espacements pour un
formulaire qui reste minimaliste — effet « sur-découpage » qui alourdit visuellement une tâche
rapide.

### 3.4 Pas de composant `app-form-field` réutilisé
Un composant `app-form-field` existe déjà (`shared/ui/form-field`) et encapsule
label + erreur + hint avec un balisage homogène. Ce formulaire ne l'utilise pas : chaque champ
réécrit à la main le `<label>`, le `@if` d'erreur, la classe `text-danger`. Résultat : pas de
`role="alert"` sur les erreurs (présent dans le composant partagé, absent ici), pas de lien
`aria-describedby` entre l'input et son message d'erreur, duplication de template dans toute la
base (déjà visible dans `assure-step`, `customer-edit-modal`, etc., chacun avec sa propre variante
manuelle).

### 3.5 Accessibilité clavier/écran incomplète
- Aucun `aria-invalid` ni `aria-describedby` sur les `<input>` en erreur — un lecteur d'écran ne
  rattache pas le message « Ce champ est requis » au champ fautif.
- Le message d'erreur n'a pas `role="alert"`, donc pas annoncé automatiquement.
- `<span class="required">*</span>` n'a pas d'équivalent texte (`aria-label="obligatoire"`) pour un
  lecteur d'écran qui ignore le CSS.
- Pas de `maxlength` sur `phone`/`firstName`/`lastName` : rien n'empêche de taper un numéro de 25
  chiffres avant de découvrir l'erreur au submit.

### 3.6 Bug de validation `Validators.min(1)` sur des champs texte
`firstName`, `lastName`, `phone` utilisent `Validators.min(1)` — ce validateur s'applique à des
**valeurs numériques**, pas à la longueur d'une chaîne. Sur un `FormControl` texte, `min(1)` est
inopérant (ne fait jamais échouer la validation, même avec `"a"` qui n'est pas censé être valide
en-dessous d'une longueur). Seul `Validators.required` protège réellement contre un champ vide ;
une chaîne d'un seul caractère ou avec uniquement des espaces (`"   "`) passe la validation (le
`required` d'Angular ne trim pas). C'est un bug silencieux, pas juste un manque ergonomique.

### 3.7 Pas de feedback de chargement/désactivation claire pendant l'enregistrement
`isSaving()` désactive le bouton `Enregistrer` mais le bouton `Annuler` reste actif — un clic rapide
sur Annuler pendant l'appel réseau laisse la requête en vol sans que l'utilisateur sache si le
client a été créé ou non (pas d'`abort` de la requête, pas de blocage de la fermeture modale). Pas
de spinner visible non plus sur `app-button` pendant `isSaving()` (à vérifier dans
`ButtonComponent`, mais rien dans ce template ne passe un état `loading`).

### 3.8 `isValid` mort / jamais mis à jour
`protected isValid = true;` est déclaré et utilisé dans `[disabled]="editForm.invalid ||
isSaving() || !isValid"`, mais **rien dans le composant ne le modifie jamais** — code mort qui
laisse penser qu'une logique de validation supplémentaire existe, alors que non. À nettoyer ou à
raccorder réellement (ex. à un contrôle de doublon asynchrone, cf. §2.1).

### 3.9 Pas de raccourci clavier pour fermer/valider
Contrairement au reste du module vente qui a des raccourcis F1-F11 documentés
(`keyboard-shortcuts.mixin.ts`), cette modale ne gère ni `Échap` pour annuler ni `Entrée` pour
valider depuis n'importe quel champ (le `type="submit"` fonctionne depuis un `<input>` mais pas
forcément perçu par l'utilisateur comme un raccourci fiable sans indication visuelle). Pour un
geste répété au comptoir, l'absence de clavier pur (tout-souris sur les boutons) ralentit le flux.

---

## 4. Synthèse priorisée

| # | Constat | Impact | Effort estimé |
|---|---|---|---|
| 2.1 | Doublon détecté seulement après submit, sans lien vers le client existant | Perte de temps, double saisie | Moyen |
| 2.3 | Téléphone obligatoire alors que souvent absent en pratique | Pollution fichier client, faux doublons | Faible |
| 3.6 | `Validators.min(1)` inopérant sur champs texte | Bug silencieux, qualité des données | Faible |
| 2.5 | Email jamais validé (ni front ni back) | Données inexploitables plus tard | Faible |
| 3.2 | Layout vertical sous-optimal vs écran équivalent déjà 2 colonnes ailleurs | Friction comptoir répétée | Faible |
| 2.2 | Téléphone limité à la Côte d'Ivoire | Clients frontaliers/étrangers rejetés | Moyen |
| 2.6 | Pas de capture consentement à la création | Conformité RGPD-like jamais faite en pratique | Moyen |
| 2.7 | Pas de bascule client comptant → assuré en cours de saisie | Ressaisie complète | Moyen |
| 3.4 / 3.5 | Pas de `app-form-field`, accessibilité incomplète | Dette UX transverse | Moyen |
| 2.4 | Pas de client « passage » générique en un clic | Lenteur sur petites ventes anonymes | Moyen |

---

## 5. Hors périmètre volontaire

- La politique de doublon « exacte » (prénom+nom+téléphone) côté serveur n'est pas remise en cause
  dans son principe — seule son exposition à l'utilisateur (avant/après submit) est discutée.
- Le module fusion de doublons (`doublons()`, `fusionner()`) existe déjà et n'est pas détaillé ici.
- La divergence Nom/Prénom (§3.1) touche plusieurs formulaires de l'app (`assure-step` vs
  `customer-edit-modal`) ; trancher la convention dépasse ce seul composant et mériterait un ticket
  dédié transverse.

