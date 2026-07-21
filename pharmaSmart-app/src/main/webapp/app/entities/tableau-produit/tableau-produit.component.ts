import { Component, inject, OnInit, ChangeDetectionStrategy } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router, RouterModule } from "@angular/router";
import { HttpResponse } from "@angular/common/http";
import { TableauProduitService } from "./tableau-produit.service";
import { IResponseDto } from "../../shared/util/response-dto";
import { ITableau } from "../../shared/model/tableau.model";
import { ButtonComponent, DataTableComponent, SelectableRowDirective } from "../../shared/ui";
import { NgbModal, NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import { FormTableauComponent } from "./form-tableau/form-tableau.component";
import { NgbConfirmDialogService } from "../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { NotificationService } from "../../shared/services/notification.service";

@Component({
  selector: "app-tableau-produit",
  templateUrl: "./tableau-produit.component.html",
  styleUrl: "./tableau-produit.component.scss",
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [FormsModule, RouterModule, ButtonComponent, DataTableComponent, SelectableRowDirective, NgbTooltip]
})
export class TableauProduitComponent implements OnInit {
  protected fileDialog?: boolean;
  protected responsedto!: IResponseDto;
  protected entites?: ITableau[];
  // `null` et non `undefined` : le `model()` de `app-data-table` ne transporte que
  // `T | T[] | null`, et la liaison bidirectionnelle doit pouvoir lui réécrire la valeur.
  protected selectedEl: ITableau | null = null;
  protected loading = false;
  protected displayDialog?: boolean;
  private readonly entityService = inject(TableauProduitService);
  private readonly router = inject(Router);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly ngModalService = inject(NgbModal);

  ngOnInit(): void {
    this.loadPage();
  }

  cancel(): void {
    this.displayDialog = false;
    this.fileDialog = false;
  }

  protected loadPage(): void {
    this.loading = true;
    this.entityService.query().subscribe({
      next: (res: HttpResponse<ITableau[]>) => this.onSuccess(res.body),
      error: () => this.onError()
    });
  }


  protected onConfirmDialog(id: number): void {
    this.confirmDialog.onConfirm(
      () => {
        this.entityService.delete(id).subscribe(() => {
          this.loadPage();
        });
      },
      "Confirmation",
      "Voulez-vous supprimer cet enregistrement ?"
    );
  }

  protected addNewEntity(): void {
    const modalRef = this.ngModalService.open(FormTableauComponent, {
      backdrop: "static",
      size: "lg",
      centered: true
    });
    modalRef.componentInstance.entity = null;
    modalRef.componentInstance.header = "Ajouter un tableau";
    modalRef.result.then(r => {
      this.loadPage();
    });
    this.displayDialog = true;
  }

  protected onEdit(entity: ITableau): void {
    const modalRef = this.ngModalService.open(FormTableauComponent, {
      backdrop: "static",
      size: "lg",
      centered: true
    });
    modalRef.componentInstance.entity = entity;
    modalRef.componentInstance.header = `Modifier un tableau [ ${entity.code} ]`;
    modalRef.result.then(r => {
      this.loadPage();
    });
    this.displayDialog = true;
  }

  protected delete(entity: ITableau): void {
    if (entity && entity.id) {
      this.confirmDelete(entity.id);
    }
  }

  protected confirmDelete(id: number): void {
    this.onConfirmDialog(id);
  }

  protected showFileDialog(): void {
    this.fileDialog = true;
  }

  private onSuccess(data: ITableau[] | null): void {
    this.router.navigate(["/tableaux"]);
    this.entites = data || [];
    this.loading = false;
  }

  private onError(): void {
    this.loading = false;
    this.notificationService.error("Erreur lors du chargement des données");
  }
}
