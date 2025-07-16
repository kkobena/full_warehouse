import { Component, inject, input, OnInit, output } from '@angular/core';
import { SuggestionService } from './suggestion.service';
import { Suggestion } from './model/suggestion.model';
import { RouterModule } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../../shared/error.service';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { ConfirmationService, LazyLoadEvent, PrimeTemplate } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { acceptButtonProps, rejectButtonProps } from '../../../shared/util/modal-button-props';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';
import { ExpandMode } from '../commande-en-cours/commande-en-cours.component';
import { Keys } from '../../../shared/model/keys.model';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { Tooltip } from 'primeng/tooltip';
import { CommonModule } from '@angular/common';
import { ConfirmDialogModule } from 'primeng/confirmdialog';

@Component({
  selector: 'jhi-suggestion',
  providers: [ConfirmationService, DialogService],
  imports: [Button, CommonModule, RouterModule, PrimeTemplate, TableModule, Tooltip, NgxSpinnerModule, ConfirmDialogModule],
  templateUrl: './suggestion.component.html',
})
export class SuggestionComponent implements OnInit {
  readonly search = input('');
  readonly selectionLength = output<number>();
  readonly selectedtypeSuggession = input<string>('ALL');
  readonly fournisseurId = input<number>(null);
  protected suggestions: Suggestion[] = [];
  protected totalItems = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected predicate = 'updatedAt';
  protected ascending!: boolean;
  protected ngbPaginationPage = 1;
  protected index = 0;
  protected rowExpandMode: ExpandMode;
  protected loading!: boolean;
  protected page = 0;
  protected selections: Suggestion[];
  protected fileDialog = false;
  protected ref!: DynamicDialogRef;
  private readonly suggestionService = inject(SuggestionService);
  private readonly errorService = inject(ErrorService);
  private readonly spinner = inject(NgxSpinnerService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly modalService = inject(NgbModal);

  constructor() {
    this.rowExpandMode = 'single';
  }

  ngOnInit(): void {
    this.onSearch();
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.suggestionService
      .query({
        page: pageToLoad,
        size: this.itemsPerPage,
        ...this.buildParameters(),
      })
      .subscribe({
        next: (res: HttpResponse<Suggestion[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }

  deleteAll(): void {
    const ids = this.selections.map(e => e.id);
    const keys: Keys = new Keys();
    keys.ids = ids;
    this.onDelete(keys);
  }

  delete(suggestionId: number): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous supprimer cette suggestions ?',
      header: ' SUPPRESSION',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () =>
        this.onDelete({
          ids: [suggestionId],
        }),
      key: 'deleteSuggestion',
    });
  }

  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'updatedAt') {
      result.push('updatedAt');
    }
    return result;
  }

  fusionner(): void {
    const ids = this.selections.map(e => e.id);
    const keys: Keys = new Keys();
    keys.ids = ids;
    const fournisseursIdArray = this.selections.map(e => e.fournisseurId);
    const firstId = fournisseursIdArray[0];
    const isSameProviderFn = (currentValue: number) => currentValue === firstId;
    const isSameProvider = fournisseursIdArray.every(isSameProviderFn);
    if (!isSameProvider) {
      this.openInfoDialog('Veillez sélectionner des suggestion du même grossiste', 'alert alert-info');
    } else {
      this.spinner.show();
      this.suggestionService.fusionner(keys).subscribe({
        next: () => {
          this.spinner.hide();
          this.selections = [];
          this.loadPage();
        },
        error: error => {
          this.spinner.hide();
          this.onCommonError(error);
        },
      });
    }
  }

  sanitize(id: number): void {
    this.suggestionService.sanitize(id).subscribe(() => {
      this.loadPage();
    });
  }

  onSearch(): void {
    if (this.index == 0) {
      this.loadPage(0);
    }
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.suggestionService
        .query({
          page: this.page,
          size: event.rows,
          ...this.buildParameters(),
        })
        .subscribe({
          next: (res: HttpResponse<Suggestion[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError(),
        });
    }
  }

  protected selectAllClik(): void {
    this.selectionLength.emit(this.selections.length);
  }

  protected onRowSelected(): void {
    this.selectionLength.emit(this.selections.length);
  }

  protected onRowUnselect(): void {
    this.selectionLength.emit(this.selections.length);
  }

  private buildParameters(): any {
    return {
      sort: this.sort(),
      search: this.search(),
      fournisseurId: this.fournisseurId(),
      typeSuggession: this.selectedtypeSuggession() ? this.selectedtypeSuggession() : null,
    };
  }

  private onDelete(ids: Keys): void {
    this.spinner.show();
    this.suggestionService.delete(ids).subscribe({
      next: () => {
        this.selections = [];
        this.spinner.hide();
        this.loadPage();
      },
      error: error => {
        this.spinner.hide();
        this.onCommonError(error);
      },
    });
  }

  private onCommonError(error: any): void {
    if (error.error && error.error.status === 500) {
      this.openInfoDialog('Erreur applicative', 'alert alert-danger');
    } else {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe({
        next: translatedErrorMessage => {
          this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
        },
        error: () => this.openInfoDialog(error.error.title, 'alert alert-danger'),
      });
    }
  }

  private openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  private onSuccess(data: Suggestion[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.suggestions = data || [];
    this.loading = false;
  }

  private onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }
}
