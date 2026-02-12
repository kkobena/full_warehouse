import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NotificationService } from '../../../../shared/services/notification.service';
import { TauriPrinterService } from '../../../../shared/services/tauri-printer.service';
import { SaleId } from '../../../../shared/model/sales.model';
import { SalesApiService } from './sales-api.service';
import { handleBlobForTauri } from '../../../../shared/util/tauri-util';

/**
 * PrintService
 *
 * Service for printing sales documents (invoices, receipts)
 * Handles both server-side PDF generation and direct printing via Tauri
 *
 * Supports:
 * - Invoice printing (facture)
 * - Receipt printing (ticket)
 * - Tauri environment detection (desktop app)
 * - ESC/POS printing for thermal printers
 * - PDF preview and download (web browser)
 *
 * @example
 * this.printService.printInvoice(saleId).subscribe();
 * this.printService.printReceipt(saleId).subscribe();
 */
@Injectable({ providedIn: 'root' })
export class PrintService {
  private readonly http = inject(HttpClient);
  private readonly notificationService = inject(NotificationService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly apiService = inject(SalesApiService);

  private readonly API_URL = 'api/sales';

  /**
   * Print invoice for a sale (facture)
   * Uses Tauri for desktop app (ESC/POS), PDF for web browser
   * @param saleId - ID of the sale
   *
   */

  printInvoice(saleId: SaleId): void {
    this.apiService.printInvoice(saleId).subscribe(blob => {
      if (this.tauriPrinterService.isRunningInTauri()) {
        handleBlobForTauri(blob, 'facture-client');
      } else {
        window.open(URL.createObjectURL(blob));
      }
    });
  }

  /**
   * Print receipt for a sale (ticket de caisse)
   * Uses Tauri for desktop app, PDF for web browser
   * @param saleId - ID of the sale
   */
  printReceipt(saleId: SaleId): Observable<void> {
    // Si dans Tauri, utiliser l'impression ESC/POS directe
    if (this.tauriPrinterService.isRunningInTauri()) {
      return this.printReceiptForTauri(saleId);
    }

    // Sinon, impression receipt backend
    return new Observable(observer => {
      this.apiService.printReceipt(saleId).subscribe({
        next: () => {
          observer.next();
          observer.complete();
        },
        error: err => {
          this.notificationService.error("Erreur d'impression", "Impossible d'imprimer le ticket");
          observer.error(err);
        },
      });
    });
  }

  /**
   * Print receipt using Tauri (ESC/POS for thermal printer)
   * @param saleId - ID of the sale
   */
  private printReceiptForTauri(saleId: SaleId): Observable<void> {
    return new Observable(observer => {
      this.apiService.getEscPosReceiptForTauri(saleId, false).subscribe({
        next: async (escposData: ArrayBuffer) => {
          try {
            await this.tauriPrinterService.printEscPosFromBuffer(escposData);
            observer.next();
            observer.complete();
          } catch (error) {
            this.notificationService.error("Erreur d'impression", "Impossible d'imprimer sur l'imprimante thermique");
            console.error('Tauri print error:', error);
            observer.error(error);
          }
        },
        error: (err: unknown) => {
          this.notificationService.error("Erreur d'impression", "Impossible de récupérer les données d'impression");
          console.error('Get ESC/POS data error:', err);
          observer.error(err);
        },
      });
    });
  }

  /**
   * Open PDF preview in new window/tab
   */
  private openPdfPreview(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const newWindow = window.open(url, '_blank');

    if (!newWindow) {
      this.notificationService.warning('Popup bloqué', 'Veuillez autoriser les popups pour prévisualiser le document');
      // Fallback to download
      this.downloadBlob(blob, filename);
    } else {
      // Clean up URL after window is loaded
      newWindow.onload = () => {
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      };
    }
  }

  /**
   * Print PDF directly using browser print dialog
   */
  private printPdf(blob: Blob): void {
    const url = URL.createObjectURL(blob);
    const iframe = document.createElement('iframe');

    iframe.style.display = 'none';
    iframe.src = url;

    document.body.appendChild(iframe);

    iframe.onload = () => {
      setTimeout(() => {
        iframe.contentWindow?.print();

        // Clean up after print dialog closes
        setTimeout(() => {
          document.body.removeChild(iframe);
          URL.revokeObjectURL(url);
        }, 1000);
      }, 100);
    };
  }

  /**
   * Download blob as file
   */
  private downloadBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');

    link.href = url;
    link.download = filename;
    link.style.display = 'none';

    document.body.appendChild(link);
    link.click();

    // Clean up
    setTimeout(() => {
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    }, 100);
  }
}
