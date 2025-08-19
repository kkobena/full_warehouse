import { Component, inject, input, OnDestroy, OnInit, output, viewChild } from '@angular/core';
import { SuggestionService } from './suggestion.service';
import { Suggestion } from './model/suggestion.model';
import { RouterModule } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../../shared/error.service';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';
import { ExpandMode } from '../commande-en-cours/commande-en-cours.component';
import { Keys } from '../../../shared/model/keys.model';
import { Button } from 'primeng/button';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Tooltip } from 'primeng/tooltip';
import { CommonModule } from '@angular/common';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { SpinnerComponent } from '../../../shared/spinner/spinner.component';

@Component({
  selector: 'jhi-suggestion',
  imports: [Button, CommonModule, RouterModule, TableModule, Tooltip, ConfirmDialogComponent, SpinnerComponent],
  templateUrl: './suggestion.component.html'
})
export class SuggestionComponent implements OnInit, OnDestroy {
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
  private destroy$ = new Subject<void>();
  private readonly suggestionService = inject(SuggestionService);
  private readonly errorService = inject(ErrorService);

  private readonly modalService = inject(NgbModal);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
   private readonly spinner = viewChild.required<SpinnerComponent>('spinner');

  constructor() {
    this.rowExpandMode = 'single';
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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
        ...this.buildParameters()
      })
      .subscribe({
        next: (res: HttpResponse<Suggestion[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError()
      });
  }

  deleteAll(): void {
    const ids = this.selections.map(e => e.id);
    const keys: Keys = new Keys();
    keys.ids = ids;
    this.onDelete(keys);
  }

  delete(suggestionId: number): void {

    this.confimDialog().onConfirm(() =>
      this.onDelete({
        ids: [suggestionId]
      }), 'Suppression', 'Êtes-vous sûr de vouloir supprimer ?');
  }

  sort(): string[] {
    return [this.predicate + ',' + (this.ascending ? 'asc' : 'desc'), 'updatedAt'];
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
      this.handleServiceCall(
        this.suggestionService.fusionner(keys),
        () => {
          this.selections = [];
          this.loadPage();
        },
        'fusionner-spinner'
      );
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

  lazyLoading(event: TableLazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.suggestionService
        .query({
          page: this.page,
          size: event.rows,
          ...this.buildParameters()
        })
        .subscribe({
          next: (res: HttpResponse<Suggestion[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError()
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
      typeSuggession: this.selectedtypeSuggession() ? this.selectedtypeSuggession() : null
    };
  }

  private onDelete(ids: Keys): void {
    this.spinner().show();
    this.suggestionService.delete(ids).subscribe({
      next: () => {
        this.selections = [];
        this.spinner().hide();
        this.loadPage();
      },
      error: error => {
        this.spinner().hide();
        this.onCommonError(error);
      }
    });
  }

  private onCommonError(error: any): void {
    if (error.error && error.error.status === 500) {
      this.openInfoDialog('Erreur applicative', 'alert alert-danger');
    } else {
      this.errorService
        .getErrorMessageTranslation(error.error.errorKey)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: translatedErrorMessage => {
            this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
          },
          error: () => this.openInfoDialog(error.error.title, 'alert alert-danger')
        });
    }
  }

  private openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true
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

  private handleServiceCall(observable: Observable<any>, successCallback: () => void, spinnerName: string): void {
    this.spinner().show();
    observable.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.spinner().hide();
        successCallback();
      },
      error: error => {
        this.spinner().hide();
        this.onCommonError(error);
      }
    });
  }
}
