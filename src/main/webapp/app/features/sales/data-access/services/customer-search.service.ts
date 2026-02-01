import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map, catchError, debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { CustomerService } from '../../../../entities/customer/customer.service';
import { ICustomer } from '../../../../shared/model/customer.model';

/**
 * Service de recherche de clients pour les ventes
 * Encapsule la logique de recherche avec debounce et gestion d'erreurs
 */
@Injectable({ providedIn: 'root' })
export class CustomerSearchService {
  private readonly customerService = inject(CustomerService);

  /**
   * Recherche de clients par terme de recherche
   * @param query Terme de recherche (nom, prénom, téléphone, etc.)
   * @param size Nombre maximum de résultats
   */
  search(query: string, size: number = 10): Observable<ICustomer[]> {
    if (!query || query.trim().length === 0) {
      return of([]);
    }

    return this.customerService
      .query({ search: query.trim(), size })
      .pipe(
        map(response => response.body || []),
        catchError(error => {
          console.error('Error searching customers:', error);
          return of([]);
        })
      );
  }

  /**
   * Recherche de client par ID
   * @param id ID du client
   */
  findById(id: number): Observable<ICustomer | null> {
    return this.customerService.find(id).pipe(
      map(response => response.body),
      catchError(error => {
        console.error('Error finding customer:', error);
        return of(null);
      })
    );
  }

  /**
   * Crée un opérateur RxJS pour recherche avec debounce
   * Utile pour les champs de recherche en temps réel
   */
  createDebouncedSearch(size: number = 10) {
    return (source: Observable<string>) =>
      source.pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap(query => this.search(query, size))
      );
  }
}
