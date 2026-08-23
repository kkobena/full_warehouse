import { AfterViewInit, Component, DestroyRef, ElementRef, inject, OnInit, signal, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { AppTableLazyLoadEvent, ButtonComponent, CardComponent, DataTableComponent, IconFieldComponent } from '../../../../shared/ui';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { ICustomer } from '../../../../shared/model';
import { CustomerService } from '../../../../entities/customer/customer.service';
import { ITEMS_PER_PAGE } from '../../../../shared/constants/pagination.constants';

/**
 * Modal de sélection de client assuré/carnet.
 * Supporte le preloading de la liste de clients et du searchTerm
 * depuis l'insurance-data-bar quand plusieurs résultats sont trouvés.
 */
@Component({
  selector: 'app-assured-customer-list-modal',
  imports: [CommonModule, FormsModule, ButtonComponent, DataTableComponent, NgbTooltip, IconFieldComponent, CardComponent],
  templateUrl: './assured-customer-list-modal.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ['./assured-customer-list-modal.component.scss'],
})
export class AssuredCustomerListModalComponent implements OnInit, AfterViewInit {
  /** Titre du modal - défini par le parent via componentInputs */
  headerLibelle = 'Sélection client assuré';

  /** Terme de recherche initial - peut être prérempli */
  protected readonly searchString = signal('');

  /** `showCommonModal` affecte les proprietes de l'instance : il ne peut pas ecrire dans un signal. */
  set search(valeur: string) {
    this.searchString.set(valeur ?? '');
  }

  /** Type de tiers payant (ASSURANCE ou CARNET) */
  typeTiersPayant = 'ASSURANCE';

  /** Liste de clients preloadés (si fournie par le parent) */
  protected readonly preloadedCustomers = signal<ICustomer[] | null>(null);

  set preloaded(valeur: ICustomer[] | null) {
    this.preloadedCustomers.set(valeur);
  }

  customers = signal<ICustomer[]>([]);
  loading = signal(false);
  totalItems = signal(0);
  selectedCustomer = signal<ICustomer | null>(null);
  itemsPerPage = ITEMS_PER_PAGE;

  protected searchInput = viewChild<ElementRef>('searchInput');

  private readonly activeModal = inject(NgbActiveModal);
  private readonly customerService = inject(CustomerService);
  private readonly destroyRef = inject(DestroyRef);
  private searchSubject$ = new Subject<string>();
  private readonly page = signal(0);

  ngOnInit(): void {
    // Debounce search input
    this.searchSubject$.pipe(debounceTime(400), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.loadPage(1);
    });

    // Si des clients sont preloadés, les afficher directement
    const precharges = this.preloadedCustomers();
    if (precharges && precharges.length > 0) {
      this.customers.set(precharges);
      this.totalItems.set(precharges.length);
    } else if (this.searchString()) {
      // Si un searchTerm est fourni sans preload, lancer la recherche
      this.loadPage(1);
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.searchInput()?.nativeElement.focus();
    }, 100);
  }

  protected onSearchInput(): void {
    // Dès que l'utilisateur tape, on passe en mode recherche serveur
    this.preloadedCustomers.set(null);
    this.searchSubject$.next(this.searchString() || '');
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.cancel();
    } else if (event.key === 'Enter' && this.selectedCustomer()) {
      this.onSelect(this.selectedCustomer()!);
    }
  }

  protected onRowSelect(customer: ICustomer): void {
    this.selectedCustomer.set(customer);
  }

  protected onDblClick(customer: ICustomer): void {
    this.onSelect(customer);
  }

  protected onSelect(customer: ICustomer): void {
    this.activeModal.close(customer);
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  protected clearSearch(): void {
    this.searchString.set('');
    this.onSearchInput();
  }

  protected loadPage(page?: number): void {
    const pageToLoad: number = page || this.page() || 1;
    this.loading.set(true);
    this.customerService
      .queryAssuredCustomer({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        search: this.searchString(),
        typeTiersPayant: this.typeTiersPayant,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res: HttpResponse<ICustomer[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }

  protected lazyLoading(event: AppTableLazyLoadEvent): void {
    if (event && !this.preloadedCustomers()) {
      this.page.set(event.first! / event.rows!);
      this.loading.set(true);
      this.customerService
        .queryAssuredCustomer({
          page: this.page(),
          size: event.rows,
          search: this.searchString(),
          typeTiersPayant: this.typeTiersPayant,
        })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (res: HttpResponse<ICustomer[]>) => this.onSuccess(res.body, res.headers, this.page()),
          error: () => this.onError(),
        });
    }
  }

  private onSuccess(data: ICustomer[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems.set(Number(headers.get('X-Total-Count')) || 0);
    this.page.set(page);
    this.customers.set(data || []);
    this.loading.set(false);
  }

  private onError(): void {
    this.loading.set(false);
  }
}
