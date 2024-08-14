import { Component, inject, OnInit } from '@angular/core';
import { ConfigurationService } from '../../shared/configuration.service';
import { Configuration, IConfiguration } from '../../shared/model/configuration.model';
import { DatePipe } from '@angular/common';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import ItemCountComponent from '../../shared/pagination/item-count.component';
import { NgbPagination } from '@ng-bootstrap/ng-bootstrap';
import { PanelModule } from 'primeng/panel';
import { SortByDirective, SortDirective } from '../../shared/sort';
import TranslateDirective from '../../shared/language/translate.directive';
import { AlertErrorComponent } from '../../shared/alert/alert-error.component';
import { ButtonDirective } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { Ripple } from 'primeng/ripple';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';

@Component({
  selector: 'jhi-parametre',
  standalone: true,
  imports: [
    DatePipe,
    FaIconComponent,
    ItemCountComponent,
    NgbPagination,
    PanelModule,
    SortByDirective,
    SortDirective,
    TranslateDirective,
    AlertErrorComponent,
    ButtonDirective,
    DialogModule,
    InputTextareaModule,
    ReactiveFormsModule,
    Ripple,
    CheckboxModule,
    FormsModule,
    InputTextModule,
  ],
  templateUrl: './parametre.component.html',
  styles: ``,
})
export class ParametreComponent implements OnInit {
  apps: IConfiguration[] = [];
  configurationService = inject(ConfigurationService);
  fb = inject(UntypedFormBuilder);
  search = '';
  editForm = this.fb.group({
    name: [Validators.required],
    description: [null, [Validators.required]],
    value: [null, [Validators.required]],
  });
  displayDialog = false;
  entity: IConfiguration = null;
  isSaving = false;

  constructor() {}

  ngOnInit(): void {
    this.loadAll();
  }

  cancel(): void {
    this.displayDialog = false;
  }

  setActive(app: Configuration, isActivated: boolean): void {
    const value = isActivated ? '1' : '0';
    this.configurationService
      .update({
        ...app,
        value,
      })
      .subscribe(() => this.loadAll());
  }

  updateForm(entity: IConfiguration): void {
    const value = entity.valueType === 'BOOLEAN' ? entity.value === '1' : entity.value;
    this.editForm.patchValue({
      value,
      name: entity.name,
      description: entity.description,
    });
  }

  save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();
    this.subscribeToSaveResponse(this.configurationService.update(entity));
  }

  onEdit(entity: IConfiguration): void {
    this.entity = entity;
    this.updateForm(entity);
    this.displayDialog = true;
  }

  loadAll(): void {
    this.configurationService.query({ search: this.search }).subscribe(res => {
      this.apps = res.body || [];
    });
  }

  subscribeToSaveResponse(result: Observable<HttpResponse<{}>>): void {
    result.subscribe({
      next: () => {
        this.isSaving = false;
        this.loadAll();
        this.displayDialog = false;
      },
      error: () => {
        this.isSaving = false;
      },
    });
  }

  private createFromForm(): IConfiguration {
    return {
      ...this.entity,
      description: this.editForm.get(['description'])!.value,
      value: this.editForm.get(['value'])!.value,
    };
  }
}