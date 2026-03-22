import { Component, computed, input } from '@angular/core';
import { TooltipModule } from 'primeng/tooltip';
import { ICommande } from '../../../../shared/model/commande.model';
import { OrderStatut } from '../../../../shared/model/enumerations/order-statut.model';

interface StatusStep {
  label: string;
  icon: string;
  tooltip: string;
}

const STEPS: StatusStep[] = [
  { label: 'Saisie', icon: 'pi pi-pencil', tooltip: 'Commande en cours de saisie' },
  { label: 'Envoyée', icon: 'pi pi-send', tooltip: 'Commande envoyée au fournisseur (PharmaML)' },
  { label: 'Réception', icon: 'pi pi-truck', tooltip: 'Bon de livraison reçu — saisie réception' },
  { label: 'Clôturée', icon: 'pi pi-check-circle', tooltip: 'Entrée en stock validée' },
];

@Component({
  selector: 'app-commande-status-bar',
  templateUrl: './commande-status-bar.component.html',
  styleUrls: ['./commande-status-bar.component.scss'],
  imports: [TooltipModule],
})
export class CommandeStatusBarComponent {
  commande = input<ICommande | null>(null);

  readonly steps = STEPS;

  readonly currentStep = computed<number>(() => {
    const c = this.commande();
    if (!c) return 0;
    if (c.orderStatus === OrderStatut.CLOSED) return 3;
    if (c.orderStatus === OrderStatut.RECEIVED) return 2;
    if (c.orderReference) return 1;
    return 0;
  });
}
