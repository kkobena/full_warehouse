import { Component, inject, OnInit, ChangeDetectionStrategy } from "@angular/core";
import { HttpResponse } from "@angular/common/http";
import { Router, RouterModule } from "@angular/router";
import { IMagasin } from "../../shared/model";
import { MagasinService } from "../magasin/magasin.service";
import { NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import { AbilityService } from "../../core/auth/ability.service";
import { NgbConfirmDialogService } from "../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { NotificationService } from "../../shared/services/notification.service";
import { ButtonComponent, DataTableComponent, IconFieldComponent, ToolbarComponent } from "../../shared/ui";
@Component({
  selector: "app-depot",

  imports: [
    RouterModule,
    ButtonComponent,
    DataTableComponent,
    ToolbarComponent,
    NgbTooltip,
    IconFieldComponent
  ],
  templateUrl: "./depot.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
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
