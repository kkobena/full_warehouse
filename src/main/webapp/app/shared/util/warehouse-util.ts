import dayjs from 'dayjs';
import { DD_MM_YYYY_HH_MM_SS } from '../constants/input.constants';
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

export const BLOCK_SPACE: RegExp = /[^\s]/;
export const DATE_FORMAT_DD_MM_YYYY_HH_MM_SS = (): string => dayjs().format(DD_MM_YYYY_HH_MM_SS);

export const checkIfRomToBeUpdated = (deliveryItem: IDeliveryItem): boolean =>
  deliveryItem.regularUnitPrice !== deliveryItem.orderUnitPrice || deliveryItem.orderCostAmount !== deliveryItem.costAmount;
export const priceRangeValidator =
  (min: number, max: number): ValidatorFn =>
  (control: AbstractControl<number>): ValidationErrors | null => {
    const inRange = control.value > min && control.value < max;
    return inRange ? null : { outOfRange: true };
  };
