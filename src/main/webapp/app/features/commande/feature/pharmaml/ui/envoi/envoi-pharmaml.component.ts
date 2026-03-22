import {Component, ElementRef, inject, Renderer2, signal} from '@angular/core';
import {CommonModule, DatePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Button} from 'primeng/button';
import {Select} from 'primeng/select';
import {Textarea} from 'primeng/textarea';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {PharmamlApiService} from '../../../../data-access/pharmaml-api.service';
import {IEnvoiPharmaParams, TypeCommande} from '../../../../../../shared/model/pharmaml.model';
import {CommandeId} from '../../../../../../shared/model/abstract-commande.model';
import {NotificationService} from "../../../../../../shared/services/notification.service";
import {ErrorService} from "../../../../../../shared/error.service";
import {Card} from "primeng/card";
import {DatePicker} from "primeng/datepicker";
import {PrimeNG} from "primeng/config";
import {TranslateService} from "@ngx-translate/core";

@Component({
  selector: 'app-envoi-pharmaml',
  imports: [CommonModule, FormsModule, Button, Select, Textarea, Card, DatePicker],
  providers: [DatePipe],
  templateUrl: './envoi-pharmaml.component.html',
  styleUrls: ['./envoi-pharmaml.scss'],
})
export class EnvoiPharmamlComponent {
  commandeId!: CommandeId;
  protected minDate = new Date();
  private readonly api = inject(PharmamlApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  readonly activeModal = inject(NgbActiveModal);
  private readonly renderer = inject(Renderer2);
  private readonly elementRef = inject(ElementRef);
  readonly loading = signal(false);
  readonly typeCommande = signal<TypeCommande>('NORMALE');
  readonly commentaire = signal('');
  readonly dateLivraison = signal(new Date());
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly translate = inject(TranslateService);
  private readonly datePipe = inject(DatePipe);
  readonly typeCommandeOptions = [
    {label: 'Normale', value: 'NORMALE' as TypeCommande},
    {label: 'Exceptionnelle', value: 'EXCEPTIONNELLE' as TypeCommande},
  ];

  constructor() {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
  }


  envoi(): void {
    const params: IEnvoiPharmaParams = {
      commandeId: {id: this.commandeId.id, orderDate: this.commandeId.orderDate},
      typeCommande: this.typeCommande(),
      commentaire: this.commentaire() || undefined,
      dateLivraisonSouhaitee: this.datePipe.transform(this.dateLivraison(), 'yyyy-MM-dd'),
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

  protected onDropdownShow(event: any): void {
    const modalBody = this.elementRef.nativeElement.querySelector('.modal-body');
    if (modalBody) {
      this.renderer.addClass(modalBody, 'overflow-visible');
    }
  }

  protected onDropdownHide(event: any): void {
    const modalBody = this.elementRef.nativeElement.querySelector('.modal-body');
    if (modalBody) {
      this.renderer.removeClass(modalBody, 'overflow-visible');
    }
  }

  dismiss(): void {
    this.activeModal.dismiss();
  }
}
