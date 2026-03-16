import { Component, computed, inject, input, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Button } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { Toast } from 'primeng/toast';
import { TableModule } from 'primeng/table';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { interval, Subscription, switchMap, takeWhile, tap } from 'rxjs';
import { IInfoProduit, IPharmaMlEnvoi, IPharmamlCommandeResponse, ISubstitutionProposee, PharmaMlStatut } from '../../../../../shared/model/pharmaml.model';
import { CommandeId } from '../../../../../shared/model/abstract-commande.model';
import { OrderStatut } from '../../../../../shared/model/enumerations/order-statut.model';
import { AnnulationPharmamlComponent } from '../ui/annulation/annulation-pharmaml.component';
import { EnvoiPharmamlComponent } from '../ui/envoi/envoi-pharmaml.component';
import { ReponsePharmamlComponent } from '../ui/reponse/reponse-pharmaml.component';
import { RetourPharmamlComponent } from '../ui/retour/retour-pharmaml.component';
import { SubstitutionPharmamlComponent } from '../ui/substitution/substitution-pharmaml.component';
import { PharmamlApiService } from '../../../data-access/pharmaml-api.service';
import {NotificationService} from "../../../../../shared/services/notification.service";
import {ErrorService} from "../../../../../shared/error.service";

const POLL_INTERVAL_MS = 5000;
const TERMINAL_STATUTS: PharmaMlStatut[] = ['SUBMITTED', 'PARTIAL', 'REJECTED', 'ERROR'];

/**
 * Widget PharmaML intégrable dans la fiche commande.
 * Expose les actions: Envoyer, Voir réponse, Historique.
 * Auto-poll le statut du dernier envoi si PENDING.
 */
@Component({
  selector: 'app-pharmaml-home',
  imports: [CommonModule, Button, TagModule, Toast, TableModule],
  templateUrl: './pharmaml-home.component.html',
})
export class PharmamlHomeComponent implements OnInit, OnDestroy {
  commandeId = input.required<CommandeId>();
  commandeRef = input.required<string>();
  orderStatus = input<OrderStatut | undefined>(undefined);

  readonly sendingAck = signal(false);

  private readonly modal = inject(NgbModal);
  private readonly notificationService = inject(NotificationService);
  private readonly api = inject(PharmamlApiService);
  private readonly errorService = inject(ErrorService);
  readonly lastResult = signal<IPharmamlCommandeResponse | null>(null);
  readonly historique = signal<IPharmaMlEnvoi[]>([]);
  readonly showHistorique = signal(false);
  readonly loadingHistorique = signal(false);
  readonly polling = signal(false);
  readonly substitutionsEnAttente = signal<ISubstitutionProposee[]>([]);
  readonly disponibilites = signal<IInfoProduit[]>([]);
  readonly showDisponibilite = signal(false);
  readonly loadingDisponibilite = signal(false);

  readonly canAnnuler = computed(() =>
    this.orderStatus() !== 'RECEIVED' &&
    this.historique().some(e => e.statut === 'SUBMITTED' || e.statut === 'PARTIAL')
  );

  private pollSub: Subscription | null = null;

