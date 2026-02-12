import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { NotificationService } from '../../../../shared/services/notification.service';
import { TauriPrinterService } from '../../../../shared/services/tauri-printer.service';
import { SalesService } from '../../../../entities/sales/sales.service';
import { SaleId } from '../../../../shared/model/sales.model';

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
  private readonly salesService = inject(SalesService);

  private readonly API_URL = 'api/sales';

  /**
   * Print invoice for a sale (facture)
   * Uses Tauri for desktop app (ESC/POS), PDF for web browser
   * @param saleId - ID of the sale
   * @param preview - If true, opens preview instead of direct print (web only)
   */
  printInvoice(saleId: SaleId, preview: boolean = false): Observable<void> {
    // Si dans Tauri, utiliser l'impression ESC/POS directe
    if (this.tauriPrinterService.isRunningInTauri()) {
      return this.printInvoiceForTauri(saleId);
    }

    // Sinon, utiliser le PDF via HTTP
    const url = `${this.API_URL}/${saleId}/print-invoice`;

    return this.http.post(url, { preview }, { responseType: 'blob' }).pipe(
      tap((blob: Blob) => {
        if (preview) {
          this.openPdfPreview(blob, `facture-${saleId}.pdf`);
        } else {
          this.printPdf(blob);
        }
      }),
      catchError(error => {
        this.notificationService.error("Erreur d'impression", "Impossible d'imprimer la facture");
        console.error('Print invoice error:', error);
        return of(void 0);
      }),
      tap(() => void 0),
    ) as Observable<void>;
  }

  /**
   * Print invoice using Tauri (ESC/POS for thermal printer)
   * @param saleId - ID of the sale
   */
  private printInvoiceForTauri(saleId: SaleId): Observable<void> {
    return new Observable(observer => {
      this.salesService.getEscPosReceiptForTauri(saleId, false).subscribe({
        next: async (escposData: ArrayBuffer) => {
          try {
            await this.tauriPrinterService.printEscPosFromBuffer(escposData);
            observer.next();
            observer.complete();
          } catch (error) {
            this.notificationService.error("Erreur d'impression", "Impossible d'imprimer sur l'imprimante thermique");
            console.error('Tauri print invoice error:', error);
            observer.error(error);
          }
        },
        error: (err: unknown) => {
          this.notificationService.error("Erreur d'impression", "Impossible de récupérer les données d'impression");
          console.error('Get ESC/POS invoice data error:', err);
          observer.error(err);
        },
      });
    });
  }

  /**
   * Print receipt for a sale (ticket de caisse)
   * Uses Tauri for desktop app, PDF for web browser
   * @param saleId - ID of the sale
   * @param preview - If true, opens preview instead of direct print (web only)
   */
  printReceipt(saleId: SaleId, preview: boolean = false): Observable<void> {
    // Si dans Tauri, utiliser l'impression ESC/POS directe
    if (this.tauriPrinterService.isRunningInTauri()) {
      return this.printReceiptForTauri(saleId);
    }

    // Sinon, utiliser le service existant (PDF)
    return new Observable(observer => {
      this.salesService.printReceipt(saleId).subscribe({
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
      this.salesService.getEscPosReceiptForTauri(saleId, false).subscribe({
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
   * Print both invoice and receipt
   * @param saleId - ID of the sale
   */
  printBoth(saleId: SaleId): Observable<void> {
    // Print invoice first, then receipt
    return new Observable(observer => {
      this.printInvoice(saleId).subscribe({
        next: () => {
          setTimeout(() => {
            this.printReceipt(saleId).subscribe({
              next: () => observer.next(),
              error: err => observer.error(err),
              complete: () => observer.complete(),
            });
          }, 500); // Delay between prints
        },
        error: err => observer.error(err),
      });
    });
  }

  /**
   * Download PDF document
   * @param saleId - ID of the sale
   * @param type - 'invoice' or 'receipt'
   */
  downloadPdf(saleId: SaleId, type: 'invoice' | 'receipt'): Observable<void> {
    const endpoint = type === 'invoice' ? 'print-invoice' : 'print-receipt';
    const filename = type === 'invoice' ? `facture-${saleId}.pdf` : `ticket-${saleId}.pdf`;
    const url = `${this.API_URL}/${saleId}/${endpoint}`;

    return this.http.post(url, { download: true }, { responseType: 'blob' }).pipe(
      tap((blob: Blob) => {
        this.downloadBlob(blob, filename);
      }),
      catchError(error => {
        this.notificationService.error('Erreur de téléchargement', 'Impossible de télécharger le document');
        console.error('Download PDF error:', error);
        return of(void 0);
      }),
      tap(() => void 0),
    ) as Observable<void>;
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

  /**
   * Print sale document (wrapper method for compatibility)
   * @param saleId - ID of the sale
   * @param type - 'invoice' or 'receipt' or 'both'
   */
  printSaleDocument(saleId: SaleId, type: 'invoice' | 'receipt' | 'both' = 'invoice'): Observable<void> {
    if (type === 'both') {
      return this.printBoth(saleId);
    } else if (type === 'receipt') {
      return this.printReceipt(saleId);
    } else {
      return this.printInvoice(saleId);
    }
  }
}
