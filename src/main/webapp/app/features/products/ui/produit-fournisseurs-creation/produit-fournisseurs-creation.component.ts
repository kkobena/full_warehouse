import { Component, Input, inject, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { KeyFilterModule } from 'primeng/keyfilter';
import { TagModule } from 'primeng/tag';
import { IFournisseur } from 'app/shared/model/fournisseur.model';

@Component({
  selector: 'app-produit-fournisseurs-creation',
  templateUrl: './produit-fournisseurs-creation.component.html',
  styleUrl: './produit-fournisseurs-creation.component.scss',
  imports: [CommonModule, ReactiveFormsModule, ButtonModule, InputTextModule, SelectModule, KeyFilterModule, TagModule],
})
export class ProduitFournisseursCreationComponent {
  readonly principalLibelle = input.required<string>();
  readonly principalCip = input.required<string>();
  readonly principalPrixAchat = input<number | null>(null);
  readonly principalPrixUni = input<number | null>(null);
  readonly fournisseurs = input.required<IFournisseur[]>();
  readonly principalFournisseurId = input<number | null>(null);

  @Input() formArray!: FormArray;

  private readonly fb = inject(FormBuilder);

  get rows(): FormGroup[] {
    return this.formArray.controls as FormGroup[];
  }

  availableFournisseurs(rowIndex: number): IFournisseur[] {
    const excluded = new Set<number>();
    const principalId = this.principalFournisseurId();
    if (principalId != null) excluded.add(principalId);
    this.rows.forEach((row, i) => {
      if (i !== rowIndex) {
        const id = row.get('fournisseurId')?.value;
        if (id != null) excluded.add(id);
      }
    });
    return this.fournisseurs().filter(f => !excluded.has(f.id!));
  }

  addRow(): void {
    this.formArray.push(
      this.fb.group({
        fournisseurId: [null, Validators.required],
        codeCip: [null, [Validators.required, Validators.minLength(6), Validators.maxLength(8)]],
        prixAchat: [null, [Validators.required, Validators.min(1)]],
        prixUni: [null, [Validators.required, Validators.min(1)]],
        qteColis: [1, Validators.min(1)],
        qteMinimaleCommande: [0, Validators.min(0)],
      })
    );
  }

  removeRow(index: number): void {
    this.formArray.removeAt(index);
  }

  isPrixValid(group: FormGroup): boolean {
    const achat = Number(group.get('prixAchat')?.value ?? 0);
    const vente = Number(group.get('prixUni')?.value ?? 0);
    return !achat || !vente || achat < vente;
  }

  formatPrix(val: number | null | undefined): string {
    if (val == null) return '—';
    return val.toLocaleString('fr-FR') + ' FCFA';
  }
}
