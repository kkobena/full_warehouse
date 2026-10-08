import {ChangeDetectionStrategy, Component, computed, effect, inject, input, model, signal, untracked} from '@angular/core';
import {DatePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgbDateStruct, NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {Subject} from 'rxjs';
import {BadgeComponent, ButtonComponent, DataTableComponent, FormFieldComponent, InputComponent, InputNumberComponent, OffcanvasComponent, SelectSearchComponent} from 'app/shared/ui';
import {PharmaDatePickerComponent} from 'app/shared/date-picker/pharma-date-picker.component';
import {ProduitSearch} from 'app/shared/model';
import {IOrdonnance, IOrdonnanceLigne, IOrdonnanceLigneCreation, StatutOrdonnance} from 'app/shared/model/ordonnance.model';
import {NotificationService} from 'app/shared/services/notification.service';
import {NGB_DATE_TO_ISO, TODAY_NGB_DATE} from 'app/shared/util/warehouse-util';
import {SalesFacade} from '../../data-access/facades/sales.facade';
import {FicheClientPanelService} from '../../data-access/services/fiche-client-panel.service';
import {OrdonnanceApiService} from '../../data-access/services/ordonnance-api.service';
import {OrdonnancePanelService} from '../../data-access/services/ordonnance-panel.service';
import {ProductSearchService} from '../../data-access/services/product-search.service';
import {PrescripteursModalComponent} from '../prescripteurs-modal/prescripteurs-modal.component';

type Severite = 'success' | 'info' | 'danger';

const STATUTS: Record<StatutOrdonnance, {libelle: string; severity: Severite}> = {
  EN_COURS: {libelle: 'En cours', severity: 'success'},
  TERMINEE: {libelle: 'Terminée', severity: 'info'},
  EXPIREE: {libelle: 'Expirée', severity: 'danger'},
};

interface LigneSaisie extends IOrdonnanceLigneCreation {
  libelle: string;
}

/**
 * Ordonnances du client consultées sans quitter la vente : reste à délivrer, renouvellements, saisie.
 * « Ajouter » présélectionne le produit par le circuit ordinaire de l'écran de vente (stock, prix, contrôles).
 */
@Component({
  selector: 'app-ordonnance-panel',
  templateUrl: './ordonnance-panel.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {'(window:keydown)': 'onKeydown($event)'},
  imports: [
    DatePipe,
    FormsModule,
    BadgeComponent,
    ButtonComponent,
    DataTableComponent,
    FormFieldComponent,
    InputComponent,
    InputNumberComponent,
    OffcanvasComponent,
    PharmaDatePickerComponent,
    SelectSearchComponent,
  ],
})
export class OrdonnancePanelComponent {
  readonly customerId = input<number | null | undefined>(null);
  readonly visible = model<boolean>(false);

  protected readonly vue = signal<'liste' | 'nouvelle'>('liste');
  protected readonly ordonnances = signal<IOrdonnance[]>([]);
  protected readonly chargement = signal(false);
  protected readonly enregistrement = signal(false);
  protected readonly voirToutes = signal(false);
  protected readonly affichees = computed(() => (this.voirToutes() ? this.ordonnances() : this.ordonnances().filter(o => o.statut === 'EN_COURS')));
  protected readonly masquees = computed(() => this.ordonnances().length - this.affichees().length);
  protected readonly venteEnCours = computed(() => this.facade.currentSale()?.saleId ?? null);
  protected readonly statuts = STATUTS;

  // Saisie d'une ordonnance
  protected prescripteurId: number | null = null;
  protected nouveauPrescripteur = '';
  protected datePrescription: NgbDateStruct | null = TODAY_NGB_DATE();
  protected dateFin: NgbDateStruct | null = null;
  protected renouvellements: number | null = 0;
  protected note = '';
  protected readonly prescripteurs = signal<{id: number; libelle: string}[]>([]);
  protected readonly lignes = signal<LigneSaisie[]>([]);
  protected readonly produits = signal<ProduitSearch[]>([]);
  protected readonly rechercheProduit$ = new Subject<string>();
  protected readonly recherchePrescripteur$ = new Subject<string>();
  protected ligneProduitId: number | null = null;
  protected ligneTexte = '';
  protected lignePosologie = '';
  protected ligneQuantite: number | null = 1;
  protected ligneDuree: number | null = null;

  private readonly api = inject(OrdonnanceApiService);
  private readonly panelService = inject(OrdonnancePanelService);
  private readonly facade = inject(SalesFacade);
  private readonly ficheClient = inject(FicheClientPanelService);
  private readonly productSearch = inject(ProductSearchService);
  private readonly notificationService = inject(NotificationService);
  private readonly modalService = inject(NgbModal);

  constructor() {
    // Rechargée à chaque ouverture : une vente a pu consommer l'ordonnance depuis.
    effect(() => {
      const id = this.customerId();
      if (this.visible() && id) {
        untracked(() => {
          this.vue.set('liste');
          this.charger(id);
        });
      }
    });
  }

  protected fermer(): void {
    this.visible.set(false);
  }

  protected gererPrescripteurs(): void {
    this.modalService.open(PrescripteursModalComponent, {centered: true, size: 'lg'});
  }

  /** Alt+N : nouvelle ordonnance ; Alt+E : enregistrer ; Alt+L : ajouter la ligne ; Alt+G : prescripteurs ; Alt+R : retour à la liste. */
  protected onKeydown(event: KeyboardEvent): void {
    if (!this.visible() || !event.altKey || event.ctrlKey || event.shiftKey || document.querySelector('ngb-modal-window')) {
      return;
    }
    const liste = this.vue() === 'liste';
    const touche = event.key.toLowerCase();
    if (liste && touche === 'n') {
      this.nouvelle();
    } else if (liste && touche === 'g') {
      this.gererPrescripteurs();
    } else if (!liste && touche === 'e') {
      this.enregistrer();
    } else if (!liste && touche === 'l') {
      this.ajouterLigne();
    } else if (!liste && touche === 'r') {
      this.annulerSaisie();
    } else {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
  }

  protected nouvelle(): void {
    this.vue.set('nouvelle');
  }

  protected annulerSaisie(): void {
    this.reinitialiserSaisie();
    this.vue.set('liste');
  }

  protected chercherProduit(terme: string): void {
    if (terme.trim().length >= 2) {
      this.productSearch.search(terme, 20).subscribe(resultats => this.produits.set(resultats));
    }
  }

  protected chercherPrescripteur(terme: string): void {
    if (terme.trim().length >= 2) {
      this.api.rechercherPrescripteurs(terme.trim()).subscribe(trouves => this.prescripteurs.set(trouves.map(p => ({id: p.id!, libelle: this.nomPrescripteur(p)}))));
    }
  }

  protected creerPrescripteur(): void {
    const nom = this.nouveauPrescripteur.trim();
    if (!nom) {
      return;
    }
    this.api.creerPrescripteur({nom, actif: true}).subscribe({
      next: p => {
        this.prescripteurs.set([{id: p.id!, libelle: this.nomPrescripteur(p)}]);
        this.prescripteurId = p.id!;
        this.nouveauPrescripteur = '';
      },
      error: err => this.notificationService.error(err?.error?.message ?? 'Création du prescripteur impossible'),
    });
  }

  protected ajouterLigne(): boolean {
    const produit = this.produits().find(p => p.id === this.ligneProduitId);
    const texte = this.ligneTexte.trim();
    if ((!produit && !texte) || !this.ligneQuantite || this.ligneQuantite < 1) {
      this.notificationService.warning('Choisissez un produit (ou saisissez un texte) et une quantité');
      return false;
    }
    this.lignes.update(l => [
      ...l,
      {
        produitId: produit?.id ?? null,
        texteLu: texte || null,
        libelle: produit?.libelle ?? texte,
        posologie: this.lignePosologie.trim() || null,
        dureeJours: this.ligneDuree || null,
        quantitePrescrite: this.ligneQuantite!,
      },
    ]);
    this.ligneProduitId = null;
    this.ligneTexte = '';
    this.lignePosologie = '';
    this.ligneQuantite = 1;
    this.ligneDuree = null;
    return true;
  }

  protected retirerLigne(index: number): void {
    this.lignes.update(l => l.filter((_, i) => i !== index));
  }

  protected enregistrer(): void {
    // Une ligne saisie mais pas encore ajoutée avec « + » est prise en compte.
    if ((this.ligneProduitId || this.ligneTexte.trim()) && !this.ajouterLigne()) {
      return;
    }
    const customerId = this.customerId();
    const date = NGB_DATE_TO_ISO(this.datePrescription);
    if (!customerId || !date || this.lignes().length === 0) {
      this.notificationService.warning('Date de prescription et au moins une ligne sont requises');
      return;
    }
    this.enregistrement.set(true);
    this.api
      .creer({
        customerId,
        prescripteurId: this.prescripteurId,
        datePrescription: date,
        renouvellements: this.renouvellements ?? 0,
        dateFinValidite: NGB_DATE_TO_ISO(this.dateFin),
        note: this.note.trim() || null,
        lignes: this.lignes().map(({libelle, ...ligne}) => ligne),
      })
      .subscribe({
        next: () => {
          this.enregistrement.set(false);
          this.reinitialiserSaisie();
          this.vue.set('liste');
          this.charger(customerId);
        },
        error: err => {
          this.enregistrement.set(false);
          this.notificationService.error(err?.error?.message ?? "L'ordonnance n'a pas pu être enregistrée");
        },
      });
  }

  /** Le produit est recherché dans le catalogue de vente : prix et stock du jour. */
  protected ajouterAuPanier(ligne: IOrdonnanceLigne): void {
    if (!ligne.produitId || !ligne.produitLibelle) {
      return;
    }
    // Les libellés de la BDPM portent la forme après une virgule (« …1 g, comprimé dispersible »), que la recherche ne retrouve pas.
    this.productSearch.search(ligne.produitLibelle.split(',')[0], 20).subscribe(resultats => {
      const trouve = resultats.find(p => p.id === ligne.produitId);
      if (trouve) {
        this.fermer();
        this.ficheClient.redelivrer$.next(trouve);
      } else {
        this.notificationService.error(`${ligne.produitLibelle} n'est plus disponible à la vente`);
      }
    });
  }

  /** Rattache la vente en cours et lie ses lignes (même produit, ou générique du même groupe) ; relançable après un ajout. */
  protected rattacher(ordonnance: IOrdonnance): void {
    const venteId = this.facade.currentSale()?.saleId;
    if (!venteId) {
      return;
    }
    this.api.apparierVente(ordonnance.id, venteId.id, venteId.saleDate).subscribe({
      next: () => this.charger(ordonnance.customerId),
      error: err => this.notificationService.error(err?.error?.message ?? 'Rattachement impossible'),
    });
  }

  protected cloturer(ordonnance: IOrdonnance): void {
    this.api.cloturer(ordonnance.id, true).subscribe({
      next: () => this.charger(ordonnance.customerId),
      error: err => this.notificationService.error(err?.error?.message ?? 'Clôture impossible'),
    });
  }

  private charger(customerId: number): void {
    this.chargement.set(true);
    this.api.duClient(customerId).subscribe({
      next: ordonnances => {
        this.ordonnances.set(ordonnances);
        this.chargement.set(false);
        this.panelService.notifierChangement();
      },
      error: () => {
        this.ordonnances.set([]);
        this.chargement.set(false);
        this.notificationService.error('Ordonnances indisponibles');
      },
    });
  }

  private reinitialiserSaisie(): void {
    this.prescripteurId = null;
    this.nouveauPrescripteur = '';
    this.datePrescription = TODAY_NGB_DATE();
    this.dateFin = null;
    this.renouvellements = 0;
    this.note = '';
    this.lignes.set([]);
    this.ligneProduitId = null;
    this.ligneTexte = '';
    this.lignePosologie = '';
    this.ligneQuantite = 1;
    this.ligneDuree = null;
  }

  private nomPrescripteur(p: {nom: string; prenom?: string | null; specialite?: string | null}): string {
    return [p.nom, p.prenom].filter(Boolean).join(' ') + (p.specialite ? ` (${p.specialite})` : '');
  }
}
