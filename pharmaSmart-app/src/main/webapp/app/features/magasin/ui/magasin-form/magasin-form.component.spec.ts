import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IMagasin, TypeMagasin } from 'app/shared/model/magasin.model';
import { nouvelleUniteDeGestion } from '../../models/magasin.model';
import { MagasinFormComponent } from './magasin-form.component';

const OFFICINE: IMagasin = {
  id: 1,
  typeMagasin: TypeMagasin.OFFICINE,
  name: 'PHARMA',
  fullName: 'Pharmacie du Centre',
  phone: '0102030405',
  address: 'Rue du Commerce',
  compteBancaire: 'CI001 0001',
  commandeEnPropre: true,
  fnePointOfSale: 'PDV-1',
  fneSecretKey: 'secret',
};

describe('MagasinFormComponent', () => {
  let fixture: ComponentFixture<MagasinFormComponent>;
  let emis: IMagasin[];

  const ouvrir = (magasin: IMagasin): void => {
    fixture = TestBed.createComponent(MagasinFormComponent);
    fixture.componentRef.setInput('magasin', magasin);
    emis = [];
    fixture.componentInstance.enregistrer.subscribe(m => emis.push(m));
    fixture.detectChanges();
  };

  const champ = (id: string): HTMLInputElement | null => fixture.nativeElement.querySelector(`#${id}`);

  const saisir = (id: string, valeur: string): void => {
    const input = champ(id)!;
    input.value = valeur;
    input.dispatchEvent(new Event('input'));
  };

  const soumettre = (): void => {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [MagasinFormComponent] });
  });

  describe('nouvelle unité de gestion', () => {
    beforeEach(() => ouvrir(nouvelleUniteDeGestion(OFFICINE)));

    it("reprend le téléphone et l'adresse de l'officine", () => {
      expect(champ('field_phone')!.value).toBe(OFFICINE.phone);
      expect((fixture.nativeElement.querySelector('#field_address') as HTMLTextAreaElement).value).toBe(OFFICINE.address);
    });

    it("ne propose pas d'identité FNE : l'unité facture sous celle de l'officine", () => {
      expect(champ('fnePointOfSale')).toBeNull();
      expect(champ('fneSecretKey')).toBeNull();
      expect(fixture.nativeElement.textContent).toContain("facture sous l'identité FNE de son officine");
    });

    it("s'enregistre comme unité de gestion rattachée à l'officine", () => {
      saisir('field_fullName', 'Parapharmacie du Centre');
      saisir('field_name', 'PARA');
      soumettre();

      expect(emis).toHaveLength(1);
      expect(emis[0]).toMatchObject({
        id: undefined,
        typeMagasin: TypeMagasin.UNITE_GESTION,
        parentId: OFFICINE.id,
        fullName: 'Parapharmacie du Centre',
        name: 'PARA',
        phone: OFFICINE.phone,
        fnePointOfSale: null,
        fneSecretKey: null,
      });
    });

    it("n'émet rien tant qu'un champ obligatoire manque", () => {
      saisir('field_name', 'PARA');
      soumettre();

      expect(emis).toHaveLength(0);
      expect(fixture.nativeElement.textContent).toContain('Ce champ est obligatoire.');
    });
  });

  describe('focus sur la raison sociale', () => {
    it("est posé à l'ouverture", () => {
      ouvrir(OFFICINE);

      expect(document.activeElement).toBe(champ('field_fullName'));
    });

    it('revient quand le magasin change', () => {
      ouvrir(OFFICINE);
      champ('field_name')!.focus();
      expect(document.activeElement).toBe(champ('field_name'));

      fixture.componentRef.setInput('magasin', { ...OFFICINE, id: 2, fullName: 'Pharmacie de la Gare' });
      fixture.detectChanges();

      expect(document.activeElement).toBe(champ('field_fullName'));
    });
  });

  it('modifier une unité de gestion la laisse unité de gestion, rattachée à son officine', () => {
    // L'ancien formulaire repartait d'une officine vierge : l'unité devenait une officine détachée.
    ouvrir({
      id: 5,
      typeMagasin: TypeMagasin.UNITE_GESTION,
      parentId: 1,
      commandeEnPropre: true,
      name: 'PARA',
      fullName: 'Para',
      phone: '01',
    });
    saisir('field_note', 'Fermé le lundi');
    soumettre();

    expect(emis[0]).toMatchObject({
      id: 5,
      typeMagasin: TypeMagasin.UNITE_GESTION,
      parentId: 1,
      commandeEnPropre: true,
      note: 'Fermé le lundi',
    });
  });

  it("modifier l'officine conserve son identité FNE et ce que le formulaire n'affiche pas", () => {
    ouvrir(OFFICINE);
    expect(champ('fnePointOfSale')!.value).toBe('PDV-1');

    saisir('email', '');
    soumettre();

    expect(emis[0]).toMatchObject({
      id: 1,
      typeMagasin: TypeMagasin.OFFICINE,
      parentId: undefined,
      compteBancaire: 'CI001 0001',
      fnePointOfSale: 'PDV-1',
      fneSecretKey: 'secret',
      // Une saisie vide part en absence, pas en chaîne vide que le serveur refuserait.
      email: null,
    });
  });
});
