import {Component, computed, inject, input, OnDestroy, OnInit, signal, ChangeDetectionStrategy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Button} from 'primeng/button';
import {SplitButton} from 'primeng/splitbutton';
import {TagModule} from 'primeng/tag';
import {TableModule} from 'primeng/table';
import {TooltipModule} from 'primeng/tooltip';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {interval, Subscription, switchMap, takeWhile, tap} from 'rxjs';
import {MenuItem} from 'primeng/api';
import {
  IInfoProduit,
  IPharmamlCommandeResponse,
  IPharmaMlEnvoi,
  ISubstitutionProposee,
  PharmaMlStatut
} from '../../../../../shared/model/pharmaml.model';
import {CommandeId} from '../../../../../shared/model/abstract-commande.model';
import {OrderStatut} from '../../../../../shared/model/enumerations/order-statut.model';
import {AnnulationPharmamlComponent} from '../ui/annulation/annulation-pharmaml.component';
import {EnvoiPharmamlComponent} from '../ui/envoi/envoi-pharmaml.component';
import {ReponsePharmamlComponent} from '../ui/reponse/reponse-pharmaml.component';
import {RetourPharmamlComponent} from '../ui/retour/retour-pharmaml.component';
import {SubstitutionPharmamlComponent} from '../ui/substitution/substitution-pharmaml.component';
import {PharmamlApiService} from '../../../data-access/pharmaml-api.service';
import {NotificationService} from '../../../../../shared/services/notification.service';
import {ErrorService} from '../../../../../shared/error.service';
import {DispoComparaisonComponent} from '../ui/dispo-comparaison/dispo-comparaison.component';

const POLL_INTERVAL_MS = 5000;
const TERMINAL_STATUTS: PharmaMlStatut[] = ['SUBMITTED', 'PARTIAL', 'REJECTED', 'ERROR'];

/**
 * Widget PharmaML intégrable dans la fiche commande.
 * Expose les actions: Envoyer, Voir réponse, Historique.
 * Auto-poll le statut du dernier envoi si PENDING.
 */
@Component({
  selector: 'app-pharmaml-home',
  imports: [CommonModule, Button, SplitButton, TagModule, TableModule, TooltipModule],
  templateUrl: './pharmaml-home.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./pharmaml-home.scss'],
})
export class PharmamlHomeComponent implements OnInit, OnDestroy {
  commandeId = input.required<CommandeId>();
  commandeRef = input.required<string>();
  orderStatus = input<OrderStatut | undefined>(undefined);

  readonly isSubmited = input(false);
  readonly lastResult = signal<IPharmamlCommandeResponse | null>(null);
  readonly historique = signal<IPharmaMlEnvoi[]>([]);
  readonly showHistorique = signal(false);
  readonly loadingHistorique = signal(false);
  readonly polling = signal(false);
  readonly substitutionsEnAttente = signal<ISubstitutionProposee[]>([]);
  readonly disponibilites = signal<IInfoProduit[]>([]);
  readonly showDisponibilite = signal(false);
  readonly loadingDisponibilite = signal(false);
  private readonly modal = inject(NgbModal);
  private readonly notificationService = inject(NotificationService);
  private readonly api = inject(PharmamlApiService);
  private readonly errorService = inject(ErrorService);
  private pollSub: Subscription | null = null;

  ngOnInit(): void {
    this.loadHistorique();
    this.loadSubstitutions();
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  openEnvoi(): void {
    const ref = this.modal.open(EnvoiPharmamlComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true
    });
    const instance = ref.componentInstance as EnvoiPharmamlComponent;
    instance.commandeId = this.commandeId();

    ref.result.then(
      (result: IPharmamlCommandeResponse) => {
        this.lastResult.set(result);
        if (result.reliquatCommandeId != null) {
          this.notificationService.info(`Une commande reliquat a été créée automatiquement (réf. interne #${result.reliquatCommandeId})`, 'Reliquat créé');

        } else {
          result.success ? this.notificationService.success(`${result.successCount} / ${result.totalProduit} produits acceptés`, 'Envoi réussi') : this.notificationService.warning(`${result.successCount} / ${result.totalProduit} produits acceptés`, 'Envoi partiel');
        }
        this.loadHistorique();
      },
      () => {
      },
    );
  }

