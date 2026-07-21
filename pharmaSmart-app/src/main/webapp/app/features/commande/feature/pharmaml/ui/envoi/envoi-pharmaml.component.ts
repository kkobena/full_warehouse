import {Component, inject, signal, ChangeDetectionStrategy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgbActiveModal, NgbDateStruct} from '@ng-bootstrap/ng-bootstrap';
import {ButtonComponent, CardComponent, SelectComponent} from '../../../../../../shared/ui';
import {PharmaDatePickerComponent} from '../../../../../../shared/date-picker/pharma-date-picker.component';
import {PharmamlApiService} from '../../../../data-access/pharmaml-api.service';
import {IEnvoiPharmaParams, TypeCommande} from '../../../../../../shared/model/pharmaml.model';
import {CommandeId} from '../../../../../../shared/model/abstract-commande.model';
import {NotificationService} from "../../../../../../shared/services/notification.service";
import {ErrorService} from "../../../../../../shared/error.service";
import {NGB_DATE_TO_ISO, TODAY_NGB_DATE} from '../../../../../../shared/util/warehouse-util';

@Component({
  selector: 'app-envoi-pharmaml',
  imports: [CommonModule, FormsModule, ButtonComponent, CardComponent, PharmaDatePickerComponent, SelectComponent],
  templateUrl: './envoi-pharmaml.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./envoi-pharmaml.scss'],
})
export class EnvoiPharmamlComponent {
  commandeId!: CommandeId;
  protected minDate = TODAY_NGB_DATE();
  private readonly api = inject(PharmamlApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  readonly activeModal = inject(NgbActiveModal);
  readonly loading = signal(false);
  readonly typeCommande = signal<TypeCommande>('NORMALE');
  readonly commentaire = signal('');
  readonly dateLivraison = signal<NgbDateStruct>(TODAY_NGB_DATE());
  readonly typeCommandeOptions = [
    {label: 'Normale', value: 'NORMALE' as TypeCommande},
    {label: 'Exceptionnelle', value: 'EXCEPTIONNELLE' as TypeCommande},
  ];

  envoi(): void {
    const params: IEnvoiPharmaParams = {
      commandeId: {id: this.commandeId.id, orderDate: this.commandeId.orderDate},
      typeCommande: this.typeCommande(),
      commentaire: this.commentaire() || undefined,
      dateLivraisonSouhaitee: NGB_DATE_TO_ISO(this.dateLivraison()) ?? undefined,
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
