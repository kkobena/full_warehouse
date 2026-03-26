import { Component, inject, effect, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
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
    SuggestionHomeComponent,
    SemoisSuggestionsComponent,

  ],
})
export class SuggestionsUnifiedComponent {
  readonly activeSource = signal<SuggestionsSource>('FOURNISSEURS');
  readonly semoisFraicheur = signal<SemoisFraicheur | null>(null);

  private readonly commandCommonService = inject(CommandCommonService);
  private readonly suggestionService = inject(SuggestionService);
  private readonly modalService = inject(NgbModal);

  constructor() {
    // Synchronise la source active avec le signal du service (navigation externe)
    effect(() => {
      const source = this.commandCommonService.suggestionsActiveSource();
      this.activeSource.set(source);
    });

    // Charge la fraîcheur SEMOIS pour l'indicateur dans l'en-tête
    this.suggestionService.getSemoisFraicheur().subscribe({
      next: f => this.semoisFraicheur.set(f),
      error: () => this.semoisFraicheur.set(null),
    });
  }

  setSource(source: SuggestionsSource): void {
    this.activeSource.set(source);
    this.commandCommonService.suggestionsActiveSource.set(source);
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

