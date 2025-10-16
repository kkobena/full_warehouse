import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { MenuDeleteDialogComponent } from './menu-delete-dialog.component';
import { PrivillegeService } from './privillege.service';
import { IAuthority } from '../../shared/model/authority.model';
import { MessageService } from 'primeng/api';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { TableModule } from 'primeng/table';
import { RouterModule } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { FormsModule } from '@angular/forms';
import { ToastModule } from 'primeng/toast';
import { PanelModule } from 'primeng/panel';
import { Authority } from '../../shared/constants/authority.constants';

@Component({
  selector: 'jhi-menu',
  templateUrl: './menu.component.html',
  styleUrl: './menu.component.scss',
  providers: [MessageService],
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    ToastModule,
    RippleModule,
    TableModule,
    RouterModule,
    InputTextModule,
    TooltipModule,
    FormsModule,
    PanelModule
  ]
})
export class MenuComponent implements OnInit {
  protected authorities?: IAuthority[];
  protected privillegeService = inject(PrivillegeService);
  protected modalService = inject(NgbModal);
  private readonly predefinedAuthorities: string[] = [
    Authority.ADMIN,
    Authority.USER,
    Authority.ROLE_CAISSIER,
    Authority.ROLE_VENDEUR,
    Authority.ROLE_RESPONSABLE_COMMANDE
  ];

  loadAll(): void {
    this.privillegeService.queryAuthorities().subscribe((res: HttpResponse<IAuthority[]>) => (this.authorities = res.body || []));
  }

  ngOnInit(): void {
    this.loadAll();
  }

  delete(authority: IAuthority): void {
    const modalRef = this.modalService.open(MenuDeleteDialogComponent, {
      size: 'lg',
      backdrop: 'static'
    });
    modalRef.componentInstance.authority = authority;
    modalRef.closed.subscribe({ complete: () => this.loadAll() });
  }

  onEdit(authority: IAuthority, evt: any): void {
    const libelle = evt.target.value;
    if (authority.libelle !== libelle && libelle !== '') {
      authority.libelle = libelle;
      this.privillegeService.create(authority).subscribe({
        next: () => this.loadAll(),
        error: () => this.loadAll()
      });
    }
  }

  protected isNotPredefined(authority: IAuthority): boolean {
    return !this.predefinedAuthorities.includes(authority.name);
  }
}
