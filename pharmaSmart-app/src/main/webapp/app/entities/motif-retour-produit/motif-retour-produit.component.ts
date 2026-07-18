import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { HttpResponse } from "@angular/common/http";
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import { ButtonModule } from "primeng/button";
import { TableModule } from "primeng/table";
import { InputTextModule } from "primeng/inputtext";
import { ToastModule } from "primeng/toast";
import { IMotifRetourProduit } from "app/shared/model/motif-retour-produit.model";
import { ModifRetourProduitService } from "./motif-retour-produit.service";
import { ITEMS_PER_PAGE } from "app/shared/constants/pagination.constants";
import { Tooltip } from "primeng/tooltip";
import { MotifRetourProduitFormModalComponent } from "./motif-retour-produit-form-modal.component";
import { NotificationService } from "../../shared/services/notification.service";
import { NgbConfirmDialogService } from "../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { Toolbar } from "primeng/toolbar";

@Component({
  selector: "jhi-motif-retour-produit",

  imports: [CommonModule, FormsModule, ButtonModule, TableModule, InputTextModule, ToastModule, Tooltip, Toolbar],
  templateUrl: "./motif-retour-produit.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./motif-retour-produit.component.scss"
})
export class MotifRetourProduitComponent implements OnInit {
  private readonly motifRetourService = inject(ModifRetourProduitService);
  private readonly modalService = inject(NgbModal);
  protected motifRetours = signal<IMotifRetourProduit[]>([]);
  protected totalRecords = signal<number>(0);
  protected loading = signal<boolean>(false);
  protected itemsPerPage = ITEMS_PER_PAGE;
  private readonly notificationService = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);

  ngOnInit(): void {
    this.loadAll();
  }

  protected loadAll(): void {
    this.loading.set(true);
    this.motifRetourService.query().subscribe({
      next: (res: HttpResponse<IMotifRetourProduit[]>) => {
        this.motifRetours.set(res.body || []);
        this.totalRecords.set(res.body?.length || 0);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.showError("Erreur lors du chargement des motifs de retour");
      }
    });
  }

  protected openNew(): void {
    const modalRef = this.modalService.open(MotifRetourProduitFormModalComponent, {
      size: "lg",
      backdrop: "static",
      centered: true,
      keyboard: false
    });

    modalRef.result.then(
      () => {
        this.loadAll();
      },
      () => {
        // Modal dismissed
      }
    );
  }

  protected edit(motif: IMotifRetourProduit): void {
    const modalRef = this.modalService.open(MotifRetourProduitFormModalComponent, {
      size: "lg",
      backdrop: "static",
      keyboard: false
    });

    modalRef.componentInstance.motifToEdit = motif;

    modalRef.result.then(
      () => {
        this.loadAll();
      },
      () => {
        // Modal dismissed
      }
    );
  }

  protected confirmDelete(motif: IMotifRetourProduit): void {
    this.confirmDialog.onConfirm(() => this.delete(motif.id), "Confirmation de suppression", `Êtes-vous sûr de vouloir supprimer le motif "${motif.libelle}" ?`
    );
  }

  protected delete(id: number): void {
    this.motifRetourService.delete(id).subscribe({
      next: () => {
        this.showSuccess("Motif de retour supprimé avec succès");
        this.loadAll();
      },
      error: () => {
        this.showError("Erreur lors de la suppression du motif de retour");
      }
    });
  }

  private showSuccess(message: string): void {
    this.notificationService.success(message);
  }

  private showError(message: string): void {
    this.notificationService.error(message);
  }
}
