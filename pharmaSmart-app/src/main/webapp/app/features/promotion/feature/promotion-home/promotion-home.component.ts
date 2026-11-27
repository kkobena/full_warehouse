import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbModal, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { NgbConfirmDialogService } from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ErrorService } from '../../../../shared/error.service';
import { DevisePipe } from '../../../../shared/utils/devise';
import {
  AppBadgeSeverity,
  AppPillOption,
  BadgeComponent,
  ButtonComponent,
  CardComponent,
  DataTableComponent,
  DetailGridComponent,
  DetailSectionComponent,
  HintComponent,
  IconFieldComponent,
  PillSelectorComponent,
  SelectableRowDirective,
  ToolbarComponent,
} from '../../../../shared/ui';
// Non réexporté par shared/ui : import direct.
import { PageLayoutComponent } from '../../../../shared/ui/page-layout/page-layout.component';
import { PromotionApiService } from '../../data-access/services/promotion-api.service';
import { saveAs } from 'file-saver';
import {
  bilanDisponible,
  ETAT_PROMOTION_LIBELLES,
  EtatPromotion,
  IBilanPromotion,
  IPromotion,
  libelleOffre,
  libelleSeuil,
  variation,
} from '../../models/promotion.model';
import { AbilityService } from 'app/core/auth/ability.service';
import { PromotionFormComponent } from '../../ui/promotion-form/promotion-form.component';
import { PromotionEtiquetteModalComponent } from '../../ui/promotion-etiquette-modal/promotion-etiquette-modal.component';

type FiltreEtat = 'ACTIVES' | 'BROUILLONS' | 'TOUTES';

/**
 * Référentiel > Promotions : les opérations du magasin de l'utilisateur.
 *
 * Une promotion se crée en brouillon, se valide, et s'applique alors seule au comptoir entre ses
 * dates. La liste montre l'état lu sur les dates (à venir, en cours, terminée) ; le détail, le prix
 * de chaque article avant et après promotion.
 */
@Component({
  selector: 'app-promotion-home',
  templateUrl: './promotion-home.component.html',
  styleUrl: './promotion-home.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    NgbTooltip,
    PageLayoutComponent,
    ToolbarComponent,
    ButtonComponent,
    BadgeComponent,
    IconFieldComponent,
    PillSelectorComponent,
    DataTableComponent,
    DetailGridComponent,
    DetailSectionComponent,
    SelectableRowDirective,
    HintComponent,
    CardComponent,
    DatePipe,
    DecimalPipe,
    DevisePipe,
  ],
})
export class PromotionHomeComponent {
  protected readonly filtreOptions: AppPillOption[] = [
    { label: 'En cours et à venir', value: 'ACTIVES' },
    { label: 'Brouillons', value: 'BROUILLONS' },
    { label: 'Toutes', value: 'TOUTES' },
  ];

  protected filtre: FiltreEtat = 'ACTIVES';
  protected readonly filtreActif = signal<FiltreEtat>('ACTIVES');
  /** Saisie brute du champ de recherche ; n'est reportée qu'à la validation. */
  protected saisie = '';
  protected readonly recherche = signal('');

  /**
   * Les droits du rôle sur le module, lus sur le même code que le serveur (`promotions`, action
   * déduite du verbe HTTP) : un bouton absent vaut mieux qu'un refus après coup. Valider et
   * suspendre modifient la promotion ; l'annuler est sa suppression.
   */
  protected readonly peutCreer = inject(AbilityService).canSignal('create', 'promotions');
  protected readonly peutModifier = inject(AbilityService).canSignal('edit', 'promotions');
  protected readonly peutAnnuler = inject(AbilityService).canSignal('delete', 'promotions');
  protected readonly peutExporter = inject(AbilityService).canSignal('export', 'promotions');

  protected readonly resource = inject(PromotionApiService).listResource();
  protected readonly loading = computed(() => this.resource.isLoading());

  protected readonly promotions = computed(() => {
    const terme = this.recherche().toLowerCase();
    return (this.resource.value() ?? [])
      .filter(p => this.correspondAuFiltre(p.etat, this.filtreActif()))
      .filter(p => !terme || p.libelle.toLowerCase().includes(terme) || p.code?.toLowerCase().includes(terme));
  });

  // ── Maître / détail ────────────────────────────────────────────────────────
  protected readonly selection = signal<IPromotion | null>(null);
  protected readonly detail = signal<IPromotion | null>(null);
  protected readonly chargementDetail = signal(false);
  /** Le bilan de la promotion sélectionnée, dès son premier jour ; nul avant. */
  protected readonly bilan = signal<IBilanPromotion | null>(null);
  protected readonly chargementBilan = signal(false);
  protected readonly variation = variation;
  protected readonly panelOpen = computed(() => this.selection() !== null);

  private readonly api = inject(PromotionApiService);
  private readonly modalService = inject(NgbModal);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notification = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  constructor() {
    // Une erreur de chargement doit se voir : sans cela, la table reste vide et l'utilisateur
    // conclut qu'il n'y a pas de promotion.
    effect(() => {
      if (this.resource.error()) {
        this.notification.error('Chargement des promotions impossible.');
      }
    });
  }

