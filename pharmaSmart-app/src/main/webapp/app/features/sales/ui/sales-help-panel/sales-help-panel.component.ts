import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

type Mode = 'comptant' | 'assurance' | 'carnet';

interface Touche {
  /** Touches à presser ensemble, ex. `['Alt', 'P']`. */
  touches: string[];
  action: string;
}

interface Section {
  titre: string;
  icone: string;
  note?: string;
  lignes: Touche[];
}

/**
 * Aide de l'espace de vente : raccourcis clavier et navigation à la touche Entrée.
 *
 * Reprend les raccourcis de `keyboard-shortcuts.mixin.ts` (F, Alt, Ctrl), les touches de la grille du panier
 * (`ProductListComponent.onGridKeydown`) et les comportements de la touche Entrée des champs de la vente.
 * L'aide complète, générée depuis le formulaire actif, reste sur F1.
 */
@Component({
  selector: 'app-sales-help-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="help-panel">
      <p class="help-intro">
        Tout se fait au clavier, sans quitter le champ en cours. <kbd>F1</kbd> ouvre l'aide complète du formulaire actif.
      </p>

      @for (section of sections(); track section.titre) {
        <section class="help-section">
          <h4 class="help-title"><i [class]="section.icone" aria-hidden="true"></i> {{ section.titre }}</h4>
          @if (section.note) {
            <p class="help-note">{{ section.note }}</p>
          }
          <dl class="help-list">
            @for (ligne of section.lignes; track ligne.action) {
              <div class="help-row">
                <dt>
                  @for (touche of ligne.touches; track $index) {
                    @if ($index > 0) {
                      <span aria-hidden="true">+</span>
                    }
                    <kbd>{{ touche }}</kbd>
                  }
                </dt>
                <dd>{{ ligne.action }}</dd>
              </div>
            }
          </dl>
        </section>
      }
    </div>
  `,
  styleUrl: './sales-help-panel.component.scss',
})
export class SalesHelpPanelComponent {
  /** Type de vente affiché : adapte F4 (client) et les raccourcis propres à l'assurance et au carnet. */
  readonly mode = input<Mode>('comptant');

  /** Pré-vente ou proforma : pas d'encaissement ni de mise en attente. */
  readonly document = input<boolean>(false);

  /** Proforma (parmi les documents) : Entrée dans le champ produit vide enregistre le proforma. */
  readonly proforma = input<boolean>(false);

  /** Vente dépôt : ni client, ni règlement, ni remise, ni mise en attente. */
  readonly depot = input<boolean>(false);

  /** Ce que fait Entrée dans le champ produit vide, panier rempli : cela dépend du type de vente (voir `onProductSearchEnter`). */
  private entreeProduitVide(comptant: boolean, doc: boolean): Touche[] {
    if (this.proforma()) {
      return [{ touches: ['Entrée'], action: 'Dans le champ produit vide, panier rempli : enregistrer le proforma' }];
    }
    if (doc) {
      return [];
    }
    if (comptant) {
      return [{ touches: ['Entrée'], action: 'Dans le champ produit vide, panier rempli : passer au règlement' }];
    }
    return [
      { touches: ['Entrée'], action: 'Dans le champ produit vide, panier rempli : si le montant à payer est 0, finaliser la vente (après confirmation)' },
      { touches: ['Entrée'], action: 'Dans le champ produit vide, panier rempli : sinon, passer au règlement' },
    ];
  }

  protected readonly sections = computed<Section[]>(() => {
    const depot = this.depot();
    const comptant = this.mode() === 'comptant';
    // Sans encaissement ni mise en attente : pré-vente, proforma et dépôt.
    const doc = this.document() || depot;
    return [
      {
        titre: 'Saisir une vente, pas à pas',
        icone: 'pi pi-list-check',
        lignes: [
          { touches: ['F2'], action: 'Aller au champ produit, taper le nom ou le code' },
          { touches: ['Entrée'], action: 'Choisir le produit trouvé, puis passer à la quantité' },
          { touches: ['Entrée'], action: 'Dans la quantité : ajouter le produit au panier' },
          ...this.entreeProduitVide(comptant, doc),
          ...(doc ? [] : [{ touches: ['Entrée'], action: "Dans un montant de règlement : valider l'encaissement" }]),
        ],
      },
      {
        titre: 'Touches de fonction',
        icone: 'pi pi-bolt',
        lignes: [
          { touches: ['F1'], action: 'Aide complète des raccourcis' },
          { touches: ['F2'], action: 'Recherche produit' },
          { touches: ['F3'], action: 'Quantité' },
          ...(depot ? [] : [{ touches: ['F4'], action: comptant ? 'Sélection du client' : 'Recherche du client' }]),
          { touches: ['F5'], action: 'Ajouter le produit' },
          { touches: ['F6'], action: 'Effacer la sélection du produit' },
          ...(doc ? [] : [{ touches: ['F7'], action: 'Aller au règlement' }]),
          ...(depot ? [] : [{ touches: ['F8'], action: 'Appliquer une remise (si autorisée)' }]),
          { touches: ['F9'], action: doc ? 'Enregistrer' : 'Finaliser (payer)' },
          ...(doc ? [] : [{ touches: ['F10'], action: 'Mettre la vente en attente' }]),
        ],
      },
      ...(depot
        ? []
        : [
            {
              titre: 'Menu des types de vente',
              icone: 'pi pi-bars',
              note: "Avec une vente en cours, le changement de type demande une confirmation.",
              lignes: [
                { touches: ['Alt', '1'], action: 'Vente comptant' },
                { touches: ['Alt', '2'], action: this.proforma() ? 'Vente assurance (indisponible en proforma)' : 'Vente assurance (si le droit est accordé)' },
                { touches: ['Alt', '3'], action: 'Vente carnet (si le droit est accordé)' },
                { touches: ['Entrée'], action: 'Ouvrir le type de vente choisi' },
              ],
            },
          ]),
      {
        titre: 'Raccourcis Alt + lettre',
        icone: 'pi pi-compass',
        note: 'Actifs même dans un champ de saisie.',
        lignes: [
          { touches: ['Alt', 'P'], action: 'Produit' },
          { touches: ['Alt', 'Q'], action: 'Quantité' },
          ...(depot ? [] : [{ touches: ['Alt', 'C'], action: 'Client' }]),
          ...(comptant ? [] : [{ touches: ['Alt', 'V'], action: 'Voir la fiche client' }]),
          ...(depot ? [] : [{ touches: ['Alt', 'O'], action: 'Ordonnances du client' }]),
          ...(comptant || doc ? [] : [{ touches: ['Alt', 'A'], action: 'Ajouter un ayant droit (assurance)' }]),
          { touches: ['Alt', 'F'], action: doc ? 'Enregistrer' : 'Finaliser' },
          ...(doc ? [] : [{ touches: ['Alt', 'S'], action: 'Mettre en attente' }]),
          { touches: ['Alt', 'T'], action: 'Imprimer le ticket' },
          ...(depot ? [] : [{ touches: ['Alt', 'R'], action: 'Appliquer ou retirer une remise' }]),
        ],
      },
      {
        titre: 'Dans le panier',
        icone: 'pi pi-shopping-cart',
        note: "Cliquer une ligne ou tabuler jusqu'au panier, puis :",
        lignes: [
          { touches: ['↑', '↓'], action: 'Changer de ligne' },
          { touches: ['+'], action: 'Une unité de plus' },
          { touches: ['−'], action: 'Une unité de moins' },
          { touches: ['Suppr'], action: 'Retirer la ligne' },
          { touches: ['Espace'], action: 'Détail du prix de la ligne' },
          { touches: ['Entrée'], action: 'Dans une quantité ou un prix : valider la modification' },
          { touches: ['Ctrl', '↑ ↓'], action: 'Dans une cellule : passer à la ligne voisine' },
        ],
      },
      ...(depot
        ? []
        : [
            {
              titre: 'Panneau des ordonnances',
              icone: 'pi pi-file-edit',
              note: "Actifs quand le panneau est ouvert (Alt + O) et qu'aucune fenêtre n'est au premier plan.",
              lignes: [
                { touches: ['Alt', 'N'], action: 'Liste : nouvelle ordonnance' },
                { touches: ['Alt', 'G'], action: 'Liste : gérer les prescripteurs' },
                { touches: ['Alt', 'L'], action: "Saisie : ajouter la ligne en cours" },
                { touches: ['Alt', 'E'], action: "Saisie : enregistrer l'ordonnance" },
                { touches: ['Alt', 'R'], action: 'Saisie : revenir à la liste sans enregistrer' },
              ],
            },
          ]),
      ...(comptant
        ? []
        : [
            {
              titre: 'Client assuré et numéros de bon',
              icone: 'pi pi-user',
              lignes: [
                { touches: ['Entrée'], action: 'Dans la recherche du client : lancer la recherche' },
                { touches: ['Entrée'], action: 'Dans la liste des assurés : sélectionner la ligne choisie' },
                { touches: ['Échap'], action: 'Fermer la liste, ou refermer la recherche une fois le client choisi' },
                { touches: ['Entrée'], action: 'Dans un numéro de bon : passer au numéro de bon suivant' },
                { touches: ['Entrée'], action: 'Dans le dernier numéro de bon : passer à la recherche de produit' },
              ],
            },
          ]),
      {
        titre: 'Application de bureau',
        icone: 'pi pi-desktop',
        note: "Ces raccourcis Ctrl sont réservés par le navigateur : ils ne fonctionnent que dans l'application de bureau.",
        lignes: [
          ...(doc ? [] : [{ touches: ['Ctrl', 'S'], action: 'Mettre en attente' }]),
          { touches: ['Ctrl', 'Entrée'], action: 'Finaliser rapidement' },
          { touches: ['Ctrl', 'P'], action: 'Imprimer le ticket' },
        ],
      },
    ];
  });
}
