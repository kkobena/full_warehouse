import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  input,
  output,
  signal,
  untracked,
  viewChild
} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgbModal, NgbDropdown, NgbDropdownItem, NgbDropdownMenu, NgbDropdownToggle, NgbPopover, NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {
  ButtonComponent,
  CheckboxComponent,
  DataTableComponent,
  EditableCellComponent,
  IconFieldComponent
} from '../../../../shared/ui';
import {IClientTiersPayant, IProduit, IRemise, ISalesLine} from '../../../../shared/model';
import {
  NgbConfirmDialogService
} from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import {NotificationService} from '../../../../shared/services/notification.service';
import {AddPrixFormComponent} from '../../../products/ui/prix-reference/add-prix-form/add-prix-form.component';
import {showCommonModal} from '../../../../entities/sales/selling-home/sale-helper';
import {PanierFocusService} from '../../data-access/services/panier-focus.service';
import {ProduitSignaux, ProduitSignauxApiService} from '../../data-access/services/produit-signaux-api.service';
import {
  formaterDatePeremption,
  formaterMoisPeremption,
  joursAvant,
  ProduitBadgesComponent,
} from '../produit-badges/produit-badges.component';

/** Colonne éditable de la grille : quantité demandée, servie, quantité unique (demandée = servie), prix. */
type ColonneEditable = 'qd' | 'qs' | 'qty' | 'pu';

/** Où remettre le focus quand le panier a été rechargé après une mise à jour. */
interface CibleApresRechargement {
  lineId: number;
  colonne?: ColonneEditable;
}

/** Une ligne du détail de prix : un tiers payant et le taux qui lui est appliqué sur cette ligne. */
export interface DetailTiersPayantLigne {
  nom: string;
  taux: number | null;
  negocie: boolean;
}

/**
 * Composant de présentation : Affichage liste des lignes de vente
 *
 * Responsabilités :
 * - Afficher les lignes de vente dans un tableau
 * - Permettre édition quantité et prix
 * - Permettre suppression, ajout d'une unité et consultation du détail de prix d'une ligne
 * - Navigation et corrections au clavier (mode « correction du panier »)
 *
 * Deux modes d'interaction qui ne se marchent jamais dessus :
 * - SCAN (inchangé) : scanner → quantité → Entrée → le focus repart sur la recherche produit.
 * - CORRECTION : ↑/↓ changent de ligne, Ctrl+↑/↓ valident la cellule en cours et ouvrent la même colonne
 *   sur la ligne voisine, Suppr supprime, +/- ajustent la quantité, Ctrl+D ajoute une unité,
 *   Espace ouvre le détail de prix. Aucune de ces touches n'est Entrée.
 *
 * Pas de logique métier - Composant pur (OnPush)
 */
