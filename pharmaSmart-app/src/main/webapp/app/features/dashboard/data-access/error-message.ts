import { HttpErrorResponse } from '@angular/common/http';

/** Message métier renvoyé par ExceptionTranslator, à défaut un message générique. */
export function errorMessage(err: HttpErrorResponse, fallback = 'Une erreur est survenue.'): string {
  const body = err?.error as { message?: string; detail?: string } | null;
  return body?.message || body?.detail || fallback;
}
