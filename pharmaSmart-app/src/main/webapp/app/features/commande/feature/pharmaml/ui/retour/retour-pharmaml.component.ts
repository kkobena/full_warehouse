import {ChangeDetectionStrategy, Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {ButtonComponent, DataTableComponent, SelectComponent} from '../../../../../../shared/ui';
import {CommandeId} from '../../../../../../shared/model/abstract-commande.model';
import {
  ILigneRetour,
  IVerificationItem,
  MotifRetour
} from '../../../../../../shared/model/pharmaml.model';
import {PharmamlApiService} from '../../../../data-access/pharmaml-api.service';
import {NotificationService} from "../../../../../../shared/services/notification.service";
import {ErrorService} from "../../../../../../shared/error.service";

interface LigneRetourRow extends IVerificationItem {
  selected: boolean;
  quantiteRetour: number;
  motifRetour: MotifRetour;
}

const MOTIFS: { value: MotifRetour; label: string }[] = [
  {value: 'AVARIE', label: 'Avarie'},
  {value: 'NON_CONFORME', label: 'Non conforme'},
  {value: 'PERIME', label: 'Périmé'},
  {value: 'ERREUR_LIVRAISON', label: 'Erreur de livraison'},
  {value: 'EXCEDENT', label: 'Excédent'},
];

@Component({
  selector: 'app-retour-pharmaml',
  imports: [CommonModule, FormsModule, ButtonComponent, DataTableComponent, SelectComponent],
  styleUrls: ['./retour-pharmam.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    <div class="modal-header">
      <h5 class="modal-title">Retour marchandise</h5>
      <button type="button" class="btn-close" (click)="cancel()"></button>
    </div>
    <div class="modal-body">
      @if (loading()) {
        <div class="text-center py-3">
          <i class="pi pi-spin pi-spinner me-2"></i>Chargement des lignes reçues...
        </div>
      } @else if (lignes().length === 0) {
        <div class="alert alert-info">Aucune ligne reçue trouvée pour cette commande.</div>
      } @else {
        <p class="text-muted mb-3">
          Sélectionnez les produits à retourner, précisez la quantité et le motif pour chaque ligne.
        </p>
        <app-data-table [value]="lignes()" class="p-datatable-sm ">
          <ng-template #header>
            <tr class="pharma-table-head">
              <th style="width:3rem"></th>
              <th>Produit</th>
              <th class="text-center" style="width:8rem">Qté reçue</th>
              <th class="text-center" style="width:8rem">Qté retour</th>
              <th style="width:12rem">Motif</th>
            </tr>
          </ng-template>
          <ng-template #body let-row>
            <tr [class.table-active]="row.selected">
              <td class="text-center">
                <input type="checkbox" class="form-check-input" [(ngModel)]="row.selected" />
              </td>
              <td>
                <div class="fw-medium">{{ row.produitLibelle }}</div>
                <small class="text-muted"><code>{{ row.codeCip || row.codeEan }}</code></small>
              </td>
              <td class="text-center">{{ row.quantitePriseEnCompte }}</td>
              <td class="text-center">
                <input
                  type="number"
                  class="form-control form-control-sm text-center"
                  [(ngModel)]="row.quantiteRetour"
                  [min]="1"
                  [max]="row.quantitePriseEnCompte"
                  [disabled]="!row.selected"
                  style="width:5rem"
                />
              </td>
              <td>
                <app-select
                  [(ngModel)]="row.motifRetour"
                  [disabled]="!row.selected"
                  [items]="motifs"
                  bindLabel="label"
                  bindValue="value"
                  [clearable]="false"
                />
              </td>
            </tr>
          </ng-template>
        </app-data-table>
      }
      @if (error()) {
        <div class="alert alert-danger py-2 mt-2">{{ error() }}</div>
      }
    </div>
    <div class="modal-footer gap-2">
      <app-button label="Fermer" severity="secondary" [outlined]="true" (clicked)="cancel()" />
      <app-button
        label="Envoyer le retour"
        severity="warn"
        icon="pi pi-reply"
        [loading]="sending()"
        [disabled]="!hasSelection()"
        (clicked)="confirmer()"
      />
    </div>
  `,
})
export class RetourPharmamlComponent implements OnInit {
  commandeId!: CommandeId;
  commandeRef!: string;
  readonly loading = signal(true);
  readonly sending = signal(false);
  readonly error = signal<string | null>(null);
  readonly lignes = signal<LigneRetourRow[]>([]);
  readonly motifs = MOTIFS;
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(PharmamlApiService);

  ngOnInit(): void {
    this.api.lignesRetour(this.commandeRef, this.commandeId.id.toString()).subscribe({
      next: res => {
        const items = res.body?.items ?? [];
        this.lignes.set(
          items.map(item => ({
            ...item,
            selected: false,
            quantiteRetour: item.quantitePriseEnCompte,
            motifRetour: 'ERREUR_LIVRAISON' as MotifRetour,
          })),
        );
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur');

      },
    });
  }

  hasSelection(): boolean {
    return this.lignes().some(l => l.selected && l.quantiteRetour > 0);
  }

  confirmer(): void {
    if (this.sending() || !this.hasSelection()) {
      return;
    }
    this.sending.set(true);
    this.error.set(null);

    const payload: ILigneRetour[] = this.lignes()
      .filter(l => l.selected && l.quantiteRetour > 0)
      .map(l => ({
        codeProduit: l.codeCip || l.codeEan,
        quantite: l.quantiteRetour,
        motifRetour: l.motifRetour,
      }));

    this.api.retour(this.commandeId.id, this.commandeId.orderDate, payload).subscribe({
      next: () => {
        this.sending.set(false);
        this.activeModal.close('retourne');
      },
      error: (err) => {
        this.sending.set(false);
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur');
      },
    });
  }

  cancel(): void {
    this.activeModal.dismiss();
  }
}
