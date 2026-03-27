import {Component, computed, effect, inject, input, Injector, signal} from '@angular/core';
import {CommonModule, DecimalPipe} from '@angular/common';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {ButtonModule} from 'primeng/button';
import {TagModule} from 'primeng/tag';
import {TooltipModule} from 'primeng/tooltip';
import {SuggestionFacadeService} from './data-access/suggestion-facade.service';
import {
  SuggestionFournisseurListComponent
} from './ui/suggestion-fournisseur-list/suggestion-fournisseur-list.component';
import {SuggestionProduitPanelComponent} from './ui/suggestion-produit-panel/suggestion-produit-panel.component';
import {
  SuggestionCommanderModalComponent
} from './ui/suggestion-commander-modal/suggestion-commander-modal.component';
import {CommanderModalResult} from './data-access/suggestion-commander.model';
import {SuggestionComparaisonComponent} from './ui/suggestion-comparaison/suggestion-comparaison.component';
import {DispoComparaisonComponent} from '../pharmaml/ui/dispo-comparaison/dispo-comparaison.component';
import {FournisseurSuggestionSummary, SuggestionLigneEnrichie} from './data-access/suggestion-enrichie.model';
import {SemoisFraicheur} from 'app/entities/commande/suggestion/suggestion.service';
import {NotificationService} from 'app/shared/services/notification.service';
import {NgbConfirmDialogService} from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { Toast } from "primeng/toast";

