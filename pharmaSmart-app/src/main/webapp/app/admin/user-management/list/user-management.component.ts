import {Component, computed, inject, OnInit, signal, ChangeDetectionStrategy} from "@angular/core";
import {ActivatedRoute, Router, RouterModule} from "@angular/router";
import {HttpHeaders, HttpResponse} from "@angular/common/http";
import {combineLatest} from "rxjs";
import {SortService, SortState, sortStateSignal} from "app/shared/sort";
import {SORT} from "app/config/navigation.constants";
import {AccountService} from "app/core/auth/account.service";
import {UserManagementService} from "../service/user-management.service";
import {User} from "../user-management.model";
import {ButtonModule} from "primeng/button";
import {RippleModule} from "primeng/ripple";
import {PanelModule} from "primeng/panel";
import {Toolbar} from "primeng/toolbar";
import {Tooltip} from "primeng/tooltip";
import {TableModule} from "primeng/table";
import {IconField} from "primeng/iconfield";
import {InputIcon} from "primeng/inputicon";
import {InputTextModule} from "primeng/inputtext";
import {CommonModule} from "@angular/common";
import {
  NgbConfirmDialogService
} from "../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import {NotificationService} from "../../../shared/services/notification.service";
@Component({
  selector: "jhi-user-mgmt",
  templateUrl: "./user-management.component.html",
  styleUrls: ["./user-management.component.scss"],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    ButtonModule,
    RippleModule,
    PanelModule,
    RouterModule,
    Toolbar,
    Tooltip,
    TableModule,
    IconField,
    InputIcon,
    InputTextModule
  ]
})
export default class UserManagementComponent implements OnInit {
  currentAccount = inject(AccountService).trackCurrentAccount();
  users = signal<User[] | null>(null);
  isLoading = signal(false);
  totalItems = signal(0);
  itemsPerPage = 50;
  page!: number;
  sortState = sortStateSignal({});
  filterQuery = signal("");

  filteredUsers = computed(() => {
    const q = this.filterQuery().toLowerCase().trim();
    const all = this.users() ?? [];
    if (!q) {
      return all;
    }
    return all.filter(u =>
      u.login?.toLowerCase().includes(q) ||
      u.firstName?.toLowerCase().includes(q) ||
      u.lastName?.toLowerCase().includes(q)
    );
  });

  private readonly userService = inject(UserManagementService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly sortService = inject(SortService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);

  ngOnInit(): void {
    this.handleNavigation();
  }

  setActive(user: User, isActivated: boolean): void {
    this.userService.update({...user, activated: isActivated}).subscribe(() => this.loadAll());
  }

  deleteUser(user: User): void {
    this.confirmDialog.onConfirm(
      () => this.userService.delete(user.login).subscribe({
        next: () => {
          this.loadAll();
          this.notificationService.success(`Utilisateur "${user.firstName} ${user.lastName}" supprimé.`, "Suppression réussie");
        },
        error: err => this.notificationService.error(`Impossible de supprimer l'utilisateur "${user.firstName} ${user.lastName}".`, "Erreur lors de la suppression")
      }),
      "Suppression",
      `Voulez-vous supprimer ce utilisateur "${user.firstName} ${user.lastName}" ?`
    );

  }


  loadAll(): void {
    this.isLoading.set(true);
    this.userService
      .query({
        page: this.page - 1,
        size: this.itemsPerPage,
        sort: this.sortService.buildSortParam(this.sortState(), "id")
      })
      .subscribe({
        next: (res: HttpResponse<User[]>) => {
          this.isLoading.set(false);
          this.onSuccess(res.body, res.headers);
        },
        error: () => this.isLoading.set(false)
      });
  }

  onPageChange(event: { first: number; rows: number }): void {
    this.page = Math.floor(event.first / event.rows) + 1;
    this.loadAll();
  }

  onFilter(event: Event): void {
    this.filterQuery.set((event.target as HTMLInputElement).value);
  }

  transition(sortState?: SortState): void {
    this.router.navigate(["./"], {
      relativeTo: this.activatedRoute.parent,
      queryParams: {
        page: this.page,
        sort: this.sortService.buildSortParam(sortState ?? this.sortState())
      }
    });
  }

  private handleNavigation(): void {
    combineLatest([this.activatedRoute.data, this.activatedRoute.queryParamMap]).subscribe(([data, params]) => {
      const page = params.get("page");
      this.page = +(page ?? 1);
      this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? data.defaultSort));
      this.loadAll();
    });
  }

  private onSuccess(users: User[] | null, headers: HttpHeaders): void {
    this.totalItems.set(Number(headers.get("X-Total-Count")));
    this.users.set(users);
  }
}
