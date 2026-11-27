import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal, NgbDateStruct, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs/operators';
import {
  BadgeComponent,
  ButtonComponent,
  CardComponent,
  DataTableComponent,
  InputNumberComponent,
  SelectComponent,
  SwitchComponent,
} from '../../../../shared/ui';
import { PharmaDatePickerComponent } from '../../../../shared/date-picker/pharma-date-picker.component';
import { ProduitSearchAutocompleteScannerComponent } from '../../../../shared/produit-search-autocomplete-scanner/produit-search-autocomplete-scanner.component';
import { ProduitSearch } from '../../../../shared/model/produit.model';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ErrorService } from '../../../../shared/error.service';
import { DevisePipe } from '../../../../shared/utils/devise';
import { ISO_TO_NGB_DATE, NGB_DATE_TO_ISO, TODAY_NGB_DATE } from '../../../../shared/util/warehouse-util';
import { PromotionApiService } from '../../data-access/services/promotion-api.service';
import {
  bilanPerimetre,
  estDeLigne,
  IPerimetre,
  IPromotion,
  IPromotionForm,
  prixApresPromotion,
  prixDansLePack,
  TYPE_PROMOTION_OPTIONS,
  TypePromotion,
} from '../../models/promotion.model';

/** Les champs du formulaire ; le périmètre d'articles est tenu à part, dans un signal. */
type PromotionFormGroup = FormGroup<{
  libelle: FormControl<string>;
  type: FormControl<TypePromotion>;
  valeur: FormControl<number | null>;
  dateDebut: FormControl<NgbDateStruct | null>;
  dateFin: FormControl<NgbDateStruct | null>;
  quantiteMin: FormControl<number>;
  quantiteMaxParVente: FormControl<number | null>;
  quantiteMaxTotale: FormControl<number | null>;
  priorite: FormControl<number>;
  seuilMontant: FormControl<number | null>;
  cumulable: FormControl<boolean>;
}>;

function creerFormulaire(fb: NonNullableFormBuilder): PromotionFormGroup {
  return fb.group({
    libelle: fb.control('', [Validators.required, Validators.maxLength(100)]),
    type: fb.control<TypePromotion>('PROMO_POURCENTAGE', Validators.required),
    valeur: fb.control<number | null>(null),
    dateDebut: fb.control<NgbDateStruct | null>(TODAY_NGB_DATE(), Validators.required),
    dateFin: fb.control<NgbDateStruct | null>(null, Validators.required),
    quantiteMin: fb.control<number>(1, [Validators.required, Validators.min(1)]),
    quantiteMaxParVente: fb.control<number | null>(null, Validators.min(1)),
    quantiteMaxTotale: fb.control<number | null>(null, Validators.min(1)),
    priorite: fb.control<number>(0),
    seuilMontant: fb.control<number | null>(null),
    cumulable: fb.control<boolean>(false),
  });
}

/** Un article du périmètre, tel que le formulaire le manipule. */
interface LigneArticle {
  produitId: number;
  libelle: string;
  codeCip?: string | null;
  prix: number;
  /** Connu seulement pour un article ajouté dans cette saisie : le serveur refait le contrôle de toute façon. */
  prixAchat?: number | null;
  prixPromo?: number | null;
  /** Unités dans le pack. */
  quantite: number;
  /** Connu pour un article d'un rayon, d'une famille ou d'une promotion enregistrée ; signalé après l'enregistrement sinon. */
  prixAdministre?: boolean;
}

/** Un rayon ou une famille dans la liste déroulante du périmètre ; la clé distingue les deux. */
interface OptionPerimetre {
  cle: string;
  libelle: string;
  groupe: 'Rayons' | 'Familles';
  critere: { rayonId: number } | { familleId: number };
}

/**
 * Création ou modification d'une promotion, en modale.
 *
 * Le magasin n'est pas demandé : c'est celui de l'utilisateur. Le formulaire montre pour chaque
 * article le prix qu'il aura en promotion, et signale d'avance une vente à perte quand le prix
 * d'achat est connu, et un médicament à prix administré. Ni l'un ni l'autre n'est refusé : c'est au
 * pharmacien d'en décider ; la liste et la validation les signalent de nouveau.
 */
