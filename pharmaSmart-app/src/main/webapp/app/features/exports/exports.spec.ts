import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { BlobDownloadService } from 'app/shared/services/blob-download.service';
import { ExportsApiService } from './data-access/exports-api.service';
import { CatalogueExports, ExportFichier, ExportModele } from './models/exports.model';
import { OngletCatalogueComponent } from './ui/onglet-catalogue/onglet-catalogue.component';
import { OngletHistoriqueComponent } from './ui/onglet-historique/onglet-historique.component';
import { OngletModelesComponent } from './ui/onglet-modeles/onglet-modeles.component';
import { PanneauModeleComponent } from './ui/panneau-modele/panneau-modele.component';

const CATALOGUE: CatalogueExports = {
  exports: [
    {
      code: 'VENTES',
      rubrique: 'DONNEES',
      libelleRubrique: 'Données',
      libelle: 'Ventes',
      description: 'Une ligne par vente.',
      periodique: true,
      colonnes: ['Date', 'Numéro'],
    },
    {
      code: 'PRODUITS',
      rubrique: 'DONNEES',
      libelleRubrique: 'Données',
      libelle: 'Produits et prix',
      description: 'Le référentiel.',
      periodique: false,
      colonnes: ['CIP'],
    },
  ],
  liens: [{ rubrique: 'Pilotage', libelle: 'Analyser', description: 'En CSV.', route: '/pilotage', onglet: 'analyser' }],
};

const fichier = (statut: ExportFichier['statut']): ExportFichier => ({
  id: 7,
  export: 'LIGNES_VENTE',
  libelleExport: 'Lignes de vente',
  format: 'XLSX',
  du: '2026-10-01',
  au: '2026-10-09',
  statut,
  nombreLignes: statut === 'TERMINE' ? 1200 : null,
  taille: statut === 'TERMINE' ? 52_000 : null,
  erreur: null,
  demandePar: 'Awa Koné',
  demandeLe: '2026-10-09T10:00:00',
  termineLe: null,
  expireLe: null,
  modele: null,
});

describe('Exports', () => {
  const api = {
    lireCatalogue: jest.fn(() => of(CATALOGUE)),
    demander: jest.fn(() => of(fichier('EN_ATTENTE'))),
    listerHistorique: jest.fn(),
    telecharger: jest.fn(() => of(new Blob(['x']))),
    listerModeles: jest.fn(() => of([] as ExportModele[])),
    enregistrerModele: jest.fn(),
    executerModele: jest.fn(),
    supprimerModele: jest.fn(),
  };
  const router = { navigate: jest.fn() };
  const telechargement = { download: jest.fn() };

  beforeEach(async () => {
    jest.clearAllMocks();
    await TestBed.configureTestingModule({
      providers: [
        { provide: ExportsApiService, useValue: api },
        { provide: Router, useValue: router },
        { provide: BlobDownloadService, useValue: telechargement },
      ],
    }).compileComponents();
  });

  describe('catalogue', () => {
    it('regroupe par rubrique, exporte sur la période et bascule vers l’historique', async () => {
      const fixture = TestBed.createComponent(OngletCatalogueComponent);
      const demandes: void[] = [];
      fixture.componentInstance.demande.subscribe(() => demandes.push(undefined));
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      expect((fixture.nativeElement as HTMLElement).textContent).toContain('Données');
      const ventes = CATALOGUE.exports[0];
      fixture.componentInstance['choisir'](ventes);
      fixture.componentInstance['du'].set({ year: 2026, month: 10, day: 1 });
      fixture.componentInstance['au'].set({ year: 2026, month: 10, day: 9 });
      fixture.componentInstance['exporter'](ventes);

      expect(api.demander).toHaveBeenCalledWith({ export: 'VENTES', format: 'XLSX', du: '2026-10-01', au: '2026-10-09' });
      expect(demandes).toHaveLength(1);
    });

    it('un export sans période part sans dates ; un lien ouvre son écran et son onglet', async () => {
      const fixture = TestBed.createComponent(OngletCatalogueComponent);
      fixture.detectChanges();
      fixture.componentInstance['exporter'](CATALOGUE.exports[1]);
      fixture.componentInstance['ouvrir'](CATALOGUE.liens[0]);

      expect(api.demander).toHaveBeenCalledWith(expect.objectContaining({ export: 'PRODUITS', du: null, au: null }));
      expect(router.navigate).toHaveBeenCalledWith(['/pilotage'], { queryParams: { onglet: 'analyser' } });
    });
  });

  describe('historique', () => {
    afterEach(() => jest.useRealTimers());

    it('se relit tant qu’un fichier se prépare, puis s’arrête', async () => {
      jest.useFakeTimers();
      api.listerHistorique
        .mockReturnValueOnce(of(new HttpResponse({ body: [fichier('EN_COURS')], headers: new HttpHeaders({ 'X-Total-Count': '1' }) })))
        .mockReturnValue(of(new HttpResponse({ body: [fichier('TERMINE')], headers: new HttpHeaders({ 'X-Total-Count': '1' }) })));
      const fixture = TestBed.createComponent(OngletHistoriqueComponent);
      fixture.detectChanges();
      await Promise.resolve();
      fixture.detectChanges();

      jest.advanceTimersByTime(3_000);
      fixture.detectChanges();
      await Promise.resolve();
      fixture.detectChanges();
      jest.advanceTimersByTime(10_000);

      expect(api.listerHistorique).toHaveBeenCalledTimes(2);
      expect((fixture.nativeElement as HTMLElement).textContent).toContain('Prêt');
    });

    it('télécharge un fichier prêt sous un nom parlant', () => {
      api.listerHistorique.mockReturnValue(of(new HttpResponse({ body: [], headers: new HttpHeaders() })));
      const fixture = TestBed.createComponent(OngletHistoriqueComponent);
      fixture.componentInstance['telecharger'](fichier('TERMINE'));

      expect(telechargement.download).toHaveBeenCalledWith(expect.any(Blob), 'lignes-vente_2026-10-01_2026-10-09', 'excel');
    });
  });

  describe('modèles', () => {
    it('dit la programmation en clair', () => {
      const fixture = TestBed.createComponent(OngletModelesComponent);
      const modele = { frequence: 'WEEKLY', heure: '07:00:00', jour: 1 } as ExportModele;

      expect(fixture.componentInstance['libelleProgrammation'](modele)).toBe('Chaque lundi à 07:00');
      expect(fixture.componentInstance['libelleProgrammation']({ ...modele, frequence: 'MONTHLY', jour: 5 })).toBe('Le 5 de chaque mois à 07:00');
    });

    it('le panneau n’envoie ni jour ni heure à un modèle non programmé, et pas de période à un export sans période', () => {
      const fixture = TestBed.createComponent(PanneauModeleComponent);
      const envois: ExportModele[] = [];
      fixture.componentRef.setInput('visible', true);
      fixture.componentRef.setInput('exports', CATALOGUE.exports);
      fixture.componentInstance.enregistrement.subscribe(modele => envois.push(modele));
      fixture.detectChanges();

      fixture.componentInstance['libelle'].set('Référentiel du lundi');
      fixture.componentInstance['export'].set('PRODUITS');
      fixture.componentInstance['enregistrer']();

      expect(envois[0]).toEqual(expect.objectContaining({ libelle: 'Référentiel du lundi', periode: null, frequence: null, heure: null, jour: null }));
    });
  });
});
