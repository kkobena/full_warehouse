import {Component, inject, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {Router} from '@angular/router';
import {ActivatedRoute} from '@angular/router';
import {ICommande} from 'app/shared/model/commande.model';
import {OrderStatut} from '../../../../shared/model/enumerations/order-statut.model';
import {CommandCommonService} from '../../../../entities/commande/command-common.service';
import {CommandeRequestedComponent} from '../commande-requested/commande-requested.component';
import {CommandeReceivedComponent} from '../commande-received/commande-received.component';

/**
 * Composant conteneur pour la gestion d'une commande.
 * Responsabilités : charger la commande depuis la route, rediriger si CLOSED,
 * puis déléguer à CommandeRequestedComponent (REQUESTED) ou CommandeReceivedComponent (RECEIVED).
 *
 */
@Component({
  selector: 'app-commande-detail',
  templateUrl: './commande-detail.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommandeRequestedComponent, CommandeReceivedComponent],
})
export class CommandeDetailComponent implements OnInit {
  protected commande: ICommande | null = null;
  protected readonly RECEIVED = OrderStatut.RECEIVED;

  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly commandCommonService = inject(CommandCommonService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({commande}) => {
      if (commande?.id) {
        this.commande = commande;
      } else if (this.commandCommonService.currentCommand()?.id) {
        this.commande = this.commandCommonService.currentCommand();
      }
      // commande peut être null pour une nouvelle saisie (route 'new') — c'est valide

      if (this.commande?.orderStatus === OrderStatut.CLOSED) {
        this.router.navigate(['/commande']);
      }
    });
  }

  protected onCommandeChange(commande: ICommande | null): void {
    this.commande = commande;
  }
}
