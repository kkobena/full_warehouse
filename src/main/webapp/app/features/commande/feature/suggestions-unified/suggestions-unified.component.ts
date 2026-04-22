import { Component, inject, computed, signal, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { BadgeModule } from 'primeng/badge';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { SuggestionHomeComponent } from '../suggestion/suggestion-home.component';
import { SemoisSuggestionsComponent } from '../semois-suggestions/semois-suggestions.component';
import { SemoisClasseConfigComponent } from '../semois-classe-config/semois-classe-config.component';
import { CommandeRequestedHomeComponent } from '../commande-requested-home/commande-requested-home.component';
import { AppListBonsComponent } from '../../ui/list-bons/list-bons.component';
import { CommandCommonService, SuggestionsSource } from 'app/entities/commande/command-common.service';
import { SuggestionService, SemoisFraicheur } from 'app/entities/commande/suggestion/suggestion.service';
import { DeliveryService } from 'app/entities/commande/delevery/delivery.service';

@Component({
  selector: 'app-suggestions-unified',
  templateUrl: './suggestions-unified.component.html',
  styleUrls: ['./suggestions-unified.component.scss'],
  imports: [
    CommonModule,
    ButtonModule,
    TagModule,
    TooltipModule,
    BadgeModule,
    SuggestionHomeComponent,
    SemoisSuggestionsComponent,
    CommandeRequestedHomeComponent,
    AppListBonsComponent,
  ],
})
export class SuggestionsUnifiedComponent implements OnInit {
  /**
   * Source active — computed dérivé directement du signal service.
   * Toujours en phase avec le contenu (@if) et le style de l'onglet, qu'on navigue
   * manuellement (clic) ou programmatiquement (facade après validation).
   */
  readonly activeSource = computed(() => this.commandCommonService.suggestionsActiveSource());
  readonly semoisFraicheur = signal<SemoisFraicheur | null>(null);
  /** Badge : nb suggestions GENEREE + VALIDEE (tab Réapprovisionnement) */
  readonly countReappro = signal<number>(0);
  /** Badge : nb commandes REQUESTED (tab Commandes à passer, toujours affiché) */
  readonly countCommandesAPasser = signal<number>(0);
  /** Badge : nb bons RECEIVED en attente de saisie */
  readonly countBonsLivraison = signal<number>(0);

  private readonly commandCommonService = inject(CommandCommonService);
  private readonly suggestionService = inject(SuggestionService);
  private readonly deliveryService = inject(DeliveryService);
  private readonly modalService = inject(NgbModal);



  ngOnInit(): void {
    this.loadBadges();
    this.suggestionService.getSemoisFraicheur().subscribe({
      next: f => this.semoisFraicheur.set(f),
      error: () => this.semoisFraicheur.set(null),
    });
  }

  /** Recharge les compteurs de badges pour tous les onglets. */
  loadBadges(): void {
    forkJoin({
      generee:    this.suggestionService.countByStatut('GENEREE'),
      validee:    this.suggestionService.countByStatut('VALIDEE'),
      requested:  this.deliveryService.countByStatut('REQUESTED'),
      received:   this.deliveryService.countByStatut('RECEIVED'),
    }).subscribe({
      next: ({ generee, validee, requested, received }) => {
        this.countReappro.set(generee + validee);
        this.countCommandesAPasser.set(requested);
        this.countBonsLivraison.set(received);
      },
      error: () => this.countReappro.set(0),
    });
  }

  setSource(source: SuggestionsSource): void {
    this.commandCommonService.suggestionsActiveSource.set(source);
    this.loadBadges();
  }

  get semoisFraicheurLabel(): string {
    const f = this.semoisFraicheur();
    if (!f) return '';
    if (f.calculeRecent) return 'VMM · À jour';
    if (f.dernierCalcul) {
      const d = new Date(f.dernierCalcul);
      return `VMM · ${d.toLocaleDateString('fr-FR')}`;
    }
    return 'VMM · Non initialisée';
  }

  get semoisFraicheurSeverity(): 'success' | 'warn' | 'danger' | 'secondary' {
    const f = this.semoisFraicheur();
    if (!f || !f.dernierCalcul) return 'danger';
    return f.calculeRecent ? 'success' : 'warn';
  }

  ouvrirConfigCriticite(): void {
    this.modalService.open(SemoisClasseConfigComponent, {
      size: 'lg',
      scrollable: true,
      centered: true,
      backdrop: 'static',
    });
  }
}

