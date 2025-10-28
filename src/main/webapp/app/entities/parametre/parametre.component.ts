import { Component, inject, OnInit } from '@angular/core';
import { ConfigurationService } from '../../shared/configuration.service';
import { Configuration, IConfiguration } from '../../shared/model/configuration.model';
import { PanelModule } from 'primeng/panel';
import { ButtonModule } from 'primeng/button';
import { FormsModule } from '@angular/forms';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { showCommonModal } from '../sales/selling-home/sale-helper';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FormParamettreComponent } from './form-paramettre/form-paramettre.component';
import { Toolbar } from 'primeng/toolbar';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';

@Component({
  selector: 'jhi-parametre',
  imports: [
    PanelModule,
    CheckboxModule,
    FormsModule,
    InputTextModule,
    ButtonModule,
    Toolbar,
    IconField,
    InputIcon
  ],
  templateUrl: './parametre.component.html',
  styleUrls: ['./parametre.component.scss']
})
export class ParametreComponent implements OnInit {
  protected apps: IConfiguration[] = [];
  protected search = '';
  private readonly configurationService = inject(ConfigurationService);
  private readonly modalService = inject(NgbModal);

  ngOnInit(): void {
    this.loadAll();
  }

  protected setActive(app: Configuration, isActivated: boolean): void {
    const value = isActivated ? '1' : '0';
    this.configurationService
      .update({
        ...app,
        value
      })
      .subscribe(() => this.loadAll());
  }


  protected onEdit(entity: IConfiguration): void {
    showCommonModal(
      this.modalService,
      FormParamettreComponent,
      {
        entity: entity,
        header: 'Modification de [ ' + entity.name + ' ]'
      },
      () => {
        this.loadAll();
      },
      'lg'
    );
  }

  protected loadAll(): void {
    this.configurationService.query({ search: this.search }).subscribe(res => {
      this.apps = res.body || [];
    });
  }


}