@Component({
  selector: 'app-promotion-form',
  templateUrl: './promotion-form.component.html',
  styleUrl: './promotion-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    NgbTooltip,
    BadgeComponent,
    ButtonComponent,
    CardComponent,
    DataTableComponent,
    InputNumberComponent,
    SelectComponent,
    SwitchComponent,
    PharmaDatePickerComponent,
    ProduitSearchAutocompleteScannerComponent,
    DevisePipe,
  ],
})
export class PromotionFormComponent {
  protected readonly titre = signal('Nouvelle promotion');
  protected readonly typeOptions = TYPE_PROMOTION_OPTIONS;
  protected readonly isSaving = signal(false);
  protected readonly lignes = signal<LigneArticle[]>([]);
  /** Rayons du magasin et familles : un raccourci pour remplir la liste, qui reste seule enregistrée. */
  protected readonly optionsPerimetre = signal<OptionPerimetre[]>([]);
  protected readonly perimetreChoisi = signal<string | null>(null);
  protected readonly chargementPerimetre = signal(false);

  protected readonly editForm = creerFormulaire(inject(NonNullableFormBuilder));

  /** Le type et la valeur, en signaux : l'aperçu des prix en dépend et la détection est OnPush. */
  protected readonly type = signal<TypePromotion>('PROMO_POURCENTAGE');
  protected readonly valeur = signal<number | null>(null);
  protected readonly achetees = signal(1);
  protected readonly estPrixFixe = computed(() => this.type() === 'PROMO_PRIX_FIXE');
  protected readonly estOffre = computed(() => this.type() === 'PROMO_OFFRE_NM');
  protected readonly estPack = computed(() => this.type() === 'PROMO_PACK');
  /** Remise dès un montant d'achat : le seuil remplace « à partir de N unités », et les plafonds en unités n'ont pas de sens. */
  protected readonly estSeuil = computed(() => this.type() === 'PROMO_SEUIL_MONTANT');
  /** Seule une mécanique de ligne se cumule avec la remise produit de la vente. */
  protected readonly peutCumuler = computed(() => estDeLigne(this.type()));
  /** Les articles du pack vendus seuls, chacun à sa quantité. */
  protected readonly prixReferencePack = computed(() => this.lignes().reduce((s, l) => s + l.prix * l.quantite, 0));
  /** Le prix d'achat du pack, si celui de chaque article est connu ; nul sinon (le serveur contrôle). */
  protected readonly prixAchatPack = computed(() =>
    this.lignes().every(l => !!l.prixAchat) ? this.lignes().reduce((s, l) => s + (l.prixAchat ?? 0) * l.quantite, 0) : null,
  );
  protected readonly packAPerte = computed(() => {
    const achat = this.prixAchatPack();
    return this.estPack() && achat !== null && this.lignes().length > 0 && (this.valeur() ?? 0) <= achat;
  });
  /** N+M : `quantiteMin` porte N, `valeur` M. */
  protected readonly tailleGroupe = computed(() => Math.max(1, this.achetees()) + Math.max(1, this.valeur() ?? 1));
  protected readonly libelleValeur = computed(() => {
    switch (this.type()) {
      case 'PROMO_POURCENTAGE':
      case 'PROMO_SEUIL_MONTANT':
        return 'Taux de remise';
      case 'PROMO_OFFRE_NM':
        return 'Unités offertes';
      case 'PROMO_PACK':
        return 'Prix du pack';
      default:
        return 'Remise par unité';
    }
  });

  /** Articles dont le prix en promotion tomberait au niveau du prix d'achat, ou en dessous. */
  protected readonly aPerte = computed(() => this.lignes().filter(l => this.estAPerte(l)));
  protected readonly administres = computed(() => this.lignes().filter(l => l.prixAdministre));
  protected readonly peutEnregistrer = computed(() => this.lignes().length > 0 && !this.isSaving());

