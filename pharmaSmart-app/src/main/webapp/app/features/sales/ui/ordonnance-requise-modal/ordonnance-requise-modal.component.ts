import {ChangeDetectionStrategy, Component, inject, OnInit, signal} from '@angular/core';
import {DatePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {Observable, Subject} from 'rxjs';
import {ButtonComponent, FormFieldComponent, InputComponent, RadioComponent, SelectSearchComponent} from 'app/shared/ui';
import {IOrdonnance} from 'app/shared/model/ordonnance.model';
import {NotificationService} from 'app/shared/services/notification.service';
import {OrdonnanceApiService} from '../../data-access/services/ordonnance-api.service';

/** Ce que le serveur renvoie avec le refus `ordonnanceRequise`. */
export interface OrdonnanceRequise {
  saleId: number;
  saleDate: string;
  customerId?: number | null;
  produits: string[];
}

/**
 * Produit sur ordonnance : la vente doit porter une ordonnance ou un prescripteur. S'il y a une ordonnance en cours
 * pour le client, on l'associe ; sinon on indique simplement le prescripteur. La vente n'est finalisée qu'ensuite.
 */
@Component({
  selector: 'app-ordonnance-requise-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './ordonnance-requise-modal.component.scss',
  imports: [DatePipe, FormsModule, ButtonComponent, FormFieldComponent, InputComponent, RadioComponent, SelectSearchComponent],
  template: `
    <div class="modal-header">
      <h5 class="modal-title"><i class="pi pi-file-edit me-2"></i>Ordonnance requise</h5>
    </div>
    <div class="modal-body">
      <div class="alert alert-warning py-2">
        Cette vente contient un produit sur ordonnance : <strong>{{ requise.produits.join(', ') }}</strong>.
        Associez l'ordonnance, ou à défaut indiquez le prescripteur.
      </div>

      @if (ordonnances().length > 0) {
        <h6 class="mb-2">Ordonnances en cours du client</h6>
        <div class="d-flex flex-column gap-2 mb-3">
          @for (o of ordonnances(); track o.id) {
            <app-radio name="ordonnance" [value]="o.id" [(ngModel)]="ordonnanceId" (ngModelChange)="prescripteurId = null"
                       [label]="(o.datePrescription | date: 'dd/MM/yyyy') + (o.prescripteurNom ? ' · ' + o.prescripteurNom : '') + ' — ' + o.lignes.length + ' ligne(s)'" />
          }
        </div>
        <h6 class="mb-2">Ou, sans ordonnance : le prescripteur</h6>
      } @else {
        <h6 class="mb-2">Prescripteur</h6>
      }

      <app-form-field label="Prescripteur" fieldId="reqPrescripteur">
        <app-select-search inputId="reqPrescripteur" [items]="prescripteurs()" bindLabel="libelle" bindValue="id" [clearable]="true"
                           [typeahead]="recherche$" (searched)="chercher($event)" [minSearchLength]="2" placeholder="Nom du prescripteur"
                           [(ngModel)]="prescripteurId" (ngModelChange)="ordonnanceId = null" />
      </app-form-field>
      <div class="d-flex gap-2 mt-2">
        <div class="flex-grow-1">
          <app-input placeholder="Pas dans la liste ? Nom du nouveau prescripteur" size="small" [(ngModel)]="nouveau" />
        </div>
        <app-button (clicked)="creer()" [disabled]="!nouveau.trim()" icon="pi pi-user-plus" label="Créer" severity="secondary" size="small" />
      </div>
      @if (erreur()) {
        <div class="text-danger mt-2">{{ erreur() }}</div>
      }
    </div>
    <div class="modal-footer">
      <app-button (clicked)="annuler()" icon="pi pi-times" label="Revenir à la vente" severity="secondary" />
      <app-button (clicked)="valider()" [disabled]="enCours() || (!ordonnanceId && !prescripteurId)" icon="pi pi-check" label="Valider" severity="primary" />
    </div>
  `,
})
export class OrdonnanceRequiseModalComponent implements OnInit {
  requise!: OrdonnanceRequise;

  protected readonly ordonnances = signal<IOrdonnance[]>([]);
  protected readonly prescripteurs = signal<{id: number; libelle: string}[]>([]);
  protected readonly enCours = signal(false);
  protected readonly erreur = signal('');
  protected readonly recherche$ = new Subject<string>();
  protected ordonnanceId: number | null = null;
  protected prescripteurId: number | null = null;
  protected nouveau = '';

  private readonly api = inject(OrdonnanceApiService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly notificationService = inject(NotificationService);

  ngOnInit(): void {
    if (this.requise.customerId) {
      this.api.duClient(this.requise.customerId, 'EN_COURS').subscribe({
        next: ordonnances => {
          this.ordonnances.set(ordonnances);
          if (ordonnances.length === 1) {
            this.ordonnanceId = ordonnances[0].id;
          }
        },
        // Sans la liste, le prescripteur reste possible : l'exigence se remplit quand même.
        error: () => this.ordonnances.set([]),
      });
    }
  }

  protected chercher(terme: string): void {
    if (terme.trim().length >= 2) {
      this.api.rechercherPrescripteurs(terme.trim()).subscribe(trouves => this.prescripteurs.set(trouves.map(p => ({id: p.id!, libelle: this.libelle(p)}))));
    }
  }

  protected creer(): void {
    const nom = this.nouveau.trim();
    if (!nom) {
      return;
    }
    this.api.creerPrescripteur({nom, actif: true}).subscribe({
      next: p => {
        this.prescripteurs.set([{id: p.id!, libelle: this.libelle(p)}]);
        this.prescripteurId = p.id!;
        this.ordonnanceId = null;
        this.nouveau = '';
      },
      error: err => this.erreur.set(err?.error?.message ?? 'Création du prescripteur impossible'),
    });
  }

  protected valider(): void {
    this.enCours.set(true);
    this.erreur.set('');
    const action: Observable<unknown> = this.ordonnanceId
      ? this.api.apparierVente(this.ordonnanceId, this.requise.saleId, this.requise.saleDate)
      : this.api.definirPrescripteurDeVente(this.requise.saleId, this.requise.saleDate, this.prescripteurId!);
    action.subscribe({
      next: () => {
        this.notificationService.success(this.ordonnanceId ? 'Ordonnance associée à la vente' : 'Prescripteur enregistré');
        this.activeModal.close(true);
      },
      error: err => {
        this.enCours.set(false);
        this.erreur.set(err?.error?.message ?? "L'ordonnance ou le prescripteur n'a pas pu être enregistré.");
      },
    });
  }

  protected annuler(): void {
    this.activeModal.close(false);
  }

  private libelle(p: {nom: string; prenom?: string | null; specialite?: string | null}): string {
    return [p.nom, p.prenom].filter(Boolean).join(' ') + (p.specialite ? ` (${p.specialite})` : '');
  }
}