  ngOnInit(): void {
    this.loadHistorique();
    this.loadSubstitutions();
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  openEnvoi(): void {
    const ref = this.modal.open(EnvoiPharmamlComponent, { size: 'lg', centered: true });
    const instance = ref.componentInstance as EnvoiPharmamlComponent;
    instance.commandeId = this.commandeId();

    ref.result.then(
      (result: IPharmamlCommandeResponse) => {
        this.lastResult.set(result);
        if (result.reliquatCommandeId != null) {
          this.notificationService.info(`Une commande reliquat a été créée automatiquement (réf. interne #${result.reliquatCommandeId})`, 'Reliquat créé');

        }else{
          result.success?   this.notificationService.success( `${result.successCount} / ${result.totalProduit} produits acceptés`, 'Envoi réussi'): this.notificationService.warning( `${result.successCount} / ${result.totalProduit} produits acceptés`, 'Envoi partiel');
        }
        this.loadHistorique();
      },
      () => {},
    );
  }

  openSubstitutions(): void {
    const ref = this.modal.open(SubstitutionPharmamlComponent, { size: 'xl', centered: true });
    const instance = ref.componentInstance as SubstitutionPharmamlComponent;
    instance.commandeId = this.commandeId();
    ref.result.then(() => this.loadSubstitutions(), () => this.loadSubstitutions());
  }

  openReponse(): void {
    const ref = this.modal.open(ReponsePharmamlComponent, { size: 'xl', centered: true });
    const instance = ref.componentInstance as ReponsePharmamlComponent;
    instance.commandeRef = this.commandeRef();
    instance.orderId = this.commandeId().id.toString();
    ref.result.then(
      () => {},
      () => {},
    );
  }

  toggleHistorique(): void {
    this.showHistorique.update(v => !v);
  }

  openAnnulation(): void {
    const ref = this.modal.open(AnnulationPharmamlComponent, { size: 'md', centered: true });
    const instance = ref.componentInstance as AnnulationPharmamlComponent;
    instance.commandeId = this.commandeId();
    ref.result.then(
      (result: string) => {
        if (result === 'annulee') {
          this.notificationService.success( `Démande d'annulation transmise au grossiste`, 'Commande annulée');
          this.loadHistorique();
        }
      },
      () => {},
    );
  }

  openRetour(): void {
    const ref = this.modal.open(RetourPharmamlComponent, { size: 'xl', centered: true });
    const instance = ref.componentInstance as RetourPharmamlComponent;
    instance.commandeId = this.commandeId();
    instance.commandeRef = this.commandeRef();
    ref.result.then(
      (result: string) => {
        if (result === 'retourne') {
          this.notificationService.success( `Démande transmise au grossiste`, 'Retour envoyé');
        }
      },
      () => {},
    );
  }

  envoiAck(): void {
    if (this.sendingAck()) return;
    const id = this.commandeId();
    this.sendingAck.set(true);
    this.api.accuseReception(id.id, id.orderDate).subscribe({
      next: () => {
        this.sendingAck.set(false);
        this.notificationService.success( `Accusé de reception transmis au grossiste`, 'Accusé envoyé');
      },
      error: (err) => {
        this.sendingAck.set(false);
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur');
      },
    });
  }

  verifierDisponibilite(): void {
    if (this.loadingDisponibilite()) return;
    const id = this.commandeId();
    this.loadingDisponibilite.set(true);
    this.showDisponibilite.set(true);
    this.api.disponibilite(id.id, id.orderDate).subscribe({
      next: res => {
        this.disponibilites.set(res.body ?? []);
        this.loadingDisponibilite.set(false);
      },
      error: (err) => {
        this.loadingDisponibilite.set(false);
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur');
      },
    });
  }

  statutSeverity(statut: PharmaMlStatut): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    switch (statut) {
      case 'SUBMITTED':
        return 'success';
      case 'PARTIAL':
        return 'warn';
      case 'REJECTED':
        return 'danger';
      case 'ERROR':
        return 'danger';
      case 'PENDING':
        return 'info';
      default:
        return 'secondary';
    }
  }

  private loadSubstitutions(): void {
    const id = this.commandeId();
    this.api.substitutions(id.id, id.orderDate).subscribe({
      next: res => this.substitutionsEnAttente.set(res.body ?? []),
      error: () => {},
    });
  }

  private loadHistorique(): void {
    const id = this.commandeId();
    this.loadingHistorique.set(true);
    this.api.historique(id.id, id.orderDate).subscribe({
      next: res => {
        const envois = res.body ?? [];
        this.historique.set(envois);
        this.loadingHistorique.set(false);
        this.startPollingIfNeeded(envois);
      },
      error: () => this.loadingHistorique.set(false),
    });
  }

  private startPollingIfNeeded(envois: IPharmaMlEnvoi[]): void {
    const latest = envois[0];
    if (!latest || TERMINAL_STATUTS.includes(latest.statut)) {
      return;
    }

    this.pollSub?.unsubscribe();
    this.polling.set(true);

    this.pollSub = interval(POLL_INTERVAL_MS)
      .pipe(
        switchMap(() => this.api.statut(latest.id)),
        tap(res => {
          if (res.body) {
            this.historique.update(list => list.map(e => (e.id === res.body!.id ? res.body! : e)));
          }
        }),
        takeWhile(res => !TERMINAL_STATUTS.includes(res.body?.statut ?? 'PENDING')),
      )
      .subscribe({
        complete: () => {
          this.polling.set(false);
          this.loadHistorique();
        },
        error: () => this.polling.set(false),
      });
  }
}
