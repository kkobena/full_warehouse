import {Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Button} from 'primeng/button';
import {TableModule} from 'primeng/table';
import {TagModule} from 'primeng/tag';
import {Toast} from 'primeng/toast';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {CommandeId} from '../../../../../../shared/model/abstract-commande.model';
import {ISubstitutionProposee} from '../../../../../../shared/model/pharmaml.model';
import {PharmamlApiService} from '../../../../data-access/pharmaml-api.service';
import {Tooltip} from "primeng/tooltip";
import {NotificationService} from "../../../../../../shared/services/notification.service";
import {ErrorService} from "../../../../../../shared/error.service";

@Component({
  selector: 'app-substitution-pharmaml',
  imports: [CommonModule, Button, TableModule, TagModule, Toast, Tooltip],
  templateUrl: './substitution-pharmaml.component.html',
  styleUrls: ['./substitution-pharmaml.scss'],
})
export class SubstitutionPharmamlComponent implements OnInit {
  commandeId!: CommandeId;

  private readonly api = inject(PharmamlApiService);
  private readonly notificationService = inject(NotificationService);
  readonly activeModal = inject(NgbActiveModal);
  private readonly errorService = inject(ErrorService);
  readonly substitutions = signal<ISubstitutionProposee[]>([]);
  readonly loading = signal(false);
  readonly processing = signal<number | null>(null);

  ngOnInit(): void {
    this.load();
  }

  accepter(sub: ISubstitutionProposee): void {
    this.processing.set(sub.id);
    this.api.accepterSubstitution(sub.id).subscribe({
      next: () => {
        this.notificationService.success(`${sub.cipPropose} ajouté à la commande`, 'Substitution acceptée');
        this.processing.set(null);
        this.load();
      },
      error: (err) => {
        this.processing.set(null);
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur');
      },
    });
  }

  refuser(sub: ISubstitutionProposee): void {
    this.processing.set(sub.id);
    this.api.refuserSubstitution(sub.id).subscribe({
      next: () => {
        this.processing.set(null);
        this.load();
      },
      error: (err) => {

        this.processing.set(null);
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur');
      },
    });
  }

  dismiss(): void {
    this.activeModal.close();
  }

  private load(): void {
    this.loading.set(true);
    this.api.substitutions(this.commandeId.id, this.commandeId.orderDate).subscribe({
      next: res => {
        this.substitutions.set(res.body ?? []);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
