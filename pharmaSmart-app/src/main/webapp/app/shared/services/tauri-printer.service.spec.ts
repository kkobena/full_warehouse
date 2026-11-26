import { TestBed } from '@angular/core/testing';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptors, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { tauriHeadersInterceptor } from 'app/core/interceptor/tauri-headers.interceptor';
import { ErrorHandlerInterceptor } from 'app/core/interceptor/error-handler.interceptor';
import { EventManager } from 'app/core/util/event-manager.service';
import { TauriDeviceDetectionService } from './tauri-device-detection.service';
import { TauriPrinterService } from './tauri-printer.service';

/**
 * Sous Tauri, le constructeur de TauriPrinterService lit la configuration du poste par l'API. La
 * requête traverse tauriHeadersInterceptor, qui injecte TauriDeviceDetectionService. Tant que ce
 * dernier injectait TauriPrinterService, la boucle levait NG0200 : l'erreur était avalée, et la
 * configuration de l'afficheur client n'était jamais lue depuis le serveur.
 */
describe('TauriPrinterService — démarrage sous Tauri', () => {
  let httpMock: HttpTestingController;
  let erreurs: jest.SpyInstance;

  beforeEach(() => {
    (window as any).__TAURI_INTERNALS__ = {};
    erreurs = jest.spyOn(console, 'error').mockImplementation(() => undefined);

    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([tauriHeadersInterceptor])), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    delete (window as any).__TAURI_INTERNALS__;
    erreurs.mockRestore();
    httpMock.verify();
  });

  it("lit la configuration du poste sans boucle d'injection", async () => {
    const service = TestBed.inject(TauriPrinterService);

    const requete = httpMock.expectOne('api/postes/current');
    expect(requete.request.headers.get('X-Tauri-App')).toBe('true');
    requete.flush(null);
    await Promise.resolve();

    expect(service.isRunningInTauri()).toBe(true);
    expect(erreurs).not.toHaveBeenCalledWith(expect.stringContaining('poste configuration'), expect.anything());
  });

  it("TauriDeviceDetectionService s'obtient sans construire TauriPrinterService", async () => {
    // C'est l'ordre du démarrage : l'initialiseur d'app.config.ts injecte ce service en premier.
    const detection = TestBed.inject(TauriDeviceDetectionService);
    await new Promise(resolve => setTimeout(resolve));

    expect(detection.isTauriAvailable()).toBe(true);
    // Ni appel au poste, ni échec de cet appel : TauriPrinterService n'a pas été construit au
    // passage. (Avec la boucle, il l'était, et son appel avortait sur NG0200.)
    httpMock.expectNone('api/postes/current');
    expect(erreurs).not.toHaveBeenCalled();
  });
});

/**
 * Un poste non enregistré (404 sur api/postes/current), ou sans afficheur actif (404 sur
 * .../devices/active), est un cas prévu : repli sur la configuration locale, sans alerte « Not
 * found » à chaque démarrage. Les vraies pannes restent signalées.
 */
describe('TauriPrinterService — configuration du poste absente', () => {
  let httpMock: HttpTestingController;
  let erreurs: jest.SpyInstance;
  let diffusions: jest.SpyInstance;

  beforeEach(() => {
    (window as any).__TAURI_INTERNALS__ = {};
    erreurs = jest.spyOn(console, 'error').mockImplementation(() => undefined);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi(), withInterceptors([tauriHeadersInterceptor])),
        provideHttpClientTesting(),
        { provide: HTTP_INTERCEPTORS, useClass: ErrorHandlerInterceptor, multi: true },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
    diffusions = jest.spyOn(TestBed.inject(EventManager), 'broadcast');
  });

  afterEach(() => {
    delete (window as any).__TAURI_INTERNALS__;
    localStorage.clear();
    erreurs.mockRestore();
    httpMock.verify();
  });

  const reponseAuPoste = async (statut: number): Promise<TauriPrinterService> => {
    const service = TestBed.inject(TauriPrinterService);
    httpMock.expectOne('api/postes/current').flush(null, { status: statut, statusText: 'x' });
    await new Promise(resolve => setTimeout(resolve));
    return service;
  };

  it('poste non enregistré (404) : ni alerte ni erreur en console', async () => {
    const service = await reponseAuPoste(404);

    expect(diffusions).not.toHaveBeenCalled();
    expect(erreurs).not.toHaveBeenCalled();
    expect(service.isCustomerDisplayEnabled()).toBe(false);
  });

  it('poste sans afficheur actif (404) : ni alerte ni erreur en console', async () => {
    TestBed.inject(TauriPrinterService);
    httpMock.expectOne('api/postes/current').flush({ id: 7, address: '192.168.1.51' });
    await new Promise(resolve => setTimeout(resolve));
    httpMock.expectOne(r => r.url === 'api/postes/7/devices/active').flush(null, { status: 404, statusText: 'x' });
    await new Promise(resolve => setTimeout(resolve));

    expect(diffusions).not.toHaveBeenCalled();
    expect(erreurs).not.toHaveBeenCalled();
  });

  it('404 : la configuration locale reste le repli', async () => {
    localStorage.setItem(
      'customerDisplayConfig',
      JSON.stringify({ enabled: true, connectionType: 'SERIAL', serialPort: 'COM3', baudRate: 9600 }),
    );
    const service = await reponseAuPoste(404);

    // Comportement inchangé : l'afficheur configuré localement reste actif.
    expect(service.isCustomerDisplayEnabled()).toBe(true);
    expect(erreurs).not.toHaveBeenCalled();
  });

  it('panne du serveur (500) : toujours signalée', async () => {
    await reponseAuPoste(500);

    expect(diffusions).toHaveBeenCalledWith(expect.objectContaining({ name: 'warehouseApp.httpError' }));
    expect(erreurs).toHaveBeenCalledWith(expect.stringContaining('poste configuration'), expect.anything());
  });
});

