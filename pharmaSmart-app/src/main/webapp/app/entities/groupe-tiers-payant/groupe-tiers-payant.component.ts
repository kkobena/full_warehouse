import { Component, computed, inject, signal, viewChild } from "@angular/core";
import { IResponseDto } from "app/shared/util/response-dto";
import { IGroupeTiersPayant } from "app/shared/model/groupe-tierspayant.model";
import { RouterModule } from "@angular/router";
import { GroupeTiersPayantService } from "app/entities/groupe-tiers-payant/groupe-tierspayant.service";
import { HttpErrorResponse, HttpResponse } from "@angular/common/http";
import {
  FormGroupeTiersPayantComponent
} from "app/entities/groupe-tiers-payant/form-groupe-tiers-payant/form-groupe-tiers-payant.component";
import { ErrorService } from "app/shared/error.service";
import { Observable } from "rxjs";
import { ButtonModule } from "primeng/button";
import { RippleModule } from "primeng/ripple";
import { ToastModule } from "primeng/toast";
import { DialogModule } from "primeng/dialog";
import { FileUploadModule } from "primeng/fileupload";
import { ToolbarModule } from "primeng/toolbar";
import { TableModule } from "primeng/table";
import { InputTextModule } from "primeng/inputtext";
import { TooltipModule } from "primeng/tooltip";
import { FormsModule } from "@angular/forms";
import { InputIconModule } from "primeng/inputicon";
import { IconFieldModule } from "primeng/iconfield";
import { PanelModule } from "primeng/panel";
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import { showCommonModal } from "../sales/selling-home/sale-helper";
import { toObservable, toSignal } from "@angular/core/rxjs-interop";
import { finalize, switchMap } from "rxjs/operators";
import { FileUploadDialogComponent } from "./file-upload-dialog/file-upload-dialog.component";
import { SpinnerComponent } from "../../shared/spinner/spinner.component";
import { NgbConfirmDialogService } from "../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { NotificationService } from "../../shared/services/notification.service";

@Component({
  selector: "app-groupe-tiers-payant",
  templateUrl: "./groupe-tiers-payant.component.html",
  styleUrls: ["./group-tiers-payant.component.scss"],
  imports: [
    ButtonModule,
    RippleModule,
    ToastModule,
    DialogModule,
    FileUploadModule,
    ToolbarModule,
    TableModule,
    RouterModule,
    InputTextModule,
    TooltipModule,
    FormsModule,
    InputIconModule,
    IconFieldModule,
    PanelModule,
    SpinnerComponent
  ]
})
export class GroupeTiersPayantComponent {
  protected readonly search = signal("");
  protected readonly responsedto = signal<IResponseDto | null>(null);
  private readonly modalService = inject(NgbModal);
  private readonly entityService = inject(GroupeTiersPayantService);
  private readonly errorService = inject(ErrorService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly reload = signal(0);

  private readonly groupTiersPayantResult = toSignal(
    toObservable(this.reload).pipe(switchMap(() => this.entityService.query({ search: this.search() })))
  );

  protected readonly entites = computed(() => this.groupTiersPayantResult()?.body ?? []);
  protected readonly loading = computed(() => !this.groupTiersPayantResult());
  private readonly spinner = viewChild.required<SpinnerComponent>("spinner");

  onSearch(): void {
    this.reload.set(this.reload() + 1);
  }

  addGroupeTiersPayant(): void {
    showCommonModal(
      this.modalService,
      FormGroupeTiersPayantComponent,
      {
        entity: null,
        header: "FORMULAIRE DE CREATION DE GROUPE TIERS-PAYANT "
      },
      () => {
        this.reload.set(this.reload() + 1);
        this.notificationService.success("Groupe tiers-payant créé avec succès");
      },
      "xl"
    );
  }

  showFileDialog(): void {
    showCommonModal(
      this.modalService,
      FileUploadDialogComponent,
      {},
      result => {
        this.spinner().show();
        this.uploadFileResponse(this.entityService.uploadFile(result));
      },
      "xl"
    );
  }

  editGroupeTiersPayant(groupeTiersPyant: IGroupeTiersPayant): void {
    showCommonModal(
      this.modalService,
      FormGroupeTiersPayantComponent,
      {
        entity: groupeTiersPyant,
        header: "FORMULAIRE DE MODIFICATION DE GROUPE TIERS-PAYANT "
      },
      () => {
        this.reload.set(this.reload() + 1);
        this.notificationService.success("Groupe tiers-payant mis à jour avec succès");
      },
      "xl"
    );
  }

  delete(groupeTiersPyant: IGroupeTiersPayant): void {
    this.entityService.delete(groupeTiersPyant.id).subscribe({
      next: () => {
        this.reload.set(this.reload() + 1);
        this.notificationService.success("Groupe tiers-payant supprimé avec succès");
      },
      error: err => this.onSaveError(err)
    });
  }

  onConfirmDelete(groupeTiersPyant: IGroupeTiersPayant): void {
    this.confirmDialog.onConfirm(() => this.delete(groupeTiersPyant), "Suppression", "Êtes-vous sûr de vouloir supprimer ce groupe ?");
  }

  private uploadFileResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result.pipe(finalize(() => this.spinner().hide())).subscribe({
      next: res => this.onPocesCsvSuccess(res.body),
      error: err => this.onSaveError(err)
    });
  }

  private onPocesCsvSuccess(responseDto: IResponseDto | null): void {
    if (responseDto) {
      this.responsedto.set(responseDto);
    }
    this.reload.set(this.reload() + 1);
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.notificationService.error(this.errorService.getErrorMessage(error));
  }
}
