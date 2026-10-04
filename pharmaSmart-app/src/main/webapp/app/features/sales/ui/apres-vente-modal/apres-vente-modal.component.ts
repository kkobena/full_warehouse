import { ModalDeplacableDirective } from 'app/shared/utils/modal-deplacable.directive';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { NgbActiveModal, NgbModal, NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { AbilityService } from 'app/core/auth/ability.service';
import { ErrorService } from 'app/shared/error.service';
import { ISales } from 'app/shared/model/sales.model';
import { DeviseDirective } from 'app/shared/utils/devise';
import { ButtonComponent } from '../../../../shared/ui';
import { AutoFocusDirective } from './auto-focus.directive';
import { NavTabsComponent } from 'app/shared/ui/nav-tabs/nav-tabs.component';
import { AvoirClientApiService, IAvoirClientDocument, ModeClotureAvoir } from '../../data-access/services/avoir-client-api.service';
import { ISaleForRetour, RetourClientApiService } from '../../data-access/services/retour-client-api.service';
import { CloturerAvoirModalComponent } from '../cloturer-avoir-modal/cloturer-avoir-modal.component';
import { RetourClientModalComponent } from '../retour-client-modal/retour-client-modal.component';

type Onglet = 'retour' | 'avoir';

/**
 * Après-vente depuis le comptoir : retourner une vente passée ou clôturer un avoir, sans quitter l'écran de vente.
 * Le panier en cours n'est pas touché ; chaque onglet n'apparaît qu'avec le droit qui le protège dans le journal des ventes.
 */
@Component({
  selector: 'app-apres-vente-modal',
  templateUrl: './apres-vente-modal.component.html',
  styleUrl: './apres-vente-modal.component.scss',
  host: {
    '[attr.data-comptoir-mode]': 'mode',
    '[attr.data-comptoir-doc]': "doc ? 'true' : null",
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ModalDeplacableDirective, FormsModule, DecimalPipe, DeviseDirective, ButtonComponent, NgbNavModule, NavTabsComponent, AutoFocusDirective],
})
export class ApresVenteModalComponent {
  /** Accent du type de vente en cours : la fenêtre, rendue hors de la page de vente, ne peut pas l'hériter. */
  mode: 'comptant' | 'assurance' | 'carnet' = 'comptant';
  doc = false;

  readonly activeModal = inject(NgbActiveModal);
  private readonly modalService = inject(NgbModal);
  private readonly retourApi = inject(RetourClientApiService);
  private readonly avoirApi = inject(AvoirClientApiService);
  private readonly errorService = inject(ErrorService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly ability = inject(AbilityService);

  protected readonly canRetour = this.ability.canSignal('execute', 'ventes.retours-client.create');
  protected readonly canCloturer = this.ability.canSignal('execute', 'ventes.avoirs.cloturer');

  protected readonly choix = signal<Onglet | null>(null);
  protected readonly onglet = computed<Onglet>(() => this.choix() ?? (this.canCloturer() ? 'avoir' : 'retour'));

  // --- Retour client
  protected readonly reference = signal('');
  protected readonly vente = signal<ISaleForRetour | null>(null);
  protected readonly erreurVente = signal<string | null>(null);
  /** Numéro cherché et absent : état vide, pas une erreur. */
  protected readonly venteIntrouvable = signal<string | null>(null);
  protected readonly rechercheVente = signal(false);

  // --- Avoirs à clôturer
  protected readonly recherche = signal('');
  protected readonly avoirs = signal<IAvoirClientDocument[]>([]);
  protected readonly chargementAvoirs = signal(false);
  protected readonly erreurAvoirs = signal<string | null>(null);

  constructor() {
    this.chargerAvoirs();
  }

  protected choisir(onglet: Onglet): void {
    this.choix.set(onglet);
  }

  protected chercherVente(): void {
    const ref = this.reference().trim();
    if (!ref) return;
    this.rechercheVente.set(true);
    this.erreurVente.set(null);
    this.venteIntrouvable.set(null);
    this.vente.set(null);
    this.retourApi
      .findSaleByRef(ref)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: vente => {
          this.vente.set(vente);
          this.rechercheVente.set(false);
        },
        error: err => {
          const message = this.errorService.getErrorMessage(err, 'Impossible de chercher la vente.');
          if (/introuvable/i.test(message)) {
            this.venteIntrouvable.set(ref);
          } else {
            this.erreurVente.set(message);
          }
          this.rechercheVente.set(false);
        },
      });
  }

  protected retourner(): void {
    const vente = this.vente();
    if (!vente?.saleId || !vente.saleDate) return;
    const ref = this.modalService.open(RetourClientModalComponent, { centered: true, size: 'xl', backdrop: 'static' });
    ref.componentInstance.sale = { saleId: { id: vente.saleId, saleDate: vente.saleDate } } as ISales;
  }

  protected chargerAvoirs(): void {
    this.chargementAvoirs.set(true);
    this.erreurAvoirs.set(null);
    this.avoirApi
      .queryDocuments({ search: this.recherche().trim() || undefined, statut: 'OUVERT', page: 0, size: 20 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.avoirs.set(res.body ?? []);
          this.chargementAvoirs.set(false);
        },
        error: err => {
          this.erreurAvoirs.set(this.errorService.getErrorMessage(err, 'Impossible de charger les avoirs.'));
          this.chargementAvoirs.set(false);
        },
      });
  }

  /** Ouvre la clôture de l'avoir, le mode déjà choisi pour les deux gestes courants du comptoir. */
  protected cloturer(avoir: IAvoirClientDocument, mode?: ModeClotureAvoir): void {
    const ref = this.modalService.open(CloturerAvoirModalComponent, { centered: true, size: 'lg', backdrop: 'static' });
    ref.componentInstance.document = avoir;
    if (mode) {
      ref.componentInstance.modeInitial = mode;
    }
    ref.result.then(
      (): void => this.chargerAvoirs(),
      (): void => undefined,
    );
  }
}
