import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { TauriDeviceDetectionService } from '../../shared/services/tauri-device-detection.service';

export const tauriHeadersInterceptor: HttpInterceptorFn = (req, next) => {
  const tauriDeviceService = inject(TauriDeviceDetectionService);

  if (!tauriDeviceService.isTauriAvailable()) {
    return next(req);
  }

  const headers: Record<string, string> = { 'X-Tauri-App': 'true' };

  const info = tauriDeviceService.systemInfo();
  if (info?.hostname) {
    headers['X-Poste-Hostname'] = info.hostname;
  }
  if (info?.localIp) {
    headers['X-Poste-Ip'] = info.localIp;
  }

  return next(req.clone({ setHeaders: headers }));
};
