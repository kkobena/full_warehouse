import {ComponentFixture, TestBed} from '@angular/core/testing';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {of, throwError} from 'rxjs';
import {PosologieEtiquetteModalComponent} from './posologie-etiquette-modal.component';
import {IFormatEtiquette, ProductsApiService} from 'app/features/products/data-access/services/products-api.service';
import {NotificationService} from 'app/shared/services/notification.service';
import {TauriPrinterService} from 'app/shared/services/tauri-printer.service';
import {OrdonnanceApiService} from '../../data-access/services/ordonnance-api.service';

jest.mock('file-saver', () => ({saveAs: jest.fn()}));

/**
 * L'étiquette part chez le patient : une ligne confondue avec une autre, et c'est la mauvaise
 * posologie qui est collée sur la boîte.
 */
describe('PosologieEtiquetteModalComponent', () => {
  let fixture: ComponentFixture<PosologieEtiquetteModalComponent>;
  let composant: PosologieEtiquetteModalComponent;

  const formats: IFormatEtiquette[] = [
    {code: 'A4_65_HISTORIQUE', libelle: '65 étiquettes', reference: 'maison', support: 'PLANCHE_A4', supportLibelle: 'Planche A4', largeurMm: 38, hauteurMm: 21.2, colonnes: 5, rangees: 13, capacite: 65, parDefaut: true},
    {code: 'ROULEAU_58X40', libelle: 'Rouleau 58 × 40', reference: 'die-cut', support: 'ROULEAU', supportLibelle: 'Rouleau', largeurMm: 58, hauteurMm: 40, colonnes: 1, rangees: 1, capacite: 1, parDefaut: false},
  ];

  const api = {
    etiquettesPosologie: jest.fn(() => of(new Blob())),
    etiquettesPosologieZpl: jest.fn(() => of('^XA^XZ')),
  };
  const produitsApi = {listerFormatsEtiquette: jest.fn(() => of(formats))};
  const tauriPrinter = {
    isRunningInTauri: jest.fn(() => false),
    getAvailablePrinters: jest.fn(() => Promise.resolve([{name: 'ZD230', isDefault: true}])),
    printZpl: jest.fn(() => Promise.resolve()),
  };
  const notification = {error: jest.fn()};
  const activeModal = {close: jest.fn(), dismiss: jest.fn()};

  const interne = (): any => composant as any;

  async function monter(): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [PosologieEtiquetteModalComponent],
      providers: [
        {provide: OrdonnanceApiService, useValue: api},
        {provide: ProductsApiService, useValue: produitsApi},
        {provide: TauriPrinterService, useValue: tauriPrinter},
        {provide: NotificationService, useValue: notification},
        {provide: NgbActiveModal, useValue: activeModal},
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(PosologieEtiquetteModalComponent);
    composant = fixture.componentInstance;
    composant.ordonnance = {
      id: 7,
      lignes: [
        {id: 11, produitLibelle: 'DOLIPRANE 1000MG'},
        {id: 12, produitLibelle: 'AMOXICILLINE 500MG'},
      ],
    } as any;
    fixture.detectChanges();
  }

  beforeEach(() => {
    jest.clearAllMocks();
    tauriPrinter.isRunningInTauri.mockReturnValue(false);
    produitsApi.listerFormatsEtiquette.mockReturnValue(of(formats));
    TestBed.resetTestingModule();
  });

  it('imprime toute l’ordonnance tant qu’aucune ligne n’est choisie', async () => {
    await monter();

    interne().imprimer();

    expect(api.etiquettesPosologie).toHaveBeenCalledWith(7, {
      ligneId: undefined,
      qty: 1,
      startAt: 1,
      format: 'A4_65_HISTORIQUE',
    });
  });

  it('n’imprime que la ligne choisie', async () => {
    await monter();

    interne().ligneId.set(12);
    interne().imprimer();

    expect(api.etiquettesPosologie).toHaveBeenCalledWith(7, expect.objectContaining({ligneId: 12}));
  });

  it('présente chaque ligne par son produit', async () => {
    await monter();

    expect(interne().lignes()).toEqual([
      {id: 11, libelle: 'DOLIPRANE 1000MG'},
      {id: 12, libelle: 'AMOXICILLINE 500MG'},
    ]);
  });

  it('un rouleau ramène la case de départ à 1 et la rend sans objet', async () => {
    await monter();
    interne().startAt = 40;

    interne().onFormatChange('ROULEAU_58X40');

    expect(interne().estRouleau()).toBe(true);
    expect(interne().startAt).toBe(1);
    expect(interne().peutImprimer()).toBe(true);
  });

  it('envoie le flux ZPL depuis le poste plutôt qu’un PDF', async () => {
    tauriPrinter.isRunningInTauri.mockReturnValue(true);
    await monter();

    interne().onFormatChange('ROULEAU_58X40');
    interne().onModeChange('ZPL');
    await Promise.resolve();
    interne().imprimer();
    await Promise.resolve();

    expect(api.etiquettesPosologieZpl).toHaveBeenCalledWith(7, expect.objectContaining({format: 'ROULEAU_58X40', resolution: 'DPI_203'}));
    expect(api.etiquettesPosologie).not.toHaveBeenCalled();
    expect(tauriPrinter.printZpl).toHaveBeenCalledWith('^XA^XZ', 'ZD230');
  });

  it('signale l’échec sans fermer la modale', async () => {
    await monter();
    api.etiquettesPosologie.mockReturnValueOnce(throwError(() => new Error('500')) as any);

    interne().imprimer();

    expect(notification.error).toHaveBeenCalled();
    expect(activeModal.close).not.toHaveBeenCalled();
    expect(interne().loading()).toBe(false);
  });
});