  protected libelleEtat(etat: EtatPromotion): string {
    return ETAT_PROMOTION_LIBELLES[etat];
  }

  protected severiteEtat(etat: EtatPromotion): AppBadgeSeverity {
    switch (etat) {
      case 'EN_COURS':
        return 'success';
      case 'A_VENIR':
        return 'info';
      case 'SUSPENDUE':
      case 'EPUISEE':
        return 'warn';
      case 'ANNULEE':
        return 'danger';
      default:
        return 'secondary';
    }
  }

  /** « −20 % », « −500 FCFA / unité », « Prix fixe » : la mécanique en un coup d'œil. */
  protected mecanique(p: IPromotion): string {
    switch (p.type) {
      case 'PROMO_POURCENTAGE':
        return `−${p.valeur ?? 0} %`;
      case 'PROMO_MONTANT':
        return `−${new DevisePipe().transform(p.valeur ?? 0)} / unité`;
      case 'PROMO_PRIX_FIXE':
        return 'Prix fixe';
      case 'PROMO_OFFRE_NM':
        return libelleOffre(p.quantiteMin, p.valeur ?? 1);
      case 'PROMO_PACK':
        return `Pack à ${new DevisePipe().transform(p.valeur ?? 0)}`;
      case 'PROMO_SEUIL_MONTANT': {
        const devise = new DevisePipe();
        return libelleSeuil(p.valeur ?? 0, p.seuilMontant ?? 0, v => devise.transform(v));
      }
    }
  }

  protected conditions(p: IPromotion): string | null {
    const morceaux: string[] = [];
    // Pour une offre N+M, `quantiteMin` porte N : la mécanique le dit déjà.
    if (p.quantiteMin > 1 && p.type !== 'PROMO_OFFRE_NM' && p.type !== 'PROMO_PACK') {
      morceaux.push(`dès ${p.quantiteMin} unités`);
    }
    if (p.quantiteMaxParVente) {
      morceaux.push(`${p.quantiteMaxParVente} max. par vente`);
    }
    if (p.quantiteMaxTotale) {
      morceaux.push(`limite ${p.quantiteMaxTotale} unités`);
    }
    if (p.cumulable) {
      morceaux.push('cumulable avec la remise produit');
    }
    return morceaux.length ? morceaux.join(' · ') : null;
  }

  protected estModifiable(p: IPromotion): boolean {
    return p.statut === 'BROUILLON' || p.statut === 'SUSPENDUE';
  }

  /** Seule une promotion que le comptoir appliquera s'étiquette : le serveur refuse les autres. */
  protected estEtiquetable(p: IPromotion): boolean {
    return p.etat === 'EN_COURS' || p.etat === 'A_VENIR';
  }

  protected estValidable(p: IPromotion): boolean {
    return (p.statut === 'BROUILLON' || p.statut === 'SUSPENDUE') && p.etat !== 'TERMINEE';
  }

  protected onFiltre(): void {
    this.filtreActif.set(this.filtre);
    this.fermerDetail();
  }

  protected onSearch(): void {
    this.recherche.set(this.saisie.trim());
    this.fermerDetail();
  }

  protected onSelection(promotion: IPromotion | null): void {
    this.selection.set(promotion);
    this.detail.set(null);
    this.bilan.set(null);
    if (!promotion) {
      return;
    }
    this.chargerBilan(promotion);
    this.chargementDetail.set(true);
    this.api.find(promotion.id).subscribe({
      next: complete => {
        this.detail.set(complete);
        this.chargementDetail.set(false);
      },
      error: (err: unknown) => {
        this.chargementDetail.set(false);
        this.notification.error(this.errorService.getErrorMessage(err, 'Chargement du détail impossible.'));
      },
    });
  }

  protected fermerDetail(): void {
    this.selection.set(null);
    this.detail.set(null);
    this.bilan.set(null);
  }

  /** Le bilan en CSV, une ligne par article ; le serveur exige le droit d'export. */
  protected exporterBilan(p: IPromotion): void {
    this.api.bilanCsv(p.id).subscribe({
      next: blob => saveAs(blob, `bilan-${p.code}.csv`),
      error: (err: unknown) => this.notification.error(this.errorService.getErrorMessage(err, 'Export impossible.')),
    });
  }

  protected async creer(): Promise<void> {
    await this.ouvrirFormulaire();
  }

  /**
   * Les actions de ligne arrêtent la propagation : sans cela, le clic remonte jusqu'à la ligne
   * sélectionnable et ouvre le détail en même temps qu'il déclenche l'action.
   */
  protected voirDetail(p: IPromotion, evenement: MouseEvent): void {
    evenement.stopPropagation();
    this.onSelection(p);
  }

  protected modifier(p: IPromotion, evenement?: MouseEvent): void {
    evenement?.stopPropagation();
    // Le formulaire a besoin des articles, que la liste ne porte pas.
    this.api.find(p.id).subscribe({
      next: complete => void this.ouvrirFormulaire(complete),
      error: (err: unknown) => this.notification.error(this.errorService.getErrorMessage(err, 'Chargement impossible.')),
    });
  }

