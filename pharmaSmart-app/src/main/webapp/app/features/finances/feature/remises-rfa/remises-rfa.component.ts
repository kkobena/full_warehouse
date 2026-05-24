import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';
import { TagModule } from 'primeng/tag';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { RemiseRfaApiService } from '../../data-access/services/remise-rfa-api.service';
import { IRemiseRfaFournisseur, IAvoirFournisseur } from '../../data-access/models';
import { formatCurrency } from 'app/shared/utils/format-utils';

@Component({
  selector: 'app-remises-rfa',
  imports: [CommonModule, ButtonModule, TableModule, ToolbarModule, TagModule, NgbNavModule],
  templateUrl: './remises-rfa.component.html',
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
