import { inject, Injectable } from "@angular/core";
import { HttpClient, HttpResponse } from "@angular/common/http";

import { IConfiguration } from "./model/configuration.model";
import { SERVER_API_URL } from "../app.constants";
import { Observable } from "rxjs";

import { createRequestOption } from "./util/request-util";
import { SessionStorageService } from "ngx-webstorage";
import { IPoste } from "./model/poste.model";

export interface ModelReapproOption {
  value: string;
  label: string;
  description: string;
}

export interface ModelReapproConfig {
  currentModel: string;
  availableModels: ModelReapproOption[];
}

type EntityResponseType = HttpResponse<IConfiguration>;
type EntityArrayResponseType = HttpResponse<IConfiguration[]>;

@Injectable({
  providedIn: "root"
})
export class ConfigurationService {
  private readonly http = inject(HttpClient);
  private readonly sessionStorageService = inject(SessionStorageService);

  private readonly resourceUrl = SERVER_API_URL + "api/app";
  private static readonly POSTE_SESSION_KEY = "current-poste";

  find(id: string): Observable<EntityResponseType> {
    return this.http.get<IConfiguration>(`${this.resourceUrl}/${id}`, { observe: "response" });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IConfiguration[]>(this.resourceUrl, {
      params: options,
      observe: "response"
    });
  }

  findStockConfig(): Observable<EntityResponseType> {
    return this.http.get<IConfiguration>(`${this.resourceUrl}/param-gestion-stock`, { observe: "response" });
  }

  storeParamByKey(key: string, value?: IConfiguration): void {
    this.sessionStorageService.store(key, value);
  }

  getParamByKey(key: string): Observable<EntityResponseType> {
   return  this.find(key)

  }

  clearParam(key: string): void {
    this.sessionStorageService.clear(key);
  }

  update(app: IConfiguration): Observable<HttpResponse<{}>> {
    return this.http.put(this.resourceUrl, app, { observe: "response" });
  }

  getSimpleSaleConfig(): Observable<boolean> {
    return new Observable<boolean>(observer => {
      this.find("use-simple-sale").subscribe({
        next: res => {
          if (res.body) {
            this.storeParamByKey("use-simple-sale", res.body);
            observer.next(Number(res.body.value) == 1);
          } else {
            observer.next(false);
          }
          observer.complete();
        },
        error() {
          observer.next(false);
          observer.complete();
        }
      });
    });
  }


  getCurrentPoste():Observable<HttpResponse<IPoste>> {
   return  this.fetchCurrentPoste();

  }


  fetchCurrentPoste(): Observable<HttpResponse<IPoste>> {
    return this.http.get<IPoste>(`${SERVER_API_URL}api/postes/current`, { observe: "response" });
  }

  getModelReappro(): Observable<HttpResponse<ModelReapproConfig>> {
    return this.http.get<ModelReapproConfig>(`${this.resourceUrl}/model-reappro`, { observe: "response" });
  }

  updateModelReappro(model: string): Observable<HttpResponse<void>> {
    return this.http.put<void>(`${this.resourceUrl}/model-reappro`, null, { params: { model }, observe: "response" });
  }

}