  protected imprimerEtiquettes(p: IPromotion, evenement?: MouseEvent): void {
    evenement?.stopPropagation();
    // La modale a besoin des articles et de leurs prix, que la liste ne porte pas.
    this.api.find(p.id).subscribe({
      next: complete => {
        const ref = this.modalService.open(PromotionEtiquetteModalComponent, {
          backdrop: 'static',
          centered: true,
          size: 'xl',
        });
        (ref.componentInstance as PromotionEtiquetteModalComponent).init(complete);
        ref.result.catch((): null => null);
      },
      error: (err: unknown) => this.notification.error(this.errorService.getErrorMessage(err, 'Chargement impossible.')),
    });
  }

  protected valider(p: IPromotion, evenement?: MouseEvent): void {
    evenement?.stopPropagation();
    const alertes = p.alertes ?? [];
    const message = `« ${p.libelle} » s’appliquera automatiquement aux ventes du ${this.dateCourte(p.dateDebut)} au ${this.dateCourte(p.dateFin)}.`;
    // Prix administré, vente à perte : rien n'est refusé, mais la validation se fait en connaissance de cause.
    this.confirmDialog.onConfirm(
      () => this.executer(this.api.valider(p.id), 'Promotion validée : elle s’appliquera au comptoir entre ses dates.'),
      alertes.length ? 'Valider malgré ces signalements ?' : 'Valider la promotion',
      alertes.length ? `${alertes.join(' ')} ${message}` : message,
      alertes.length ? 'pi pi-exclamation-triangle' : 'pi pi-check-circle',
    );
  }

  protected suspendre(p: IPromotion, evenement?: MouseEvent): void {
    evenement?.stopPropagation();
    this.confirmDialog.onConfirm(
      () => this.executer(this.api.suspendre(p.id), 'Promotion suspendue.'),
      'Suspendre la promotion',
      `« ${p.libelle} » cessera de s’appliquer jusqu’à ce que vous la validiez de nouveau.`,
      'pi pi-pause-circle',
    );
  }

  protected annuler(p: IPromotion, evenement?: MouseEvent): void {
    evenement?.stopPropagation();
    this.confirmDialog.onConfirm(
      () => this.executer(this.api.annuler(p.id), 'Promotion annulée.'),
      'Annuler la promotion',
      `L’annulation de « ${p.libelle} » est définitive : pour la relancer, il faudra en créer une nouvelle.`,
      'pi pi-exclamation-triangle',
    );
  }

  private executer(action: Observable<IPromotion>, succes: string): void {
    action.subscribe({
      next: maj => {
        this.notification.success(succes);
        this.resource.reload();
        if (this.selection()?.id === maj.id) {
          this.selection.set(maj);
          this.detail.set(maj);
        }
      },
      // Le serveur explique son refus (transition interdite…) : on relaie SON message.
      error: (err: unknown) => this.notification.error(this.errorService.getErrorMessage(err, 'Action impossible.')),
    });
  }

  private async ouvrirFormulaire(promotion?: IPromotion): Promise<void> {
    const ref = this.modalService.open(PromotionFormComponent, {
      backdrop: 'static',
      centered: true,
      size: 'xl',
      scrollable: true,
    });
    (ref.componentInstance as PromotionFormComponent).init(promotion);
    const resultat = await ref.result.catch((): IPromotion | null => null);
    if (resultat) {
      this.notification.success(promotion ? 'Promotion modifiée.' : 'Promotion créée en brouillon : validez-la pour l’appliquer.');
      // Un article ajouté par la recherche ne disait pas sa TVA : le serveur signale ce qu'il a vu.
      if (resultat.alertes?.length) {
        this.notification.warning(resultat.alertes.join(' '));
      }
      this.resource.reload();
      if (this.selection()?.id === resultat.id) {
        this.selection.set(resultat);
        this.detail.set(resultat);
      }
    }
  }

  private chargerBilan(promotion: IPromotion): void {
    if (!bilanDisponible(promotion, new Date().toLocaleDateString('sv-SE'))) {
      return;
    }
    this.chargementBilan.set(true);
    this.api.bilan(promotion.id).subscribe({
      next: bilan => {
        // La sélection a pu changer pendant le chargement : un bilan ne s'affiche que sous SA promotion.
        if (this.selection()?.id === bilan.promotionId) {
          this.bilan.set(bilan);
        }
        this.chargementBilan.set(false);
      },
      error: (err: unknown) => {
        this.chargementBilan.set(false);
        this.notification.error(this.errorService.getErrorMessage(err, 'Bilan indisponible.'));
      },
    });
  }

  private correspondAuFiltre(etat: EtatPromotion, filtre: FiltreEtat): boolean {
    switch (filtre) {
      case 'ACTIVES':
        return etat === 'EN_COURS' || etat === 'A_VENIR' || etat === 'EPUISEE' || etat === 'SUSPENDUE';
      case 'BROUILLONS':
        return etat === 'BROUILLON';
      default:
        return true;
    }
  }

  private dateCourte(iso: string): string {
    const [annee, mois, jour] = iso.split('-');
    return `${jour}/${mois}/${annee}`;
  }
}
