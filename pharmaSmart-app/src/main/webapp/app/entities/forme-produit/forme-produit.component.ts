import {ChangeDetectionStrategy, Component, inject, OnInit} from '@angular/core';
import {HttpHeaders, HttpResponse} from '@angular/common/http';
import {FormeProduitService} from './forme-produit.service';
import {ITEMS_PER_PAGE} from '../../shared/constants/pagination.constants';
import {IFormProduit} from '../../shared/model/form-produit.model';
import {ButtonModule} from 'primeng/button';
import {ToolbarModule} from 'primeng/toolbar';
import {TableLazyLoadEvent, TableModule} from 'primeng/table';
import {Tooltip} from 'primeng/tooltip';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {InputText} from 'primeng/inputtext';
import {showCommonModal} from '../sales/selling-home/sale-helper';
import {FormFormeProduitComponent} from './form-forme-produit/form-forme-produit.component';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {
  NgbConfirmDialogService
} from "../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";

@Component({
  selector: 'app-forme-produit',
  templateUrl: './forme-produit.component.html',
  styleUrl: './forme-produit.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [ButtonModule, ToolbarModule, TableModule, Tooltip, IconField, InputIcon, InputText],
})
export class FormeProduitComponent implements OnInit {
  protected entites?: IFormProduit[];
  protected totalItems = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = 0;
  protected loading!: boolean;

  private readonly modalService = inject(NgbModal);
  private readonly entityService = inject(FormeProduitService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);

  ngOnInit(): void {
    this.loadPage();
  }

  protected search(event: any): void {
    this.loadPage(0, event.target.value);
  }

  protected loadPage(page?: number, search?: string): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.entityService
      .query({
        page: pageToLoad,
        size: this.itemsPerPage,
        search: search || null,
      })
      .subscribe({
        next: (res: HttpResponse<IFormProduit[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }

  protected lazyLoading(event: TableLazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.entityService
        .query({
          page: this.page,
          size: event.rows,
        })
        .subscribe({
          next: (res: HttpResponse<IFormProduit[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError(),
        });
    }
  }

  protected addNewEntity(): void {
    showCommonModal(
      this.modalService,
      FormFormeProduitComponent,
      {
        entity: null,
        header: "Ajout d'une nouvelle forme de produit",
      },
      () => {
        this.loadPage(0);
      },
      'lg',
    );
  }

  protected onEdit(entity: IFormProduit): void {
    showCommonModal(
      this.modalService,
      FormFormeProduitComponent,
      {
        entity,
        header: 'Modification de ' + entity.libelle,
      },
      () => {
        this.loadPage(0);
      },
      'lg',
    );
  }

  protected delete(entity: IFormProduit): void {
    if (entity) {
      this.confirmDelete(entity.id);
    }
  }

  protected confirmDelete(id: number): void {
    this.confirm(id);
  }

  private confirm(id: number): void {
    this.confirmDialog.onConfirm(
      () => {
        this.entityService.delete(id).subscribe(() => {
          this.loadPage(0);
        });
      },
      'Suppression',
      'Êtes-vous sûr de vouloir supprimer ?',
    );
  }

  private onSuccess(data: IFormProduit[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.entites = data || [];
    this.loading = false;
  }

  private onError(): void {
    this.loading = false;
  }
}
