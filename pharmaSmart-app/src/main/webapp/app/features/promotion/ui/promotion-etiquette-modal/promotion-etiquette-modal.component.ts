import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, Signal, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { AppPillOption, ButtonComponent, CardComponent, InputNumberComponent, PillSelectorComponent, SelectComponent } from 'app/shared/ui';
import { NotificationService } from 'app/shared/services/notification.service';
import { PrinterInfo, TauriPrinterService } from 'app/shared/services/tauri-printer.service';
import { BlobDownloadService } from 'app/shared/services/blob-download.service';
import { DevisePipe } from 'app/shared/utils/devise';
// Le catalogue des formats est celui de toutes les étiquettes : on ne le recopie pas.
import { IFormatEtiquette, ProductsApiService } from '../../../products/data-access/services/products-api.service';
import { PromotionApiService } from '../../data-access/services/promotion-api.service';
import { IPromotion } from '../../models/promotion.model';

/** Un rouleau n'a pas de planche à remplir : son seul plafond est le bon sens. */
const MAX_PAR_ARTICLE_ROULEAU = 100;

type ModeImpression = 'PDF' | 'ZPL';

/**
 * Étiquettes de rayon d'une promotion : une étiquette « PROMO » par article remisé, avec le prix
 * barré, le prix remisé et la date de fin. Mêmes formats et mêmes chemins d'impression (PDF par le
 * pilote, ZPL en direct) que la fiche produit.
 */
