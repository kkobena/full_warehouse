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
      libelleRubrique: 'Données',
      libelle: 'Ventes',
      description: 'Une ligne par vente.',
      periodique: true,
      programmable: true,
      formats: ['XLSX', 'CSV', 'PDF'],
      colonnes: ['Date', 'Numéro'],
      filtres: [
        {
          code: 'NATURE_VENTE',
          libelle: 'Type de vente',
          multiple: true,
          options: [
            { valeur: 'COMPTANT', libelle: 'Comptant' },
            { valeur: 'ASSURANCE', libelle: 'Assurance' },
          ],
        },
        { code: 'VENDEUR', libelle: 'Vendeur', multiple: true, options: [{ valeur: '3', libelle: 'Awa Koné' }] },
      ],
    },
    {
      code: 'PRODUITS',
      libelleRubrique: 'Données',
      libelle: 'Produits et prix',
      description: 'Le référentiel.',
      periodique: false,
      programmable: true,
      formats: ['XLSX', 'CSV', 'PDF'],
      colonnes: ['CIP'],
      filtres: [{ code: 'FAMILLE', libelle: 'Famille', multiple: true, options: [{ valeur: '12', libelle: 'Antalgiques' }] }],
    },
    {
      code: 'PILOTAGE_ANNEES',
      libelleRubrique: 'Pilotage',
      libelle: 'Pilotage — comparaison des années',
      description: 'Un indicateur mois par mois.',
      periodique: false,
      programmable: false,
      formats: ['CSV'],
      colonnes: [],
      filtres: [
        {
          code: 'INDICATEUR_COMPARE',
          libelle: 'Indicateur comparé',
          multiple: false,
          options: [
            { valeur: 'CA_TTC', libelle: "Chiffre d'affaires TTC" },
            { valeur: 'MARGE_BRUTE', libelle: 'Marge brute' },
          ],
        },
      ],
    },
  ],
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
  filtres: ['Famille : 1 valeur'],
});

const ANNEES = CATALOGUE.exports[2];

