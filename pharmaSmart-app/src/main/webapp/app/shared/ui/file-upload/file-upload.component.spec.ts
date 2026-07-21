import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FileUploadComponent } from './file-upload.component';

/** Fabrique un File de taille arbitraire sans allouer réellement les octets. */
const fileOf = (name: string, sizeMb: number): File => {
  const file = new File(['x'], name);
  Object.defineProperty(file, 'size', { value: sizeMb * 1024 * 1024 });
  return file;
};

describe('FileUploadComponent', () => {
  let fixture: ComponentFixture<FileUploadComponent>;

  const render = (): HTMLElement => {
    fixture.detectChanges();
    return fixture.nativeElement;
  };

  const fileInput = (): HTMLInputElement => render().querySelector('input[type=file]')!;

  /** Simule un choix de fichiers dans le sélecteur natif. */
  const choose = (...files: File[]): void => {
    const input = fileInput();
    Object.defineProperty(input, 'files', { value: files, configurable: true });
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [FileUploadComponent] }).compileComponents();
    fixture = TestBed.createComponent(FileUploadComponent);
  });

  it('masque le champ natif derrière un bouton', () => {
    expect([...fileInput().classList]).toContain('d-none');
    expect(render().querySelector('button')!.textContent).toContain('Choisir un fichier');
  });

  it('propage accept et multiple au champ natif', () => {
    fixture.componentRef.setInput('accept', '.csv,.xlsx');
    fixture.componentRef.setInput('multiple', true);
    expect(fileInput().accept).toBe('.csv,.xlsx');
    expect(fileInput().multiple).toBe(true);
  });

  it('émet les fichiers choisis', () => {
    const selected = jest.fn();
    fixture.componentInstance.filesSelected.subscribe(selected);
    const file = fileOf('produits.csv', 1);
    choose(file);
    expect(selected).toHaveBeenCalledWith([file]);
  });

  it('affiche le nom du fichier retenu', () => {
    choose(fileOf('produits.csv', 1));
    expect(render().textContent).toContain('produits.csv');
  });

  it('résume la sélection au-delà d\'un fichier', () => {
    choose(fileOf('a.csv', 1), fileOf('b.csv', 1));
    expect(render().textContent).toContain('2 fichiers sélectionnés');
  });

  describe('contrôle de taille', () => {
    beforeEach(() => fixture.componentRef.setInput('maxSizeMb', 5));

    it('écarte un fichier trop volumineux et explique pourquoi', () => {
      const selected = jest.fn();
      const rejected = jest.fn();
      fixture.componentInstance.filesSelected.subscribe(selected);
      fixture.componentInstance.rejected.subscribe(rejected);

      choose(fileOf('gros.csv', 10));

      expect(selected).not.toHaveBeenCalled();
      expect(rejected).toHaveBeenCalledWith('« gros.csv » dépasse la taille maximale de 5 Mo.');
    });

    it('ne retient que les fichiers valides d\'une sélection mixte', () => {
      const selected = jest.fn();
      fixture.componentInstance.filesSelected.subscribe(selected);
      const ok = fileOf('ok.csv', 1);
      choose(ok, fileOf('gros.csv', 10));
      expect(selected).toHaveBeenCalledWith([ok]);
    });

    it('laisse passer les fichiers quand aucune limite n\'est fixée', () => {
      fixture.componentRef.setInput('maxSizeMb', 0);
      const selected = jest.fn();
      fixture.componentInstance.filesSelected.subscribe(selected);
      const enorme = fileOf('enorme.csv', 500);
      choose(enorme);
      expect(selected).toHaveBeenCalledWith([enorme]);
    });
  });

  it('réinitialise le champ natif, pour que rechoisir le même fichier émette à nouveau', () => {
    choose(fileOf('produits.csv', 1));
    expect(fileInput().value).toBe('');
  });

  it('permet de retirer la sélection', () => {
    const selected = jest.fn();
    choose(fileOf('produits.csv', 1));
    fixture.componentInstance.filesSelected.subscribe(selected);

    const clearButton = [...render().querySelectorAll('button')].find(b => b.textContent?.includes('Retirer'))!;
    clearButton.click();
    fixture.detectChanges();

    expect(selected).toHaveBeenCalledWith([]);
    expect(render().textContent).not.toContain('produits.csv');
  });

  it('désactive le bouton et le champ', () => {
    fixture.componentRef.setInput('disabled', true);
    expect(render().querySelector('button')!.disabled).toBe(true);
    expect(fileInput().disabled).toBe(true);
  });
});
