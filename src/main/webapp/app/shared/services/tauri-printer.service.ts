import { Injectable } from '@angular/core';

export interface PrinterInfo {
  name: string;
  isDefault: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class TauriPrinterService {
  private tauriInvoke: any = null;

  constructor() {
   void this.initializeTauri();
  }

  /**
   * Initialize Tauri invoke function
   */
  private async initializeTauri(): Promise<void> {
    if (typeof window === 'undefined') {
      return;
    }

    // Try global API first (withGlobalTauri: true)
    // @ts-ignore
    if (window.__TAURI__?.core?.invoke) {
      // @ts-ignore
      this.tauriInvoke = window.__TAURI__.core.invoke;
      console.log('Tauri initialized via global API');
      return;
    }

    // Fallback to modern import method
    try {
      const { invoke } = await import('@tauri-apps/api/core');
      this.tauriInvoke = invoke;
      console.log('Tauri initialized via ES module import');
    } catch (error) {
      console.log('Tauri not available:', error);
    }
  }

  /**
   * Check if running in Tauri environment
   */
  isRunningInTauri(): boolean {
    if (typeof window === 'undefined') {
      return false;
    }

    // Check for Tauri runtime internals (only exists in actual Tauri app, not browser)
    // @ts-ignore
    return !!window.__TAURI_INTERNALS__;
  }

  /**
   * Get the Tauri invoke function
   */
  private async getInvoke(): Promise<any> {
    if (this.tauriInvoke) {
      return this.tauriInvoke;
    }

    // Wait a bit for initialization
    await new Promise(resolve => setTimeout(resolve, 100));
    await this.initializeTauri();

    if (!this.tauriInvoke) {
      throw new Error('Tauri is not available');
    }

    return this.tauriInvoke;
  }

  /**
   * Get list of available printers on the local machine
   */
  async getAvailablePrinters(): Promise<PrinterInfo[]> {
    if (!this.isRunningInTauri()) {
      console.warn('Not running in Tauri, cannot detect printers');
      return [];
    }

    try {
      const invoke = await this.getInvoke();
      const printers = (await invoke('get_printers')) as PrinterInfo[];
      return printers;
    } catch (error) {

      return [];
    }
  }

  /**
   * Get the default printer
   */
  async getDefaultPrinter(): Promise<PrinterInfo | null> {
    const printers = await this.getAvailablePrinters();
    return printers.find(p => p.isDefault) || printers[0] || null;
  }

  /**
   * Print PNG image data to a specific printer
   * @param imageBase64 Base64 encoded PNG image
   * @param printerName Name of the printer (optional, uses default if not specified)
   */
  async printImage(imageBase64: string, printerName?: string): Promise<void> {
    if (!this.isRunningInTauri()) {
      throw new Error('Tauri printing is only available in Tauri application');
    }

    try {
      const invoke = await this.getInvoke();

      // If no printer specified, get the default
      if (!printerName) {
        const defaultPrinter = await this.getDefaultPrinter();
        if (!defaultPrinter) {
          throw new Error('No printer available');
        }
        printerName = defaultPrinter.name;
      }

      await invoke('print_image', {
        imageData: imageBase64,
        printerName: printerName
      });

      console.log(`Image sent to printer: ${printerName}`);
    } catch (error) {
      console.error('Error printing image:', error);
      throw error;
    }
  }

  /**
   * Print multiple pages (receipt with multiple pages)
   * @param pages Array of base64 encoded PNG images
   * @param printerName Name of the printer (optional, uses default if not specified)
   */
  async printPages(pages: string[], printerName?: string): Promise<void> {
    if (!pages || pages.length === 0) {
      throw new Error('No pages to print');
    }

    // Get printer name once for all pages
    if (!printerName) {
      const defaultPrinter = await this.getDefaultPrinter();
      if (!defaultPrinter) {
        throw new Error('No printer available');
      }
      printerName = defaultPrinter.name;
    }

    // Print each page sequentially
    for (let i = 0; i < pages.length; i++) {
      console.log(`Printing page ${i + 1}/${pages.length}...`);
      await this.printImage(pages[i], printerName);

      // Small delay between pages to avoid overwhelming the printer
      if (i < pages.length - 1) {
        await this.delay(200);
      }
    }

    console.log(`Successfully printed ${pages.length} page(s)`);
  }

  /**
   * Open browser print dialog with the image (fallback for web)
   * @param pages Array of base64 encoded PNG images
   */
  printInBrowser(pages: string[]): void {
    const printWindow = window.open('', '_blank');
    if (!printWindow) {
      console.error('Failed to open print window');
      return;
    }

    printWindow.document.write(`
      <html>
        <head>
          <title>Impression ticket</title>
          <style>
            @media print {
              body { margin: 0; }
              img {
                display: block;
                max-width: 100%;
                page-break-after: always;
              }
              img:last-child {
                page-break-after: auto;
              }
            }
            body {
              margin: 0;
              padding: 10px;
            }
            img {
              display: block;
              margin-bottom: 10px;
              max-width: 100%;
            }
          </style>
        </head>
        <body>
    `);

    pages.forEach((page, index) => {
      printWindow.document.write(
        `<img src="data:image/png;base64,${page}" alt="Page ${index + 1}" />`
      );
    });

    printWindow.document.write('</body></html>');
    printWindow.document.close();

    // Wait for images to load before printing
    printWindow.onload = () => {
      setTimeout(() => {
        printWindow.print();
        // Close window after printing (optional)
        // printWindow.close();
      }, 500);
    };
  }

  /**
   * Utility function to add delay
   */
  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  /**
   * Print receipt - automatically detects environment and prints accordingly
   * @param pages Array of base64 encoded PNG images
   * @param printerName Optional printer name (for Tauri)
   */
  async printReceipt(pages: string[], printerName?: string): Promise<void> {
    if (this.isRunningInTauri()) {
      await this.printPages(pages, printerName);
    } else {
      this.printInBrowser(pages);
    }
  }
}
