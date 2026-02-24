import { SalesStore } from '../store/sales.store';
import { NotificationService } from '../../../../shared/services/notification.service';

/** Extrait errorKey et errorMessage depuis une erreur HTTP API */
export function extractApiError(error: any, defaultMessage: string): { errorMessage: string; errorKey: string | null } {
  let errorMessage = defaultMessage;
  let errorKey: string | null = null;

  if (error?.error) {
    errorKey = error.error.errorKey ?? null;

    if (errorKey === 'stock') {
      errorMessage = error.error.message || error.error.detail || 'Stock insuffisant';
    } else if (errorKey === 'stockChInsufisant') {
      errorMessage = 'Stock insuffisant - Déconditionnement nécessaire';
    } else if (errorKey === 'customerInsuranceCreditLimit') {
      errorMessage = error.error.message || 'Plafond de vente atteint';
    } else if (error.error.message) {
      errorMessage = error.error.message;
    } else if (error.error.detail) {
      errorMessage = error.error.detail;
    }
  }

  return { errorMessage, errorKey };
}

/** Notifie le dépassement de plafond une seule fois par vente */
export function handlePlafondVenteWarning(
  store: InstanceType<typeof SalesStore>,
  notificationService: NotificationService,
  errorMessage: string,
): void {
  if (!store.plafondIsReached()) {
    notificationService.warning(errorMessage);
    store.setPlafondIsReached(true, errorMessage);
  }
}
