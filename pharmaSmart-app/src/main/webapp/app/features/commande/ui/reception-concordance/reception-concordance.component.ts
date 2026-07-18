import { Component, computed, input, ChangeDetectionStrategy } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { IOrderLine } from '../../../../shared/model/order-line.model';

export interface ConcordanceStats {
  total: number;
  ecartQuantite: number;
  ecartPrix: number;
  ecartColisage: number;
  nonValidees: number;
  lotsManquants: number;
  montantCommande: number;
  montantRecu: number;
}

@Component({
  selector: 'app-reception-concordance',
  templateUrl: './reception-concordance.component.html',
  styleUrls: ['./reception-concordance.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [DecimalPipe, TagModule, TooltipModule],
})
export class ReceptionConcordanceComponent {
  /** Toutes les lignes de la commande (pas la page courante du tableau). */
  orderLines = input<IOrderLine[]>([]);
  /** Activer le compteur de lots manquants (APP_GESTION_LOT). */
  showLotInfo = input<boolean>(false);

  readonly stats = computed<ConcordanceStats>(() => {
    const lines = this.orderLines();
    let ecartQuantite = 0;
    let ecartPrix = 0;
    let ecartColisage = 0;
    let nonValidees = 0;
    let lotsManquants = 0;
    let montantCommande = 0;
    let montantRecu = 0;

    for (const l of lines) {
      const qteCmd = l.quantityRequested ?? 0;
      const qteRec = l.quantityReceivedTmp ?? l.quantityReceived ?? 0;
      const paCmd = l.orderCostAmount ?? 0;
      const paRec = l.costAmount ?? paCmd;

      if (qteRec !== qteCmd) ecartQuantite++;
      if (paRec !== paCmd && paCmd > 0) ecartPrix++;
      if (l.updated === false) nonValidees++;
      if (this.showLotInfo() && (l.lots?.length ?? 0) === 0) lotsManquants++;
      const pcb = l.qteColis;
      if (pcb && pcb > 1 && qteRec > 0 && qteRec % pcb !== 0) ecartColisage++;

      montantCommande += paCmd * qteCmd;
      montantRecu += paRec * qteRec;
    }

    return {
      total: lines.length,
      ecartQuantite,
      ecartPrix,
      ecartColisage,
      nonValidees,
      lotsManquants,
      montantCommande,
      montantRecu,
    };
  });

  readonly ecartMontant = computed(() => this.stats().montantRecu - this.stats().montantCommande);
  readonly hasAnomalies = computed(() => {
    const s = this.stats();
    return s.ecartQuantite > 0 || s.ecartPrix > 0 || s.ecartColisage > 0 || s.nonValidees > 0 || s.lotsManquants > 0;
  });
}
