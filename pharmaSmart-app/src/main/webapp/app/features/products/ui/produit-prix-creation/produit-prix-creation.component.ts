import { Component, Input, inject, input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { InputNumberModule } from 'primeng/inputnumber';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { ITiersPayant } from 'app/shared/model/tierspayant.model';

const PRICE_TYPES = [
  { code: 'REFERENCE', libelle: 'Prix de référence assurance' },
  { code: 'POURCENTAGE', libelle: "Pourcentage appliqué par l'assureur" },
  { code: 'MIXED_REFERENCE_POURCENTAGE', libelle: 'Pourcentage appliqué au prix de référence' },
] as const;

@Component({
  selector: 'app-produit-prix-creation',
  templateUrl: './produit-prix-creation.component.html',
  styleUrl: './produit-prix-creation.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, ReactiveFormsModule, ButtonModule, SelectModule, InputNumberModule, ToggleSwitchModule],
})
export class ProduitPrixCreationComponent {
  readonly tiersPayants = input.required<ITiersPayant[]>();

  @Input() formArray!: FormArray;

  protected readonly priceTypes = PRICE_TYPES;

  private readonly fb = inject(FormBuilder);

  get rows(): FormGroup[] {
    return this.formArray.controls as FormGroup[];
  }

  addRow(): void {
    const group = this.fb.group({
      tiersPayantId: [null, Validators.required],
      type: ['REFERENCE', Validators.required],
      price: [null, Validators.required],
      rate: [null as number | null],
      enabled: [true],
    });
    group.get('type')!.valueChanges.subscribe(val => this.updateValidators(group, val));
    this.formArray.push(group);
  }

  removeRow(index: number): void {
    this.formArray.removeAt(index);
  }

  showPrice(group: FormGroup): boolean {
    return group.get('type')?.value !== 'POURCENTAGE';
  }

  showRate(group: FormGroup): boolean {
    const t = group.get('type')?.value;
    return t === 'POURCENTAGE' || t === 'MIXED_REFERENCE_POURCENTAGE';
  }

  private updateValidators(group: FormGroup, type: string | null): void {
    const price = group.get('price')!;
    const rate = group.get('rate')!;
    price.clearValidators();
    rate.clearValidators();
    if (type !== 'POURCENTAGE') {
      price.setValidators(Validators.required);
    }
    if (type === 'POURCENTAGE' || type === 'MIXED_REFERENCE_POURCENTAGE') {
      rate.setValidators([Validators.required, Validators.min(0), Validators.max(100)]);
    }
    price.updateValueAndValidity({ emitEvent: false });
    rate.updateValueAndValidity({ emitEvent: false });
  }
}
