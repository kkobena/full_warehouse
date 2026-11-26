import { afterNextRender, Component, computed, DestroyRef, ElementRef, inject, OnInit, Signal, signal, viewChild, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule } from "@angular/forms";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { AppPillOption, ButtonComponent, CardComponent, InputNumberComponent, PillSelectorComponent, SelectComponent } from "app/shared/ui";
import { takeUntilDestroyed, toSignal } from "@angular/core/rxjs-interop";
import { IProduit } from "app/shared/model/produit.model";
import { IEtiquettePreparation, IFormatEtiquette, ILotProduit, ProductsApiService } from "../../data-access/services/products-api.service";
import { NgbDateStruct } from "@ng-bootstrap/ng-bootstrap";
import { PharmaDatePickerComponent } from "app/shared/date-picker/pharma-date-picker.component";
import { NGB_DATE_TO_ISO } from "app/shared/util/warehouse-util";
import { Observable } from "rxjs";
import { NotificationService } from "app/shared/services/notification.service";
import { PrinterInfo, TauriPrinterService } from "app/shared/services/tauri-printer.service";
import { BlobDownloadService } from "../../../../shared/services/blob-download.service";
import { DevisePipe } from 'app/shared/utils/devise';

/** Les types d'étiquette proposés à la génération depuis la fiche produit. */
type TypeEtiquette = "STANDARD" | "LOT" | "REGLETTE" | "PREPARATION";

/** Un rouleau n'a pas de planche à remplir : son seul plafond est le bon sens. */
const MAX_ETIQUETTES_ROULEAU = 500;

/**
 * Chemin d'impression d'un rouleau : le PDF passe par le pilote Windows (Dymo, Brother), le ZPL
 * part en direct dans l'imprimante (Zebra, TSC, Godex), qui trace elle-même les symboles.
 */
type ModeImpression = "PDF" | "ZPL";

/** Un lot présenté dans la liste déroulante : numéro et péremption. */
interface OptionLot {
  id: number;
  libelle: string;
}

@Component({
  selector: "app-produit-etiquette-modal",
  templateUrl: "./produit-etiquette-modal.component.html",
  styleUrls: ["./produit-etiquette-modal.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonComponent,
    InputNumberComponent,
    CardComponent,
    DevisePipe,
    PillSelectorComponent,
    SelectComponent,
    PharmaDatePickerComponent,
  ]
})
export class ProduitEtiquetteModalComponent implements OnInit {
  produit!: IProduit;

  /**
   * La préparation magistrale n'est pas conservée : l'étiquette est une sortie, pas un
   * enregistrement — le registre des préparations est un autre chantier.
   */
  protected readonly form = inject(NonNullableFormBuilder).group({
    typeEtiquette: ["STANDARD" as TypeEtiquette],
    lotId: [null as number | null],
    formatCode: [null as string | null],
    modeImpression: ["PDF" as ModeImpression],
    imprimante: [null as string | null],
    resolution: ["DPI_203"],
    qty: [1 as number | null],
    startAt: [1 as number | null],
    preparation: inject(NonNullableFormBuilder).group({
      libelle: "",
      composition: "",
      patient: "",
      prescripteur: "",
      posologie: "",
      mention: "",
      datePreparation: [null as NgbDateStruct | null],
      peremption: [null as NgbDateStruct | null],
    }),
  });

  protected loading = signal(false);

  protected readonly formats = signal<IFormatEtiquette[]>([]);
  // Les choix dont dépendent des `computed` sont relus en signaux : un contrôle n'en est pas un.
  protected readonly formatCode = this.suivre(this.form.controls.formatCode);

  protected readonly typeEtiquette = this.suivre(this.form.controls.typeEtiquette);
  protected readonly lots = signal<OptionLot[]>([]);
  protected readonly lotId = this.suivre(this.form.controls.lotId);
  protected readonly etatLots = signal<"VIERGE" | "CHARGEMENT" | "PRET">("VIERGE");

  /** Planche choisie, d'ou vient la borne de la case de depart. */
  protected readonly formatChoisi = computed(() => this.formats().find(f => f.code === this.formatCode()) ?? null);

  /** Nombre d'etiquettes de la planche. 65 pour le format historique, pas 64. */
  protected readonly capacite = computed(() => this.formatChoisi()?.capacite ?? 65);

  /** Rouleau : une etiquette par page, donc ni planche entamee ni case de depart a afficher. */
  protected readonly estRouleau = computed(() => this.formatChoisi()?.support === "ROULEAU");

  /** Sur une planche, on ne depasse pas la planche ; sur un rouleau, on deroule. */
  protected readonly qtyMax = computed(() => (this.estRouleau() ? MAX_ETIQUETTES_ROULEAU : this.capacite()));

  protected readonly modeImpression = this.suivre(this.form.controls.modeImpression);
  protected readonly imprimantes = signal<PrinterInfo[]>([]);

