import { IOrderLine } from '../model/order-line.model';
import dayjs from 'dayjs';
import { DD_MM_YYYY_HH_MM_SS } from '../constants/input.constants';
import { IDeliveryItem } from '../model/delivery-item';
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const formatNumberToString = (number: any): string => {
  return Math.floor(number.value)
    .toString()
    .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1 ');
};
export const checkIfAlineToBeUpdated = (orderLine: IOrderLine): boolean => {
  return (
    //  orderLine.provisionalCode ||
    !(orderLine.regularUnitPrice === orderLine.orderUnitPrice) || !(orderLine.orderCostAmount === orderLine.costAmount)
  );
};
export const DATE_FORMAT_YYYY_MM_DD = (date: Date): string => {
  return date ? date.toLocaleDateString('fr-CA') : null;
};
export const BLOCK_SPACE: RegExp = /[^\s]/;
export const DATE_FORMAT_DD_MM_YYYY_HH_MM_SS = (): string => {
  return dayjs().format(DD_MM_YYYY_HH_MM_SS);
};

export const checkIfRomToBeUpdated = (deliveryItem: IDeliveryItem): boolean => {
  return deliveryItem.regularUnitPrice !== deliveryItem.orderUnitPrice || deliveryItem.orderCostAmount !== deliveryItem.costAmount;
};
export const priceRangeValidator = (min: number, max: number): ValidatorFn => {
  return (control: AbstractControl<number>): ValidationErrors | null => {
    const inRange = control.value > min && control.value < max;
    return inRange ? null : { outOfRange: true };
  };
};
//validators: [Validators.required, priceRangeValidator(1,1000)]
/*
this.price.valueChanges.subscribe(price => {
if (price) {
this.showPriceRangeHint = price > 1 && price < 10000;
}
});
this.cartForm.controls.products.push(
new FormControl(1, { nonNullable: true })
);
cartForm = new FormGroup({
products: new FormArray<FormControl<number>>([])
});
<div [formGroup]="cartForm">
<div
formArrayName="products"
*ngFor="let product of cartForm.controls.products.controls; let
i=index">
<label>{{cart[i].name}}</label>
<input type="number" [formControlName]="i" />
</div>
</div>
 */
