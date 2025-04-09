import { Component, inject, input, OnInit } from '@angular/core';
import { Suggestion } from './model/suggestion.model';
import { SuggestionService } from './suggestion.service';
import { ConfirmationService, LazyLoadEvent, PrimeTemplate } from 'primeng/api';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { SuggestionLine } from './model/suggestion-line.model';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { acceptButtonProps, rejectButtonProps } from '../../../shared/util/modal-button-props';
import { DialogService } from 'primeng/dynamicdialog';
import { Button } from 'primeng/button';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TableModule } from 'primeng/table';
import { Tooltip } from 'primeng/tooltip';
import { Keys } from '../../../shared/model/keys.model';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { EtaProduitComponent } from '../../../shared/eta-produit/eta-produit.component';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { ToolbarModule } from 'primeng/toolbar';

@Component({
  selector: 'jhi-edit-suggestion',
  imports: [
    Button,
    CommonModule,
    RouterModule,
    PrimeTemplate,
    TableModule,
    Tooltip,
    NgxSpinnerModule,
    ConfirmDialogModule,
    EtaProduitComponent,
    IconField,
    InputIcon,
    InputTextModule,
    ToolbarModule,
  ],
  templateUrl: './edit-suggestion.component.html',
  providers: [ConfirmationService, DialogService],
  styles: ``,
})
export class EditSuggestionComponent implements OnInit {
  readonly suggestion = input<Suggestion>(null);
  private readonly suggestionService = inject(SuggestionService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly spinner = inject(NgxSpinnerService);
  protected search: string = '';
  protected page = 0;
  protected selections: SuggestionLine[] = [];
  protected index = 0;
  protected totalItems = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected loading!: boolean;
  protected suggestions: SuggestionLine[] = [];
  protected ngbPaginationPage = 1;
  private buildParameters(): any {
    return {
      search: this.search,
      suggestionId: this.suggestion()?.id,
    };
  }
  sanitize(): void {
    this.spinner.show();
    this.suggestionService.sanitize(this.suggestion()?.id).subscribe({
      next: () => {
        this.onSearch();
        this.spinner.hide();
      },
      error: err => {
        this.spinner.hide();
        this.onCommonError(err);
      },
    });
  }
  ngOnInit(): void {
    this.onSearch();
  }
  deleteAll(): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous supprimer toutes les lignes ?',
      header: ' SUPPRESSION',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () =>
        this.onDelete({
          ids: this.suggestions.map(suggestion => suggestion.id),
        }),
    });
  }
  previousState(): void {
    window.history.back();
  }
  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.suggestionService
      .queryItems({
        page: pageToLoad,
        size: this.itemsPerPage,
        ...this.buildParameters(),
      })
      .subscribe({
        next: (res: HttpResponse<SuggestionLine[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }
  onSearch(search: string = ''): void {
    this.search = search;
    if (this.index == 0) {
      this.loadPage(0);
    }
  }
  private onDelete(ids: Keys): void {
    this.spinner.show();
    this.suggestionService.deleteItem(ids).subscribe({
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
  private onCommonError(error: any): void {}
  delete(id: number): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous supprimer cete suggestions  ?',
      header: ' SUPPRESSION',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () =>
        this.onDelete({
          ids: [id],
        }),
    });
  }
  protected lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.suggestionService
        .queryItems({
          page: this.page,
          size: event.rows,
          ...this.buildParameters(),
        })
        .subscribe({
          next: (res: HttpResponse<SuggestionLine[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError(),
        });
    }
  }

  private onSuccess(data: SuggestionLine[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.suggestions = data || [];
    this.loading = false;
  }

  private onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }
}
