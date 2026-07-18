import { Component, computed, input, ChangeDetectionStrategy } from '@angular/core';
import { Tag } from 'primeng/tag';
import { IDelivery } from '../../../../shared/model/delevery.model';

type PrimeSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast';

@Component({
  selector: 'app-list-bons-statut',
  imports: [Tag],
  template: `<p-tag [value]="label()" [severity]="severity()" [icon]="icon()" [rounded]="true" />`,
  changeDetection: ChangeDetectionStrategy.Eager,
  styles: [`:host { display:flex; align-items:center; height:100% }`],
})
export class ListBonsStatutComponent {
  delivery = input<IDelivery | null>(null);

  readonly label = computed(() => {
    const status = this.delivery()?.orderStatus ?? (this.delivery() as any)?.statut;
    return status === 'RECEIVED' ? 'En attente de saisie' : 'Clôturé';
  });

  readonly severity = computed<PrimeSeverity>(() => {
    const status = this.delivery()?.orderStatus ?? (this.delivery() as any)?.statut;
    return status === 'RECEIVED' ? 'warn' : 'success';
  });

  readonly icon = computed(() => {
    const status = this.delivery()?.orderStatus ?? (this.delivery() as any)?.statut;
    return status === 'RECEIVED' ? 'pi pi-inbox' : 'pi pi-check-circle';
  });
}
