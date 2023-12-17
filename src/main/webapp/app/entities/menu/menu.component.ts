import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { MenuDeleteDialogComponent } from './menu-delete-dialog.component';
import { PrivillegeService } from './privillege.service';
import { IAuthority } from '../../shared/model/authority.model';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'jhi-menu',
  templateUrl: './menu.component.html',
  providers: [MessageService],
})
export class MenuComponent implements OnInit {
  protected authorities?: IAuthority[];
  protected eventSubscriber?: Subscription;

  constructor(private messageService: MessageService, protected privillegeService: PrivillegeService, protected modalService: NgbModal) {}

  loadAll(): void {
    this.privillegeService.queryAuthorities().subscribe((res: HttpResponse<IAuthority[]>) => (this.authorities = res.body || []));
  }

  ngOnInit(): void {
    this.loadAll();
    this.registerChangeInMenus();
  }

  registerChangeInMenus(): void {
    this.loadAll();
  }

  delete(authority: IAuthority): void {
    const modalRef = this.modalService.open(MenuDeleteDialogComponent, {
      size: 'lg',
      backdrop: 'static',
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
        error: () => this.loadAll(),
      });
    }
  }
}
