import { inject, Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { TauriPrinterService } from './tauri-printer.service';
import { handleBlobForTauri } from '../util/tauri-util';

export type DownloadFormat = 'pdf' | 'excel' | 'csv';

const MIME_TYPES: Record<DownloadFormat, string> = {
  pdf: 'application/pdf',
  excel: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  csv: 'text/csv',
};

const FILE_EXTENSIONS: Record<DownloadFormat, string> = {
  pdf: 'pdf',
  excel: 'xlsx',
  csv: 'csv',
};

/**
 * Service utilitaire centralisé pour le téléchargement de fichiers Blob.
 *
 * Gère de manière transparente les deux environnements :
 * - **Navigateur** : PDF → `window.open`, Excel/CSV → balise `<a>` avec download
 * - **Tauri desktop** : délègue à `handleBlobForTauri` (boîte de dialogue "Enregistrer sous")
 *
 * Usage minimal :
 * ```typescript
 * private readonly downloadService = inject(BlobDownloadService);
 *
 * exportToPdf(): void {
 *   this.downloadService.downloadFromObservable(
 *     this.myService.exportToPdf(params),
 *     'mon-rapport',
 *     'pdf',
 *     () => this.spinner.show(),
 *     () => this.spinner.hide(),
 *     () => this.alert.showError("Erreur lors de l'export PDF"),
 *   );
 * }
 * ```
 */
@Injectable({ providedIn: 'root' })
export class BlobDownloadService {
  private readonly tauriPrinterService = inject(TauriPrinterService);

  // ─────────────────────────────────────────────────────────────────────────
  // Méthodes de bas niveau — travaillent directement sur un Blob
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Télécharge un PDF.
   * - Navigateur : ouvre dans un nouvel onglet.
   * - Tauri : boîte de dialogue "Enregistrer sous".
   */
  downloadPdf(blob: Blob, fileName: string): void {
    this.dispatch(blob, fileName, 'pdf');
  }

  /**
   * Télécharge un fichier Excel (.xlsx).
   * - Navigateur : déclenche le téléchargement via une balise `<a>`.
   * - Tauri : boîte de dialogue "Enregistrer sous".
   */
  downloadExcel(blob: Blob, fileName: string): void {
    this.dispatch(blob, fileName, 'excel');
  }

  /**
   * Télécharge un fichier CSV.
   * - Navigateur : déclenche le téléchargement via une balise `<a>`.
   * - Tauri : boîte de dialogue "Enregistrer sous".
   */
  downloadCsv(blob: Blob, fileName: string): void {
    this.dispatch(blob, fileName, 'csv');
  }

  /**
   * Méthode générique — choisit le comportement selon le format.
   *
   * @param blob     Données binaires reçues du backend
   * @param fileName Nom de base du fichier (sans extension ni timestamp)
   * @param format   'pdf' | 'excel' | 'csv'
   */
  download(blob: Blob, fileName: string, format: DownloadFormat): void {
    this.dispatch(blob, fileName, format);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Méthode de haut niveau — souscrit à un Observable<HttpResponse<Blob>>
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Souscrit à un `Observable<HttpResponse<Blob>>`, gère le spinner et les erreurs,
   * puis déclenche le téléchargement.
   *
   * @param source$    Observable retourné par un service HTTP (ex: `myService.exportToPdf(...)`)
   * @param fileName   Nom de base du fichier (sans extension ni timestamp)
   * @param format     'pdf' | 'excel' | 'csv'
   * @param onStart    Callback appelé avant la souscription (ex: `() => spinner.show()`)
   * @param onFinalize Callback appelé en fin de souscription, succès ou erreur (ex: `() => spinner.hide()`)
   * @param onError    Callback appelé en cas d'erreur (ex: `() => alert.showError(...)`) — optionnel
   */
  downloadFromObservable(
    source$: Observable<HttpResponse<Blob>>,
    fileName: string,
    format: DownloadFormat='pdf',
    onStart?: () => void,
    onFinalize?: () => void,
    onError?: () => void,
  ): void {
    onStart?.();

    const piped$ = onFinalize ? source$.pipe(finalize(onFinalize)) : source$;

    piped$.subscribe({
      next: (res: HttpResponse<Blob>) => {
        if (res.body) {
          const mimeType = MIME_TYPES[format];
          const blob = new Blob([res.body], { type: mimeType });
          this.dispatch(blob, fileName, format);
        }
      },
      error: () => onError?.(),
    });
  }


  private dispatch(blob: Blob, fileName: string, format: DownloadFormat): void {
    if (this.tauriPrinterService.isRunningInTauri()) {
      handleBlobForTauri(blob, fileName, format);
      return;
    }

    if (format === 'pdf') {
      this.openInNewTab(blob);
    } else {
      this.triggerDownload(blob, fileName, FILE_EXTENSIONS[format]);
    }
  }

  /** Ouvre le Blob dans un nouvel onglet (PDF). */
  private openInNewTab(blob: Blob): void {
    const url = URL.createObjectURL(blob);
    window.open(url);

  }

  /** Déclenche un téléchargement de fichier via une balise <a> (Excel, CSV). */
  private triggerDownload(blob: Blob, fileName: string, ext: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${fileName}-${new Date().getTime()}.${ext}`;
    link.click();
    window.URL.revokeObjectURL(url);
  }
}

