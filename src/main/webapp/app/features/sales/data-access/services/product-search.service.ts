import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map, catchError, debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { ProduitService } from '../../../../entities/produit/produit.service';
import { ProduitSearch } from '../../../../shared/model/produit.model';

/**
 * Service de recherche de produits pour les ventes
 * Encapsule la logique de recherche avec debounce et gestion d'erreurs
 */
@Injectable({ providedIn: 'root' })
export class ProductSearchService {
  private readonly produitService = inject(ProduitService);

  /**
   * Recherche de produits par terme de recherche
   * @param query Terme de recherche (libellé, code-barres, référence)
   * @param size Nombre maximum de résultats
   * @param searchByStorage Si true, recherche uniquement dans le magasin actuel
   */
  search(query: string, size: number = 10, searchByStorage: boolean = true): Observable<ProduitSearch[]> {
    if (!query || query.trim().length === 0) {
      return of([]);
    }

    return this.produitService
      .search({ search: query.trim(), size }, searchByStorage)
      .pipe(
        map(response => response.body || []),
        catchError(error => {
          console.error('Error searching products:', error);
          return of([]);
        })
      );
  }

  /**
   * Recherche de produit par code-barres
   * @param barcode Code-barres du produit
   */
  searchByBarcode(barcode: string): Observable<ProduitSearch | null> {
    return this.search(barcode, 1).pipe(
      map(products => products.length > 0 ? products[0] : null)
    );
  }

  /**
   * Crée un opérateur RxJS pour recherche avec debounce
   * Utile pour les champs de recherche en temps réel
   */
  createDebouncedSearch(size: number = 10, searchByStorage: boolean = true) {
    return (source: Observable<string>) =>
      source.pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap(query => this.search(query, size, searchByStorage))
      );
  }
}
