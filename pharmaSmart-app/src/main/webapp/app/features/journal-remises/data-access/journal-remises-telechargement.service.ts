import { inject, Injectable, signal } from '@angular/core';
import { saveAs } from 'file-saver';
import { ErrorService } from 'app/shared/error.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { AppSplitButtonItem } from 'app/shared/ui/split-button/split-button.component';
import { FormatExport, IFiltreJournal, NatureJournal, nomFichier } from '../models/journal-remises.model';
import { JournalRemisesApiService } from './journal-remises-api.service';

/** Télécharge un export du journal ; un seul à la fois, le bouton le dit. */
@Injectable({ providedIn: 'root' })
export class JournalRemisesTelechargementService {
  readonly enCours = signal(false);

  private readonly api = inject(JournalRemisesApiService);
  private readonly notification = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  telecharger(format: FormatExport, nature: NatureJournal | null, filtre: IFiltreJournal): void {
    this.enCours.set(true);
    this.api.exporter(format, nature, filtre).subscribe({
      next: blob => {
        this.enCours.set(false);
        saveAs(blob, nomFichier(format, nature, filtre));
      },
      error: (err: unknown) => {
        this.enCours.set(false);
        this.notification.error(this.errorService.getErrorMessage(err, 'Export impossible.'));
      },
    });
  }

  /** Les entrées du bouton « Exporter » : le PDF est l'action principale, le classeur et le CSV au menu. */
  formats(nature: NatureJournal | null, filtre: () => IFiltreJournal): AppSplitButtonItem[] {
    return [
      { label: 'PDF', icon: 'pi pi-file-pdf', command: () => this.telecharger('PDF', nature, filtre()) },
      { label: 'Excel', icon: 'pi pi-file-excel', command: () => this.telecharger('XLSX', nature, filtre()) },
      { label: 'CSV', icon: 'pi pi-file', command: () => this.telecharger('CSV', nature, filtre()) },
    ];
  }
}
