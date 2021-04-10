import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { IMenu } from 'app/shared/model/menu.model';
import { MenuService } from './menu.service';
import { MenuDeleteDialogComponent } from './menu-delete-dialog.component';

@Component({
  selector: 'jhi-menu',
  templateUrl: './menu.component.html',
})
export class MenuComponent implements OnInit, OnDestroy {
  menus?: IMenu[];
  eventSubscriber?: Subscription;

  constructor(protected menuService: MenuService, protected eventManager: JhiEventManager, protected modalService: NgbModal) {}

  loadAll(): void {
    this.menuService.query().subscribe((res: HttpResponse<IMenu[]>) => (this.menus = res.body || []));
  }

  ngOnInit(): void {
    this.loadAll();
    this.registerChangeInMenus();
  }

  ngOnDestroy(): void {
    if (this.eventSubscriber) {
      this.eventManager.destroy(this.eventSubscriber);
    }
  }

  trackId(index: number, item: IMenu): number {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.id!;
  }

  registerChangeInMenus(): void {
    this.eventSubscriber = this.eventManager.subscribe('menuListModification', () => this.loadAll());
  }

  delete(menu: IMenu): void {
    const modalRef = this.modalService.open(MenuDeleteDialogComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.menu = menu;
  }
}
