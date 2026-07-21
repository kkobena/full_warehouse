import { Component, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { finalize } from "rxjs/operators";
import { HttpResponse } from "@angular/common/http";
import { NgbDateStruct, NgbModal, NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import { BedService } from "../data-access/bed.service";
import { IBed, IBedSummary, MotifBed, MOTIFS_BED } from "../data-access/bed.model";
import { NotificationService } from "app/shared/services/notification.service";
import { NgbConfirmDialogService } from "app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { BedDetailComponent } from "../bed-detail/bed-detail.component";
import { BedValidateModalComponent, BedValidateResult } from "../ui/bed-header-form/bed-validate-modal.component";
import { ITEMS_PER_PAGE } from "app/shared/constants/pagination.constants";
import { NGB_DATE_TO_ISO } from "app/shared/util/warehouse-util";
import { PharmaDatePickerComponent } from "app/shared/date-picker/pharma-date-picker.component";
import {
  AppSplitButtonItem,
  AppTableLazyLoadEvent,
  BadgeComponent,
  ButtonComponent,
  DataTableComponent,
  IconFieldComponent,
  SelectComponent,
  SplitButtonComponent,
} from "app/shared/ui";
import {
  ImportProduitModalComponent
} from "../../../../../entities/produit/import-produit-modal/import-produit-modal.component";
import { IResponseDto } from "../../../../../shared/util/response-dto";
import { showCommonModal } from "../../../../../entities/sales/selling-home/sale-helper";
import {
  ImportProduitReponseModalComponent
} from "../../../../../entities/produit/import-produit-reponse-modal/import-produit-reponse-modal.component";

export type BedTab = "BROUILLON" | "HISTORIQUE";

@Component({
  selector: "app-bed-home",
  templateUrl: "./bed-home.component.html",
  styleUrls: ["./bed-home.component.scss"],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    DataTableComponent,
    NgbTooltip,
    BadgeComponent,
    IconFieldComponent,
    PharmaDatePickerComponent,
    BedDetailComponent,
    SplitButtonComponent,
    SelectComponent,
  ]
})
export class BedHomeComponent implements OnInit {
  readonly beds = signal<IBedSummary[]>([]);
  readonly loading = signal(false);
  readonly totalRecords = signal(0);
  readonly activeTab = signal<BedTab>("BROUILLON");
  readonly editingBed = signal<IBed | null>(null);
  readonly countBrouillon = signal<number>(0);
  readonly showHint = signal<boolean>(true);

  protected search = "";
  protected filterMotif: MotifBed | null = null;
  protected dtStart: NgbDateStruct | null = null;
  protected dtEnd: NgbDateStruct | null = null;

  readonly itemsPerPage = ITEMS_PER_PAGE;
  private page = 0;

  readonly motifOptions = MOTIFS_BED;

  private readonly bedService = inject(BedService);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly modalService = inject(NgbModal);
  private readonly destroyRef = inject(DestroyRef);
  protected importMenuItems: AppSplitButtonItem[] = [
    { label: "Basculement", icon: "pi pi-filter", command: () => this.onImport("BASCULEMENT") },
    { label: "Basculement prestige", icon: "pi pi-file", command: () => this.onImport("BASCULEMENT_PRESTIGE") }
  ];

  ngOnInit(): void {
    this.loadBadges();
    this.loadAll();
  }

  protected setTab(tab: BedTab): void {
    if (this.activeTab() === tab) return;
    this.activeTab.set(tab);
    this.search = "";
    this.filterMotif = null;
    this.dtStart = null;
    this.dtEnd = null;
    this.page = 0;
    this.loadBadges();
    this.loadAll();
  }

  protected loadBadges(): void {
    this.bedService.findAll({ orderStatus: "REQUESTED", page: 0, size: 1 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.countBrouillon.set(Number(res.headers.get("X-Total-Count")) || 0),
        error: () => {
        }
      });
  }

  protected onSearch(): void {
    this.page = 0;
    this.loadAll();
  }

  protected onPageChange(event: AppTableLazyLoadEvent): void {
    this.page = Math.floor(event.first / event.rows);
    this.loadAll();
  }

  protected loadAll(): void {
    this.loading.set(true);
    const orderStatus = this.activeTab() === "BROUILLON" ? "REQUESTED" : "CLOSED";
    this.bedService
      .findAll({
        search: this.search || undefined,
        motifBed: this.filterMotif || undefined,
        orderStatus,
        fromDate: this.dtStart ? NGB_DATE_TO_ISO(this.dtStart)! : undefined,
        toDate: this.dtEnd ? NGB_DATE_TO_ISO(this.dtEnd)! : undefined,
        page: this.page,
        size: this.itemsPerPage
      })
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (res: HttpResponse<IBedSummary[]>) => {
          this.beds.set(res.body ?? []);
          this.totalRecords.set(Number(res.headers.get("X-Total-Count") ?? 0));
        },
        error: () => this.notificationService.error("Erreur lors du chargement des BED", "Erreur")
      });
  }

  protected onNouveauBed(): void {
    this.editingBed.set({});
  }

  protected onEditer(summary: IBedSummary): void {
    this.bedService.findById(summary.id!, summary.orderDate!).subscribe({
      next: bed => this.editingBed.set(bed),
      error: () => this.notificationService.error("Erreur lors du chargement du BED", "Erreur")
    });
  }

  protected onRetour(): void {
    this.editingBed.set(null);
    this.loadAll();
  }

  protected onSupprimerBed(summary: IBedSummary): void {
    this.confirmDialog.onConfirm(
      () => {
        this.bedService
          .delete(summary.id!, summary.orderDate!)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.loadAll();
              this.notificationService.success("BED supprimé", "Succès");
            },
            error: () => this.notificationService.error("Erreur lors de la suppression", "Erreur")
          });
      },
      "Supprimer le BED",
      `Supprimer définitivement ${summary.receiptReference} ?`
    );
  }

  protected onValiderDepuisListe(summary: IBedSummary): void {
    const modalRef = this.modalService.open(BedValidateModalComponent, {
      centered: true,
      size: "lg",
      backdrop: "static"
    });
    const instance = modalRef.componentInstance as BedValidateModalComponent;
    instance.bed = { receiptReference: summary.receiptReference, motifBed: summary.motifBed } as any;
    if (summary.motifBed) instance.formMotif.set(summary.motifBed);
    modalRef.result.then(
      (result: BedValidateResult) => {
        this.bedService
          .validate(summary.id!, {
            orderDate: summary.orderDate!,
            motif: result.motif,
            fournisseurId: result.fournisseurId,
            commentaire: result.commentaire
          })
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.loadAll();
              this.notificationService.success(`BED ${summary.receiptReference} validé`, "Succès");
            },
            error: () => this.notificationService.error("Erreur lors de la validation", "Erreur")
          });
      },
      () => {
      }
    );
  }

  protected dismissHint(): void {
    this.showHint.set(false);
  }

  protected getStatutLabel(statut: string | undefined): string {
    return statut === "CLOSED" ? "Validé" : statut === "REQUESTED" ? "En cours" : (statut ?? "");
  }

  protected getStatutSeverity(statut: string | undefined): "success" | "warn" | "secondary" {
    return statut === "CLOSED" ? "success" : statut === "REQUESTED" ? "warn" : "secondary";
  }

  private onImport(type: string): void {
    const modalRef = this.modalService.open(ImportProduitModalComponent, {
      backdrop: "static",
      size: "lg",
      centered: true
    });
    modalRef.componentInstance.type = type;
    modalRef.closed.subscribe(reason => {
      if (reason) {
        this.showResponse(reason);
        this.loadAll();
      }
    });
  }

  private showResponse(responsedto: IResponseDto): void {
    showCommonModal(this.modalService, ImportProduitReponseModalComponent, { responsedto }, () => {
    }, "lg");
  }

}
