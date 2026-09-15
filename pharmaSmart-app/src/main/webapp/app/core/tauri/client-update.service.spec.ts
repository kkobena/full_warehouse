import { TestBed } from '@angular/core/testing';

import { ClientUpdateService } from './client-update.service';
import { TauriPrinterService } from 'app/shared/services/tauri-printer.service';

/**
 * Le service est une façade au-dessus de trois commandes Tauri. Ce qui mérite d'être
 * verrouillé, c'est son comportement **hors** application de bureau : dans un
 * navigateur, aucune de ces commandes n'existe, et l'écran ne doit ni appeler dans le
 * vide ni basculer en erreur — il doit simplement rester inerte.
 */
describe('ClientUpdateService', () => {
  let isTauri: boolean;

  const build = (): ClientUpdateService => {
    TestBed.configureTestingModule({
      providers: [{ provide: TauriPrinterService, useValue: { isRunningInTauri: () => isTauri } }],
    });
    return TestBed.inject(ClientUpdateService);
  };

  beforeEach(() => {
    isTauri = false;
    TestBed.resetTestingModule();
  });

  it('se déclare non supporté hors application de bureau', () => {
    expect(build().isSupported()).toBe(false);
  });

  it('se déclare supporté dans l’application de bureau', () => {
    isTauri = true;
    expect(build().isSupported()).toBe(true);
  });

  it('ne change pas d’état quand une vérification est demandée hors Tauri', async () => {
    const service = build();

    await service.check();

    // Ni « checking », ni « error » : l'écran affiche son explication, sans faux signal.
    expect(service.phase()).toBe('idle');
    expect(service.status()).toBeNull();
    expect(service.errorMessage()).toBe('');
  });

  it('ne tente aucune installation hors Tauri', async () => {
    const service = build();

    await service.apply();

    expect(service.phase()).toBe('idle');
    expect(service.errorMessage()).toBe('');
  });

  it('part d’un état vierge', () => {
    const service = build();

    expect(service.phase()).toBe('idle');
    expect(service.status()).toBeNull();
    expect(service.errorMessage()).toBe('');
  });
});
