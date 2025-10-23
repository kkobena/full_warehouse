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
    }
  }

  /**
   * Print ESC/POS receipt data directly to thermal printer
   * This method is much more efficient than PNG printing - smaller payload and faster
   * @param escposData Base64 encoded ESC/POS commands
   * @param printerName Name of the printer (optional, uses default if not specified)
   */
  async printEscPos(escposData: string, printerName?: string): Promise<void> {
    if (!this.isRunningInTauri()) {
      throw new Error('ESC/POS printing is only available in Tauri application');
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

      await invoke('print_escpos', {
        escposData: escposData,
        printerName: printerName
      });

      console.log(`ESC/POS receipt sent to printer: ${printerName}`);
    } catch (error) {
      console.error('Error printing ESC/POS receipt:', error);
      throw error;
    }
  }

  /**
   * Convert ArrayBuffer to base64 string
   * Used for converting binary ESC/POS data from backend
   */
  private arrayBufferToBase64(buffer: ArrayBuffer): string {
    let binary = '';
    const bytes = new Uint8Array(buffer);
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary);
  }

  /**
   * Print ESC/POS receipt from ArrayBuffer data
   * Converts ArrayBuffer to base64 and sends to printer
   * @param escposBuffer ArrayBuffer containing ESC/POS commands from backend
   * @param printerName Name of the printer (optional, uses default if not specified)
   */
  async printEscPosFromBuffer(escposBuffer: ArrayBuffer, printerName?: string): Promise<void> {
    const base64Data = this.arrayBufferToBase64(escposBuffer);
    await this.printEscPos(base64Data, printerName);
  }
}
