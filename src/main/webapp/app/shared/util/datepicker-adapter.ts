/**
 * Angular bootstrap Date adapter
 */
import { inject, Injectable } from '@angular/core';
import { NgbDateAdapter, NgbDateParserFormatter, NgbDatepickerI18n, NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import moment, { Moment } from 'moment';

@Injectable()
export class NgbDateMomentAdapter extends NgbDateAdapter<Moment> {
  fromModel(date: Moment): NgbDateStruct {
    if (date && moment.isMoment(date) && date.isValid()) {
      return { year: date.year(), month: date.month() + 1, day: date.date() };
    }
    // ! can be removed after https://github.com/ng-bootstrap/ng-bootstrap/issues/1544 is resolved
    return null!;
  }

  toModel(date: NgbDateStruct): Moment {
    // ! after null can be removed after https://github.com/ng-bootstrap/ng-bootstrap/issues/1544 is resolved
    return date ? moment(date.year + '-' + date.month + '-' + date.day, 'YYYY-MM-DD') : null!;
  }
}

/**
 * This Service handles how the date is represented in scripts i.e. ngModel.
 */

@Injectable()
export class CustomAdapter extends NgbDateAdapter<string> {
  readonly DELIMITER = '-';

  fromModel(value: string | null): NgbDateStruct | null {
    if (value) {
      if (typeof value === 'string') {
        const date = value.split(this.DELIMITER);

        return {
          day: parseInt(date[0], 10),
          month: parseInt(date[1], 10),
          year: parseInt(date[2], 10),
        };
      }
      if (typeof value === 'object') {
        const date = value as NgbDateStruct;
        return {
          day: date.day,
          month: date.month,
          year: date.year,
        };
      }
    }
    return null;
  }

  toModel(date: NgbDateStruct | null): string | null {
    return date ? date.day + this.DELIMITER + date.month + this.DELIMITER + date.year : null;
  }
}

/**
 * This Service handles how the date is rendered and parsed from keyboard i.e. in the bound input field.
 */
@Injectable()
export class CustomDateParserFormatter extends NgbDateParserFormatter {
  readonly DELIMITER = '/';

  parse(value: string): NgbDateStruct | null {
    if (value) {
      const date = value.split(this.DELIMITER);
      return {
        day: parseInt(date[0], 10),
        month: parseInt(date[1], 10),
        year: parseInt(date[2], 10),
      };
    }
    return null;
  }

  format(date: NgbDateStruct | null): string {
    let d = null;
    if (date) {
      if (date.day < 10) {
        d = '0' + date.day;
      } else {
        d = date.day;
      }
      if (date.month < 10) {
        return d + this.DELIMITER + '0' + date.month + this.DELIMITER + date.year;
      } else {
        return d + this.DELIMITER + date.month + this.DELIMITER + date.year;
      }
    }
    return d;
  }
}

const I18N_VALUES: any = {
  fr: {
    weekdays: ['Lu', 'Ma', 'Me', 'Je', 'Ve', 'Sa', 'Di'],
    months: ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Aou', 'Sep', 'Oct', 'Nov', 'Déc'],
    weekLabel: 'sem',
  },
  // other languages you would support
};

// Define a service holding the language. You probably already have one if your app is i18ned. Or you could also
// use the Angular LOCALE_ID value
@Injectable()
export class I18n {
  language = 'fr';
}

// Define custom service providing the months and weekdays translations
@Injectable()
export class CustomDatepickerI18n extends NgbDatepickerI18n {
  private _i18n = inject(I18n);

  getWeekdayLabel(weekday: number): string {
    return I18N_VALUES[this._i18n.language].weekdays[weekday - 1];
  }

  getWeekLabel(): string {
    return I18N_VALUES[this._i18n.language].weekLabel;
  }

  getMonthShortName(month: number): string {
    return I18N_VALUES[this._i18n.language].months[month - 1];
  }

  getMonthFullName(month: number): string {
    return this.getMonthShortName(month);
  }

  getDayAriaLabel(date: NgbDateStruct): string {
    return `${date.day}-${date.month}-${date.year}`;
  }
}
