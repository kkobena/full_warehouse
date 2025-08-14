import { Component, inject, OnDestroy, OnInit, viewChild } from '@angular/core';
import { Card } from 'primeng/card';
import { ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { Button } from 'primeng/button';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { CommonModule } from '@angular/common';
import { Select } from 'primeng/select';
import { IMagasin, IStorage } from '../../../shared/model/magasin.model';
import { IRayon } from '../../../shared/model/rayon.model';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../../shared/error.service';
import { SpinerService } from '../../../shared/spiner.service';
import { RayonService } from '../rayon.service';
import { Observable, Subject } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { IResponseDto } from '../../../shared/util/response-dto';
import { finalize, takeUntil } from 'rxjs/operators';
import { MagasinService } from '../../magasin/magasin.service';
import { StorageService } from '../../storage/storage.service';
import { Storage } from '../../storage/storage.model';

@Component({
  selector: 'jhi-clone-form',
  imports: [Card, ReactiveFormsModule, Button, ToastAlertComponent, CommonModule, Select],
  templateUrl: './clone-form.component.html',
  styleUrls: ['../../common-modal.component.scss'],
})
export class CloneFormComponent implements OnInit, OnDestroy {
  rayons: IRayon[] = [];
  protected magasins: IMagasin[] = [];
  protected magasin: IMagasin | null = null;
  protected storages: IStorage[] = [];
  protected fb = inject(UntypedFormBuilder);
  protected isSaving = false;
  protected editForm = this.fb.group({
    storageId: [null, [Validators.required]],
    magasinId: [null, [Validators.required]],
  });
  private readonly activeModal = inject(NgbActiveModal);
  private readonly errorService = inject(ErrorService);
  private readonly spinner = inject(SpinerService);
  private readonly entityService = inject(RayonService);
  private readonly magasinService = inject(MagasinService);
  private readonly storageService = inject(StorageService);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private destroy$ = new Subject<void>();
  ngOnInit(): void {
    this.findUserMagasin();

    this.editForm
      .get('magasinId')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(value => {
        this.findMagsinStorage(value as number);
      });
  }
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  protected cancel(): void {
    this.activeModal.dismiss();
  }

  protected save(): void {
    this.isSaving = true;
    const storageId = this.editForm.get(['storageId']).value;
    this.onResponse(this.entityService.cloner(this.rayons, storageId));
  }

  private onResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result
      .pipe(finalize(() => this.spinner.hide()))
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: HttpResponse<IResponseDto>) => {
          this.activeModal.close(res.body);
        },
        error: err => {
          this.isSaving = false;
          this.alert().showError(this.errorService.getErrorMessage(err));
        },
      });
  }

  private findUserMagasin(): void {
    this.magasinService
      .fetchAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe((res: HttpResponse<IMagasin[]>) => {
        this.magasins = res.body;
      });
  }
  private findMagsinStorage(magasinId: number): void {
    this.storageService
      .fetchStorages({
        magasinId,
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe((res: HttpResponse<Storage[]>) => {
        this.storages = res.body;
      });
  }
}
