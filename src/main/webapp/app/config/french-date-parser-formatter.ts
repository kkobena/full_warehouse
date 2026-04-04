import { Injectable } from '@angular/core';
import { NgbDateParserFormatter, NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';

/**
 * Formateur de date ng-bootstrap au format français : jj/mm/aaaa
 * À fournir via : { provide: NgbDateParserFormatter, useClass: FrenchDateParserFormatter }
 */
@Injectable()
export class FrenchDateParserFormatter extends NgbDateParserFormatter {
  parse(value: string): NgbDateStruct | null {
    if (value) {
      const parts = value.trim().split('/');
      if (parts.length === 3) {
        return {
          day: parseInt(parts[0], 10),
          month: parseInt(parts[1], 10),
          year: parseInt(parts[2], 10),
        };
      }
    }
    return null;
  }

  format(date: NgbDateStruct | null): string {
    return date
      ? `${String(date.day).padStart(2, '0')}/${String(date.month).padStart(2, '0')}/${date.year}`
      : '';
  }
}

