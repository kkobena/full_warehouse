import { Component, inject, OnInit } from "@angular/core";
import { HttpResponse } from "@angular/common/http";
import { Router, RouterModule } from "@angular/router";
import { IMagasin } from "../../shared/model";
import { MagasinService } from "../magasin/magasin.service";
import { ButtonModule } from "primeng/button";
import { TableModule } from "primeng/table";
import { CardModule } from "primeng/card";
import { TagModule } from "primeng/tag";
import { Tooltip } from "primeng/tooltip";
import { Toolbar } from "primeng/toolbar";
import { InputText } from "primeng/inputtext";
import { IconField } from "primeng/iconfield";
import { InputIcon } from "primeng/inputicon";
import { AbilityService } from "../../core/auth/ability.service";
import { NgbConfirmDialogService } from "../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { NotificationService } from "../../shared/services/notification.service";
import { Toast } from "primeng/toast";

@Component({
  selector: "app-depot",

  imports: [
    RouterModule,
    ButtonModule,
    TableModule,
    CardModule,
    TagModule,
    Tooltip,
    Toolbar,
    InputText,
    IconField,
    InputIcon,
    Toast
  ],
  templateUrl: "./depot.component.html",
  styleUrl: "./depot.component.scss"
})
export class DepotComponent implements OnInit {
  protected readonly ability = inject(AbilityService);
  protected readonly canNewVente = this.ability.canSignal("execute", "depot.liste-depots");
  protected readonly canCreate = this.ability.canSignal("create", "depot.liste-depots");
  protected readonly canEdit = this.ability.canSignal("edit", "depot.liste-depots");
  protected readonly canDelete = this.ability.canSignal("delete", "depot.liste-depots");
  protected readonly canReturn = this.ability.canSignal("access", "depot.retour-depot");
  protected depots: IMagasin[] = [];
  protected loading = false;
  private magasinService = inject(MagasinService);
  private router = inject(Router);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading = true;
    this.magasinService.fetchAllDepots().subscribe({
      next: (res: HttpResponse<IMagasin[]>) => {
        this.depots = res.body || [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.notificationService.error("Une erreur est survenue lors du chargement des dépôts.");
      }
    });
  }

  onEdit(depot: IMagasin): void {
    this.router.navigate(["/depot", depot.id, "edit"]);
  }

  onCreate(): void {
    this.router.navigate(["/depot", "new"]);
  }

  onNewVente(): void {
    this.router.navigate(["/sales-home", "vente-depot"]);
  }

  onDelete(depot: IMagasin): void {
    this.confirmDialog.onConfirm(
      () => {
        this.magasinService.delete(depot.id).subscribe({
          next: () => {
            this.notificationService.success("Le dépôt a été supprimé avec succès.");
            this.loadAll();
          }
        });
      },
      "Suppression du dépôt",
      "Etes-vous sûr de vouloir changer le dépôt ?",
      null
    );
  }
}
