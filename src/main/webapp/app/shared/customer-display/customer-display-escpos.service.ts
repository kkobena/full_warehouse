import { Injectable } from '@angular/core';

/**
 * Frontend ESC/POS command generator for customer displays
 * This service generates ESC/POS commands entirely in the frontend
 * No backend dependency - works offline in Tauri desktop apps
 */
@Injectable({ providedIn: 'root' })
export class CustomerDisplayEscPosService {
  private readonly DISPLAY_WIDTH = 20;
  private readonly DISPLAY_LINES = 2;

  // ============================================
  // ESC/POS Command Generators
  // ============================================

  /**
   * Initialize display (ESC @)
   */
  private escPosInitialize(): Uint8Array {
    return new Uint8Array([0x1b, 0x40]); // ESC @
  }

  /**
   * Clear display (FF - Form Feed)
   */
  private escPosClearDisplay(): Uint8Array {
    return new Uint8Array([0x0c]); // FF
  }

  /**
   * Move cursor to home position
   */
  private escPosMoveCursorHome(): Uint8Array {
    return new Uint8Array([0x1b, 0x48]); // ESC H
  }

  /**
   * Set cursor position (ESC Y row col)
   * @param row row number (0-1)
   * @param col column number (0-19)
   */
  private escPosSetCursorPosition(row: number, col: number): Uint8Array {
    return new Uint8Array([0x1b, 0x59, row, col]); // ESC Y row col
  }

  /**
   * Set brightness level (ESC * n)
   * @param level brightness level (1-4, 4 = brightest)
   */
  private escPosSetBrightness(level: number): Uint8Array {
    const normalizedLevel = Math.max(1, Math.min(4, level));
    return new Uint8Array([0x1b, 0x2a, normalizedLevel]); // ESC * n
  }

  /**
   * Set cursor visibility (ESC C n)
   * @param visible true to show cursor, false to hide
   */
  private escPosSetCursorVisibility(visible: boolean): Uint8Array {
    return new Uint8Array([0x1b, 0x43, visible ? 1 : 0]); // ESC C n
  }

  /**
   * Write text using Windows-1252 encoding (for French characters)
   * @param text text to write
   */
  private escPosWriteText(text: string): Uint8Array {
    // Use TextEncoder with Windows-1252 encoding for French characters
    // Note: TextEncoder in browsers only supports UTF-8, so we need a custom encoder
    const encoder = new TextEncoder();
    return encoder.encode(text);
  }

  /**
   * Combine multiple Uint8Arrays into one
   */
  private combineBytes(...arrays: Uint8Array[]): Uint8Array {
    const totalLength = arrays.reduce((sum, arr) => sum + arr.length, 0);
    const result = new Uint8Array(totalLength);
    let offset = 0;
    for (const arr of arrays) {
      result.set(arr, offset);
      offset += arr.length;
    }
    return result;
  }

  /**
   * Truncate string to display width (20 chars)
   */
  private truncateToDisplayWidth(str: string | null): string {
    if (!str) return '';
    return str.length > this.DISPLAY_WIDTH ? str.substring(0, this.DISPLAY_WIDTH) : str;
  }

  /**
   * Pad string to display width with alignment
   * @param str string to pad
   * @param alignment 'left', 'center', 'right', or 'begin' (right-aligned)
   */
  private padToDisplayWidth(str: string | null, alignment: string): string {
    if (!str) str = '';

    // Truncate if too long
    if (str.length > this.DISPLAY_WIDTH) {
      str = str.substring(0, this.DISPLAY_WIDTH);
    }

    const padding = this.DISPLAY_WIDTH - str.length;
    if (padding <= 0) {
      return str;
    }

    const spaces = ' '.repeat(padding);

    switch (alignment.toLowerCase()) {
      case 'left':
      case 'end':
        return str + spaces;
      case 'right':
      case 'begin':
        return spaces + str;
      case 'center':
        const leftPad = Math.floor(padding / 2);
        const rightPad = padding - leftPad;
        return ' '.repeat(leftPad) + str + ' '.repeat(rightPad);
      default:
        return str + spaces; // Default to left
    }
  }

