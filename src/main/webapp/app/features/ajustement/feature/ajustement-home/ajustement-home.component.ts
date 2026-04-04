import {Component, DestroyRef, effect, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed, toObservable} from '@angular/core/rxjs-interop';
import {CommonModule} from '@angular/common';
import {Router, RouterModule} from '@angular/router';
import {FormsModule} from '@angular/forms';
import {filter} from 'rxjs';
import moment from 'moment';
import {NgxSpinnerService} from 'ngx-spinner';
import {MessageService} from 'primeng/api';

import {ButtonModule} from 'primeng/button';
import {TableModule} from 'primeng/table';
import {TagModule} from 'primeng/tag';
import {TooltipModule} from 'primeng/tooltip';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {InputTextModule} from 'primeng/inputtext';
import {FloatLabel} from 'primeng/floatlabel';
import {Select} from 'primeng/select';
import {Toolbar} from 'primeng/toolbar';
import {SelectButton} from 'primeng/selectbutton';
import {Toast} from 'primeng/toast';

import {AjustementFacade} from '../../data-access/facades/ajustement.facade';
import {AjustEvent} from '../../models';
import {IAjust} from '../../../../shared/model/ajust.model';
import {IAjustement} from '../../../../shared/model/ajustement.model';
import {IUser, User} from '../../../../core/user/user.model';
import {UserService} from '../../../../core/user/user.service';
import {HttpResponse} from '@angular/common/http';
import {ITEMS_PER_PAGE} from '../../../../config/pagination.constants';
import {APPEND_TO} from '../../../../shared/constants/pagination.constants';
import {DatePickerComponent} from '../../../../shared/date-picker/date-picker.component';
import {TauriPrinterService} from '../../../../shared/services/tauri-printer.service';
import {handleBlobForTauri} from '../../../../shared/util/tauri-util';

@Component({
  selector: 'app-ajustement-home',
  templateUrl: './ajustement-home.component.html',
  styleUrl: './ajustement-home.component.scss',
  providers: [MessageService],
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ButtonModule,
    TableModule,
    TagModule,
    TooltipModule,
    IconField,
    InputIcon,
    InputTextModule,
    FloatLabel,
    Select,
    Toolbar,
    SelectButton,
    Toast,
    DatePickerComponent,
  ],
})
export class AjustementHomeComponent implements OnInit {
  readonly facade = inject(AjustementFacade);
  private readonly userService = inject(UserService);
  private readonly spinner = inject(NgxSpinnerService);
  private readonly tauriPrinter = inject(TauriPrinterService);
  private readonly router = inject(Router);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly appendTo = APPEND_TO;
  protected readonly itemsPerPage = ITEMS_PER_PAGE;

  // Filters
  protected search = '';
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();
  protected user: IUser | null = {id: null, abbrName: 'TOUT'};
  protected users: IUser[] = [];
  protected typeFilter = 'TOUT';
  protected readonly typeOptions = [
    {label: 'Tout', value: 'TOUT'},
    {label: 'Entrées', value: 'IN'},
    {label: 'Sorties', value: 'OUT'},
  ];

  // Pagination
  protected page = signal(1);

  private lastEvent$ = toObservable(this.facade.lastEvent).pipe(
    filter((e): e is AjustEvent => e !== null),
  );

  constructor() {
    effect(() => {
      const err = this.facade.error();
      if (err) {
        this.messageService.add({severity: 'error', summary: 'Erreur', detail: err, life: 5000});
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
    this.page.set(1);
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

  private loadUsers(): void {
    this.userService.query()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((res: HttpResponse<User[]>) => {
        this.users = [{id: null, abbrName: 'TOUT'}, ...(res.body ?? [])];
      });
  }

  protected load(): void {
    this.facade.loadHistory({
      page: this.page() - 1,
      size: this.itemsPerPage,
      fromDate: moment(this.fromDate).format('YYYY-MM-DD'),
      toDate: moment(this.toDate).format('YYYY-MM-DD'),
      userId: this.user?.id ?? null,
      search: this.search || null,
      type: this.typeFilter !== 'TOUT' ? this.typeFilter : null,
    });
  }
}
