import { HttpClient, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { DeviceType, IPosteDevice } from '../../../../shared/model/poste-device.model';

type EntityResponseType = HttpResponse<IPosteDevice>;
type EntityArrayResponseType = HttpResponse<IPosteDevice[]>;

@Injectable({
  providedIn: 'root',
})
export class PosteDeviceService {
  private readonly http = inject(HttpClient);

  private resourceUrl(posteId: number): string {
    return `${SERVER_API_URL}api/postes/${posteId}/devices`;
  }

  /**
   * Liste tous les périphériques configurés pour un poste.
   */
  fetchAll(posteId: number, type?: DeviceType): Observable<EntityArrayResponseType> {
    const params: Record<string, string> = {};
    if (type) {
      params['type'] = type;
    }
    return this.http.get<IPosteDevice[]>(this.resourceUrl(posteId), { params, observe: 'response' });
  }

  /**
   * Récupère le périphérique actif pour un poste et un type donné.
   */
  getActiveDevice(posteId: number, type: DeviceType): Observable<EntityResponseType> {
    return this.http.get<IPosteDevice>(`${this.resourceUrl(posteId)}/active`, {
      params: { type },
      observe: 'response',
    });
  }

  /**
   * Ajoute un nouveau périphérique pour un poste.
   */
  create(posteId: number, device: IPosteDevice): Observable<EntityResponseType> {
    return this.http.post<IPosteDevice>(this.resourceUrl(posteId), device, { observe: 'response' });
  }

  /**
   * Met à jour un périphérique.
   */
  update(posteId: number, deviceId: number, device: IPosteDevice): Observable<EntityResponseType> {
    return this.http.put<IPosteDevice>(`${this.resourceUrl(posteId)}/${deviceId}`, device, { observe: 'response' });
  }

  /**
   * Active un périphérique (désactive les autres du même type).
   * Action manuelle de l'utilisateur pour choisir son device préféré.
   */
  activate(posteId: number, deviceId: number): Observable<HttpResponse<void>> {
    return this.http.put<void>(`${this.resourceUrl(posteId)}/${deviceId}/activate`, null, { observe: 'response' });
  }


  /**
   * Supprime un périphérique configuré.
   */
  delete(posteId: number, deviceId: number): Observable<HttpResponse<void>> {
    return this.http.delete<void>(`${this.resourceUrl(posteId)}/${deviceId}`, { observe: 'response' });
  }
}

