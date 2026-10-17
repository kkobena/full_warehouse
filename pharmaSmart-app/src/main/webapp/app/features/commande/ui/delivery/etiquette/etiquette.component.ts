import {signal, computed, AfterViewInit, Component, ElementRef, inject, OnDestroy, OnInit, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { IDelivery } from 'app/shared/model/delevery.model';
import { saveAs } from 'file-saver';
import { DATE_FORMAT_DD_MM_YYYY_HH_MM_SS } from 'app/shared/util/warehouse-util';
import { DeliveryService } from '../../../../../entities/commande/delevery/delivery.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { handleBlobForTauri } from 'app/shared/util/tauri-util';
import { PrinterInfo, TauriPrinterService } from 'app/shared/services/tauri-printer.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { AppPillOption, ButtonComponent, CardComponent, KeyFilterDirective, PillSelectorComponent, SelectComponent } from 'app/shared/ui';
import { IFormatEtiquette, ProductsApiService } from 'app/features/products/data-access/services/products-api.service';

/** Les deux types d'étiquette proposés à la génération. */
type TypeEtiquette = 'STANDARD' | 'LOT';

/** PDF par le pilote Windows, ou ZPL en direct dans l'imprimante — voir la modale produit. */
type ModeImpression = 'PDF' | 'ZPL';

@Component({
  selector: 'jhi-etiquette-delevery',
  templateUrl: './etiquette.component.html',
  styleUrls: ['etiquette.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, KeyFilterDirective, ButtonComponent, FormsModule, CardComponent, PillSelectorComponent, SelectComponent],
})
export class EtiquetteComponent implements AfterViewInit, OnDestroy, OnInit {
  protected readonly isSaving = signal(false);
  entity?: IDelivery;
  header = 'Impression des étiquettes';
  protected startAt = 1;
  protected readonly typeEtiquette = signal<TypeEtiquette>('STANDARD');
  protected readonly typeOptions: AppPillOption[] = [
    { label: 'Standard', value: 'STANDARD', icon: 'pi pi-tag' },
    { label: 'Lot (DataMatrix)', value: 'LOT', icon: 'pi pi-qrcode' },
  ];

  protected readonly formats = signal<IFormatEtiquette[]>([]);
  protected readonly formatCode = signal<string | null>(null);
  protected readonly formatChoisi = computed(() => this.formats().find(f => f.code === this.formatCode()) ?? null);

  /** Borne de la case de depart : elle vient de la planche, elle n'est plus codee en dur a 64. */
  protected readonly capacite = computed(() => this.formatChoisi()?.capacite ?? 65);

  /** Rouleau : une etiquette par page, donc pas de planche entamee a reprendre. */
  protected readonly estRouleau = computed(() => this.formatChoisi()?.support === 'ROULEAU');

  protected readonly modeImpression = signal<ModeImpression>('PDF');
  protected readonly imprimantes = signal<PrinterInfo[]>([]);
  protected readonly imprimante = signal<string | null>(null);
  protected readonly resolution = signal<string>('DPI_203');

  /** Le ZPL n'a de sens qu'en rouleau, et le poste doit pouvoir parler a l'imprimante. */
  protected readonly zplPossible = computed(() => this.estRouleau() && this.tauriPrinterService.isRunningInTauri());

  protected readonly estZpl = computed(() => this.zplPossible() && this.modeImpression() === 'ZPL');

  protected readonly modeOptions: AppPillOption[] = [
    { label: 'PDF (pilote)', value: 'PDF', icon: 'pi pi-file-pdf' },
    { label: 'ZPL (Zebra, TSC)', value: 'ZPL', icon: 'pi pi-print' },
  ];

  protected readonly resolutions = [
    { code: 'DPI_203', libelle: '203 dpi — entrée de gamme' },
    { code: 'DPI_300', libelle: '300 dpi' },
    { code: 'DPI_600', libelle: '600 dpi' },
  ];
  private readonly entityService = inject(DeliveryService);
  private readonly produitsApi = inject(ProductsApiService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly notificationService = inject(NotificationService);
  private destroy$ = new Subject<void>();
  private readonly activeModal = inject(NgbActiveModal);
  // Optionnel : le champ disparait du gabarit des qu'un rouleau est choisi.
  private readonly startInput = viewChild<ElementRef>('startInput');

  ngOnInit(): void {
    this.produitsApi
      .listerFormatsEtiquette()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: formats => {
          this.formats.set(formats);
          this.formatCode.set((formats.find(f => f.parDefaut) ?? formats[0])?.code ?? null);
        },
        error: () => this.formats.set([]),
      });
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.startInput()?.nativeElement.focus();
    }, 100);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  save(): void {
    this.isSaving.set(true);
    this.printEtiquette();
  }

  cancel(): void {
    this.isSaving.set(false);
    this.activeModal.dismiss();
  }

  protected onTypeChange(type: unknown): void {
    this.typeEtiquette.set(type as TypeEtiquette);
  }

  protected onFormatChange(code: string | null): void {
    this.formatCode.set(code);
    // Une planche s'imprime en PDF : le ZPL ne vaut que pour un rouleau.
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
      this.chargerImprimantes();
    }
  }

  private chargerImprimantes(): void {
    void this.tauriPrinterService.getAvailablePrinters().then(imprimantes => {
      this.imprimantes.set(imprimantes);
      this.imprimante.set(imprimantes.find(i => i.isDefault)?.name ?? imprimantes[0]?.name ?? null);
    });
  }

  /** La case de depart doit tenir dans la planche choisie — un rouleau n'en a pas. */
  protected departValide(): boolean {
    return this.estRouleau() || (this.startAt >= 1 && this.startAt <= this.capacite());
  }

  /** Le flux part dans l'imprimante : rien a telecharger, rien a ouvrir. */
  private envoyerEnZpl(): void {
    const req = { ...(this.formatCode() ? { format: this.formatCode() } : {}), resolution: this.resolution() };
    const requete = this.typeEtiquette() === 'LOT'
      ? this.entityService.printEtiquetteLotZpl(this.entity.commandeId, req)
      : this.entityService.printEtiquetteZpl(this.entity.commandeId, req);
    requete.pipe(takeUntil(this.destroy$)).subscribe({
      next: (zpl: string) => {
        this.tauriPrinterService
          .printZpl(zpl, this.imprimante() ?? undefined)
          .then(() => {
            this.isSaving.set(false);
            this.cancel();
          })
          .catch(() => {
            this.notificationService.error("L'imprimante d'étiquettes n'a pas accepté le flux");
            this.isSaving.set(false);
          });
      },
      error: () => {
        this.isSaving.set(false);
      },
    });
  }

  private printEtiquette(): void {
    if (this.estZpl()) {
      this.envoyerEnZpl();
      return;
    }
    const req = { startAt: this.estRouleau() ? 1 : this.startAt, ...(this.formatCode() ? { format: this.formatCode() } : {}) };
    const requete = this.typeEtiquette() === 'LOT'
      ? this.entityService.printEtiquetteLot(this.entity.commandeId, req)
      : this.entityService.printEtiquette(this.entity.commandeId, req);
    requete
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob: Blob) => {
          this.isSaving.set(false);
          if (this.tauriPrinterService.isRunningInTauri()) {
            handleBlobForTauri(blob, this.entity.receiptReference);
          } else {
            saveAs(blob, this.entity.receiptReference + '_' + DATE_FORMAT_DD_MM_YYYY_HH_MM_SS());
          }
          this.cancel();
        },
        error: () => {
          this.isSaving.set(false);
        },
      });
  }
}
