import { Injectable } from '@angular/core';
import { NgbDateParserFormatter, NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';

/**
 * Formateur de date ng-bootstrap au format français : jj/mm/aaaa
 * À fournir via : { provide: NgbDateParserFormatter, useClass: FrenchDateParserFormatter }
 */
@Injectable()
export class FrenchDateParserFormatter extends NgbDateParserFormatter {
  parse(value: string): NgbDateStruct | null {
    if (!value) return null;
    const trimmed = value.trim();

    // Saisie standard "jj/mm/aaaa" (ou déjà reformatée automatiquement).
    if (trimmed.includes('/')) {
      const parts = trimmed.split('/');
      if (parts.length !== 3) return null;
      return this.toValidStruct(parts[0], parts[1], parts[2]);
    }

    // Saisie rapide "jjmmaaaa" (8 chiffres, sans séparateur) — reformatée en jj/mm/aaaa
    // dès que la date est valide (cf. pharma-date-picker.component.ts).
    if (/^\d{8}$/.test(trimmed)) {
      return this.toValidStruct(trimmed.slice(0, 2), trimmed.slice(2, 4), trimmed.slice(4, 8));
    }

    return null;
  }

  format(date: NgbDateStruct | null): string {
    return date
      ? `${String(date.day).padStart(2, '0')}/${String(date.month).padStart(2, '0')}/${date.year}`
      : '';
  }

  /** Valide jour/mois/année (rejette par ex. le 30/02) avant de construire le `NgbDateStruct`. */
  private toValidStruct(dayStr: string, monthStr: string, yearStr: string): NgbDateStruct | null {
    const day = parseInt(dayStr, 10);
    const month = parseInt(monthStr, 10);
    const year = parseInt(yearStr, 10);
    if (!day || !month || !year) return null;
    if (month < 1 || month > 12) return null;
    if (year < 1000 || year > 9999) return null;
    const daysInMonth = new Date(year, month, 0).getDate();
    if (day < 1 || day > daysInMonth) return null;
    return { day, month, year };
  }
}