@Component({
  selector: 'app-product-list',
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.scss'],
  imports: [
    CommonModule,
    FormsModule,
    DataTableComponent,
    ButtonComponent,
    CheckboxComponent,
    ProduitBadgesComponent,
    NgbTooltip,
    NgbPopover,
    NgbDropdown,
    NgbDropdownToggle,
    NgbDropdownMenu,
    NgbDropdownItem,
    IconFieldComponent,
    EditableCellComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductListComponent {

  // Inputs
  salesLines = input.required<ISalesLine[]>();
  isEditable = input(true);
  canEditPrice = input(false);
  selectedLineId = input<number | null>(null);
  saleId = input<number | undefined>(undefined);
  saleType = input<'COMPTANT' | 'ASSURANCE' | 'CARNET'>('COMPTANT');
  remises = input<IRemise[]>([]);
  currentRemise = input<IRemise | null>(null);
  showRemiseSection = input(true);
  /** La pagination n'apparaît que lorsque le panier compte PLUS de lignes que ce seuil (5 par défaut). */
  seuilPagination = input<number>(5);
  /** Tiers payants de la vente : donnent leur nom aux taux négociés du détail de prix. */
  tiersPayants = input<IClientTiersPayant[]>([]);
  // Outputs
  quantityChanged = output<{ line: ISalesLine; newQty: number }>();
  quantityRequestedChanged = output<{ line: ISalesLine; newQty: number }>();
  priceChanged = output<{ line: ISalesLine; newPrice: number }>();
  lineRemoved = output<ISalesLine>();
  lineSelected = output<ISalesLine>();
  discountChanged = output<{ line: ISalesLine; newDiscount: number }>();
  authorizationRequired = output<{ line: ISalesLine; action: 'delete' | 'discount' }>();
  remiseSelected = output<IRemise>();
  removeRemise = output<void>();
  remiseActionCancelled = output<void>();
  /** Case « non remboursé » cochée ou décochée sur une ligne (ventes assurance et carnet). */
  nonRembourseChanged = output<{ line: ISalesLine; nonRembourse: boolean }>();
  /** Un prix négocié vient d'être enregistré pour le produit d'une ligne : la vente est à recalculer. */
  prixNegocieAjoute = output<ISalesLine>();
  // Local state
  filterValue = signal('');
  /** Lignes visibles : le champ de recherche filtre par libellé, sans toucher au panier. */
  protected readonly lignesAffichees = computed(() => {
    const terme = this.filterValue().trim().toLowerCase();
    const lignes = this.salesLines();
    return terme
      ? lignes.filter(l => (l.produitLibelle ?? '').toLowerCase().includes(terme))
      : lignes;
  });
  selectedRemise = signal<IRemise | null>(null);
  isEditMode = signal(false);
  /** Pagination du tableau : suivie ici pour pouvoir changer de page quand la sélection au clavier en sort. */
  protected readonly first = signal(0);
  protected readonly rows = signal(10);
  /** Un petit panier tient à l'écran : la pagination ne s'ajoute que passé le seuil. */
  protected readonly paginationVisible = computed(() => this.salesLines().length > this.seuilPagination());
  /** Remises disponibles pour le popover (exclut la remise courante en mode édition) */
  availableRemises = computed(() => {
    const all = this.remises();
    if (this.isEditMode()) {
      const currentId = this.currentRemise()?.id;
      return all.filter(r => r.id !== currentId);
    }
    return all;
  });
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private editRemisePopover = viewChild<NgbPopover>('editRemisePopover');
  private addRemisePopover = viewChild<NgbPopover>('addRemisePopover');
  private readonly notificationService = inject(NotificationService);
  private readonly panierFocus = inject(PanierFocusService);
  private readonly hote: ElementRef<HTMLElement> = inject(ElementRef);

  /** Cible à retrouver quand le panier est rechargé après une correction au clavier. */
  private cible: CibleApresRechargement | null = null;
  private delaiCible: ReturnType<typeof setTimeout> | null = null;
  /** Dernière quantité demandée, pour signaler qu'elle a été plafonnée par le stock. */
  private demandeEnAttente: { lineId: number; quantite: number } | null = null;

  private readonly signauxApi = inject(ProduitSignauxApiService);
  private readonly modalService = inject(NgbModal);
  /** Signaux (générique, stupéfiant, péremption, stock faible) des produits du panier. */
  protected readonly signaux = signal<Map<number, ProduitSignaux>>(new Map());
  /** Seule la vente assurance distingue lignes remboursées et non remboursées (pas le carnet, pas le comptant). */
  protected readonly nonRembourseDisponible = computed(() => this.saleType() === 'ASSURANCE');
  protected readonly nombreColonnes = computed(() => 6 + (this.nonRembourseDisponible() ? 1 : 0) + (this.isEditable() ? 1 : 0));

  constructor() {
    effect(() => this.chargerSignaux(this.salesLines()));
    // Le panier est rechargé après chaque mise à jour : c'est le signal pour retrouver le focus de la
    // correction en cours et pour dire si le stock a limité la quantité servie.
    effect(() => {
      const lignes = this.salesLines();
      untracked(() => this.apresRechargement(lignes));
    });
  }

  // ===== Édition en ligne (flux de saisie rapide, inchangé) =====

  onQuantityRequestedChange(line: ISalesLine, newQty: string): boolean {
    const qty = Number(newQty);
    if (qty > 0) {
      this.annoncerDemande(line, qty);
      this.quantityRequestedChanged.emit({line, newQty: qty});
      return true;
    }
    return false;
  }

  onQuantitySoldChange(line: ISalesLine, newQty: string): boolean {
    const qty = Number(newQty);
    if (qty >= 0) {
      // Validation: quantitySold ne peut pas dépasser quantityRequested
      if (line.quantityRequested && qty > line.quantityRequested) {
        this.notificationService.error(
          `La quantité servie (${qty}) ne peut pas dépasser la quantité demandée (${line.quantityRequested})`,
          'Erreur',
        );
        return false;
      }
      this.quantityChanged.emit({line, newQty: qty});
      return true;
    }
    return false;
  }

  onPriceChange(line: ISalesLine, newPrice: string): boolean {
    const price = Number(newPrice);
    if (price > 0) {
      this.priceChanged.emit({line, newPrice: price});
      return true;
    }
    return false;
  }

  onRemoveLine(line: ISalesLine): void {
    // Utiliser le modal de confirmation
    this.confirmDialog.onConfirm(
      () => this.authorizationRequired.emit({line, action: 'delete'}),
      'Supprimer Produit',
      `Voulez-vous supprimer ${line.produitLibelle || 'ce produit'} ?`,
      undefined,
      () => {
        //TODO: Action on reject , le champ produitSearch reçoit le focus
      },
    );
  }

  onSelectLine(line: ISalesLine): void {
    this.lineSelected.emit(line);
  }

  /** Clic sur une ligne : sélection, et focus sur la ligne pour que le clavier la pilote ensuite. */
  protected onRowClick(line: ISalesLine, event: Event): void {
    this.onSelectLine(line);
    const cible = event.target as HTMLElement;
    if (!cible.closest('app-editable-cell, app-button, button, input, a, [ngbDropdownMenu], .prix-anchor')) {
      (event.currentTarget as HTMLElement).focus();
    }
  }

  // Méthodes helper pour le template
  isLineSelected(line: ISalesLine): boolean {
    return this.selectedLineId() === line.id;
  }

  /** Quantités demandée et servie identiques : une seule valeur à afficher (95 % des lignes). */
  protected quantitesEgales(line: ISalesLine): boolean {
    return line.quantitySold === line.quantityRequested;
  }

  // ===== Mode correction du panier (clavier) =====

  /**
   * ↑/↓ : changer de ligne. Suppr : supprimer. +/- : une unité de plus ou de moins. Ctrl+D : une unité de
   * plus. Espace : détail de prix. Ctrl+↑/↓ dans une cellule en cours d'édition : la valider et ouvrir la
   * même colonne sur la ligne voisine. Entrée n'est jamais interceptée : elle garde son sens (valider et
   * retourner à la recherche produit).
   */
  protected onGridKeydown(event: KeyboardEvent): void {
    if (!this.isEditable()) {
      return;
    }
    const cible = event.target as HTMLElement;
    const enEdition =
      cible instanceof HTMLInputElement || cible instanceof HTMLTextAreaElement || cible instanceof HTMLSelectElement || cible.isContentEditable;

    if (enEdition) {
      if (event.ctrlKey && (event.key === 'ArrowDown' || event.key === 'ArrowUp') && cible.dataset['col']) {
        event.preventDefault();
        this.naviguerEnEdition(cible as HTMLInputElement, event.key === 'ArrowDown' ? 1 : -1);
      }
      return;
    }
    // Hors champ : seules la grille et ses lignes pilotent le clavier (pas les boutons, menus, popovers).
    if (!cible.matches('tr[data-line-id], .product-list-container') ) {
      return;
    }

    const ligne = this.ligneSelectionnee();
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.deplacerSelection(1);
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.deplacerSelection(-1);
        break;
      case 'Delete':
        if (ligne) {
          event.preventDefault();
          this.onRemoveLine(ligne);
        }
        break;
      case '+':
        if (ligne) {
          event.preventDefault();
          this.ajouterUneUnite(ligne);
        }
        break;
      case '-':
        if (ligne) {
          event.preventDefault();
          this.retirerUneUnite(ligne);
        }
        break;
      case 'd':
      case 'D':
        if (ligne && event.ctrlKey) {
          event.preventDefault(); // Ctrl+D est « marque-page » dans le navigateur
          this.ajouterUneUnite(ligne);
        }
        break;
      case ' ':
        if (ligne) {
          event.preventDefault();
          this.ouvrirDetailPrix(ligne.id);
        }
        break;
      default:
        break;
    }
  }


  /** Tarif négocié (prix de référence ou taux) du produit de la ligne, pour les tiers payants de la vente. */
  protected ajouterPrixNegocie(line: ISalesLine): void {
    showCommonModal(
      this.modalService,
      AddPrixFormComponent,
      {
        isFromProduit: true,
        produit: {id: line.produitId, libelle: line.produitLibelle, codeCip: line.code, regularUnitPrice: line.regularUnitPrice} as IProduit,
        tiersPayantsVente: this.tiersPayants(),
        entity: null,
      },
      () => this.prixNegocieAjoute.emit(line),
      'lg',
    );
  }

  protected basculerNonRembourse(line: ISalesLine, nonRembourse: boolean): void {
    this.nonRembourseChanged.emit({line, nonRembourse});
  }

  /** Sous-titre des lots prélevés : les deux premiers, puis « +N » ; le détail complet est dans l'infobulle. */
  protected lotsAffiches(line: ISalesLine): {
    visibles: { numLot: string; quantity: number; mois: string | null; urgence: 'perime' | 'proche' | null }[];
    reste: number;
    plusieurs: boolean;
    infobulle: string;
  } {
    const limite = this.signaux().get(line.produitId ?? -1)?.dateLimitePeremption;
    const lots = (line.lots ?? []).map(lot => {
      const date = lot.expiryDate ?? null;
      const urgence = !date ? null : joursAvant(date) < 0 ? ('perime' as const) : limite && date <= limite ? ('proche' as const) : null;
      return {numLot: lot.numLot, quantity: lot.quantity, mois: date ? formaterMoisPeremption(date) : null, urgence, date};
    });
    return {
      visibles: lots.slice(0, 2),
      reste: Math.max(lots.length - 2, 0),
      plusieurs: lots.length > 1,
      infobulle: lots.map(l => `Lot ${l.numLot} × ${l.quantity}${l.date ? ` — exp. ${formaterDatePeremption(l.date)}` : ''}`).join('\n'),
    };
  }

  /** Rechargés à chaque évolution du panier : le stock restant bouge à chaque vente. */
  private chargerSignaux(lignes: ISalesLine[]): void {
    const ids = [...new Set(lignes.map(l => l.produitId).filter((id): id is number => !!id))];
    if (ids.length === 0) {
      return;
    }
    this.signauxApi.chargerSignaux(ids).subscribe({
      next: recus => this.signaux.update(map => new Map([...map, ...recus.map(s => [s.produitId, s] as const)])),
      error: () => undefined, // repères d'aide : leur absence ne doit pas gêner la vente
    });
  }

  protected ajouterUneUnite(line: ISalesLine): void {
    const nouvelle = (line.quantityRequested ?? line.quantitySold ?? 0) + 1;
    this.corrigerQuantite(line, nouvelle);
  }

  protected retirerUneUnite(line: ISalesLine): void {
    const actuelle = line.quantityRequested ?? line.quantitySold ?? 0;
    if (actuelle <= 1) {
      this.notificationService.info('Pour retirer la ligne, utilisez Suppr.', 'Quantité minimale');
      return;
    }
    this.corrigerQuantite(line, actuelle - 1);
  }

  private corrigerQuantite(line: ISalesLine, quantite: number): void {
    this.panierFocus.demanderMaintien();
    this.programmerCible({lineId: line.id});
    this.annoncerDemande(line, quantite);
    this.quantityRequestedChanged.emit({line, newQty: quantite});
  }

  private ligneSelectionnee(): ISalesLine | undefined {
    return this.salesLines().find(l => l.id === this.selectedLineId());
  }

  private deplacerSelection(delta: 1 | -1): void {
    const lignes = this.salesLines();
    if (lignes.length === 0) {
      return;
    }
    const courant = lignes.findIndex(l => l.id === this.selectedLineId());
    const suivant = courant < 0 ? (delta === 1 ? 0 : lignes.length - 1) : Math.min(lignes.length - 1, Math.max(0, courant + delta));
    this.selectionnerEtFocaliser(lignes[suivant]);
  }

  private selectionnerEtFocaliser(ligne: ISalesLine): void {
    this.assurerPage(ligne);
    this.lineSelected.emit(ligne);
    setTimeout(() => this.focaliserLigne(ligne.id), 30);
  }

  /** Ctrl+↑/↓ dans une cellule : valide la valeur saisie (si elle a changé) puis ouvre la même colonne ailleurs. */
  private naviguerEnEdition(champ: HTMLInputElement, delta: 1 | -1): void {
    const colonne = champ.dataset['col'] as ColonneEditable;
    const lineId = Number(champ.closest('tr')?.dataset['lineId']);
    const lignes = this.salesLines();
    const index = lignes.findIndex(l => l.id === lineId);
    const voisine = lignes[index + delta];
    if (index < 0 || !voisine) {
      return;
    }
    const ligne = lignes[index];
    if (!this.valeurChangee(ligne, colonne, champ.value)) {
      this.retrouverCible({lineId: voisine.id, colonne});
      return;
    }
    // La validation peut être refusée (valeur invalide) : on ne bouge alors pas, l'erreur est affichée.
    this.panierFocus.demanderMaintien();
    if (this.validerColonne(ligne, colonne, champ.value)) {
      this.programmerCible({lineId: voisine.id, colonne});
    } else {
      this.panierFocus.consommer();
    }
  }

  private valeurChangee(ligne: ISalesLine, colonne: ColonneEditable, valeur: string): boolean {
    const nombre = Number(valeur);
    switch (colonne) {
      case 'qs':
        return nombre !== ligne.quantitySold;
      case 'pu':
        return nombre !== ligne.regularUnitPrice;
      default:
        return nombre !== ligne.quantityRequested;
    }
  }

  private validerColonne(ligne: ISalesLine, colonne: ColonneEditable, valeur: string): boolean {
    switch (colonne) {
      case 'qs':
        return this.onQuantitySoldChange(ligne, valeur);
      case 'pu':
        return this.onPriceChange(ligne, valeur);
      default:
        return this.onQuantityRequestedChange(ligne, valeur);
    }
  }

  // ===== Retour du focus après une mise à jour =====

  private programmerCible(cible: CibleApresRechargement): void {
    this.cible = cible;
    if (this.delaiCible) {
      clearTimeout(this.delaiCible);
    }
    // Si le panier n'est pas rechargé (mise à jour refusée, sans changement), on retrouve quand même la cible.
    this.delaiCible = setTimeout(() => this.appliquerCible(), 1500);
  }

  private apresRechargement(lignes: ISalesLine[]): void {
    // Sans pagination, ou avec une seule page, la page affichée ne peut être que la première.
    if (lignes.length <= Math.max(this.rows(), this.seuilPagination()) && this.first() !== 0) {
      this.first.set(0);
    }
    const attendue = this.demandeEnAttente;
    if (attendue) {
      const ligne = lignes.find(l => l.id === attendue.lineId);
      if (ligne && ligne.quantitySold != null) {
        this.demandeEnAttente = null;
        if (ligne.quantitySold < attendue.quantite) {
          this.notificationService.warning(
            `Quantité limitée par le stock : ${ligne.quantitySold} servie(s) sur ${attendue.quantite} demandée(s).`,
            ligne.produitLibelle ?? 'Stock insuffisant',
          );
        }
      }
    }
    if (this.cible) {
      setTimeout(() => this.appliquerCible());
    }
  }

  private appliquerCible(): void {
    const cible = this.cible;
    this.cible = null;
    if (this.delaiCible) {
      clearTimeout(this.delaiCible);
      this.delaiCible = null;
    }
    if (cible) {
      this.retrouverCible(cible);
    }
  }

  private retrouverCible(cible: CibleApresRechargement): void {
    const ligne = this.salesLines().find(l => l.id === cible.lineId);
    if (!ligne) {
      return;
    }
    this.assurerPage(ligne);
    this.lineSelected.emit(ligne);
    setTimeout(() => (cible.colonne ? this.ouvrirCellule(ligne.id, cible.colonne) : this.focaliserLigne(ligne.id)), 50);
  }

  private annoncerDemande(line: ISalesLine, quantite: number): void {
    if (line.id != null) {
      this.demandeEnAttente = {lineId: line.id, quantite};
    }
  }

  /** Change de page si la ligne n'est pas sur la page affichée. */
  private assurerPage(ligne: ISalesLine): void {
    const index = this.salesLines().findIndex(l => l.id === ligne.id);
    const taille = this.rows();
    if (index >= 0 && taille > 0) {
      const debutPage = Math.floor(index / taille) * taille;
      if (debutPage !== this.first()) {
        this.first.set(debutPage);
      }
    }
  }

  private focaliserLigne(lineId: number): void {
    this.hote.nativeElement.querySelector<HTMLElement>(`tr[data-line-id="${lineId}"]`)?.focus();
  }

  /** Ouvre la cellule demandée ; à défaut (quantités égales ou non), la colonne de quantité qui existe sur cette ligne. */
  private ouvrirCellule(lineId: number, colonne: ColonneEditable): void {
    const ligne = this.hote.nativeElement.querySelector<HTMLElement>(`tr[data-line-id="${lineId}"]`);
    if (!ligne) {
      return;
    }
    const equivalentes: ColonneEditable[] = colonne === 'pu' ? ['pu'] : [colonne, 'qty', 'qs', 'qd'];
    for (const candidate of equivalentes) {
      const cellule = ligne.querySelector<HTMLElement>(`app-editable-cell[data-col="${candidate}"]`);
      if (cellule) {
        cellule.click();
        return;
      }
    }
    ligne.focus();
  }

  // ===== Détail de prix (prix négocié par tiers payant) =====

  /** Ouvre le détail de prix de la ligne (ancre invisible ou sous-titre « nég. » du PU). */
  protected ouvrirDetailPrix(lineId: number): void {
    this.hote.nativeElement.querySelector<HTMLElement>(`tr[data-line-id="${lineId}"] .prix-anchor`)?.click();
  }

  /**
   * Prix de référence négocié, quand il diffère du prix public : c'est la base de calcul de la part
   * tiers payant (le serveur la retient déjà sur la ligne). Nul en vente comptant.
   */
  protected prixNegocie(line: ISalesLine): number | null {
    if (this.saleType() === 'COMPTANT') {
      return null;
    }
    const base = line.calculationBasePrice;
    return base != null && base > 0 && base !== line.regularUnitPrice ? base : null;
  }

  protected aDesTauxNegocies(line: ISalesLine): boolean {
    return this.saleType() !== 'COMPTANT' && (line.rates?.length ?? 0) > 0;
  }

  /** Les tiers payants de la vente, par priorité, avec le taux réellement appliqué à cette ligne. */
  protected detailTiersPayants(line: ISalesLine): DetailTiersPayantLigne[] {
    return [...this.tiersPayants()]
      .sort((a, b) => (a.priorite ?? 0) - (b.priorite ?? 0))
      .map(tp => {
        const negocie = line.rates?.find(r => r.compteTiersPayantId === tp.id);
        return {
          nom: tp.tiersPayantName ?? tp.tiersPayantFullName ?? 'Tiers payant',
          taux: negocie ? Math.round(negocie.rate * 100) : (tp.taux ?? null),
          negocie: !!negocie,
        };
      });
  }

  // Méthodes pour le footer
  getTotalQuantityRequested(): number {
    return this.salesLines().reduce((sum, line) => sum + (line.quantityRequested || 0), 0);
  }

  getTotalQuantitySold(): number {
    return this.salesLines().reduce((sum, line) => sum + (line.quantitySold || 0), 0);
  }

  getTotalAmount(): number {
    return this.salesLines().reduce((sum, line) => sum + line.salesAmount, 0);
  }

  onRemoveRemise(): void {
    this.selectedRemise.set(null);
    this.removeRemise.emit();
  }

  /** Ouvre le popover en mode ajout */
  openAddRemisePopover(): void {
    this.isEditMode.set(false);
  }

  /** Ouvre le popover en mode édition (exclut la remise courante) */
  openEditRemisePopover(): void {
    this.isEditMode.set(true);
  }

  /** Sélection d'une remise depuis le popover */
  onPopoverRemiseSelect(remise: IRemise): void {
    this.editRemisePopover()?.close();
    this.addRemisePopover()?.close();
    if (this.isEditMode()) {
      this.confirmDialog.onConfirm(
        () => this.remiseSelected.emit(remise),
        'Modifier la remise',
        `Voulez-vous remplacer la remise actuelle par "${remise.valeur}" ?`,
        undefined,
        () => this.remiseActionCancelled.emit(),
      );
    } else {
      this.remiseSelected.emit(remise);
    }
  }

  /**
   * Popover de remise au clavier : ↑/↓ parcourent les remises, Entrée ou Espace choisit celle qui a le
   * focus, Échap referme (géré par le popover). Les éléments portent `tabindex="0"`.
   */
  protected onRemiseKeydown(event: KeyboardEvent, remise: IRemise): void {
    const liste = (event.currentTarget as HTMLElement).parentElement;
    const items = Array.from(liste?.querySelectorAll<HTMLElement>('.remise-popover-item') ?? []);
    const index = items.indexOf(event.currentTarget as HTMLElement);
    switch (event.key) {
      case 'Enter':
      case ' ':
        event.preventDefault();
        this.onPopoverRemiseSelect(remise);
        break;
      case 'ArrowDown':
        event.preventDefault();
        items[Math.min(items.length - 1, index + 1)]?.focus();
        break;
      case 'ArrowUp':
        event.preventDefault();
        items[Math.max(0, index - 1)]?.focus();
        break;
      default:
        break;
    }
  }

  /** À l'ouverture du popover de remise, le focus va sur la première remise : tout se fait ensuite au clavier. */
  protected focaliserPremiereRemise(): void {
    setTimeout(() => document.querySelector<HTMLElement>('.remise-popover-item')?.focus(), 0);
  }

  /** Taux affiché selon le type de vente */
  getRemiseRate(remise: IRemise): string {
    const rate = this.saleType() === 'COMPTANT' ? remise.vnoDiscountRate : remise.voDiscountRate;
    return rate != null ? rate + ' %' : '';
  }

  getRemiseTaux(): string {
    const remise = this.currentRemise();
    if (remise) {
      return this.getRemiseRate(remise);
    }
    return '';
  }

  // Filtrage
  onFilterChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.filterValue.set(value);
  }
}
