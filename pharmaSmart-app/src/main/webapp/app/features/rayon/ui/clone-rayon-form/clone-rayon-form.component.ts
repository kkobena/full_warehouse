import {signal, Component, DestroyRef, inject, OnInit, ChangeDetectionStrategy } from "@angular/core";
import { ReactiveFormsModule, UntypedFormBuilder, Validators } from "@angular/forms";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { HttpResponse } from "@angular/common/http";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { finalize } from "rxjs/operators";
import { IMagasin } from "../../../../shared/model";
import { Storage } from "../../../../entities/storage/storage.model";
import { MagasinService } from "../../../../entities/magasin/magasin.service";
import { StorageService } from "../../../../entities/storage/storage.service";
import { NotificationService } from "../../../../shared/services/notification.service";
import { ErrorService } from "../../../../shared/error.service";
import { RayonApiService } from "../../data-access/services/rayon-api.service";
import { IRayon } from "../../models/rayon.model";
import { ButtonComponent, SelectComponent } from "../../../../shared/ui";

@Component({
  selector: "app-clone-rayon-form",
  templateUrl: "./clone-rayon-form.component.html",
  styleUrl: "./clone-rayon-form.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, ButtonComponent, SelectComponent]
})
export class CloneRayonFormComponent implements OnInit {
  rayons: IRayon[] = [];

  protected readonly magasins = signal([]);
  protected readonly storages = signal([]);
  protected readonly isSaving = signal(false);

  protected readonly editForm = inject(UntypedFormBuilder).group({
    magasinId: [null, [Validators.required]],
    storageId: [null, [Validators.required]]
  });

  private readonly activeModal = inject(NgbActiveModal);
  private readonly errorService = inject(ErrorService);
  private readonly notificationService = inject(NotificationService);
  private readonly rayonApi = inject(RayonApiService);
  private readonly magasinService = inject(MagasinService);
  private readonly storageService = inject(StorageService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.magasinService
      .fetchAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((res: HttpResponse<IMagasin[]>) => {
        this.magasins.set(res.body ?? []);
      });

    this.editForm
      .get("magasinId")!
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((magasinId: number) => {
        if (!magasinId) return;
        this.storageService
          .fetchStorages({ magasinId })
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe((res: HttpResponse<Storage[]>) => {
            this.storages.set(res.body ?? []);
            this.editForm.get("storageId")!.reset();
          });
      });
  }

  protected save(): void {
    if (this.editForm.invalid) return;
    this.isSaving.set(true);
    const storageId = this.editForm.get("storageId")!.value as number;
    this.rayonApi
      .cloner(this.rayons, storageId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => (this.isSaving.set(false)))
      )
      .subscribe({
        next: res => this.activeModal.close(res.body),
        error: err => this.notificationService.error(this.errorService.getErrorMessage(err))
      });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }
}
