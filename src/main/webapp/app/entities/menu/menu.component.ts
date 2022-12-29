import {Component, OnInit} from '@angular/core';
import {HttpResponse} from '@angular/common/http';
import {Subscription} from 'rxjs';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';

import {IMenu} from 'app/shared/model/menu.model';
import {MenuService} from './menu.service';
import {MenuDeleteDialogComponent} from './menu-delete-dialog.component';

@Component({
  selector: 'jhi-menu',
  templateUrl: './menu.component.html',
})
export class MenuComponent implements OnInit {
  menus?: IMenu[];
  eventSubscriber?: Subscription;

  constructor(protected menuService: MenuService, protected modalService: NgbModal) {
  }

  loadAll(): void {
    this.menuService.query().subscribe((res: HttpResponse<IMenu[]>) => (this.menus = res.body || []));
  }

  ngOnInit(): void {
    this.loadAll();
    this.registerChangeInMenus();
  }


  trackId(index: number, item: IMenu): number {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.id!;
  }

  registerChangeInMenus(): void {
    this.loadAll();
  }

  delete(menu: IMenu): void {
    const modalRef = this.modalService.open(MenuDeleteDialogComponent, {size: 'lg', backdrop: 'static'});
    modalRef.componentInstance.menu = menu;
  }
}