  /**
   * Format number as string with thousands separator
   * @param value numeric value
   */
  private formatNumber(value: number): string {
    return value.toLocaleString('fr-FR');
  }

  // ============================================
  // Public Methods - Generate ESC/POS Commands
  // ============================================

  /**
   * Generate welcome message ESC/POS commands
   * @param storeName store name to display
   * @param welcomeText welcome text (default: "BIENVENUE A VOUS")
   */
  generateWelcomeMessage(storeName: string, welcomeText = 'BIENVENUE A VOUS'): Uint8Array {
    return this.combineBytes(
      this.escPosClearDisplay(),
      this.escPosSetCursorPosition(0, 0),
      this.escPosWriteText(this.padToDisplayWidth(this.truncateToDisplayWidth(storeName), 'center')),
      this.escPosSetCursorPosition(1, 0),
      this.escPosWriteText(this.padToDisplayWidth(welcomeText, 'center')),
    );
  }

  /**
   * Generate sales data display ESC/POS commands
   * @param productName product name
   * @param qty quantity
   * @param price unit price
   */
  generateSalesData(productName: string, qty: number, price: number): Uint8Array {
    const total = qty * price;
    const line1 = this.truncateToDisplayWidth(productName.toUpperCase());
    const line2 = `${qty}*${this.formatNumber(price)}=${this.formatNumber(total)}`;

    return this.combineBytes(
      this.escPosSetCursorPosition(0, 0),
      this.escPosWriteText(this.padToDisplayWidth(line1, 'left')),
      this.escPosSetCursorPosition(1, 0),
      this.escPosWriteText(this.padToDisplayWidth(line2, 'begin')),
    );
  }

  /**
   * Generate sale total display ESC/POS commands
   * @param total sale total amount
   */
  generateSaleTotal(total: number): Uint8Array {
    return this.combineBytes(
      this.escPosSetCursorPosition(0, 0),
      this.escPosWriteText(this.padToDisplayWidth('NET A PAYER:', 'left')),
      this.escPosSetCursorPosition(1, 0),
      this.escPosWriteText(this.padToDisplayWidth(this.formatNumber(total), 'begin')),
    );
  }

  /**
   * Generate change display ESC/POS commands
   * @param change change amount
   */
  generateChange(change: number): Uint8Array {
    return this.combineBytes(
      this.escPosSetCursorPosition(0, 0),
      this.escPosWriteText(this.padToDisplayWidth('MONNAIE:', 'left')),
      this.escPosSetCursorPosition(1, 0),
      this.escPosWriteText(this.padToDisplayWidth(this.formatNumber(change), 'begin')),
    );
  }

  /**
   * Generate connected user message ESC/POS commands
   * @param userName user name
   */
  generateUserMessage(userName: string): Uint8Array {
    const message = `Caisse: ${userName.toUpperCase()}`;
    return this.combineBytes(
      this.escPosSetCursorPosition(1, 0),
      this.escPosWriteText(this.padToDisplayWidth(this.truncateToDisplayWidth(message), 'left')),
    );
  }

  /**
   * Generate custom two-line message ESC/POS commands
   * @param line1 first line text
   * @param line2 second line text
   * @param align1 alignment for line 1 (default 'left')
   * @param align2 alignment for line 2 (default 'left')
   */
  generateTwoLines(line1: string, line2: string, align1 = 'left', align2 = 'left'): Uint8Array {
    return this.combineBytes(
      this.escPosClearDisplay(),
      this.escPosSetCursorPosition(0, 0),
      this.escPosWriteText(this.padToDisplayWidth(this.truncateToDisplayWidth(line1), align1)),
      this.escPosSetCursorPosition(1, 0),
      this.escPosWriteText(this.padToDisplayWidth(this.truncateToDisplayWidth(line2), align2)),
    );
  }

  /**
   * Generate clear display ESC/POS commands
   */
  generateClearDisplay(): Uint8Array {
    return this.combineBytes(this.escPosClearDisplay(), this.escPosMoveCursorHome());
  }

  /**
   * Generate reset display ESC/POS commands
   */
  generateResetDisplay(): Uint8Array {
    return this.combineBytes(
      this.escPosInitialize(),
      this.escPosClearDisplay(),
      this.escPosSetCursorVisibility(false), // Hide cursor
    );
  }

