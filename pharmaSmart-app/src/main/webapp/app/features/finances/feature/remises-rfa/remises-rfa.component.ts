import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { RemiseRfaApiService } from '../../data-access/services/remise-rfa-api.service';
import { IRemiseRfaFournisseur, IAvoirFournisseur } from '../../data-access/models';
import { formatCurrency } from 'app/shared/utils/format-utils';
import { BadgeComponent, ButtonComponent, DataTableComponent, ToolbarComponent } from '../../../../shared/ui';

@Component({
  selector: 'app-remises-rfa',
  imports: [CommonModule, ButtonComponent, DataTableComponent, ToolbarComponent, BadgeComponent, NgbNavModule],
  templateUrl: './remises-rfa.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './remises-rfa.component.scss',
})
export class RemisesRfaComponent implements OnInit {
  rfas = signal<IRemiseRfaFournisseur[]>([]);
  avoirs = signal<IAvoirFournisseur[]>([]);
  isLoading = signal(false);
  activeTab = signal<'rfa' | 'avoirs'>('rfa');

  formatCurrency = formatCurrency;
  Math = Math;

  private readonly api = inject(RemiseRfaApiService);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.api.getRfaFournisseurs().subscribe({
      next: res => {
        this.rfas.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
    this.api.getAvoirsFournisseurs().subscribe({
      next: res => this.avoirs.set(res.body ?? []),
    });
  }
}
