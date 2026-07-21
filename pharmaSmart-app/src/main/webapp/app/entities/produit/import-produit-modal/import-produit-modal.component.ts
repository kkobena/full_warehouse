import { Component, inject, OnInit, viewChild, ChangeDetectionStrategy } from "@angular/core";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { ProduitService } from "../produit.service";
import { Observable } from "rxjs";
import { HttpErrorResponse, HttpResponse } from "@angular/common/http";
import { IResponseDto } from "../../../shared/util/response-dto";
import { IFournisseur } from "../../../shared/model/fournisseur.model";
import { NotificationService } from "../../../shared/services/notification.service";
import { ErrorService } from "../../../shared/error.service";
import { finalize } from "rxjs/operators";
import { SpinnerComponent } from "../../../shared/spinner/spinner.component";
import { FournisseurApiService } from "../../../features/partners/data-access/services/fournisseur-api.service";
import { ButtonComponent, CardComponent, FileUploadComponent, SelectComponent } from "../../../shared/ui";

@Component({
  selector: "jhi-import-produit-modal",
  imports: [
    FormsModule,
    ReactiveFormsModule,
    ButtonComponent,
    CardComponent,
    FileUploadComponent,
    SelectComponent,
    SpinnerComponent
  ],
  templateUrl: "./import-produit-modal.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ["../../common-modal.component.scss"]
})
export class ImportProduitModalComponent implements OnInit {
  type: string | null = null;
  fournisseurService = inject(FournisseurApiService);
  protected isSaving = false;
  protected title: string | null = null;
  protected fournisseurs: IFournisseur[] = [];
  protected accept = ".csv";
  protected file: File | null = null;
  protected selectedFournisseurId: number | null = null;
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly produitService = inject(ProduitService);
  private readonly spinner = viewChild.required<SpinnerComponent>("spinner");

  get isFileUploadValid(): boolean {
    return !!this.file && !this.isSaving && !!this.selectedFournisseurId;
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  onUpload(): void {
    this.spinner().show();
    this.isSaving = true;
    this.uploadFileResponse(this.produitService.uploadFile(this.buildFormData()));
  }

  ngOnInit(): void {
    if (this.type === "NOUVELLE_INSTALLATION") {
      this.title = "Nouvelle Installation";
    } else if (this.type === "BASCULEMENT") {
      this.title = "Basculement";
    } else {
      this.title = "Basculement de perstige";
    }
    this.fournisseurService
      .queryParents({
        page: 0,
        size: 9999
      })
      .subscribe((res: HttpResponse<IFournisseur[]>) => {
        this.fournisseurs = res.body || [];
      });
  }

  protected onFilesSelected(files: File[]): void {
    this.file = files[0] ?? null;
  }

  private buildFormData(): FormData {
    const file = this.file!;
    const formData: FormData = new FormData();
    const body = new Blob(
      [
        JSON.stringify({
          typeImportation: this.type,
          fournisseurId: this.selectedFournisseurId
        })
      ],
      {
        type: "application/json"
      }
    );
    formData.append("data", body);
    formData.append("fichier", file, file.name);
    return formData;
  }

  private uploadFileResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result
      .pipe(
        finalize(() => {
          this.spinner().hide();
          this.isSaving = false;
        })
      )
      .subscribe({
        next: (res: HttpResponse<IResponseDto>) => this.onPocesCsvSuccess(res.body),
        error: err => this.onSaveError(err)
      });
  }

  private onPocesCsvSuccess(responseDto: IResponseDto | null): void {
    this.activeModal.close(responseDto);
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.spinner().hide();
    this.isSaving = false;
    this.notificationService.error(this.errorService.getErrorMessage(error));
  }
}
