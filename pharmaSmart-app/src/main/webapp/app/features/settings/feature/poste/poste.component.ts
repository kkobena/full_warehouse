import { HttpErrorResponse, HttpResponse } from "@angular/common/http";
import { Component, inject, OnInit, signal } from "@angular/core";
import { RouterModule } from "@angular/router";
import { PosteService } from "./poste.service";
import { IPoste } from "../../../../shared/model/poste.model";
import { ButtonModule } from "primeng/button";
import { ToolbarModule } from "primeng/toolbar";
import { TableModule } from "primeng/table";
import { InputTextModule } from "primeng/inputtext";
import { TooltipModule } from "primeng/tooltip";
import { FormsModule } from "@angular/forms";
import { IconField } from "primeng/iconfield";
import { InputIcon } from "primeng/inputicon";
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import { ErrorService } from "../../../../shared/error.service";
import { showCommonModal } from "../../../../entities/sales/selling-home/sale-helper";
import { FormPosteComponent } from "./form-poste/form-poste.component";
import { Tag } from "primeng/tag";
import { PosteDeviceService } from "./poste-device.service";
import { IPosteDevice } from "../../../../shared/model/poste-device.model";
import { BadgeModule } from "primeng/badge";
import { NgbConfirmDialogService } from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { NotificationService } from "../../../../shared/services/notification.service";
import { Toast } from "primeng/toast";
import { CommonModule } from "@angular/common";

@Component({
  selector: "app-poste",
  templateUrl: "./poste.component.html",
  styleUrls: ["./poste.component.scss"],
  imports: [
    CommonModule,
    ButtonModule,
    ToolbarModule,
    TableModule,
    RouterModule,
    InputTextModule,
    TooltipModule,
    FormsModule,
    IconField,
    InputIcon,
    Tag,
    BadgeModule,
    Toast
  ]
})
export class PosteComponent implements OnInit {
  protected loading?: boolean;
  protected entites: IPoste[] = [];
  protected filteredEntities: IPoste[] = [];
  protected searchQuery = "";
  protected expandedRows: Record<number, boolean> = {};
  protected devicesMap = signal<Record<number, IPosteDevice[]>>({});
  private readonly entityService = inject(PosteService);
  private readonly posteDeviceService = inject(PosteDeviceService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly modalService = inject(NgbModal);
  private readonly errorService = inject(ErrorService);

  ngOnInit(): void {
    this.loadAll();
  }

  protected loadAll(): void {
    this.loading = true;
    this.entityService.fetchAll().subscribe({
      next: (res: HttpResponse<IPoste[]>) => this.onSuccess(res.body),
      error: err => this.onError(err)
    });
  }

  protected addNewEntity(): void {
    showCommonModal(
      this.modalService,
      FormPosteComponent,
      {
        entity: null,
        title: "Ajouter un nouveau poste"
      },
      () => {
        this.loadAll();
      },
      "lg"
    );
  }

  protected onEdit(entity: IPoste): void {
    showCommonModal(
      this.modalService,
      FormPosteComponent,
      {
        entity,
        title: "Modification de " + entity.name
      },
      () => {
        this.loadAll();
      },
      "lg"
    );
  }

  protected delete(entity: IPoste): void {
    if (entity.id) {
      this.confirmDelete(entity.id);
    }
  }


  protected confirmDelete(id: number): void {
    this.confirmDialog.onConfirm(
      () => {
        this.entityService.delete(id).subscribe(() => {
          this.notificationService.success("Le poste a été supprimé avec succès.", "Suppression réussie");
          this.loadAll();
        });
      },
      "Suppression",
      "Êtes-vous sûr de vouloir supprimer ce poste ?"
    );
  }

  protected search(event: any): void {
    this.searchQuery = event.target.value.toLowerCase();
    this.applyFilter();
  }

  protected onRowExpand(event: { data: IPoste }): void {
    const posteId = event.data.id;
    if (posteId) {
      this.loadDevices(posteId);
    }
  }

  protected loadDevices(posteId: number): void {
    this.posteDeviceService.fetchAll(posteId).subscribe(res => {
      this.devicesMap.update(map => ({ ...map, [posteId]: res.body ?? [] }));
    });
  }

  protected deviceTypeLabel(type: string): string {
    switch (type) {
      case "SCANNER":
        return "Scanner";
      case "DISPLAY":
        return "Afficheur";
      case "PRINTER":
        return "Imprimante";
      default:
        return type;
    }
  }

  protected deviceTypeSeverity(type: string): "info" | "success" | "warn" | "danger" | "secondary" {
    switch (type) {
      case "SCANNER":
        return "info";
      case "DISPLAY":
        return "warn";
      case "PRINTER":
        return "success";
      default:
        return "secondary";
    }
  }

  private applyFilter(): void {
    if (!this.searchQuery) {
      this.filteredEntities = [...this.entites];
    } else {
      this.filteredEntities = this.entites.filter(
        entity =>
          entity.name?.toLowerCase().includes(this.searchQuery) ||
          entity.address?.toLowerCase().includes(this.searchQuery) ||
          entity.posteNumber?.toLowerCase().includes(this.searchQuery)
      );
    }
  }

  private onSuccess(data: IPoste[] | null): void {
    this.entites = data || [];
    this.filteredEntities = [...this.entites];
    this.loading = false;
  }

  private onError(error: HttpErrorResponse): void {
    this.loading = false;
    this.notificationService.error(this.errorService.getErrorMessage(error));
  }
}