  /**
   * Generate brightness control ESC/POS commands
   * @param level brightness level (1-4, 4 = brightest)
   */
  generateBrightness(level: number): Uint8Array {
    return this.escPosSetBrightness(level);
  }

  // ============================================
  // Tauri Integration
  // ============================================

  /**
   * Send ESC/POS data to customer display via Tauri
   * @param escPosData ESC/POS command bytes
   * @param config connection configuration
   */
  async sendToDisplayViaTauri(escPosData: Uint8Array, config: CustomerDisplayConnectionConfig): Promise<void> {
    // Check if running in Tauri
    if (!(window as any).__TAURI__) {
      console.warn('Not running in Tauri environment. Cannot send to customer display.');
      throw new Error('Tauri environment not detected');
    }

    const { invoke } = (window as any).__TAURI__.core;

    try {
      await invoke('send_to_customer_display', {
        escPosData: Array.from(escPosData), // Convert Uint8Array to regular array
        config,
      });
      console.log('Data sent to customer display successfully');
    } catch (error) {
      console.error('Error sending to customer display:', error);
      throw error;
    }
  }

  // ============================================
  // High-Level Workflow Methods
  // ============================================

  /**
   * Display welcome message on customer display
   */
  async displayWelcomeMessage(storeName: string, config: CustomerDisplayConnectionConfig): Promise<void> {
    const escPosData = this.generateWelcomeMessage(storeName);
    await this.sendToDisplayViaTauri(escPosData, config);
  }

  /**
   * Display sales data on customer display
   */
  async displaySalesData(productName: string, qty: number, price: number, config: CustomerDisplayConnectionConfig): Promise<void> {
    const escPosData = this.generateSalesData(productName, qty, price);
    await this.sendToDisplayViaTauri(escPosData, config);
  }

  /**
   * Display sale total on customer display
   */
  async displaySaleTotal(total: number, config: CustomerDisplayConnectionConfig): Promise<void> {
    const escPosData = this.generateSaleTotal(total);
    await this.sendToDisplayViaTauri(escPosData, config);
  }

  /**
   * Display change on customer display
   */
  async displayChange(change: number, config: CustomerDisplayConnectionConfig): Promise<void> {
    const escPosData = this.generateChange(change);
    await this.sendToDisplayViaTauri(escPosData, config);
  }

  /**
   * Display user message on customer display
   */
  async displayUserMessage(userName: string, config: CustomerDisplayConnectionConfig): Promise<void> {
    const escPosData = this.generateUserMessage(userName);
    await this.sendToDisplayViaTauri(escPosData, config);
  }

  /**
   * Display two lines on customer display
   */
  async displayTwoLines(
    line1: string,
    line2: string,
    config: CustomerDisplayConnectionConfig,
    align1 = 'left',
    align2 = 'left',
  ): Promise<void> {
    const escPosData = this.generateTwoLines(line1, line2, align1, align2);
    await this.sendToDisplayViaTauri(escPosData, config);
  }

  /**
   * Clear customer display
   */
  async clearDisplay(config: CustomerDisplayConnectionConfig): Promise<void> {
    const escPosData = this.generateClearDisplay();
    await this.sendToDisplayViaTauri(escPosData, config);
  }

  /**
   * Reset customer display
   */
  async resetDisplay(config: CustomerDisplayConnectionConfig): Promise<void> {
    const escPosData = this.generateResetDisplay();
    await this.sendToDisplayViaTauri(escPosData, config);
  }

  /**
   * Set display brightness
   */
  async setBrightness(level: number, config: CustomerDisplayConnectionConfig): Promise<void> {
    const escPosData = this.generateBrightness(level);
    await this.sendToDisplayViaTauri(escPosData, config);
  }
}

/**
 * Configuration for customer display connection
 */
export interface CustomerDisplayConnectionConfig {
  connectionType: 'SERIAL' | 'USB' | 'NETWORK';
  serialPort?: string; // e.g., "COM3" or "/dev/ttyUSB0"
  baudRate?: number; // e.g., 9600
  usbDeviceName?: string; // USB device identifier
  ipAddress?: string; // For network displays
  port?: number; // For network displays (typically 9100)
}
