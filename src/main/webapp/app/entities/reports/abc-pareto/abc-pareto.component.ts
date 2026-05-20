import {Component, inject, OnInit, signal} from '@angular/core';
import {HttpResponse} from '@angular/common/http';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';

import {TableModule} from 'primeng/table';
import {ButtonModule} from 'primeng/button';
import {SelectModule} from 'primeng/select';
import {ToolbarModule} from 'primeng/toolbar';
import {DividerModule} from 'primeng/divider';
import {ChipModule} from 'primeng/chip';
import {ProgressBarModule} from 'primeng/progressbar';
import {WarehouseCommonModule} from '../../../shared/warehouse-common/warehouse-common.module';

import {IABCPareto, IABCParetoSummary} from 'app/shared/model/report';
import {ClassePareto} from 'app/shared/model/report/classe-pareto.enum';
import {ABCParetoReportService} from '../services/abc-pareto-report.service';
import {InputText} from 'primeng/inputtext';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {Drawer} from 'primeng/drawer';
import {formatCurrency} from 'app/shared/utils/format-utils';
import {TauriPrinterService} from "../../../shared/services/tauri-printer.service";
import {handleBlobForTauri} from "../../../shared/util/tauri-util";

@Component({
  selector: 'jhi-abc-pareto',
  templateUrl: './abc-pareto.component.html',
  styleUrl: './abc-pareto.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    SelectModule,
    ToolbarModule,
    DividerModule,
    ChipModule,
    ProgressBarModule,
    InputText,
    IconField,
    InputIcon,
    Drawer,
  ],
})
export default class ABCParetoComponent implements OnInit {
  products = signal<IABCPareto[]>([]);
  summary = signal<IABCParetoSummary | null>(null);
  isLoading = signal<boolean>(false);
  selectedFamille = signal<string | null>(null);
  selectedClassePareto = signal<ClassePareto | null>(null);
  helpDrawerVisible = signal<boolean>(false);

  familleOptions = signal<{ label: string; value: string }[]>([]);
  classeParetoOptions = signal<{ label: string; value: ClassePareto | '' }[]>([
    {label: 'Toutes', value: ''},
    {label: 'A+ — Top 60% du CA', value: ClassePareto.A_PLUS},
    {label: 'A — 60-80% du CA', value: ClassePareto.A},
    {label: 'B — 80-95% du CA', value: ClassePareto.B},
    {label: 'C — 95-99% du CA', value: ClassePareto.C},
    {label: 'D — Sans ventes / >99%', value: ClassePareto.D},
  ]);

  ClassePareto = ClassePareto;
  formatCurrency = formatCurrency;
  private readonly abcParetoService = inject(ABCParetoReportService);
  private readonly tauriPrinter = inject(TauriPrinterService);

  ngOnInit(): void {
    this.loadABCPareto();
    this.loadSummary();
  }

  loadABCPareto(): void {
    this.isLoading.set(true);
    const famille = this.selectedFamille();
    const classePareto = this.selectedClassePareto();

    let request;
    if (classePareto) {
      request = this.abcParetoService.getABCParetoByClass(classePareto);
    } else if (famille) {
      request = this.abcParetoService.getABCParetoByCategory(famille);
    } else {
      request = this.abcParetoService.getAllABCParetoAnalysis();
    }

    request.subscribe({
      next: (res: HttpResponse<IABCPareto[]>) => {
        this.products.set(res.body ?? []);
        this.extractFamilleOptions(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  loadSummary(): void {
    this.abcParetoService.getABCParetoSummary().subscribe({
      next: (res: HttpResponse<IABCParetoSummary>) => {
        this.summary.set(res.body ?? null);
      },
      error() {
        console.error('Error loading summary');
      },
    });
  }

  onFilterChange(): void {
    this.loadABCPareto();
  }

  onClearFilters(): void {
    this.selectedFamille.set(null);
    this.selectedClassePareto.set(null);
    this.loadABCPareto();
  }

  showTopContributors(): void {
    this.isLoading.set(true);
    this.abcParetoService.getTopRevenueContributors(50).subscribe({
      next: (res: HttpResponse<IABCPareto[]>) => {
        this.products.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  exportToPdf(): void {
    this.abcParetoService.exportABCParetoToPdf()
      .subscribe(resp => {
        if (this.tauriPrinter.isRunningInTauri()) {
          handleBlobForTauri(resp.body, `abc_pareto_${new Date().getTime()}`);
        } else {
          window.open(URL.createObjectURL(resp.body));
        }
      });
  }

  getClasseParetoLabel(classePareto: ClassePareto | undefined): string {
    switch (classePareto) {
      case ClassePareto.A_PLUS: return 'A+';
      case ClassePareto.A:      return 'A';
      case ClassePareto.B:      return 'B';
      case ClassePareto.C:      return 'C';
      case ClassePareto.D:      return 'D';
      default:                  return '';
    }
  }

  getClasseParetoSeverity(classePareto: ClassePareto | undefined): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    switch (classePareto) {
      case ClassePareto.A_PLUS: return 'danger';
      case ClassePareto.A:      return 'success';
      case ClassePareto.B:      return 'info';
      case ClassePareto.C:      return 'warn';
      case ClassePareto.D:      return 'secondary';
      default:                  return 'secondary';
    }
  }

  getClasseParetoDescription(classePareto: ClassePareto | undefined): string {
    switch (classePareto) {
      case ClassePareto.A_PLUS: return '≤ 60% CA cumulé';
      case ClassePareto.A:      return '60-80% CA cumulé';
      case ClassePareto.B:      return '80-95% CA cumulé';
      case ClassePareto.C:      return '95-99% CA cumulé';
      case ClassePareto.D:      return 'Sans ventes / >99%';
      default:                  return '';
    }
  }

  getClasseParetoClass(classePareto: ClassePareto | undefined): string {
    switch (classePareto) {
      case ClassePareto.A_PLUS: return 'pareto-badge pareto-a-plus';
      case ClassePareto.A:      return 'pareto-badge pareto-a';
      case ClassePareto.B:      return 'pareto-badge pareto-b';
      case ClassePareto.C:      return 'pareto-badge pareto-c';
      case ClassePareto.D:      return 'pareto-badge pareto-d';
      default:                  return 'pareto-badge pareto-d';
    }
  }

  getCumulativePercentageColor(caCumulePct: number | undefined): string {
    if (!caCumulePct) return 'secondary';
    if (caCumulePct <= 60)  return 'danger';
    if (caCumulePct <= 80)  return 'success';
    if (caCumulePct <= 95)  return 'info';
    if (caCumulePct <= 99)  return 'warn';
    return 'secondary';
  }

  toggleHelpDrawer(): void {
    this.helpDrawerVisible.update(value => !value);
  }

  private extractFamilleOptions(products: IABCPareto[]): void {
    const familles = [...new Set(products.map(p => p.famille).filter(f => f))];
    this.familleOptions.set([
      {label: 'Toutes les familles', value: ''},
      ...familles.map(f => ({label: f!, value: f!})),
    ]);
  }
}
