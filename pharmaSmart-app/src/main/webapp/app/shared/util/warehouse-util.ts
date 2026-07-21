import dayjs from 'dayjs';
import { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { DD_MM_YYYY, DD_MM_YYYY_HH_MM_SS } from '../constants/input.constants';
import { IDeliveryItem } from '../model/delivery-item';
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const formatNumberToString = (cellValue: any): string | null => {
  if (cellValue?.value !== null && cellValue?.value !== undefined) {
    return Math.floor(cellValue.value)
      .toString()
      .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1 ');
  }
  return null;
};

export const formatNumber = (numberValue: number): string | null => {
  if (numberValue !== null && numberValue !== undefined) {
    return Math.floor(numberValue)
      .toString()
      .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1 ');
  }
  return null;
};
export const DATE_FORMAT_YYYY_MM_DD = (date: Date): string | null => (date ? date.toLocaleDateString('fr-CA') : null);
export const DATE_FORMAT_ISO_DATE = (date: Date): string | null => (date ? dayjs(date).format('YYYY-MM-DD') : null);

/**
 * Date du jour au format attendu par `pharma-date-picker` (ng-bootstrap).
 *
 * `NgbDateStruct` compte les mois à partir de 1, `Date` à partir de 0 — d'où le `+ 1`,
 * source d'erreur classique lors du passage de `p-datepicker` à `pharma-date-picker`.
 */
export const TODAY_NGB_DATE = (): NgbDateStruct => {
  const now = new Date();
  return { year: now.getFullYear(), month: now.getMonth() + 1, day: now.getDate() };
};

/**
 * Convertit un `NgbDateStruct` en `YYYY-MM-DD`.
 *
 * Volontairement sans passage par `Date` : construire un `Date` puis le formater
 * réintroduirait le fuseau horaire, et une date saisie en fin de journée pouvait
 * basculer au jour précédent une fois sérialisée en UTC.
 */
export const NGB_DATE_TO_ISO = (date: NgbDateStruct | null): string | null =>
  date ? `${date.year}-${String(date.month).padStart(2, '0')}-${String(date.day).padStart(2, '0')}` : null;
export const DATE_FORMAT_FROM_STRING_FR = (date: string): string | null => {
  if (date) {
    const dateArray = date.split('/');
    return `${dateArray[2]}-${dateArray[1]}-${dateArray[0]}`;
  }
  return null;
};

export const BLOCK_SPACE = /[^\s]/;
export const DATE_FORMAT_DD_MM_YYYY_HH_MM_SS = (): string => dayjs().format(DD_MM_YYYY_HH_MM_SS);

export const DATE_FORMAT_DD_MM_YYYY = (): string => dayjs().format(DD_MM_YYYY);
export const DATE_FROM_STRING_FR = (date: string): string | null => {
  if (date) {
    const dateArray = date.split('-');
    let day = dateArray[0];
    if (day.length === 1) {
      day = `0${day}`;
    }
    let month = dateArray[1];
    if (month.length === 1) {
      month = `0${month}`;
    }
    return `${dateArray[2]}-${month}-${day}`;
  }
  return null;
};

export const priceRangeValidator =
  (min: number, max: number): ValidatorFn =>
  (control: AbstractControl<number>): ValidationErrors | null => {
    const inRange = control.value > min && control.value < max;
    return inRange ? null : { outOfRange: true };
  };
export const FORMAT_ISO_DATE_TO_STRING_FR = (date: string): string | null => {
  if (date) {
    const dateArray = date.split('-');
    return `${dateArray[2]}/${dateArray[1]}/${dateArray[0]}`;
  }
  return null;
};
export const FORMAT_DD_MM_YYYY = (date: Date): string | null => {
  if (date) {
    return dayjs(date).format(DD_MM_YYYY);
  }
  return '';
};
export const TYPE_AFFICHAGE = [
  { icon: 'pi pi-align-justify', value: 'table' },
  { icon: 'pi pi-chart-bar', value: 'graphe' },
];
export const retriveMonthLabel = (date: Date): string | null => {
  if (date !== null && date !== undefined) {
    return date.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
  }
  return null;
};
