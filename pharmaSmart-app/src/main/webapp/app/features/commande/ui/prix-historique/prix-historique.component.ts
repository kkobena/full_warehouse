import {signal, Component, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { DecimalPipe, DatePipe, NgTemplateOutlet } from '@angular/common';
import { NgbActiveModal, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { BadgeComponent } from 'app/shared/ui';
import { CommandeService, IPriceHistory } from '../../../../entities/commande/commande.service';

@Component({
  selector: 'app-prix-historique',
  templateUrl: './prix-historique.component.html',
  styleUrls: ['./prix-historique.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, DatePipe, NgTemplateOutlet, BadgeComponent, NgbTooltip],
})
export class PrixHistoriqueComponent implements OnInit {
  fournisseurProduitId!: number;
  produitLibelle!: string;
  header!: string;

  protected readonly historique = signal([]);
  protected readonly loading = signal(true);

  private readonly activeModal = inject(NgbActiveModal);
  private readonly commandeService = inject(CommandeService);

  ngOnInit(): void {
    this.commandeService.getPriceHistory(this.fournisseurProduitId).subscribe({
      next: data => {
        this.historique.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  protected variationPct(oldVal: number, newVal: number): number {
    if (!oldVal) return 0;
    return Math.round(((newVal - oldVal) / oldVal) * 100);
  }

  protected close(): void {
    this.activeModal.dismiss();
  }
}
