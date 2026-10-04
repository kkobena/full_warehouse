# Plan — Organisation des boutons d'action (barres d'outils et barres d'actions)

> Rédigé le 2026-10-04. **Proposition à valider : aucun écran n'est modifié par ce document.** Les deux changements déjà faits
> (13 boutons de création passés en `primary`, boutons sémantiques nuancés par thème) sont décrits en §9.
> S'appuie sur les thèmes de couleur ([PLAN-THEMES-APPLICATION.md](PLAN-THEMES-APPLICATION.md)).

## 1. Constat chiffré (relevé du 2026-10-04, tous les gabarits)

| Zone | Boutons `app-button` | Répartition des sévérités |
|---|---|---|
| `app-toolbar` | **177** | info 40 · primary 40 (+8 par défaut) · secondary 37 · warn 25 · success 14 · danger 7 · help 6 |
| Barres d'actions groupées (`bulk-action-bar`) | 19 | info 4 · warn 4 · success 4 · danger 3 · secondary 2 · primary 2 |
| Ailleurs (formulaires, modales, panneaux) | 744 | secondary 222 · primary 211 · success 98 · danger 89 · info 83 · warn 29 · help 11 · contrast 1 |

**La couleur ne dit pas l'importance, elle dit une habitude d'écran.** Exemples dans les barres d'outils :

| Action | Sévérités actuelles | Problème |
|---|---|---|
| « Rechercher » (35 occurrences) | `info` ×24, `primary` ×11 | même action, deux niveaux |
| « Nouveau » / « Ajouter » | `success` ×5 (avant correction), `primary` ×2 | création tantôt verte, tantôt primaire |
| « Exporter PDF » | `warn` ×7, `danger` ×3 | PDF en orange ou en rouge : `danger` annonce une action destructive |
| « Imprimer » | `warn` ×5 | idem, `warn` = prudence, pas impression |
| « Excel » | `warn`, `info`, `success`, `help` | quatre couleurs pour un seul libellé |
| « Retour » | `secondary` ×8 | cohérent |

Conséquences : l'utilisateur ne peut pas apprendre « le bouton plein de couleur est l'action principale » ; sur un écran,
2 à 4 boutons pleins se disputent l'attention ; avec les thèmes de couleur, `success`/`info`/`help` ressemblent
au primaire selon le thème.

## 2. Référentiels retenus

**Conception** (convergents entre Material 3, IBM Carbon, Shopify Polaris, GOV.UK, Apple HIG) :
- **Un seul bouton principal par vue ou par section** : l'action qui fait avancer la tâche. Il est le seul à être plein et saturé.
- **Hiérarchie visuelle à 3 ou 4 niveaux** (plein, contour, texte/icône) plutôt qu'une couleur par sens.
- **Destructif séparé**, jamais en position d'action principale, et **confirmé** (Nielsen n° 5, prévention des erreurs).
- **Cohérence** : une même action a le même libellé, la même icône et le même niveau partout (Nielsen n° 4).
- **Hick / Miller** : peu de choix visibles (≤ 4), le reste dans un menu de débordement.
- **Loi de Fitts** : l'action la plus fréquente est la plus grande et la plus proche du pouce / du curseur (bord droit de la barre).

**WCAG 2.2** (niveau AA, sauf mention) :

| Critère | Exigence pour les boutons d'action |
|---|---|
| 1.3.1 / 1.3.2 | Regroupement exposé (`role="toolbar"` / `group` + `aria-label`) ; l'ordre du DOM est l'ordre visuel |
| 1.4.1 | Le niveau ne repose pas sur la seule couleur : libellé + icône + forme (plein / contour / texte) |
| 1.4.3 / 1.4.11 | Texte ≥ 4,5:1 sur le fond du bouton (repos, survol, actif) ; contour et icône ≥ 3:1 |
| 2.1.1 / 2.4.3 | Tout au clavier, ordre de tabulation = ordre visuel |
| 2.4.7 / 2.4.11 | Focus visible (≥ 3:1) et **non masqué** par une barre fixe (`position: sticky`) |
| 2.5.8 | Cible ≥ 24 × 24 px (AA) ; **recommandé 44 × 44 px** sur poste tactile (AAA 2.5.5) ; 8 px entre deux boutons |
| 3.2.3 / 3.2.4 | Navigation et identification **cohérentes** : même fonction, même nom, même niveau |
| 3.3.4 | Actions financières ou de données (suppression, annulation de vente) : confirmées ou réversibles |
| 4.1.2 | Bouton icône seul : `aria-label` + info-bulle ; état de chargement : `aria-busy` |