/**
 * Un afficheur désactivé côté serveur restait actif sur le poste qui l'avait mémorisé : la réponse
 * « aucun afficheur actif » (404) passait par le repli sur la configuration locale, qui le
 * réactivait. Le serveur fait foi dès lors que le poste est connu.
 */
describe('TauriPrinterService — afficheur désactivé côté serveur', () => {
  let httpMock: HttpTestingController;
  let erreurs: jest.SpyInstance;

  const AFFICHEUR_MEMORISE = { enabled: true, connectionType: 'SERIAL', serialPort: 'COM3', baudRate: 9600 };

  beforeEach(() => {
    (window as any).__TAURI_INTERNALS__ = {};
    erreurs = jest.spyOn(console, 'error').mockImplementation(() => undefined);
    localStorage.setItem('customerDisplayConfig', JSON.stringify(AFFICHEUR_MEMORISE));

    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([tauriHeadersInterceptor])), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    delete (window as any).__TAURI_INTERNALS__;
    localStorage.clear();
    erreurs.mockRestore();
    httpMock.verify();
  });

  const demarrer = async (afficheur: { status: number; body: unknown }): Promise<TauriPrinterService> => {
    const service = TestBed.inject(TauriPrinterService);
    httpMock.expectOne('api/postes/current').flush({ id: 7, address: '192.168.1.51' });
    await new Promise(resolve => setTimeout(resolve));
    httpMock
      .expectOne(r => r.url === 'api/postes/7/devices/active')
      .flush(afficheur.body, { status: afficheur.status, statusText: 'x' });
    await new Promise(resolve => setTimeout(resolve));
    return service;
  };

  const memorise = (): { enabled: boolean } => JSON.parse(localStorage.getItem('customerDisplayConfig')!);

  it('aucun afficheur actif (404) : désactivé, et mémorisé comme tel', async () => {
    const service = await demarrer({ status: 404, body: null });

    expect(service.isCustomerDisplayEnabled()).toBe(false);
    expect(memorise().enabled).toBe(false);
  });

  it('périphérique sans port : désactivé, et mémorisé comme tel', async () => {
    const service = await demarrer({ status: 200, body: { id: 1 } });

    expect(service.isCustomerDisplayEnabled()).toBe(false);
    expect(memorise().enabled).toBe(false);
  });

  it('serveur en panne au démarrage suivant : le repli ne ressuscite pas l\'afficheur retiré', async () => {
    await demarrer({ status: 404, body: null });

    // Démarrage suivant : nouveau service, serveur injoignable sur le poste.
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([tauriHeadersInterceptor])), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(TauriPrinterService);
    httpMock.expectOne('api/postes/current').flush(null, { status: 503, statusText: 'x' });
    await new Promise(resolve => setTimeout(resolve));

    expect(service.isCustomerDisplayEnabled()).toBe(false);
  });

  it('panne sur la lecture de l\'afficheur (500) : le repli local reste en place', async () => {
    const service = await demarrer({ status: 500, body: null });

    // Le serveur ne s'est pas prononcé : l'afficheur mémorisé reste actif.
    expect(service.isCustomerDisplayEnabled()).toBe(true);
    expect(memorise().enabled).toBe(true);
  });

  it('afficheur actif : activé, et mémorisé', async () => {
    const service = await demarrer({ status: 200, body: { id: 1, portName: 'COM5', baudRate: 19200 } });

    expect(service.isCustomerDisplayEnabled()).toBe(true);
    expect(JSON.parse(localStorage.getItem('customerDisplayConfig')!)).toMatchObject({ enabled: true, serialPort: 'COM5', baudRate: 19200 });
  });
});
