import { HttpErrorResponse, HttpResponse } from "@angular/common/http";
import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { RouterModule } from "@angular/router";
import { PosteService } from "./poste.service";
import { IPoste } from "../../../../shared/model/poste.model";
import { FormsModule } from "@angular/forms";
import { NgbModal, NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import { ErrorService } from "../../../../shared/error.service";
import { showCommonModal } from "../../../../entities/sales/selling-home/sale-helper";
import { FormPosteComponent } from "./form-poste/form-poste.component";
import { PosteDeviceService } from "./poste-device.service";
import { IPosteDevice } from "../../../../shared/model/poste-device.model";
import { NgbConfirmDialogService } from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { NotificationService } from "../../../../shared/services/notification.service";
import { CommonModule } from "@angular/common";
import {
  BadgeComponent,
  ButtonComponent,
  DataTableComponent,
  IconFieldComponent,
  RowTogglerDirective,
  ToolbarComponent
} from "../../../../shared/ui";

@Component({
  selector: "app-poste",
  templateUrl: "./poste.component.html",
  styleUrls: ["./poste.component.scss"],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    ButtonComponent,
    ToolbarComponent,
    DataTableComponent,
    RouterModule,
    FormsModule,
    IconFieldComponent,
    RowTogglerDirective,
    BadgeComponent,
    NgbTooltip
  ]
})
export class PosteComponent implements OnInit {
  protected loading?: boolean;
  protected entites: IPoste[] = [];
  protected filteredEntities: IPoste[] = [];
  protected searchQuery = "";
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

  protected onRowExpand(poste: IPoste): void {
    const posteId = poste.id;
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