@Component({
  selector: 'app-suggestion-home',
  templateUrl: './suggestion-home.component.html',
  styleUrls: ['./suggestion-home.component.scss'],
  imports: [
    CommonModule,
    SuggestionFournisseurListComponent,
    SuggestionProduitPanelComponent,
    ButtonModule,
    TagModule,
    TooltipModule,
    DecimalPipe,
    Toast
  ]
})
export class SuggestionHomeComponent {
  protected readonly facade = inject(SuggestionFacadeService);
  private readonly modalService = inject(NgbModal);
  private readonly injector = inject(Injector);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);

  /**
   * Filtre par statut (v12) — signal input réactif :
   * - 'GENEREE'  → onglet "Réapprovisionnement"
   * - 'VALIDEE'  → onglet "Commandes à passer"
   * - undefined  → tous
   */
  readonly statut = input<'GENEREE' | 'VALIDEE' | undefined>('GENEREE');

  readonly showHelp = signal(false);
  readonly selectedLignes = signal<SuggestionLigneEnrichie[]>([]);

  readonly nbCritiques = computed(() =>
    this.facade.fournisseurs().filter(f => f.nbUrgents > 0).length,
  );
  readonly nbFournisseurs = computed(() => this.facade.fournisseurs().length);

  constructor() {
    // Réagit à chaque changement du statut (y compris l'init)
    effect(() => {
      this.facade.loadAll(this.statut());
    });
  }

  onFournisseurSelected(f: FournisseurSuggestionSummary): void {
    this.facade.selectFournisseur(f);
  }

  /** Commander toute la suggestion, envoie uniquement l'ID au backend. */
  onCommander(): void {
    const fournisseur = this.facade.selectedFournisseur();
    if (!fournisseur) {
      this.notificationService.warning('Aucun fournisseur sélectionné.');
      return;
    }
    const lignes = this.facade.lignesEnrichies();
    this.openCommanderModal(fournisseur, lignes, 'full');
  }

  /** Commander uniquement les lignes sélectionnées (la suggestion n'est pas supprimée si des lignes restent). */
  onCommanderSelection(): void {
    const fournisseur = this.facade.selectedFournisseur();
    if (!fournisseur) {
      this.notificationService.warning('Aucun fournisseur sélectionné.');
      return;
    }
    const lignes = this.selectedLignes();
    if (lignes.length === 0) {
      this.notificationService.warning('Aucune ligne sélectionnée.');
      return;
    }
    this.openCommanderModal(fournisseur, lignes, 'selection');
  }

  /**
   * Commander les lignes visibles après filtrage urgence.
   * Déclenché par le bouton "Commander filtrés (N)" quand un filtre est actif.
   */
  onCommanderFiltre(lignes: SuggestionLigneEnrichie[]): void {
    const fournisseur = this.facade.selectedFournisseur();
    if (!fournisseur) {
      this.notificationService.warning('Aucun fournisseur sélectionné.');
      return;
    }
    if (lignes.length === 0) {
      this.notificationService.warning('Aucune ligne visible après filtrage.');
      return;
    }
    this.openCommanderModal(fournisseur, lignes, 'selection');
  }

  onQuantiteChanged(data: { ligne: SuggestionLigneEnrichie; qte: number }): void {
    this.facade.updateQuantite(data.ligne, data.qte);
  }

  onResetQte(ligne: SuggestionLigneEnrichie): void {
    this.facade.resetQuantite(ligne);
  }


  onSelectionChanged(lignes: SuggestionLigneEnrichie[]): void {
    this.selectedLignes.set(lignes);
  }

  onFilterChange(event: { search: string; urgence: string }): void {
    this.facade.applyFilter(event.search, event.urgence);
  }

  onPageChange(event: { page: number; rows: number }): void {
    this.facade.changePage(event.page, event.rows);
  }

  onSanitize(): void {
    const fournisseur = this.facade.selectedFournisseur();
    if (!fournisseur?.suggestionId) return;
    this.facade.sanitize(fournisseur.suggestionId);
  }

  onExporterPdf(): void {
    this.facade.exporterPdf();
  }

  onExporterCsv(): void {
    this.facade.exporterCsv();
  }

  onComparer(ligne: SuggestionLigneEnrichie): void {
    if (!ligne.produitId) return;
    const modalRef = this.modalService.open(SuggestionComparaisonComponent, {
      size: 'lg',
      scrollable: true,
      injector: this.injector,
    });
    modalRef.componentInstance.produitId = ligne.produitId;
    modalRef.componentInstance.produitLibelle = ligne.libelle;
    modalRef.componentInstance.currentFournisseurProduitId = ligne.fournisseurProduitId;
  }

  onValider(): void {
    const fournisseur = this.facade.selectedFournisseur();
    if (!fournisseur?.suggestionId) return;
    this.facade.valider(fournisseur.suggestionId);
  }

  onRejeter(): void {
    const fournisseur = this.facade.selectedFournisseur();
    if (!fournisseur?.suggestionId) return;
    this.confirmDialog.onConfirm(
      () => this.facade.rejeter(fournisseur.suggestionId!),
      'Rejeter la suggestion',
      'Cette action supprimera définitivement la suggestion. Confirmer ?',
    );
  }

  onLigneSupprimee(ligne: SuggestionLigneEnrichie): void {
    if (!ligne.id) return;
    this.confirmDialog.onConfirm(
      () => this.facade.supprimerLigne(ligne.id!),
      'Supprimer le produit',
      `Retirer "${ligne.libelle}" de cette suggestion ?`,
    );
  }

  onLignesSupprimeees(lignes: SuggestionLigneEnrichie[]): void {
    const ids = lignes.map(l => l.id!).filter(id => id != null);
    if (ids.length === 0) return;
    this.confirmDialog.onConfirm(
      () => this.facade.supprimerLignes(ids),
      'Supprimer la sélection',
      `Retirer ${ids.length} produit(s) de cette suggestion ?`,
    );
  }

  onSuggestionSupprimee(id: number): void {
    this.confirmDialog.onConfirm(
      () => this.facade.supprimerSuggestions([id]),
      'Supprimer la suggestion',
      'Cette action supprimera définitivement la suggestion et toutes ses lignes. Confirmer ?',
    );
  }

  onSuggestionsSupprimeees(ids: number[]): void {
    if (ids.length === 0) return;
    this.confirmDialog.onConfirm(
      () => this.facade.supprimerSuggestions(ids),
      'Supprimer les suggestions',
      `Supprimer définitivement ${ids.length} suggestion(s) ? Cette action est irréversible.`,
    );
  }

  onAjouterProduit(data: { produitId: number; fournisseurProduitId: number; quantite: number }): void {
    this.facade.ajouterProduit(data.produitId, data.fournisseurProduitId, data.quantite);
  }

  onVerifierDispo(): void {
    const fournisseur = this.facade.selectedFournisseur();
    if (!fournisseur?.suggestionId) {
      this.notificationService.warning('Aucune suggestion sélectionnée.');
      return;
    }
    const modalRef = this.modalService.open(DispoComparaisonComponent, {
      size: 'xl',
      scrollable: true,
      injector: this.injector,
    });
    modalRef.componentInstance.suggestionId = fournisseur.suggestionId;
    modalRef.componentInstance.header = `Disponibilité PharmaML — ${fournisseur.libelle}`;
  }

  onFusionnerSuggestions(ids: number[]): void {
    if (ids.length < 2) return;
    this.confirmDialog.onConfirm(
      () => this.facade.fusionnerSuggestions(ids),
      'Fusionner les suggestions',
      `Fusionner ${ids.length} suggestions en une seule ? Les lignes seront regroupées.`,
    );
  }

  semoisFraicheurLabel(f: SemoisFraicheur): string {
    if (!f.dernierCalcul) return 'Suggestion : jamais calculée';
    const diff = Date.now() - new Date(f.dernierCalcul).getTime();
    const heures = Math.floor(diff / 3600000);
    if (heures < 1) return `Suggestion calculée: il y a moins ${heures}h`;
    if (heures < 24) return `Suggestion calculée: il y a ${heures}h`;
    const jours = Math.floor(heures / 24);
    return `Suggestion calculée: il y a ${jours}j`;
  }

  semoisFraicheurSeverity(f: SemoisFraicheur): 'success' | 'warn' | 'danger' | 'secondary' {
    if (!f.dernierCalcul) return 'danger';
    const heures = (Date.now() - new Date(f.dernierCalcul).getTime()) / 3600000;
    if (heures < 24) return 'success';
    if (heures < 72) return 'warn';
    return 'danger';
  }

  onRecalculer(): void {
    this.facade.recalculerSemois(this.statut());
  }

  refresh(): void {
    this.facade.loadAll(this.statut());
  }


  private openCommanderModal(
    fournisseur: FournisseurSuggestionSummary,
    lignes: SuggestionLigneEnrichie[],
    mode: 'full' | 'selection' = 'selection',
  ): void {
    const modalRef = this.modalService.open(SuggestionCommanderModalComponent, {
      size: 'xl',
      backdrop: 'static',
    });
    modalRef.componentInstance.fournisseurLibelle = fournisseur.libelle;
    modalRef.componentInstance.fournisseurId = fournisseur.fournisseurId;
    modalRef.componentInstance.lignes = lignes;
    if (fournisseur.suggestionId) {
      const budget = this.facade.budget();
      if (budget && !budget.budgetIllimite) {
        modalRef.componentInstance.budgetRestant = budget.budgetRestant;
      }
    }

    modalRef.result.then(
      (result: CommanderModalResult) => {
        if (!result?.type) return;
        if (mode === 'full') {
          // Bug 3 : envoie uniquement l'id de suggestion au backend
          this.facade.commanderFull(result);
        } else {
          // Bug 1 : envoie seulement les lignes sélectionnées, ne supprime pas la suggestion si des lignes restent
          this.facade.commanderSelection(lignes, result);
        }
      },
      () => {
        // dismissed — do nothing
      },
    );
  }
}
