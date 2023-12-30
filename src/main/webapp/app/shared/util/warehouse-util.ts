import dayjs from 'dayjs';
import { DD_MM_YYYY_HH_MM_SS } from '../constants/input.constants';
import { IDeliveryItem } from '../model/delivery-item';
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const formatNumberToString = (cellValue: any): string | null => {
  if (cellValue?.value !== null && cellValue?.value !== undefined)
    {return Math.floor(cellValue.value)
      .toString()
      .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1 ');}
  return null;
};

export const DATE_FORMAT_YYYY_MM_DD = (date: Date): string | null => date ? date.toLocaleDateString('fr-CA') : null;
export const BLOCK_SPACE: RegExp = /[^\s]/;
export const DATE_FORMAT_DD_MM_YYYY_HH_MM_SS = (): string => dayjs().format(DD_MM_YYYY_HH_MM_SS);

export const checkIfRomToBeUpdated = (deliveryItem: IDeliveryItem): boolean => deliveryItem.regularUnitPrice !== deliveryItem.orderUnitPrice || deliveryItem.orderCostAmount !== deliveryItem.costAmount;
export const priceRangeValidator = (min: number, max: number): ValidatorFn => (control: AbstractControl<number>): ValidationErrors | null => {
    const inRange = control.value > min && control.value < max;
    return inRange ? null : { outOfRange: true };
  };

// validators: [Validators.required, priceRangeValidator(1,1000)]
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
