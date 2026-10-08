import {ChangeDetectionStrategy, Component, computed, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {ButtonComponent, DataTableComponent, FormFieldComponent, InputComponent, SelectSearchComponent} from 'app/shared/ui';
import {IPrescripteur} from 'app/shared/model/ordonnance.model';
import {NotificationService} from 'app/shared/services/notification.service';
import {OrdonnanceApiService} from '../../data-access/services/ordonnance-api.service';

type Mode = 'liste' | 'edition' | 'fusion';

/**
 * Gestion du référentiel local des prescripteurs : retrouver, corriger, désactiver, fusionner un doublon.
 * Une fiche n'est jamais supprimée ; fusionner reporte ses ordonnances sur la fiche conservée.
 */
@Component({
  selector: 'app-prescripteurs-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './prescripteurs-modal.component.scss',
  imports: [FormsModule, ButtonComponent, DataTableComponent, FormFieldComponent, InputComponent, SelectSearchComponent],
  template: `
    <div class="modal-header">
      <h5 class="modal-title"><i class="pi pi-users me-2"></i>Prescripteurs</h5>
      <button type="button" class="btn-close" aria-label="Fermer" (click)="fermer()"></button>
    </div>
    <div class="modal-body">
      @if (mode() === 'liste') {
        <div class="mb-3">
          <app-input placeholder="Rechercher par nom ou n° d'ordre" size="small" [(ngModel)]="terme" (ngModelChange)="rechercher()" />
        </div>
        <app-data-table [value]="prescripteurs()" [stripedRows]="true" size="small" emptyMessage="Aucun prescripteur trouvé">
          <ng-template #header>
            <tr class="pharma-table-head">
              <th>Nom</th>
              <th>Spécialité</th>
              <th>N° d'ordre</th>
              <th></th>
            </tr>
          </ng-template>
          <ng-template #body let-p>
            <tr>
              <td>{{ nomComplet(p) }}</td>
              <td>{{ p.specialite }}</td>
              <td>{{ p.numeroOrdre }}</td>
              <td class="text-end text-nowrap">
                <app-button (clicked)="editer(p)" icon="pi pi-pencil" severity="primary" size="small" [text]="true" [iconOnly]="true" ariaLabel="Modifier ce prescripteur" />
                <app-button (clicked)="preparerFusion(p)" icon="pi pi-clone" severity="info" size="small" [text]="true" [iconOnly]="true" ariaLabel="Fusionner ce prescripteur dans un autre" />
                <app-button (clicked)="desactiver(p)" icon="pi pi-eye-slash" severity="secondary" size="small" [text]="true" [iconOnly]="true" ariaLabel="Désactiver ce prescripteur" />
              </td>
            </tr>
          </ng-template>
        </app-data-table>
      } @else if (mode() === 'edition' && courant(); as p) {
        <div class="d-flex flex-column gap-2">
          <app-form-field label="Nom" fieldId="prNom" [required]="true"><app-input [(ngModel)]="p.nom" /></app-form-field>
          <app-form-field label="Prénom" fieldId="prPrenom"><app-input [(ngModel)]="p.prenom" /></app-form-field>
          <app-form-field label="Spécialité" fieldId="prSpecialite"><app-input [(ngModel)]="p.specialite" /></app-form-field>
          <app-form-field label="N° d'ordre" fieldId="prOrdre" hint="Unique s'il est renseigné"><app-input [(ngModel)]="p.numeroOrdre" /></app-form-field>
          <app-form-field label="Structure" fieldId="prStructure"><app-input [(ngModel)]="p.structure" /></app-form-field>
          <app-form-field label="Téléphone" fieldId="prTel"><app-input type="tel" [(ngModel)]="p.telephone" /></app-form-field>
        </div>
      } @else if (mode() === 'fusion' && courant(); as p) {
        <p class="mb-2">
          Les ordonnances de <strong>{{ nomComplet(p) }}</strong> passent sur la fiche conservée ; cette fiche est ensuite désactivée.
        </p>
        <app-form-field label="Fiche à conserver" fieldId="prCible">
          <app-select-search inputId="prCible" [items]="cibles()" bindLabel="libelle" bindValue="id" placeholder="Choisir la fiche à conserver"
                             [(ngModel)]="cibleId" />
        </app-form-field>
      }
      @if (erreur()) {
        <div class="text-danger mt-2">{{ erreur() }}</div>
      }
    </div>
    <div class="modal-footer">
      @if (mode() === 'liste') {
        <app-button (clicked)="fermer()" label="Fermer" severity="secondary" />
      } @else {
        <app-button (clicked)="retour()" label="Retour" severity="secondary" />
        <app-button (clicked)="valider()" [loading]="enCours()" [disabled]="mode() === 'fusion' && !cibleId" icon="pi pi-check"
                    [label]="mode() === 'fusion' ? 'Fusionner' : 'Enregistrer'" severity="primary" />
      }
    </div>
  `,
})
export class PrescripteursModalComponent {
  protected readonly mode = signal<Mode>('liste');
  protected readonly prescripteurs = signal<IPrescripteur[]>([]);
  protected readonly courant = signal<IPrescripteur | null>(null);
  protected readonly enCours = signal(false);
  protected readonly erreur = signal('');
  protected readonly cibles = computed(() =>
    this.prescripteurs()
      .filter(p => p.id !== this.courant()?.id)
      .map(p => ({id: p.id!, libelle: this.nomComplet(p)})),
  );
  protected terme = '';
  protected cibleId: number | null = null;

  private readonly api = inject(OrdonnanceApiService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly notificationService = inject(NotificationService);

  constructor() {
    this.rechercher();
  }

  protected rechercher(): void {
    this.api.rechercherPrescripteurs(this.terme.trim(), 30).subscribe(trouves => this.prescripteurs.set(trouves));
  }

  protected nomComplet(p: IPrescripteur): string {
    return [p.nom, p.prenom].filter(Boolean).join(' ');
  }

  protected editer(p: IPrescripteur): void {
    this.courant.set({...p});
    this.erreur.set('');
    this.mode.set('edition');
  }

  protected preparerFusion(p: IPrescripteur): void {
    this.courant.set(p);
    this.cibleId = null;
    this.erreur.set('');
    this.mode.set('fusion');
  }

  protected retour(): void {
    this.mode.set('liste');
    this.erreur.set('');
  }

  protected desactiver(p: IPrescripteur): void {
    this.api.definirPrescripteurActif(p.id!, false).subscribe({
      next: () => {
        this.notificationService.success(`« ${this.nomComplet(p)} » désactivé`);
        this.rechercher();
      },
      error: err => this.notificationService.error(err?.error?.message ?? 'Désactivation impossible'),
    });
  }

  protected valider(): void {
    const p = this.courant();
    if (!p) {
      return;
    }
    if (this.mode() === 'edition' && !p.nom?.trim()) {
      this.erreur.set('Le nom est obligatoire.');
      return;
    }
    this.enCours.set(true);
    this.erreur.set('');
    const action = this.mode() === 'fusion' ? this.api.fusionnerPrescripteur(p.id!, this.cibleId!) : this.api.modifierPrescripteur(p.id!, p);
    action.subscribe({
      next: () => {
        this.enCours.set(false);
        this.notificationService.success(this.mode() === 'fusion' ? 'Fiches fusionnées' : 'Prescripteur enregistré');
        this.mode.set('liste');
        this.rechercher();
      },
      error: err => {
        this.enCours.set(false);
        this.erreur.set(err?.error?.message ?? "L'opération n'a pas pu être enregistrée.");
      },
    });
  }

  protected fermer(): void {
    this.activeModal.close();
  }
}
