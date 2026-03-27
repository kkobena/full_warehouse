import { Component, inject, effect, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { BadgeModule } from 'primeng/badge';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { SuggestionHomeComponent } from '../suggestion/suggestion-home.component';
import { SemoisSuggestionsComponent } from '../semois-suggestions/semois-suggestions.component';
import { SemoisClasseConfigComponent } from '../semois-classe-config/semois-classe-config.component';
import { CommandCommonService, SuggestionsSource } from 'app/entities/commande/command-common.service';
import { SuggestionService, SemoisFraicheur } from 'app/entities/commande/suggestion/suggestion.service';

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
  ],
})
export class SuggestionsUnifiedComponent implements OnInit {
  readonly activeSource = signal<SuggestionsSource>('REAPPRO');
  readonly semoisFraicheur = signal<SemoisFraicheur | null>(null);
  /** Badge : nb suggestions GENEREE (tab Réapprovisionnement) */
  readonly countReappro = signal<number>(0);
  /** Badge : nb suggestions VALIDEE (tab Commandes à passer) */
  readonly countCommandesAPasser = signal<number>(0);

  private readonly commandCommonService = inject(CommandCommonService);
  private readonly suggestionService = inject(SuggestionService);
  private readonly modalService = inject(NgbModal);

  constructor() {
    // Synchronise la source active avec le signal du service (navigation externe)
    effect(() => {
      const source = this.commandCommonService.suggestionsActiveSource();
      this.activeSource.set(source);
    });
  }

  ngOnInit(): void {
    this.loadBadges();
    this.suggestionService.getSemoisFraicheur().subscribe({
      next: f => this.semoisFraicheur.set(f),
      error: () => this.semoisFraicheur.set(null),
    });
  }

  /** Recharge les compteurs de badges pour les deux onglets actifs. */
  loadBadges(): void {
    this.suggestionService.countByStatut('GENEREE').subscribe({
      next: n => this.countReappro.set(n),
      error: () => this.countReappro.set(0),
    });
    this.suggestionService.countByStatut('VALIDEE').subscribe({
      next: n => this.countCommandesAPasser.set(n),
      error: () => this.countCommandesAPasser.set(0),
    });
  }

  setSource(source: SuggestionsSource): void {
    this.activeSource.set(source);
    this.commandCommonService.suggestionsActiveSource.set(source);
    // Recharger les badges à chaque changement d'onglet
    this.loadBadges();
  }

  get semoisFraicheurLabel(): string {
    const f = this.semoisFraicheur();
    if (!f) return '';
    if (f.calculeRecent) return `SEMOIS · Récent`;
    if (f.dernierCalcul) {
      const d = new Date(f.dernierCalcul);
      return `SEMOIS · ${d.toLocaleDateString('fr-FR')}`;
    }
    return 'SEMOIS · Jamais calculé';
  }

  get semoisFraicheurSeverity(): 'success' | 'warn' | 'danger' | 'secondary' {
    const f = this.semoisFraicheur();
    if (!f || !f.dernierCalcul) return 'danger';
    return f.calculeRecent ? 'success' : 'warn';
  }

  navigateToSemoisConfig(): void {
    this.modalService.open(SemoisClasseConfigComponent, {
      size: 'lg',
      scrollable: true,
      backdrop: 'static',
    });
  }
}

