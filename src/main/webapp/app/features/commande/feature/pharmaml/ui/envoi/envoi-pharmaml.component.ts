import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Select } from 'primeng/select';
import { Textarea } from 'primeng/textarea';
import { Toast } from 'primeng/toast';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { PharmamlApiService } from '../../../../data-access/pharmaml-api.service';
import { IEnvoiPharmaParams, TypeCommande } from '../../../../../../shared/model/pharmaml.model';
import { CommandeId } from '../../../../../../shared/model/abstract-commande.model';
import {NotificationService} from "../../../../../../shared/services/notification.service";
import {ErrorService} from "../../../../../../shared/error.service";

@Component({
  selector: 'app-envoi-pharmaml',
  imports: [CommonModule, FormsModule, Button, Select, Textarea, Toast],
  templateUrl: './envoi-pharmaml.component.html',
  styleUrls: ['./envoi-pharmaml.scss'],
})
export class EnvoiPharmamlComponent implements OnInit {
  commandeId!: CommandeId;

  private readonly api = inject(PharmamlApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  readonly activeModal = inject(NgbActiveModal);

  readonly loading = signal(false);
  readonly typeCommande = signal<TypeCommande>('NORMALE');
  readonly commentaire = signal('');
  readonly dateLivraison = signal('');

  readonly typeCommandeOptions = [
    { label: 'Normale', value: 'NORMALE' as TypeCommande },
    { label: 'Exceptionnelle', value: 'EXCEPTIONNELLE' as TypeCommande },
  ];

  ngOnInit(): void {}

  envoi(): void {
    const params: IEnvoiPharmaParams = {
      commandeId: { id: this.commandeId.id, orderDate: this.commandeId.orderDate },
      typeCommande: this.typeCommande(),
      commentaire: this.commentaire() || undefined,
      dateLivraisonSouhaitee: this.dateLivraison() || undefined,
    };

    this.loading.set(true);
    this.api.envoi(params).subscribe({
      next: res => {
        this.loading.set(false);
        this.activeModal.close(res.body!);
      },
      error: (err) => {
        this.loading.set(false);
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur');
      },
    });
  }

  dismiss(): void {
    this.activeModal.dismiss();
  }
}