describe('Exports', () => {
  const api = {
    lireCatalogue: jest.fn(() => of(CATALOGUE)),
    exporter: jest.fn(() => of(new Blob(['x']))),
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
    it('exporte sur la période et télécharge le fichier aussitôt, sous un nom parlant', () => {
      const fixture = TestBed.createComponent(OngletCatalogueComponent);
      fixture.detectChanges();

      const ventes = CATALOGUE.exports[0];
      fixture.componentInstance['choisir'](ventes);
      fixture.componentInstance['du'].set({ year: 2026, month: 10, day: 1 });
      fixture.componentInstance['au'].set({ year: 2026, month: 10, day: 9 });
      fixture.componentInstance['exporter'](ventes);

      expect(api.exporter).toHaveBeenCalledWith({ export: 'VENTES', format: 'PDF', du: '2026-10-01', au: '2026-10-09', filtres: {} });
      expect(telechargement.download).toHaveBeenCalledWith(expect.any(Blob), 'ventes_2026-10-01_2026-10-09', 'pdf');
      expect(api.demander).not.toHaveBeenCalled();
    });

    it('les filtres choisis partent avec la demande ; un filtre vidé ne part pas', () => {
      const fixture = TestBed.createComponent(OngletCatalogueComponent);
      fixture.detectChanges();
      const ventes = CATALOGUE.exports[0];

      fixture.componentInstance['choisir'](ventes);
      fixture.componentInstance['regler']({ code: 'NATURE_VENTE', valeurs: ['COMPTANT', 'ASSURANCE'] });
      fixture.componentInstance['regler']({ code: 'VENDEUR', valeurs: [] });
      fixture.componentInstance['du'].set({ year: 2026, month: 10, day: 1 });
      fixture.componentInstance['au'].set({ year: 2026, month: 10, day: 9 });

      expect(fixture.componentInstance['nombreDeFiltres']()).toBe(1);
      fixture.componentInstance['exporter'](ventes);

      expect(api.exporter).toHaveBeenCalledWith(expect.objectContaining({ filtres: { NATURE_VENTE: ['COMPTANT', 'ASSURANCE'] } }));
    });

    it('changer d’export remet les filtres à zéro', () => {
      const fixture = TestBed.createComponent(OngletCatalogueComponent);
      fixture.detectChanges();

      fixture.componentInstance['choisir'](CATALOGUE.exports[0]);
      fixture.componentInstance['regler']({ code: 'NATURE_VENTE', valeurs: ['COMPTANT'] });
      fixture.componentInstance['choisir'](CATALOGUE.exports[1]);

      expect(fixture.componentInstance['nombreDeFiltres']()).toBe(0);
    });

    it('un modèle préparé depuis le catalogue emporte le format et les filtres réglés', () => {
      const fixture = TestBed.createComponent(OngletCatalogueComponent);
      const modeles: ExportModele[] = [];
      fixture.componentInstance.modele.subscribe(modele => modeles.push(modele));
      fixture.detectChanges();
      const ventes = CATALOGUE.exports[0];

      fixture.componentInstance['choisir'](ventes);
      fixture.componentInstance['choisirFormat']('XLSX');
      fixture.componentInstance['regler']({ code: 'VENDEUR', valeurs: ['3'] });
      fixture.componentInstance['enregistrerCommeModele'](ventes);

      expect(modeles[0]).toEqual(expect.objectContaining({ format: 'XLSX', filtres: { VENDEUR: ['3'] } }));
    });

    it('une édition n’offre que ses formats : le format retenu s’aligne dessus', () => {
      const fixture = TestBed.createComponent(OngletCatalogueComponent);
      fixture.detectChanges();

      fixture.componentInstance['choisirFormat']('XLSX');
      fixture.componentInstance['choisir'](ANNEES);

      expect(fixture.componentInstance['format']()).toBe('CSV');
      expect(fixture.componentInstance['formats']()).toHaveLength(1);
    });

    it('une édition part avec son réglage à valeur unique, sans période', () => {
      const fixture = TestBed.createComponent(OngletCatalogueComponent);
      fixture.detectChanges();

      fixture.componentInstance['choisir'](ANNEES);
      fixture.componentInstance['regler']({ code: 'INDICATEUR_COMPARE', valeurs: ['MARGE_BRUTE'] });
      fixture.componentInstance['exporter'](ANNEES);

      expect(api.exporter).toHaveBeenCalledWith({
        export: 'PILOTAGE_ANNEES',
        format: 'CSV',
        du: null,
        au: null,
        filtres: { INDICATEUR_COMPARE: ['MARGE_BRUTE'] },
      });
    });

    it('un export sans période part sans dates', () => {
      const fixture = TestBed.createComponent(OngletCatalogueComponent);
      fixture.detectChanges();
      fixture.componentInstance['exporter'](CATALOGUE.exports[1]);

      expect(api.exporter).toHaveBeenCalledWith(expect.objectContaining({ export: 'PRODUITS', du: null, au: null }));
    });

    // Une seule grille, sans rubriques ni renvoi vers un autre écran : toutes les entrées ouvertes
    // à l'utilisateur sont là, prêtes à produire leur fichier sur place.
    it('range toutes les entrées dans une seule grille', async () => {
      const fixture = TestBed.createComponent(OngletCatalogueComponent);
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();
      const page = fixture.nativeElement as HTMLElement;

      expect(page.querySelectorAll('.catalogue-grille')).toHaveLength(1);
      expect(page.querySelectorAll('.catalogue-grille app-card')).toHaveLength(CATALOGUE.exports.length);
      expect(page.textContent).not.toContain('restent dans leur écran');
    });

    // Détection de changement automatique, comme dans le navigateur : le gabarit des réglages est
    // réévalué à chaque cycle. Un modèle d'identité nouvelle à chaque appel y faisait réécrire le
    // champ, dont le signal interne relançait un cycle — le fil principal ne rendait plus la main
    // et l'onglet se figeait au premier clic. La régression ne fait pas échouer ce test, elle le
    // fait tourner sans fin : c'est exactement ce que vivait l'écran.
    it('ouvrir les réglages d’un export ne boucle pas', async () => {
      const fixture = TestBed.createComponent(OngletCatalogueComponent);
      fixture.autoDetectChanges();
      await fixture.whenStable();

      const boutons = Array.from<HTMLButtonElement>((fixture.nativeElement as HTMLElement).querySelectorAll('button'));
      boutons.find(bouton => (bouton.textContent ?? '').includes("Préparer l'export"))?.click();
      await new Promise(resolve => setTimeout(resolve, 0));

      expect((fixture.nativeElement as HTMLElement).querySelectorAll('ng-select')).toHaveLength(2);
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

    it('un fichier PDF se télécharge en PDF', () => {
      api.listerHistorique.mockReturnValue(of(new HttpResponse({ body: [], headers: new HttpHeaders() })));
      const fixture = TestBed.createComponent(OngletHistoriqueComponent);
      fixture.componentInstance['telecharger']({ ...fichier('TERMINE'), format: 'PDF' });

      expect(telechargement.download).toHaveBeenCalledWith(expect.any(Blob), 'lignes-vente_2026-10-01_2026-10-09', 'pdf');
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

    it('le panneau refuse de programmer une édition et n’offre que ses formats', () => {
      const fixture = TestBed.createComponent(PanneauModeleComponent);
      const envois: ExportModele[] = [];
      fixture.componentRef.setInput('visible', true);
      fixture.componentRef.setInput('exports', CATALOGUE.exports);
      fixture.componentInstance.enregistrement.subscribe(modele => envois.push(modele));
      fixture.detectChanges();

      fixture.componentInstance['choisirExport']('PILOTAGE_ANNEES');
      fixture.componentInstance['frequence'].set('MONTHLY');
      fixture.componentInstance['libelle'].set('Années comparées');
      fixture.componentInstance['enregistrer']();

      expect(fixture.componentInstance['programmable']()).toBe(false);
      expect(fixture.componentInstance['formats']()).toEqual([expect.objectContaining({ value: 'CSV' })]);
      expect(envois[0]).toEqual(expect.objectContaining({ frequence: null, heure: null, jour: null }));
    });

    it('le panneau propose les filtres de l’export choisi, et les oublie quand on change d’export', () => {
      const fixture = TestBed.createComponent(PanneauModeleComponent);
      const envois: ExportModele[] = [];
      fixture.componentRef.setInput('visible', true);
      fixture.componentRef.setInput('exports', CATALOGUE.exports);
      fixture.componentInstance.enregistrement.subscribe(modele => envois.push(modele));
      fixture.detectChanges();

      fixture.componentInstance['choisirExport']('VENTES');
      expect(fixture.componentInstance['filtresDuModele']()).toHaveLength(2);
      fixture.componentInstance['regler']({ code: 'NATURE_VENTE', valeurs: ['CARNET'] });
      fixture.componentInstance['choisirExport']('PRODUITS');

      fixture.componentInstance['libelle'].set('Référentiel');
      fixture.componentInstance['enregistrer']();

      expect(fixture.componentInstance['filtresDuModele']()).toHaveLength(1);
      expect(envois[0].filtres).toEqual({});
    });
  });
});
