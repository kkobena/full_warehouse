import { AfterViewInit, Component, ElementRef, inject, OnInit, Renderer2, viewChild } from "@angular/core";
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from "@angular/forms";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { ButtonModule } from "primeng/button";
import { InputTextModule } from "primeng/inputtext";
import { SelectModule } from "primeng/select";
import { FloatLabelModule } from "primeng/floatlabel";
import { ToggleSwitch } from "primeng/toggleswitch";
import { HttpResponse } from "@angular/common/http";
import { finalize } from "rxjs/operators";
import { MagasinService } from "../../../../entities/magasin/magasin.service";
import { StorageService } from "../../../../entities/storage/storage.service";
import { Storage } from "../../../../entities/storage/storage.model";
import { ToastAlertComponent } from "../../../../shared/toast-alert/toast-alert.component";
import { ErrorService } from "../../../../shared/error.service";
import { RayonApiService } from "../../data-access/services/rayon-api.service";
import { IRayon, TYPE_ZONE_OPTIONS } from "../../models/rayon.model";
import { Toast } from "primeng/toast";
import { NotificationService } from "../../../../shared/services/notification.service";

@Component({
  selector: "app-rayon-form",
  templateUrl: "./rayon-form.component.html",
  styleUrl: "./rayon-form.component.scss",
  imports: [
    FormsModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    SelectModule,
    FloatLabelModule,
    ToggleSwitch,
    Toast
  ]
})
export class RayonFormComponent implements OnInit, AfterViewInit {
  header = "";
  entity: IRayon | null = null;

  protected storages: Storage[] = [];
  protected isSaving = false;
  protected readonly typeZoneOptions = TYPE_ZONE_OPTIONS;

  protected readonly fb = inject(UntypedFormBuilder);
  protected readonly editForm = this.fb.group({
    id: [],
    code: [null, [Validators.required]],
    storageId: [null, [Validators.required]],
    libelle: [null, [Validators.required]],
    typeZone: [null],
    position: [null],
    exclude: [false]
  });
  private readonly notificationService = inject(NotificationService);
  private readonly rayonApi = inject(RayonApiService);
  private readonly magasinService = inject(MagasinService);
  private readonly storageService = inject(StorageService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly errorService = inject(ErrorService);
  private readonly libelleInput = viewChild.required<ElementRef>("libelleInput");
  private readonly renderer = inject(Renderer2);
  private readonly elementRef = inject(ElementRef);

  ngOnInit(): void {
    this.loadStorages();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.libelleInput().nativeElement.focus(), 100);
  }

  protected onDropdownShow(event: any): void {
    const modalBody = this.elementRef.nativeElement.querySelector(".modal-body");
    if (modalBody) {
      this.renderer.addClass(modalBody, "overflow-visible");
    }
  }

  protected onDropdownHide(event: any): void {
    const modalBody = this.elementRef.nativeElement.querySelector(".modal-body");
    if (modalBody) {
      this.renderer.removeClass(modalBody, "overflow-visible");
    }
  }

  protected save(): void {
    if (this.editForm.invalid) return;
    this.isSaving = true;
    const rayon = this.buildFromForm();
    const call = rayon.id ? this.rayonApi.update(rayon) : this.rayonApi.create(rayon);
    call.pipe(finalize(() => (this.isSaving = false))).subscribe({
      next: (res: HttpResponse<IRayon>) => this.activeModal.close(res.body),
      error: err => this.notificationService.error(this.errorService.getErrorMessage(err))
    });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  private loadStorages(): void {
    this.magasinService.findCurrentUserMagasin().then(magasin => {
      this.storageService
        .fetchStorages({ magasinId: magasin.id })
        .subscribe((res: HttpResponse<Storage[]>) => {
          this.storages = res.body ?? [];
          if (this.entity) {
            this.patchForm(this.entity);
          } else {
            const principal = this.storages.find(s => s.type === "PRINCIPAL");
            this.editForm.get("storageId")?.setValue(principal?.id ?? null);
          }
        });
    });
  }

  protected get isSansRayon(): boolean {
    return this.entity?.code === 'SANS';
  }

  private patchForm(rayon: IRayon): void {
    this.editForm.patchValue({
      id: rayon.id,
      code: rayon.code,
      libelle: rayon.libelle,
      storageId: rayon.storageId,
      typeZone: rayon.typeZone ?? null,
      position: rayon.position ?? null,
      exclude: rayon.exclude ?? false
    });
    if (rayon.code === 'SANS') {
      this.editForm.get('code')?.disable();
    }
  }

  private buildFromForm(): IRayon {
    return {
      id: this.editForm.get("id")?.value ?? undefined,
      code: this.editForm.get("code")?.value ?? this.entity?.code,
      libelle: this.editForm.get("libelle")?.value,
      storageId: this.editForm.get("storageId")?.value,
      typeZone: this.editForm.get("typeZone")?.value ?? undefined,
      position: this.editForm.get("position")?.value ?? undefined,
      exclude: this.editForm.get("exclude")?.value ?? false
    };
  }
}
