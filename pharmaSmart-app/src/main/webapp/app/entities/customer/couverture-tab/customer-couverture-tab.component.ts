import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbModal, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BadgeComponent, ButtonComponent, DataTableComponent, DetailGridComponent, DetailSectionComponent } from 'app/shared/ui';
import { NgbConfirmDialogService } from 'app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { NotificationService } from 'app/shared/services/notification.service';
import { ErrorService } from 'app/shared/error.service';
import { IS_ISO_DATE_PAST } from 'app/shared/util/warehouse-util';
import { ICustomer } from 'app/shared/model/customer.model';
import { IClientTiersPayant } from 'app/shared/model/client-tiers-payant.model';
import { showCommonModal } from '../../sales/selling-home/sale-helper';
import { CustomerService } from '../customer.service';
import { CustomerTiersPayantComponent } from '../customer-tiers-payant/customer-tiers-payant.component';
import { FormAyantDroitComponent } from '../form-ayant-droit/form-ayant-droit.component';

/** Onglet « Couverture » de la fiche client : tiers payants et ayants droit. */
@Component({
  selector: 'app-customer-couverture-tab',
  templateUrl: './customer-couverture-tab.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, NgbTooltip, BadgeComponent, ButtonComponent, DataTableComponent, DetailGridComponent, DetailSectionComponent],
})
export class CustomerCouvertureTabComponent {
  readonly customer = input.required<ICustomer>();
  readonly canEdit = input<boolean>(false);
  /** La fiche a été modifiée (tiers payant ou ayant droit) : le parent recharge le client. */
  readonly changed = output<void>();

  /** Tiers payants par priorité : le premier est la couverture principale. */
  protected readonly tiersPayants = computed<IClientTiersPayant[]>(() =>
    [...(this.customer().tiersPayants ?? [])].sort((a, b) => (a.categorie ?? 0) - (b.categorie ?? 0)),
  );
  protected readonly carteExpiree = IS_ISO_DATE_PAST;
  /** Quatre tiers payants au plus (R0 à R3), et seulement pour un assuré. */
  protected readonly peutAjouterTiersPayant = computed(() => {
    const client = this.customer();
    return client.typeTiersPayant === 'ASSURANCE' && (client.tiersPayants?.length ?? 0) < 4;
  });

  private readonly customerService = inject(CustomerService);
  private readonly modalService = inject(NgbModal);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly destroyRef = inject(DestroyRef);

  protected addTiersPayant(): void {
    showCommonModal(
      this.modalService,
      CustomerTiersPayantComponent,
      { entity: null, customer: this.customer(), title: "FORMULAIRE D'AJOUT DE TIERS PAYANT " },
      (resp: ICustomer) => this.siEnregistre(resp),
      'xl',
    );
  }

  protected editTiersPayant(tiersPayant: IClientTiersPayant): void {
    showCommonModal(
      this.modalService,
      CustomerTiersPayantComponent,
      { entity: tiersPayant, customer: this.customer(), title: `FORMULAIRE DE MODIFICATION DE TIERS PAYANT [ ${tiersPayant.tiersPayantName} ]` },
      (resp: ICustomer) => this.siEnregistre(resp),
      'xl',
    );
  }

  protected removeTiersPayant(tiersPayant: IClientTiersPayant): void {
    this.confirmDialog.onConfirm(
      () => this.appeler(this.customerService.deleteTiersPayant(tiersPayant.id)),
      'SUPPRESSION DE TIERS PAYANT',
      'Voulez-vous vraiment supprimer ce tiers payant ?',
    );
  }

  protected addAyantDroit(): void {
    showCommonModal(
      this.modalService,
      FormAyantDroitComponent,
      { entity: null, assure: this.customer(), title: "FORMULAIRE D'AJOUT D'AYANT DROIT " },
      (resp: ICustomer) => this.siEnregistre(resp),
      'xl',
    );
  }

  protected editAyantDroit(ayantDroit: ICustomer): void {
    showCommonModal(
      this.modalService,
      FormAyantDroitComponent,
      { entity: ayantDroit, assure: this.customer(), title: `FORMULAIRE DE MODIFICATION D'AYANT DROIT [ ${ayantDroit.fullName}  ]` },
      (resp: ICustomer) => this.siEnregistre(resp),
    );
  }

  protected removeAyantDroit(ayantDroit: ICustomer): void {
    this.confirmDialog.onConfirm(
      () => this.appeler(this.customerService.deleteAssuredCustomer(ayantDroit.id)),
      'SUPPRESSION',
      'Voulez-vous vraiment supprimer cet ayant droit ?',
    );
  }

  private siEnregistre(resp: unknown): void {
    if (resp) {
      this.changed.emit();
    }
  }

  private appeler(requete: Observable<unknown>): void {
    requete.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.changed.emit(),
      error: err => this.notificationService.error(this.errorService.getErrorMessage(err, "L'opération a échoué")),
    });
  }
}

