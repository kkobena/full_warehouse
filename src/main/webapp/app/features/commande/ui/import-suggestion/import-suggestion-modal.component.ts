import { Component, inject, OnInit, signal } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { SuggestionService } from '../../../../entities/commande/suggestion/suggestion.service';
import { Suggestion } from '../../../../entities/commande/suggestion/model/suggestion.model';
import { CommandeService } from '../../../../entities/commande/commande.service';
import { CommandeId } from '../../../../shared/model/abstract-commande.model';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ErrorService } from '../../../../shared/error.service';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-import-suggestion-modal',
  templateUrl: './import-suggestion-modal.component.html',
  styleUrls: ['./import-suggestion.scss'],
  imports: [CommonModule, ButtonModule, TableModule, TagModule, TooltipModule],
})
export class ImportSuggestionModalComponent implements OnInit {
  commandeId!: CommandeId;
  fournisseurId: number | null | undefined = null;

  protected suggestions = signal<Suggestion[]>([]);
  protected loading = signal(false);
  protected importing = signal(false);

  private readonly activeModal = inject(NgbActiveModal);
  private readonly suggestionService = inject(SuggestionService);
  private readonly commandeService = inject(CommandeService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  ngOnInit(): void {
    this.loadSuggestions();
  }

  protected dismiss(): void {
    this.activeModal.dismiss();
  }

  protected onImport(suggestion: Suggestion): void {
    this.importing.set(true);
    this.commandeService.importSuggestionIntoCommande(this.commandeId, suggestion.id).subscribe({
      next: () => {
        this.notificationService.success('Suggestion importée avec succès', 'Import');
        this.activeModal.close(true);
      },
      error: err => {
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur');
        this.importing.set(false);
      },
    });
  }

  protected statutSeverity(statut: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    switch (statut) {
      case 'ACTIVE':
        return 'success';
      case 'PENDING':
        return 'info';
      case 'CLOSED':
        return 'secondary';
      default:
        return 'info';
    }
  }

  private loadSuggestions(): void {
    this.loading.set(true);
    const params: any = { page: 0, size: 50, statut: 'ACTIVE' };
    if (this.fournisseurId) {
      params.fournisseurId = this.fournisseurId;
    }
    this.suggestionService.query(params).subscribe({
      next: res => {
        this.suggestions.set(res.body ?? []);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
