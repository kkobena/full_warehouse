import { Component, inject, Input, OnInit, ViewChildren, QueryList, ElementRef } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { InputNumber } from 'primeng/inputnumber';
import { FormsModule } from '@angular/forms';
import { ISuggestionReassort, ILigneReassort } from '../repartition-stock.model';
import { RepartitionStockService } from '../repartition-stock.service';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';

@Component({
  selector: 'jhi-suggestion-reassort',
  templateUrl: './suggestion-reassort.component.html',
  styleUrls: ['./suggestion-reassort.component.scss'],
  imports: [CommonModule, TableModule, ButtonModule, TagModule, InputNumber, FormsModule, ToastModule, ConfirmDialogModule],
  providers: [ConfirmationService, MessageService],
})
export class SuggestionReassortComponent implements OnInit {
  @Input() typeReassort = 'RAYON';
  @ViewChildren('qtyInput') qtyInputs!: QueryList<ElementRef>;

  protected repartitionService = inject(RepartitionStockService);
  protected confirmationService = inject(ConfirmationService);
  protected messageService = inject(MessageService);

  protected suggestions: ISuggestionReassort[] = [];
  protected loading = false;
  protected clonedLines: { [s: number]: ILigneReassort } = {};
  protected currentEditingRow = -1;

  ngOnInit(): void {
    this.loadSuggestions();
  }

  loadSuggestions(): void {
    this.loading = true;
    this.repartitionService.getOpenSuggestions(this.typeReassort).subscribe({
      next: (res: HttpResponse<ISuggestionReassort[]>) => {
        this.suggestions = res.body ?? [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  onRowEditInit(ligne: ILigneReassort, rowIndex: number): void {
    if (ligne.id) {
      this.clonedLines[ligne.id] = { ...ligne };
      this.currentEditingRow = rowIndex;
      // Focus the input after a short delay to ensure it's rendered
      setTimeout(() => {
        const inputs = this.qtyInputs.toArray();
        if (inputs[rowIndex]) {
          const inputElement = inputs[rowIndex].nativeElement.querySelector('input');
          inputElement?.focus();
          inputElement?.select();
        }
      }, 100);
    }
  }

  onRowEditSave(ligne: ILigneReassort, moveToNext = false): void {
    if (ligne.id && ligne.quantity !== undefined && ligne.quantity > 0) {
      if (ligne.stockAvailable && ligne.quantity > ligne.stockAvailable) {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'La quantité ne peut pas dépasser le stock disponible',
        });
        this.onRowEditCancel(ligne, -1);
        return;
      }

      this.repartitionService.updateLigneQuantity(ligne.id, ligne.quantity).subscribe({
        next: () => {
          delete this.clonedLines[ligne.id!];
          this.currentEditingRow = -1;
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Quantité mise à jour',
          });

          // Move to next row if requested
          if (moveToNext) {
            this.moveToNextRow();
          }
        },
        error: () => {
          this.onRowEditCancel(ligne, -1);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Erreur lors de la mise à jour',
          });
        },
      });
    }
  }

  onRowEditCancel(ligne: ILigneReassort, index: number): void {
    if (ligne.id && this.clonedLines[ligne.id]) {
      Object.assign(ligne, this.clonedLines[ligne.id]);
      delete this.clonedLines[ligne.id];
    }
    this.currentEditingRow = -1;
  }

  /**
   * Handle keyboard navigation in the editable quantity input
   */
  onKeyDown(event: KeyboardEvent, ligne: ILigneReassort, rowIndex: number, suggestion: ISuggestionReassort): void {
    const totalRows = suggestion.ligneReassorts?.length ?? 0;

    if (event.key === 'Enter') {
      event.preventDefault();
      // Save and move to next row
      this.onRowEditSave(ligne, true);
    } else if (event.key === 'ArrowDown') {
      event.preventDefault();
      // Save current and move to next
      this.onRowEditSave(ligne, false);
      if (rowIndex < totalRows - 1) {
        setTimeout(() => {
          const nextLigne = suggestion.ligneReassorts![rowIndex + 1];
          this.onRowEditInit(nextLigne, rowIndex + 1);
        }, 200);
      }
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      // Save current and move to previous
      this.onRowEditSave(ligne, false);
      if (rowIndex > 0) {
        setTimeout(() => {
          const prevLigne = suggestion.ligneReassorts![rowIndex - 1];
          this.onRowEditInit(prevLigne, rowIndex - 1);
        }, 200);
      }
    } else if (event.key === 'Escape') {
      event.preventDefault();
      this.onRowEditCancel(ligne, rowIndex);
    }
  }

  /**
   * Move to the next editable row
   */
  private moveToNextRow(): void {
    // Find the current suggestion and row
    for (const suggestion of this.suggestions) {
      const lines = suggestion.ligneReassorts ?? [];
      const currentIndex = lines.findIndex(l => Object.keys(this.clonedLines).includes(String(l.id)));

      if (currentIndex !== -1 && currentIndex < lines.length - 1) {
        setTimeout(() => {
          this.onRowEditInit(lines[currentIndex + 1], currentIndex + 1);
        }, 200);
        break;
      }
    }
  }

  deleteLigne(ligne: ILigneReassort, suggestion: ISuggestionReassort): void {
    this.confirmationService.confirm({
      message: 'Êtes-vous sûr de vouloir supprimer cette ligne?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      accept: () => {
        if (ligne.id) {
          this.repartitionService.deleteLigne(ligne.id).subscribe({
            next: () => {
              suggestion.ligneReassorts = suggestion.ligneReassorts?.filter(l => l.id !== ligne.id);
              this.messageService.add({
                severity: 'success',
                summary: 'Succès',
                detail: 'Ligne supprimée',
              });
            },
            error: () => {
              this.messageService.add({
                severity: 'error',
                summary: 'Erreur',
                detail: 'Erreur lors de la suppression',
              });
            },
          });
        }
      },
    });
  }

  validateSuggestion(suggestion: ISuggestionReassort): void {
    this.confirmationService.confirm({
      message: 'Êtes-vous sûr de vouloir valider cette suggestion? Cette action déplacera le stock.',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      accept: () => {
        if (suggestion.id) {
          this.repartitionService.validateSuggestion(suggestion.id).subscribe({
            next: () => {
              this.messageService.add({
                severity: 'success',
                summary: 'Succès',
                detail: 'Suggestion validée et stock déplacé',
              });
              this.loadSuggestions();
            },
            error: () => {
              this.messageService.add({
                severity: 'error',
                summary: 'Erreur',
                detail: 'Erreur lors de la validation',
              });
            },
          });
        }
      },
    });
  }

  deleteSuggestion(suggestion: ISuggestionReassort): void {
    this.confirmationService.confirm({
      message: 'Êtes-vous sûr de vouloir supprimer cette suggestion?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      accept: () => {
        if (suggestion.id) {
          this.repartitionService.deleteSuggestion(suggestion.id).subscribe({
            next: () => {
              this.suggestions = this.suggestions.filter(s => s.id !== suggestion.id);
              this.messageService.add({
                severity: 'success',
                summary: 'Succès',
                detail: 'Suggestion supprimée',
              });
            },
            error: () => {
              this.messageService.add({
                severity: 'error',
                summary: 'Erreur',
                detail: 'Erreur lors de la suppression',
              });
            },
          });
        }
      },
    });
  }
}
