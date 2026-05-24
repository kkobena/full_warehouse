import { HttpParams } from '@angular/common/http';

export interface Pagination {
  page: number;
  size: number;
  sort: string[];
}

export interface Search {
  query: string;
}

export const createRequestOption = (req?: any): HttpParams => {
  let options: HttpParams = new HttpParams();
  if (req) {
    Object.keys(req).forEach(key => {
      if (key !== 'sort') {
        options = options.set(key, req[key]);
      }
    });

    if (req.sort) {
      req.sort.forEach((val: string) => {
        options = options.append('sort', val);
      });
    }
  }

  return options;
};
export const createRequestOptions = (req?: any): HttpParams => {
  let options: HttpParams = new HttpParams();
  if (req) {
    Object.keys(req).forEach(key => {
      if (key !== 'sort') {
        const value = req[key];
        if (value !== null && value !== undefined) {
          if (Array.isArray(value)) {
            // Chaque valeur du tableau → param répété (?key=v1&key=v2)
            (value as any[]).forEach(v => {
              options = options.append(key, v);
            });
          } else {
            options = options.set(key, value);
          }
        }
      }
    });

    if (req.sort) {
      req.sort.forEach((val: string) => {
        options = options.append('sort', val);
      });
    }
  }

  return options;
};
