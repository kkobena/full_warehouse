import { Component, inject, OnInit, viewChild } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Router, RouterModule } from '@angular/router';
import { IMagasin, TypeMagasin } from '../../shared/model/magasin.model';
import { MagasinService } from '../magasin/magasin.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { Tooltip } from 'primeng/tooltip';
import { Toolbar } from 'primeng/toolbar';
import { InputText } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';

@Component({
  selector: 'jhi-depot',
  standalone: true,
  imports: [
    WarehouseCommonModule,
    RouterModule,
    ButtonModule,
    TableModule,
    CardModule,
    TagModule,
    ConfirmDialogComponent,
    Tooltip,
    Toolbar,
    InputText,
    IconField,
    InputIcon,
  ],
  templateUrl: './depot.component.html',
  styleUrl: './depot.component.scss',
})
export class DepotComponent implements OnInit {
  protected depots: IMagasin[] = [];
  protected loading = false;
  protected readonly TypeMagasin = TypeMagasin;
  private magasinService = inject(MagasinService);
  private router = inject(Router);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
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
      },
    });
  }

  onEdit(depot: IMagasin): void {
    this.router.navigate(['/depot', depot.id, 'edit']);
  }

  onCreate(): void {
    this.router.navigate(['/depot', 'new']);
  }

  onNewVente(): void {
    this.router.navigate(['/depot', 'new-vente']);
  }

  onDelete(depot: IMagasin): void {
    this.confimDialog().onConfirm(
      () => {
        this.magasinService.delete(depot.id!).subscribe({
          next: () => {
            this.loadAll();
          },
        });
      },
      'Suppression du dépôt',
      'Etes-vous sûr de vouloir changer le dépôt ?',
      null,
    );
  }
}