  /** Le ZPL n'a de sens qu'en rouleau, et le poste doit pouvoir parler a l'imprimante. */
  protected readonly zplPossible = computed(() => this.estRouleau() && this.tauriPrinter.isRunningInTauri());

  protected readonly estZpl = computed(() => this.zplPossible() && this.modeImpression() === "ZPL");

  protected readonly modeOptions: AppPillOption[] = [
    { label: "PDF (pilote)", value: "PDF", icon: "pi pi-file-pdf" },
    { label: "ZPL (Zebra, TSC)", value: "ZPL", icon: "pi pi-print" },
  ];

  protected readonly resolutions = [
    { code: "DPI_203", libelle: "203 dpi — entrée de gamme" },
    { code: "DPI_300", libelle: "300 dpi" },
    { code: "DPI_600", libelle: "600 dpi" },
  ];

  protected readonly typeOptions: AppPillOption[] = [
    { label: "Standard", value: "STANDARD", icon: "pi pi-tag" },
    { label: "Lot (DataMatrix)", value: "LOT", icon: "pi pi-qrcode" },
    { label: "Réglette", value: "REGLETTE", icon: "pi pi-bars" },
    { label: "Préparation", value: "PREPARATION", icon: "pi pi-flask" },
  ];

  private readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(ProductsApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  // #qtyInput porte sur <app-input-number> : sans `read: ElementRef`, viewChild rend l'instance du
  // composant, dont nativeElement est undefined.
  private readonly qtyInput = viewChild.required<unknown, ElementRef<HTMLElement>>("qtyInput", { read: ElementRef });
  private readonly blobDocumentService = inject(BlobDownloadService);
  private readonly tauriPrinter = inject(TauriPrinterService);

  constructor() {
    // Un formulaire réactif écrit la valeur pendant le rendu : elle est là quand on la sélectionne.
    afterNextRender(() => {
      const el = this.qtyInput().nativeElement.querySelector("input");
      el?.focus();
      el?.select();
    });
  }

  ngOnInit(): void {
    this.chargerFormats();
  }

  private get qty(): number {
    return this.form.controls.qty.value ?? 0;
  }

  private get startAt(): number {
    return this.form.controls.startAt.value ?? 0;
  }

  private suivre<T>(controle: FormControl<T>): Signal<T> {
    return toSignal(controle.valueChanges, { initialValue: controle.value });
  }

  protected onPrint(): void {
    if (!this.peutImprimer()) return;
    this.loading.set(true);
    if (this.estZpl()) {
      this.imprimerEnZpl();
      return;
    }
    this.demanderPdf(this.formatCode() ?? undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob) => {
          this.loading.set(false);
          this.blobDocumentService.downloadPdf(blob, this.nomDuFichier());
          this.activeModal.close(true);
        },
        error: () => {
          this.notificationService.error("Erreur lors de la génération des étiquettes");
          this.loading.set(false);
        }
      });
  }

  /** Le type « lot » exige un lot : sans lui, la trame GS1 n'aurait rien à porter. */
  protected peutImprimer(): boolean {
    if (this.qty < 1 || this.qty > this.qtyMax()) return false;
    if (!this.estRouleau() && (this.startAt < 1 || this.startAt > this.capacite())) return false;
    if (this.typeEtiquette() === "LOT") return this.lotId() !== null;
    // Une préparation sans composition n'est pas une étiquette de préparation.
    if (this.typeEtiquette() === "PREPARATION") {
      const { libelle, composition } = this.form.controls.preparation.getRawValue();
      return !!libelle.trim() && !!composition.trim();
    }
    return true;
  }

  /** Le flux part dans l'imprimante : rien à télécharger, rien à ouvrir. */
  private imprimerEnZpl(): void {
    this.demanderZpl(this.formatCode() ?? undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: zpl => {
          this.tauriPrinter
            .printZpl(zpl, this.form.controls.imprimante.value ?? undefined)
            .then(() => {
              this.loading.set(false);
              this.activeModal.close(true);
            })
            .catch(() => {
              this.notificationService.error("L'imprimante d'étiquettes n'a pas accepté le flux");
              this.loading.set(false);
            });
        },
        error: () => {
          this.notificationService.error("Erreur lors de la génération des étiquettes");
          this.loading.set(false);
        }
      });
  }

  /** Chaque type a son endpoint : le contenu de l'étiquette n'est pas le même. */
  private demanderPdf(format?: string): Observable<Blob> {
    const id = this.produit.id!;
    switch (this.typeEtiquette()) {
      case "LOT":
        return this.api.getEtiquettesLot(id, this.lotId()!, this.qty, this.startAt, format);
      case "REGLETTE":
        return this.api.getReglettes(id, this.qty, this.startAt, format);
      case "PREPARATION":
        return this.api.imprimerPreparation(this.saisiePreparation(), this.qty, this.startAt, format);
      default:
        return this.api.getEtiquettes(id, this.qty, this.startAt, format);
    }
  }

  private demanderZpl(format?: string): Observable<string> {
    const id = this.produit.id!;
    switch (this.typeEtiquette()) {
      case "LOT":
        return this.api.getEtiquettesLotZpl(id, this.lotId()!, this.qty, format, this.form.controls.resolution.value);
      case "REGLETTE":
        return this.api.getReglettesZpl(id, this.qty, format, this.form.controls.resolution.value);
      case "PREPARATION":
        return this.api.imprimerPreparationZpl(this.saisiePreparation(), this.qty, format, this.form.controls.resolution.value);
      default:
        return this.api.getEtiquettesZpl(id, this.qty, format, this.form.controls.resolution.value);
    }
  }

  private saisiePreparation(): IEtiquettePreparation {
    const p = this.form.controls.preparation.getRawValue();
    return {
      libelle: p.libelle,
      composition: p.composition,
      patient: p.patient || null,
      prescripteur: p.prescripteur || null,
      posologie: p.posologie || null,
      mention: p.mention || null,
      datePreparation: p.datePreparation ? NGB_DATE_TO_ISO(p.datePreparation) : null,
      peremption: p.peremption ? NGB_DATE_TO_ISO(p.peremption) : null,
    };
  }

  protected onModeChange(mode: unknown): void {
    this.form.controls.modeImpression.setValue(mode as ModeImpression);
    if (this.modeImpression() === "ZPL" && !this.imprimantes().length) {
      this.chargerImprimantes();
    }
  }

  private chargerImprimantes(): void {
    void this.tauriPrinter.getAvailablePrinters().then(imprimantes => {
      this.imprimantes.set(imprimantes);
      this.form.controls.imprimante.setValue(imprimantes.find(i => i.isDefault)?.name ?? imprimantes[0]?.name ?? null);
    });
  }

  protected onFormatChange(code: unknown): void {
    this.form.controls.formatCode.setValue(code as string | null);
    // Une planche s'imprime en PDF : le ZPL ne vaut que pour un rouleau.
    if (!this.estRouleau()) {
      this.form.controls.modeImpression.setValue("PDF");
    }
    // Un rouleau n'a pas de planche entamee a reprendre, et une planche plus petite peut
    // rendre la case de depart hors bornes.
    if (this.estRouleau() || this.startAt > this.capacite()) {
      this.form.controls.startAt.setValue(1);
    }
  }

  protected onTypeChange(type: unknown): void {
    this.form.controls.typeEtiquette.setValue(type as TypeEtiquette);
    // Le pot porte le nom de l'article d'où l'impression est lancée, sauf à le remplacer.
    const libelle = this.form.controls.preparation.controls.libelle;
    if (this.typeEtiquette() === "PREPARATION" && !libelle.value) {
      libelle.setValue(this.produit.libelle ?? "");
    }
    // Les lots ne se chargent qu'à la demande : la modale s'ouvre sur le type standard, et
    // `produit` n'est renseigné par l'appelant qu'après la construction du composant.
    if (this.typeEtiquette() === "LOT" && this.etatLots() === "VIERGE") {
      this.chargerLots();
    }
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  private nomDuFichier(): string {
    const reference = this.produit.codeCip ?? this.produit.id;
    switch (this.typeEtiquette()) {
      case "LOT":
        return `etiquettes-lot-${reference}`;
      case "REGLETTE":
        return `reglettes-${reference}`;
      case "PREPARATION":
        return "preparation";
      default:
        return `etiquettes-${reference}`;
    }
  }

  private chargerFormats(): void {
    this.api.listerFormatsEtiquette()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: formats => {
          this.formats.set(formats);
          this.form.controls.formatCode.setValue((formats.find(f => f.parDefaut) ?? formats[0])?.code ?? null);
        },
        error: () => this.formats.set([]),
      });
  }

  private chargerLots(): void {
    if (!this.produit.id) return;
    this.etatLots.set("CHARGEMENT");
    this.api.getLots(this.produit.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: lots => {
          this.lots.set(lots.filter(lot => lot.id).map(lot => this.versOption(lot)));
          if (this.lots().length === 1) {
            this.form.controls.lotId.setValue(this.lots()[0].id);
          }
          this.etatLots.set("PRET");
        },
        error: () => {
          this.lots.set([]);
          this.etatLots.set("PRET");
        },
      });
  }

  private versOption(lot: ILotProduit): OptionLot {
    const peremption = lot.expiryDate ? new Date(lot.expiryDate).toLocaleDateString("fr-FR", { month: "2-digit", year: "numeric" }) : null;
    const quantite = lot.currentQuantity ?? 0;
    return {
      id: lot.id!,
      libelle: `${lot.numLot ?? "—"}${peremption ? ` — exp. ${peremption}` : ""} (${quantite} en stock)`,
    };
  }
}
