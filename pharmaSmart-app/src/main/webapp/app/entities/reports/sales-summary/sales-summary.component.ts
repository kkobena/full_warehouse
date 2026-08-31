import {NGB_DATE_TO_ISO} from '../../../shared/util/warehouse-util';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  OnInit,
  signal
} from "@angular/core";
import {HttpResponse} from "@angular/common/http";
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";


import {IDailySalesSummary} from "app/shared/model/report/daily-sales-summary.model";
import {SalesSummaryReportService} from "../services/sales-summary-report.service";
import {NgbDateStruct} from '@ng-bootstrap/ng-bootstrap';
import {
  AppBadgeSeverity,
  BadgeComponent,
  ButtonComponent,
  DataTableComponent,
  KpiItemComponent,
  KpiStripComponent,
  SelectComponent,
  ToolbarComponent
} from '../../../shared/ui';
import {PharmaDatePickerComponent} from '../../../shared/date-picker/pharma-date-picker.component';
import {DeviseDirective} from "../../../shared/utils/devise";
import {
  libelleTypeVente,
  OPTIONS_FILTRE_TYPE_VENTE,
  severiteTypeVente
} from "../../../shared/constants/type-vente.constants";

@Component({
  selector: "app-sales-summary",
  templateUrl: "./sales-summary.component.html",
  styleUrl: "./sales-summary.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    BadgeComponent,
    ButtonComponent,
    DataTableComponent,
    SelectComponent,
    ToolbarComponent,
    PharmaDatePickerComponent,
    KpiStripComponent,
    KpiItemComponent,
    DeviseDirective
  ]
})
export default class SalesSummaryComponent implements OnInit {
  /**
   * Code de l'entrée de navigation dont cet écran est le contenu.
   *
   * <p>Fourni par le layout : ce rapport est parfois atteint depuis deux menus, qui ne le nomment
   * pas de la même façon. Le titre suit celui par lequel on est entré.
   */
  readonly navCode = input<string>('');

  summaries = signal<IDailySalesSummary[]>([]);
  isLoading = signal<boolean>(false);
  startDate = signal<NgbDateStruct | null>(null);
  endDate = signal<NgbDateStruct | null>(null);
  selectedTypeVente = signal<string | null>(null);

  /** Les libellés viennent de la même table que le tableau : voir `type-vente.constants`. */
  typeVenteOptions = OPTIONS_FILTRE_TYPE_VENTE;
  /**
   * Ventilation par TYPE DE VENTE des lignes affichées.
   *
   * Les quatre indicateurs du bandeau totalisent la période, tous types confondus : pour
   * savoir ce que pèse le tiers payant, il fallait poser un filtre, relire, en poser un autre
   * et faire la soustraction de tête. Or c'est précisément la question qu'on se pose devant
   * cet écran — quelle part du chiffre d'affaires attend un règlement, et laquelle est déjà
   * encaissée.
   *
   * La ventilation se calcule donc sur les lignes DÉJÀ CHARGÉES, sans requête ni filtre : le
   * détail par type est là dès l'ouverture, à côté du total.
   */
  readonly ventilationParType = computed(() => {
    const cumul = new Map<string, { nbVentes: number; caTotal: number; caNet: number }>();
    for (const ligne of this.summaries()) {
      const type = ligne.typeVente ?? 'N/A';
      const courant = cumul.get(type) ?? {nbVentes: 0, caTotal: 0, caNet: 0};
      courant.nbVentes += ligne.nbVentes ?? 0;
      courant.caTotal += ligne.caTotal ?? 0;
      courant.caNet += ligne.caNet ?? 0;
      cumul.set(type, courant);
    }
    const totalNet = Array.from(cumul.values()).reduce((somme, item) => somme + item.caNet, 0);
    return Array.from(cumul.entries())
      .map(([type, item]) => ({
        type,
        libelle: this.getLibelleType(type),
        severity: this.getSeverityForType(type),
        nbVentes: item.nbVentes,
        caTotal: item.caTotal,
        caNet: item.caNet,
        panierMoyen: item.nbVentes > 0 ? item.caTotal / item.nbVentes : 0,
        part: totalNet > 0 ? (item.caNet * 100) / totalNet : 0,
      }))
      .sort((a, b) => b.caNet - a.caNet);
  });
  private readonly salesSummaryService = inject(SalesSummaryReportService);

  ngOnInit(): void {

    const now = new Date();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    this.startDate.set({
      year: firstDay.getFullYear(),
      month: firstDay.getMonth() + 1,
      day: firstDay.getDate()
    });
    this.endDate.set({
      year: lastDay.getFullYear(),
      month: lastDay.getMonth() + 1,
      day: lastDay.getDate()
    });

    this.loadSummaries();
  }

  loadSummaries(): void {
    if (!this.startDate() || !this.endDate()) {
      return;
    }

    this.isLoading.set(true);
    const startDateStr = NGB_DATE_TO_ISO(this.startDate())!;
    const endDateStr = NGB_DATE_TO_ISO(this.endDate())!;
    const typeVente = this.selectedTypeVente();

    const request = typeVente
      ? this.salesSummaryService.getDailySalesSummaryByType(startDateStr, endDateStr, typeVente)
      : this.salesSummaryService.getDailySalesSummary(startDateStr, endDateStr);

    request.subscribe({
      next: (res: HttpResponse<IDailySalesSummary[]>) => {
        this.summaries.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  onFilterChange(): void {
    this.loadSummaries();
  }

  getTotalCA(): number {
    return this.summaries().reduce((sum, item) => sum + (item.caTotal || 0), 0);
  }

  getTotalCANet(): number {
    return this.summaries().reduce((sum, item) => sum + (item.caNet || 0), 0);
  }

  getTotalVentes(): number {
    return this.summaries().reduce((sum, item) => sum + (item.nbVentes || 0), 0);
  }

  getAveragePanier(): number {
    const total = this.getTotalCA();
    const count = this.getTotalVentes();
    return count > 0 ? total / count : 0;
  }


  /** Libellé d'écran d'un type de vente — source unique, partagée avec le tableau de bord. */
  getLibelleType(type: string | undefined): string {
    return libelleTypeVente(type);
  }

  getSeverityForType(type: string | undefined): AppBadgeSeverity {
    return severiteTypeVente(type);
  }
}
