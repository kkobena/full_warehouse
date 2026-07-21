import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { HttpResponse } from "@angular/common/http";
import { RouterLink } from "@angular/router";
import { FormsModule } from "@angular/forms";
import { NgbDateStruct } from "@ng-bootstrap/ng-bootstrap";
import { IRetourDepot } from "app/shared/model/retour-depot.model";
import { RetourDepotService } from "../retour-depot.service";
import { MagasinService } from "../../magasin/magasin.service";
import { IMagasin } from "../../../shared/model";
import { ITEMS_PER_PAGE } from "app/shared/constants/pagination.constants";
import dayjs from "dayjs/esm";
import { NGB_DATE_TO_ISO } from "../../../shared/util/warehouse-util";
import { NotificationService } from "../../../shared/services/notification.service";
import {
  AppTableLazyLoadEvent,
  ButtonComponent,
  DataTableComponent,
  RowTogglerDirective,
  SelectComponent,
  ToolbarComponent
} from "../../../shared/ui";
import { PharmaDatePickerComponent } from "../../../shared/date-picker/pharma-date-picker.component";

@Component({
  selector: "app-depot-retour-list",
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    DataTableComponent,
    SelectComponent,
    ToolbarComponent,
    PharmaDatePickerComponent,
    RowTogglerDirective,
    RouterLink
  ],
  templateUrl: "./depot-retour-list.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./depot-retour-list.component.scss"
})
export class DepotRetourListComponent implements OnInit {
  private readonly retourDepotService = inject(RetourDepotService);
  private readonly magasinService = inject(MagasinService);

  protected depots = signal<IMagasin[]>([]);
  protected selectedDepot: IMagasin | null = null;
  protected fromDate: NgbDateStruct | null = this.dateToNgbStruct(new Date());
  protected toDate: NgbDateStruct | null = this.dateToNgbStruct(new Date());
  protected search = "";

  protected retourDepots = signal<IRetourDepot[]>([]);
  protected loading = signal<boolean>(false);
  protected totalRecords = signal<number>(0);
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = signal<number>(0);

  private readonly notificationService = inject(NotificationService);

  /** Options du sélecteur de dépôt, avec l'adresse ajoutée au libellé (remplace le `#item` custom de `p-select`). */
  protected get depotOptions(): (IMagasin & { displayLabel: string })[] {
    return this.depots().map(depot => ({
      ...depot,
      displayLabel: depot.address ? `${depot.name} — ${depot.address}` : depot.name
    }));
  }

  ngOnInit(): void {
    this.loadDepots();
  }

  protected loadDepots(): void {
    this.magasinService.fetchAllDepots().subscribe({
      next: (res: HttpResponse<IMagasin[]>) => {
        this.depots.set(res.body || []);
      },
      error: () => {
        this.notificationService.error("Erreur lors du chargement des dépôts");
      }
    });
  }

  protected onDepotChange(): void {
    this.page.set(0);
    this.loadAll();
  }

  protected onSearch(): void {
    this.page.set(0);
    this.loadAll();
  }

  protected loadAll(): void {
    this.loading.set(true);
    const query: any = {
      page: this.page(),
      size: this.itemsPerPage
    };

    if (this.fromDate) {
      query.dtStart = NGB_DATE_TO_ISO(this.fromDate);
    }
    if (this.toDate) {
      query.dtEnd = NGB_DATE_TO_ISO(this.toDate);
    }

    if (this.search) {
      query.search = this.search;
    }

    if (this.selectedDepot) {
      query.depotId = this.selectedDepot.id;
    }

    this.retourDepotService.query(query).subscribe({
      next: (res: HttpResponse<IRetourDepot[]>) => {
        this.onSuccess(res.body, res.headers);
      },
      error: () => {
        this.onError();
      },
      complete: () => {
        this.loading.set(false);
      }
    });
  }

  protected onSuccess(data: IRetourDepot[] | null, headers: any): void {
    this.totalRecords.set(Number(headers.get("X-Total-Count")));
    this.retourDepots.set(data || []);
  }

  protected onError(): void {
    this.notificationService.error("Erreur lors du chargement des retours dépôt");
  }

  protected onPageChange(event: AppTableLazyLoadEvent): void {
    this.page.set(event.first / event.rows);
    this.loadAll();
  }

  private dateToNgbStruct(date: Date): NgbDateStruct {
    return { year: date.getFullYear(), month: date.getMonth() + 1, day: date.getDate() };
  }

  protected formatDate(date: string | undefined): string {
    return date ? dayjs(date).format("DD/MM/YYYY HH:mm") : "";
  }

  protected getTotalItems(retourDepot: IRetourDepot): number {
    return retourDepot.retourDepotItems?.length || 0;
  }

  protected getTotalQuantity(retourDepot: IRetourDepot): number {
    return retourDepot.retourDepotItems?.reduce((sum, item) => sum + (item.qtyMvt || 0), 0) || 0;
  }
}
