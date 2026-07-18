import { Component, computed, effect, inject, input, output, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TooltipModule } from 'primeng/tooltip';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { IProduit } from 'app/shared/model/produit.model';
import { IProduitIndicateurs } from '../../models/produit-indicateurs.model';
import { ILotPeremption, ILotProduit, ProductsApiService } from "../../data-access/services/products-api.service";
import { PrixReference } from '../prix-reference/model/prix-reference.model';
import { PrixReferenceService } from '../prix-reference/prix-reference.service';
import { AddPrixFormComponent } from '../prix-reference/add-prix-form/add-prix-form.component';
import { NgbConfirmDialogService } from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { showCommonModal } from '../../../../entities/sales/selling-home/sale-helper';
import { NotificationService } from '../../../../shared/services/notification.service';

@Component({
  selector: 'app-produit-synthese-tab',
  templateUrl: './produit-synthese-tab.component.html',
  styleUrls: ['./produit-synthese-tab.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, FormsModule, TooltipModule, ButtonModule, TagModule, ToggleSwitch],
})
export class ProduitSyntheseTabComponent {
  readonly produit = input.required<IProduit>();
  readonly indicateurs = input<IProduitIndicateurs | null>(null);
  readonly lots = input<ILotProduit[]>([]);
  readonly loadingIndicateurs = input<boolean>(false);
  readonly canEdit = input<boolean>(false);

  readonly refreshRequested = output<void>();

  protected prixReferences = signal<PrixReference[]>([]);
  protected gestionLotPending = signal(false);
  protected thermosensiblePending = signal(false);
  protected medicamentEssentielPending = signal(false);
  protected produitGardePending = signal(false);
  protected classificationOverriddenPending = signal(false);

  private readonly prixReferenceService = inject(PrixReferenceService);
  private readonly modalService = inject(NgbModal);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly api = inject(ProductsApiService);
  private readonly notificationService = inject(NotificationService);

  constructor() {
    effect(() => {
      const id = this.produit()?.id;
      if (id) {
        this.loadPrixReferences(id);
      }
    });
  }

  protected readonly margeAbsolue = computed(() => {
    const p = this.produit();
    if (!p.regularUnitPrice || !p.costAmount) return null;
    return p.regularUnitPrice - p.costAmount;
  });

  protected readonly tauxMarge = computed(() => {
    const ind = this.indicateurs();
    if (ind?.tauxMarge != null) return ind.tauxMarge;
    const p = this.produit();
    if (!p.regularUnitPrice || p.regularUnitPrice === 0) return null;
    return Math.round(((p.regularUnitPrice - (p.costAmount ?? 0)) / p.regularUnitPrice) * 100);
  });

  /** couvertureStockJours sanitisé : null si négatif ou invalide */
  protected readonly couvertureJours = computed(() => {
    const v = this.indicateurs()?.couvertureStockJours;
    if (v == null || v < 0) return null;
    return v;
  });

  protected readonly statutLegalTag = computed((): { label: string; severity: 'success' | 'warn' | 'danger' | 'secondary'; icon: string } => {
    switch (this.produit().statutLegal) {
      case 'SANS_LISTE':  return { label: 'Sans liste',  severity: 'success', icon: 'pi pi-check-circle' };
      case 'LISTE_I':     return { label: 'Liste I',     severity: 'warn',    icon: 'pi pi-exclamation-circle' };
      case 'LISTE_II':    return { label: 'Liste II',    severity: 'warn',    icon: 'pi pi-exclamation-circle' };
      case 'STUPEFIANTS': return { label: 'Stupéfiants', severity: 'danger',  icon: 'pi pi-ban' };
      case 'PSO':         return { label: 'PSO',         severity: 'danger',  icon: 'pi pi-ban' };
      default:            return { label: '—',           severity: 'secondary', icon: 'pi pi-minus' };
    }
  });

  /** rotationAnnuelleQte sanitisée : null si négative */
  protected readonly rotationAnnuelle = computed(() => {
    const v = this.indicateurs()?.rotationAnnuelleQte;
    if (v == null || v < 0) return null;
    return v;
  });

  /** Couleur du stock actuel : danger si rupture, warning si sous seuil, success sinon */
  protected readonly stockClass = computed(() => {
    const qty = this.produit().totalQuantity ?? 0;
    const seuil = this.produit().seuilMini ?? 0;
    if (qty <= 0) return 'kpi-danger';
    if (seuil > 0 && qty < seuil) return 'kpi-warning';
    return 'kpi-success';
  });

  protected readonly joursStockClass = computed(() => {
    const jours = this.couvertureJours();
    if (jours == null) return '';
    if (jours < 7) return 'kpi-danger';
    if (jours < 30) return 'kpi-warning';
    return 'kpi-success';
  });

  /** Lot avec la date de péremption la plus proche */
  protected readonly lotLeProchePrime = computed(() => {
    const lots = this.lots();
    if (!lots.length) return null;
    return lots.reduce((nearest, lot) => {
      if (!nearest?.peremptionStatut) return lot;
      if (!lot.peremptionStatut) return nearest;
      return lot.peremptionStatut.days < nearest.peremptionStatut.days ? lot : nearest;
    });
  });

  protected readonly alertePeremption = computed((): 'critical' | 'warning' | null => {
    const lot = this.lotLeProchePrime();
    if (!lot?.peremptionStatut) return null;
    const mois = lot.peremptionStatut.mouths;
    if (mois < 3) return 'critical';
    if (mois < 6) return 'warning';
    return null;
  });

  protected onToggleGestionLot(active: boolean): void {
    const id = this.produit().id;
    if (!id) return;
    this.gestionLotPending.set(true);
    this.api.patchGestionLot(id, active).subscribe({
      next: () => {
        this.gestionLotPending.set(false);
        this.notificationService.success(
          active ? 'Contrôle des lots activé pour ce produit' : 'Contrôle des lots désactivé',
          'Gestion des lots'
        );
        this.refreshRequested.emit();
      },
      error: () => {
        this.gestionLotPending.set(false);
        this.notificationService.error('Impossible de modifier le paramètre', 'Erreur');
      },
    });
  }

  protected onToggleThermosensible(value: boolean): void {
    this.toggleFlag('THERMOSENSIBLE', value, this.thermosensiblePending, 'Thermosensible');
  }

  protected onToggleMedicamentEssentiel(value: boolean): void {
    this.toggleFlag('MEDICAMENT_ESSENTIEL', value, this.medicamentEssentielPending, 'Médicament essentiel');
  }

  protected onToggleProduitGarde(value: boolean): void {
    this.toggleFlag('PRODUIT_GARDE', value, this.produitGardePending, 'Produit de garde');
  }

  protected onToggleClassificationOverridden(value: boolean): void {
    this.toggleFlag('CLASSIFICATION_OVERRIDDEN', value, this.classificationOverriddenPending, 'Classification personnalisée');
  }

  private toggleFlag(
    flag: 'THERMOSENSIBLE' | 'MEDICAMENT_ESSENTIEL' | 'PRODUIT_GARDE' | 'CLASSIFICATION_OVERRIDDEN',
    value: boolean,
    pending: ReturnType<typeof signal<boolean>>,
    label: string
  ): void {
    const id = this.produit().id;
    if (!id) return;
    pending.set(true);
    this.api.patchFlag(id, flag, value).subscribe({
      next: () => {
        pending.set(false);
        this.notificationService.success(
          `${label} ${value ? 'activé' : 'désactivé'} pour ce produit`,
          label
        );
        this.refreshRequested.emit();
      },
      error: () => {
        pending.set(false);
        this.notificationService.error('Impossible de modifier le paramètre', 'Erreur');
      },
    });
  }

  protected formatPrix(montant?: number | null): string {
    if (montant == null) return '—';
    return montant.toLocaleString('fr-FR', { minimumFractionDigits: 0 }) + ' FCFA';
  }

  protected formatDate(date?: any): string {
    if (!date) return '—';
    return new Date(date).toLocaleDateString('fr-FR');
  }

  protected onAddPrix(): void {
    this.openPrixModal();
  }

  protected onEditPrix(prix: PrixReference): void {
    this.openPrixModal(prix);
  }

  protected onDeletePrix(prix: PrixReference): void {
    this.confirmDialog.onConfirm(
      () => this.prixReferenceService.delete(prix.id!).subscribe(() => this.loadPrixReferences(this.produit().id!)),
      'Suppression',
      'Voulez-vous vraiment supprimer ce prix de référence ?'
    );
  }

  protected onTogglePrix(prix: PrixReference): void {
    const msg = prix.enabled ? 'Désactiver ce prix de référence ?' : 'Activer ce prix de référence ?';
    this.confirmDialog.onConfirm(
      () => {
        prix.enabled = !prix.enabled;
        this.prixReferenceService.update(prix).subscribe(() => this.loadPrixReferences(this.produit().id!));
      },
      'Activation/Désactivation',
      msg
    );
  }

  private loadPrixReferences(produitId: number): void {
    this.prixReferenceService.query(produitId).subscribe(res => {
      this.prixReferences.set(res.body ?? []);
    });
  }

  private openPrixModal(entity?: PrixReference): void {
    showCommonModal(
      this.modalService,
      AddPrixFormComponent,
      { isFromProduit: true, produit: this.produit(), entity: entity ?? null },
      () => this.loadPrixReferences(this.produit().id!),
      'lg',
      null,
      () => this.loadPrixReferences(this.produit().id!)
    );
  }
}