  private promotionId: number | null = null;
  private readonly produitSearch = viewChild(ProduitSearchAutocompleteScannerComponent);
  private readonly libelleInput = viewChild.required<ElementRef<HTMLInputElement>>('libelleInput');
  private readonly api = inject(PromotionApiService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly notification = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  constructor() {
    // Le curseur dans le libellé dès l'ouverture : c'est le premier champ à saisir. En
    // modification, le texte est sélectionné, prêt à être remplacé.
    afterNextRender(() => {
      const champ = this.libelleInput().nativeElement;
      champ.focus();
      champ.select();
    });
    const destroyRef = inject(DestroyRef);
    this.editForm.controls.type.valueChanges.pipe(takeUntilDestroyed(destroyRef)).subscribe(type => {
      this.type.set(type);
      this.appliquerReglesValeur(type);
    });
    this.editForm.controls.valeur.valueChanges.pipe(takeUntilDestroyed(destroyRef)).subscribe(v => this.valeur.set(v));
    this.editForm.controls.quantiteMin.valueChanges.pipe(takeUntilDestroyed(destroyRef)).subscribe(q => this.achetees.set(q ?? 1));
    this.appliquerReglesValeur('PROMO_POURCENTAGE');
    this.api
      .choixPerimetre()
      .pipe(takeUntilDestroyed(destroyRef))
      .subscribe(choix =>
        this.optionsPerimetre.set([
          ...choix.rayons.map(r => ({
            cle: `R${r.id}`,
            libelle: `${r.libelle} (${r.articles})`,
            groupe: 'Rayons' as const,
            critere: { rayonId: r.id },
          })),
          ...choix.familles.map(f => ({
            cle: `F${f.id}`,
            libelle: `${f.libelle} (${f.articles})`,
            groupe: 'Familles' as const,
            critere: { familleId: f.id },
          })),
        ]),
      );
  }

  /** Appelé par l'ouvrant après instanciation, avant affichage ; sans argument en création. */
  init(promotion?: IPromotion): void {
    if (!promotion) {
      return;
    }
    this.promotionId = promotion.id;
    this.titre.set(`Modifier la promotion ${promotion.code}`);
    this.editForm.setValue({
      libelle: promotion.libelle,
      type: promotion.type,
      valeur: promotion.valeur ?? null,
      dateDebut: ISO_TO_NGB_DATE(promotion.dateDebut),
      dateFin: ISO_TO_NGB_DATE(promotion.dateFin),
      quantiteMin: promotion.quantiteMin,
      quantiteMaxParVente: promotion.quantiteMaxParVente ?? null,
      quantiteMaxTotale: promotion.quantiteMaxTotale ?? null,
      priorite: promotion.priorite,
      seuilMontant: promotion.seuilMontant ?? null,
      cumulable: promotion.cumulable ?? false,
    });
    this.lignes.set(
      promotion.produits.map(p => ({
        produitId: p.produitId,
        libelle: p.libelle,
        codeCip: p.codeCip,
        prix: p.prixReference,
        prixPromo: p.prixPromo,
        quantite: p.quantite ?? 1,
        prixAdministre: p.prixAdministre,
      })),
    );
  }

  protected prixApres(ligne: LigneArticle): number {
    if (this.estPack()) {
      return prixDansLePack(ligne.prix, this.prixReferencePack(), this.valeur());
    }
    return prixApresPromotion(this.type(), this.valeur(), ligne.prix, ligne.prixPromo, this.achetees());
  }

  protected estAPerte(ligne: LigneArticle): boolean {
    // Un pack se contrôle en entier (`packAPerte`) : un article peut y être vendu sous son prix d'achat.
    return !this.estPack() && !!ligne.prixAchat && this.prixApres(ligne) <= ligne.prixAchat;
  }

  protected ajouterArticle(produit: ProduitSearch | null): void {
    if (!produit) {
      return;
    }
    if (this.lignes().some(l => l.produitId === produit.id)) {
      this.notification.warning(`« ${produit.libelle} » est déjà dans la promotion.`);
    } else {
      this.lignes.update(lignes => [
        ...lignes,
        {
          produitId: produit.id,
          libelle: produit.libelle,
          codeCip: produit.fournisseurProduit?.codeCip,
          prix: produit.regularUnitPrice,
          prixAchat: produit.fournisseurProduit?.prixAchat,
          prixPromo: null,
          quantite: 1,
        },
      ]);
    }
    this.produitSearch()?.reset();
  }

  /** Tous les articles du rayon ou de la famille choisis, sauf ceux déjà présents ; les prix administrés sont signalés. */
  protected ajouterPerimetre(): void {
    const option = this.optionsPerimetre().find(o => o.cle === this.perimetreChoisi());
    if (!option) {
      return;
    }
    this.chargementPerimetre.set(true);
    this.api
      .perimetre(option.critere)
      .pipe(finalize(() => this.chargementPerimetre.set(false)))
      .subscribe({
        next: perimetre => this.fusionner(perimetre),
        error: (err: unknown) => this.notification.error(this.errorService.getErrorMessage(err, 'Articles introuvables.')),
      });
  }

  /** Après un ajout en bloc : on écarte les médicaments à prix administré, si on ne veut pas les promouvoir. */
  protected retirerAdministres(): void {
    this.lignes.update(lignes => lignes.filter(l => !l.prixAdministre));
  }

  /** Après un ajout en bloc : on ne garde que les articles vendus au-dessus de leur prix d'achat. */
  protected retirerAPerte(): void {
    const aPerte = new Set(this.aPerte().map(l => l.produitId));
    this.lignes.update(lignes => lignes.filter(l => !aPerte.has(l.produitId)));
  }

  protected retirerArticle(ligne: LigneArticle): void {
    this.lignes.update(lignes => lignes.filter(l => l.produitId !== ligne.produitId));
  }

  protected changerQuantite(ligne: LigneArticle, quantite: number | null): void {
    const q = Math.max(1, quantite ?? 1);
    this.lignes.update(lignes => lignes.map(l => (l.produitId === ligne.produitId ? { ...l, quantite: q } : l)));
  }

  protected changerPrixPromo(ligne: LigneArticle, prixPromo: number | null): void {
    this.lignes.update(lignes => lignes.map(l => (l.produitId === ligne.produitId ? { ...l, prixPromo } : l)));
  }

  protected save(): void {
    if (this.editForm.invalid || !this.lignes().length) {
      this.editForm.markAllAsTouched();
      return;
    }
    const v = this.editForm.getRawValue();
    const payload: IPromotionForm = {
      libelle: v.libelle.trim(),
      type: v.type,
      valeur: v.type === 'PROMO_PRIX_FIXE' ? null : v.valeur,
      dateDebut: NGB_DATE_TO_ISO(v.dateDebut)!,
      dateFin: NGB_DATE_TO_ISO(v.dateFin)!,
      priorite: v.priorite ?? 0,
      quantiteMin: v.type === 'PROMO_SEUIL_MONTANT' ? 1 : v.quantiteMin,
      quantiteMaxParVente: v.type === 'PROMO_SEUIL_MONTANT' ? null : v.quantiteMaxParVente,
      quantiteMaxTotale: v.type === 'PROMO_SEUIL_MONTANT' ? null : v.quantiteMaxTotale,
      seuilMontant: v.type === 'PROMO_SEUIL_MONTANT' ? v.seuilMontant : null,
      cumulable: estDeLigne(v.type) && v.cumulable,
      produits: this.lignes().map(l => ({
        produitId: l.produitId,
        prixPromo: v.type === 'PROMO_PRIX_FIXE' ? l.prixPromo : null,
        quantite: v.type === 'PROMO_PACK' ? l.quantite : null,
      })),
    };
    this.isSaving.set(true);
    const requete = this.promotionId ? this.api.update(this.promotionId, payload) : this.api.create(payload);
    requete.pipe(finalize(() => this.isSaving.set(false))).subscribe({
      next: promotion => this.activeModal.close(promotion),
      // Le serveur explique son refus (dates, plafonds…) : on relaie SON message.
      error: (err: unknown) => this.notification.error(this.errorService.getErrorMessage(err, 'Enregistrement impossible.')),
    });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  private fusionner(perimetre: IPerimetre): void {
    const presents = new Set(this.lignes().map(l => l.produitId));
    const nouveaux = perimetre.articles.filter(a => !presents.has(a.produitId));
    this.lignes.update(lignes => [
      ...lignes,
      ...nouveaux.map((a): LigneArticle => ({
        produitId: a.produitId,
        libelle: a.libelle,
        codeCip: a.codeCip,
        prix: a.prix,
        prixAchat: a.prixAchat,
        prixPromo: null,
        quantite: 1,
        prixAdministre: a.prixAdministre,
      })),
    ]);
    const bilan = bilanPerimetre(nouveaux.length, perimetre.articles.length - nouveaux.length, perimetre.administres);
    if (nouveaux.length) {
      this.notification.success(bilan);
    } else {
      this.notification.warning(bilan);
    }
    this.perimetreChoisi.set(null);
  }

  /** La valeur est requise sauf pour le prix fixe, qui se saisit article par article. */
  private appliquerReglesValeur(type: TypePromotion): void {
    const seuil = this.editForm.controls.seuilMontant;
    seuil.setValidators(type === 'PROMO_SEUIL_MONTANT' ? [Validators.required, Validators.min(1)] : []);
    seuil.updateValueAndValidity({ emitEvent: false });
    const valeur = this.editForm.controls.valeur;
    if (type === 'PROMO_PRIX_FIXE') {
      valeur.clearValidators();
      valeur.disable({ emitEvent: false });
    } else {
      valeur.setValidators(
        type === 'PROMO_POURCENTAGE' || type === 'PROMO_SEUIL_MONTANT'
          ? [Validators.required, Validators.min(1), Validators.max(99)]
          : [Validators.required, Validators.min(1)],
      );
      valeur.enable({ emitEvent: false });
    }
    valeur.updateValueAndValidity({ emitEvent: false });
  }
}