@Component({
  selector: 'app-promotion-etiquette-modal',
  templateUrl: './promotion-etiquette-modal.component.html',
  styleUrl: './promotion-etiquette-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, ButtonComponent, CardComponent, InputNumberComponent, PillSelectorComponent, SelectComponent, DevisePipe],
})
export class PromotionEtiquetteModalComponent implements OnInit {
  protected readonly promotion = signal<IPromotion | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    formatCode: [null as string | null],
    modeImpression: ['PDF' as ModeImpression],
    imprimante: [null as string | null],
    resolution: ['DPI_203'],
    qty: [1 as number | null],
    startAt: [1 as number | null],
  });

  protected readonly loading = signal(false);
  protected readonly formats = signal<IFormatEtiquette[]>([]);
  protected readonly imprimantes = signal<PrinterInfo[]>([]);

  protected readonly formatCode = this.suivre(this.form.controls.formatCode);
  protected readonly modeImpression = this.suivre(this.form.controls.modeImpression);
  protected readonly qty = this.suivre(this.form.controls.qty);
  protected readonly startAt = this.suivre(this.form.controls.startAt);

  /** Seuls les articles que la promotion remise reçoivent une étiquette : le serveur fait le même tri. */
  protected readonly articles = computed(() => (this.promotion()?.produits ?? []).filter(p => p.prixApresPromo < p.prixReference));
  /** Un pack s'étiquette sur son emballage : une étiquette par pack, pas par article. */
  protected readonly estPack = computed(() => this.promotion()?.type === 'PROMO_PACK');
  protected readonly total = computed(() => (this.estPack() ? 1 : this.articles().length) * (this.qty() ?? 0));

  protected readonly formatChoisi = computed(() => this.formats().find(f => f.code === this.formatCode()) ?? null);
  protected readonly capacite = computed(() => this.formatChoisi()?.capacite ?? 65);
  protected readonly estRouleau = computed(() => this.formatChoisi()?.support === 'ROULEAU');
  /** Plusieurs articles : la planche n'est pas une borne, l'impression continue sur la suivante. */
  protected readonly qtyMax = computed(() => (this.estRouleau() ? MAX_PAR_ARTICLE_ROULEAU : this.capacite()));

  protected readonly zplPossible = computed(() => this.estRouleau() && this.tauriPrinter.isRunningInTauri());
  protected readonly estZpl = computed(() => this.zplPossible() && this.modeImpression() === 'ZPL');

  protected readonly peutImprimer = computed(() => {
    const qty = this.qty() ?? 0;
    const startAt = this.startAt() ?? 0;
    if (!this.articles().length || qty < 1 || qty > this.qtyMax()) {
      return false;
    }
    return this.estRouleau() || (startAt >= 1 && startAt <= this.capacite());
  });

  protected readonly modeOptions: AppPillOption[] = [
    { label: 'PDF (pilote)', value: 'PDF', icon: 'pi pi-file-pdf' },
    { label: 'ZPL (Zebra, TSC)', value: 'ZPL', icon: 'pi pi-print' },
  ];

  protected readonly resolutions = [
    { code: 'DPI_203', libelle: '203 dpi — entrée de gamme' },
    { code: 'DPI_300', libelle: '300 dpi' },
    { code: 'DPI_600', libelle: '600 dpi' },
  ];

  private readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(PromotionApiService);
  private readonly produitsApi = inject(ProductsApiService);
  private readonly notification = inject(NotificationService);
  private readonly blobDownload = inject(BlobDownloadService);
  private readonly tauriPrinter = inject(TauriPrinterService);
  private readonly destroyRef = inject(DestroyRef);

  /** Appelé par l'ouvrant : la promotion complète, avec ses articles. */
  init(promotion: IPromotion): void {
    this.promotion.set(promotion);
  }

  ngOnInit(): void {
    this.produitsApi
      .listerFormatsEtiquette()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: formats => {
          this.formats.set(formats);
          this.form.controls.formatCode.setValue((formats.find(f => f.parDefaut) ?? formats[0])?.code ?? null);
        },
        error: () => this.formats.set([]),
      });
  }

  protected onFormatChange(code: unknown): void {
    this.form.controls.formatCode.setValue(code as string | null);
    // Une planche s'imprime en PDF : le ZPL ne vaut que pour un rouleau.
    if (!this.estRouleau()) {
      this.form.controls.modeImpression.setValue('PDF');
    }
    if (this.estRouleau() || (this.startAt() ?? 1) > this.capacite()) {
      this.form.controls.startAt.setValue(1);
    }
  }

  protected onModeChange(mode: unknown): void {
    this.form.controls.modeImpression.setValue(mode as ModeImpression);
    if (this.modeImpression() === 'ZPL' && !this.imprimantes().length) {
      void this.tauriPrinter.getAvailablePrinters().then(imprimantes => {
        this.imprimantes.set(imprimantes);
        this.form.controls.imprimante.setValue(imprimantes.find(i => i.isDefault)?.name ?? imprimantes[0]?.name ?? null);
      });
    }
  }

  protected onPrint(): void {
    const promotion = this.promotion();
    if (!promotion || !this.peutImprimer()) {
      return;
    }
    this.loading.set(true);
    const qty = this.qty() ?? 1;
    const format = this.formatCode() ?? undefined;
    if (this.estZpl()) {
      this.api
        .etiquettesZpl(promotion.id, qty, format, this.form.controls.resolution.value)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: zpl => {
            void this.tauriPrinter
              .printZpl(zpl, this.form.controls.imprimante.value ?? undefined)
              .then(() => this.activeModal.close(true))
              .catch(() => this.echec("L'imprimante d'étiquettes n'a pas accepté le flux"));
          },
          error: () => this.echec('Erreur lors de la génération des étiquettes'),
        });
      return;
    }
    this.api
      .etiquettes(promotion.id, qty, this.startAt() ?? 1, format)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => {
          this.loading.set(false);
          this.blobDownload.downloadPdf(blob, `etiquettes-${promotion.code}`);
          this.activeModal.close(true);
        },
        error: () => this.echec('Erreur lors de la génération des étiquettes'),
      });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  private echec(message: string): void {
    this.notification.error(message);
    this.loading.set(false);
  }

  private suivre<T>(controle: FormControl<T>): Signal<T> {
    return toSignal(controle.valueChanges, { initialValue: controle.value });
  }
}
