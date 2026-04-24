import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import {
  IReconciliationFactureFournisseur,
  IReconciliationCommand,
} from 'app/shared/model/reconciliation-facture-fournisseur.model';

@Injectable({ providedIn: 'root' })
export class ReconciliationFournisseurService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(ApplicationConfigService);
  private readonly base = this.config.getEndpointFor('api/bons');

  find(id: number, date: string): Observable<IReconciliationFactureFournisseur | null> {
    return this.http
      .get<IReconciliationFactureFournisseur>(`${this.base}/${id}/${date}/reconciliation`)
      .pipe(catchError(() => of(null)));
  }

  save(id: number, date: string, cmd: IReconciliationCommand): Observable<IReconciliationFactureFournisseur> {
    return this.http.post<IReconciliationFactureFournisseur>(
      `${this.base}/${id}/${date}/reconciliation`,
      cmd,
    );
  }
}
