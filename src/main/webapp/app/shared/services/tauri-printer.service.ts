import { inject, Injectable } from '@angular/core';
import { CustomerDisplayEscPosService, CustomerDisplayConnectionConfig } from '../customer-display/customer-display-escpos.service';
import { PosteService } from '../../entities/poste/poste.service';
import { firstValueFrom } from 'rxjs';

export interface PrinterInfo {
  name: string;
  isDefault: boolean;
}

export interface CustomerDisplayConfig {
  enabled: boolean;
  connectionType: 'SERIAL' | 'USB' | 'NETWORK';
  serialPort?: string;
  baudRate?: number;
  ipAddress?: string;
  port?: number;
}

@Injectable({
  providedIn: 'root',
})
export class TauriPrinterService {
  private tauriInvoke: any = null;
  private displayConfig: CustomerDisplayConnectionConfig | null = null;
  private displayEnabled = false;
  private readonly customerDisplayService = inject(CustomerDisplayEscPosService);
  private readonly posteService = inject(PosteService);

  constructor() {
    void this.initializeTauri();
    void this.initializeCustomerDisplay();
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
   * Get list of available printers on the local machine
   */
  async getAvailablePrinters(): Promise<PrinterInfo[]> {
    if (!this.isRunningInTauri()) {
      return [];
    }

    try {
      const invoke = await this.getInvoke();
      return (await invoke('get_printers')) as PrinterInfo[];
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
        printerName: printerName,
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
      await this.printImage(pages[i], printerName);

      // Small delay between pages to avoid overwhelming the printer
      if (i < pages.length - 1) {
        await this.delay(200);
      }
    }
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
        printerName: printerName,
      });
    } catch (error) {
      console.error('Error printing ESC/POS receipt:', error);
      throw error;
    }
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

      return;
    }

    // Fallback to modern import method
    try {
      const { invoke } = await import('@tauri-apps/api/core');
      this.tauriInvoke = invoke;
    } catch (error) {
      console.log('Tauri not available:', error);
    }
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
   * Utility function to add delay
   */
  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
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

  // ============================================
  // Customer Display Methods
  // ============================================

  /**
   * Initialize customer display configuration
   * Fetches configuration from the current poste via API
   */
  private async initializeCustomerDisplay(): Promise<void> {
    if (!this.isRunningInTauri()) {
      return;
    }

    try {
      // Fetch current poste configuration from API
      const response = await firstValueFrom(this.posteService.getCurrentPoste());
      const posteData = response.body;

      if (posteData && posteData.customerDisplay) {
        // Determine connection type based on port format
        const connectionType = this.determineConnectionType(posteData.customerDisplayPort);

        this.displayConfig = {
          connectionType: connectionType,
          serialPort: connectionType === 'SERIAL' ? posteData.customerDisplayPort : undefined,
          baudRate: 9600, // Default baud rate for serial
          ipAddress: connectionType === 'NETWORK' ? posteData.address : undefined,
          port: connectionType === 'NETWORK' ? 9100 : undefined, // Default ESC/POS network port
        };
        this.displayEnabled = true;

        // Save to localStorage for offline fallback
        this.saveDisplayConfig({
          enabled: true,
          connectionType: connectionType,
          serialPort: this.displayConfig.serialPort,
          baudRate: this.displayConfig.baudRate,
          ipAddress: this.displayConfig.ipAddress,
          port: this.displayConfig.port,
        });
      } else {
        // Customer display not enabled for this poste
        this.displayEnabled = false;
        this.displayConfig = null;
      }
    } catch (error) {
      console.error('Failed to fetch poste configuration, falling back to localStorage:', error);

      // Fallback to localStorage configuration
      const savedConfig = this.loadDisplayConfig();
      if (savedConfig && savedConfig.enabled) {
        this.displayConfig = {
          connectionType: savedConfig.connectionType,
          serialPort: savedConfig.serialPort,
          baudRate: savedConfig.baudRate || 9600,
          ipAddress: savedConfig.ipAddress,
          port: savedConfig.port || 9100,
        };
        this.displayEnabled = true;
      } else {
        // Default configuration (disabled)
        this.displayConfig = null;
        this.displayEnabled = false;
      }
    }
  }

  /**
   * Determine connection type based on port/address format
   * @param port Port identifier (e.g., "COM3", "/dev/ttyUSB0", or "USB")
   */
  private determineConnectionType(port?: string): 'SERIAL' | 'USB' | 'NETWORK' {
    if (!port) {
      return 'SERIAL'; // Default to serial
    }

    const portUpper = port.toUpperCase();

    // Check for USB identifiers
    if (portUpper.includes('USB') && !portUpper.includes('TTY')) {
      return 'USB';
    }

    // Check for serial port identifiers
    // Windows: COM1, COM2, etc.
    // Linux/Mac: /dev/ttyUSB0, /dev/ttyS0, /dev/tty.usbserial, etc.
    if (portUpper.startsWith('COM') || portUpper.includes('/DEV/TTY') || portUpper.includes('SERIAL')) {
      return 'SERIAL';
    }

    // If it looks like an IP address or contains "NET", assume network
    if (portUpper.includes('NET') || /^\d+\.\d+\.\d+\.\d+/.test(port)) {
      return 'NETWORK';
    }

    // Default to SERIAL
    return 'SERIAL';
  }

  /**
   * Check if customer display is enabled
   */
  isCustomerDisplayEnabled(): boolean {
    return this.isRunningInTauri() && this.displayEnabled && this.displayConfig !== null;
  }

  /**
   * Refresh customer display configuration from API
   * Useful when poste configuration changes
   */
  async refreshCustomerDisplayConfig(): Promise<void> {
    await this.initializeCustomerDisplay();
  }

  /**
   * Configure customer display
   * @param config Display configuration
   */
  setCustomerDisplayConfig(config: CustomerDisplayConfig): void {
    this.displayConfig = {
      connectionType: config.connectionType,
      serialPort: config.serialPort,
      baudRate: config.baudRate || 9600,
      ipAddress: config.ipAddress,
      port: config.port || 9100,
    };
    this.displayEnabled = config.enabled;
    this.saveDisplayConfig(config);
  }

  /**
   * Get current customer display configuration
   */
  getCustomerDisplayConfig(): CustomerDisplayConfig | null {
    return this.loadDisplayConfig();
  }

  /**
   * List available serial ports for customer display
   */
  async listSerialPorts(): Promise<string[]> {
    if (!this.isRunningInTauri()) {
      return [];
    }

    try {
      const invoke = await this.getInvoke();
      return (await invoke('list_serial_ports')) as string[];
    } catch (error) {
      console.error('Error listing serial ports:', error);
      return [];
    }
  }

  /**
   * Test customer display connection
   */
  async testCustomerDisplayConnection(): Promise<boolean> {
    if (!this.isCustomerDisplayEnabled()) {
      return false;
    }

    try {
      const invoke = await this.getInvoke();
      await invoke('test_customer_display_connection', { config: this.displayConfig });
      return true;
    } catch (error) {
      console.error('Customer display connection test failed:', error);
      return false;
    }
  }

  /**
   * Show welcome message on customer display
   * @param storeName Store name to display
   */
  async showWelcomeMessage(storeName: string): Promise<void> {
    if (!this.isCustomerDisplayEnabled()) {
      return;
    }

    try {
      await this.customerDisplayService.displayWelcomeMessage(storeName, this.displayConfig!);
    } catch (error) {
      console.error('Failed to show welcome message on customer display:', error);
    }
  }

  /**
   * Update customer display with user/cashier info
   * @param userName User name to display
   */
  async updateDisplayForUser(userName: string): Promise<void> {
    if (!this.isCustomerDisplayEnabled()) {
      return;
    }

    try {
      await this.customerDisplayService.displayUserMessage(userName, this.displayConfig!);
    } catch (error) {
      console.error('Failed to update customer display for user:', error);
    }
  }

  /**
   * Update customer display with product information
   * @param productName Product name
   * @param qty Quantity
   * @param price Unit price
   */
  async updateDisplayForProduct(productName: string, qty: number, price: number): Promise<void> {
    if (!this.isCustomerDisplayEnabled()) {
      return;
    }

    try {
      await this.customerDisplayService.displaySalesData(productName, qty, price, this.displayConfig!);
    } catch (error) {
      console.error('Failed to update customer display for product:', error);
    }
  }

  /**
   * Update customer display with sale total
   * @param total Sale total amount
   */
  async updateDisplayForTotal(total: number): Promise<void> {
    if (!this.isCustomerDisplayEnabled()) {
      return;
    }

    try {
      await this.customerDisplayService.displaySaleTotal(total, this.displayConfig!);
    } catch (error) {
      console.error('Failed to update customer display for total:', error);
    }
  }

  /**
   * Update customer display with change amount
   * @param change Change amount
   */
  async updateDisplayForChange(change: number): Promise<void> {
    if (!this.isCustomerDisplayEnabled()) {
      return;
    }

    try {
      await this.customerDisplayService.displayChange(change, this.displayConfig!);
    } catch (error) {
      console.error('Failed to update customer display for change:', error);
    }
  }

  /**
   * Display custom two-line message on customer display
   * @param line1 First line text
   * @param line2 Second line text
   * @param align1 Alignment for line 1 (default 'left')
   * @param align2 Alignment for line 2 (default 'left')
   */
  async displayCustomMessage(line1: string, line2: string, align1 = 'left', align2 = 'left'): Promise<void> {
    if (!this.isCustomerDisplayEnabled()) {
      return;
    }

    try {
      await this.customerDisplayService.displayTwoLines(line1, line2, this.displayConfig!, align1, align2);
    } catch (error) {
      console.error('Failed to display custom message on customer display:', error);
    }
  }

  /**
   * Clear customer display
   */
  async clearCustomerDisplay(): Promise<void> {
    if (!this.isCustomerDisplayEnabled()) {
      return;
    }

    try {
      await this.customerDisplayService.clearDisplay(this.displayConfig!);
    } catch (error) {
      console.error('Failed to clear customer display:', error);
    }
  }

  /**
   * Reset customer display to default state
   */
  async resetCustomerDisplay(): Promise<void> {
    if (!this.isCustomerDisplayEnabled()) {
      return;
    }

    try {
      await this.customerDisplayService.resetDisplay(this.displayConfig!);
    } catch (error) {
      console.error('Failed to reset customer display:', error);
    }
  }

  /**
   * Set customer display brightness
   * @param level Brightness level (1-4, 4 = brightest)
   */
  async setDisplayBrightness(level: number): Promise<void> {
    if (!this.isCustomerDisplayEnabled()) {
      return;
    }

    try {
      await this.customerDisplayService.setBrightness(level, this.displayConfig!);
    } catch (error) {
      console.error('Failed to set customer display brightness:', error);
    }
  }

  // ============================================
  // Configuration Persistence
  // ============================================

  /**
   * Save display configuration to localStorage
   */
  private saveDisplayConfig(config: CustomerDisplayConfig): void {
    if (typeof window !== 'undefined' && window.localStorage) {
      localStorage.setItem('customerDisplayConfig', JSON.stringify(config));
    }
  }

  /**
   * Load display configuration from localStorage
   */
  private loadDisplayConfig(): CustomerDisplayConfig | null {
    if (typeof window !== 'undefined' && window.localStorage) {
      const saved = localStorage.getItem('customerDisplayConfig');
      if (saved) {
        try {
          return JSON.parse(saved) as CustomerDisplayConfig;
        } catch (error) {
          console.error('Failed to parse saved display config:', error);
        }
      }
    }
    return null;
  }

  /**
   * Enable customer display
   */
  enableCustomerDisplay(): void {
    this.displayEnabled = true;
    const config = this.loadDisplayConfig();
    if (config) {
      config.enabled = true;
      this.saveDisplayConfig(config);
    }
  }

  /**
   * Disable customer display
   */
  disableCustomerDisplay(): void {
    this.displayEnabled = false;
    const config = this.loadDisplayConfig();
    if (config) {
      config.enabled = false;
      this.saveDisplayConfig(config);
    }
  }
}
