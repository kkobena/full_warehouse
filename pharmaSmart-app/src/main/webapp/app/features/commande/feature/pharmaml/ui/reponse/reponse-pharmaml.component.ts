import {ChangeDetectionStrategy, Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {
  BadgeComponent,
  ButtonComponent,
  CardComponent,
  DataTableComponent
} from '../../../../../../shared/ui';
import {PharmamlApiService} from '../../../../data-access/pharmaml-api.service';
import {
  IVerificationItem,
  IVerificationResponse
} from '../../../../../../shared/model/pharmaml.model';
import {NotificationService} from "../../../../../../shared/services/notification.service";
import {ErrorService} from "../../../../../../shared/error.service";

@Component({
  selector: 'app-reponse-pharmaml',
  imports: [CommonModule, ButtonComponent, DataTableComponent, BadgeComponent, CardComponent],
  templateUrl: './reponse-pharmaml.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./reponse-pharmaml.scss'],
})
export class ReponsePharmamlComponent implements OnInit {
  commandeRef!: string;
  orderId!: string;
  readonly activeModal = inject(NgbActiveModal);
  readonly loading = signal(false);
  readonly response = signal<IVerificationResponse | null>(null);
  private readonly api = inject(PharmamlApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  get items(): IVerificationItem[] {
    return this.response()?.items ?? [];
  }

  get extraItems(): IVerificationItem[] {
    return this.response()?.extraItems ?? [];
  }

  ngOnInit(): void {
    this.loadRetour();
  }

  loadRetour(): void {
    this.loading.set(true);
    this.api.lignesRetour(this.commandeRef, this.orderId).subscribe({
      next: res => {
        this.loading.set(false);
        this.response.set(res.body);
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