## 3. Modèle proposé : quatre niveaux

| Niveau | Rôle | Rendu (`app-button`) | Nombre par barre |
|---|---|---|---|
| **N1 — Principal** | L'action qui fait avancer la tâche | `severity="primary"` plein | **1** (0 si l'écran n'en a pas) |
| **N2 — Secondaire** | Actions fréquentes de second rang | `severity="secondary"` `[outlined]="true"` | 0 à 2 |
| **N3 — Tertiaire / utilitaire** | Retour, aide, actualiser, effacer les filtres | `[text]="true"`, icône + `ariaLabel`, `ngbTooltip` | 0 à 3 |
| **N4 — Destructif** | Supprimer, annuler une vente | `severity="danger"` `[outlined]="true"` ; plein seulement dans la boîte de confirmation | 0 ou 1, **séparé** |

Les sévérités `success`, `warn`, `info`, `help`, `contrast` **quittent les barres d'outils** : elles servent aux **états** (badges, alertes,
messages de résultat) et aux boutons de **confirmation dans les boîtes de dialogue**, pas à classer des actions. `success` reste pertinent pour
« Finaliser / Encaisser » dans l'espace de vente, qui a ses propres accents.

## 4. Placement dans `app-toolbar`

```
[icône] Titre   ·   filtres (champs)   ·············   [N3 utilitaires]  [N2 groupe Exporter ▾]  [N2 secondaire]  [N1 PRINCIPAL]
```

- **Filtres à gauche, actions à droite** (déjà le cas). Le **principal est le dernier, au bord droit** : emplacement constant, atteint en dernier au clavier,
  ce qui est cohérent avec les boîtes de dialogue (validation à droite).
- **Un seul regroupement « Exporter ▾ »** (`app-split-button`) remplace Imprimer / PDF / Excel / Exporter dispersés : entrées « PDF », « Excel », « Imprimer ».
  Plus de PDF rouge ni d'Excel vert : le format se lit à l'icône et au libellé.
- **Plafond : 4 contrôles visibles** (1 N1 + 2 N2 + utilitaires en icônes). Au-delà, menu « Plus ▾ ».
- **« Retour »** : N3 à gauche, dans la zone du titre, pas dans le groupe d'actions.
- **Suppression** : jamais dans la barre d'outils. Elle apparaît dans la **barre d'actions groupées** (sélection) ou le menu de ligne, avec confirmation.
- La barre porte `role="toolbar"` et `aria-label` ; la navigation fléchée (tabulation itinérante, patron ARIA APG) est **recommandée à partir de 5 contrôles**, facultative sinon.

## 5. Règles par action récurrente

| Action | Niveau | Libellé / icône | Remarque |
|---|---|---|---|
| Nouveau / Ajouter / Créer | **N1** | « Nouveau … » `pi-plus` | une seule création principale par écran |
| Rechercher | **N2** (`info`, décidé) | « Rechercher » `pi-search` | la touche Entrée dans un filtre déclenche la recherche ; même libellé partout |
| Enregistrer / Valider | N1 | « Enregistrer » `pi-check` | dans les formulaires et modales |
| Exporter / Imprimer / PDF / Excel | N2 (groupe) | « Exporter ▾ » `pi-download` | une seule entrée de barre |
| Importer | N2 (dans « Plus ▾ » ou groupe) | « Importer » `pi-upload` | |
| Actualiser / Rafraîchir | N3 icône | `pi-refresh`, `ariaLabel="Actualiser"` | |
| Effacer les filtres | N3 texte | « Effacer les filtres » | |
| Aide | N3 icône | `pi-question-circle` | remplace `help` |
| Retour | N3 texte | « Retour » `pi-arrow-left` | |
| Supprimer / Annuler la vente | N4 | « Supprimer » `pi-trash` | confirmation obligatoire (3.3.4) |
| Mettre en veille / Réactiver (lot) | N2 dans la barre d'actions groupées | | réversibles : pas de N4 |

