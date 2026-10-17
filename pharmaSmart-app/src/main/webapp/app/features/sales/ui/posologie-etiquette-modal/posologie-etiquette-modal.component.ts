import {ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {saveAs} from 'file-saver';
import {AppPillOption, ButtonComponent, CardComponent, InputNumberComponent, PillSelectorComponent, SelectComponent} from 'app/shared/ui';
import {IFormatEtiquette, ProductsApiService} from 'app/features/products/data-access/services/products-api.service';
import {IOrdonnance} from 'app/shared/model/ordonnance.model';
import {NotificationService} from 'app/shared/services/notification.service';
import {PrinterInfo, TauriPrinterService} from 'app/shared/services/tauri-printer.service';
import {OrdonnanceApiService} from '../../data-access/services/ordonnance-api.service';

/** PDF par le pilote, ou ZPL en direct dans l'imprimante — voir la modale d'étiquettes produit. */
type ModeImpression = 'PDF' | 'ZPL';

/**
 * Étiquettes de posologie d'une ordonnance : une par ligne, ou une seule.
 *
 * <p>Patient, prescripteur et instructions viennent de l'ordonnance : il n'y a rien à ressaisir,
 * et donc rien qui puisse différer de ce qui a été délivré.
 */
@Component({
  selector: 'app-posologie-etiquette-modal',
  templateUrl: './posologie-etiquette-modal.component.html',
  styleUrl: './posologie-etiquette-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ButtonComponent, CardComponent, InputNumberComponent, PillSelectorComponent, SelectComponent],
})
export class PosologieEtiquetteModalComponent implements OnInit {
  ordonnance!: IOrdonnance;

  protected qty = 1;
  protected startAt = 1;
  protected readonly loading = signal(false);
  protected readonly ligneId = signal<number | null>(null);

  protected readonly formats = signal<IFormatEtiquette[]>([]);
  protected readonly formatCode = signal<string | null>(null);
  protected readonly formatChoisi = computed(() => this.formats().find(f => f.code === this.formatCode()) ?? null);
  protected readonly capacite = computed(() => this.formatChoisi()?.capacite ?? 65);
  protected readonly estRouleau = computed(() => this.formatChoisi()?.support === 'ROULEAU');

  protected readonly modeImpression = signal<ModeImpression>('PDF');
  protected readonly imprimantes = signal<PrinterInfo[]>([]);
  protected readonly imprimante = signal<string | null>(null);
  protected readonly resolution = signal<string>('DPI_203');
  protected readonly zplPossible = computed(() => this.estRouleau() && this.tauriPrinter.isRunningInTauri());
  protected readonly estZpl = computed(() => this.zplPossible() && this.modeImpression() === 'ZPL');

  protected readonly modeOptions: AppPillOption[] = [
    {label: 'PDF (pilote)', value: 'PDF', icon: 'pi pi-file-pdf'},
    {label: 'ZPL (Zebra, TSC)', value: 'ZPL', icon: 'pi pi-print'},
  ];

  protected readonly resolutions = [
    {code: 'DPI_203', libelle: '203 dpi — entrée de gamme'},
    {code: 'DPI_300', libelle: '300 dpi'},
    {code: 'DPI_600', libelle: '600 dpi'},
  ];

  private readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(OrdonnanceApiService);
  private readonly produitsApi = inject(ProductsApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly tauriPrinter = inject(TauriPrinterService);
  private readonly destroyRef = inject(DestroyRef);

  /** Les lignes de l'ordonnance, plus le choix « toutes ». */
  protected readonly lignes = computed(() => this.ordonnance.lignes.map(l => ({id: l.id, libelle: l.produitLibelle ?? l.texteLu ?? '—'})));

  ngOnInit(): void {
    this.produitsApi
      .listerFormatsEtiquette()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: formats => {
          this.formats.set(formats);
          this.formatCode.set((formats.find(f => f.parDefaut) ?? formats[0])?.code ?? null);
        },
        error: () => this.formats.set([]),
      });
  }

  protected onFormatChange(code: string | null): void {
    this.formatCode.set(code);
    if (!this.estRouleau()) {
      this.modeImpression.set('PDF');
    }
    if (this.estRouleau() || this.startAt > this.capacite()) {
      this.startAt = 1;
    }
  }

  protected onModeChange(mode: unknown): void {
    this.modeImpression.set(mode as ModeImpression);
    if (this.modeImpression() === 'ZPL' && !this.imprimantes().length) {
      void this.tauriPrinter.getAvailablePrinters().then(imprimantes => {
        this.imprimantes.set(imprimantes);
        this.imprimante.set(imprimantes.find(i => i.isDefault)?.name ?? imprimantes[0]?.name ?? null);
      });
    }
  }

  protected peutImprimer(): boolean {
    if (this.qty < 1) return false;
    return this.estRouleau() || (this.startAt >= 1 && this.startAt <= this.capacite());
  }

  protected imprimer(): void {
    if (!this.peutImprimer()) return;
    this.loading.set(true);
    const format = this.formatCode() ?? undefined;
    const ligneId = this.ligneId() ?? undefined;
    if (this.estZpl()) {
      this.envoyerEnZpl(ligneId, format);
      return;
    }
    this.api
      .etiquettesPosologie(this.ordonnance.id, {ligneId, qty: this.qty, startAt: this.estRouleau() ? 1 : this.startAt, format})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => {
          this.loading.set(false);
          saveAs(blob, `posologie-${this.ordonnance.id}.pdf`);
          this.activeModal.close(true);
        },
        error: () => {
          this.notificationService.error('Erreur lors de la génération des étiquettes');
          this.loading.set(false);
        },
      });
  }

  protected annuler(): void {
    this.activeModal.dismiss();
  }

  private envoyerEnZpl(ligneId: number | undefined, format: string | undefined): void {
    this.api
      .etiquettesPosologieZpl(this.ordonnance.id, {ligneId, qty: this.qty, format, resolution: this.resolution()})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: zpl => {
          this.tauriPrinter
            .printZpl(zpl, this.imprimante() ?? undefined)
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
          this.notificationService.error('Erreur lors de la génération des étiquettes');
          this.loading.set(false);
        },
      });
  }
}
