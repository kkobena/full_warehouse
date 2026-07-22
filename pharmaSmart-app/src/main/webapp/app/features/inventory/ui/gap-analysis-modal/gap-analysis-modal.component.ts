import {Component, inject, OnInit, signal, ChangeDetectionStrategy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgbActiveModal, NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {NotificationService} from '../../../../shared/services/notification.service';
import {
  CAUSE_ECART_OPTIONS,
  CauseEcart,
  IGapEntry,
  IGapLine
} from '../../models/gap-analysis.model';
import {GapAnalysisApiService} from '../../data-access/services/gap-analysis-api.service';
import {ButtonComponent, CardComponent, DataTableComponent, SelectSearchComponent} from '../../../../shared/ui';

interface GapLineVM extends IGapLine {
  selectedCause: CauseEcart | null;
  comment: string;
}

@Component({
  selector: 'app-gap-analysis-modal',
  imports: [CommonModule, FormsModule, ButtonComponent, SelectSearchComponent, DataTableComponent, NgbTooltip, CardComponent],
  templateUrl: './gap-analysis-modal.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './gap-analysis-modal.component.scss',
})
export class GapAnalysisModalComponent implements OnInit {
  inventoryId!: number;

  lines = signal<GapLineVM[]>([]);
  loading = signal(false);
  saving = signal(false);

  readonly causeOptions = CAUSE_ECART_OPTIONS;

  private readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(GapAnalysisApiService);
  private readonly notificationService = inject(NotificationService);

  get totalGapLines(): number {
    return this.lines().length;
  }

  get qualifiedCount(): number {
    return this.lines().filter(l => l.selectedCause && l.selectedCause !== 'INCONNU').length;
  }

  ngOnInit(): void {
    this.loading.set(true);
    this.api.getGapLines(this.inventoryId).subscribe({
      next: lines => {
        this.lines.set(lines.map(l => ({
          ...l,
          selectedCause: (l.existingCause as CauseEcart) ?? 'INCONNU',
          comment: l.existingComment ?? '',
        })));
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  save(): void {
    this.saving.set(true);
    const entries: IGapEntry[] = this.lines()
      .filter(l => l.selectedCause)
      .map(l => ({lineId: l.lineId, cause: l.selectedCause!, commentaire: l.comment || undefined}));

    this.api.saveAnalysis(this.inventoryId, entries).subscribe({
      next: () => {
        this.saving.set(false);
        this.activeModal.close('saved');
      },
      error: () => {
        this.saving.set(false);
        this.notificationService.error("Échec de l'enregistrement", 'Erreur');
      },
    });
  }

  skip(): void {
    this.activeModal.dismiss();
  }

  getCauseSeverity(cause: CauseEcart | null): string {
    switch (cause) {
      case 'VOL':
        return 'danger';
      case 'CASSE':
        return 'warning';
      case 'PEREMPTION':
        return 'info';
      case 'INCONNU':
        return 'secondary';
      default:
        return 'secondary';
    }
  }
}
