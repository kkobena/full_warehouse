import {ChangeDetectionStrategy, Component, computed, inject, OnInit, signal} from '@angular/core';
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
import {
  AppTableLazyLoadEvent,
  ButtonComponent,
  CardComponent,
  DataTableComponent,
  SelectSearchComponent,
} from '../../../../shared/ui';

interface GapLineVM extends IGapLine {
  selectedCause: CauseEcart | null;
  comment: string;
}

/** Qualification saisie par l'opérateur, conservée hors de la page courante. */
interface PendingChange {
  cause: CauseEcart | null;
  comment: string;
}

@Component({
  selector: 'app-gap-analysis-modal',
  imports: [CommonModule, FormsModule, ButtonComponent, SelectSearchComponent, DataTableComponent, NgbTooltip, CardComponent],
  templateUrl: './gap-analysis-modal.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './gap-analysis-modal.component.scss',
})
export class GapAnalysisModalComponent implements OnInit {
  inventoryId!: number;

  /** Page courante uniquement — la liste complète peut compter plusieurs milliers de lignes. */
  lines = signal<GapLineVM[]>([]);
  totalItems = signal(0);
  /** Nombre de lignes déjà qualifiées en base, tous écrans confondus. */
  savedCount = signal(0);
  loading = signal(false);
  saving = signal(false);

  first = signal(0);
  rows = signal(10);
  readonly rowsPerPageOptions = [10, 20, 50, 100];

  readonly causeOptions = CAUSE_ECART_OPTIONS;

  /**
   * Saisies non enregistrées, indexées par ligne.
   *
   * Indispensable dès lors que l'écran est paginé : `lines()` est remplacé à chaque
   * changement de page, une saisie faite page 1 serait perdue en revenant page 2. Le
   * bouton « Enregistrer » ne soumet que ce dictionnaire.
   */
  private readonly pending = signal(new Map<number, PendingChange>());

  readonly pendingCount = computed(() => this.pending().size);

  private readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(GapAnalysisApiService);
  private readonly notificationService = inject(NotificationService);

  ngOnInit(): void {
    this.loadPage();
    this.loadSavedCount();
  }

  onLazyLoad(_event: AppTableLazyLoadEvent): void {
    this.loadPage();
  }

  onCauseChange(line: GapLineVM, cause: CauseEcart | null): void {
    line.selectedCause = cause;
    this.trackChange(line);
  }

  onCommentChange(line: GapLineVM, comment: string): void {
    line.comment = comment;
    this.trackChange(line);
  }

  save(): void {
    const entries: IGapEntry[] = [...this.pending().entries()].map(([lineId, change]) => ({
      lineId,
      cause: change.cause!,
      commentaire: change.comment || undefined,
    }));

    if (entries.length === 0) {
      this.activeModal.close('skipped');
      return;
    }

    this.saving.set(true);
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

  private loadPage(): void {
    const size = this.rows();
    const page = size > 0 ? Math.floor(this.first() / size) : 0;
    this.loading.set(true);
    this.api.getGapLines(this.inventoryId, page, size).subscribe({
      next: res => {
        const changes = this.pending();
        this.lines.set((res.body ?? []).map(line => {
          // Une saisie non enregistrée prime sur ce que renvoie le serveur : sans cela,
          // revenir sur une page déjà modifiée écraserait le travail de l'opérateur.
          const change = changes.get(line.lineId);
          return {
            ...line,
            selectedCause: change ? change.cause : (line.existingCause ?? 'INCONNU'),
            comment: change ? change.comment : (line.existingComment ?? ''),
          };
        }));
        this.totalItems.set(Number(res.headers.get('X-Total-Count')) || 0);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  /**
   * Le compteur « causes enregistrées » porte sur tout l'inventaire, il ne peut donc pas se
   * déduire de la page affichée. Le résumé par cause, déjà agrégé côté serveur, le fournit.
   */
  private loadSavedCount(): void {
    this.api.getSummary(this.inventoryId).subscribe({
      next: summary => this.savedCount.set(summary.reduce((total, row) => total + row.nbProduits, 0)),
      error: () => this.savedCount.set(0),
    });
  }

  private trackChange(line: GapLineVM): void {
    this.pending.update(changes => {
      const next = new Map(changes);
      next.set(line.lineId, {cause: line.selectedCause, comment: line.comment});
      return next;
    });
  }
}