  openSubstitutions(): void {
    const ref = this.modal.open(SubstitutionPharmamlComponent, {
      size: 'xl',
      backdrop: 'static',
      centered: true
    });
    const instance = ref.componentInstance as SubstitutionPharmamlComponent;
    instance.commandeId = this.commandeId();
    ref.result.then(() => this.loadSubstitutions(), () => this.loadSubstitutions());
  }

  openReponse(): void {
    const ref = this.modal.open(ReponsePharmamlComponent, {
      size: 'xl',
      backdrop: 'static',
      centered: true
    });
    const instance = ref.componentInstance as ReponsePharmamlComponent;
    instance.commandeRef = this.commandeRef();
    instance.orderId = this.commandeId().id.toString();
    ref.result.then(
      () => {
      },
      () => {
      },
    );
  }

  toggleHistorique(): void {
    this.showHistorique.update(v => !v);
  }

  openAnnulation(): void {
    const ref = this.modal.open(AnnulationPharmamlComponent, {
      size: 'md',
      backdrop: 'static',
      centered: true
    });
    const instance = ref.componentInstance as AnnulationPharmamlComponent;
    instance.commandeId = this.commandeId();
    ref.result.then(
      (result: string) => {
        if (result === 'annulee') {
          this.notificationService.success(`Démande d'annulation transmise au grossiste`, 'Commande annulée');
          this.loadHistorique();
        }
      },
      () => {
      },
    );
  }

  openRetour(): void {
    const ref = this.modal.open(RetourPharmamlComponent, {
      size: 'xl',
      backdrop: 'static',
      centered: true
    });
    const instance = ref.componentInstance as RetourPharmamlComponent;
    instance.commandeId = this.commandeId();
    instance.commandeRef = this.commandeRef();
    ref.result.then(
      (result: string) => {
        if (result === 'retourne') {
          this.notificationService.success(`Démande transmise au grossiste`, 'Retour envoyé');
        }
      },
      () => {
      },
    );
  }


  ouvrirComparaison(): void {
    const ref = this.modal.open(DispoComparaisonComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true,
      scrollable: true
    });
    const instance = ref.componentInstance as DispoComparaisonComponent;
    instance.commandeId = this.commandeId();
    instance.header = `Comparaison disponibilité multi-grossistes — commande ${this.commandeRef()}`;
  }

  verifierDisponibilite(): void {
    if (this.loadingDisponibilite()) {
      return;
    }
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

  libelleStatut(statut: PharmaMlStatut): string {
    switch (statut) {
      case 'SUBMITTED':
        return 'Soumise';
      case 'PARTIAL':
        return 'Reception partielle';
      case 'REJECTED':
        return 'Rejetée';
      case 'ERROR':
        return 'En erreur';
      case 'PENDING':
        return 'En attente';
      default:
        return '';

    }
  }

  private readonly utilsMenuItems = (): MenuItem[] => [
    {separator: true},
    {
      label: 'Vérifier disponibilité',
      icon: 'pi pi-search',
      command: () => this.verifierDisponibilite()
    },
    {
      label: 'Comparer multi-grossistes',
      icon: 'pi pi-chart-bar',
      command: () => this.ouvrirComparaison()
    },
    {label: 'Historique des envois', icon: 'pi pi-history', command: () => this.toggleHistorique()},
  ];

  /** Actions du SplitButton en mode REQUESTED */
  readonly actionsRequested = computed<MenuItem[]>(() => [
    {label: 'Envoyer via PharmaML', icon: 'pi pi-send', command: () => this.openEnvoi()},
    {label: 'Voir réponse', icon: 'pi pi-file', command: () => this.openReponse()},
    ...this.utilsMenuItems(),
  ]);
  /** Actions du SplitButton en mode RECEIVED */
  readonly actionsReceived = computed<MenuItem[]>(() => [
    {label: 'Voir réponse', icon: 'pi pi-file', command: () => this.openReponse()},
    ...this.utilsMenuItems(),
  ]);

  private loadSubstitutions(): void {
    const id = this.commandeId();
    this.api.substitutions(id.id, id.orderDate).subscribe({
      next: res => this.substitutionsEnAttente.set(res.body ?? []),
      error: () => {
      },
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
