import { Component, inject, OnInit, signal, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { NgbModal, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { SpinnerComponent } from '../../../../shared/spinner/spinner.component';
import { FileUploadDialogComponent } from '../../../../entities/groupe-tiers-payant/file-upload-dialog/file-upload-dialog.component';
import { ErrorService } from '../../../../shared/error.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { NgbConfirmDialogService } from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { ITEMS_PER_PAGE } from '../../../../shared/constants/pagination.constants';
import { IFournisseur } from '../../../../shared/model/fournisseur.model';
import { IResponseDto } from '../../../../shared/util/response-dto';
import { FournisseurApiService } from '../../data-access/services/fournisseur-api.service';
import { FournisseurFormComponent } from '../../ui/fournisseur-form/fournisseur-form.component';
import { CommonModule } from "@angular/common";
import {
  AppTableLazyLoadEvent,
  BadgeComponent,
  ButtonComponent,
  DataTableComponent,
  FrozenColumnDirective,
  IconFieldComponent,
  RowTogglerDirective,
  ToolbarComponent
} from '../../../../shared/ui';

@Component({
  selector: 'app-fournisseur-home',
  templateUrl: './fournisseur-home.component.html',
  styleUrls: ['./fournisseur-home.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    ToolbarComponent,
    DataTableComponent,
    FrozenColumnDirective,
    RowTogglerDirective,
    IconFieldComponent,
    BadgeComponent,
    NgbTooltip,
    SpinnerComponent,
  ],
})
export class FournisseurHomeComponent implements OnInit {
  protected fournisseurs: IFournisseur[] = [];
  protected totalItems = 0;
  protected page = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected loading = signal(false);
  protected search = '';

  protected readonly agencesMap = signal(new Map<number, IFournisseur[]>());
  protected readonly agencesLoadingIds = signal(new Set<number>());

  private readonly api = inject(FournisseurApiService);
  private readonly modalService = inject(NgbModal);
  private readonly notif = inject(NotificationService);
  private readonly confirmService = inject(NgbConfirmDialogService);
  private readonly errorService = inject(ErrorService);
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');

  ngOnInit(): void {
    this.loadPage();
  }

  protected loadPage(page = 0): void {
    this.page = page;
    this.loading.set(true);
    this.api.queryParents({ page, size: this.itemsPerPage, search: this.search })
      .subscribe({
        next: (res: HttpResponse<IFournisseur[]>) => this.onSuccess(res.body, res.headers, page),
        error: err => this.onError(err),
      });
  }

  protected lazyLoading(event: AppTableLazyLoadEvent): void {
    const page = Math.floor((event.first ?? 0) / (event.rows ?? this.itemsPerPage));
    this.page = page;
    this.itemsPerPage = event.rows ?? this.itemsPerPage;
    this.loading.set(true);
    this.api.queryParents({ page, size: this.itemsPerPage, search: this.search })
      .subscribe({
        next: (res: HttpResponse<IFournisseur[]>) => this.onSuccess(res.body, res.headers, page),
        error: err => this.onError(err),
      });
  }

  protected onRowExpand(f: IFournisseur): void {
    const id = f.id!;
    if (this.agencesMap().has(id)) return;
    this.agencesLoadingIds.update(s => new Set(s).add(id));
    this.api.findAgences(id).subscribe({
      next: agences => {
        this.agencesMap.update(m => new Map(m).set(id, agences));
        this.agencesLoadingIds.update(s => { const n = new Set(s); n.delete(id); return n; });
      },
      error: () => this.agencesLoadingIds.update(s => { const n = new Set(s); n.delete(id); return n; }),
    });
  }

  protected openNewParent(): void {
    const ref = this.modalService.open(FournisseurFormComponent, { size: 'xl', backdrop: 'static' });
    ref.componentInstance.fournisseur = null;
    ref.componentInstance.presetParentId = null;
    ref.componentInstance.title = 'Nouveau fournisseur';
    ref.result.then(() => this.loadPage(0), () => {});
  }

  protected editFournisseur(f: IFournisseur): void {
    const ref = this.modalService.open(FournisseurFormComponent, { size: 'xl', backdrop: 'static' });
    ref.componentInstance.fournisseur = f;
    ref.componentInstance.presetParentId = null;
    ref.componentInstance.title = `Modifier ${f.libelle}`;
    ref.result.then(() => {
      this.loadPage(this.page);
      if (f.parentId) this.refreshAgences(f.parentId);
    }, () => {});
  }

  protected addAgence(parent: IFournisseur): void {
    const ref = this.modalService.open(FournisseurFormComponent, { size: 'xl', backdrop: 'static' });
    ref.componentInstance.fournisseur = null;
    ref.componentInstance.presetParentId = parent.id;
    ref.componentInstance.title = `Nouvelle agence — ${parent.libelle}`;
    ref.result.then(() => this.refreshAgences(parent.id!), () => {});
  }

  protected deleteParent(f: IFournisseur): void {
    this.confirmService.onConfirm(
      () => this.api.delete(f.id!).subscribe({
        next: () => this.loadPage(0),
        error: err => this.onError(err),
      }),
      'Confirmation',
      `Supprimer "${f.libelle}" ? Les agences rattachées seront également supprimées.`,
    );
  }

  protected deleteAgence(agence: IFournisseur, parentId: number): void {
    this.confirmService.onConfirm(
      () => this.api.delete(agence.id!).subscribe({
        next: () => this.refreshAgences(parentId),
        error: err => this.onError(err),
      }),
      'Confirmation',
      `Supprimer l'agence "${agence.libelle}" ?`,
    );
  }

  protected showFileDialog(): void {
    const ref = this.modalService.open(FileUploadDialogComponent, { size: 'xl' });
    ref.result.then(file => {
      this.spinner().show();
      this.api.uploadFile(file)
        .pipe(finalize(() => this.spinner().hide()))
        .subscribe({
          next: (res: HttpResponse<IResponseDto>) => {
            this.notif.info('Fichier importé avec succès');
            this.loadPage(0);
          },
          error: err => this.onError(err),
        });
    }, () => {});
  }

  protected agencesOf(parentId: number): IFournisseur[] {
    return this.agencesMap().get(parentId) ?? [];
  }

  protected isLoadingAgences(parentId: number): boolean {
    return this.agencesLoadingIds().has(parentId);
  }

  private refreshAgences(parentId: number): void {
    this.agencesMap.update(m => { const n = new Map(m); n.delete(parentId); return n; });
    this.agencesLoadingIds.update(s => new Set(s).add(parentId));
    this.api.findAgences(parentId).subscribe({
      next: agences => {
        this.agencesMap.update(m => new Map(m).set(parentId, agences));
        this.agencesLoadingIds.update(s => { const n = new Set(s); n.delete(parentId); return n; });
      },
      error: () => this.agencesLoadingIds.update(s => { const n = new Set(s); n.delete(parentId); return n; }),
    });
  }

  private onSuccess(data: IFournisseur[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.fournisseurs = data ?? [];
    this.loading.set(false);
  }

  private onError(err: any): void {
    this.loading.set(false);
    this.notif.error(this.errorService.getErrorMessage(err));
  }
}