**Barre d'actions groupées** (`bulk-action-bar`) : compteur « N sélectionné(s) » à gauche, puis l'action de lot principale (N1 ou N2 selon l'écran),
les secondaires, le **destructif séparé en dernier**, et « Désélectionner » en N3.

## 6. Garde-fous proposés (mesurables, rejouables)

1. **Test de structure** (script sur les gabarits, en CI) : par `app-toolbar`, ≤ 1 bouton `primary` plein ; aucune sévérité `success|warn|info|help|contrast` dans les barres ;
   « Rechercher », « Nouveau », « Retour »… toujours au même niveau (table de référence unique, une ligne par action).
2. **Test navigateur** (Playwright + axe, sur le modèle de `themes-menus.spec.ts`) pour chaque thème : contraste du texte des boutons au repos, survol, actif, focus,
   cibles ≥ 24 px (44 px pour l'espace de vente), focus non masqué par la barre fixe, `aria-label` sur les boutons icône.
3. **Test de cohérence** (3.2.4) : même libellé ⇒ même niveau et même icône dans toute l'application.
4. Le **garde-fou de thème** existant (`theme-contraste.spec.ts`) couvre déjà le contraste des sept sévérités par thème et leur distance au primaire.

## 7. Plan de mise en œuvre

| Phase | Contenu | Livrable | Effort |
|---|---|---|---|
| **0** | Valider le modèle et les décisions du §8 ; écrire le registre « action → niveau, libellé, icône » | table de référence (TS) | 0,5 j |
| **1** | `app-toolbar` : trois emplacements (`[toolbarPrimary]`, `[toolbarSecondary]`, `[toolbarUtility]`) qui imposent l'ordre, l'espacement de 8 px, `role="toolbar"` et `aria-label` ; plafond avec menu « Plus ▾ » | composant + tests | 1,5 j |
| **2** | Migration des **177** boutons de barre d'outils : script semi-automatique (libellé → niveau) puis relecture écran par écran ; regroupement « Exporter ▾ » | ~60 gabarits | 2 j |
| **3** | Barres d'actions groupées (19 boutons) et boutons de ligne | ~8 gabarits | 0,5 j |
| **4** | Garde-fous du §6 en CI et dans la suite `a11y` | 3 tests | 1 j |
| **5** | Documentation du Design System (page « Boutons d'action ») | doc + captures par thème | 0,5 j |
| **Total** | | | **≈ 6 jours** |

## 8. Décisions à prendre avant la phase 1

1. **« Rechercher »** — **DÉCIDÉ (2026-10-04) : sévérité `info`**, partout, qu'il y ait ou non une création sur l'écran. Il n'est donc pas le bouton principal. Les 44 occurrences des gabarits sont déjà en `info`. La touche Entrée dans un filtre déclenche la recherche.
2. **Position du principal** : proposé au bord droit, dernier dans l'ordre de tabulation (alternative : à gauche du groupe, premier au clavier).
3. **Sort des sévérités `info`, `help`, `contrast`** : proposé de ne plus les utiliser sur des boutons d'action (conservées pour badges et alertes), `help` remplacé par l'icône d'aide N3.
4. **Conventions PDF / Excel** : proposé de les supprimer au profit d'un menu « Exporter ▾ » neutre. À confirmer si les utilisateurs s'y fient.
5. **Cibles tactiles** : 24 px minimum partout (AA) ; 44 px pour l'espace de vente et les écrans utilisés au doigt ?
6. **Navigation fléchée dans la barre** (`role="toolbar"` itinérant) : généralisée ou limitée aux barres de plus de 5 contrôles ?

## 9. Déjà fait (2026-10-04)

- **13 boutons de création des barres d'outils passés de `success` à `primary`** : « Nouveau » (groupe tiers payant, mouvements de caisse, remises, inventaire, rayons), « Nouveau rôle »,
  « Nouvel ajustement », « Nouvel avoir », « Nouveau Poste », « Ajouter » (lots périmés), « Pré-vente Comptant », « Vente Comptant », bouton « Nouvelle vente » du journal.
  Non touché : « Excel » (historique des règlements), qui est un export (§5).
- **Boutons sémantiques propres à chaque thème** : `secondary`, `success`, `info`, `warning`, `danger`, `help`, `contrast` (pleins et contour), table `$pharma-chrome-buttons` de `_pharma-chrome-themes.scss`.
  Un même bouton n'a plus la même couleur d'un thème à l'autre (première version, mélange à 12 % : écarts de 3 à 11 ΔE seulement, donc quasi identiques). Chaque sévérité garde sa famille
  (vert, bleu, rouge, violet, ambre) mais sa teinte et sa clarté sont décalées différemment par thème, saturation réduite d'environ 15 % (teintes vives jugées agressives). Garde-fou, pour chaque thème :
  contraste texte / fond au repos, survol et actif, texte des boutons contour sur blanc et sur fond de page (≥ 4,5:1), écart au primaire du thème (ΔE ≥ 25), et **écart d'un thème à l'autre pour une même
  sévérité (ΔE ≥ 12)**. Mesuré : `success` 17,6 · `info` 24,1 · `danger` 14,1 · `help` 16,7 · `warning` 18,0 · `contrast` ≥ 12 (avant : 2 à 11).
- **Limite connue** : la variante « texte » de `app-button` (`text-<sévérité>`, avec `!important`) lit encore la couleur de la charte et n'est pas nuancée.

## 10. Sévérité de référence par action (appliquée le 2026-10-04)

« Les boutons qui font la même chose doivent avoir la même sévérité » (WCAG 3.2.4, identification cohérente). 84 boutons alignés sur la table ci-dessous, dans tous les gabarits
(le relevé complet des écarts figure plus haut, §1). La table est l'embryon du registre de la phase 0 du §7 : une ligne par action.

| Action (libellé) | Sévérité | Avant (écarts corrigés) |
|---|---|---|
| Enregistrer · Valider · Régler · Clôturer · Activer · Nouveau · Nouvelle vente · Modifier · Ajouter · Ajouter un ayant droit · Sélectionner · Cloner · Retour à la liste | `primary` | `success` ×18, `info` ×3, `secondary` ×3, `warn` ×1 |
| Rechercher · Actualiser · Rafraîchir | `info` | `primary` ×24, `secondary` ×3 |
| Annuler · Fermer · Aide · Importer · Importation | `secondary` | `primary` ×4, `success` ×1, `danger` ×1, `help` ×5, `info` ×1 |
| Exporter PDF · PDF · Imprimer · Excel | `warn` | `danger` ×6, `primary` ×2, `secondary` ×2, `info` ×3, `success` ×1, `help` ×1 |

**« Valider », « Régler », « Clôturer » et « Activer » sont `primary`** (décision de l'utilisateur, 2026-10-04) : valider revient à enregistrer, et clôturer ou activer valident une action. La sévérité `success` ne sert donc plus à aucun libellé de la table.

**Conservés en `danger`** (réellement destructifs) : « Annuler » dans `sale-actions` (annulation de la vente), « Confirmer » de l'annulation d'un avoir.

Choix qui suivent la majorité observée, **à confirmer** (ils recoupent les décisions du §8) : « Rechercher » en `info` (30 occurrences contre 14 en `primary`), les exports en `warn`. Les modificateurs de style
(`outlined`, `text`) n'ont pas été touchés : seule la sévérité est alignée.

