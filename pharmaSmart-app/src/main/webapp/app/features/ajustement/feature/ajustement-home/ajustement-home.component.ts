import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  inject,
  OnInit,
  signal
} from '@angular/core';
import {takeUntilDestroyed, toObservable} from '@angular/core/rxjs-interop';
import {CommonModule} from '@angular/common';
import {Router, RouterModule} from '@angular/router';
import {FormsModule} from '@angular/forms';
import {filter} from 'rxjs';
import {NgxSpinnerService} from 'ngx-spinner';
import {NotificationService} from '../../../../shared/services/notification.service';

import {NgbDateStruct, NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {AjustementFacade} from '../../data-access/facades/ajustement.facade';
import {AjustEvent} from '../../models';
import {IAjust} from '../../../../shared/model/ajust.model';
import {IAjustement} from '../../../../shared/model/ajustement.model';
import {IUser, User} from '../../../../core/user/user.model';
import {UserService} from '../../../../core/user/user.service';
import {HttpResponse} from '@angular/common/http';
import {ITEMS_PER_PAGE} from '../../../../config/pagination.constants';
import {APPEND_TO} from '../../../../shared/constants/pagination.constants';
import {
  PharmaDatePickerComponent
} from '../../../../shared/date-picker/pharma-date-picker.component';
import {TauriPrinterService} from '../../../../shared/services/tauri-printer.service';
import {handleBlobForTauri} from '../../../../shared/util/tauri-util';
import {NGB_DATE_TO_ISO, TODAY_NGB_DATE} from '../../../../shared/util/warehouse-util';
import {
  AppTableLazyLoadEvent,
  BadgeComponent,
  ButtonComponent,
  CardComponent,
  DataTableComponent,
  IconFieldComponent,
  PillSelectorComponent,
  RowTogglerDirective,
  SelectComponent,
  ToolbarComponent
} from '../../../../shared/ui';

@Component({
  selector: 'app-ajustement-home',
  templateUrl: './ajustement-home.component.html',
  styleUrl: './ajustement-home.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ButtonComponent,
    DataTableComponent,
    BadgeComponent,
    NgbTooltip,
    IconFieldComponent,
    SelectComponent,
    ToolbarComponent,
    PillSelectorComponent,
    PharmaDatePickerComponent,
    RowTogglerDirective,
    CardComponent
  ]
})
export class AjustementHomeComponent implements OnInit {
  readonly facade = inject(AjustementFacade);
  protected readonly appendTo = APPEND_TO;
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  // Filters
  protected search = '';
  protected fromDate: NgbDateStruct = TODAY_NGB_DATE();
  protected toDate: NgbDateStruct = TODAY_NGB_DATE();
  protected user: IUser | null = {id: null, abbrName: 'TOUT'};
  protected users: IUser[] = [];
  protected typeFilter = 'TOUT';
  protected readonly typeOptions = [
    {label: 'Tout', value: 'TOUT'},
    {label: 'Entrées', value: 'AJUSTEMENT_IN'},
    {label: 'Sorties', value: 'AJUSTEMENT_OUT'},
  ];
  // Pagination — offset (0-based), piloté par le paginateur commun de `app-data-table`.
  protected first = signal(0);
  private readonly userService = inject(UserService);
  private readonly spinner = inject(NgxSpinnerService);
  private readonly tauriPrinter = inject(TauriPrinterService);
  private readonly router = inject(Router);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private lastEvent$ = toObservable(this.facade.lastEvent).pipe(
    filter((e): e is AjustEvent => e !== null),
  );

  constructor() {
    effect(() => {
      const err = this.facade.error();
      if (err) {
        this.notificationService.error(err, 'Erreur');
      }
    });
  }

  ngOnInit(): void {
    this.loadUsers();
    this.load();
    this.lastEvent$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(event => {
      if (event.type === 'HISTORY_LOADED') {
        // no-op, table updates via signal
      }
    });
  }

  protected onSearch(): void {
    this.first.set(0);
    this.load();
  }

  protected onLazyLoad(_event: AppTableLazyLoadEvent): void {
    // `[(first)]` a déjà mis à jour `this.first` avant l'émission de `(onLazyLoad)`.
    this.load();
  }

  protected goToNew(): void {
    this.router.navigate(['/features-ajustement/new']);
  }

  protected exportPdf(ajust: IAjust): void {
    this.spinner.show();
    this.facade.exportToPdf(ajust.id!).subscribe({
      next: blob => {
        this.spinner.hide();
        if (this.tauriPrinter.isRunningInTauri()) {
          handleBlobForTauri(blob, 'ajustement');
        } else {
          window.open(URL.createObjectURL(blob));
        }
      },
      error: () => this.spinner.hide(),
    });
  }

  protected hasEntrees(lines: IAjustement[]): boolean {
    return lines.some(l => (l.qtyMvt ?? 0) >= 0);
  }

  protected hasSorties(lines: IAjustement[]): boolean {
    return lines.some(l => (l.qtyMvt ?? 0) < 0);
  }

  protected nbEntrees(lines: IAjustement[]): number {
    return lines.filter(l => (l.qtyMvt ?? 0) >= 0).length;
  }

  protected nbSorties(lines: IAjustement[]): number {
    return lines.filter(l => (l.qtyMvt ?? 0) < 0).length;
  }

  protected load(): void {
    this.facade.loadHistory({
      page: Math.floor(this.first() / this.itemsPerPage),
      size: this.itemsPerPage,
      fromDate: NGB_DATE_TO_ISO(this.fromDate),
      toDate: NGB_DATE_TO_ISO(this.toDate),
      userId: this.user?.id ?? null,
      search: this.search || null,
      type: this.typeFilter !== 'TOUT' ? this.typeFilter : null,
    });
  }

  private loadUsers(): void {
    this.userService.query()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((res: HttpResponse<User[]>) => {
        this.users = [{id: null, abbrName: 'TOUT'}, ...(res.body ?? [])];
      });
  }
}
