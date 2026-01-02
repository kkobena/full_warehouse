/**
 * Utility functions for formatting numbers, currency, percentages, etc.
 * Used across dashboard and report components
 */

/**
 * Format a number as currency in FCFA (French format)
 * @param value - The numeric value to format
 * @returns Formatted currency string without the currency symbol
 * @example formatCurrency(1234567.89) // "1 234 568"
 */
export function formatCurrency(value: number | undefined | null): string {
  if (value === undefined || value === null || isNaN(value)) return '0';
  return new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(value);
}

/**
 * Format a number with French locale formatting
 * @param value - The numeric value to format
 * @returns Formatted number string
 * @example formatNumber(1234567) // "1 234 567"
 */
export function formatNumber(value: number | undefined | null): string {
  if (value === undefined || value === null || isNaN(value)) return '0';
  return new Intl.NumberFormat('fr-FR').format(value);
}

/**
 * Format a number as a percentage with 2 decimal places
 * @param value - The numeric value to format
 * @returns Formatted percentage string (without % symbol)
 * @example formatPercent(12.3456) // "12.35"
 */
export function formatPercent(value: number | undefined | null): string {
  if (value === undefined || value === null || isNaN(value)) return '0.00';
  return new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

/**
 * Format a number as a decimal with specified decimal places
 * @param value - The numeric value to format
 * @param decimals - Number of decimal places (default: 2)
 * @returns Formatted decimal string
 * @example formatDecimal(12.3456, 2) // "12.35"
 */
export function formatDecimal(value: number | undefined | null, decimals = 2): string {
  if (value === undefined || value === null || isNaN(value)) {
    return '0.' + '0'.repeat(decimals);
  }
  return new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value);
}

/**
 * Format a date to ISO date string (YYYY-MM-DD)
 * @param date - The date to format
 * @returns ISO date string
 * @example formatDate(new Date('2024-01-15')) // "2024-01-15"
 */
export function formatDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * Format a date to French locale string (DD/MM/YYYY)
 * @param date - The date to format
 * @returns Formatted date string
 * @example formatDateFR(new Date('2024-01-15')) // "15/01/2024"
 */
export function formatDateFR(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date;
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = d.getFullYear();
  return `${day}/${month}/${year}`;
}

/**
 * Format a month from YYYY-MM format to French month name and year
 * @param monthStr - Month string in YYYY-MM format
 * @returns Formatted month string
 * @example formatMonth("2024-01") // "Janvier 2024"
 */
export function formatMonth(monthStr: string): string {
  if (!monthStr) return '';
  const [year, month] = monthStr.split('-');
  const monthNames = [
    'Janvier',
    'Février',
    'Mars',
    'Avril',
    'Mai',
    'Juin',
    'Juillet',
    'Août',
    'Septembre',
    'Octobre',
    'Novembre',
    'Décembre',
  ];
  const monthIndex = parseInt(month, 10) - 1;
  return `${monthNames[monthIndex]} ${year}`;
}

/**
 * Truncate a string to a maximum length and add ellipsis
 * @param str - The string to truncate
 * @param maxLength - Maximum length before truncation
 * @returns Truncated string
 * @example truncate("Long text here", 10) // "Long text..."
 */
export function truncate(str: string | undefined | null, maxLength: number): string {
  if (!str) return '';
  if (str.length <= maxLength) return str;
  return str.substring(0, maxLength - 3) + '...';
}
